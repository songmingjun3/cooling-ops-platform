/* =====================================================================
   运行总览（首页大屏）
   ---------------------------------------------------------------------
   看点：
   1. KPI 指标行：设备总数 / 运行中 / 故障离线 / 维护中 / 未处理告警 / 覆盖区域，
      点击任意指标可直接跳到对应页签并带好筛选条件；
   2. 设备连接关系拓扑：按真实制冷流程画出冷却水回路与冷冻水回路，
      状态着色、供水/回水线型区分、运行回路有流动动画，点击设备可看详情；
   3. 设备概况：类型 × 状态矩阵 + 区域分布 + 最近告警。
   拓扑与统计全部由一次设备列表推导，不额外引入后端接口。
   ===================================================================== */
(function () {
    'use strict';

    var App = window.App;
    var request = App.request;
    var escapeHtml = App.escapeHtml;
    var dash = App.dash;
    var toast = App.toast;
    var $ = App.$;

    /* ------------------------------ 拓扑几何参数 ------------------------------ */

    var NODE_W = 156;
    var NODE_H = 64;
    var GAP_Y = 16;
    var CANVAS_W = 1240;
    var CANVAS_H = 470;
    var MID_Y = 235;

    /* 五列中心横坐标：冷却塔 → 冷却水泵 → 冷水机组 → 冷冻水泵 → 精密空调 */
    var COL_X = [114, 367, 620, 873, 1126];

    var STATUS_COLOR = {
        RUNNING: '#35d18a',
        STOPPED: '#8fa3bb',
        FAULT: '#ff6b6b',
        MAINTENANCE: '#f2b23e',
        OFFLINE: '#5f7387'
    };

    var state = {
        devices: [],
        overview: null,
        alarms: [],
        pumps: [],
        loaded: false
    };

    /* ------------------------------ 数据处理 ------------------------------ */

    /**
     * 把设备按「回路位置」分桶。
     * 水泵要区分冷冻水泵与冷却水泵，接口列表里没有该字段，
     * 因此用监控接口返回的规格参数（水泵类型）判定，取不到时回退到名称关键字。
     */
    function bucketize(devices, pumps) {
        var pumpType = {};
        (pumps || []).forEach(function (p) {
            var label = (p.specs && p.specs['水泵类型']) || '';
            if (label.indexOf('冷却') >= 0) {
                pumpType[p.deviceId] = 'COOLING';
            } else if (label.indexOf('冷冻') >= 0) {
                pumpType[p.deviceId] = 'CHILLED';
            }
        });

        var buckets = {
            tower: [],
            coolingPump: [],
            chiller: [],
            chilledPump: [],
            unit: []
        };

        devices.forEach(function (d) {
            if (d.deviceType === 'COOLING_TOWER') {
                buckets.tower.push(d);
            } else if (d.deviceType === 'CHILLER') {
                buckets.chiller.push(d);
            } else if (d.deviceType === 'COOLING_UNIT') {
                buckets.unit.push(d);
            } else if (d.deviceType === 'PUMP') {
                var kind = pumpType[d.deviceId]
                    || (d.deviceName && d.deviceName.indexOf('冷却') >= 0 ? 'COOLING' : 'CHILLED');
                if (kind === 'COOLING') {
                    buckets.coolingPump.push(d);
                } else {
                    buckets.chilledPump.push(d);
                }
            }
        });
        return buckets;
    }

    function truncate(text, max) {
        var value = text === null || text === undefined ? '' : String(text);
        return value.length > max ? value.substring(0, max - 1) + '…' : value;
    }

    function hasRunning(list) {
        return list.some(function (d) {
            return d.status === 'RUNNING';
        });
    }

    /* ------------------------------ 拓扑绘制 ------------------------------ */

    function columnTop(count) {
        var height = count * NODE_H + Math.max(0, count - 1) * GAP_Y;
        return MID_Y - height / 2;
    }

    function centerY(index, count) {
        return columnTop(count) + index * (NODE_H + GAP_Y) + NODE_H / 2;
    }

    function renderNodes(title, list, colIndex) {
        var cx = COL_X[colIndex];
        var left = cx - NODE_W / 2;
        var count = list.length;
        var top = columnTop(count);
        var svg = '';

        svg += '<text class="topo-col-title" x="' + cx + '" y="' + (top - 16) + '" text-anchor="middle">'
            + escapeHtml(title) + '</text>';

        if (count === 0) {
            svg += '<rect class="topo-empty" x="' + left + '" y="' + (MID_Y - NODE_H / 2) + '"'
                + ' width="' + NODE_W + '" height="' + NODE_H + '" rx="10"/>'
                + '<text class="topo-empty-text" x="' + cx + '" y="' + (MID_Y + 4)
                + '" text-anchor="middle">暂无设备</text>';
            return svg;
        }

        list.forEach(function (d, index) {
            var y = top + index * (NODE_H + GAP_Y);
            var color = STATUS_COLOR[d.status] || STATUS_COLOR.OFFLINE;
            var running = d.status === 'RUNNING';
            svg += '<g class="tnode' + (running ? ' is-running' : '') + '" data-device="'
                + escapeHtml(d.deviceId) + '" tabindex="0" role="button"'
                + ' aria-label="' + escapeHtml(d.deviceId + ' ' + d.deviceName + ' ' + (d.statusLabel || '')) + '">'
                + '<rect class="tnode-box" x="' + left + '" y="' + y + '" width="' + NODE_W + '" height="' + NODE_H
                + '" rx="10" stroke="' + color + '"/>'
                + '<circle class="tnode-dot" cx="' + (left + NODE_W - 14) + '" cy="' + (y + 15)
                + '" r="4" fill="' + color + '"/>'
                + '<text class="tnode-id" x="' + (left + 12) + '" y="' + (y + 19) + '">'
                + escapeHtml(d.deviceId) + '</text>'
                + '<text class="tnode-name" x="' + (left + 12) + '" y="' + (y + 37) + '">'
                + escapeHtml(truncate(d.deviceName, 9)) + '</text>'
                + '<text class="tnode-val" x="' + (left + 12) + '" y="' + (y + 54) + '">'
                + escapeHtml(truncate(dash(d.keyParameterSummary), 13)) + '</text>'
                + '</g>';
        });
        return svg;
    }

    /**
     * 绘制一段「上游客群 → 下游客群」的供水管路。
     * 两侧各画一根集管（沿设备边缘的竖直母线），集中管再接到下一列的集管上，
     * 这样无论每列有几台设备，管路都不会随着设备数量增长而变成一团麻。
     */
    function renderSegment(from, to, label, colFrom, colTo) {
        var cxA = COL_X[colFrom];
        var cxB = COL_X[colTo];
        var manifoldR = cxA + NODE_W / 2 + 14;
        var manifoldL = cxB - NODE_W / 2 - 14;
        var svg = '';
        var flowing = hasRunning(from);

        // 上游集管
        if (from.length > 0) {
            var yA = from.map(function (d, i) {
                return centerY(i, from.length);
            });
            from.forEach(function (d, i) {
                svg += '<line class="topo-stub" x1="' + (cxA + NODE_W / 2) + '" y1="' + yA[i]
                    + '" x2="' + manifoldR + '" y2="' + yA[i] + '"/>';
            });
            svg += '<line class="topo-manifold" x1="' + manifoldR + '" y1="' + yA[0]
                + '" x2="' + manifoldR + '" y2="' + yA[yA.length - 1] + '"/>';
        }

        // 下游集管
        if (to.length > 0) {
            var yB = to.map(function (d, i) {
                return centerY(i, to.length);
            });
            to.forEach(function (d, i) {
                svg += '<line class="topo-stub" x1="' + manifoldL + '" y1="' + yB[i]
                    + '" x2="' + (cxB - NODE_W / 2) + '" y2="' + yB[i] + '"/>';
            });
            svg += '<line class="topo-manifold" x1="' + manifoldL + '" y1="' + yB[0]
                + '" x2="' + manifoldL + '" y2="' + yB[yB.length - 1] + '"/>';
        }

        // 主连接管 + 流向
        svg += '<line class="topo-pipe" x1="' + manifoldR + '" y1="' + MID_Y
            + '" x2="' + manifoldL + '" y2="' + MID_Y + '" marker-end="url(#arrowSupply)"/>';
        if (flowing) {
            svg += '<line class="topo-pipe-flow" x1="' + manifoldR + '" y1="' + MID_Y
                + '" x2="' + manifoldL + '" y2="' + MID_Y + '"/>';
        }

        // 管路名称贴在主连接管上方
        var mid = (manifoldR + manifoldL) / 2;
        svg += '<text class="topo-pipe-label" x="' + mid + '" y="' + (MID_Y - 10)
            + '" text-anchor="middle">' + escapeHtml(label) + '</text>';
        return svg;
    }

    /** 回水管：沿画布底部折回，虚线表示回水方向 */
    function renderReturn(buckets) {
        var svg = '';

        // 冷冻水回水：精密空调 → 冷水机组
        if (buckets.unit.length > 0 && buckets.chiller.length > 0) {
            var startY = columnTop(buckets.unit.length) + buckets.unit.length * (NODE_H + GAP_Y) - GAP_Y;
            var endY = columnTop(buckets.chiller.length) + buckets.chiller.length * (NODE_H + GAP_Y) - GAP_Y;
            var xFrom = COL_X[4];
            var xTo = COL_X[2] + 42;
            var path = 'M' + xFrom + ' ' + startY
                + ' V430 H' + xTo + ' V' + endY;
            svg += '<path class="topo-return' + (hasRunning(buckets.unit) ? ' is-flowing' : '') + '"'
                + ' d="' + path + '" marker-end="url(#arrowReturn)"/>';
            svg += '<text class="topo-return-label" x="' + ((xFrom + xTo) / 2 + 40) + '" y="424">'
                + '冷冻水回水</text>';
        }

        // 冷却水回水：冷水机组 → 冷却塔
        if (buckets.chiller.length > 0 && buckets.tower.length > 0) {
            var cStartY = columnTop(buckets.chiller.length) + buckets.chiller.length * (NODE_H + GAP_Y) - GAP_Y;
            var cEndY = columnTop(buckets.tower.length) + buckets.tower.length * (NODE_H + GAP_Y) - GAP_Y;
            var cxFrom = COL_X[2] - 42;
            var cxTo = COL_X[0];
            var cpath = 'M' + cxFrom + ' ' + cStartY
                + ' V452 H' + cxTo + ' V' + cEndY;
            svg += '<path class="topo-return' + (hasRunning(buckets.chiller) ? ' is-flowing' : '') + '"'
                + ' d="' + cpath + '" marker-end="url(#arrowReturn)"/>';
            svg += '<text class="topo-return-label" x="' + ((cxFrom + cxTo) / 2) + '" y="446">'
                + '冷却水回水</text>';
        }
        return svg;
    }

    function renderTopology(buckets) {
        var container = $('topology');
        var svg = '<svg class="topo-svg" viewBox="0 0 ' + CANVAS_W + ' ' + CANVAS_H + '"'
            + ' preserveAspectRatio="xMidYMid meet" role="img"'
            + ' aria-label="设备连接关系：冷却塔到冷却水泵到冷水机组，冷水机组到冷冻水泵到精密空调">';

        svg += '<defs>'
            + '<marker id="arrowSupply" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7"'
            + ' orient="auto-start-reverse"><path d="M0 0 L10 5 L0 10 z" fill="#6fd3ff"/></marker>'
            + '<marker id="arrowReturn" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7"'
            + ' orient="auto-start-reverse"><path d="M0 0 L10 5 L0 10 z" fill="#8fa6c0"/></marker>'
            + '<linearGradient id="flowGrad" x1="0" y1="0" x2="1" y2="0">'
            + '<stop offset="0%" stop-color="#6fd3ff" stop-opacity="0"/>'
            + '<stop offset="50%" stop-color="#9fe4ff"/>'
            + '<stop offset="100%" stop-color="#6fd3ff" stop-opacity="0.2"/>'
            + '</linearGradient>'
            + '</defs>';

        // 背景网格，强化「大屏」质感
        svg += '<g class="topo-grid">';
        for (var gx = 40; gx < CANVAS_W; gx += 40) {
            svg += '<line x1="' + gx + '" y1="0" x2="' + gx + '" y2="' + CANVAS_H + '"/>';
        }
        for (var gy = 20; gy < CANVAS_H; gy += 40) {
            svg += '<line x1="0" y1="' + gy + '" x2="' + CANVAS_W + '" y2="' + gy + '"/>';
        }
        svg += '</g>';

        svg += renderReturn(buckets);
        svg += renderSegment(buckets.tower, buckets.coolingPump, '冷却水供水', 0, 1);
        svg += renderSegment(buckets.coolingPump, buckets.chiller, '冷却水供水', 1, 2);
        svg += renderSegment(buckets.chiller, buckets.chilledPump, '冷冻水供水', 2, 3);
        svg += renderSegment(buckets.chilledPump, buckets.unit, '冷冻水供水', 3, 4);

        svg += renderNodes('冷却塔', buckets.tower, 0);
        svg += renderNodes('冷却水泵', buckets.coolingPump, 1);
        svg += renderNodes('冷水机组', buckets.chiller, 2);
        svg += renderNodes('冷冻水泵', buckets.chilledPump, 3);
        svg += renderNodes('精密空调', buckets.unit, 4);

        svg += '</svg>';
        container.innerHTML = svg;
    }

    /* ------------------------------ KPI 与概况 ------------------------------ */

    function countByStatus(list, status) {
        return list.filter(function (d) {
            return d.status === status;
        }).length;
    }

    function renderKpi() {
        var list = state.devices;
        var zones = {};
        list.forEach(function (d) {
            if (d.zone) {
                zones[d.zone] = 1;
            }
        });
        $('kpiTotal').textContent = list.length;
        $('kpiRunning').textContent = countByStatus(list, 'RUNNING');
        $('kpiAbnormal').textContent = countByStatus(list, 'FAULT') + countByStatus(list, 'OFFLINE');
        $('kpiMaintenance').textContent = countByStatus(list, 'MAINTENANCE');
        $('kpiAlarm').textContent = state.overview ? (state.overview.unhandledAlarms || 0) : '-';
        $('kpiZone').textContent = Object.keys(zones).length;
    }

    function renderTypeMatrix() {
        var types = ['CHILLER', 'COOLING_TOWER', 'PUMP', 'COOLING_UNIT'];
        var statuses = ['RUNNING', 'STOPPED', 'FAULT', 'MAINTENANCE', 'OFFLINE'];
        var html = '<table class="matrix"><thead><tr><th>类型</th><th>总数</th>';
        statuses.forEach(function (st) {
            html += '<th class="m-' + st + '">' + App.STATUS_LABEL[st].replace('已', '') + '</th>';
        });
        html += '</tr></thead><tbody>';

        var totals = {total: 0};
        statuses.forEach(function (st) {
            totals[st] = 0;
        });

        types.forEach(function (type) {
            var list = state.devices.filter(function (d) {
                return d.deviceType === type;
            });
            totals.total += list.length;
            html += '<tr><td class="m-type">' + App.TYPE_LABEL[type] + '</td><td class="m-total">'
                + list.length + '</td>';
            statuses.forEach(function (st) {
                var n = countByStatus(list, st);
                totals[st] += n;
                html += '<td class="' + (n > 0 ? 'm-hit m-' + st : 'm-zero') + '">' + n + '</td>';
            });
            html += '</tr>';
        });

        html += '<tr class="m-sum"><td class="m-type">合计</td><td class="m-total">' + totals.total + '</td>';
        statuses.forEach(function (st) {
            html += '<td>' + totals[st] + '</td>';
        });
        html += '</tr></tbody></table>';
        $('typeMatrix').innerHTML = html;
    }

    function renderZoneSummary() {
        var map = {};
        state.devices.forEach(function (d) {
            var key = d.zone || '未分配';
            if (!map[key]) {
                map[key] = {zone: key, total: 0, RUNNING: 0, STOPPED: 0, FAULT: 0, MAINTENANCE: 0, OFFLINE: 0};
            }
            map[key].total++;
            map[key][d.status] = (map[key][d.status] || 0) + 1;
        });
        var zones = Object.keys(map).map(function (k) {
            return map[k];
        }).sort(function (a, b) {
            return b.total - a.total;
        });

        if (zones.length === 0) {
            $('zoneSummary').innerHTML = '<p class="empty">暂无设备</p>';
            return;
        }

        var max = zones[0].total || 1;
        var order = ['RUNNING', 'MAINTENANCE', 'FAULT', 'OFFLINE', 'STOPPED'];
        $('zoneSummary').innerHTML = zones.map(function (z) {
            var segments = order.filter(function (st) {
                return z[st] > 0;
            }).map(function (st) {
                var width = z[st] / z.total * 100;
                return '<i class="zbar-seg z-' + st + '" style="width:' + width + '%" title="'
                    + App.STATUS_LABEL[st] + ' ' + z[st] + ' 台"></i>';
            }).join('');
            return '<div class="zone-sum-item">'
                + '<div class="zone-sum-line">'
                + '<span class="zone-sum-name">' + escapeHtml(z.zone) + '</span>'
                + '<span class="zone-sum-count">' + z.total + ' 台</span>'
                + '</div>'
                + '<div class="zone-sum-bar" style="max-width:' + Math.max(12, z.total / max * 100) + '%">'
                + segments + '</div>'
                + '</div>';
        }).join('');
    }

    function renderRecentAlarms() {
        var list = state.alarms || [];
        var box = $('recentAlarms');
        if (list.length === 0) {
            box.innerHTML = '<li class="empty">暂无告警记录</li>';
            return;
        }
        box.innerHTML = list.map(function (a) {
            var action = a.status === 'UNHANDLED'
                ? '<button class="btn btn-sm" data-dash-handle="' + a.id + '">处理</button>'
                : '<span class="hint">' + escapeHtml(dash(a.handler)) + '</span>';
            return '<li class="alarm-mini-item">'
                + '<span class="badge badge-' + escapeHtml(a.alarmLevel) + '">'
                + escapeHtml(a.alarmLevelLabel) + '</span>'
                + '<div class="alarm-mini-main">'
                + '<div class="alarm-mini-title">' + escapeHtml(a.deviceName || a.deviceId)
                + ' · ' + escapeHtml(a.paramName) + '</div>'
                + '<div class="alarm-mini-sub">' + escapeHtml(dash(a.alarmNo))
                + ' · ' + escapeHtml(dash(a.triggerTime)) + '</div>'
                + '</div>'
                + '<span class="alarm-mini-value">'
                + (a.actualValue === null || a.actualValue === undefined ? '-' : a.actualValue.toFixed(1))
                + '</span>'
                + action
                + '</li>';
        }).join('');
    }

    /* ------------------------------ 加载 ------------------------------ */

    function load() {
        return Promise.all([
            request('/api/devices?pageSize=200').catch(function () {
                return {list: []};
            }),
            request('/api/statistics/overview').catch(function () {
                return null;
            }),
            request('/api/alarms?pageNum=1&pageSize=5').catch(function () {
                return {list: []};
            }),
            request('/api/monitor/devices?deviceType=PUMP').catch(function () {
                return [];
            })
        ]).then(function (res) {
            state.devices = (res[0] && res[0].list) || [];
            state.overview = res[1];
            state.alarms = (res[2] && res[2].list) || [];
            state.pumps = res[3] || [];
            state.loaded = true;

            renderKpi();
            renderTopology(bucketize(state.devices, state.pumps));
            renderTypeMatrix();
            renderZoneSummary();
            renderRecentAlarms();
            App.markUpdated();
        }).catch(function (err) {
            $('topology').innerHTML = '<p class="empty">' + escapeHtml(err.message) + '</p>';
            toast(err.message, 'error');
        });
    }

    /* ------------------------------ 事件 ------------------------------ */

    function bind() {
        // KPI 卡：跳到对应页面并预置筛选条件
        Array.prototype.forEach.call(document.querySelectorAll('#kpiRow .kpi'), function (card) {
            card.addEventListener('click', function () {
                var key = card.getAttribute('data-kpi');
                if (key === 'alarm') {
                    App.showTab('alarm');
                    if (App.filterAlarms) {
                        App.filterAlarms({status: 'UNHANDLED'});
                    }
                    return;
                }
                App.showTab('device');
                if (!App.filterDevices) {
                    return;
                }
                if (key === 'running') {
                    App.filterDevices({status: 'RUNNING'});
                } else if (key === 'abnormal') {
                    App.filterDevices({status: 'FAULT'});
                } else if (key === 'maintenance') {
                    App.filterDevices({status: 'MAINTENANCE'});
                } else if (key === 'zone') {
                    App.filterDevices({reset: true});
                    App.showDeviceZoneView();
                } else {
                    App.filterDevices({reset: true});
                }
            });
        });

        // 拓扑节点：点击看设备详情
        $('topology').addEventListener('click', function (e) {
            var node = e.target.closest ? e.target.closest('[data-device]') : null;
            if (node && App.openDeviceDetail) {
                App.openDeviceDetail(node.getAttribute('data-device'));
            }
        });
        $('topology').addEventListener('keydown', function (e) {
            if (e.key !== 'Enter' && e.key !== ' ') {
                return;
            }
            var node = e.target.closest ? e.target.closest('[data-device]') : null;
            if (node && App.openDeviceDetail) {
                e.preventDefault();
                App.openDeviceDetail(node.getAttribute('data-device'));
            }
        });

        // 最近告警里的「处理」按钮
        $('recentAlarms').addEventListener('click', function (e) {
            var btn = e.target.closest('[data-dash-handle]');
            if (btn && App.openHandleAlarm) {
                App.openHandleAlarm(Number(btn.getAttribute('data-dash-handle')));
            }
        });

        document.addEventListener('tab:shown', function (e) {
            if (e.detail === 'dashboard') {
                load();
            }
        });

        // 任一模块完成写操作后刷新总览，保证首页数字不滞后
        document.addEventListener('data:changed', function () {
            if (!$('tab-dashboard').classList.contains('hidden')) {
                load();
            }
        });
    }

    document.addEventListener('DOMContentLoaded', bind);

    window.Dashboard = {
        reload: load,
        buckets: function () {
            return bucketize(state.devices, state.pumps);
        }
    };
})();
