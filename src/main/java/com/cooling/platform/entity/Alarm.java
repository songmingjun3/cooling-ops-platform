package com.cooling.platform.entity;

import com.cooling.platform.common.enums.AlarmLevel;
import com.cooling.platform.common.enums.AlarmStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警记录（需求文档 AM-02 ~ AM-05，对应 {@code alarm}）。
 *
 * <p>保存触发时的设备名与参数名快照，即使设备后续被删除，历史告警仍可追溯。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class Alarm {

    /** 主键。 */
    private Long id;

    /** 业务告警编号，如 AL-20261020-0001。 */
    private String alarmNo;

    /** 设备编号。 */
    private String deviceId;

    /** 设备名称快照。 */
    private String deviceName;

    /** 触发参数键。 */
    private String paramKey;

    /** 触发参数名快照。 */
    private String paramName;

    /** 触发时的实际值。 */
    private Double actualValue;

    /** 触发阈值（DEVIATION 规则时为额定值）。 */
    private Double thresholdValue;

    /** 告警级别。 */
    private AlarmLevel alarmLevel;

    /** 告警描述。 */
    private String message;

    /** 处理状态。 */
    private AlarmStatus status;

    /** 触发时间。 */
    private LocalDateTime triggerTime;

    /** 处理人。 */
    private String handler;

    /** 处理时间。 */
    private LocalDateTime handleTime;

    /** 处理措施。 */
    private String handleMeasure;
}
