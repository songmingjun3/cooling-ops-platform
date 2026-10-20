package com.cooling.platform.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备列表项视图对象（FM-05）。
 *
 * <p>只暴露列表展示所需字段，不包含子类专有参数详情，避免列表接口返回过大数据量。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class DeviceVO {

    /** 设备编号。 */
    private String deviceId;

    /** 设备名称。 */
    private String deviceName;

    /** 设备类型枚举名，如 CHILLER。 */
    private String deviceType;

    /** 设备类型中文名，如「冷水机组」。 */
    private String deviceTypeLabel;

    /** 生产厂商。 */
    private String manufacturer;

    /** 设备型号。 */
    private String model;

    /** 安装日期。 */
    private LocalDate installDate;

    /** 运行状态枚举名，如 RUNNING。 */
    private String status;

    /** 运行状态中文名，如「运行中」。 */
    private String statusLabel;

    /** 所属区域。 */
    private String zone;

    /** 关键参数摘要，如「供温:7.2℃ 负载:65%」。 */
    private String keyParameterSummary;

    /** 建档时间。 */
    private LocalDateTime createTime;

    /** 最后更新时间。 */
    private LocalDateTime updateTime;
}
