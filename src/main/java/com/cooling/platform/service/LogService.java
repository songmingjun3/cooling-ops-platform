package com.cooling.platform.service;

import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.vo.AlarmLogVO;
import com.cooling.platform.vo.RunLogVO;

/**
 * 日志查询业务接口（需求文档 PM-02 / PM-03）。
 *
 * @author 项目组
 * @since 1.0.0
 */
public interface LogService {

    /**
     * 分页查询设备运行日志。
     *
     * @param deviceId 设备编号，可为空表示全部
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<RunLogVO> runLogs(String deviceId, Integer pageNum, Integer pageSize);

    /**
     * 分页查询告警流水日志。
     *
     * @param alarmNo  告警编号，可为空表示全部
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<AlarmLogVO> alarmLogs(String alarmNo, Integer pageNum, Integer pageSize);
}
