package com.cooling.platform.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * 设备详情视图对象（MM-01 / 详情页）。
 *
 * <p>在列表字段基础上追加「传感器参数列表」与「静态规格参数」：
 * 前者随采集变化，后者是设备铭牌类信息（制冷量、扬程、制冷剂等），
 * 两者分开便于前端分区展示。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DeviceDetailVO extends DeviceVO {

    /** 运行参数（传感器），随采集任务刷新。 */
    private List<SensorVO> sensors;

    /** 规格参数，属性名 → 带单位的显示值，如「制冷量」→「130.0 kW」。 */
    private Map<String, String> specs;
}
