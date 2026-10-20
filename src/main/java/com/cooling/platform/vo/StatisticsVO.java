package com.cooling.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * 设备运行统计视图（AN-01）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class StatisticsVO {

    /** 设备总数。 */
    private long total;

    /** 运行中数量。 */
    private long running;

    /** 停机数量。 */
    private long stopped;

    /** 故障数量。 */
    private long fault;

    /** 维护中数量。 */
    private long maintenance;

    /** 离线数量。 */
    private long offline;

    /** 未处理告警数。 */
    private long unhandledAlarms;

    /** 告警总数。 */
    private long totalAlarms;

    /** 按设备类型分布。 */
    private List<NameCountVO> byType;

    /** 按运行状态分布。 */
    private List<NameCountVO> byStatus;

    /** 按区域分布。 */
    private List<NameCountVO> byZone;
}
