package com.cooling.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警阈值视图对象（AM-01）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmThresholdVO {

    /** 主键。 */
    private Long id;

    /** 设备类型枚举名。 */
    private String deviceType;

    /** 设备类型中文名。 */
    private String deviceTypeLabel;

    /** 参数键。 */
    private String paramKey;

    /** 参数名。 */
    private String paramName;

    /** 比较方式。 */
    private String compareOp;

    /** 比较方式中文描述，如「大于」。 */
    private String compareOpLabel;

    /** 阈值。 */
    private Double thresholdValue;

    /** 波动百分比。 */
    private Double deviationPercent;

    /** 单位。 */
    private String unit;

    /** 告警级别枚举名。 */
    private String alarmLevel;

    /** 告警级别中文名。 */
    private String alarmLevelLabel;

    /** 是否启用。 */
    private Boolean enabled;

    /** 备注。 */
    private String remark;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 规则的中文表达式，如「冷冻水供水温度 > 16.0 ℃」，便于界面直接展示。 */
    private String expression;
}
