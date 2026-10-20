package com.cooling.platform.service.impl;

import com.cooling.platform.common.constant.SensorCatalog;
import com.cooling.platform.common.enums.AlarmLevel;
import com.cooling.platform.common.enums.AlarmStatus;
import com.cooling.platform.common.enums.CompareOp;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.dto.AlarmHandleDTO;
import com.cooling.platform.dto.AlarmQueryDTO;
import com.cooling.platform.dto.AlarmThresholdDTO;
import com.cooling.platform.entity.Alarm;
import com.cooling.platform.entity.AlarmLog;
import com.cooling.platform.entity.AlarmThreshold;
import com.cooling.platform.entity.Device;
import com.cooling.platform.entity.Sensor;
import com.cooling.platform.mapper.AlarmLogMapper;
import com.cooling.platform.mapper.AlarmMapper;
import com.cooling.platform.mapper.AlarmThresholdMapper;
import com.cooling.platform.rule.AlarmRule;
import com.cooling.platform.rule.AlarmRuleRegistry;
import com.cooling.platform.service.AlarmService;
import com.cooling.platform.vo.AlarmThresholdVO;
import com.cooling.platform.vo.AlarmVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警管理业务实现。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@Service
public class AlarmServiceImpl implements AlarmService {

    /** 告警编号日期部分格式。 */
    private static final DateTimeFormatter ALARM_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 告警抑制窗口（分钟）：同一设备同一参数在窗口内不重复告警。 */
    private static final int SUPPRESS_MINUTES = 5;

    private final AlarmMapper alarmMapper;

    private final AlarmThresholdMapper thresholdMapper;

    private final AlarmLogMapper alarmLogMapper;

    private final AlarmRuleRegistry ruleRegistry;

    /**
     * 构造业务实现。
     *
     * @param alarmMapper     告警数据访问接口
     * @param thresholdMapper 阈值数据访问接口
     * @param alarmLogMapper  告警日志数据访问接口
     * @param ruleRegistry    告警规则注册表
     */
    public AlarmServiceImpl(AlarmMapper alarmMapper,
                            AlarmThresholdMapper thresholdMapper,
                            AlarmLogMapper alarmLogMapper,
                            AlarmRuleRegistry ruleRegistry) {
        this.alarmMapper = alarmMapper;
        this.thresholdMapper = thresholdMapper;
        this.alarmLogMapper = alarmLogMapper;
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public PageResult<AlarmVO> query(AlarmQueryDTO query) {
        AlarmQueryDTO condition = query == null ? new AlarmQueryDTO() : query;
        long total = alarmMapper.countByCondition(condition);
        List<AlarmVO> list = new ArrayList<>();
        if (total > 0) {
            for (Alarm alarm : alarmMapper.selectByCondition(condition, condition.offset(), condition.limit())) {
                list.add(toVO(alarm));
            }
        }
        return PageResult.of(list, total, condition.safePageNum(), condition.limit());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, AlarmHandleDTO dto) {
        Alarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(404, "告警不存在: " + id);
        }
        if (AlarmStatus.HANDLED == alarm.getStatus()) {
            throw new BusinessException(400, "该告警已处理，请勿重复操作: " + alarm.getAlarmNo());
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = alarmMapper.updateHandle(id, dto.getHandler().trim(), now, dto.getMeasure().trim());
        if (rows == 0) {
            throw new BusinessException(400, "告警状态已变更，请刷新后重试");
        }

        AlarmLog logEntry = new AlarmLog();
        logEntry.setAlarmNo(alarm.getAlarmNo());
        logEntry.setAction("HANDLE");
        logEntry.setDetail("处理措施：" + dto.getMeasure().trim());
        logEntry.setOperator(dto.getHandler().trim());
        logEntry.setLogTime(now);
        alarmLogMapper.insert(logEntry);

        log.info("告警处理完成: alarmNo={}, handler={}", alarm.getAlarmNo(), dto.getHandler());
    }

    @Override
    public List<AlarmThresholdVO> listThresholds(DeviceType deviceType) {
        List<AlarmThresholdVO> result = new ArrayList<>();
        for (AlarmThreshold threshold : thresholdMapper.selectList(deviceType)) {
            result.add(toThresholdVO(threshold));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createThreshold(AlarmThresholdDTO dto) {
        AlarmThreshold threshold = buildThreshold(dto);
        if (thresholdMapper.countDuplicate(dto.getDeviceType(), threshold.getParamKey(),
                threshold.getCompareOp(), null) > 0) {
            throw new BusinessException(400, "该设备类型的此参数已存在相同比较方式的阈值");
        }
        thresholdMapper.insert(threshold);
        log.info("新增告警阈值: type={}, param={}, op={}, value={}",
                threshold.getDeviceType(), threshold.getParamKey(), threshold.getCompareOp(),
                threshold.getThresholdValue());
        return threshold.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateThreshold(Long id, AlarmThresholdDTO dto) {
        AlarmThreshold existing = thresholdMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "阈值配置不存在: " + id);
        }
        AlarmThreshold threshold = buildThreshold(dto);
        threshold.setId(id);
        if (thresholdMapper.countDuplicate(dto.getDeviceType(), threshold.getParamKey(),
                threshold.getCompareOp(), id) > 0) {
            throw new BusinessException(400, "该设备类型的此参数已存在相同比较方式的阈值");
        }
        thresholdMapper.update(threshold);
        log.info("修改告警阈值: id={}, type={}, param={}", id, threshold.getDeviceType(), threshold.getParamKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteThreshold(Long id) {
        if (thresholdMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "阈值配置不存在: " + id);
        }
        log.info("删除告警阈值: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleThreshold(Long id, boolean enabled) {
        if (thresholdMapper.updateEnabled(id, enabled) == 0) {
            throw new BusinessException(404, "阈值配置不存在: " + id);
        }
        log.info("阈值启用状态变更: id={}, enabled={}", id, enabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int detect(Device device) {
        List<AlarmThreshold> thresholds = thresholdMapper.selectEnabledByType(device.getDeviceType());
        if (thresholds.isEmpty()) {
            return 0;
        }
        Map<String, Sensor> sensorMap = new HashMap<>(8);
        for (Sensor sensor : device.getSensors()) {
            sensorMap.put(sensor.getKey(), sensor);
        }

        int created = 0;
        for (AlarmThreshold threshold : thresholds) {
            Sensor sensor = sensorMap.get(threshold.getParamKey());
            if (sensor == null) {
                continue;
            }
            AlarmRule rule = ruleRegistry.get(threshold.getCompareOp());
            if (!rule.triggered(threshold, sensor.getValue())) {
                continue;
            }
            if (alarmMapper.countRecentUnhandled(device.getDeviceId(), sensor.getKey(), SUPPRESS_MINUTES) > 0) {
                continue;
            }
            Alarm alarm = new Alarm();
            alarm.setAlarmNo(nextAlarmNo());
            alarm.setDeviceId(device.getDeviceId());
            alarm.setDeviceName(device.getDeviceName());
            alarm.setParamKey(sensor.getKey());
            alarm.setParamName(sensor.getName());
            alarm.setActualValue(sensor.getValue());
            alarm.setThresholdValue(threshold.getThresholdValue());
            alarm.setAlarmLevel(threshold.getAlarmLevel() == null ? AlarmLevel.WARNING : threshold.getAlarmLevel());
            alarm.setMessage(buildMessage(device, sensor, rule, threshold));
            alarm.setStatus(AlarmStatus.UNHANDLED);
            alarm.setTriggerTime(LocalDateTime.now());
            alarmMapper.insert(alarm);

            AlarmLog logEntry = new AlarmLog();
            logEntry.setAlarmNo(alarm.getAlarmNo());
            logEntry.setAction("TRIGGER");
            logEntry.setDetail(alarm.getMessage());
            logEntry.setOperator("system");
            logEntry.setLogTime(alarm.getTriggerTime());
            alarmLogMapper.insert(logEntry);

            created++;
            log.info("触发告警: {} | {}", alarm.getAlarmNo(), alarm.getMessage());
        }
        return created;
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private String buildMessage(Device device, Sensor sensor, AlarmRule rule, AlarmThreshold threshold) {
        return String.format("%s %s 超限：实际 %s，规则 %s",
                device.getDeviceName(),
                sensor.getName(),
                String.format("%.1f", sensor.getValue()) + (sensor.getUnit() == null ? "" : " " + sensor.getUnit()),
                rule.expression(threshold));
    }

    /**
     * 生成告警编号：AL-yyyyMMdd-0001，按天重置序号。
     *
     * @return 告警编号
     */
    private synchronized String nextAlarmNo() {
        String prefix = "AL-" + LocalDate.now().format(ALARM_NO_DATE) + "-";
        String max = alarmMapper.selectMaxAlarmNo(prefix);
        int seq = 0;
        if (max != null && max.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(max.substring(prefix.length()));
            } catch (NumberFormatException ex) {
                log.warn("告警编号序号非数字，按从 1 开始处理: {}", max);
            }
        }
        return String.format("%s%04d", prefix, seq + 1);
    }

    private AlarmThreshold buildThreshold(AlarmThresholdDTO dto) {
        CompareOp op;
        try {
            op = CompareOp.of(dto.getCompareOp());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "未知的比较方式: " + dto.getCompareOp());
        }
        if (!SensorCatalog.isSensor(dto.getDeviceType(), dto.getParamKey())) {
            throw new BusinessException(400, "参数 " + dto.getParamKey() + " 不是 "
                    + dto.getDeviceType().getLabel() + " 的可采集参数");
        }
        if (CompareOp.DEVIATION == op) {
            if (dto.getThresholdValue() == null || dto.getThresholdValue() <= 0) {
                throw new BusinessException(400, "波动检测的额定值必须大于 0");
            }
            if (dto.getDeviationPercent() == null) {
                throw new BusinessException(400, "波动检测必须填写允许波动百分比");
            }
        }

        SensorCatalog.Definition definition = SensorCatalog.find(dto.getDeviceType(), dto.getParamKey());
        AlarmThreshold threshold = new AlarmThreshold();
        threshold.setDeviceType(dto.getDeviceType());
        threshold.setParamKey(dto.getParamKey());
        threshold.setParamName(dto.getParamName() == null || dto.getParamName().trim().isEmpty()
                ? definition.getName() : dto.getParamName().trim());
        threshold.setCompareOp(op.name());
        threshold.setThresholdValue(dto.getThresholdValue());
        threshold.setDeviationPercent(CompareOp.DEVIATION == op ? dto.getDeviationPercent() : null);
        threshold.setUnit(dto.getUnit() == null || dto.getUnit().trim().isEmpty()
                ? definition.getUnit() : dto.getUnit().trim());
        threshold.setAlarmLevel(dto.getAlarmLevel());
        threshold.setEnabled(dto.getEnabled() == null ? Boolean.TRUE : dto.getEnabled());
        threshold.setRemark(dto.getRemark());
        return threshold;
    }

    private AlarmVO toVO(Alarm alarm) {
        AlarmVO vo = new AlarmVO();
        vo.setId(alarm.getId());
        vo.setAlarmNo(alarm.getAlarmNo());
        vo.setDeviceId(alarm.getDeviceId());
        vo.setDeviceName(alarm.getDeviceName());
        vo.setParamName(alarm.getParamName());
        vo.setActualValue(alarm.getActualValue());
        vo.setThresholdValue(alarm.getThresholdValue());
        if (alarm.getAlarmLevel() != null) {
            vo.setAlarmLevel(alarm.getAlarmLevel().name());
            vo.setAlarmLevelLabel(alarm.getAlarmLevel().getLabel());
        }
        vo.setMessage(alarm.getMessage());
        if (alarm.getStatus() != null) {
            vo.setStatus(alarm.getStatus().name());
            vo.setStatusLabel(alarm.getStatus().getLabel());
        }
        vo.setTriggerTime(alarm.getTriggerTime());
        vo.setHandler(alarm.getHandler());
        vo.setHandleTime(alarm.getHandleTime());
        vo.setHandleMeasure(alarm.getHandleMeasure());
        return vo;
    }

    private AlarmThresholdVO toThresholdVO(AlarmThreshold threshold) {
        AlarmThresholdVO vo = new AlarmThresholdVO();
        vo.setId(threshold.getId());
        vo.setDeviceType(threshold.getDeviceType().name());
        vo.setDeviceTypeLabel(threshold.getDeviceType().getLabel());
        vo.setParamKey(threshold.getParamKey());
        vo.setParamName(threshold.getParamName());
        vo.setCompareOp(threshold.getCompareOp());
        vo.setThresholdValue(threshold.getThresholdValue());
        vo.setDeviationPercent(threshold.getDeviationPercent());
        vo.setUnit(threshold.getUnit());
        if (threshold.getAlarmLevel() != null) {
            vo.setAlarmLevel(threshold.getAlarmLevel().name());
            vo.setAlarmLevelLabel(threshold.getAlarmLevel().getLabel());
        }
        vo.setEnabled(threshold.getEnabled());
        vo.setRemark(threshold.getRemark());
        vo.setUpdateTime(threshold.getUpdateTime());
        try {
            CompareOp op = CompareOp.of(threshold.getCompareOp());
            vo.setCompareOpLabel(op.getLabel());
            vo.setExpression(ruleRegistry.get(op).expression(threshold));
        } catch (IllegalArgumentException | BusinessException ex) {
            vo.setCompareOpLabel(threshold.getCompareOp());
            vo.setExpression(threshold.getParamName() + " " + threshold.getThresholdValue());
        }
        return vo;
    }
}
