package com.cooling.platform.common.enums;

import lombok.Getter;

/**
 * 告警级别枚举（需求文档 AM-05）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public enum AlarmLevel {

    /** 警告：参数轻微越界，需关注。 */
    WARNING("警告", 1),

    /** 严重：参数严重越界，需立即处理。 */
    CRITICAL("严重", 2);

    /** 中文描述。 */
    private final String label;

    /** 严重程度权重，数值越大越严重。 */
    private final int severity;

    AlarmLevel(String label, int severity) {
        this.label = label;
        this.severity = severity;
    }
}
