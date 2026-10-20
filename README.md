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
│
├── src/main/java/com/cooling/platform/
│   ├── CoolingPlatformApplication.java    # 启动类
│   ├── common/                            # 通用能力（跨模块复用）
│   │   ├── constant/                      # 常量：设备编号前缀、业务字典、接口路径
│   │   ├── enums/                         # 枚举：DeviceStatus / DeviceType / TowerType
│   │   │                                  #       PumpType / AlarmLevel / AlarmStatus
│   │   ├── exception/                     # BusinessException、GlobalExceptionHandler
│   │   ├── response/                      # Result 统一响应包装
│   │   └── util/                          # 工具：DeviceIdGenerator、CsvExporter、DateUtils
│   ├── config/                            # 配置：SQLite 数据源、MyBatis、WebMvc、定时任务
│   ├── controller/                        # Spring MVC 控制层（REST 接口，按模块拆分）
│   ├── service/                           # 业务接口（按模块拆分）
│   │   └── impl/                          # 业务实现
│   ├── mapper/                            # MyBatis Mapper 接口
│   ├── entity/                            # 领域模型：Device 及四个子类、Sensor、Alarm 等
│   ├── dto/                               # 请求对象：DeviceCreateDTO、DeviceQueryDTO 等
│   ├── vo/                                # 响应对象：DeviceVO、AlarmVO、StatisticsVO 等
│   ├── converter/                         # Entity ↔ DTO/VO 转换（含 params JSON 编解码）
│   ├── scheduler/                         # 定时任务：DataAcquisitionTask（MM-05）
│   ├── rule/                              # 告警规则策略：AlarmRule + 注册表 + 各实现
│   └── factory/                           # 设备工厂：按 DeviceType 创建具体设备子类
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
│       ├── index.html                     # 主界面（设备清单 / 监控 / 告警 / 分析）
│       ├── css/  js/  pages/  assets/      # 样式、脚本、子页面、图片与第三方库
│
├── src/test/java/com/cooling/platform/    # 单元测试（与主代码同包结构）
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
git clone <repository-url>
cd 大数据冷却系统运维平台

# 2. 编译打包（首次会下载依赖）
mvn clean package -DskipTests

# 3. 启动（默认 dev 环境，端口 8080）
mvn spring-boot:run
# 或运行打包产物
java -jar target/cooling-ops-platform.jar
```

启动成功后：

- 主界面：<http://localhost:8080/index.html>
- 接口示例：<http://localhost:8080/api/devices>
- 数据库文件：`./data/cooling-ops.db`（首次启动自动创建，含 12 台示例设备）
- 运行日志：`./logs/cooling-ops.log`

> 若需重置数据，停止应用后删除 `data/cooling-ops-dev.db`，下次启动会重新建表并播种示例数据。

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
| `400` | 参数校验失败 |
| `-1` | 通用业务失败（如设备不存在、编号重复） |
| `500` | 系统内部错误 |

**规划中的主要接口**

| 模块 | 方法与路径 | 说明 |
| --- | --- | --- |
| 设备 | `GET /api/devices` | 多条件分页查询（FM-04/05） |
| 设备 | `GET /api/devices/{deviceId}` | 设备详情（MM-01） |
| 设备 | `POST /api/devices` | 新增，编号由后端生成（FM-01） |
| 设备 | `PUT /api/devices/{deviceId}` | 修改基本信息（FM-03） |
| 设备 | `DELETE /api/devices/{deviceId}` | 删除（FM-02） |
| 设备 | `GET /api/devices/group-by-zone` | 按区域分组（FM-06） |
| 监控 | `PATCH /api/devices/{deviceId}/status` | 启动 / 停机（MM-03/04） |
| 监控 | `GET /api/devices/type/{deviceType}` | 按类型批量查看（MM-02） |
| 告警 | `GET /api/alarms` | 告警列表，可按状态/级别过滤（AM-03） |
| 告警 | `PATCH /api/alarms/{id}/handle` | 处理告警（AM-04） |
| 告警 | `GET|PUT /api/alarm-thresholds` | 阈值查询与配置（AM-01） |
| 分析 | `GET /api/statistics/overview` | 设备数量与状态分布（AN-01） |
| 分析 | `GET /api/statistics/trend` | 温度趋势点（AN-02） |
| 数据 | `GET /api/export/devices.csv` | 设备清单导出 CSV（PM-04） |

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
| M2 | 设备管理模块 FM-01~06（Mapper / Service / Controller / 页面） | ⬜ 待开始 |
| M3 | 运行监控模块 MM-01~05（含定时模拟采集） | ⬜ 待开始 |
| M4 | 告警管理模块 AM-01~05（阈值配置、策略化检测、处理闭环） | ⬜ 待开始 |
| M5 | 数据持久化与导出 PM-01~04（运行日志、告警日志、CSV 导出） | ⬜ 待开始 |
| M6 | 分析与展示 AN-01~02（统计接口 + ECharts 趋势图） | ⬜ 待开始 |
| M7 | 单元测试、异常场景演练、文档与答辩材料 | ⬜ 待开始 |

---

## 十二、需求待澄清事项

实现过程中发现需求文档存在少量前后不一致，已按下述方式处理，**需与指导教师确认**：

1. **冷冻水供水温度取值范围**：3.3 节标注正常带为 `12–16℃`，4.3 节告警示例为 `> 16℃ 触发 CRITICAL`，而 6.2 节控制台示例设备值为 `7.2℃`。
   处理方式：温度正常带以表 3.3 为准，告警阈值全部落在 `alarm_threshold` 表中可配置；模拟采集区间取 `6.5–17.5℃` 以覆盖「正常 / 越界」两种演示场景。
2. **PM-01 持久化方式**：文档要求「程序退出时保存到文件」，本项目改用 SQLite 实时持久化（见 3.4 节说明）。
3. **UI 形态**：文档 6 章为控制台界面参考，本项目改为 Web 界面（Spring MVC 托管静态页面 + REST 接口），交互流程与菜单结构保持一致。
4. **用户与权限**：文档 2.2 明确仅「运维管理员」一种角色且无需登录，故本期不实现认证鉴权。

---

## 十三、参考

- 需求文档：[`docs/requirements/数据中心冷却系统智能运维平台——系统需求文档.docx`](docs/requirements/)
- 数据库脚本：[`src/main/resources/db/schema.sql`](src/main/resources/db/schema.sql)、[`src/main/resources/db/data.sql`](src/main/resources/db/data.sql)
- 应用配置：[`src/main/resources/application.yml`](src/main/resources/application.yml)
