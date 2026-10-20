package com.cooling.platform.common.enums;

import lombok.Getter;

/**
 * 设备类型枚举。
 *
 * <p>{@code prefix} 用于生成设备编号（需求文档 3.1：设备类型前缀-序号，如 CH-001），
 * 新增设备类型时只需在此扩展并使用对应的类与 Mapper，无需修改既有逻辑。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public enum DeviceType {

    /** 冷水机组。 */
    CHILLER("CH", "冷水机组"),

    /** 冷却塔。 */
    COOLING_TOWER("CT", "冷却塔"),

    /** 水泵。 */
    PUMP("PM", "水泵"),

    /** 精密空调。 */
    COOLING_UNIT("CU", "精密空调");

    /** 设备编号前缀。 */
    private final String prefix;

    /** 中文名称。 */
    private final String label;

    DeviceType(String prefix, String label) {
        this.prefix = prefix;
        this.label = label;
    }

    /**
     * 根据设备编号前缀反查设备类型。
     *
     * @param prefix 设备编号前缀，如 {@code CH}
     * @return 匹配的设备类型
     * @throws IllegalArgumentException 前缀不合法时抛出
     */
    public static DeviceType fromPrefix(String prefix) {
        for (DeviceType type : values()) {
            if (type.prefix.equalsIgnoreCase(prefix)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的设备编号前缀: " + prefix);
    }
}
