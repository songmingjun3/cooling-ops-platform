package com.cooling.platform.controller;

import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.config.JacksonConfig;
import com.cooling.platform.service.LogService;
import com.cooling.platform.service.StatisticsService;
import com.cooling.platform.vo.AlarmLogVO;
import com.cooling.platform.vo.NameCountVO;
import com.cooling.platform.vo.RunLogVO;
import com.cooling.platform.vo.StatisticsVO;
import com.cooling.platform.vo.TrendPointVO;
import com.cooling.platform.vo.TrendVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据分析与数据持久化接口测试（AN-01 / AN-02、PM-02 / PM-03）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@WebMvcTest(controllers = {StatisticsController.class, LogController.class})
@Import(JacksonConfig.class)
@DisplayName("分析与日志接口")
class StatisticsLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private LogService logService;

    private static NameCountVO count(String name, String label, long value) {
        NameCountVO vo = new NameCountVO();
        vo.setName(name);
        vo.setLabel(label);
        vo.setCount(value);
        return vo;
    }

    @Test
    @DisplayName("AN-01 统计概览含总数、状态分布与分组明细")
    void shouldReturnOverview() throws Exception {
        StatisticsVO vo = new StatisticsVO();
        vo.setTotal(12);
        vo.setRunning(8);
        vo.setStopped(1);
        vo.setFault(1);
        vo.setMaintenance(1);
        vo.setOffline(0);
        vo.setUnhandledAlarms(3);
        vo.setTotalAlarms(9);
        vo.setByType(Arrays.asList(count("CHILLER", "冷水机组", 2), count("PUMP", "水泵", 4)));
        vo.setByStatus(Collections.singletonList(count("RUNNING", "运行中", 8)));
        vo.setByZone(Arrays.asList(count("A区", "A区", 5), count("B区", "B区", 4)));
        when(statisticsService.overview()).thenReturn(vo);

        mockMvc.perform(get("/api/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.running").value(8))
                .andExpect(jsonPath("$.data.unhandledAlarms").value(3))
                .andExpect(jsonPath("$.data.byType[0].name").value("CHILLER"))
                .andExpect(jsonPath("$.data.byZone.length()").value(2));
    }

    @Test
    @DisplayName("AN-02 趋势查询返回点位、极值与单位")
    void shouldReturnTrend() throws Exception {
        TrendVO vo = new TrendVO();
        vo.setDeviceId("CH-001");
        vo.setDeviceName("1号冷水机组");
        vo.setSensorKey("chilledWaterSupplyTemp");
        vo.setSensorName("冷冻水供水温度");
        vo.setUnit("℃");
        vo.setMinValue(6.8);
        vo.setMaxValue(17.2);
        vo.setPoints(Arrays.asList(
                new TrendPointVO("2026-10-20 11:00:00", 7.2),
                new TrendPointVO("2026-10-20 11:00:03", 7.6)));
        when(statisticsService.trend(any())).thenReturn(vo);

        mockMvc.perform(get("/api/statistics/trend")
                        .param("deviceId", "CH-001")
                        .param("sensorKey", "chilledWaterSupplyTemp")
                        .param("limit", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sensorName").value("冷冻水供水温度"))
                .andExpect(jsonPath("$.data.unit").value("℃"))
                .andExpect(jsonPath("$.data.minValue").value(6.8))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.data.points[0].time").value("2026-10-20 11:00:00"));
    }

    @Test
    @DisplayName("AN-02 缺少设备编号返回 400")
    void shouldRejectTrendWithoutDeviceId() throws Exception {
        mockMvc.perform(get("/api/statistics/trend").param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("设备编号")));
    }

    @Test
    @DisplayName("AN-02 采集点数超过 200 返回 400")
    void shouldRejectTooManyPoints() throws Exception {
        mockMvc.perform(get("/api/statistics/trend")
                        .param("deviceId", "CH-001")
                        .param("limit", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("200")));
    }

    @Test
    @DisplayName("AN-02 设备不存在返回 404")
    void shouldReturn404ForUnknownDevice() throws Exception {
        when(statisticsService.trend(any())).thenThrow(new BusinessException(404, "设备不存在: NO-1"));

        mockMvc.perform(get("/api/statistics/trend").param("deviceId", "NO-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PM-02 运行日志分页查询")
    void shouldQueryRunLogs() throws Exception {
        RunLogVO vo = new RunLogVO();
        vo.setId(1L);
        vo.setDeviceId("CH-001");
        vo.setDeviceType("CHILLER");
        vo.setStatus("RUNNING");
        vo.setParams("{\"chilledWaterSupplyTemp\":7.2}");
        vo.setCollectTime(LocalDateTime.of(2026, 10, 20, 11, 0, 0));
        when(logService.runLogs(eq("CH-001"), eq(1), eq(10)))
                .thenReturn(PageResult.of(Collections.singletonList(vo), 1, 1, 10));

        mockMvc.perform(get("/api/logs/run")
                        .param("deviceId", "CH-001")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].deviceId").value("CH-001"))
                .andExpect(jsonPath("$.data.list[0].collectTime").value("2026-10-20 11:00:00"));
    }

    @Test
    @DisplayName("PM-03 告警日志按告警编号追踪")
    void shouldQueryAlarmLogs() throws Exception {
        AlarmLogVO vo = new AlarmLogVO();
        vo.setId(1L);
        vo.setAlarmNo("AL-20261020-0001");
        vo.setAction("TRIGGER");
        vo.setDetail("冷冻水供水温度 17.2 ℃ 超过阈值 16.0 ℃");
        vo.setOperator("system");
        vo.setLogTime(LocalDateTime.of(2026, 10, 20, 11, 5, 0));
        when(logService.alarmLogs(eq("AL-20261020-0001"), eq(1), eq(20)))
                .thenReturn(PageResult.of(Collections.singletonList(vo), 1, 1, 20));

        mockMvc.perform(get("/api/logs/alarm").param("alarmNo", "AL-20261020-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].action").value("TRIGGER"))
                .andExpect(jsonPath("$.data.list[0].logTime").value("2026-10-20 11:05:00"));
    }

    @Test
    @DisplayName("PM-02 分页参数默认可省略")
    void shouldUseDefaultPaging() throws Exception {
        when(logService.runLogs(eq(null), eq(1), eq(20)))
                .thenReturn(PageResult.of(Collections.<RunLogVO>emptyList(), 0, 1, 20));

        mockMvc.perform(get("/api/logs/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
