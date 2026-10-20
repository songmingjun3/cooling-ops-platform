package com.cooling.platform.service;

import com.cooling.platform.dto.TrendQueryDTO;
import com.cooling.platform.vo.StatisticsVO;
import com.cooling.platform.vo.TrendVO;

/**
 * 数据分析业务接口（需求文档 4.5，AN-01 / AN-02）。
 *
 * @author 项目组
 * @since 1.0.0
 */
public interface StatisticsService {

    /**
     * 设备运行统计：数量、状态分布、类型分布、区域分布与告警概览（AN-01）。
     *
     * @return 统计结果
     */
    StatisticsVO overview();

    /**
     * 某设备某参数最近 N 次采集的趋势（AN-02）。
     *
     * @param query 查询条件
     * @return 趋势数据
     */
    TrendVO trend(TrendQueryDTO query);
}
