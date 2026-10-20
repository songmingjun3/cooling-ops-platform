package com.cooling.platform.service.impl;

import com.cooling.platform.common.constant.DeviceConstants;
import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.entity.AlarmLog;
import com.cooling.platform.entity.RunLog;
import com.cooling.platform.mapper.AlarmLogMapper;
import com.cooling.platform.mapper.RunLogMapper;
import com.cooling.platform.service.LogService;
import com.cooling.platform.vo.AlarmLogVO;
import com.cooling.platform.vo.RunLogVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志查询业务实现。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Service
public class LogServiceImpl implements LogService {

    private final RunLogMapper runLogMapper;

    private final AlarmLogMapper alarmLogMapper;

    /**
     * 构造业务实现。
     *
     * @param runLogMapper   运行日志数据访问接口
     * @param alarmLogMapper 告警日志数据访问接口
     */
    public LogServiceImpl(RunLogMapper runLogMapper, AlarmLogMapper alarmLogMapper) {
        this.runLogMapper = runLogMapper;
        this.alarmLogMapper = alarmLogMapper;
    }

    @Override
    public PageResult<RunLogVO> runLogs(String deviceId, Integer pageNum, Integer pageSize) {
        int num = normalize(pageNum, DeviceConstants.DEFAULT_PAGE_NUM);
        int size = normalize(pageSize, DeviceConstants.DEFAULT_PAGE_SIZE);
        long total = runLogMapper.count(deviceId);
        List<RunLogVO> list = new ArrayList<>();
        if (total > 0) {
            for (RunLog log : runLogMapper.selectPage(deviceId, (num - 1) * size, size)) {
                RunLogVO vo = new RunLogVO();
                vo.setId(log.getId());
                vo.setDeviceId(log.getDeviceId());
                vo.setDeviceType(log.getDeviceType());
                vo.setStatus(log.getStatus());
                vo.setParams(log.getParams());
                vo.setCollectTime(log.getCollectTime());
                list.add(vo);
            }
        }
        return PageResult.of(list, total, num, size);
    }

    @Override
    public PageResult<AlarmLogVO> alarmLogs(String alarmNo, Integer pageNum, Integer pageSize) {
        int num = normalize(pageNum, DeviceConstants.DEFAULT_PAGE_NUM);
        int size = normalize(pageSize, DeviceConstants.DEFAULT_PAGE_SIZE);
        long total = alarmLogMapper.count(alarmNo);
        List<AlarmLogVO> list = new ArrayList<>();
        if (total > 0) {
            for (AlarmLog log : alarmLogMapper.selectPage(alarmNo, (num - 1) * size, size)) {
                AlarmLogVO vo = new AlarmLogVO();
                vo.setId(log.getId());
                vo.setAlarmNo(log.getAlarmNo());
                vo.setAction(log.getAction());
                vo.setDetail(log.getDetail());
                vo.setOperator(log.getOperator());
                vo.setLogTime(log.getLogTime());
                list.add(vo);
            }
        }
        return PageResult.of(list, total, num, size);
    }

    private int normalize(Integer value, int defaultValue) {
        if (value == null || value < 1) {
            return defaultValue;
        }
        return Math.min(value, DeviceConstants.MAX_PAGE_SIZE);
    }
}
