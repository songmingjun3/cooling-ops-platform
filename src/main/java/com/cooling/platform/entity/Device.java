package com.cooling.platform.entity;

import com.cooling.platform.common.enums.DeviceStatus;
import com.cooling.platform.common.enums.DeviceType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 设备抽象基类（需求文档 3.1）。
 *
 * <p>承载所有设备的公共属性，并通过抽象方法把「类型标识、传感器集合、关键参数摘要」
 * 交给子类实现，体现面向对象的抽象与多态；新增设备类型只需继承本类，
 * 既有业务代码（设备管理、采集、告警）无需修改，符合开闭原则。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public abstract class Device {

    /** 设备编号（唯一标识，格式：设备类型前缀-序号，如 CH-001）。 */
    private String deviceId;

    /** 设备名称，如「1号冷水机组」。 */
    private String deviceName;

    /** 生产厂商。 */
    private String manufacturer;

    /** 设备型号。 */
    private String model;

    /** 安装日期。 */
    private LocalDate installDate;

    /** 运行状态。 */
    private DeviceStatus status;

    /** 所属区域，如「A区」。 */
    private String zone;

    /** 建档时间（由持久层维护，业务代码只读）。 */
    private LocalDateTime createTime;

    /** 最后更新时间（由持久层维护，业务代码只读）。 */
    private LocalDateTime updateTime;

    /**
     * 返回设备类型，用于持久化区分与列表过滤。
     *
     * @return 设备类型枚举
     */
    public abstract DeviceType getDeviceType();

    /**
     * 返回该设备参与采集与告警的全部传感器。
     *
     * @return 传感器列表，不可为 {@code null}
     */
    public abstract List<Sensor> getSensors();

    /**
     * 返回一句话关键参数摘要，用于设备清单列表展示。
     *
     * @return 如「供温:7.2℃ 负载:65%」
     */
    public abstract String getKeyParameterSummary();

    /**
     * 判断设备是否处于运行态（仅运行态参与模拟数据采集）。
     *
     * @return 运行中返回 {@code true}
     */
    public boolean isRunning() {
        return DeviceStatus.RUNNING == this.status;
    }

    /**
     * 安全获取传感器列表：子类未初始化时返回空集合，避免调用方判空。
     *
     * @param sensors 子类返回的传感器列表
     * @return 非 {@code null} 的列表
     */
    protected static List<Sensor> nullSafe(List<Sensor> sensors) {
        return sensors == null ? Collections.emptyList() : sensors;
    }
}
