package com.cooling.platform.dto;

import com.cooling.platform.common.constant.DeviceConstants;
import com.cooling.platform.common.enums.DeviceStatus;
import com.cooling.platform.common.enums.DeviceType;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 设备查询请求参数（FM-04 / FM-05）。
 *
 * <p>各条件之间为「与」关系，全部为空表示查询全部设备。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class DeviceQueryDTO {

    /** 设备编号精确匹配。 */
    private String deviceId;

    /** 关键字，模糊匹配设备编号或名称。 */
    private String keyword;

    /** 设备类型。 */
    private DeviceType deviceType;

    /** 所属区域。 */
    private String zone;

    /** 运行状态。 */
    private DeviceStatus status;

    /** 页码，从 1 开始。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = DeviceConstants.DEFAULT_PAGE_NUM;

    /** 每页条数。 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = DeviceConstants.MAX_PAGE_SIZE, message = "每页条数不能超过 200")
    private Integer pageSize = DeviceConstants.DEFAULT_PAGE_SIZE;

    /**
     * 计算 SQL 偏移量。
     *
     * @return 偏移量
     */
    public int offset() {
        int num = pageNum == null || pageNum < 1 ? DeviceConstants.DEFAULT_PAGE_NUM : pageNum;
        int size = limit();
        return (num - 1) * size;
    }

    /**
     * 计算实际每页条数（防御非法值与超大值）。
     *
     * @return 每页条数
     */
    public int limit() {
        int size = pageSize == null ? DeviceConstants.DEFAULT_PAGE_SIZE : pageSize;
        if (size < 1) {
            return DeviceConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, DeviceConstants.MAX_PAGE_SIZE);
    }

    /**
     * 实际页码（防御非法值）。
     *
     * @return 页码
     */
    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? DeviceConstants.DEFAULT_PAGE_NUM : pageNum;
    }
}
