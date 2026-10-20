package com.cooling.platform.dto;

import com.cooling.platform.common.enums.AlarmLevel;
import com.cooling.platform.common.enums.DeviceType;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 告警阈值新增 / 修改请求（AM-01）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmThresholdDTO {

    /** 适用设备类型。 */
    @NotNull(message = "设备类型不能为空")
    private DeviceType deviceType;

    /** 参数键，须为该设备类型已登记的传感器参数。 */
    @NotBlank(message = "参数键不能为空")
    private String paramKey;

    /** 参数中文名，留空时按传感器目录自动补齐。 */
    @Size(max = 30, message = "参数名长度不能超过 30 个字符")
    private String paramName;

    /** 比较方式：GT / LT / DEVIATION。 */
    @NotBlank(message = "比较方式不能为空")
    private String compareOp;

    /** 阈值；DEVIATION 规则下表示额定值。 */
    @NotNull(message = "阈值不能为空")
    private Double thresholdValue;

    /** 允许波动百分比，仅 DEVIATION 规则使用。 */
    @DecimalMin(value = "0.1", message = "波动百分比必须大于 0")
    @DecimalMax(value = "100", message = "波动百分比不能超过 100")
    private Double deviationPercent;

    /** 单位，留空时按传感器目录自动补齐。 */
    private String unit;

    /** 告警级别。 */
    @NotNull(message = "告警级别不能为空")
    private AlarmLevel alarmLevel;

    /** 是否启用，默认启用。 */
    private Boolean enabled;

    /** 备注。 */
    @Size(max = 100, message = "备注长度不能超过 100 个字符")
    private String remark;
}
