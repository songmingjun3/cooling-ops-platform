-- =====================================================================
--  数据中心冷却系统智能运维平台 —— 数据库结构（SQLite 3）
--  说明：所有语句均使用 IF NOT EXISTS，每次启动执行是幂等的。
--       时间统一以 ISO-8601 文本存储（yyyy-MM-dd HH:mm:ss）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 设备表（单表继承：公共属性 → 列，子类专有属性 → params JSON）
--    新增设备类型时无需改表结构，符合开闭原则。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS device (
    device_id     TEXT PRIMARY KEY,              -- 设备编号，如 CH-001
    device_type   TEXT NOT NULL,                 -- CHILLER / COOLING_TOWER / PUMP / COOLING_UNIT
    device_name   TEXT NOT NULL,
    manufacturer  TEXT,
    model         TEXT,
    install_date  TEXT,
    status        TEXT NOT NULL DEFAULT 'STOPPED', -- DeviceStatus
    zone          TEXT,
    params        TEXT,                          -- 子类专有属性 JSON，如 {"coolingCapacity":130.0,...}
    create_time   TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    update_time   TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_device_type   ON device (device_type);
CREATE INDEX IF NOT EXISTS idx_device_zone   ON device (zone);
CREATE INDEX IF NOT EXISTS idx_device_status ON device (status);

-- ---------------------------------------------------------------------
-- 2. 传感器采集数据（时序，长表结构，便于按设备+参数画趋势图 AN-02）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sensor_data (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id    TEXT NOT NULL,
    sensor_key   TEXT NOT NULL,                  -- 如 chilledWaterSupplyTemp
    sensor_name  TEXT,
    value        REAL NOT NULL,
    unit         TEXT,
    collect_time TEXT NOT NULL,
    FOREIGN KEY (device_id) REFERENCES device (device_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sensor_device_key_time
    ON sensor_data (device_id, sensor_key, collect_time DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_collect_time
    ON sensor_data (collect_time);

-- ---------------------------------------------------------------------
-- 3. 告警阈值配置（AM-01：可按设备类型/参数灵活配置，不硬编码）
--    compare_op: GT 大于 / LT 小于 / DEVIATION 相对额定值波动百分比
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alarm_threshold (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    device_type       TEXT NOT NULL,
    param_key         TEXT NOT NULL,
    param_name        TEXT NOT NULL,
    compare_op        TEXT NOT NULL DEFAULT 'GT',
    threshold_value   REAL NOT NULL,
    deviation_percent REAL,
    unit              TEXT,
    alarm_level       TEXT NOT NULL DEFAULT 'WARNING',
    enabled           INTEGER NOT NULL DEFAULT 1,
    remark            TEXT,
    update_time       TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE (device_type, param_key, compare_op)
);

-- ---------------------------------------------------------------------
-- 4. 告警记录（AM-03 / AM-04 / AM-05）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alarm (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    alarm_no        TEXT UNIQUE,                 -- 业务告警编号 AL-yyyyMMdd-0001
    device_id       TEXT NOT NULL,
    device_name     TEXT,
    param_key       TEXT,
    param_name      TEXT,
    actual_value    REAL,
    threshold_value REAL,
    alarm_level     TEXT NOT NULL,               -- WARNING / CRITICAL
    message         TEXT,
    status          TEXT NOT NULL DEFAULT 'UNHANDLED', -- UNHANDLED / HANDLED
    trigger_time    TEXT NOT NULL,
    handler         TEXT,
    handle_time     TEXT,
    handle_measure  TEXT
);

CREATE INDEX IF NOT EXISTS idx_alarm_device   ON alarm (device_id);
CREATE INDEX IF NOT EXISTS idx_alarm_status   ON alarm (status, trigger_time DESC);

-- ---------------------------------------------------------------------
-- 5. 运行日志（PM-02：每次采集写入一条设备参数快照）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS run_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id    TEXT NOT NULL,
    device_type  TEXT,
    status       TEXT,
    params       TEXT,                           -- 参数快照 JSON
    collect_time TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_run_log_device_time ON run_log (device_id, collect_time DESC);

-- ---------------------------------------------------------------------
-- 6. 告警日志（PM-03：告警触发与处理的流水，只追加不修改）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alarm_log (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    alarm_no  TEXT,
    action    TEXT NOT NULL,                     -- TRIGGER / HANDLE
    detail    TEXT,
    operator  TEXT,
    log_time  TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alarm_log_time ON alarm_log (log_time DESC);

-- ---------------------------------------------------------------------
-- 7. 系统配置（键值对，用于保存采集开关、采集间隔等运行期参数）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_config (
    config_key   TEXT PRIMARY KEY,
    config_value TEXT,
    remark       TEXT,
    update_time  TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
