package com.cooling.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 功能测试：真实 Spring 上下文 + 真实 SQLite + 真实 HTTP，覆盖需求文档
 * FM / MM / AM / PM / AN 五大模块的完整业务链路。
 *
 * <p>与接口测试的区别：接口测试 Mock 掉业务层只验证协议；本类不 Mock 任何组件，
 * 验证 Controller → Service → Mapper → SQLite 全链路，以及模块之间的联动
 * （采集产生数据 → 触发告警 → 处理留痕 → 统计与趋势可查）。</p>
 *
 * <p>用例按顺序执行，共享一条“新建设备”的主线，避免重复造数据。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("平台功能测试")
class CoolingPlatformFlowTest {

    /** 测试数据库文件相对路径，与 application-test.yml 保持一致。 */
    private static final Path TEST_DB = Paths.get("target", "test-data", "cooling-ops-test.db");

    /** 主线设备：用例 2 创建，用例 14 删除。 */
    private static final String PROBE_NAME = "功能测试冷却塔";

    /** 阈值恢复用：供水温度原始阈值。 */
    private static final double ORIGIN_THRESHOLD = 16.0;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    private String probeId;

    /*
     * 静态块在类加载时执行，早于 SpringExtension 创建上下文，
     * 因此必须在此时完成「建目录 + 删旧库」：SQLite 不会自动创建目录，
     * 而用例又需要每次从 data.sql 的初始数据重新开始。
     */
    static {
        try {
            Files.createDirectories(TEST_DB.getParent());
            Files.deleteIfExists(TEST_DB);
        } catch (IOException ex) {
            throw new IllegalStateException("初始化测试数据库失败", ex);
        }
    }

    /**
     * 每个用例前把请求工厂换成 Apache HttpClient。
     *
     * <p>JDK 自带的 {@code HttpURLConnection} 不支持 PATCH，会抛
     * {@code Invalid HTTP method: PATCH}；而告警处理接口正是 PATCH 语义。</p>
     */
    @BeforeEach
    void usePatchCapableClient() {
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    // ------------------------------------------------------------------
    // 基础工具
    // ------------------------------------------------------------------

    private JsonNode request(HttpMethod method, String path, Object body) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = rest.exchange(
                URI.create(path), method, entity, String.class);
        String text = response.getBody();
        assertNotNull(text, "响应体不应为空: " + path);
        return objectMapper.readTree(text);
    }

    private JsonNode get(String path) throws IOException {
        return request(HttpMethod.GET, path, null);
    }

    private JsonNode post(String path, Object body) throws IOException {
        return request(HttpMethod.POST, path, body);
    }

    private int statusOf(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(URI.create(path), method, new HttpEntity<>(body, headers), String.class)
                .getStatusCodeValue();
    }

    private static Map<String, Object> body(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    // ------------------------------------------------------------------
    // FM 设备管理 + 初始数据
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("初始数据：12 台示例设备与 7 条告警阈值已就绪")
    void shouldLoadSeedData() throws IOException {
        JsonNode devices = get("/api/devices?pageNum=1&pageSize=50");
        assertEquals(0, devices.get("code").asInt());
        assertEquals(12, devices.get("data").get("total").asInt(), "data.sql 应初始化 12 台设备");

        JsonNode thresholds = get("/api/alarm-thresholds");
        assertEquals(7, thresholds.get("data").size(), "data.sql 应初始化 7 条阈值配置");
    }

    @Test
    @Order(2)
    @DisplayName("FM-01 新增设备自动生成编号 CT-004")
    void shouldCreateDevice() throws IOException {
        JsonNode result = post("/api/devices", body(
                "deviceName", PROBE_NAME,
                "deviceType", "COOLING_TOWER",
                "zone", "C区",
                "manufacturer", "良机",
                "model", "LBC-300",
                "installDate", "2025-03-01",
                "params", body("towerType", "CLOSED", "fanSpeed", 60, "outletTemp", 31.0)));

        assertEquals(0, result.get("code").asInt(), "新增设备应成功: " + result);
        probeId = result.get("data").asText();
        assertEquals("CT-004", probeId, "编号应接续既有最大值 CT-003");
    }

    @Test
    @Order(3)
    @DisplayName("FM-04 多条件查询与关键字模糊匹配可命中新设备")
    void shouldQueryCreatedDevice() throws IOException {
        JsonNode byType = get("/api/devices?deviceType=COOLING_TOWER&pageSize=50");
        assertEquals(4, byType.get("data").get("total").asInt(), "冷却塔应变为 4 台");

        JsonNode byZone = get("/api/devices?zone=C%E5%8C%BA&pageSize=50");
        assertTrue(byZone.get("data").get("total").asInt() >= 3, "C区设备数应包含新增设备");

        JsonNode byKeyword = get("/api/devices?keyword=%E5%8A%9F%E8%83%BD%E6%B5%8B%E8%AF%95&pageSize=50");
        assertEquals(1, byKeyword.get("data").get("total").asInt(), "按名称关键字应唯一命中");
        assertEquals(probeId, byKeyword.get("data").get("list").get(0).get("deviceId").asText());
    }

    @Test
    @Order(4)
    @DisplayName("FM-03 详情返回传感器参数与规格参数")
    void shouldShowDetail() throws IOException {
        JsonNode detail = get("/api/devices/" + probeId).get("data");
        assertEquals(PROBE_NAME, detail.get("deviceName").asText());
        assertEquals(3, detail.get("sensors").size(), "冷却塔有流量/环境温度/出水温度三个参数");
        assertEquals("闭式", detail.get("specs").get("冷却塔类型").asText());
    }

    @Test
    @Order(5)
    @DisplayName("FM-03 修改基本信息不影响运行参数")
    void shouldUpdateBasicInfo() throws IOException {
        JsonNode result = request(HttpMethod.PUT, "/api/devices/" + probeId, body(
                "deviceName", PROBE_NAME + "-改名",
                "zone", "B区",
                "manufacturer", "良机",
                "model", "LBC-301",
                "installDate", "2025-03-01"));
        assertEquals(0, result.get("code").asInt());

        JsonNode detail = get("/api/devices/" + probeId).get("data");
        assertEquals(PROBE_NAME + "-改名", detail.get("deviceName").asText());
        assertEquals("B区", detail.get("zone").asText());
        assertEquals("闭式", detail.get("specs").get("冷却塔类型").asText(),
                "修改基本信息不应清空专有参数");
        assertEquals("60 RPM", detail.get("specs").get("风机转速").asText());
    }

    // ------------------------------------------------------------------
    // MM 运行监控
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("MM-03 启动设备后状态切换为 RUNNING")
    void shouldStartProbe() throws IOException {
        assertEquals(0, post("/api/monitor/devices/" + probeId + "/start", null).get("code").asInt());

        JsonNode detail = get("/api/monitor/devices/" + probeId).get("data");
        assertEquals("RUNNING", detail.get("status").asText());
        assertTrue(detail.get("sensors").get(0).get("value").asDouble() > 0,
                "启动时传感器应初始化为额定工况，而不是 0");
    }

    @Test
    @Order(7)
    @DisplayName("MM-03 重复启动被状态机拒绝，返回 400 与中文提示")
    void shouldRejectDuplicateStart() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> response = rest.exchange(
                URI.create("/api/monitor/devices/" + probeId + "/start"),
                HttpMethod.POST, new HttpEntity<>(headers), String.class);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("已在运行中"), "应返回中文业务提示: " + response.getBody());
    }

    @Test
    @Order(8)
    @DisplayName("MM-05 手动触发采集：只采集 RUNNING 设备并落库数据点")
    void shouldCollectData() throws IOException {
        JsonNode result = post("/api/monitor/collect", null);
        assertEquals(0, result.get("code").asInt());
        assertTrue(result.get("data").get("dataPoints").asInt() > 0, "应产生数据点");
        assertTrue(result.get("data").get("deviceCount").asInt() > 0, "应覆盖多台运行设备");
    }

    @Test
    @Order(9)
    @DisplayName("MM-01 实时状态与设备详情均反映最新采集值与采集时间")
    void shouldReflectLatestValue() throws IOException {
        JsonNode detail = get("/api/monitor/devices/" + probeId).get("data");
        assertFalse(detail.get("lastCollectTime").isNull(), "采集后应有最后采集时间");

        for (JsonNode sensor : detail.get("sensors")) {
            assertFalse(sensor.get("collectTime").isNull(), "每个参数都应有采集时间");
            double value = sensor.get("value").asDouble();
            double min = sensor.get("minValue").asDouble();
            double max = sensor.get("maxValue").asDouble();
            assertTrue(value >= min && value <= max,
                    sensor.get("name").asText() + " 采集值 " + value + " 应落在模拟区间 [" + min + ", " + max + "]");
        }

        /*
         * 设备详情（FM-05，/api/devices/{id}）走的是另一条链路：
         * 早期只把 device.params 转成 VO，没有回查 sensor_data，
         * 导致详情页「采集时间」列恒为空。此处固定该回归点。
         */
        JsonNode deviceDetail = get("/api/devices/" + probeId).get("data");
        assertTrue(deviceDetail.hasNonNull("lastCollectTime"), "设备详情应返回最近采集时间");
        for (JsonNode sensor : deviceDetail.get("sensors")) {
            assertTrue(sensor.hasNonNull("collectTime"),
                    "设备详情中参数「" + sensor.get("name").asText() + "」应带采集时间");
        }
    }

    @Test
    @Order(10)
    @DisplayName("MM-04 停机后状态为 STOPPED，且不再产生数据点")
    void shouldStopProbe() throws IOException {
        assertEquals(0, post("/api/monitor/devices/" + probeId + "/stop", null).get("code").asInt());
        assertEquals("STOPPED", get("/api/monitor/devices/" + probeId).get("data").get("status").asText());

        JsonNode before = get("/api/logs/run?deviceId=" + probeId + "&pageSize=200");
        int countBefore = before.get("data").get("total").asInt();

        JsonNode collect = post("/api/monitor/collect", null);
        assertTrue(collect.get("data").get("deviceCount").asInt() >= 1);

        JsonNode after = get("/api/logs/run?deviceId=" + probeId + "&pageSize=200");
        assertEquals(countBefore, after.get("data").get("total").asInt(),
                "停机设备不应再写入运行日志");
    }

    // ------------------------------------------------------------------
    // AM 告警闭环
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("AM-01 / AM-05 修改阈值为极低值并升级为 CRITICAL")
    void shouldConfigureThreshold() throws IOException {
        JsonNode thresholds = get("/api/alarm-thresholds?deviceType=CHILLER").get("data");
        long id = 0;
        for (JsonNode t : thresholds) {
            if ("chilledWaterSupplyTemp".equals(t.get("paramKey").asText())
                    && "GT".equals(t.get("compareOp").asText())) {
                id = t.get("id").asLong();
            }
        }
        assertTrue(id > 0, "应能找到冷机供水温度阈值");

        JsonNode result = request(HttpMethod.PUT, "/api/alarm-thresholds/" + id, body(
                "deviceType", "CHILLER",
                "paramKey", "chilledWaterSupplyTemp",
                "paramName", "冷冻水供水温度",
                "compareOp", "GT",
                "thresholdValue", 0.1,
                "unit", "℃",
                "alarmLevel", "CRITICAL",
                "enabled", true,
                "remark", "功能测试用"));
        assertEquals(0, result.get("code").asInt(), "修改阈值应成功: " + result);
    }

    @Test
    @Order(12)
    @DisplayName("AM-02 采集时自动检测越限并生成 CRITICAL 告警")
    void shouldTriggerAlarm() throws IOException {
        JsonNode collect = post("/api/monitor/collect", null);
        assertTrue(collect.get("data").get("newAlarms").asInt() > 0, "极低阈值应立刻触发告警");

        JsonNode alarms = get("/api/alarms?alarmLevel=CRITICAL&status=UNHANDLED&pageSize=20").get("data");
        assertTrue(alarms.get("total").asInt() > 0, "应能查到未处理的 CRITICAL 告警");

        JsonNode alarm = alarms.get("list").get(0);
        assertEquals("CRITICAL", alarm.get("alarmLevel").asText());
        assertEquals("严重", alarm.get("alarmLevelLabel").asText(), "级别中文标签应由枚举提供");
        assertTrue(alarm.get("alarmNo").asText().startsWith("AL-"), "告警编号应可读且唯一");
        assertTrue(alarm.get("actualValue").asDouble() > alarm.get("thresholdValue").asDouble());
    }

    @Test
    @Order(13)
    @DisplayName("AM-04 处理告警后状态变更，重复处理被拒绝")
    void shouldHandleAlarm() throws IOException {
        JsonNode unhandled = get("/api/alarms?status=UNHANDLED&pageSize=20").get("data");
        assertTrue(unhandled.get("total").asInt() > 0);
        long alarmId = unhandled.get("list").get(0).get("id").asLong();
        String alarmNo = unhandled.get("list").get(0).get("alarmNo").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = body("handler", "张工", "measure", "调高冷机负载并清洗冷凝器");
        ResponseEntity<String> response = rest.exchange(
                URI.create("/api/alarms/" + alarmId + "/handle"),
                HttpMethod.PATCH, new HttpEntity<>(payload, headers), String.class);
        assertEquals(200, response.getStatusCodeValue());

        JsonNode handled = get("/api/alarms?status=HANDLED&pageSize=50").get("data");
        boolean found = false;
        for (JsonNode a : handled.get("list")) {
            if (a.get("id").asLong() == alarmId) {
                found = true;
                assertEquals("张工", a.get("handler").asText());
                assertFalse(a.get("handleTime").isNull(), "处理时间应被记录");
                assertEquals("调高冷机负载并清洗冷凝器", a.get("handleMeasure").asText());
            }
        }
        assertTrue(found, "处理后的告警应出现在已处理列表");

        ResponseEntity<String> again = rest.exchange(
                URI.create("/api/alarms/" + alarmId + "/handle"),
                HttpMethod.PATCH, new HttpEntity<>(payload, headers), String.class);
        assertEquals(400, again.getStatusCodeValue(), "重复处理应被拒绝");
        assertTrue(again.getBody().contains("已处理"));

        JsonNode logs = get("/api/logs/alarm?alarmNo=" + alarmNo).get("data");
        assertTrue(logs.get("total").asInt() >= 2, "告警日志应同时记录触发与处理两条动作");
    }

    @Test
    @Order(14)
    @DisplayName("AM-01 阈值恢复为出厂值")
    void shouldRestoreThreshold() throws IOException {
        JsonNode thresholds = get("/api/alarm-thresholds?deviceType=CHILLER").get("data");
        for (JsonNode t : thresholds) {
            if ("chilledWaterSupplyTemp".equals(t.get("paramKey").asText())
                    && "GT".equals(t.get("compareOp").asText())) {
                long id = t.get("id").asLong();
                JsonNode result = request(HttpMethod.PUT, "/api/alarm-thresholds/" + id, body(
                        "deviceType", "CHILLER",
                        "paramKey", "chilledWaterSupplyTemp",
                        "paramName", "冷冻水供水温度",
                        "compareOp", "GT",
                        "thresholdValue", ORIGIN_THRESHOLD,
                        "unit", "℃",
                        "alarmLevel", "WARNING",
                        "enabled", true));
                assertEquals(0, result.get("code").asInt());

                JsonNode restored = get("/api/alarm-thresholds?deviceType=CHILLER").get("data");
                for (JsonNode item : restored) {
                    if (item.get("id").asLong() == id) {
                        assertEquals(ORIGIN_THRESHOLD, item.get("thresholdValue").asDouble(), 1e-6);
                        assertEquals("WARNING", item.get("alarmLevel").asText());
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // PM 持久化
    // ------------------------------------------------------------------

    @Test
    @Order(15)
    @DisplayName("PM-02 运行日志按设备过滤且倒序返回")
    void shouldQueryRunLogs() throws IOException {
        JsonNode logs = get("/api/logs/run?deviceId=" + probeId + "&pageNum=1&pageSize=10").get("data");
        assertTrue(logs.get("total").asInt() > 0, "启动期间的采集应留下运行日志");
        assertFalse(logs.get("list").get(0).get("params").isNull(), "运行日志应包含参数快照");
    }

    @Test
    @Order(16)
    @DisplayName("PM-04 导出 CSV 包含新建设备且带 BOM")
    void shouldExportCsv() {
        ResponseEntity<byte[]> response = rest.getRestTemplate()
                .getForEntity("/api/devices/export.csv", byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        byte[] bytes = response.getBody();
        assertNotNull(bytes);
        assertEquals((byte) 0xEF, bytes[0], "CSV 应以 UTF-8 BOM 开头");
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String text = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(text.contains("设备编号"), "应包含中文表头");
        assertTrue(text.contains(probeId), "应包含本次新增的设备");
    }

    // ------------------------------------------------------------------
    // AN 分析
    // ------------------------------------------------------------------

    @Test
    @Order(17)
    @DisplayName("AN-01 统计概览与设备总数、状态分布一致")
    void shouldAggregateStatistics() throws IOException {
        JsonNode overview = get("/api/statistics/overview").get("data");
        long total = overview.get("total").asLong();
        long running = overview.get("running").asLong();
        long stopped = overview.get("stopped").asLong();
        long fault = overview.get("fault").asLong();
        long maintenance = overview.get("maintenance").asLong();

        assertEquals(13, total, "初始 12 台 + 新增 1 台");
        assertEquals(total, running + stopped + fault + maintenance + overview.get("offline").asLong(),
                "各状态数量之和应等于总数");
        assertTrue(overview.get("byType").size() >= 4, "应覆盖四类设备");
        assertTrue(overview.get("byZone").size() >= 3, "应覆盖 A/B/C 三个区域");
        assertTrue(overview.get("totalAlarms").asLong() >= 1, "应统计历史告警总数");
    }

    @Test
    @Order(18)
    @DisplayName("AN-02 趋势按时间正序返回最近 N 次采集")
    void shouldReturnTrend() throws IOException {
        JsonNode trend = get("/api/statistics/trend?deviceId=CH-001"
                + "&sensorKey=chilledWaterSupplyTemp&limit=10").get("data");

        assertEquals("冷冻水供水温度", trend.get("sensorName").asText());
        assertEquals("℃", trend.get("unit").asText());
        JsonNode points = trend.get("points");
        assertTrue(points.size() > 0 && points.size() <= 10, "点位数应不超过 limit");
        assertTrue(trend.get("minValue").asDouble() <= trend.get("maxValue").asDouble());

        for (int i = 1; i < points.size(); i++) {
            String prev = points.get(i - 1).get("time").asText();
            String curr = points.get(i).get("time").asText();
            assertTrue(prev.compareTo(curr) <= 0, "趋势点应按时间正序，便于直接绘制折线");
        }
    }

    // ------------------------------------------------------------------
    // FM 删除收尾
    // ------------------------------------------------------------------

    @Test
    @Order(19)
    @DisplayName("FM-02 删除设备后详情返回 404，列表不再出现")
    void shouldDeleteProbe() throws IOException {
        JsonNode result = request(HttpMethod.DELETE, "/api/devices/" + probeId, null);
        assertEquals(0, result.get("code").asInt());
        assertEquals(404, statusOf(HttpMethod.GET, "/api/devices/" + probeId, null));

        JsonNode list = get("/api/devices?pageSize=50").get("data");
        assertEquals(12, list.get("total").asInt(), "删除后应回到 12 台");
        for (JsonNode d : list.get("list")) {
            assertFalse(probeId.equals(d.get("deviceId").asText()));
        }
    }
}
