# 数据中心冷却系统智能运维平台

> Data Center Cooling System Intelligent Operation Platform

一套面向数据中心水冷冷却系统的设备台账管理与运行监控平台，覆盖**设备管理、实时监控、告警处理、数据持久化、分析展示**五大业务模块。系统模拟真实冷却系统的「设备管理层 + 监控预警层」，不涉及硬件控制，传感器数据由系统按合理区间模拟生成。

本仓库为「面向对象程序设计（Java）」课程项目的工程骨架与开发仓库，需求依据见 [`docs/requirements/数据中心冷却系统智能运维平台——系统需求文档.docx`](docs/requirements/)。

---

## 一、文档信息

| 项目 | 内容 |
| --- | --- |
| 系统名称 | 数据中心冷却系统智能运维平台 |
| 需求文档版本 | V1.0 |
| 适用课程 | 面向对象程序设计（Java） |
| 技术栈 | Java 8 + Spring Boot 2.7 + Spring MVC + MyBatis + SQLite 3 |
| 构建方式 | Maven（单模块，单体应用） |

---

## 二、业务场景

某大型数据中心配备一套水冷冷却系统，包含以下核心设备：

| 设备 | 说明 | 编号前缀 |
| --- | --- | --- |
| 冷水机组 Chiller | 提供冷冻水，冷却系统的「心脏」 | `CH` |
| 冷却塔 Cooling Tower | 将热量排放至大气 | `CT` |
| 水泵 Pump | 驱动冷却水 / 冷冻水循环（含冷冻水泵、冷却水泵） | `PM` |
| 精密空调 Cooling Unit | 向 IT 设备提供冷量 | `CU` |

运维人员（本系统仅「运维管理员」一种角色，不做多用户与权限管理）通过平台完成：设备台账维护、实时参数查看、异常告警处理、历史数据分析。

---

## 三、功能需求

### 3.1 设备管理模块（FM）

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| FM-01 | 添加设备 | 录入新设备信息，**自动生成设备编号**（前缀-序号，如 `CH-001`） |
| FM-02 | 删除设备 | 根据设备编号删除（需二次确认） |
| FM-03 | 修改设备 | 仅修改基本信息，不允许修改运行参数 |
| FM-04 | 查询设备 | 按编号、名称、类型、区域、状态多条件组合查询 |
| FM-05 | 列出所有设备 | 表格展示全部设备清单与关键参数摘要 |
| FM-06 | 按区域分组查看 | 按 `zone` 分组展示设备 |

### 3.2 设备运行监控模块（MM）

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| MM-01 | 查看设备实时状态 | 展示指定设备的全部运行参数 |
| MM-02 | 批量查看某类设备 | 按设备类型列出该类全部设备参数 |
| MM-03 | 启动设备 | 状态切换为 `RUNNING`，执行启动逻辑 |
| MM-04 | 停机设备 | 状态切换为 `STOPPED`，执行停机逻辑 |
| MM-05 | 模拟数据采集 | 默认每 3 秒为所有 `RUNNING` 设备生成合理区间内的模拟传感器数据 |

### 3.3 告警管理模块（AM）

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| AM-01 | 阈值配置 | 按设备类型 / 参数配置告警阈值，**不硬编码** |
| AM-02 | 自动告警检测 | 采集时自动比对阈值并触发告警 |
| AM-03 | 查看告警列表 | 未处理 / 已处理告警记录 |
| AM-04 | 处理告警 | 记录处理人、处理时间、处理措施 |
| AM-05 | 告警分级 | `WARNING`（警告）/ `CRITICAL`（严重） |

默认告警示例：冷水机组冷冻水供水温度 > 16℃ → `CRITICAL`；冷却塔出水温度 > 35℃ → `WARNING`；水泵功率相对额定值波动 > ±20% → `WARNING`。

### 3.4 数据持久化模块（PM）

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| PM-01 | 设备数据持久化 | 设备信息持久化并在启动时自动加载 |
| PM-02 | 运行日志记录 | 每次采集写入设备运行参数快照 |
| PM-03 | 告警日志记录 | 告警触发与处理均写入告警日志 |
| PM-04 | 数据导出 | 设备清单导出为 CSV |

> **实现说明**：需求文档原方案为「程序退出时保存到文件」，本项目采用 **SQLite 实时持久化**替代——每次写操作即落库、应用启动即加载，天然满足「退出保存 / 启动加载」的要求，且避免了文件损坏与并发写入问题。

### 3.5 数据分析与展示模块（AN）

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| AN-01 | 设备运行统计 | 各类设备数量、运行状态分布 |
| AN-02 | 温度趋势简图 | 某设备最近 N 次采集的温度变化曲线 |

---

## 四、技术选型

| 层次 | 技术 | 版本 | 说明 |
| --- | --- | --- | --- |
| 运行环境 | JDK | 1.8 | 与本机默认 `JAVA_HOME` 对齐，开箱即可构建 |
| 框架 | Spring Boot | 2.7.18 | 集成 Spring MVC、自动装配、内嵌 Tomcat |
| Web | Spring MVC | 5.3.x | `@RestController` 提供 RESTful 接口，同时托管静态前端 |
| 持久层 | MyBatis | 2.3.2（starter） | XML 映射 + 注解混用，SQL 可控、便于教学讲解 |
| 数据库 | SQLite | 3.45.3.0（JDBC） | 单文件、零安装，`./data/cooling-ops.db` |
| 连接池 | HikariCP | Spring Boot 内置 | 因 SQLite 单写者模型，连接池固定为 1 |
| 参数校验 | Hibernate Validator | Spring Boot 内置 | JSR-380 注解式校验，保证健壮性 |
| 工具 | Lombok | Spring Boot 内置 | 省略 getter/setter/日志声明 |
| 前端 | 原生 HTML + CSS + JS + ECharts | — | 静态资源由 Spring MVC 托管，免 Node 构建，用于 AN-02 趋势图 |

> **升级路径**：若后续环境统一到 JDK 17+，只需把 `pom.xml` 的 `spring-boot-starter-parent` 升级到 `3.x`、`java.version` 改为 `17`，并将 `javax.*` 校验注解替换为 `jakarta.*`，业务代码无需改动。

---

## 五、系统分层架构

```
┌─────────────────────────────────────────────────────────┐
│ 前端页面（static/，HTML + ECharts）                       │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP / JSON
┌───────────────────────▼─────────────────────────────────┐
│ Controller  controller/  —— 参数校验、协议转换             │
├─────────────────────────────────────────────────────────┤
│ Service     service/ + service/impl/ —— 业务编排与事务     │
│             rule/   告警规则策略    scheduler/ 定时采集     │
├─────────────────────────────────────────────────────────┤
│ Mapper      mapper/ + resources/mapper/*.xml —— SQL 映射  │
├─────────────────────────────────────────────────────────┤
│ Domain      entity/  —— Device 抽象基类与四类设备子类       │
├─────────────────────────────────────────────────────────┤
│ Storage     SQLite  ./data/cooling-ops.db                 │
└─────────────────────────────────────────────────────────┘
```

**面向对象设计要点**

- `entity/Device` 为抽象基类，承载 7 项公共属性（`deviceId`、`deviceName`、`manufacturer`、`model`、`installDate`、`status`、`zone`），并声明 `getDeviceType()`、`getSensors()`、`getKeyParameterSummary()` 三个抽象方法；
- `Chiller`、`CoolingTower`、`Pump`、`CoolingUnit` 继承 `Device`，各自持有专有属性与 `Sensor` 传感器；
- `Sensor` 封装量测值、单位与合理波动区间，`MM-05` 模拟采集在区间内随机波动；
- 告警规则采用策略模式（`rule/`），新增规则或调整阈值无需修改采集与检测主流程，满足开闭原则。

**设备参数的读写链路（单表继承落地方式）**

```
HTTP/JSON ──► DeviceCreateDTO.params(Map)
                    │  DeviceFactory.applyParams()  ← 类型转换 + 合法性校验
                    ▼
              Chiller / CoolingTower / Pump / CoolingUnit
                    │  DeviceConverter.writeParams()  ← 只把「值」写进 JSON
                    ▼
        device 表：公共属性成列，params 存 {"coolingCapacity":130.0,...}
                    │  DeviceConverter.toDevice() + SensorCatalog 补齐名称/单位/区间
                    ▼
              领域对象（多态）──► DeviceVO / DeviceDetailVO
```

传感器在 { } 中只存数值，名称、单位与合理区间由 `SensorCatalog` 统一提供，避免同一参数在每条设备记录里重复维护；`DeviceIdGenerator` 按「同前缀最大序号 + 1」生成编号并加锁，保证并发下不重复。

---

## 六、目录结构

```
大数据冷却系统运维平台/
├── docs/                                  # 项目文档
│   ├── requirements/                      # 原始需求文档（docx，唯一需求依据）
│   │   └── 数据中心冷却系统智能运维平台——系统需求文档.docx
│   └── design/                            # 设计文档（数据库设计、接口设计，待补充）
│
├── data/                                  # SQLite 数据文件目录（*.db 已被 gitignore，目录保留）
├── logs/                                  # 运行日志输出目录（*.log 已被 gitignore）
├── export/                                # CSV 导出目录（运行时生成）
├── scripts/
│   ├── e2e_verify.py                      # 端到端接口巡检脚本（53 个检查点）
│   └── e2e-ui.js                          # 端到端界面测试脚本（61 个检查点，CDP 驱动真实浏览器）
│
├── src/main/java/com/cooling/platform/
│   ├── CoolingPlatformApplication.java    # 启动类
│   ├── common/                            # 通用能力（跨模块复用）
│   │   ├── constant/                      # DeviceConstants 常量、SensorCatalog 传感器元数据
│   │   ├── enums/                         # 枚举：DeviceStatus / DeviceType / TowerType
│   │   │                                  #       PumpType / AlarmLevel / AlarmStatus / CompareOp
│   │   ├── exception/                     # BusinessException、GlobalExceptionHandler
│   │   ├── response/                      # Result 统一响应、PageResult 分页结果
│   │   └── util/                          # 工具：CsvExporter
│   ├── config/                            # 配置：Jackson 格式、MyBatis 扫描、SQLite 类型处理器
│   │   └── typehandler/                   # LocalDate / LocalDateTime ↔ SQLite 文本
│   ├── controller/                        # Spring MVC 控制层（REST 接口，按模块拆分）
│   ├── service/                           # 业务接口（按模块拆分）
│   │   └── impl/                          # 业务实现
│   ├── mapper/                            # MyBatis Mapper 接口
│   ├── entity/                            # 领域模型 Device 及四个子类、Sensor、DevicePO、各数据实体
│   ├── dto/                               # 请求对象：DeviceCreateDTO、AlarmQueryDTO 等
│   ├── vo/                                # 响应对象：DeviceVO、AlarmVO、TrendVO、StatisticsVO 等
│   ├── converter/                         # Entity ↔ DTO/VO 转换（含 params JSON 编解码）
│   ├── scheduler/                         # 定时任务：DataAcquisitionTask（MM-05 + 过期清理）
│   ├── rule/                              # 告警规则策略：AlarmRule + 注册表 + 三种实现
│   └── factory/                           # DeviceFactory 设备工厂、DeviceIdGenerator 编号生成
│
├── src/main/resources/
│   ├── application.yml                    # 主配置（端口、数据源、MyBatis、业务参数）
│   ├── application-dev.yml                # 开发环境覆盖配置
│   ├── logback-spring.xml                 # 日志配置（控制台 + 滚动文件）
│   ├── db/
│   │   ├── schema.sql                     # 建表脚本（幂等，启动自动执行）
│   │   └── data.sql                       # 初始化示例数据（幂等，INSERT OR IGNORE）
│   ├── mapper/                            # MyBatis XML 映射文件
│   └── static/                            # 前端静态资源
│       ├── index.html                     # 主界面（五个页签 + 五个弹窗）
│       ├── css/app.css                    # 样式（CSS 变量设计令牌 + 三档响应式）
│       ├── js/common.js                   # 公共封装：请求、提示、确认弹窗、页签切换
│       ├── js/dashboard.js                # 运行总览（拓扑大屏 / KPI / 设备概况）
│       ├── js/device.js                   # 设备管理（FM）
│       ├── js/monitor.js                  # 运行监控（MM）
│       ├── js/alarm.js                    # 告警与阈值（AM）
│       └── js/analysis.js                 # 统计与趋势图（AN）
│
├── src/test/
│   ├── java/com/cooling/platform/         # 测试代码（与主代码同包结构）
│   │   ├── CoolingPlatformFlowTest.java   # 功能测试（真实上下文 + SQLite + HTTP）
│   │   ├── common/util/CsvExporterTest.java
│   │   ├── factory/                       # DeviceFactoryTest、DeviceIdGeneratorTest
│   │   ├── rule/AlarmRuleTest.java
│   │   └── controller/                    # 四个 Controller 的接口测试
│   └── resources/application-test.yml     # 测试专用配置（独立数据库、关闭定时任务）
│
├── pom.xml                                # Maven 构建与依赖
├── .gitignore                             # 忽略构建产物、数据库、日志、IDE 配置
├── .editorconfig                          # 统一编码 / 缩进 / 换行
└── README.md                              # 本文件
```

### 分层职责约定

| 包 | 职责 | 约束 |
| --- | --- | --- |
| `controller` | 接收请求、参数校验、调用 Service、返回 `Result` | 不写业务逻辑，不直接调 Mapper |
| `service` / `impl` | 业务编排、事务边界、跨 Mapper 组合 | 接口与实现分离，事务注解只出现在实现类 |
| `mapper` | 单表 / 多表 SQL 映射 | 只做数据存取，不写业务判断 |
| `entity` | 领域模型与持久化载体 | 只含属性与领域行为，不含框架注解 |
| `dto` / `vo` | 入参 / 出参结构 | 与 entity 隔离，避免直接暴露表结构 |
| `rule` | 告警判定策略 | 新增规则只加实现类，不改采集主流程 |
| `scheduler` | 周期任务 | 只做调度与异常兜底，逻辑下沉到 Service |

---

## 七、数据库设计（SQLite）

数据库文件：`./data/cooling-ops.db`，首次启动由 `db/schema.sql` + `db/data.sql` 自动建表并播种。

| 表名 | 说明 | 主要来源需求 |
| --- | --- | --- |
| `device` | 设备台账，单表继承存储 | 3.1–3.3、FM-01~06 |
| `sensor_data` | 传感器采集时序数据（长表） | MM-05、AN-02 |
| `alarm_threshold` | 告警阈值配置 | AM-01、5.3 |
| `alarm` | 告警记录 | AM-02~05 |
| `run_log` | 设备运行参数快照 | PM-02 |
| `alarm_log` | 告警触发 / 处理流水 | PM-03 |
| `system_config` | 系统参数键值对 | 系统设置 |

**设备存储策略（单表继承）**

公共属性映射为列（`device_id`、`device_name`、`device_type`、`manufacturer`、`model`、`install_date`、`status`、`zone`），子类专有属性序列化为 JSON 存入 `params` 列：

```json
{"coolingCapacity":130.0,"currentLoad":0.65,
 "chilledWaterSupplyTemp":7.2,"chilledWaterReturnTemp":12.8,"refrigerantType":"R134a"}
```

优点：新增设备类型无需 DDL 变更，持久层保持一套 Mapper，符合需求文档 5.3「新增设备类型遵循开闭原则」。
代价：`params` 内的字段不能直接建索引 / 做聚合，因此**参与查询、告警与趋势分析的量测值统一写入 `sensor_data`**，避免 JSON 内查询。

**传感器数据长表设计**：`sensor_data(device_id, sensor_key, value, unit, collect_time)` 每设备每参数每次采集一行，趋势查询按 `(device_id, sensor_key, collect_time)` 索引即可，天然支持 AN-02「最近 N 次采集」。

---

## 八、快速开始

### 环境要求

- JDK 1.8 及以上
- Maven 3.6+
- 无需安装数据库，SQLite 以文件形式内嵌

### 构建与运行

```bash
# 1. 克隆仓库
git clone https://github.com/songmingjun3/cooling-ops-platform.git
cd cooling-ops-platform

# 2. 编译打包（首次会下载依赖）
mvn clean package -DskipTests

# 3. 启动（默认 dev 环境，端口 8080）
mvn spring-boot:run
# 或运行打包产物
java -jar target/cooling-ops-platform.jar
```

启动成功后：

- 主界面：<http://localhost:8080/>（运行总览、设备管理、运行监控、告警管理、数据分析五个页签）
- 接口示例：<http://localhost:8080/api/devices?pageSize=5>
- 数据库文件：`./data/cooling-ops.db`（首次启动自动创建，含 12 台示例设备）
- 运行日志：`./logs/cooling-ops.log`

> 若需重置数据，停止应用后删除 `data/cooling-ops-dev.db`，下次启动会重新建表并播种示例数据。

### 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 启动报 `Port xxxxx is already in use` | Spring Boot 会把环境变量 `SERVER_PORT` / `SERVER__PORT` 当作 `server.port`，覆盖 `application.yml`。显式指定端口即可：`mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=8080"`，或先清掉该环境变量 |
| 页面能打开但列表为空 | 打开浏览器控制台看 `/api/devices` 是否返回 `code:0`；若数据库被手工改坏，删库重启即可重新播种 |
| 中文乱码 | 确认运行环境为 UTF-8；项目已在 `application.yml` 与 `logback-spring.xml` 中强制 UTF-8 |

---

## 九、接口约定

- 统一前缀 `/api`，资源名使用复数、小写中划线：`/api/devices`、`/api/alarms`、`/api/alarm-thresholds`；
- 方法语义：`GET` 查询、`POST` 新增、`PUT` 修改、`DELETE` 删除、`PATCH` 状态变更（启停、告警处理）；
- 请求与响应均为 `application/json; charset=UTF-8`；
- 所有响应统一包装为 `Result`：

```json
{ "code": 0, "message": "操作成功", "data": { }, "timestamp": "2026-10-20 14:40:30" }
```

| code | 含义 |
| --- | --- |
| `0` | 成功 |
| `400` | 参数校验失败 / 业务规则不允许 |
| `404` | 资源不存在（设备、告警、阈值） |
| `500` | 系统内部错误 |

> 响应体的 `code` 与 HTTP 状态码保持一致：失败时 HTTP 也返回 4xx/5xx，
> 前端与测试都可直接按 HTTP 语义判断成败。全部异常由 `GlobalExceptionHandler`
> 统一转换为中文提示，不会把框架异常原文抛给用户。

**接口清单**

| 模块 | 方法与路径 | 说明 | 状态 |
| --- | --- | --- | --- |
| 设备 | `GET /api/devices` | 多条件分页查询（FM-04/05） | ✅ |
| 设备 | `GET /api/devices/{deviceId}` | 设备详情含传感器与规格（FM-03） | ✅ |
| 设备 | `POST /api/devices` | 新增，编号由后端生成（FM-01） | ✅ |
| 设备 | `PUT /api/devices/{deviceId}` | 修改基本信息（FM-03） | ✅ |
| 设备 | `DELETE /api/devices/{deviceId}` | 删除（FM-02） | ✅ |
| 设备 | `GET /api/devices/group-by-zone` | 按区域分组（FM-06） | ✅ |
| 设备 | `GET /api/devices/export.csv` | 设备清单导出 CSV（PM-04） | ✅ |
| 监控 | `GET /api/monitor/devices/{deviceId}` | 实时状态与最新采集值（MM-01） | ✅ |
| 监控 | `GET /api/monitor/devices?deviceType=` | 按类型批量查看（MM-02） | ✅ |
| 监控 | `POST /api/monitor/devices/{deviceId}/start` | 启动设备（MM-03） | ✅ |
| 监控 | `POST /api/monitor/devices/{deviceId}/stop` | 停机设备（MM-04） | ✅ |
| 监控 | `POST /api/monitor/collect` | 手动触发一次模拟采集（MM-05） | ✅ |
| 告警 | `GET /api/alarms` | 告警列表，可按状态/级别/设备过滤（AM-03） | ✅ |
| 告警 | `PATCH /api/alarms/{id}/handle` | 处理告警并留痕（AM-04） | ✅ |
| 告警 | `GET /api/alarm-thresholds` | 阈值查询，可按设备类型过滤（AM-01） | ✅ |
| 告警 | `POST /api/alarm-thresholds` | 新增阈值（AM-01） | ✅ |
| 告警 | `PUT /api/alarm-thresholds/{id}` | 修改阈值（AM-01） | ✅ |
| 告警 | `DELETE /api/alarm-thresholds/{id}` | 删除阈值（AM-01） | ✅ |
| 告警 | `PATCH /api/alarm-thresholds/{id}/enabled` | 启用 / 停用阈值（AM-01） | ✅ |
| 分析 | `GET /api/statistics/overview` | 设备数量与状态分布（AN-01） | ✅ |
| 分析 | `GET /api/statistics/trend` | 温度趋势点（AN-02） | ✅ |
| 数据 | `GET /api/logs/run` | 运行日志分页查询（PM-02） | ✅ |
| 数据 | `GET /api/logs/alarm` | 告警日志按编号追踪（PM-03） | ✅ |

**请求示例**

```bash
# 新增一台水泵（编号自动生成为 PM-005）
curl -X POST http://localhost:8080/api/devices \
  -H "Content-Type: application/json" \
  -d '{"deviceType":"PUMP","deviceName":"3号冷冻水泵","manufacturer":"格兰富",
       "model":"NK150-400","installDate":"2025-01-10","zone":"A区",
       "params":{"pumpType":"CHILLED_WATER","flowRate":80,"head":32,"power":21,"speed":70}}'

# 多条件查询：A 区运行中的冷水机组
curl "http://localhost:8080/api/devices?zone=A区&status=RUNNING&deviceType=CHILLER"
```

> `params` 的键名与需求文档 3.3 节属性名一致，传感器类参数直接传数值；未传的字段按类型默认值填充，传了未定义的键会返回 400 并指出具体键名。

---

## 十、开发规范

依据需求文档 5.1「代码质量要求」制定，提交前自检。

### 10.1 命名

| 元素 | 规则 | 示例 |
| --- | --- | --- |
| 类 / 接口 / 枚举 | 大驼峰 PascalCase | `DeviceServiceImpl`、`AlarmLevel` |
| 方法 / 变量 / 参数 | 小驼峰 camelCase | `findByDeviceId`、`deviceStatus` |
| 常量 | 全大写 + 下划线 | `MAX_RETRY_COUNT`、`DEFAULT_ZONE` |
| 包 | 全小写，按功能分层 | `com.cooling.platform.service.impl` |
| 数据库表 / 列 | 小写下划线 | `alarm_threshold`、`device_id` |
| URL | 小写中划线、资源复数 | `/api/alarm-thresholds` |
| 测试类 | 被测类 + `Test` | `DeviceServiceTest` |

### 10.2 注释

- 每个类、每个 `public` 方法必须有 Javadoc，说明**职责、参数、返回值、异常**；
- 关键业务逻辑（阈值判定、编号生成、状态流转）必须有行内注释说明「为什么」；
- 禁止无意义注释（如 `// 设置名称`）。

### 10.3 异常与健壮性

- 禁止空 `catch` 块；可预期业务失败抛 `BusinessException`，由 `GlobalExceptionHandler` 统一处理；
- 未预期异常必须 `log.error` 记录完整堆栈；
- 用户输入非法值（负数温度、空白字符串、非法枚举）由 DTO 校验注解拦截，不进入业务层；
- 设备编号由 `DeviceIdGenerator` 依据「前缀 - 现有最大序号 + 1」生成并加锁保证唯一，不依赖人工输入；
- 文件与 CSV 导出失败需返回友好提示，不得抛出堆栈给前端。

### 10.4 日志

- 使用 Lombok `@Slf4j`，禁止 `System.out.println`；
- 采集、告警、状态变更、数据导出记录 `INFO`；异常与降级记录 `WARN` / `ERROR`；
- 日志输出同时落地 `logs/cooling-ops.log`（按天 + 10MB 滚动，保留 15 天），满足 PM-02 / PM-03 的可追溯要求。

### 10.5 Git 提交规范

采用 Conventional Commits：

```
<type>(<scope>): <subject>

feat(device): 实现设备新增与自动编号生成
fix(alarm): 修复阈值比较时单位换算错误
docs(readme): 补充数据库表设计说明
refactor(entity): 抽取 Device 公共属性到抽象基类
test(service): 增加告警判定规则单元测试
chore(build): 升级 sqlite-jdbc 版本
```

| type | 用途 |
| --- | --- |
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档变更 |
| `style` | 格式调整（不影响逻辑） |
| `refactor` | 重构 |
| `test` | 测试相关 |
| `chore` | 构建、依赖、配置 |

**分支策略**：`main` 保持可运行；功能开发在 `feature/<模块名>` 分支进行，完成后合并回 `main`。

---

## 十一、开发进度与路线图

| 里程碑 | 内容 | 状态 |
| --- | --- | --- |
| M1 | 仓库初始化、规范化目录、领域模型与枚举、数据库脚本、统一响应/异常 | ✅ 已完成 |
| M2 | 设备管理模块 FM-01~06（Mapper / Service / Controller / 页面 / 接口联调） | ✅ 已完成 |
| M3 | 运行监控模块 MM-01~05（含定时模拟采集） | ✅ 已完成 |
| M4 | 告警管理模块 AM-01~05（阈值配置、策略化检测、处理闭环） | ✅ 已完成 |
| M5 | 数据持久化与导出 PM-01~04（运行日志、告警日志、CSV 导出） | ✅ 已完成 |
| M6 | 分析与展示 AN-01~02（统计接口 + ECharts 趋势图） | ✅ 已完成 |
| M7 | 单元测试、接口测试、功能测试、文档与答辩材料 | ✅ 已完成 |
| M8 | 前端布局重构、运行总览大屏（拓扑 + 概况）、界面端到端测试 | ✅ 已完成 |

### 11.1 页面结构

界面为单页应用，左侧固定导航 + 右侧主内容区，共五个页签：

| 页签 | 职责 | 数据来源 |
| --- | --- | --- |
| **运行总览**（默认首页） | KPI 指标行、设备连接关系拓扑大屏、设备概况（类型 × 状态矩阵）、区域分布、最近告警 | `/api/devices`、`/api/statistics/overview`、`/api/alarms`、`/api/monitor/devices` |
| 设备管理 | 台账列表 / 按区域分组、多条件查询、分页、新增、编辑、删除、详情、CSV 导出 | `/api/devices/**` |
| 运行监控 | 按类型查看实时参数、启停状态机、立即采集 | `/api/monitor/**` |
| 告警管理 | 告警记录与处理闭环、阈值规则 CRUD 与启停 | `/api/alarms`、`/api/alarm-thresholds` |
| 数据分析 | 运行统计分布、运行参数趋势（SVG 折线 + 悬停读数） | `/api/statistics/**` |

**关于设备概况的位置**：设备概况只在「运行总览」出现，不再放在页头。
页头仅保留页面标题、一句话说明与「数据更新于 HH:mm:ss」的全局状态，
避免每个功能页重复渲染同一组统计数字，也避免数字与当前页内容无关造成的误导。

**设备连接关系拓扑**按真实制冷流程绘制，分两条回路：

- 冷却水回路：冷却塔 → 冷却水泵 → 冷水机组（冷凝侧散热）
- 冷冻水回路：冷水机组 → 冷冻水泵 → 精密空调（末端供冷），再由底部虚线回到冷水机组

节点按运行状态着色（绿=运行 / 红=故障 / 灰=停机 / 橙=维护），
运行中的回路带流动动画；点击节点直接打开该设备的详情弹窗；
水泵归属哪条回路取自监控接口返回的「水泵类型」规格参数，取不到时回退到名称关键字判断。

---

## 十二、测试

测试分四层，共 **110 个 JUnit 用例 + 53 个接口检查点 + 61 个界面检查点**，全部通过。执行：

```bash
mvn test                                    # 三层 JUnit 用例
mvn test -Dtest=AlarmRuleTest               # 只跑某个单元测试
mvn test -Dtest=CoolingPlatformFlowTest     # 只跑功能测试
python scripts/e2e_verify.py                # 端到端接口巡检（需先启动应用）
node scripts/e2e-ui.js --url http://127.0.0.1:18082   # 端到端界面测试（需先启动应用）
```

### 12.1 分层策略

| 层次 | 关注点 | 技术手段 | 用例数 |
| --- | --- | --- | --- |
| 单元测试 | 纯逻辑：告警判定边界、参数校验红线、编号生成、CSV 转义 | JUnit 5 + Mockito，不起 Spring | 40 |
| 接口测试 | 协议契约：路径、必填参数、统一响应、异常→HTTP 状态码 | `@WebMvcTest` + MockMvc，Mock 业务层 | 51 |
| 功能测试 | 全链路：Controller → Service → Mapper → SQLite，含模块联动 | `@SpringBootTest(RANDOM_PORT)` + TestRestTemplate + 真实 SQLite | 19 |
| 接口端到端 | 运行中实例的全量接口巡检与负向路径 | `scripts/e2e_verify.py`（Python 标准库） | 53 检查点 |
| 界面端到端 | 用户可见行为：点击、填表、DOM 状态、控制台异常、响应式 | `scripts/e2e-ui.js`（Node + CDP 驱动真实浏览器） | 60 检查点 |

### 12.2 覆盖清单

**单元测试**

| 测试类 | 覆盖内容 |
| --- | --- |
| `AlarmRuleTest` | `GT` / `LT` / `DEVIATION` 三种策略的边界值（等于阈值不告警）、额定值非正时不除零、默认波动百分比、规则注册表的未知/未注册分支 |
| `DeviceFactoryTest` | 四类设备实例化、`params` 强类型转换、未知键与非法枚举拒绝、负载率 0~1 与转速 0~100 边界、跨类型参数拒绝 |
| `DeviceIdGeneratorTest` | 取最大序号加一（防删中间编号后撞主键）、三位补零、脏数据忽略、999 上限 |
| `CsvExporterTest` | UTF-8 BOM、CRLF、逗号/引号/换行转义、null 渲染为空串 |

**接口测试**

| 测试类 | 覆盖内容 |
| --- | --- |
| `DeviceControllerTest` | 分页元信息、枚举非法返回中文提示、`pageSize` 越界、详情日期格式、404 / 500 映射、CSV 内容协商（`Accept: application/json` 仍返回 CSV） |
| `MonitorControllerTest` | 实时状态、按类型批量、缺参 400、状态机 400、手动采集统计 |
| `AlarmControllerTest` | 告警分页与多条件过滤、处理告警必填校验、重复处理、阈值 CRUD 与启停、波动百分比越界 |
| `StatisticsLogControllerTest` | 统计分组结构、趋势参数校验（`limit` 上限 200）、日志分页默认值 |

**功能测试（`CoolingPlatformFlowTest`）**

19 个有序用例串成一条主线，验证模块间联动：

1. 初始数据就绪（12 台设备 / 7 条阈值）；
2. 新增冷却塔 → 编号自动为 `CT-004`；
3. 多条件与关键字查询命中；
4. 详情含 3 个传感器与规格参数；
5. 修改基本信息不清空运行参数；
6. 启动 → `RUNNING` 且传感器初始化为额定值；
7. 重复启动被状态机拒绝；
8. 手动采集只覆盖 `RUNNING` 设备；
9. 实时值落在模拟区间内；
10. 停机后不再产生运行日志；
11. 阈值改为极低值并升级为 `CRITICAL`；
12. 采集自动触发告警并落库；
13. 处理告警 → 留痕 → 重复处理被拒 → 告警日志有两条动作；
14. 阈值恢复出厂值；
15. 运行日志按设备过滤；
16. CSV 导出含新设备且带 BOM；
17. 统计总数与状态分布自洽；
18. 趋势点按时间正序；
19. 删除后详情 404、列表回到 12 台。

### 12.3 测试环境注意事项

- 功能测试使用 `src/test/resources/application-test.yml`，数据库指向 `target/test-data/cooling-ops-test.db`，
  并在类加载时删除旧库，保证每次从 `data.sql` 初始数据开始，**不会污染开发库 `./data/`**。
- 测试配置中 `cooling.acquisition.enabled=false`，关闭定时采集，避免与用例的显式采集互相干扰。
- JDK 自带 `HttpURLConnection` 不支持 `PATCH`，功能测试改用 Apache HttpClient 作为请求工厂。
- `@MapperScan` 放在独立的 `MybatisConfig` 中，否则 `@WebMvcTest` 会尝试创建 Mapper Bean 而启动失败。

### 12.4 端到端巡检脚本

`scripts/e2e_verify.py` 在真实运行的应用上巡检 **53 个检查点**，覆盖 FM / MM / AM / PM / AN 全部功能点，
包含越权路径（未知设备、非法枚举、重复处理、参数越界）的负向验证，并在结束时自动还原被修改的阈值配置。

### 12.5 界面端到端测试

`scripts/e2e-ui.js` 用 Node 内置 WebSocket 直连 Chromium 调试协议（CDP）驱动真实浏览器，
不依赖 Playwright / Puppeteer，也无需 `npm install`。它在界面上真实点击、填表、读 DOM，
验证的是「用户看到的东西」，与只打接口的巡检脚本互补。

建议单独起一个实例，使用独立数据库，避免污染开发数据：

```bash
java -jar target/cooling-ops-platform.jar \
     --server.port=18082 \
     --spring.datasource.url=jdbc:sqlite:./target/e2e-data/e2e-ui.db \
     --cooling.acquisition.enabled=false

node scripts/e2e-ui.js --url http://127.0.0.1:18082
```

**61 个检查点**按功能域分组：

| 分组 | 检查内容 |
| --- | --- |
| A 启动与导航 | 默认落在运行总览、五个页签各自唯一显示、页头不再有设备概况、更新时间戳、无遗漏可见面板 |
| B 运行总览大屏 | 六项 KPI 与接口一致、拓扑节点数=设备数、五列顺序、4 段供水 + 2 条回水、流动动画、矩阵每行状态和=总数、矩阵合计=KPI、区域条数、点击节点开详情、KPI 卡跳转并预置筛选 |
| C 设备管理 | 分页、翻页、关键字、类型筛选、重置、区域分组、详情三段、必填校验、新增回显编号、编辑（类型不可改）、删除取消/确认两条分支 |
| D 运行监控 | 按类型切换卡片、停机设备启动后变运行中、维护中设备给出启停限制、立即采集、采集后显示采集时间、**设备详情每个参数都带采集时间** |
| E 告警管理 | 列表加载、阈值加载、新增阈值、采集触发新告警、处理必填校验、处理后移出待办、阈值启停、删除阈值 |
| F 数据分析 | 七项统计卡、三类分布图、趋势图绘制、**按容器宽度等比绘制（文字不变形）**、悬停显示单点数值 |
| G 全局一致性 | 总览数字实时同步、无原生 `confirm` 弹窗、无未捕获异常、无 `console.error` |
| H 响应式 | 900px 侧栏收为图标条、430px 导航「图标上文字下」、KPI 两列、拓扑横向滚动 |

失败时会把现场截图写入 `target/e2e-ui/`，并以退出码 1 结束，便于接入 CI。

### 12.6 本机环境注意事项

- 部分 Windows 环境的安全策略会拦截 `wmic.exe`，而 Maven Surefire 2.22 在 Windows 上正是用
  `wmic` 结束测试进程树，导致 `mvn test` 报「PROGRAM BLOCKED」。此时改用 `mvn test -DforkCount=0`
  即可（测试全部在 Maven 进程内运行），测试用例本身无需改动。
- 本机存在环境变量 `SERVER__PORT`，会覆盖 `server.port`；用命令行参数 `--server.port=xxxx`
  启动可优先于该环境变量。
- 前端改动后若 `spring-boot:run` 正在运行，可用
  `cp -r src/main/resources/static/. target/classes/static/` 热同步静态资源，无需重启。

---

## 十三、需求待澄清事项

实现过程中发现需求文档存在少量前后不一致，已按下述方式处理，**需与指导教师确认**：

1. **冷冻水供水温度取值范围**：3.3 节标注正常带为 `12–16℃`，4.3 节告警示例为 `> 16℃ 触发 CRITICAL`，而 6.2 节控制台示例设备值为 `7.2℃`。
   处理方式：温度正常带以表 3.3 为准，告警阈值全部落在 `alarm_threshold` 表中可配置；模拟采集区间取 `6.5–17.5℃` 以覆盖「正常 / 越界」两种演示场景。
2. **PM-01 持久化方式**：文档要求「程序退出时保存到文件」，本项目改用 SQLite 实时持久化（见 3.4 节说明）。
3. **UI 形态**：文档 6 章为控制台界面参考，本项目改为 Web 界面（Spring MVC 托管静态页面 + REST 接口），交互流程与菜单结构保持一致。
4. **用户与权限**：文档 2.2 明确仅「运维管理员」一种角色且无需登录，故本期不实现认证鉴权。

---

## 十四、参考

- 需求文档：[`docs/requirements/数据中心冷却系统智能运维平台——系统需求文档.docx`](docs/requirements/)
- 数据库脚本：[`src/main/resources/db/schema.sql`](src/main/resources/db/schema.sql)、[`src/main/resources/db/data.sql`](src/main/resources/db/data.sql)
- 应用配置：[`src/main/resources/application.yml`](src/main/resources/application.yml)
