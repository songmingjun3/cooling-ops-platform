package com.cooling.platform.entity;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.enums.PumpType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 水泵（需求文档 3.3）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Pump extends Device {

    /** 水泵类型：冷冻水泵 / 冷却水泵。 */
    private PumpType pumpType;

    /** 当前流量，单位 m³/h。 */
    private Sensor flowRate;

    /** 扬程，单位 m。 */
    private double head;

    /** 当前功率，单位 kW。 */
    private double power;

    /** 转速，单位 RPM，取值 0–100。 */
    private int speed;

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.PUMP;
    }

    @Override
    public List<Sensor> getSensors() {
        List<Sensor> sensors = new ArrayList<>(1);
        if (flowRate != null) {
            sensors.add(flowRate);
        }
        return sensors;
    }

    @Override
    public String getKeyParameterSummary() {
        String flow = flowRate == null ? "--" : String.format("%.0fm³/h", flowRate.getValue());
        return String.format("流量:%s 功率:%.0fkW", flow, power);
    }
}
