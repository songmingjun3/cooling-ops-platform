package com.cooling.platform.service.impl;

import com.cooling.platform.common.enums.DeviceStatus;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.converter.DeviceConverter;
import com.cooling.platform.entity.Device;
import com.cooling.platform.entity.DevicePO;
import com.cooling.platform.entity.RunLog;
import com.cooling.platform.entity.Sensor;
import com.cooling.platform.entity.SensorData;
import com.cooling.platform.mapper.DeviceMapper;
import com.cooling.platform.mapper.RunLogMapper;
import com.cooling.platform.mapper.SensorDataMapper;
import com.cooling.platform.service.AlarmService;
import com.cooling.platform.service.MonitorService;
import com.cooling.platform.vo.CollectResultVO;
import com.cooling.platform.vo.DeviceDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 设备运行监控业务实现。
 *
 * <p>模拟采集采用「随机游走」：每次在当前值基础上小幅波动，并限制在传感器合理区间内，
 * 使设备状态看起来连续可信；同时以较小概率放大步长，用于演示越限告警。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@Service
public class MonitorServiceImpl implements MonitorService {

    /** 随机游走的基础步长比例（相对区间宽度）。 */
    private static final double STEP_RATIO = 0.12;

    /** 出现较大波动的概率。 */
    private static final double SPIKE_PROBABILITY = 0.05;

    /** 大波动的放大倍数。 */
    private static final double SPIKE_FACTOR = 4.0;

    /** 时间格式。 */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceMapper deviceMapper;

    private final SensorDataMapper sensorDataMapper;

    private final RunLogMapper runLogMapper;

    private final DeviceConverter deviceConverter;

    private final AlarmService alarmService;

    /**
     * 构造业务实现。
     *
     * @param deviceMapper     设备数据访问接口
     * @param sensorDataMapper 采集数据访问接口
     * @param runLogMapper     运行日志数据访问接口
     * @param deviceConverter  设备转换器
     * @param alarmService     告警业务接口
     */
    public MonitorServiceImpl(DeviceMapper deviceMapper,
                              SensorDataMapper sensorDataMapper,
                              RunLogMapper runLogMapper,
                              DeviceConverter deviceConverter,
                              AlarmService alarmService) {
        this.deviceMapper = deviceMapper;
        this.sensorDataMapper = sensorDataMapper;
        this.runLogMapper = runLogMapper;
        this.deviceConverter = deviceConverter;
        this.alarmService = alarmService;
    }

    @Override
    public DeviceDetailVO realtime(String deviceId) {
        Device device = requireDevice(deviceId);
        DeviceDetailVO vo = deviceConverter.toDetailVO(device);
        // 用最新一次采集值覆盖参数值，并补齐每个参数与整体的采集时间
        deviceConverter.applyLatestSensors(vo, sensorDataMapper.selectLatestByDevice(device.getDeviceId()));
        return vo;
    }

    @Override
    public List<DeviceDetailVO> listByType(DeviceType deviceType) {
        if (deviceType == null) {
            throw new BusinessException(400, "设备类型不能为空");
        }
        List<DeviceDetailVO> result = new ArrayList<>();
        for (DevicePO po : deviceMapper.selectAll()) {
            if (!deviceType.name().equals(po.getDeviceType())) {
                continue;
            }
            result.add(realtime(po.getDeviceId()));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(String deviceId) {
        Device device = requireDevice(deviceId);
        DeviceStatus status = device.getStatus();
        if (DeviceStatus.RUNNING == status) {
            throw new BusinessException(400, "设备已在运行中，无需重复启动: " + deviceId);
        }
        if (DeviceStatus.STOPPED != status) {
            throw new BusinessException(400, "当前状态为「" + statusLabel(status) + "」，不允许启动，请先排除故障或结束维护");
        }
        // 启动时把传感器置为额定工况（区间中值），避免显示 0 值
        for (Sensor sensor : device.getSensors()) {
            sensor.setValue(nominal(sensor));
            sensor.setCollectTime(LocalDateTime.now());
        }
        device.setStatus(DeviceStatus.RUNNING);
        deviceMapper.updateStatus(deviceId, DeviceStatus.RUNNING.name());
        deviceMapper.updateParams(deviceId, deviceConverter.toPO(device).getParams());
        log.info("设备启动: id={}, name={}", deviceId, device.getDeviceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stop(String deviceId) {
        Device device = requireDevice(deviceId);
        DeviceStatus status = device.getStatus();
        if (DeviceStatus.STOPPED == status) {
            throw new BusinessException(400, "设备已停机，无需重复操作: " + deviceId);
        }
        if (DeviceStatus.RUNNING != status && DeviceStatus.FAULT != status) {
            throw new BusinessException(400, "当前状态为「" + statusLabel(status) + "」，不允许停机");
        }
        for (Sensor sensor : device.getSensors()) {
            sensor.setValue(0.0);
            sensor.setCollectTime(LocalDateTime.now());
        }
        device.setStatus(DeviceStatus.STOPPED);
        deviceMapper.updateStatus(deviceId, DeviceStatus.STOPPED.name());
        deviceMapper.updateParams(deviceId, deviceConverter.toPO(device).getParams());
        log.info("设备停机: id={}, name={}", deviceId, device.getDeviceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectResultVO collectOnce() {
        long startMillis = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        List<SensorData> dataBuffer = new ArrayList<>();
        CollectResultVO result = new CollectResultVO();
        int deviceCount = 0;
        int newAlarms = 0;

        for (DevicePO po : deviceMapper.selectAll()) {
            if (!DeviceStatus.RUNNING.name().equals(po.getStatus())) {
                continue;
            }
            Device device = deviceConverter.toDevice(po);
            deviceCount++;

            for (Sensor sensor : device.getSensors()) {
                sensor.setValue(simulate(sensor));
                sensor.setCollectTime(now);

                SensorData data = new SensorData();
                data.setDeviceId(device.getDeviceId());
                data.setSensorKey(sensor.getKey());
                data.setSensorName(sensor.getName());
                data.setValue(sensor.getValue());
                data.setUnit(sensor.getUnit());
                data.setCollectTime(now);
                dataBuffer.add(data);
            }

            String paramsJson = deviceConverter.toPO(device).getParams();
            deviceMapper.updateParams(device.getDeviceId(), paramsJson);

            RunLog runLog = new RunLog();
            runLog.setDeviceId(device.getDeviceId());
            runLog.setDeviceType(device.getDeviceType().name());
            runLog.setStatus(device.getStatus().name());
            runLog.setParams(paramsJson);
            runLog.setCollectTime(now);
            runLogMapper.insert(runLog);

            newAlarms += alarmService.detect(device);
        }

        if (!dataBuffer.isEmpty()) {
            sensorDataMapper.insertBatch(dataBuffer);
        }
        result.setDeviceCount(deviceCount);
        result.setDataPoints(dataBuffer.size());
        result.setNewAlarms(newAlarms);
        result.setCostMillis(System.currentTimeMillis() - startMillis);
        log.debug("模拟采集完成: 设备 {} 台, 数据点 {} 条, 新告警 {} 条, 耗时 {} ms",
                deviceCount, dataBuffer.size(), newAlarms, result.getCostMillis());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpired(int keepDays) {
        int days = keepDays <= 0 ? 7 : keepDays;
        String cutoff = LocalDateTime.now().minusDays(days).format(TIME_FORMAT);
        int sensorRows = sensorDataMapper.deleteOlderThan(cutoff);
        int logRows = runLogMapper.deleteOlderThan(cutoff);
        log.info("清理过期数据完成: 截止 {}, 采集数据 {} 条, 运行日志 {} 条", cutoff, sensorRows, logRows);
        return sensorRows + logRows;
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 随机游走生成下一个量测值。
     *
     * @param sensor 传感器
     * @return 模拟值（保留一位小数）
     */
    private double simulate(Sensor sensor) {
        double min = sensor.getMinValue();
        double max = sensor.getMaxValue();
        double span = max - min;
        double current = sensor.getValue() <= 0 ? nominal(sensor) : sensor.getValue();

        double step = (ThreadLocalRandom.current().nextDouble() - 0.5) * span * STEP_RATIO;
        if (ThreadLocalRandom.current().nextDouble() < SPIKE_PROBABILITY) {
            step *= SPIKE_FACTOR;
        }
        double next = Math.max(min, Math.min(max, current + step));
        return Math.round(next * 10.0) / 10.0;
    }

    /**
     * 传感器额定工况值：合理区间中值。
     *
     * @param sensor 传感器
     * @return 额定值
     */
    private double nominal(Sensor sensor) {
        return Math.round((sensor.getMinValue() + sensor.getMaxValue()) / 2 * 10.0) / 10.0;
    }

    private String statusLabel(DeviceStatus status) {
        return status == null ? "未知" : status.getLabel();
    }

    /**
     * 按编号取设备，不存在时抛出业务异常。
     *
     * @param deviceId 设备编号
     * @return 设备领域对象
     */
    private Device requireDevice(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BusinessException(400, "设备编号不能为空");
        }
        DevicePO po = deviceMapper.selectById(deviceId.trim());
        if (po == null) {
            throw new BusinessException(404, "设备不存在: " + deviceId);
        }
        return deviceConverter.toDevice(po);
    }
}
