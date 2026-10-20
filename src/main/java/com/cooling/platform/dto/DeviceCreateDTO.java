package com.cooling.platform.dto;

import com.cooling.platform.common.enums.DeviceType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

/**
 * 新增设备请求参数（FM-01）。
 *
 * <p>设备编号由后端自动生成，不接受前端传入，避免编号冲突与格式不一致。
 * 子类专有属性通过 {@code params} 传递，键名与需求文档 3.3 节的属性名一致，
 * 未传的字段由 {@code DeviceFactory} 填充默认值。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class DeviceCreateDTO {

    /** 设备类型。 */
    @NotNull(message = "设备类型不能为空")
    private DeviceType deviceType;

    /** 设备名称，如「1号冷水机组」。 */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 50, message = "设备名称长度不能超过 50 个字符")
    private String deviceName;

    /** 生产厂商。 */
    @Size(max = 50, message = "生产厂商长度不能超过 50 个字符")
    private String manufacturer;

    /** 设备型号。 */
    @Size(max = 50, message = "设备型号长度不能超过 50 个字符")
    private String model;

    /** 安装日期，不允许晚于当天。 */
    @PastOrPresent(message = "安装日期不能晚于今天")
    private LocalDate installDate;

    /** 所属区域，如「A区」。 */
    @NotBlank(message = "所属区域不能为空")
    @Size(max = 20, message = "所属区域长度不能超过 20 个字符")
    private String zone;

    /**
     * 子类专有属性，如制冷量、类型、扬程等。
     * 键名与需求文档 3.3 节属性名一致，传感器类属性直接传数值。
     */
    private Map<String, Object> params;
}
