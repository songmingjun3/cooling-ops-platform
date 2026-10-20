package com.cooling.platform.controller;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.response.Result;
import com.cooling.platform.service.MonitorService;
import com.cooling.platform.vo.CollectResultVO;
import com.cooling.platform.vo.DeviceDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备运行监控接口（需求文档 4.2，MM-01 ~ MM-05）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    /**
     * 构造控制器。
     *
     * @param monitorService 监控业务接口
     */
    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * 查看设备实时状态（MM-01）。
     *
     * @param deviceId 设备编号
     * @return 设备详情（含最新采集值与采集时间）
     */
    @GetMapping("/devices/{deviceId}")
    public Result<DeviceDetailVO> realtime(@PathVariable String deviceId) {
        return Result.ok(monitorService.realtime(deviceId));
    }

    /**
     * 按设备类型批量查看运行状态（MM-02）。
     *
     * @param deviceType 设备类型
     * @return 设备详情列表
     */
    @GetMapping("/devices")
    public Result<List<DeviceDetailVO>> listByType(@RequestParam DeviceType deviceType) {
        return Result.ok(monitorService.listByType(deviceType));
    }

    /**
     * 启动设备（MM-03）。
     *
     * @param deviceId 设备编号
     * @return 操作结果
     */
    @PostMapping("/devices/{deviceId}/start")
    public Result<Void> start(@PathVariable String deviceId) {
        monitorService.start(deviceId);
        return Result.ok();
    }

    /**
     * 停机设备（MM-04）。
     *
     * @param deviceId 设备编号
     * @return 操作结果
     */
    @PostMapping("/devices/{deviceId}/stop")
    public Result<Void> stop(@PathVariable String deviceId) {
        monitorService.stop(deviceId);
        return Result.ok();
    }

    /**
     * 手动触发一次模拟采集（MM-05），便于演示与测试；正常由定时任务驱动。
     *
     * @return 采集结果
     */
    @PostMapping("/collect")
    public Result<CollectResultVO> collect() {
        return Result.ok(monitorService.collectOnce());
    }
}
