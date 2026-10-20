package com.cooling.platform.controller;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.config.JacksonConfig;
import com.cooling.platform.service.MonitorService;
import com.cooling.platform.vo.CollectResultVO;
import com.cooling.platform.vo.DeviceDetailVO;
import com.cooling.platform.vo.SensorVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 运行监控接口测试（MM-01 ~ MM-05）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@WebMvcTest(controllers = MonitorController.class)
@Import(JacksonConfig.class)
@DisplayName("运行监控接口")
class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitorService monitorService;

    private static SensorVO sensor(String key, String name, double value) {
        SensorVO vo = new SensorVO();
        vo.setKey(key);
        vo.setName(name);
        vo.setValue(value);
        vo.setUnit("℃");
        vo.setMinValue(6.5);
        vo.setMaxValue(17.5);
        vo.setCollectTime(LocalDateTime.of(2026, 10, 20, 11, 0, 0));
        return vo;
    }

    private static DeviceDetailVO detail(String id) {
        DeviceDetailVO vo = new DeviceDetailVO();
        vo.setDeviceId(id);
        vo.setDeviceName("1号冷水机组");
        vo.setDeviceType(DeviceType.CHILLER.name());
        vo.setStatus("RUNNING");
        vo.setStatusLabel("运行中");
        vo.setSensors(Arrays.asList(
                sensor("chilledWaterSupplyTemp", "冷冻水供水温度", 7.2),
                sensor("chilledWaterReturnTemp", "冷冻水回水温度", 12.8)));
        vo.setLastCollectTime(LocalDateTime.of(2026, 10, 20, 11, 0, 0));
        return vo;
    }

    @Test
    @DisplayName("MM-01 实时状态返回全部运行参数与最后采集时间")
    void shouldReturnRealtime() throws Exception {
        when(monitorService.realtime("CH-001")).thenReturn(detail("CH-001"));

        mockMvc.perform(get("/api/monitor/devices/CH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deviceId").value("CH-001"))
                .andExpect(jsonPath("$.data.sensors.length()").value(2))
                .andExpect(jsonPath("$.data.sensors[0].key").value("chilledWaterSupplyTemp"))
                .andExpect(jsonPath("$.data.sensors[0].value").value(7.2))
                .andExpect(jsonPath("$.data.sensors[0].unit").value("℃"))
                .andExpect(jsonPath("$.data.lastCollectTime").value("2026-10-20 11:00:00"));
    }

    @Test
    @DisplayName("MM-01 设备不存在返回 404")
    void shouldReturn404WhenMissing() throws Exception {
        when(monitorService.realtime("NO-1")).thenThrow(new BusinessException(404, "设备不存在: NO-1"));

        mockMvc.perform(get("/api/monitor/devices/NO-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("MM-02 按类型批量查看")
    void shouldListByType() throws Exception {
        when(monitorService.listByType(DeviceType.CHILLER))
                .thenReturn(Arrays.asList(detail("CH-001"), detail("CH-002")));

        mockMvc.perform(get("/api/monitor/devices").param("deviceType", "CHILLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("MM-02 缺少 deviceType 参数返回 400")
    void shouldRejectMissingType() throws Exception {
        mockMvc.perform(get("/api/monitor/devices"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("MM-02 设备类型取值非法返回 400 与中文提示")
    void shouldRejectIllegalType() throws Exception {
        mockMvc.perform(get("/api/monitor/devices").param("deviceType", "REACTOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("deviceType")));
    }

    @Test
    @DisplayName("MM-03 启动设备调用业务层 start")
    void shouldStartDevice() throws Exception {
        mockMvc.perform(post("/api/monitor/devices/CH-001/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(monitorService).start("CH-001");
    }

    @Test
    @DisplayName("MM-03 状态机不允许时返回 400")
    void shouldRejectStartWhenNotAllowed() throws Exception {
        doThrow(new BusinessException(400, "设备已在运行中，无需重复启动: CH-001"))
                .when(monitorService).start("CH-001");

        mockMvc.perform(post("/api/monitor/devices/CH-001/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("设备已在运行中，无需重复启动: CH-001"));
    }

    @Test
    @DisplayName("MM-04 停机设备调用业务层 stop")
    void shouldStopDevice() throws Exception {
        mockMvc.perform(post("/api/monitor/devices/CH-001/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(monitorService).stop("CH-001");
    }

    @Test
    @DisplayName("MM-05 手动触发采集返回采集统计")
    void shouldTriggerCollect() throws Exception {
        CollectResultVO result = new CollectResultVO();
        result.setDeviceCount(8);
        result.setDataPoints(15);
        result.setNewAlarms(1);
        result.setCostMillis(26L);
        when(monitorService.collectOnce()).thenReturn(result);

        mockMvc.perform(post("/api/monitor/collect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceCount").value(8))
                .andExpect(jsonPath("$.data.dataPoints").value(15))
                .andExpect(jsonPath("$.data.newAlarms").value(1));
    }

    @Test
    @DisplayName("MM-02 某类型下无设备时返回空数组而不是 null")
    void shouldReturnEmptyList() throws Exception {
        when(monitorService.listByType(DeviceType.COOLING_UNIT))
                .thenReturn(Collections.<DeviceDetailVO>emptyList());

        mockMvc.perform(get("/api/monitor/devices").param("deviceType", "COOLING_UNIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
