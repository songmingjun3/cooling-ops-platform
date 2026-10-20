package com.cooling.platform.scheduler;

import com.cooling.platform.service.MonitorService;
import com.cooling.platform.vo.CollectResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 模拟数据采集定时任务（需求文档 MM-05）。
 *
 * <p>按配置的周期为所有 RUNNING 状态设备生成模拟量测值、写入采集数据与运行日志，
 * 并触发告警检测（AM-02）。单次任务异常只记录日志，不中断后续调度。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@Component
public class DataAcquisitionTask {

    private final MonitorService monitorService;

    /** 是否启用模拟采集。 */
    @Value("${cooling.acquisition.enabled:true}")
    private boolean enabled;

    /** 采集数据保留天数。 */
    @Value("${cooling.acquisition.keep-days:7}")
    private int keepDays;

    /**
     * 构造定时任务。
     *
     * @param monitorService 监控业务接口
     */
    public DataAcquisitionTask(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * 周期采集：默认每 3 秒一次（{@code cooling.acquisition.interval}）。
     */
    @Scheduled(fixedDelayString = "${cooling.acquisition.interval:3000}")
    public void collect() {
        if (!enabled) {
            return;
        }
        try {
            CollectResultVO result = monitorService.collectOnce();
            if (result.getNewAlarms() > 0) {
                log.info("模拟采集完成: 设备 {} 台, 数据点 {} 条, 新告警 {} 条",
                        result.getDeviceCount(), result.getDataPoints(), result.getNewAlarms());
            } else {
                log.debug("模拟采集完成: 设备 {} 台, 数据点 {} 条",
                        result.getDeviceCount(), result.getDataPoints());
            }
        } catch (Exception ex) {
            // 定时任务必须兜底，否则异常会中断后续调度；此处记录完整堆栈便于排查
            log.error("模拟采集任务执行失败", ex);
        }
    }

    /**
     * 每日凌晨清理过期采集数据与运行日志，避免数据库无限膨胀。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanup() {
        try {
            monitorService.cleanupExpired(keepDays);
        } catch (Exception ex) {
            log.error("过期数据清理失败", ex);
        }
    }
}
