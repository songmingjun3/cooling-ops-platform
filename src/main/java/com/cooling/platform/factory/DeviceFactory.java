package com.cooling.platform.factory;

import com.cooling.platform.common.constant.DeviceConstants;
import com.cooling.platform.common.constant.SensorCatalog;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.enums.PumpType;
import com.cooling.platform.common.enums.TowerType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.entity.Chiller;
import com.cooling.platform.entity.CoolingTower;
import com.cooling.platform.entity.CoolingUnit;
import com.cooling.platform.entity.Device;
import com.cooling.platform.entity.Pump;
import com.cooling.platform.entity.Sensor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 设备工厂。
 *
 * <p>职责有二：</p>
 * <ol>
 *   <li>按 {@link DeviceType} 创建具体设备子类实例，并初始化其专有属性的默认值；</li>
 *   <li>把前端或数据库传入的「参数键 → 值」映射写入具体设备子类，同时完成类型转换与合法性校验。</li>
 * </ol>
 *
 * <p>新增设备类型时，除新增子类外只需在 {@link #create(DeviceType)} 与对应的
 * {@code applyXxx} 方法中补充分支，既有的查询、列表与分组逻辑无需改动。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Component
public class DeviceFactory {

    /**
     * 创建指定类型的设备实例（专有属性为默认值，传感器值为 0 表示尚未采集）。
     *
     * @param type 设备类型
     * @return 设备实例
     */
    public Device create(DeviceType type) {
        if (type == null) {
            throw new BusinessException(400, "设备类型不能为空");
        }
        switch (type) {
            case CHILLER:
                Chiller chiller = new Chiller();
                chiller.setCoolingCapacity(0.0);
                chiller.setCurrentLoad(0.0);
                chiller.setRefrigerantType("R134a");
                chiller.setChilledWaterSupplyTemp(SensorCatalog.create(type, "chilledWaterSupplyTemp", 0.0));
                chiller.setChilledWaterReturnTemp(SensorCatalog.create(type, "chilledWaterReturnTemp", 0.0));
                return chiller;
            case COOLING_TOWER:
                CoolingTower tower = new CoolingTower();
                tower.setTowerType(TowerType.OPEN);
                tower.setFanSpeed(0);
                tower.setWaterFlowRate(SensorCatalog.create(type, "waterFlowRate", 0.0));
                tower.setAmbientTemp(SensorCatalog.create(type, "ambientTemp", 0.0));
                tower.setOutletTemp(SensorCatalog.create(type, "outletTemp", 0.0));
                return tower;
            case PUMP:
                Pump pump = new Pump();
                pump.setPumpType(PumpType.CHILLED_WATER);
                pump.setHead(0.0);
                pump.setPower(0.0);
                pump.setSpeed(0);
                pump.setFlowRate(SensorCatalog.create(type, "flowRate", 0.0));
                return pump;
            case COOLING_UNIT:
                CoolingUnit unit = new CoolingUnit();
                unit.setSpeed(0);
                unit.setSupplyTemp(SensorCatalog.create(type, "supplyTemp", 0.0));
                unit.setReturnTemp(SensorCatalog.create(type, "returnTemp", 0.0));
                return unit;
            default:
                throw new BusinessException(400, "不支持的设备类型: " + type);
        }
    }

    /**
     * 将参数映射写入设备子类。
     *
     * <p>未出现在映射中的字段保持默认值；出现未登记的键时抛出业务异常，
     * 便于前端及早发现拼写错误，而不是静默忽略。</p>
     *
     * @param device 目标设备
     * @param params 参数映射，可为 {@code null}
     */
    public void applyParams(Device device, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        switch (device.getDeviceType()) {
            case CHILLER:
                applyChiller((Chiller) device, params);
                break;
            case COOLING_TOWER:
                applyCoolingTower((CoolingTower) device, params);
                break;
            case PUMP:
                applyPump((Pump) device, params);
                break;
            case COOLING_UNIT:
                applyCoolingUnit((CoolingUnit) device, params);
                break;
            default:
                throw new BusinessException(400, "不支持的设备类型: " + device.getDeviceType());
        }
    }

    private void applyChiller(Chiller chiller, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "coolingCapacity":
                    chiller.setCoolingCapacity(nonNegative(key, toDouble(key, entry.getValue())));
                    break;
                case "currentLoad":
                    chiller.setCurrentLoad(ratio(key, toDouble(key, entry.getValue())));
                    break;
                case "refrigerantType":
                    chiller.setRefrigerantType(toStringValue(key, entry.getValue()));
                    break;
                case "chilledWaterSupplyTemp":
                case "chilledWaterReturnTemp":
                    setSensor(chiller, DeviceType.CHILLER, key, entry.getValue());
                    break;
                default:
                    throw unsupportedKey(DeviceType.CHILLER, key);
            }
        }
    }

    private void applyCoolingTower(CoolingTower tower, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "towerType":
                    tower.setTowerType(toEnum(TowerType.class, key, entry.getValue()));
                    break;
                case "fanSpeed":
                    tower.setFanSpeed(rotateSpeed(key, toDouble(key, entry.getValue())));
                    break;
                case "waterFlowRate":
                case "ambientTemp":
                case "outletTemp":
                    setSensor(tower, DeviceType.COOLING_TOWER, key, entry.getValue());
                    break;
                default:
                    throw unsupportedKey(DeviceType.COOLING_TOWER, key);
            }
        }
    }

    private void applyPump(Pump pump, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "pumpType":
                    pump.setPumpType(toEnum(PumpType.class, key, entry.getValue()));
                    break;
                case "head":
                    pump.setHead(nonNegative(key, toDouble(key, entry.getValue())));
                    break;
                case "power":
                    pump.setPower(nonNegative(key, toDouble(key, entry.getValue())));
                    break;
                case "speed":
                    pump.setSpeed(rotateSpeed(key, toDouble(key, entry.getValue())));
                    break;
                case "flowRate":
                    setSensor(pump, DeviceType.PUMP, key, entry.getValue());
                    break;
                default:
                    throw unsupportedKey(DeviceType.PUMP, key);
            }
        }
    }

    private void applyCoolingUnit(CoolingUnit unit, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "speed":
                    unit.setSpeed(rotateSpeed(key, toDouble(key, entry.getValue())));
                    break;
                case "supplyTemp":
                case "returnTemp":
                    setSensor(unit, DeviceType.COOLING_UNIT, key, entry.getValue());
                    break;
                default:
                    throw unsupportedKey(DeviceType.COOLING_UNIT, key);
            }
        }
    }

    // ------------------------------------------------------------------
    // 参数转换与校验
    // ------------------------------------------------------------------

    private void setSensor(Device device, DeviceType type, String key, Object rawValue) {
        double value = nonNegative(key, toDouble(key, rawValue));
        Sensor sensor = SensorCatalog.create(type, key, value);
        if (device instanceof Chiller) {
            Chiller chiller = (Chiller) device;
            if ("chilledWaterSupplyTemp".equals(key)) {
                chiller.setChilledWaterSupplyTemp(sensor);
            } else {
                chiller.setChilledWaterReturnTemp(sensor);
            }
        } else if (device instanceof CoolingTower) {
            CoolingTower tower = (CoolingTower) device;
            if ("waterFlowRate".equals(key)) {
                tower.setWaterFlowRate(sensor);
            } else if ("ambientTemp".equals(key)) {
                tower.setAmbientTemp(sensor);
            } else {
                tower.setOutletTemp(sensor);
            }
        } else if (device instanceof Pump) {
            ((Pump) device).setFlowRate(sensor);
        } else if (device instanceof CoolingUnit) {
            CoolingUnit unit = (CoolingUnit) device;
            if ("supplyTemp".equals(key)) {
                unit.setSupplyTemp(sensor);
            } else {
                unit.setReturnTemp(sensor);
            }
        }
    }

    private double toDouble(String key, Object value) {
        if (value instanceof Number) {
            double result = ((Number) value).doubleValue();
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                throw new BusinessException(400, "参数 " + key + " 非法：必须为有效数值");
            }
            return result;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ex) {
                throw new BusinessException(400, "参数 " + key + " 非法：必须为数值");
            }
        }
        throw new BusinessException(400, "参数 " + key + " 非法：必须为数值");
    }

    private String toStringValue(String key, Object value) {
        if (value == null) {
            throw new BusinessException(400, "参数 " + key + " 不能为空");
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new BusinessException(400, "参数 " + key + " 不能为空字符串");
        }
        return text;
    }

    private double nonNegative(String key, double value) {
        if (value < 0) {
            throw new BusinessException(400, "参数 " + key + " 不能为负数");
        }
        return value;
    }

    private double ratio(String key, double value) {
        if (value < 0 || value > 1) {
            throw new BusinessException(400, "参数 " + key + " 取值范围为 0.0–1.0");
        }
        return value;
    }

    private int rotateSpeed(String key, double value) {
        if (value < 0 || value > DeviceConstants.MAX_ROTATE_SPEED) {
            throw new BusinessException(400, "参数 " + key + " 取值范围为 0–" + DeviceConstants.MAX_ROTATE_SPEED);
        }
        return (int) Math.round(value);
    }

    private <E extends Enum<E>> E toEnum(Class<E> enumClass, String key, Object value) {
        String text = toStringValue(key, value);
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        throw new BusinessException(400, "参数 " + key + " 取值非法: " + text);
    }

    private BusinessException unsupportedKey(DeviceType type, String key) {
        return new BusinessException(400, "设备类型 " + type.getLabel() + " 不支持参数: " + key);
    }
}
