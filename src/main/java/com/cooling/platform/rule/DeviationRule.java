package com.cooling.platform.rule;

import com.cooling.platform.common.enums.CompareOp;
import com.cooling.platform.entity.AlarmThreshold;
import org.springframework.stereotype.Component;

/**
 * 「相对额定值波动超限」规则。
 *
 * <p>需求文档 4.3 的示例为「水泵功率异常波动 &gt; ±20%」。通用实现为：
 * {@code |实际值 - 额定值| / 额定值 > 允许百分比 / 100}，
 * 其中额定值取阈值配置的 {@code thresholdValue} 字段（因此该规则要求额定值大于 0）。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Component
public class DeviationRule implements AlarmRule {

    /** 默认允许波动百分比。 */
    private static final double DEFAULT_PERCENT = 20.0;

    @Override
    public CompareOp op() {
        return CompareOp.DEVIATION;
    }

    @Override
    public boolean triggered(AlarmThreshold threshold, double actual) {
        double rated = threshold.getThresholdValue() == null ? 0.0 : threshold.getThresholdValue();
        if (rated <= 0) {
            return false;
        }
        double percent = threshold.getDeviationPercent() == null ? DEFAULT_PERCENT : threshold.getDeviationPercent();
        return Math.abs(actual - rated) / rated * 100.0 > percent;
    }

    @Override
    public String expression(AlarmThreshold threshold) {
        double percent = threshold.getDeviationPercent() == null ? DEFAULT_PERCENT : threshold.getDeviationPercent();
        return threshold.getParamName() + " 偏离 " + withUnit(threshold.getThresholdValue(), threshold.getUnit())
                + " 超过 ±" + String.format("%.0f", percent) + "%";
    }
}
