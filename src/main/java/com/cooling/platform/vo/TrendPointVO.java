package com.cooling.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势图数据点（AN-02）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO {

    /** 采集时间，格式 HH:mm:ss。 */
    private String time;

    /** 量测值。 */
    private double value;
}
