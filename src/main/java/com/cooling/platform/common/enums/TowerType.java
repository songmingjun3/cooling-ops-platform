package com.cooling.platform.common.enums;

import lombok.Getter;

/**
 * 冷却塔类型枚举（需求文档 3.3：开式 / 闭式）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public enum TowerType {

    /** 开式冷却塔。 */
    OPEN("开式"),

    /** 闭式冷却塔。 */
    CLOSED("闭式");

    /** 中文描述。 */
    private final String label;

    TowerType(String label) {
        this.label = label;
    }
}
