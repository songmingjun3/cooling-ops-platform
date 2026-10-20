package com.cooling.platform.common.enums;

import lombok.Getter;

/**
 * 设备运行状态枚举（需求文档 3.2）。
 *
 * <p>存储于数据库时直接使用枚举名，保证与需求文档定义一致。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public enum DeviceStatus {

    /** 运行中。 */
    RUNNING("运行中"),

    /** 已停机。 */
    STOPPED("已停机"),

    /** 故障。 */
    FAULT("故障"),

    /** 维护中。 */
    MAINTENANCE("维护中"),

    /** 离线（通讯中断）。 */
    OFFLINE("离线");

    /** 中文描述，用于界面展示。 */
    private final String label;

    DeviceStatus(String label) {
        this.label = label;
    }
}
