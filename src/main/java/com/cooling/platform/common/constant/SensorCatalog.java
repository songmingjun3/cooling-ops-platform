package com.cooling.platform.common.constant;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.entity.Sensor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 传感器元数据目录。
 *
 * <p>集中定义每个设备类型下各传感器的「中文名、单位、合理波动区间」，是传感器信息的唯一来源：
 * 数据库只保存量测值本身（{@code params} JSON 中的数值、{@code sensor_data} 表中的 value），
 * 名称与单位在读取时由此目录补齐，避免同一参数在多条记录中重复维护。</p>
 *
 * <p>波动区间参考需求文档 3.3 节的参数说明，并适度放宽以保证模拟采集
 * （MM-05）既能覆盖正常工况、也能演示越界告警。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
public final class SensorCatalog {

    /** 传感器定义：名称、单位、合理波动区间。 */
    public static final class Definition {

        private final String name;
        private final String unit;
        private final double min;
        private final double max;

        Definition(String name, String unit, double min, double max) {
            this.name = name;
            this.unit = unit;
            this.min = min;
            this.max = max;
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }
    }

    /** 传感器定义表，键为「设备类型.参数键」。 */
    private static final Map<String, Definition> DEFINITIONS;

    static {
        Map<String, Definition> map = new HashMap<>(16);
        // 冷水机组：供水温度正常带 12–16℃（需求文档 3.3）
        map.put(key(DeviceType.CHILLER, "chilledWaterSupplyTemp"),
                new Definition("冷冻水供水温度", "℃", 6.5, 17.5));
        map.put(key(DeviceType.CHILLER, "chilledWaterReturnTemp"),
                new Definition("冷冻水回水温度", "℃", 10.0, 21.0));
        // 冷却塔：出水温度正常带 18–34℃
        map.put(key(DeviceType.COOLING_TOWER, "waterFlowRate"),
                new Definition("水流量", "m³/h", 60.0, 200.0));
        map.put(key(DeviceType.COOLING_TOWER, "ambientTemp"),
                new Definition("环境温度", "℃", 15.0, 38.0));
        map.put(key(DeviceType.COOLING_TOWER, "outletTemp"),
                new Definition("出水温度", "℃", 18.0, 36.0));
        // 水泵：流量额定 85 m³/h（见 alarm_threshold 中的波动阈值）
        map.put(key(DeviceType.PUMP, "flowRate"),
                new Definition("当前流量", "m³/h", 60.0, 110.0));
        // 精密空调：送风 18–27℃，回风 28–42℃
        map.put(key(DeviceType.COOLING_UNIT, "supplyTemp"),
                new Definition("送风温度", "℃", 18.0, 27.0));
        map.put(key(DeviceType.COOLING_UNIT, "returnTemp"),
                new Definition("回风温度", "℃", 26.0, 42.0));
        DEFINITIONS = Collections.unmodifiableMap(map);
    }

    private SensorCatalog() {
    }

    private static String key(DeviceType type, String paramKey) {
        return type.name() + "." + paramKey;
    }

    /**
     * 查询传感器定义。
     *
     * @param type     设备类型
     * @param paramKey 参数键
     * @return 定义；未登记时返回 {@code null}
     */
    public static Definition find(DeviceType type, String paramKey) {
        return DEFINITIONS.get(key(type, paramKey));
    }

    /**
     * 判断某参数是否为该设备类型的传感器参数。
     *
     * @param type     设备类型
     * @param paramKey 参数键
     * @return 是传感器参数返回 {@code true}
     */
    public static boolean isSensor(DeviceType type, String paramKey) {
        return DEFINITIONS.containsKey(key(type, paramKey));
    }

    /**
     * 按目录定义构造传感器实例。
     *
     * @param type     设备类型
     * @param paramKey 参数键
     * @param value    量测值
     * @return 传感器实例
     * @throws IllegalArgumentException 参数未在目录中登记时抛出
     */
    public static Sensor create(DeviceType type, String paramKey, double value) {
        Definition def = find(type, paramKey);
        if (def == null) {
            throw new IllegalArgumentException("未登记的传感器参数: " + key(type, paramKey));
        }
        Sensor sensor = new Sensor(paramKey, def.getName(), value, def.getUnit(), def.getMin(), def.getMax());
        return sensor;
    }
}
