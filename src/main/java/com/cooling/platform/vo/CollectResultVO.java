package com.cooling.platform.vo;

import lombok.Data;

/**
 * 一次模拟采集的执行结果（MM-05）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class CollectResultVO {

    /** 参与采集的设备数。 */
    private int deviceCount;

    /** 写入的数据点数。 */
    private int dataPoints;

    /** 新产生的告警数。 */
    private int newAlarms;

    /** 采集耗时（毫秒）。 */
    private long costMillis;
}
