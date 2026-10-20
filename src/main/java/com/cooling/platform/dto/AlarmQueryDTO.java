package com.cooling.platform.dto;

import com.cooling.platform.common.constant.DeviceConstants;
import com.cooling.platform.common.enums.AlarmLevel;
import com.cooling.platform.common.enums.AlarmStatus;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 告警查询条件（AM-03）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmQueryDTO {

    /** 处理状态。 */
    private AlarmStatus status;

    /** 告警级别。 */
    private AlarmLevel alarmLevel;

    /** 设备编号。 */
    private String deviceId;

    /** 页码。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = DeviceConstants.DEFAULT_PAGE_NUM;

    /** 每页条数。 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = DeviceConstants.MAX_PAGE_SIZE, message = "每页条数不能超过 200")
    private Integer pageSize = DeviceConstants.DEFAULT_PAGE_SIZE;

    /**
     * SQL 偏移量。
     *
     * @return 偏移量
     */
    public int offset() {
        int size = limit();
        return (safePageNum() - 1) * size;
    }

    /**
     * 实际每页条数。
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
     * 实际页码。
     *
     * @return 页码
     */
    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? DeviceConstants.DEFAULT_PAGE_NUM : pageNum;
    }
}
