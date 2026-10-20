package com.cooling.platform.controller;

import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.config.JacksonConfig;
import com.cooling.platform.service.AlarmService;
import com.cooling.platform.vo.AlarmThresholdVO;
import com.cooling.platform.vo.AlarmVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 告警管理与阈值配置接口测试（AM-01 ~ AM-05）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@WebMvcTest(controllers = {AlarmController.class, AlarmThresholdController.class})
@Import(JacksonConfig.class)
@DisplayName("告警管理接口")
class AlarmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlarmService alarmService;

    private static AlarmVO alarm(long id, String level, String status) {
        AlarmVO vo = new AlarmVO();
        vo.setId(id);
        vo.setAlarmNo("AL-20261020-000" + id);
        vo.setDeviceId("CH-001");
        vo.setDeviceName("1号冷水机组");
        vo.setParamName("冷冻水供水温度");
        vo.setActualValue(17.2);
        vo.setThresholdValue(16.0);
        vo.setAlarmLevel(level);
        vo.setAlarmLevelLabel("CRITICAL".equals(level) ? "严重" : "警告");
        vo.setMessage("冷冻水供水温度 17.2 ℃ 超过阈值 16.0 ℃");
        vo.setStatus(status);
        vo.setStatusLabel("UNHANDLED".equals(status) ? "未处理" : "已处理");
        vo.setTriggerTime(LocalDateTime.of(2026, 10, 20, 11, 5, 0));
        return vo;
    }

    private static AlarmThresholdVO threshold(long id, String level) {
        AlarmThresholdVO vo = new AlarmThresholdVO();
        vo.setId(id);
        vo.setDeviceType("CHILLER");
        vo.setDeviceTypeLabel("冷水机组");
        vo.setParamKey("chilledWaterSupplyTemp");
        vo.setParamName("冷冻水供水温度");
        vo.setCompareOp("GT");
        vo.setCompareOpLabel("大于");
        vo.setThresholdValue(16.0);
        vo.setUnit("℃");
        vo.setAlarmLevel(level);
        vo.setAlarmLevelLabel("CRITICAL".equals(level) ? "严重" : "警告");
        vo.setEnabled(Boolean.TRUE);
        vo.setExpression("冷冻水供水温度 > 16.0 ℃");
        vo.setUpdateTime(LocalDateTime.of(2026, 10, 20, 9, 0, 0));
        return vo;
    }

    @Test
    @DisplayName("AM-03 告警分页查询返回列表与分页元信息")
    void shouldQueryAlarms() throws Exception {
        when(alarmService.query(any())).thenReturn(PageResult.of(
                Arrays.asList(alarm(1, "CRITICAL", "UNHANDLED"), alarm(2, "WARNING", "HANDLED")),
                2, 1, 20));

        mockMvc.perform(get("/api/alarms").param("pageNum", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].alarmNo").value("AL-20261020-0001"))
                .andExpect(jsonPath("$.data.list[0].alarmLevelLabel").value("严重"))
                .andExpect(jsonPath("$.data.list[0].triggerTime").value("2026-10-20 11:05:00"));
    }

    @Test
    @DisplayName("AM-03 按状态与级别过滤")
    void shouldFilterByStatusAndLevel() throws Exception {
        when(alarmService.query(any())).thenReturn(PageResult.of(
                Collections.singletonList(alarm(3, "CRITICAL", "UNHANDLED")), 1, 1, 10));

        mockMvc.perform(get("/api/alarms")
                        .param("status", "UNHANDLED")
                        .param("alarmLevel", "CRITICAL")
                        .param("deviceId", "CH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("AM-03 告警状态取值非法时返回 400")
    void shouldRejectIllegalStatus() throws Exception {
        mockMvc.perform(get("/api/alarms").param("status", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("AM-04 处理告警返回成功")
    void shouldHandleAlarm() throws Exception {
        mockMvc.perform(patch("/api/alarms/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handler\":\"张工\",\"measure\":\"已调高负载并清洗冷凝器\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(alarmService).handle(eq(1L), any());
    }

    @Test
    @DisplayName("AM-04 处理人与措施均必填")
    void shouldRejectEmptyHandler() throws Exception {
        mockMvc.perform(patch("/api/alarms/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handler\":\"\",\"measure\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("处理人")));
    }

    @Test
    @DisplayName("AM-04 重复处理返回 400")
    void shouldRejectDuplicateHandle() throws Exception {
        doThrow(new BusinessException(400, "该告警已处理，请勿重复操作: AL-20261020-0001"))
                .when(alarmService).handle(eq(1L), any());

        mockMvc.perform(patch("/api/alarms/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handler\":\"张工\",\"measure\":\"重复处理\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("已处理")));
    }

    @Test
    @DisplayName("AM-04 告警不存在返回 404")
    void shouldReturn404WhenAlarmMissing() throws Exception {
        doThrow(new BusinessException(404, "告警不存在: 99")).when(alarmService).handle(eq(99L), any());

        mockMvc.perform(patch("/api/alarms/99/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handler\":\"张工\",\"measure\":\"处理\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("AM-05 告警级别只有 WARNING / CRITICAL 两种取值")
    void shouldRejectIllegalAlarmLevel() throws Exception {
        mockMvc.perform(get("/api/alarms").param("alarmLevel", "FATAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("AM-01 查询阈值列表")
    void shouldListThresholds() throws Exception {
        when(alarmService.listThresholds(null))
                .thenReturn(Arrays.asList(threshold(1, "WARNING"), threshold(2, "CRITICAL")));

        mockMvc.perform(get("/api/alarm-thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].expression").value("冷冻水供水温度 > 16.0 ℃"))
                .andExpect(jsonPath("$.data[1].alarmLevelLabel").value("严重"));
    }

    @Test
    @DisplayName("AM-01 新增阈值返回主键")
    void shouldCreateThreshold() throws Exception {
        when(alarmService.createThreshold(any())).thenReturn(7L);

        mockMvc.perform(post("/api/alarm-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceType\":\"CHILLER\",\"paramKey\":\"chilledWaterReturnTemp\","
                                + "\"compareOp\":\"GT\",\"thresholdValue\":13.0,"
                                + "\"unit\":\"℃\",\"alarmLevel\":\"WARNING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @DisplayName("AM-01 缺少阈值数值被拦截")
    void shouldRejectThresholdWithoutValue() throws Exception {
        mockMvc.perform(post("/api/alarm-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceType\":\"CHILLER\",\"paramKey\":\"chilledWaterReturnTemp\","
                                + "\"compareOp\":\"GT\",\"alarmLevel\":\"WARNING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("阈值")));
    }

    @Test
    @DisplayName("AM-01 波动百分比越界被拦截")
    void shouldRejectIllegalDeviationPercent() throws Exception {
        mockMvc.perform(post("/api/alarm-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceType\":\"PUMP\",\"paramKey\":\"flowRate\","
                                + "\"compareOp\":\"DEVIATION\",\"thresholdValue\":85.0,"
                                + "\"deviationPercent\":150,\"alarmLevel\":\"WARNING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("AM-01 修改阈值返回成功")
    void shouldUpdateThreshold() throws Exception {
        mockMvc.perform(put("/api/alarm-thresholds/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceType\":\"CHILLER\",\"paramKey\":\"chilledWaterReturnTemp\","
                                + "\"compareOp\":\"GT\",\"thresholdValue\":13.5,"
                                + "\"unit\":\"℃\",\"alarmLevel\":\"CRITICAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("AM-01 删除阈值返回成功")
    void shouldDeleteThreshold() throws Exception {
        mockMvc.perform(delete("/api/alarm-thresholds/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("AM-01 启用 / 停用阈值")
    void shouldToggleThreshold() throws Exception {
        mockMvc.perform(patch("/api/alarm-thresholds/7/enabled").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(alarmService).toggleThreshold(7L, false);
    }

    @Test
    @DisplayName("AM-01 删除不存在的阈值返回 404")
    void shouldReturn404WhenThresholdMissing() throws Exception {
        doThrow(new BusinessException(404, "阈值配置不存在: 99")).when(alarmService).deleteThreshold(99L);

        mockMvc.perform(delete("/api/alarm-thresholds/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
