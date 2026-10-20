package com.cooling.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运行日志视图（PM-02）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class RunLogVO {

    /** 主键。 */
    private Long id;

    /** 设备编号。 */
    private String deviceId;

    /** 设备类型。 */
    private String deviceType;

    /** 采集时的运行状态。 */
    private String status;

    /** 参数快照 JSON。 */
    private String params;

    /** 采集时间。 */
    private LocalDateTime collectTime;
}
