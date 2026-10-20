package com.cooling.platform.factory;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.entity.Chiller;
import com.cooling.platform.entity.CoolingTower;
import com.cooling.platform.entity.CoolingUnit;
import com.cooling.platform.entity.Device;
import com.cooling.platform.entity.Pump;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 设备工厂单元测试（FM-01 参数录入校验）。
 *
 * <p>单表继承下子类专有属性走 {@code params} JSON，工厂负责把弱类型 Map
 * 转成强类型字段并执行边界校验，是防止脏数据入库的最后一道关口。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@DisplayName("设备工厂")
class DeviceFactoryTest {

    private final DeviceFactory factory = new DeviceFactory();

    private static Map<String, Object> params(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    @Nested
    @DisplayName("按类型创建")
    class Create {

        @Test
        @DisplayName("四种设备类型均返回对应子类，且传感器按元数据初始化")
        void shouldCreateConcreteSubclass() {
            assertTrue(factory.create(DeviceType.CHILLER) instanceof Chiller);
            assertTrue(factory.create(DeviceType.COOLING_TOWER) instanceof CoolingTower);
            assertTrue(factory.create(DeviceType.PUMP) instanceof Pump);
            assertTrue(factory.create(DeviceType.COOLING_UNIT) instanceof CoolingUnit);

            Device chiller = factory.create(DeviceType.CHILLER);
            assertEquals(DeviceType.CHILLER, chiller.getDeviceType());
            assertEquals("冷水机组", chiller.getDeviceType().getLabel());
            assertEquals(2, chiller.getSensors().size(), "冷机默认带供水/回水两个传感器");
            assertTrue(chiller.getSensors().stream()
                    .allMatch(s -> s.getMinValue() < s.getMaxValue()));
            assertEquals(1, factory.create(DeviceType.PUMP).getSensors().size());
            assertEquals(3, factory.create(DeviceType.COOLING_TOWER).getSensors().size());
        }

        @Test
        @DisplayName("设备类型为空时抛出 400")
        void shouldRejectNullType() {
            BusinessException ex = assertThrows(BusinessException.class, () -> factory.create(null));
            assertEquals(400, ex.getCode());
        }
    }

    @Nested
    @DisplayName("冷机参数")
    class ChillerParams {

        @Test
        @DisplayName("合法参数写入专有属性与传感器")
        void shouldApplyValidParams() {
            Chiller chiller = (Chiller) factory.create(DeviceType.CHILLER);
            factory.applyParams(chiller, params(
                    "coolingCapacity", 130.0, "currentLoad", 0.65,
                    "refrigerantType", "R134a", "chilledWaterSupplyTemp", 7.5));

            assertEquals(130.0, chiller.getCoolingCapacity(), 1e-6);
            assertEquals(0.65, chiller.getCurrentLoad(), 1e-6);
            assertEquals("R134a", chiller.getRefrigerantType());
            assertEquals(7.5, chiller.getChilledWaterSupplyTemp().getValue(), 1e-6);
            assertEquals("℃", chiller.getChilledWaterSupplyTemp().getUnit(),
                    "单位应由 SensorCatalog 补齐，而不是让前端传");
        }

        @Test
        @DisplayName("负载率超出 0~1 抛出 400")
        void shouldRejectInvalidCurrentLoad() {
            Chiller chiller = (Chiller) factory.create(DeviceType.CHILLER);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> factory.applyParams(chiller, params("currentLoad", 1.5)));
            assertEquals(400, ex.getCode());
            assertTrue(ex.getMessage().contains("currentLoad"));
        }

        @Test
        @DisplayName("负值参数抛出 400")
        void shouldRejectNegativeValue() {
            Chiller chiller = (Chiller) factory.create(DeviceType.CHILLER);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> factory.applyParams(chiller, params("coolingCapacity", -1.0)));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("未知参数键抛出 400，避免静默丢弃")
        void shouldRejectUnknownKey() {
            Chiller chiller = (Chiller) factory.create(DeviceType.CHILLER);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> factory.applyParams(chiller, params("unknownKey", 1)));
            assertEquals(400, ex.getCode());
            assertTrue(ex.getMessage().contains("unknownKey"));
        }

        @Test
        @DisplayName("非数值文本抛出 400")
        void shouldRejectNonNumericValue() {
            Chiller chiller = (Chiller) factory.create(DeviceType.CHILLER);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> factory.applyParams(chiller, params("coolingCapacity", "很多")));
            assertEquals(400, ex.getCode());
        }
    }

    @Nested
    @DisplayName("冷却塔与泵参数")
    class TowerAndPumpParams {

        @Test
        @DisplayName("冷却塔枚举与转速解析")
        void shouldApplyTowerParams() {
            CoolingTower tower = (CoolingTower) factory.create(DeviceType.COOLING_TOWER);
            factory.applyParams(tower, params("towerType", "CLOSED", "fanSpeed", 60, "outletTemp", 32.0));

            assertEquals("CLOSED", tower.getTowerType().name());
            assertEquals(60, tower.getFanSpeed());
            assertEquals(32.0, tower.getOutletTemp().getValue(), 1e-6);
        }

        @Test
        @DisplayName("转速超过 100 抛出 400")
        void shouldRejectTooFastFan() {
            CoolingTower tower = (CoolingTower) factory.create(DeviceType.COOLING_TOWER);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> factory.applyParams(tower, params("fanSpeed", 120)));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("非法枚举值抛出 400")
        void shouldRejectInvalidEnum() {
            Pump pump = (Pump) factory.create(DeviceType.PUMP);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> factory.applyParams(pump, params("pumpType", "PLASMA")));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("泵的扬程、功率、流量写入正确")
        void shouldApplyPumpParams() {
            Pump pump = (Pump) factory.create(DeviceType.PUMP);
            factory.applyParams(pump, params("pumpType", "CHILLED_WATER", "head", 30.0,
                    "power", 22.0, "speed", 75, "flowRate", 85.0));

            assertEquals("CHILLED_WATER", pump.getPumpType().name());
            assertEquals(30.0, pump.getHead(), 1e-6);
            assertEquals(22.0, pump.getPower(), 1e-6);
            assertEquals(75, pump.getSpeed());
            assertEquals(85.0, pump.getFlowRate().getValue(), 1e-6);
        }

        @Test
        @DisplayName("空参数不做任何改动，保留创建时的默认值")
        void shouldIgnoreEmptyParams() {
            Chiller chiller = (Chiller) factory.create(DeviceType.CHILLER);
            factory.applyParams(chiller, null);
            factory.applyParams(chiller, new HashMap<String, Object>());
            assertEquals("R134a", chiller.getRefrigerantType(),
                    "未传的参数应保持创建时的默认值，而不是被覆盖成空串");
        }

        @Test
        @DisplayName("把冷却塔参数喂给冷机会被拒绝")
        void shouldRejectCrossTypeKey() {
            Chiller chiller = (Chiller) factory.create(DeviceType.CHILLER);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> factory.applyParams(chiller, params("towerType", "CLOSED")));
            assertEquals(400, ex.getCode());
        }
    }
}
