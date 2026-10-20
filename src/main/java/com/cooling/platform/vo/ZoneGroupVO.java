package com.cooling.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 按区域分组的设备视图（FM-06）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneGroupVO {

    /** 区域名称，如「A区」。 */
    private String zone;

    /** 该区域设备数量。 */
    private int deviceCount;

    /** 该区域设备列表。 */
    private List<DeviceVO> devices;
}
