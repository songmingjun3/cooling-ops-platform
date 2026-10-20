package com.cooling.platform.rule;

import com.cooling.platform.common.enums.AlarmLevel;
import com.cooling.platform.common.enums.CompareOp;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.entity.AlarmThreshold;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 告警规则策略的单元测试（AM-02 判定逻辑、AM-05 分级无关性）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@DisplayName("告警规则策略")
class AlarmRuleTest {

    private static AlarmThreshold threshold(CompareOp op, Double value, Double percent) {
        AlarmThreshold threshold = new AlarmThreshold();
        threshold.setDeviceType(DeviceType.CHILLER);
        threshold.setParamKey("chilledWaterSupplyTemp");
        threshold.setParamName("冷冻水供水温度");
        threshold.setCompareOp(op.name());
        threshold.setThresholdValue(value);
        threshold.setDeviationPercent(percent);
        threshold.setUnit("℃");
        threshold.setAlarmLevel(AlarmLevel.WARNING);
        return threshold;
    }

    @Nested
    @DisplayName("大于阈值")
    class GreaterThan {

        private final GreaterThanRule rule = new GreaterThanRule();

        @Test
        @DisplayName("等于阈值不触发，超过才触发")
        void shouldTriggerOnlyWhenStrictlyGreater() {
            AlarmThreshold t = threshold(CompareOp.GT, 16.0, null);
            assertFalse(rule.triggered(t, 16.0), "等于阈值属于正常带边界，不应告警");
            assertFalse(rule.triggered(t, 15.9));
            assertTrue(rule.triggered(t, 16.1));
            assertTrue(rule.triggered(t, 99.0));
        }

        @Test
        @DisplayName("表达式包含参数名、阈值与单位")
        void shouldRenderExpression() {
            assertEquals("冷冻水供水温度 > 16.0 ℃", rule.expression(threshold(CompareOp.GT, 16.0, null)));
        }

        @Test
        @DisplayName("单位为空时表达式不带单位")
        void shouldRenderExpressionWithoutUnit() {
            AlarmThreshold t = threshold(CompareOp.GT, 60.0, null);
            t.setUnit(null);
            assertEquals("冷冻水供水温度 > 60.0", rule.expression(t));
        }
    }

    @Nested
    @DisplayName("小于阈值")
    class LessThan {

        private final LessThanRule rule = new LessThanRule();

        @Test
        @DisplayName("等于阈值不触发，低于才触发")
        void shouldTriggerOnlyWhenStrictlyLess() {
            AlarmThreshold t = threshold(CompareOp.LT, 6.5, null);
            assertFalse(rule.triggered(t, 6.5));
            assertTrue(rule.triggered(t, 6.4));
            assertFalse(rule.triggered(t, 20.0));
        }

        @Test
        @DisplayName("表达式方向正确")
        void shouldRenderExpression() {
            assertEquals("冷冻水供水温度 < 6.5 ℃", rule.expression(threshold(CompareOp.LT, 6.5, null)));
        }
    }

    @Nested
    @DisplayName("偏离额定值")
    class Deviation {

        private final DeviationRule rule = new DeviationRule();

        @Test
        @DisplayName("相对偏差严格大于百分比才触发")
        void shouldTriggerWhenDeviationExceeded() {
            AlarmThreshold t = threshold(CompareOp.DEVIATION, 100.0, 20.0);
            assertFalse(rule.triggered(t, 100.0), "额定值本身不应告警");
            assertFalse(rule.triggered(t, 120.0), "恰好 20% 属于边界，不触发");
            assertTrue(rule.triggered(t, 120.1));
            assertTrue(rule.triggered(t, 79.9), "低于额定值同样按绝对值判定");
        }

        @Test
        @DisplayName("额定值非正数时不触发，避免除零")
        void shouldNotTriggerWhenRatedValueIsNotPositive() {
            assertFalse(rule.triggered(threshold(CompareOp.DEVIATION, 0.0, 20.0), 50.0));
            assertFalse(rule.triggered(threshold(CompareOp.DEVIATION, -5.0, 20.0), 50.0));
            assertFalse(rule.triggered(threshold(CompareOp.DEVIATION, null, 20.0), 50.0));
        }

        @Test
        @DisplayName("未配置波动百分比时按默认 20% 判定")
        void shouldFallbackToDefaultPercent() {
            AlarmThreshold t = threshold(CompareOp.DEVIATION, 100.0, null);
            assertFalse(rule.triggered(t, 119.0));
            assertTrue(rule.triggered(t, 121.0));
        }

        @Test
        @DisplayName("表达式包含允许波动范围")
        void shouldRenderExpression() {
            assertEquals("冷冻水供水温度 偏离 100.0 ℃ 超过 ±20%",
                    rule.expression(threshold(CompareOp.DEVIATION, 100.0, 20.0)));
        }
    }

    @Nested
    @DisplayName("规则注册表")
    class Registry {

        @Test
        @DisplayName("按枚举与名称均可取到规则")
        void shouldLookupByEnumAndName() {
            AlarmRuleRegistry registry = new AlarmRuleRegistry(Arrays.asList(
                    new GreaterThanRule(), new LessThanRule(), new DeviationRule()));

            assertEquals(CompareOp.GT, registry.get(CompareOp.GT).op());
            assertEquals(CompareOp.LT, registry.get("LT").op());
            assertEquals(CompareOp.DEVIATION, registry.get("deviation").op());
        }

        @Test
        @DisplayName("未知比较方式抛出业务异常")
        void shouldRejectUnknownOp() {
            AlarmRuleRegistry registry = new AlarmRuleRegistry(Arrays.asList(new GreaterThanRule()));
            BusinessException ex = assertThrows(BusinessException.class, () -> registry.get("BETWEEN"));
            assertEquals(400, ex.getCode());
            assertTrue(ex.getMessage().contains("未知的比较方式"));
        }

        @Test
        @DisplayName("已识别但未注册的规则抛出 500")
        void shouldReportMissingRegistration() {
            AlarmRuleRegistry registry = new AlarmRuleRegistry(Arrays.asList(new GreaterThanRule()));
            BusinessException ex = assertThrows(BusinessException.class, () -> registry.get(CompareOp.LT));
            assertEquals(500, ex.getCode());
        }
    }
}
