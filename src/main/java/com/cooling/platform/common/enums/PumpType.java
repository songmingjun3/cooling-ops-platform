package com.cooling.platform.common.enums;

import lombok.Getter;

/**
 * 水泵类型枚举（需求文档 3.3：冷冻水泵 / 冷却水泵）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public enum PumpType {

    /** 冷冻水泵。 */
    CHILLED_WATER("冷冻水泵"),

    /** 冷却水泵。 */
    COOLING_WATER("冷却水泵");

    /** 中文描述。 */
    private final String label;

    PumpType(String label) {
        this.label = label;
    }
}
