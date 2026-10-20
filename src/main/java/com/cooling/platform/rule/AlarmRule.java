package com.cooling.platform.rule;

import com.cooling.platform.common.enums.CompareOp;
import com.cooling.platform.entity.AlarmThreshold;

/**
 * 告警判定规则（策略模式）。
 *
 * <p>采集到新数据后，由检测流程遍历已启用的阈值，交由对应的规则实现判断是否触发，
 * 从而把「怎么判」与「何时判、判完做什么」解耦，满足需求文档 5.3 的可扩展性要求。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
public interface AlarmRule {

    /**
     * 本规则支持的比较方式。
     *
     * @return 比较方式
     */
    CompareOp op();

    /**
     * 判断是否触发告警。
     *
     * @param threshold 阈值配置
     * @param actual    实际量测值
     * @return 触发返回 {@code true}
     */
    boolean triggered(AlarmThreshold threshold, double actual);

    /**
     * 生成规则的中文表达式，用于界面展示与告警描述。
     *
     * @param threshold 阈值配置
     * @return 表达式，如「冷冻水供水温度 &gt; 16.0 ℃」
     */
    String expression(AlarmThreshold threshold);

    /**
     * 拼接带单位的值，避免各处重复判空。
     *
     * @param value 数值
     * @param unit  单位，可为空
     * @return 如「16.0 ℃」
     */
    default String withUnit(Double value, String unit) {
        String text = value == null ? "-" : String.format("%.1f", value);
        return unit == null || unit.trim().isEmpty() ? text : text + " " + unit;
    }
}
