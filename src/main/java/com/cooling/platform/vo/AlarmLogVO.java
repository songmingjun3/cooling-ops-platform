package com.cooling.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警日志视图（PM-03）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmLogVO {

    /** 主键。 */
    private Long id;

    /** 告警编号。 */
    private String alarmNo;

    /** 动作：TRIGGER / HANDLE。 */
    private String action;

    /** 明细。 */
    private String detail;

    /** 操作人。 */
    private String operator;

    /** 记录时间。 */
    private LocalDateTime logTime;
}
