package com.cooling.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警流水日志（需求文档 PM-03，对应 {@code alarm_log}）。
 *
 * <p>只追加、不修改：告警触发与告警处理各写一条，形成完整处置链路。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmLog {

    /** 主键。 */
    private Long id;

    /** 关联告警编号。 */
    private String alarmNo;

    /** 动作：TRIGGER 触发 / HANDLE 处理。 */
    private String action;

    /** 明细描述。 */
    private String detail;

    /** 操作人。 */
    private String operator;

    /** 记录时间。 */
    private LocalDateTime logTime;
}
