package com.cooling.platform.common.enums;

import lombok.Getter;

/**
 * 阈值比较方式枚举（AM-01 / AM-02）。
 *
 * <p>每种比较方式对应 {@code rule} 包中的一个策略实现，新增比较方式只需
 * 增加枚举值与对应策略类，既有检测流程无需修改。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public enum CompareOp {

    /** 大于阈值触发。 */
    GT("大于"),

    /** 小于阈值触发。 */
    LT("小于"),

    /** 相对额定值波动超过百分比触发。 */
    DEVIATION("波动超限");

    /** 中文描述。 */
    private final String label;

    CompareOp(String label) {
        this.label = label;
    }

    /**
     * 按名称解析比较方式。
     *
     * @param name 名称，忽略大小写
     * @return 比较方式
     * @throws IllegalArgumentException 名称非法时抛出
     */
    public static CompareOp of(String name) {
        for (CompareOp op : values()) {
            if (op.name().equalsIgnoreCase(name)) {
                return op;
            }
        }
        throw new IllegalArgumentException("未知的比较方式: " + name);
    }
}
