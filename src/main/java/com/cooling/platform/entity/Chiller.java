package com.cooling.platform.entity;

import com.cooling.platform.common.enums.DeviceType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 冷水机组（需求文档 3.3）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Chiller extends Device {

    /** 制冷量，单位 kW。 */
    private double coolingCapacity;

    /** 当前负载率，取值 0.0–1.0。 */
    private double currentLoad;

    /** 冷冻水供水温度（正常带 12–16 ℃）。 */
    private Sensor chilledWaterSupplyTemp;

    /** 冷冻水回水温度（℃）。 */
    private Sensor chilledWaterReturnTemp;

    /** 制冷剂类型，如 R134a。 */
    private String refrigerantType;

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.CHILLER;
    }

    @Override
    public List<Sensor> getSensors() {
        List<Sensor> sensors = new ArrayList<>(2);
        addIfNotNull(sensors, chilledWaterSupplyTemp);
        addIfNotNull(sensors, chilledWaterReturnTemp);
        return sensors;
    }

    @Override
    public String getKeyParameterSummary() {
        String supply = chilledWaterSupplyTemp == null
                ? "--" : String.format("%.1f℃", chilledWaterSupplyTemp.getValue());
        return String.format("供温:%s 负载:%.0f%%", supply, currentLoad * 100);
    }

    private static void addIfNotNull(List<Sensor> target, Sensor sensor) {
        if (sensor != null) {
            target.add(sensor);
        }
    }
}
