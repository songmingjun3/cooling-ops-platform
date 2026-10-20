package com.cooling.platform.rule;

import com.cooling.platform.common.enums.CompareOp;
import com.cooling.platform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 告警规则注册表。
 *
 * <p>启动时把所有 {@link AlarmRule} 实现按比较方式登记，检测流程按需取用；
 * 新增规则只需实现接口并标注 {@code @Component}，此处无需改动。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Component
public class AlarmRuleRegistry {

    /** 比较方式 → 规则实现。 */
    private final Map<CompareOp, AlarmRule> rules = new EnumMap<>(CompareOp.class);

    /**
     * 构造时收集全部规则实现。
     *
     * @param ruleList Spring 注入的全部规则
     */
    public AlarmRuleRegistry(List<AlarmRule> ruleList) {
        for (AlarmRule rule : ruleList) {
            rules.put(rule.op(), rule);
        }
    }

    /**
     * 获取规则实现。
     *
     * @param op 比较方式
     * @return 规则实现
     * @throws BusinessException 未注册时抛出
     */
    public AlarmRule get(CompareOp op) {
        AlarmRule rule = rules.get(op);
        if (rule == null) {
            throw new BusinessException(500, "未注册的告警规则: " + op);
        }
        return rule;
    }

    /**
     * 获取规则实现（按名称解析）。
     *
     * @param opName 比较方式名称
     * @return 规则实现
     */
    public AlarmRule get(String opName) {
        try {
            return get(CompareOp.of(opName));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "未知的比较方式: " + opName);
        }
    }
}
