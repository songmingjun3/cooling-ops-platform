package com.cooling.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * 温度趋势视图（AN-02）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class TrendVO {

    /** 设备编号。 */
    private String deviceId;

    /** 设备名称。 */
    private String deviceName;

    /** 参数键。 */
    private String sensorKey;

    /** 参数中文名。 */
    private String sensorName;

    /** 单位。 */
    private String unit;

    /** 合理区间下限。 */
    private Double minValue;

    /** 合理区间上限。 */
    private Double maxValue;

    /** 数据点（按时间正序）。 */
    private List<TrendPointVO> points;
}
