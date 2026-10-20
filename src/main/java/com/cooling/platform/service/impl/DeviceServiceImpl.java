package com.cooling.platform.service.impl;

import com.cooling.platform.common.constant.DeviceConstants;
import com.cooling.platform.common.enums.DeviceStatus;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.common.util.CsvExporter;
import com.cooling.platform.converter.DeviceConverter;
import com.cooling.platform.dto.DeviceCreateDTO;
import com.cooling.platform.dto.DeviceQueryDTO;
import com.cooling.platform.dto.DeviceUpdateDTO;
import com.cooling.platform.entity.Device;
import com.cooling.platform.entity.DevicePO;
import com.cooling.platform.factory.DeviceFactory;
import com.cooling.platform.factory.DeviceIdGenerator;
import com.cooling.platform.mapper.DeviceMapper;
import com.cooling.platform.service.DeviceService;
import com.cooling.platform.vo.DeviceDetailVO;
import com.cooling.platform.vo.DeviceVO;
import com.cooling.platform.vo.ZoneGroupVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备管理业务实现。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    private final DeviceFactory deviceFactory;

    private final DeviceConverter deviceConverter;

    private final DeviceIdGenerator deviceIdGenerator;

    /**
     * 构造业务实现。
     *
     * @param deviceMapper      设备数据访问接口
     * @param deviceFactory     设备工厂
     * @param deviceConverter   对象转换器
     * @param deviceIdGenerator 编号生成器
     */
    public DeviceServiceImpl(DeviceMapper deviceMapper,
                             DeviceFactory deviceFactory,
                             DeviceConverter deviceConverter,
                             DeviceIdGenerator deviceIdGenerator) {
        this.deviceMapper = deviceMapper;
        this.deviceFactory = deviceFactory;
        this.deviceConverter = deviceConverter;
        this.deviceIdGenerator = deviceIdGenerator;
    }

    @Override
    public PageResult<DeviceVO> query(DeviceQueryDTO query) {
        DeviceQueryDTO condition = query == null ? new DeviceQueryDTO() : query;
        long total = deviceMapper.countByCondition(condition);
        List<DeviceVO> list = new ArrayList<>();
        if (total > 0) {
            List<DevicePO> rows = deviceMapper.selectByCondition(condition, condition.offset(), condition.limit());
            for (DevicePO row : rows) {
                list.add(deviceConverter.toVO(deviceConverter.toDevice(row)));
            }
        }
        log.debug("查询设备: total={}, pageNum={}, pageSize={}", total, condition.safePageNum(), condition.limit());
        return PageResult.of(list, total, condition.safePageNum(), condition.limit());
    }

    @Override
    public DeviceDetailVO detail(String deviceId) {
        return deviceConverter.toDetailVO(requireDevice(deviceId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DeviceCreateDTO dto) {
        Device device = deviceFactory.create(dto.getDeviceType());
        deviceConverter.fillBasic(device, dto);
        device.setDeviceId(deviceIdGenerator.nextId(dto.getDeviceType()));
        // 新设备默认停机，需通过启停接口（MM-03）投入运行
        device.setStatus(DeviceStatus.STOPPED);
        deviceFactory.applyParams(device, dto.getParams());

        DevicePO po = deviceConverter.toPO(device);
        deviceMapper.insert(po);
        log.info("新增设备成功: id={}, name={}, type={}, zone={}",
                device.getDeviceId(), device.getDeviceName(), device.getDeviceType(), device.getZone());
        return device.getDeviceId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String deviceId, DeviceUpdateDTO dto) {
        Device device = requireDevice(deviceId);
        deviceConverter.fillBasic(device, dto);
        DevicePO po = deviceConverter.toPO(device);
        int rows = deviceMapper.updateBasicInfo(po);
        if (rows == 0) {
            throw new BusinessException(404, "设备不存在或已被删除: " + deviceId);
        }
        log.info("修改设备信息成功: id={}, name={}, zone={}", deviceId, dto.getDeviceName(), dto.getZone());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String deviceId) {
        requireDevice(deviceId);
        // 先清理采集数据，再删除设备；告警与运行日志作为历史留痕保留
        int sensorRows = deviceMapper.deleteSensorDataByDevice(deviceId);
        int rows = deviceMapper.deleteById(deviceId);
        if (rows == 0) {
            throw new BusinessException(404, "设备不存在或已被删除: " + deviceId);
        }
        log.info("删除设备成功: id={}, 同时清理传感器数据 {} 条", deviceId, sensorRows);
    }

    @Override
    public List<ZoneGroupVO> groupByZone() {
        List<DevicePO> rows = deviceMapper.selectAll();
        Map<String, List<DeviceVO>> grouped = new LinkedHashMap<>();
        for (DevicePO row : rows) {
            String zone = row.getZone() == null || row.getZone().trim().isEmpty()
                    ? DeviceConstants.UNKNOWN_ZONE : row.getZone();
            grouped.computeIfAbsent(zone, key -> new ArrayList<>())
                    .add(deviceConverter.toVO(deviceConverter.toDevice(row)));
        }
        List<ZoneGroupVO> groups = new ArrayList<>(grouped.size());
        for (Map.Entry<String, List<DeviceVO>> entry : grouped.entrySet()) {
            groups.add(new ZoneGroupVO(entry.getKey(), entry.getValue().size(), entry.getValue()));
        }
        log.debug("按区域分组查询完成: 区域数={}, 设备数={}", groups.size(), rows.size());
        return groups;
    }

    @Override
    public String exportCsv() {
        List<DevicePO> rows = deviceMapper.selectAll();
        String[] header = {"设备编号", "设备名称", "设备类型", "所属区域", "运行状态",
                "生产厂商", "设备型号", "安装日期", "关键参数"};
        List<String[]> data = new ArrayList<>(rows.size());
        for (DevicePO row : rows) {
            Device device = deviceConverter.toDevice(row);
            data.add(new String[]{
                    device.getDeviceId(),
                    device.getDeviceName(),
                    device.getDeviceType().getLabel(),
                    device.getZone(),
                    device.getStatus() == null ? "" : device.getStatus().getLabel(),
                    device.getManufacturer(),
                    device.getModel(),
                    device.getInstallDate() == null ? "" : device.getInstallDate().toString(),
                    device.getKeyParameterSummary()
            });
        }
        log.info("导出设备清单: {} 条", data.size());
        return CsvExporter.build(header, data);
    }

    /**
     * 按编号取设备，不存在时抛出业务异常。
     *
     * @param deviceId 设备编号
     * @return 设备领域对象
     */
    private Device requireDevice(String deviceId) {        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BusinessException(400, "设备编号不能为空");
        }
        DevicePO po = deviceMapper.selectById(deviceId.trim());
        if (po == null) {
            throw new BusinessException(404, "设备不存在: " + deviceId);
        }
        return deviceConverter.toDevice(po);
    }
}
