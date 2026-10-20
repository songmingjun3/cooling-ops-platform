package com.cooling.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 传感器视图对象（MM-01 设备详情中的运行参数）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class SensorVO {

    /** 参数键。 */
    private String key;

    /** 参数中文名。 */
    private String name;

    /** 当前量测值。 */
    private double value;

    /** 计量单位。 */
    private String unit;

    /** 合理波动下限。 */
    private double minValue;

    /** 合理波动上限。 */
    private double maxValue;

    /** 最近采集时间。 */
    private LocalDateTime collectTime;

    /**
     * 是否超出合理波动区间，供前端标红提示。
     *
     * @return 超限返回 {@code true}
     */
    public boolean isOutOfRange() {
        return value < minValue || value > maxValue;
    }
}
