package com.cooling.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 「名称 - 数量」统计项（AN-01 的按类型 / 按状态 / 按区域维度）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameCountVO {

    /** 分组名称（或枚举名）。 */
    private String name;

    /** 分组中文名。 */
    private String label;

    /** 数量。 */
    private long count;
}
