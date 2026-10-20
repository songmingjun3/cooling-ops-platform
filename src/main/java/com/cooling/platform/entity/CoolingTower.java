package com.cooling.platform.entity;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.enums.TowerType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 冷却塔（需求文档 3.3）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CoolingTower extends Device {

    /** 冷却塔类型：开式 / 闭式。 */
    private TowerType towerType;

    /** 风机转速，单位 RPM，取值 0–100。 */
    private int fanSpeed;

    /** 水流量，单位 m³/h。 */
    private Sensor waterFlowRate;

    /** 环境温度，单位 ℃。 */
    private Sensor ambientTemp;

    /** 出水温度（正常带 18–34 ℃）。 */
    private Sensor outletTemp;

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.COOLING_TOWER;
    }

    @Override
    public List<Sensor> getSensors() {
        List<Sensor> sensors = new ArrayList<>(3);
        addIfNotNull(sensors, waterFlowRate);
        addIfNotNull(sensors, ambientTemp);
        addIfNotNull(sensors, outletTemp);
        return sensors;
    }

    @Override
    public String getKeyParameterSummary() {
        String outlet = outletTemp == null ? "--" : String.format("%.1f℃", outletTemp.getValue());
        String flow = waterFlowRate == null ? "--" : String.format("%.0fm³/h", waterFlowRate.getValue());
        return String.format("出温:%s 流量:%s", outlet, flow);
    }

    private static void addIfNotNull(List<Sensor> target, Sensor sensor) {
        if (sensor != null) {
            target.add(sensor);
        }
    }
}
