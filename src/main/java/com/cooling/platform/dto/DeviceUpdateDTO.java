package com.cooling.platform.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 修改设备请求参数（FM-03）。
 *
 * <p>仅允许修改基本信息。设备编号、设备类型、运行状态与运行参数不允许通过本接口修改：
 * 编号与类型是设备身份标识，运行状态走启停接口（MM-03/04），运行参数由采集任务写入（MM-05）。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class DeviceUpdateDTO {

    /** 设备名称。 */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 50, message = "设备名称长度不能超过 50 个字符")
    private String deviceName;

    /** 生产厂商。 */
    @Size(max = 50, message = "生产厂商长度不能超过 50 个字符")
    private String manufacturer;

    /** 设备型号。 */
    @Size(max = 50, message = "设备型号长度不能超过 50 个字符")
    private String model;

    /** 安装日期。 */
    @PastOrPresent(message = "安装日期不能晚于今天")
    private LocalDate installDate;

    /** 所属区域。 */
    @NotBlank(message = "所属区域不能为空")
    @Size(max = 20, message = "所属区域长度不能超过 20 个字符")
    private String zone;
}
