package com.cooling.platform.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 温度趋势查询条件（AN-02）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class TrendQueryDTO {

    /** 设备编号。 */
    @NotBlank(message = "设备编号不能为空")
    private String deviceId;

    /** 参数键，留空时取该设备的第一个传感器（通常是温度类）。 */
    private String sensorKey;

    /** 采集点数，默认 30，最多 200。 */
    @Min(value = 1, message = "采集点数不能小于 1")
    @Max(value = 200, message = "采集点数不能超过 200")
    private Integer limit = 30;
}
