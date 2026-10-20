package com.cooling.platform.controller;

import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.common.response.Result;
import com.cooling.platform.service.LogService;
import com.cooling.platform.vo.AlarmLogVO;
import com.cooling.platform.vo.RunLogVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志查询接口（需求文档 PM-02 / PM-03）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    /**
     * 构造控制器。
     *
     * @param logService 日志业务接口
     */
    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * 查询设备运行日志。
     *
     * @param deviceId 设备编号，可为空
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/run")
    public Result<PageResult<RunLogVO>> runLogs(@RequestParam(required = false) String deviceId,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(logService.runLogs(deviceId, pageNum, pageSize));
    }

    /**
     * 查询告警流水日志。
     *
     * @param alarmNo  告警编号，可为空
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/alarm")
    public Result<PageResult<AlarmLogVO>> alarmLogs(@RequestParam(required = false) String alarmNo,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(logService.alarmLogs(alarmNo, pageNum, pageSize));
    }
}
