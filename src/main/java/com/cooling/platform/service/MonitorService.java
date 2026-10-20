package com.cooling.platform.service;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.vo.CollectResultVO;
import com.cooling.platform.vo.DeviceDetailVO;

import java.util.List;

/**
 * 设备运行监控业务接口（需求文档 4.2，MM-01 ~ MM-05）。
 *
 * @author 项目组
 * @since 1.0.0
 */
public interface MonitorService {

    /**
     * 查看设备实时状态：以最新一次采集的数据为准，未采集时回退到建档参数（MM-01）。
     *
     * @param deviceId 设备编号
     * @return 设备详情
     */
    DeviceDetailVO realtime(String deviceId);

    /**
     * 按设备类型批量查看运行状态（MM-02）。
     *
     * @param deviceType 设备类型
     * @return 设备详情列表
     */
    List<DeviceDetailVO> listByType(DeviceType deviceType);

    /**
     * 启动设备：STOPPED → RUNNING（MM-03）。
     *
     * @param deviceId 设备编号
     */
    void start(String deviceId);

    /**
     * 停机设备：RUNNING / FAULT → STOPPED（MM-04）。
     *
     * @param deviceId 设备编号
     */
    void stop(String deviceId);

    /**
     * 执行一次模拟数据采集：为所有 RUNNING 设备生成参数并检测告警（MM-05）。
     *
     * @return 采集结果
     */
    CollectResultVO collectOnce();

    /**
     * 清理过期的采集数据与运行日志。
     *
     * @param keepDays 保留天数
     * @return 清理的记录数
     */
    int cleanupExpired(int keepDays);
}
