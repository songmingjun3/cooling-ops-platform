package com.cooling.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 传感器（需求文档 3.3 中标注为 {@code Sensor} 的属性类型）。
 *
 * <p>一个传感器包含量测值、单位以及合理波动区间。合理波动区间用于模拟数据采集（MM-05）：
 * 每次采集在 [{@code minValue}, {@code maxValue}] 之间随机波动，从而既能模拟真实运行，
 * 又能在需要时通过超阈值配置触发告警。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {

    /** 参数键，与数据库 {@code sensor_data.sensor_key}、阈值配置 {@code alarm_threshold.param_key} 对应。 */
    private String key;

    /** 参数中文名，用于界面展示。 */
    private String name;

    /** 当前量测值。 */
    private double value;

    /** 计量单位，如 ℃、m³/h。 */
    private String unit;

    /** 合理波动下限（模拟采集用）。 */
    private double minValue;

    /** 合理波动上限（模拟采集用）。 */
    private double maxValue;

    /** 最近一次采集时间。 */
    private LocalDateTime collectTime;

    /**
     * 构造一个不含采集时间的传感器。
     *
     * @param key      参数键
     * @param name     参数中文名
     * @param value    当前值
     * @param unit     单位
     * @param minValue 合理下限
     * @param maxValue 合理上限
     */
    public Sensor(String key, String name, double value, String unit, double minValue, double maxValue) {
        this(key, name, value, unit, minValue, maxValue, null);
    }

    /**
     * 判断当前值是否超出合理波动区间。
     *
     * @return 超出区间返回 {@code true}
     */
    public boolean isOutOfRange() {
        return value < minValue || value > maxValue;
    }
}
