package com.cooling.platform.common.enums;

import lombok.Getter;

/**
 * 告警处理状态枚举（需求文档 AM-03 / AM-04）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public enum AlarmStatus {

    /** 未处理。 */
    UNHANDLED("未处理"),

    /** 已处理。 */
    HANDLED("已处理");

    /** 中文描述。 */
    private final String label;

    AlarmStatus(String label) {
        this.label = label;
    }
}
