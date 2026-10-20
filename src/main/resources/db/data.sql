-- =====================================================================
--  初始化示例数据（幂等：使用 INSERT OR IGNORE，重复启动不会产生脏数据）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 设备台账：12 台（冷水机组 2、冷却塔 3、水泵 4、精密空调 3）
-- device_id 前缀约定：CH 冷水机组 / CT 冷却塔 / PM 水泵 / CU 精密空调
-- ---------------------------------------------------------------------
INSERT OR IGNORE INTO device (device_id, device_type, device_name, manufacturer, model, install_date, status, zone, params) VALUES
('CH-001', 'CHILLER', '1号冷水机组', '格力', 'LSQWRF130M', '2024-06-15', 'RUNNING',
 'A区', '{"coolingCapacity":130.0,"currentLoad":0.65,"chilledWaterSupplyTemp":7.2,"chilledWaterReturnTemp":12.8,"refrigerantType":"R134a"}'),
('CH-002', 'CHILLER', '2号冷水机组', '约克', 'YK-1500', '2024-06-15', 'FAULT',
 'A区', '{"coolingCapacity":150.0,"currentLoad":0.0,"chilledWaterSupplyTemp":13.5,"chilledWaterReturnTemp":18.2,"refrigerantType":"R134a"}'),
('CT-001', 'COOLING_TOWER', '1号冷却塔', '良机', 'LBC-200', '2024-07-02', 'RUNNING',
 'A区', '{"towerType":"OPEN","fanSpeed":80,"waterFlowRate":180.0,"ambientTemp":28.5,"outletTemp":32.1}'),
('CT-002', 'COOLING_TOWER', '2号冷却塔', '良机', 'LBC-200', '2024-07-02', 'RUNNING',
 'A区', '{"towerType":"OPEN","fanSpeed":75,"waterFlowRate":176.0,"ambientTemp":28.8,"outletTemp":31.6}'),
('CT-003', 'COOLING_TOWER', '3号冷却塔', '金日', 'JNC-150', '2024-07-08', 'STOPPED',
 'B区', '{"towerType":"CLOSED","fanSpeed":0,"waterFlowRate":0.0,"ambientTemp":27.9,"outletTemp":29.4}'),
('PM-001', 'PUMP', '1号冷冻水泵', '格兰富', 'NK150-400', '2024-06-20', 'RUNNING',
 'A区', '{"pumpType":"CHILLED_WATER","flowRate":85.0,"head":32.0,"power":22.0,"speed":75}'),
('PM-002', 'PUMP', '2号冷冻水泵', '格兰富', 'NK150-400', '2024-06-20', 'RUNNING',
 'A区', '{"pumpType":"CHILLED_WATER","flowRate":83.5,"head":31.8,"power":21.6,"speed":73}'),
('PM-003', 'PUMP', '1号冷却水泵', '威乐', 'NL125-315', '2024-06-25', 'RUNNING',
 'B区', '{"pumpType":"COOLING_WATER","flowRate":120.0,"head":26.0,"power":30.0,"speed":80}'),
('PM-004', 'PUMP', '2号冷却水泵', '威乐', 'NL125-315', '2024-06-25', 'MAINTENANCE',
 'B区', '{"pumpType":"COOLING_WATER","flowRate":0.0,"head":0.0,"power":0.0,"speed":0}'),
('CU-001', 'COOLING_UNIT', '1号精密空调', '艾默生', 'PEX4-30', '2024-08-01', 'RUNNING',
 'C区', '{"supplyTemp":22.5,"returnTemp":33.8,"speed":60}'),
('CU-002', 'COOLING_UNIT', '2号精密空调', '艾默生', 'PEX4-30', '2024-08-01', 'RUNNING',
 'C区', '{"supplyTemp":23.1,"returnTemp":34.6,"speed":62}'),
('CU-003', 'COOLING_UNIT', '3号精密空调', '施耐德', 'ACRC301', '2024-08-05', 'OFFLINE',
 'C区', '{"supplyTemp":0.0,"returnTemp":0.0,"speed":0}');

-- ---------------------------------------------------------------------
-- 告警阈值（AM-01）：
--   冷水机组冷冻水供水温度 > 16℃        → CRITICAL
--   冷却塔出水温度 > 35℃                → WARNING
--   水泵功率相对额定值波动 > ±20%       → WARNING
--   精密空调回风温度 > 40℃              → WARNING
-- ---------------------------------------------------------------------
INSERT OR IGNORE INTO alarm_threshold
(device_type, param_key, param_name, compare_op, threshold_value, deviation_percent, unit, alarm_level, enabled, remark) VALUES
('CHILLER',       'chilledWaterSupplyTemp', '冷冻水供水温度', 'GT',        16.0, NULL, '℃',   'CRITICAL', 1, '高于阈值判定制冷能力不足'),
('CHILLER',       'chilledWaterReturnTemp', '冷冻水回水温度', 'GT',        20.0, NULL, '℃',   'WARNING',  1, '回水温度偏高'),
('CHILLER',       'currentLoad',            '当前负载率',     'GT',         0.95, NULL, NULL, 'WARNING',  1, '接近满载运行'),
('COOLING_TOWER', 'outletTemp',             '出水温度',       'GT',        35.0, NULL, '℃',   'WARNING',  1, '散热能力下降'),
('COOLING_TOWER', 'waterFlowRate',          '水流量',         'LT',        80.0, NULL, 'm³/h','WARNING',  1, '流量不足'),
('PUMP',          'power',                  '当前功率',       'DEVIATION',  0.0, 20.0, 'kW',   'WARNING',  1, '功率相对额定值波动超过±20%'),
('COOLING_UNIT',  'returnTemp',             '回风温度',       'GT',        40.0, NULL, '℃',   'WARNING',  1, '机房热负荷过高');

-- ---------------------------------------------------------------------
-- 系统配置默认值
-- ---------------------------------------------------------------------
INSERT OR IGNORE INTO system_config (config_key, config_value, remark) VALUES
('acquisition.enabled',  'true', '模拟采集开关'),
('acquisition.interval', '3000', '采集间隔毫秒'),
('alarm.auto-detect',    'true', '采集后自动执行告警检测');
