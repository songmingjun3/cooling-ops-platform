package com.cooling.platform.rule;

import com.cooling.platform.common.enums.CompareOp;
import com.cooling.platform.entity.AlarmThreshold;
import org.springframework.stereotype.Component;

/**
 * 「小于阈值」规则：参数低于下限时触发（如冷却塔水流量 &lt; 80 m³/h）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Component
public class LessThanRule implements AlarmRule {

    @Override
    public CompareOp op() {
        return CompareOp.LT;
    }

    @Override
    public boolean triggered(AlarmThreshold threshold, double actual) {
        return actual < threshold.getThresholdValue();
    }

    @Override
    public String expression(AlarmThreshold threshold) {
        return threshold.getParamName() + " < " + withUnit(threshold.getThresholdValue(), threshold.getUnit());
    }
}
