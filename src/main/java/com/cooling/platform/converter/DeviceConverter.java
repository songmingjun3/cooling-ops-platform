package com.cooling.platform.converter;

import com.cooling.platform.common.enums.DeviceStatus;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.dto.DeviceCreateDTO;
import com.cooling.platform.dto.DeviceUpdateDTO;
import com.cooling.platform.entity.Chiller;
import com.cooling.platform.entity.CoolingTower;
import com.cooling.platform.entity.CoolingUnit;
import com.cooling.platform.entity.Device;
import com.cooling.platform.entity.DevicePO;
import com.cooling.platform.entity.Pump;
import com.cooling.platform.entity.Sensor;
import com.cooling.platform.factory.DeviceFactory;
import com.cooling.platform.vo.DeviceDetailVO;
import com.cooling.platform.vo.DeviceVO;
import com.cooling.platform.vo.SensorVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备对象转换器。
 *
 * <p>负责三组转换，保证 entity 不直接暴露给前端：</p>
 * <ol>
 *   <li>{@link DevicePO} ↔ {@link Device}：含 {@code params} JSON 的编解码；</li>
 *   <li>{@link Device} → {@link DeviceVO} / {@link DeviceDetailVO}：列表与详情视图；</li>
 *   <li>DTO → {@link Device}：新增 / 修改时的字段搬运。</li>
 * </ol>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@Component
public class DeviceConverter {

    private static final TypeReference<Map<String, Object>> PARAM_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final ObjectMapper objectMapper;

    private final DeviceFactory deviceFactory;

    /**
     * 构造转换器。
     *
     * @param objectMapper  JSON 处理器
     * @param deviceFactory 设备工厂
     */
    public DeviceConverter(ObjectMapper objectMapper, DeviceFactory deviceFactory) {
        this.objectMapper = objectMapper;
        this.deviceFactory = deviceFactory;
    }

    /**
     * 持久化对象还原为领域对象。
     *
     * @param po 持久化对象
     * @return 设备领域对象（具体子类）
     */
    public Device toDevice(DevicePO po) {
        DeviceType type = parseType(po);
        Device device = deviceFactory.create(type);
        device.setDeviceId(po.getDeviceId());
        device.setDeviceName(po.getDeviceName());
        device.setManufacturer(po.getManufacturer());
        device.setModel(po.getModel());
        device.setInstallDate(po.getInstallDate());
        device.setStatus(parseStatus(po.getStatus()));
        device.setZone(po.getZone());
        device.setCreateTime(po.getCreateTime());
        device.setUpdateTime(po.getUpdateTime());
        deviceFactory.applyParams(device, readParams(po.getParams(), po.getDeviceId()));
        return device;
    }

    /**
     * 领域对象转为持久化对象（公共字段入列，专有属性入 params JSON）。
     *
     * @param device 设备领域对象
     * @return 持久化对象
     */
    public DevicePO toPO(Device device) {
        DevicePO po = new DevicePO();
        po.setDeviceId(device.getDeviceId());
        po.setDeviceType(device.getDeviceType().name());
        po.setDeviceName(device.getDeviceName());
        po.setManufacturer(device.getManufacturer());
        po.setModel(device.getModel());
        po.setInstallDate(device.getInstallDate());
        po.setStatus(device.getStatus() == null ? DeviceStatus.STOPPED.name() : device.getStatus().name());
        po.setZone(device.getZone());
        po.setParams(writeParams(device));
        return po;
    }

    /**
     * 将新增请求中的基本信息写入设备。
     *
     * @param device 目标设备
     * @param dto    新增请求
     */
    public void fillBasic(Device device, DeviceCreateDTO dto) {
        device.setDeviceName(dto.getDeviceName().trim());
        device.setManufacturer(trimToNull(dto.getManufacturer()));
        device.setModel(trimToNull(dto.getModel()));
        device.setInstallDate(dto.getInstallDate());
        device.setZone(dto.getZone().trim());
    }

    /**
     * 将修改请求中的基本信息写入设备。
     *
     * @param device 目标设备
     * @param dto    修改请求
     */
    public void fillBasic(Device device, DeviceUpdateDTO dto) {
        device.setDeviceName(dto.getDeviceName().trim());
        device.setManufacturer(trimToNull(dto.getManufacturer()));
        device.setModel(trimToNull(dto.getModel()));
        device.setInstallDate(dto.getInstallDate());
        device.setZone(dto.getZone().trim());
    }

    /**
     * 转换为列表视图对象。
     *
     * @param device 设备
     * @return 列表视图
     */
    public DeviceVO toVO(Device device) {
        DeviceVO vo = new DeviceVO();
        fillCommon(vo, device);
        return vo;
    }

    /**
     * 转换为详情视图对象。
     *
     * @param device 设备
     * @return 详情视图
     */
    public DeviceDetailVO toDetailVO(Device device) {
        DeviceDetailVO vo = new DeviceDetailVO();
        fillCommon(vo, device);

        List<SensorVO> sensors = new ArrayList<>();
        for (Sensor sensor : device.getSensors()) {
            SensorVO sensorVO = new SensorVO();
            sensorVO.setKey(sensor.getKey());
            sensorVO.setName(sensor.getName());
            sensorVO.setValue(sensor.getValue());
            sensorVO.setUnit(sensor.getUnit());
            sensorVO.setMinValue(sensor.getMinValue());
            sensorVO.setMaxValue(sensor.getMaxValue());
            sensorVO.setCollectTime(sensor.getCollectTime());
            sensors.add(sensorVO);
        }
        vo.setSensors(sensors);
        vo.setSpecs(buildSpecs(device));
        return vo;
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private void fillCommon(DeviceVO vo, Device device) {
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType().name());
        vo.setDeviceTypeLabel(device.getDeviceType().getLabel());
        vo.setManufacturer(device.getManufacturer());
        vo.setModel(device.getModel());
        vo.setInstallDate(device.getInstallDate());
        if (device.getStatus() != null) {
            vo.setStatus(device.getStatus().name());
            vo.setStatusLabel(device.getStatus().getLabel());
        }
        vo.setZone(device.getZone());
        vo.setKeyParameterSummary(device.getKeyParameterSummary());
        vo.setCreateTime(device.getCreateTime());
        vo.setUpdateTime(device.getUpdateTime());
    }

    /**
     * 构造「规格参数」展示映射：属性名 → 带单位的显示值。
     *
     * @param device 设备
     * @return 有序的展示映射
     */
    private Map<String, String> buildSpecs(Device device) {
        Map<String, String> specs = new LinkedHashMap<>();
        switch (device.getDeviceType()) {
            case CHILLER:
                Chiller chiller = (Chiller) device;
                specs.put("制冷量", String.format("%.1f kW", chiller.getCoolingCapacity()));
                specs.put("当前负载率", String.format("%.0f%%", chiller.getCurrentLoad() * 100));
                specs.put("制冷剂类型", nullToDash(chiller.getRefrigerantType()));
                break;
            case COOLING_TOWER:
                CoolingTower tower = (CoolingTower) device;
                specs.put("冷却塔类型", tower.getTowerType() == null ? "-" : tower.getTowerType().getLabel());
                specs.put("风机转速", tower.getFanSpeed() + " RPM");
                break;
            case PUMP:
                Pump pump = (Pump) device;
                specs.put("水泵类型", pump.getPumpType() == null ? "-" : pump.getPumpType().getLabel());
                specs.put("扬程", String.format("%.1f m", pump.getHead()));
                specs.put("当前功率", String.format("%.1f kW", pump.getPower()));
                specs.put("转速", pump.getSpeed() + " RPM");
                break;
            case COOLING_UNIT:
                CoolingUnit unit = (CoolingUnit) device;
                specs.put("转速", unit.getSpeed() + " RPM");
                break;
            default:
                break;
        }
        return specs;
    }

    /**
     * 把子类专有属性序列化为 params JSON（传感器只存数值，名称与单位由 SensorCatalog 提供）。
     *
     * @param device 设备
     * @return JSON 文本
     */
    private String writeParams(Device device) {
        Map<String, Object> params = new LinkedHashMap<>();
        switch (device.getDeviceType()) {
            case CHILLER:
                Chiller chiller = (Chiller) device;
                params.put("coolingCapacity", chiller.getCoolingCapacity());
                params.put("currentLoad", chiller.getCurrentLoad());
                params.put("chilledWaterSupplyTemp", sensorValue(chiller.getChilledWaterSupplyTemp()));
                params.put("chilledWaterReturnTemp", sensorValue(chiller.getChilledWaterReturnTemp()));
                params.put("refrigerantType", chiller.getRefrigerantType());
                break;
            case COOLING_TOWER:
                CoolingTower tower = (CoolingTower) device;
                params.put("towerType", tower.getTowerType() == null ? null : tower.getTowerType().name());
                params.put("fanSpeed", tower.getFanSpeed());
                params.put("waterFlowRate", sensorValue(tower.getWaterFlowRate()));
                params.put("ambientTemp", sensorValue(tower.getAmbientTemp()));
                params.put("outletTemp", sensorValue(tower.getOutletTemp()));
                break;
            case PUMP:
                Pump pump = (Pump) device;
                params.put("pumpType", pump.getPumpType() == null ? null : pump.getPumpType().name());
                params.put("flowRate", sensorValue(pump.getFlowRate()));
                params.put("head", pump.getHead());
                params.put("power", pump.getPower());
                params.put("speed", pump.getSpeed());
                break;
            case COOLING_UNIT:
                CoolingUnit unit = (CoolingUnit) device;
                params.put("supplyTemp", sensorValue(unit.getSupplyTemp()));
                params.put("returnTemp", sensorValue(unit.getReturnTemp()));
                params.put("speed", unit.getSpeed());
                break;
            default:
                throw new BusinessException(500, "无法序列化设备参数: " + device.getDeviceType());
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "设备参数序列化失败: " + device.getDeviceId(), ex);
        }
    }

    private Map<String, Object> readParams(String json, String deviceId) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            Map<String, Object> params = objectMapper.readValue(json, PARAM_TYPE);
            return params == null ? new LinkedHashMap<String, Object>() : params;
        } catch (JsonProcessingException ex) {
            log.error("设备参数 JSON 解析失败: deviceId={}, json={}", deviceId, json, ex);
            throw new BusinessException(500, "设备 " + deviceId + " 的参数数据已损坏");
        }
    }

    private DeviceType parseType(DevicePO po) {
        try {
            return DeviceType.valueOf(po.getDeviceType());
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.error("设备类型非法: deviceId={}, type={}", po.getDeviceId(), po.getDeviceType());
            throw new BusinessException(500, "设备 " + po.getDeviceId() + " 的类型非法");
        }
    }

    private DeviceStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return DeviceStatus.STOPPED;
        }
        try {
            return DeviceStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            log.warn("设备状态非法，按停机处理: status={}", status);
            return DeviceStatus.STOPPED;
        }
    }

    private double sensorValue(Sensor sensor) {
        return sensor == null ? 0.0 : sensor.getValue();
    }

    private String nullToDash(String text) {
        return text == null || text.trim().isEmpty() ? "-" : text;
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }
}
