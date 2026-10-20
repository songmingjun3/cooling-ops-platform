# -*- coding: utf-8 -*-
"""端到端功能验证脚本（M3 ~ M6 全接口）。

覆盖需求文档 FM / MM / AM / PM / AN 五大模块的接口级验证。
用法：python scripts/e2e_verify.py [base_url]
默认 base_url = http://127.0.0.1:18081
"""
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:18081"

PASSED = []
FAILED = []


def call(method, path, body=None, raw=False):
    """发起一次 HTTP 请求，返回 (status, payload)。"""
    url = BASE + path
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=UTF-8"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            text = resp.read().decode("utf-8")
            return resp.status, (text if raw else json.loads(text))
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8")
        try:
            return e.code, json.loads(text)
        except ValueError:
            return e.code, text


def check(name, cond, detail=""):
    if cond:
        PASSED.append(name)
        print("  [PASS] %s %s" % (name, detail))
    else:
        FAILED.append(name)
        print("  [FAIL] %s %s" % (name, detail))


def section(title):
    print("\n=== %s ===" % title)


# ---------------------------------------------------------------- MM 运行监控
section("MM 运行监控（MM-01 ~ MM-05）")

st, r = call("GET", "/api/monitor/devices?deviceType=CHILLER")
check("MM-02 按类型查看运行状态", st == 200 and r["code"] == 0 and len(r["data"]) > 0,
      "-> %d 台冷机" % (len(r["data"]) if st == 200 and r["data"] else 0))

st, r = call("GET", "/api/monitor/devices/CH-001")
ok = st == 200 and r["code"] == 0 and r["data"]["deviceId"] == "CH-001"
sensors = r["data"].get("sensors") or [] if ok else []
check("MM-01 查看设备实时状态", ok and len(sensors) > 0,
      "-> %s 传感器 %d 个, 最后采集 %s" % (r["data"].get("deviceName"), len(sensors),
                                          r["data"].get("lastCollectTime")) if ok else "")

st, r = call("GET", "/api/monitor/devices/NOT-EXIST")
check("MM-01 不存在设备返回 404", st == 404 and r["code"] == 404,
      "-> %s" % (r.get("message") if isinstance(r, dict) else r))

# 状态机用「新建一台泵 → 启动 → 重复启动 → 停机 → 删除」的闭环验证，
# 避免依赖 data.sql 中既有设备的初始状态，同时顺带回归 FM-01 / FM-05。
st, r = call("POST", "/api/devices", {
    "deviceName": "E2E验证泵", "deviceType": "PUMP", "zone": "C区",
    "manufacturer": "格兰富", "model": "NKG-200", "installDate": "2025-01-10",
    "params": {"pumpType": "CHILLED_WATER", "flowRate": 80.0, "head": 30.0,
               "power": 22.0, "speed": 75}
})
probe_id = r["data"] if st == 200 and r["code"] == 0 else None
check("FM-01 新增设备（用于状态机验证）", probe_id is not None, "-> %s" % probe_id)

if probe_id:
    st, r = call("POST", "/api/monitor/devices/%s/start" % probe_id)
    check("MM-03 启动设备", st == 200 and r["code"] == 0, "-> %s" % probe_id)
    st, r = call("GET", "/api/monitor/devices/%s" % probe_id)
    check("MM-03 启动后状态为 RUNNING",
          st == 200 and r["data"]["status"] == "RUNNING",
          "-> %s" % (r["data"]["status"] if st == 200 else ""))

    st, r = call("POST", "/api/monitor/devices/%s/start" % probe_id)
    check("MM-03 重复启动被拒绝（状态机）", st == 400 and r["code"] == 400,
          "-> %s" % (r.get("message") if isinstance(r, dict) else r))

    st, r = call("POST", "/api/monitor/devices/%s/stop" % probe_id)
    check("MM-04 停机设备", st == 200 and r["code"] == 0, "")
    st, r = call("GET", "/api/monitor/devices/%s" % probe_id)
    check("MM-04 停机后状态为 STOPPED",
          st == 200 and r["data"]["status"] == "STOPPED",
          "-> %s" % (r["data"]["status"] if st == 200 else ""))

    st, r = call("POST", "/api/monitor/devices/%s/stop" % probe_id)
    check("MM-04 重复停机被拒绝（状态机）", st == 400 and r["code"] == 400,
          "-> %s" % (r.get("message") if isinstance(r, dict) else r))

st, r = call("POST", "/api/monitor/collect")
check("MM-05 手动触发采集",
      st == 200 and r["code"] == 0 and r["data"]["dataPoints"] > 0,
      "-> 设备 %s 台 / 数据点 %s 条 / 新告警 %s 条 / 耗时 %s ms"
      % (r["data"]["deviceCount"], r["data"]["dataPoints"], r["data"]["newAlarms"],
         r["data"]["costMillis"]) if st == 200 and r["code"] == 0 else "")

st, r = call("GET", "/api/monitor/devices?deviceType=BAD")
check("MM-02 非法设备类型返回 400", st == 400 or (st == 200 and r["code"] == 400),
      "-> %s" % (r.get("message") if isinstance(r, dict) else r))

# ---------------------------------------------------------------- AM 告警管理
section("AM 告警管理（AM-01 ~ AM-05）")

st, r = call("GET", "/api/alarms?pageNum=1&pageSize=10")
total_alarms = r["data"]["total"] if st == 200 and r["code"] == 0 else 0
check("AM-03 分页查询告警列表", st == 200 and r["code"] == 0,
      "-> 共 %d 条告警" % total_alarms)

st, r = call("GET", "/api/alarms?status=UNHANDLED&pageSize=50")
unhandled = r["data"]["list"] if st == 200 and r["code"] == 0 else []
check("AM-03 按状态过滤", st == 200 and r["code"] == 0,
      "-> 未处理 %d 条" % len(unhandled))
check("AM-02 自动告警检测已产生告警", total_alarms > 0, "-> %d 条" % total_alarms)

if unhandled:
    alarm_id = unhandled[0]["id"]
    alarm_no = unhandled[0]["alarmNo"]
    st, r = call("PATCH", "/api/alarms/%s/handle" % alarm_id,
                 {"handler": "张工", "measure": "已调高冷机负载并清洗冷凝器"})
    check("AM-04 处理告警", st == 200 and r["code"] == 0, "-> %s" % alarm_no)
    st, r = call("GET", "/api/alarms?status=HANDLED&pageSize=50")
    handled = r["data"]["list"] if st == 200 and r["code"] == 0 else []
    check("AM-04 处理结果是已处理",
          any(a["id"] == alarm_id for a in handled), "-> 已处理 %d 条" % len(handled))
    st, r = call("PATCH", "/api/alarms/%s/handle" % alarm_id,
                 {"handler": "张工", "measure": "重复处理"})
    check("AM-04 重复处理被拒绝", st == 400 and r["code"] == 400,
          "-> %s" % (r.get("message") if isinstance(r, dict) else r))
    st, r = call("PATCH", "/api/alarms/%s/handle" % alarm_id, {"handler": "", "measure": ""})
    check("AM-04 处理人为空被拦截", st == 400 and r["code"] == 400,
          "-> %s" % (r.get("message") if isinstance(r, dict) else r))

st, r = call("GET", "/api/alarms?alarmLevel=CRITICAL&pageNum=1&pageSize=5")
check("AM-03 按级别过滤", st == 200 and r["code"] == 0, "")

# ---------------------------------------------------------------- 阈值管理
section("AM-01 阈值配置")

st, r = call("GET", "/api/alarm-thresholds")
check("AM-01 查询全部阈值", st == 200 and r["code"] == 0 and len(r["data"]) >= 7,
      "-> %d 条" % (len(r["data"]) if st == 200 and r["code"] == 0 else 0))

st, r = call("GET", "/api/alarm-thresholds?deviceType=CHILLER")
check("AM-01 按设备类型查询阈值", st == 200 and r["code"] == 0, "")

st, r = call("POST", "/api/alarm-thresholds", {
    "deviceType": "CHILLER", "paramKey": "chilledWaterReturnTemp",
    "paramName": "冷冻水回水温度", "compareOp": "LT", "thresholdValue": 10.5,
    "unit": "℃", "alarmLevel": "WARNING", "enabled": True, "remark": "E2E 验证用"
})
new_id = r["data"] if st == 200 and r["code"] == 0 else None
check("AM-01 新增阈值", new_id is not None,
      "-> id=%s" % new_id if new_id else "-> %s" % (r.get("message") if isinstance(r, dict) else r))

if new_id:
    st, r = call("PUT", "/api/alarm-thresholds/%s" % new_id, {
        "deviceType": "CHILLER", "paramKey": "chilledWaterReturnTemp",
        "paramName": "冷冻水回水温度", "compareOp": "LT", "thresholdValue": 10.0,
        "unit": "℃", "alarmLevel": "CRITICAL", "enabled": True, "remark": "E2E 验证已更新"
    })
    check("AM-01 修改阈值", st == 200 and r["code"] == 0,
          "-> %s" % (r.get("message") if isinstance(r, dict) and r["code"] != 0 else ""))

    st, r = call("PATCH", "/api/alarm-thresholds/%s/enabled?enabled=false" % new_id)
    check("AM-01 停用阈值", st == 200 and r["code"] == 0, "")

    st, r = call("PATCH", "/api/alarm-thresholds/%s/enabled?enabled=true" % new_id)
    check("AM-01 启用阈值", st == 200 and r["code"] == 0, "")

    st, r = call("DELETE", "/api/alarm-thresholds/%s" % new_id)
    check("AM-01 删除阈值", st == 200 and r["code"] == 0, "")

    st, r = call("GET", "/api/alarm-thresholds?deviceType=CHILLER")
    check("AM-01 删除后不在列表中",
          all(t["id"] != new_id for t in (r["data"] if st == 200 else [])), "")

st, r = call("POST", "/api/alarm-thresholds", {
    "deviceType": "CHILLER", "paramKey": "chilledWaterSupplyTemp", "compareOp": "GT",
    "thresholdValue": 1.0
})
check("AM-01 缺少告警级别被拦截", st == 400 and r["code"] == 400,
      "-> %s" % (r.get("message") if isinstance(r, dict) else r))

st, r = call("POST", "/api/alarm-thresholds", {
    "deviceType": "CHILLER", "paramKey": "notASensor", "compareOp": "GT",
    "thresholdValue": 1.0, "alarmLevel": "WARNING"
})
check("AM-01 非法参数键被拦截", st == 400 and r["code"] == 400,
      "-> %s" % (r.get("message") if isinstance(r, dict) else r))

# ---------------------------------------------------------------- AM-05 告警分级
section("AM-05 告警分级（WARNING / CRITICAL）")

st, r = call("GET", "/api/alarm-thresholds?deviceType=CHILLER")
thresholds = r["data"] if st == 200 and r["code"] == 0 else []
origin = None
for t in thresholds:
    if t["paramKey"] == "chilledWaterSupplyTemp" and t["compareOp"] == "GT":
        origin = t
        break

if origin:
    levels = sorted({t["alarmLevel"] for t in thresholds})
    check("AM-05 阈值可配置为 WARNING / CRITICAL 两级",
          "WARNING" in levels and "CRITICAL" in levels, "-> %s" % levels)

    # 先处理掉存量未处理告警，避免 5 分钟抑制窗口挡住新告警
    st, r = call("GET", "/api/alarms?status=UNHANDLED&pageSize=100")
    for a in (r["data"]["list"] if st == 200 and r["code"] == 0 else []):
        call("PATCH", "/api/alarms/%s/handle" % a["id"],
             {"handler": "E2E", "measure": "分级验证前清理"})

    # 把供水温度阈值临时改到 0.1℃ 并升级为 CRITICAL，触发一次采集
    st, r = call("PUT", "/api/alarm-thresholds/%s" % origin["id"], {
        "deviceType": "CHILLER", "paramKey": origin["paramKey"],
        "paramName": origin["paramName"], "compareOp": "GT",
        "thresholdValue": 0.1, "unit": origin["unit"],
        "alarmLevel": "CRITICAL", "enabled": True, "remark": "AM-05 分级验证"
    })
    call("POST", "/api/monitor/collect")
    st, r = call("GET", "/api/alarms?alarmLevel=CRITICAL&pageSize=10")
    critical = r["data"]["list"] if st == 200 and r["code"] == 0 else []
    hit = [a for a in critical if a["deviceId"] == "CH-001"]
    check("AM-05 CRITICAL 级告警由阈值配置触发并落库", len(hit) > 0,
          "-> %s %s（阈值 %s，实测 %s）"
          % (hit[0]["alarmNo"], hit[0]["alarmLevelLabel"], hit[0]["thresholdValue"],
             hit[0]["actualValue"]) if hit else "-> 未产生 CRITICAL 告警")

    # 恢复原阈值配置
    call("PUT", "/api/alarm-thresholds/%s" % origin["id"], {
        "deviceType": "CHILLER", "paramKey": origin["paramKey"],
        "paramName": origin["paramName"], "compareOp": origin["compareOp"],
        "thresholdValue": origin["thresholdValue"], "unit": origin["unit"],
        "alarmLevel": origin["alarmLevel"], "enabled": origin["enabled"],
        "remark": origin["remark"]
    })
    st, r = call("GET", "/api/alarm-thresholds?deviceType=CHILLER")
    restored = [t for t in (r["data"] if st == 200 and r["code"] == 0 else [])
                if t["id"] == origin["id"]]
    check("AM-05 验证后阈值配置已还原",
          restored and restored[0]["alarmLevel"] == origin["alarmLevel"],
          "-> 级别 %s，阈值 %s" % (restored[0]["alarmLevel"], restored[0]["thresholdValue"])
          if restored else "")

# ---------------------------------------------------------------- AN 数据分析
section("AN 数据分析（AN-01 / AN-02）")

st, r = call("GET", "/api/statistics/overview")
ok = st == 200 and r["code"] == 0
if ok:
    d = r["data"]
    check("AN-01 统计概览", d["total"] > 0,
          "-> 总数 %s / 运行 %s / 停机 %s / 未处理告警 %s"
          % (d["total"], d["running"], d["stopped"], d["unhandledAlarms"]))
    check("AN-01 按类型分组非空", len(d.get("byType") or []) > 0,
          "-> %s" % [x["name"] for x in (d.get("byType") or [])])
    check("AN-01 按区域分组非空", len(d.get("byZone") or []) > 0,
          "-> %s" % [x["name"] for x in (d.get("byZone") or [])])
else:
    check("AN-01 统计概览", False)

st, r = call("GET", "/api/statistics/trend?deviceId=CH-001&sensorKey=chilledWaterSupplyTemp&limit=20")
ok = st == 200 and r["code"] == 0
if ok:
    d = r["data"]
    check("AN-02 传感器趋势查询", len(d.get("points") or []) > 0,
          "-> %s %s 共 %d 点, 区间 [%s, %s] %s"
          % (d["deviceName"], d["sensorName"], len(d["points"]), d["minValue"], d["maxValue"], d["unit"]))
    check("AN-02 趋势点含时间与数值",
          all(p.get("time") and p.get("value") is not None for p in d["points"]), "")
else:
    check("AN-02 传感器趋势查询", False)

st, r = call("GET", "/api/statistics/trend?deviceId=CH-001&limit=20")
check("AN-02 不指定传感器时返回全部参数趋势", st == 200 and r["code"] == 0,
      "-> %s" % (r["data"].get("sensorKey") if st == 200 and r["code"] == 0 else ""))

st, r = call("GET", "/api/statistics/trend?deviceId=CH-001&limit=999")
check("AN-02 limit 越界被拦截", st == 400 and r["code"] == 400,
      "-> %s" % (r.get("message") if isinstance(r, dict) else r))

st, r = call("GET", "/api/statistics/trend?deviceId=NO-1&limit=10")
check("AN-02 不存在设备返回 404", st == 404 and r["code"] == 404,
      "-> %s" % (r.get("message") if isinstance(r, dict) else r))

# ---------------------------------------------------------------- PM 持久化
section("PM 数据持久化（PM-01 ~ PM-04）")

st, r = call("GET", "/api/logs/run?pageNum=1&pageSize=5")
check("PM-02 运行日志分页查询", st == 200 and r["code"] == 0 and r["data"]["total"] > 0,
      "-> 共 %d 条" % (r["data"]["total"] if st == 200 and r["code"] == 0 else 0))

st, r = call("GET", "/api/logs/run?deviceId=CH-001&pageSize=5")
check("PM-02 按设备号过滤运行日志", st == 200 and r["code"] == 0, "")

st, r = call("GET", "/api/logs/alarm?pageNum=1&pageSize=5")
check("PM-03 告警日志分页查询", st == 200 and r["code"] == 0 and r["data"]["total"] > 0,
      "-> 共 %d 条" % (r["data"]["total"] if st == 200 and r["code"] == 0 else 0))

if total_alarms > 0:
    st, r0 = call("GET", "/api/alarms?pageSize=1")
    no = r0["data"]["list"][0]["alarmNo"]
    st, r = call("GET", "/api/logs/alarm?alarmNo=%s" % urllib.parse.quote(no))
    check("PM-03 按告警编号追踪日志", st == 200 and r["code"] == 0, "-> %s" % no)

st, body = call("GET", "/api/devices/export.csv", raw=True)
ok = st == 200 and isinstance(body, str) and "CH-001" in body and body.startswith("\ufeff")
check("PM-04 设备清单导出 CSV（含 BOM，Excel 直开不乱码）", ok,
      "-> %d 字节, 行数 %d" % (len(body.encode("utf-8")) if isinstance(body, str) else 0,
                               body.count("\r\n") if isinstance(body, str) else 0))

# ---------------------------------------------------------------- FM 回归
section("FM 设备管理回归")

st, r = call("GET", "/api/devices?pageNum=1&pageSize=50")
check("FM-05 列出所有设备（分页）", st == 200 and r["code"] == 0,
      "-> 共 %d 台" % (r["data"]["total"] if st == 200 else 0))

st, r = call("GET", "/api/devices/group-by-zone")
check("FM-06 按区域分组查看", st == 200 and r["code"] == 0 and len(r["data"]) > 0,
      "-> %d 个区域" % (len(r["data"]) if st == 200 and r["code"] == 0 else 0))

st, r = call("GET", "/api/devices?deviceType=PUMP&status=RUNNING&pageSize=50")
check("FM-04 多条件组合查询", st == 200 and r["code"] == 0,
      "-> 命中 %d 台运行中的泵" % (r["data"]["total"] if st == 200 else 0))

if probe_id:
    st, r = call("PUT", "/api/devices/%s" % probe_id, {
        "deviceName": "E2E验证泵-改名", "zone": "B区",
        "manufacturer": "格兰富", "model": "NKG-201", "installDate": "2025-01-10"
    })
    check("FM-03 修改设备基本信息", st == 200 and r["code"] == 0,
          "-> %s" % (r.get("message") if isinstance(r, dict) and r["code"] != 0 else ""))
    st, r = call("GET", "/api/devices/%s" % probe_id)
    ok = st == 200 and r["code"] == 0
    sensor_names = [s["name"] for s in (r["data"].get("sensors") or [])] if ok else []
    check("FM-03 修改结果已生效且未污染运行参数",
          ok and r["data"]["deviceName"] == "E2E验证泵-改名" and r["data"]["zone"] == "B区"
          and r["data"]["model"] == "NKG-201"
          and str(r["data"]["specs"].get("扬程", "")).find("30.0") >= 0
          and "当前流量" in sensor_names,
          "-> %s / %s / 型号 %s / 扬程 %s / 参数 %s"
          % (r["data"]["deviceName"], r["data"]["zone"], r["data"]["model"],
             r["data"]["specs"].get("扬程"), sensor_names) if ok else "")

    st, r = call("DELETE", "/api/devices/%s" % probe_id)
    check("FM-02 删除设备", st == 200 and r["code"] == 0, "")
    st, r = call("GET", "/api/devices/%s" % probe_id)
    check("FM-02 删除后再查返回 404", st == 404 and r["code"] == 404,
          "-> %s" % (r.get("message") if isinstance(r, dict) else r))

# ---------------------------------------------------------------- 汇总
print("\n" + "=" * 60)
print("通过 %d 项 / 失败 %d 项 / 合计 %d 项" % (len(PASSED), len(FAILED), len(PASSED) + len(FAILED)))
if FAILED:
    print("失败用例：")
    for name in FAILED:
        print("  - " + name)
    sys.exit(1)
print("全部通过")
