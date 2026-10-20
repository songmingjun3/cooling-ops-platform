package com.cooling.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警视图对象（AM-03）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmVO {

    /** 主键。 */
    private Long id;

    /** 告警编号。 */
    private String alarmNo;

    /** 设备编号。 */
    private String deviceId;

    /** 设备名称。 */
    private String deviceName;

    /** 参数名。 */
    private String paramName;

    /** 实际值。 */
    private Double actualValue;

    /** 阈值。 */
    private Double thresholdValue;

    /** 级别枚举名。 */
    private String alarmLevel;

    /** 级别中文名。 */
    private String alarmLevelLabel;

    /** 告警描述。 */
    private String message;

    /** 状态枚举名。 */
    private String status;

    /** 状态中文名。 */
    private String statusLabel;

    /** 触发时间。 */
    private LocalDateTime triggerTime;

    /** 处理人。 */
    private String handler;

    /** 处理时间。 */
    private LocalDateTime handleTime;

    /** 处理措施。 */
    private String handleMeasure;
}
