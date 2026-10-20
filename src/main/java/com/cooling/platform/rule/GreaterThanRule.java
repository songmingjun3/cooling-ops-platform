package com.cooling.platform.rule;

import com.cooling.platform.common.enums.CompareOp;
import com.cooling.platform.entity.AlarmThreshold;
import org.springframework.stereotype.Component;

/**
 * 「大于阈值」规则：参数高于上限时触发（如冷冻水供水温度 &gt; 16℃）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Component
public class GreaterThanRule implements AlarmRule {

    @Override
    public CompareOp op() {
        return CompareOp.GT;
    }

    @Override
    public boolean triggered(AlarmThreshold threshold, double actual) {
        return actual > threshold.getThresholdValue();
    }

    @Override
    public String expression(AlarmThreshold threshold) {
        return threshold.getParamName() + " > " + withUnit(threshold.getThresholdValue(), threshold.getUnit());
    }
}
