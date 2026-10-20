package com.cooling.platform.controller;

import com.cooling.platform.common.enums.DeviceStatus;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.config.JacksonConfig;
import com.cooling.platform.service.DeviceService;
import com.cooling.platform.vo.DeviceDetailVO;
import com.cooling.platform.vo.DeviceVO;
import com.cooling.platform.vo.ZoneGroupVO;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 设备管理接口测试（FM-01 ~ FM-06、PM-04）。
 *
 * <p>只装配 MVC 层并 Mock 业务层，聚焦协议契约：路径、必填参数、
 * 统一响应结构、异常到 HTTP 状态码的映射。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@WebMvcTest(controllers = DeviceController.class)
@Import(JacksonConfig.class)
@DisplayName("设备管理接口")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    private static DeviceVO deviceVO(String id, String name) {
        DeviceVO vo = new DeviceVO();
        vo.setDeviceId(id);
        vo.setDeviceName(name);
        vo.setDeviceType(DeviceType.CHILLER.name());
        vo.setDeviceTypeLabel("冷水机组");
        vo.setStatus(DeviceStatus.RUNNING.name());
        vo.setStatusLabel("运行中");
        vo.setZone("A区");
        vo.setCreateTime(LocalDateTime.of(2026, 10, 20, 9, 0, 0));
        return vo;
    }

    @Test
    @DisplayName("FM-04 分页查询返回统一 Result 结构与分页元信息")
    void shouldReturnPage() throws Exception {
        when(deviceService.query(any())).thenReturn(PageResult.of(
                Arrays.asList(deviceVO("CH-001", "1号冷水机组"), deviceVO("CH-002", "2号冷水机组")),
                12, 1, 20));

        mockMvc.perform(get("/api/devices").param("pageNum", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.list[0].deviceId").value("CH-001"))
                .andExpect(jsonPath("$.data.list[0].statusLabel").value("运行中"));
    }

    @Test
    @DisplayName("FM-04 设备类型非法时返回 400 与中文提示，而不是框架原文")
    void shouldRejectIllegalDeviceType() throws Exception {
        mockMvc.perform(get("/api/devices").param("deviceType", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("deviceType")));
    }

    @Test
    @DisplayName("FM-04 每页条数超过 200 时被拦截")
    void shouldRejectTooLargePageSize() throws Exception {
        mockMvc.perform(get("/api/devices").param("pageSize", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("FM-03 查看详情返回传感器与规格参数")
    void shouldReturnDetail() throws Exception {
        DeviceDetailVO detail = new DeviceDetailVO();
        detail.setDeviceId("CH-001");
        detail.setDeviceName("1号冷水机组");
        detail.setDeviceType(DeviceType.CHILLER.name());
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("制冷量", "130.0 kW");
        detail.setSpecs(specs);
        detail.setLastCollectTime(LocalDateTime.of(2026, 10, 20, 10, 30, 0));
        when(deviceService.detail("CH-001")).thenReturn(detail);

        mockMvc.perform(get("/api/devices/CH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specs.制冷量").value("130.0 kW"))
                .andExpect(jsonPath("$.data.lastCollectTime").value("2026-10-20 10:30:00"));
    }

    @Test
    @DisplayName("设备不存在时返回 404 且 code 与 HTTP 状态一致")
    void shouldReturn404WhenDeviceMissing() throws Exception {
        when(deviceService.detail("NO-1")).thenThrow(new BusinessException(404, "设备不存在: NO-1"));

        mockMvc.perform(get("/api/devices/NO-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("设备不存在: NO-1"));
    }

    @Test
    @DisplayName("FM-01 新增设备返回新编号")
    void shouldCreateDevice() throws Exception {
        when(deviceService.create(any())).thenReturn("CT-004");

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceName\":\"4号冷却塔\",\"deviceType\":\"COOLING_TOWER\","
                                + "\"zone\":\"C区\",\"params\":{\"towerType\":\"CLOSED\",\"fanSpeed\":60}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("CT-004"));
    }

    @Test
    @DisplayName("FM-01 缺少设备名称时返回 400")
    void shouldRejectCreateWithoutName() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceType\":\"CHILLER\",\"zone\":\"A区\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("设备名称")));
    }

    @Test
    @DisplayName("FM-01 设备类型非法时返回 400")
    void shouldRejectCreateWithIllegalType() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceName\":\"X\",\"deviceType\":\"ROBOT\",\"zone\":\"A区\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("FM-01 请求体不是合法 JSON 时返回 400 而不是 500")
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("FM-03 修改设备成功返回空数据")
    void shouldUpdateDevice() throws Exception {
        mockMvc.perform(put("/api/devices/CH-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceName\":\"1号冷水机组\",\"zone\":\"B区\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("FM-02 删除设备成功返回空数据")
    void shouldDeleteDevice() throws Exception {
        mockMvc.perform(delete("/api/devices/CH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("FM-02 删除不存在的设备返回 404")
    void shouldReturn404WhenDeletingMissing() throws Exception {
        doThrow(new BusinessException(404, "设备不存在: X-1")).when(deviceService).delete("X-1");

        mockMvc.perform(delete("/api/devices/X-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("FM-06 按区域分组返回分组列表")
    void shouldGroupByZone() throws Exception {
        ZoneGroupVO group = new ZoneGroupVO();
        group.setZone("A区");
        group.setDeviceCount(4);
        group.setDevices(Collections.singletonList(deviceVO("CH-001", "1号冷水机组")));
        when(deviceService.groupByZone()).thenReturn(Collections.singletonList(group));

        mockMvc.perform(get("/api/devices/group-by-zone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].zone").value("A区"))
                .andExpect(jsonPath("$.data[0].deviceCount").value(4))
                .andExpect(jsonPath("$.data[0].devices[0].deviceId").value("CH-001"));
    }

    @Test
    @DisplayName("PM-04 导出 CSV 使用 text/csv 且带附件下载头")
    void shouldExportCsv() throws Exception {
        when(deviceService.exportCsv()).thenReturn("\uFEFF设备编号,设备名称\r\nCH-001,1号冷水机组\r\n");

        mockMvc.perform(get("/api/devices/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CH-001")));
    }

    @Test
    @DisplayName("PM-04 客户端只接受 JSON 时导出仍返回 CSV，不被内容协商拒绝")
    void shouldExportCsvEvenWhenAcceptIsJson() throws Exception {
        when(deviceService.exportCsv()).thenReturn("\uFEFF设备编号\r\nCH-001\r\n");

        mockMvc.perform(get("/api/devices/export.csv").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CH-001")));
    }

    @Test
    @DisplayName("业务层抛未预期异常时返回 500 且不泄露堆栈")
    void shouldReturn500OnUnexpectedError() throws Exception {
        when(deviceService.detail(anyString())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/devices/CH-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统内部错误，请联系管理员"));
    }

    @Test
    @DisplayName("路径参数为纯数字时仍按设备编号处理")
    void shouldTreatNumericPathAsDeviceId() throws Exception {
        when(deviceService.detail(eq("123"))).thenThrow(new BusinessException(404, "设备不存在: 123"));

        mockMvc.perform(get("/api/devices/123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("设备不存在: 123"));
    }
}
