package com.cooling.platform.service.impl;

import com.cooling.platform.common.constant.DeviceConstants;
import com.cooling.platform.common.constant.SensorCatalog;
import com.cooling.platform.common.enums.AlarmStatus;
import com.cooling.platform.common.enums.DeviceStatus;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.converter.DeviceConverter;
import com.cooling.platform.dto.TrendQueryDTO;
import com.cooling.platform.entity.Device;
import com.cooling.platform.entity.DevicePO;
import com.cooling.platform.entity.Sensor;
import com.cooling.platform.entity.SensorData;
import com.cooling.platform.mapper.AlarmMapper;
import com.cooling.platform.mapper.DeviceMapper;
import com.cooling.platform.mapper.SensorDataMapper;
import com.cooling.platform.service.StatisticsService;
import com.cooling.platform.vo.NameCountVO;
import com.cooling.platform.vo.StatisticsVO;
import com.cooling.platform.vo.TrendPointVO;
import com.cooling.platform.vo.TrendVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析业务实现。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DeviceMapper deviceMapper;

    private final SensorDataMapper sensorDataMapper;

    private final AlarmMapper alarmMapper;

    private final DeviceConverter deviceConverter;

    /**
     * 构造业务实现。
     *
     * @param deviceMapper     设备数据访问接口
     * @param sensorDataMapper 采集数据访问接口
     * @param alarmMapper      告警数据访问接口
     * @param deviceConverter  设备转换器
     */
    public StatisticsServiceImpl(DeviceMapper deviceMapper,
                                 SensorDataMapper sensorDataMapper,
                                 AlarmMapper alarmMapper,
                                 DeviceConverter deviceConverter) {
        this.deviceMapper = deviceMapper;
        this.sensorDataMapper = sensorDataMapper;
        this.alarmMapper = alarmMapper;
        this.deviceConverter = deviceConverter;
    }

    @Override
    public StatisticsVO overview() {
        List<DevicePO> devices = deviceMapper.selectAll();

        Map<String, Long> typeCount = new LinkedHashMap<>();
        Map<String, Long> statusCount = new LinkedHashMap<>();
        Map<String, Long> zoneCount = new LinkedHashMap<>();
        for (DeviceType type : DeviceType.values()) {
            typeCount.put(type.name(), 0L);
        }
        for (DeviceStatus status : DeviceStatus.values()) {
            statusCount.put(status.name(), 0L);
        }
        for (DevicePO po : devices) {
            typeCount.merge(po.getDeviceType(), 1L, Long::sum);
            statusCount.merge(po.getStatus(), 1L, Long::sum);
            String zone = po.getZone() == null || po.getZone().trim().isEmpty()
                    ? DeviceConstants.UNKNOWN_ZONE : po.getZone();
            zoneCount.merge(zone, 1L, Long::sum);
        }

        StatisticsVO vo = new StatisticsVO();
        vo.setTotal(devices.size());
        vo.setRunning(statusCount.getOrDefault(DeviceStatus.RUNNING.name(), 0L));
        vo.setStopped(statusCount.getOrDefault(DeviceStatus.STOPPED.name(), 0L));
        vo.setFault(statusCount.getOrDefault(DeviceStatus.FAULT.name(), 0L));
        vo.setMaintenance(statusCount.getOrDefault(DeviceStatus.MAINTENANCE.name(), 0L));
        vo.setOffline(statusCount.getOrDefault(DeviceStatus.OFFLINE.name(), 0L));

        List<NameCountVO> byType = new ArrayList<>();
        for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
            DeviceType type = DeviceType.valueOf(entry.getKey());
            byType.add(new NameCountVO(entry.getKey(), type.getLabel(), entry.getValue()));
        }
        List<NameCountVO> byStatus = new ArrayList<>();
        for (Map.Entry<String, Long> entry : statusCount.entrySet()) {
            DeviceStatus status = DeviceStatus.valueOf(entry.getKey());
            byStatus.add(new NameCountVO(entry.getKey(), status.getLabel(), entry.getValue()));
        }
        List<NameCountVO> byZone = new ArrayList<>();
        for (Map.Entry<String, Long> entry : zoneCount.entrySet()) {
            byZone.add(new NameCountVO(entry.getKey(), entry.getKey(), entry.getValue()));
        }

        vo.setByType(byType);
        vo.setByStatus(byStatus);
        vo.setByZone(byZone);
        vo.setUnhandledAlarms(alarmMapper.countByStatus(AlarmStatus.UNHANDLED));
        vo.setTotalAlarms(alarmMapper.countAll());
        return vo;
    }

    @Override
    public TrendVO trend(TrendQueryDTO query) {
        DevicePO po = deviceMapper.selectById(query.getDeviceId());
        if (po == null) {
            throw new BusinessException(404, "设备不存在: " + query.getDeviceId());
        }
        Device device = deviceConverter.toDevice(po);
        List<Sensor> sensors = device.getSensors();
        if (sensors.isEmpty()) {
            throw new BusinessException(400, "该设备没有可展示的运行参数");
        }

        Sensor sensor = pickSensor(device, query.getSensorKey());
        int limit = query.getLimit() == null || query.getLimit() < 1 ? 30 : query.getLimit();
        List<SensorData> recent = sensorDataMapper.selectRecent(device.getDeviceId(), sensor.getKey(), limit);
        Collections.reverse(recent);

        List<TrendPointVO> points = new ArrayList<>(recent.size());
        for (SensorData data : recent) {
            points.add(new TrendPointVO(data.getCollectTime().format(TIME_FORMAT), data.getValue()));
        }

        TrendVO vo = new TrendVO();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        vo.setSensorKey(sensor.getKey());
        vo.setSensorName(sensor.getName());
        vo.setUnit(sensor.getUnit());
        SensorCatalog.Definition definition = SensorCatalog.find(device.getDeviceType(), sensor.getKey());
        if (definition != null) {
            vo.setMinValue(definition.getMin());
            vo.setMaxValue(definition.getMax());
        }
        vo.setPoints(points);
        return vo;
    }

    /**
     * 选取趋势参数：优先使用指定键，否则取第一个传感器。
     *
     * @param device    设备
     * @param sensorKey 指定参数键，可为空
     * @return 传感器
     */
    private Sensor pickSensor(Device device, String sensorKey) {
        List<Sensor> sensors = device.getSensors();
        if (sensorKey == null || sensorKey.trim().isEmpty()) {
            return sensors.get(0);
        }
        for (Sensor sensor : sensors) {
            if (sensor.getKey().equals(sensorKey)) {
                return sensor;
            }
        }
        throw new BusinessException(400, "参数 " + sensorKey + " 不属于设备 " + device.getDeviceId());
    }
}
