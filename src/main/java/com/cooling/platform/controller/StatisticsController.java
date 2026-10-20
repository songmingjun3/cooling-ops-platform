package com.cooling.platform.controller;

import com.cooling.platform.common.response.Result;
import com.cooling.platform.dto.TrendQueryDTO;
import com.cooling.platform.service.StatisticsService;
import com.cooling.platform.vo.StatisticsVO;
import com.cooling.platform.vo.TrendVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 数据分析接口（需求文档 4.5，AN-01 / AN-02）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 构造控制器。
     *
     * @param statisticsService 统计业务接口
     */
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 设备运行统计（AN-01）。
     *
     * @return 统计结果
     */
    @GetMapping("/overview")
    public Result<StatisticsVO> overview() {
        return Result.ok(statisticsService.overview());
    }

    /**
     * 温度趋势简图数据（AN-02）。
     *
     * @param query 查询条件
     * @return 趋势数据
     */
    @GetMapping("/trend")
    public Result<TrendVO> trend(@Valid TrendQueryDTO query) {
        return Result.ok(statisticsService.trend(query));
    }
}
