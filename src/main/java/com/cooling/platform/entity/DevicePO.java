package com.cooling.platform.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备表的持久化对象（PO）。
 *
 * <p>数据库采用单表继承：公共属性为列，子类专有属性存于 {@code params} 列（JSON 文本）。
 * 本类只负责与 {@code device} 表一一对应，不承载业务行为；业务层通过
 * {@code DeviceConverter} 将其还原为 {@link Device} 及其子类。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class DevicePO {

    /** 设备编号，如 CH-001。 */
    private String deviceId;

    /** 设备类型枚举名，如 CHILLER。 */
    private String deviceType;

    /** 设备名称。 */
    private String deviceName;

    /** 生产厂商。 */
    private String manufacturer;

    /** 设备型号。 */
    private String model;

    /** 安装日期。 */
    private LocalDate installDate;

    /** 运行状态枚举名，如 RUNNING。 */
    private String status;

    /** 所属区域。 */
    private String zone;

    /** 子类专有属性 JSON，如 {"coolingCapacity":130.0,...}。 */
    private String params;

    /** 建档时间。 */
    private LocalDateTime createTime;

    /** 最后更新时间。 */
    private LocalDateTime updateTime;
}
