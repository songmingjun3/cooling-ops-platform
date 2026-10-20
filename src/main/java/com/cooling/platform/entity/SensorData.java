package com.cooling.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 传感器采集数据（时序长表，对应 {@code sensor_data}）。
 *
 * <p>每台设备每个参数每次采集写一行，用于趋势查询（AN-02）与历史回溯。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class SensorData {

    /** 主键。 */
    private Long id;

    /** 设备编号。 */
    private String deviceId;

    /** 参数键。 */
    private String sensorKey;

    /** 参数中文名。 */
    private String sensorName;

    /** 量测值。 */
    private Double value;

    /** 计量单位。 */
    private String unit;

    /** 采集时间。 */
    private LocalDateTime collectTime;
}
