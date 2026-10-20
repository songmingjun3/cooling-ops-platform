package com.cooling.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备运行日志（需求文档 PM-02，对应 {@code run_log}）。
 *
 * <p>每个采集周期为运行中的设备写入一条参数快照，用于运行过程追溯。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class RunLog {

    /** 主键。 */
    private Long id;

    /** 设备编号。 */
    private String deviceId;

    /** 设备类型枚举名。 */
    private String deviceType;

    /** 采集时刻的运行状态。 */
    private String status;

    /** 参数快照 JSON。 */
    private String params;

    /** 采集时间。 */
    private LocalDateTime collectTime;
}
