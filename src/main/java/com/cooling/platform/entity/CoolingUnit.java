package com.cooling.platform.entity;

import com.cooling.platform.common.enums.DeviceType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 精密空调（需求文档 3.3）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CoolingUnit extends Device {

    /** 送风温度（正常带 18–27 ℃）。 */
    private Sensor supplyTemp;

    /** 回风温度（正常带 28–42 ℃）。 */
    private Sensor returnTemp;

    /** 转速，单位 RPM，取值 0–100。 */
    private int speed;

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.COOLING_UNIT;
    }

    @Override
    public List<Sensor> getSensors() {
        List<Sensor> sensors = new ArrayList<>(2);
        addIfNotNull(sensors, supplyTemp);
        addIfNotNull(sensors, returnTemp);
        return sensors;
    }

    @Override
    public String getKeyParameterSummary() {
        String supply = supplyTemp == null ? "--" : String.format("%.1f℃", supplyTemp.getValue());
        String ret = returnTemp == null ? "--" : String.format("%.1f℃", returnTemp.getValue());
        return String.format("送温:%s 回温:%s", supply, ret);
    }

    private static void addIfNotNull(List<Sensor> target, Sensor sensor) {
        if (sensor != null) {
            target.add(sensor);
        }
    }
}
