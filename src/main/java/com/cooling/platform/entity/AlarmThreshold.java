package com.cooling.platform.entity;

import com.cooling.platform.common.enums.AlarmLevel;
import com.cooling.platform.common.enums.DeviceType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警阈值配置（需求文档 AM-01，对应 {@code alarm_threshold}）。
 *
 * <p>阈值不硬编码在代码中，运维人员可按设备类型与参数自由配置，
 * 满足需求文档 5.3「告警规则应支持灵活配置」。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmThreshold {

    /** 主键。 */
    private Long id;

    /** 适用设备类型。 */
    private DeviceType deviceType;

    /** 参数键。 */
    private String paramKey;

    /** 参数中文名。 */
    private String paramName;

    /** 比较方式：GT 大于 / LT 小于 / DEVIATION 相对额定值波动。 */
    private String compareOp;

    /** 阈值（DEVIATION 规则下为额定值）。 */
    private Double thresholdValue;

    /** 允许波动百分比（仅 DEVIATION 规则使用）。 */
    private Double deviationPercent;

    /** 单位。 */
    private String unit;

    /** 触发后的告警级别。 */
    private AlarmLevel alarmLevel;

    /** 是否启用。 */
    private Boolean enabled;

    /** 备注。 */
    private String remark;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
