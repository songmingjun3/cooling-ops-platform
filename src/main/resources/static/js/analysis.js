/* =====================================================================
   数据分析模块（AN-01 统计 / AN-02 趋势简图）
   图表使用原生 SVG 绘制，不依赖任何第三方图表库
   ===================================================================== */
(function () {
    'use strict';

    var App = window.App;
    var request = App.request;
    var escapeHtml = App.escapeHtml;
    var toast = App.toast;

    var state = {
        devices: []
    };

    /* ------------------------------ 统计概览 ------------------------------ */

    function statCard(label, value) {
        return '<div class="stat-card"><div class="n">' + value + '</div><div class="t">' + label + '</div></div>';
    }

    function renderBars(containerId, items) {
        var container = App.$(containerId);
        if (!items || items.length === 0) {
            container.innerHTML = '<p class="empty">暂无数据</p>';
            return;
        }
        var max = items.reduce(function (acc, item) {
            return Math.max(acc, item.count);
        }, 0) || 1;
        container.innerHTML = items.map(function (item) {
            var ratio = Math.round(item.count / max * 100);
            return '<div class="bar-item">'
                + '<span class="label">' + escapeHtml(item.label || item.name) + '</span>'
                + '<span class="bar-track"><span class="bar-fill" style="width:' + ratio + '%"></span></span>'
                + '<span class="count">' + item.count + '</span>'
                + '</div>';
        }).join('');
    }

    function loadStats() {
        request('/api/statistics/overview').then(function (stat) {
            App.$('statCards').innerHTML =
                statCard('设备总数', stat.total)
                + statCard('运行中', stat.running)
                + statCard('已停机', stat.stopped)
                + statCard('故障', stat.fault)
                + statCard('维护中', stat.maintenance)
                + statCard('未处理告警', stat.unhandledAlarms)
                + statCard('告警总数', stat.totalAlarms);
            renderBars('barByType', stat.byType);
            renderBars('barByStatus', stat.byStatus);
            renderBars('barByZone', stat.byZone);
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    /* ------------------------------ 趋势图 ------------------------------ */

    function fillDeviceSelect() {
        request('/api/devices?pageSize=200').then(function (page) {
            state.devices = page.list || [];
            var select = App.$('trendDevice');
            select.innerHTML = state.devices.map(function (d) {
                return '<option value="' + escapeHtml(d.deviceId) + '">'
                    + escapeHtml(d.deviceId + ' ' + d.deviceName) + '</option>';
            }).join('');
            fillSensorSelect();
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    function currentDevice() {
        var id = App.$('trendDevice').value;
        return state.devices.filter(function (d) {
            return d.deviceId === id;
        })[0];
    }

    function fillSensorSelect() {
        var device = currentDevice();
        var options = device ? (App.SENSOR_OPTIONS[device.deviceType] || []) : [];
        App.$('trendSensor').innerHTML = options.map(function (item) {
            return '<option value="' + item.key + '">' + item.label + '</option>';
        }).join('');
    }

    function drawTrend(trend) {
        var box = App.$('trendChart');
        var points = trend.points || [];
        if (points.length === 0) {
            box.innerHTML = '<p class="empty">该设备暂无采集数据，请先启动设备并等待定时采集</p>';
            App.$('trendMeta').textContent = '';
            return;
        }

        var W = 640;
        var H = 220;
        var padL = 46;
        var padR = 18;
        var padT = 18;
        var padB = 30;

        var values = points.map(function (p) {
            return p.value;
        });
        var yMin = Math.min.apply(null, values);
        var yMax = Math.max.apply(null, values);
        if (trend.minValue !== null && trend.minValue !== undefined) {
            yMin = Math.min(yMin, trend.minValue);
            yMax = Math.max(yMax, trend.maxValue);
        }
        if (yMax - yMin < 0.001) {
            yMax = yMin + 1;
        }
        var padding = (yMax - yMin) * 0.1;
        yMin -= padding;
        yMax += padding;

        function x(index) {
            if (points.length === 1) {
                return padL + (W - padL - padR) / 2;
            }
            return padL + (W - padL - padR) * index / (points.length - 1);
        }

        function y(value) {
            return padT + (H - padT - padB) * (1 - (value - yMin) / (yMax - yMin));
        }

        var svg = '<svg class="trend-svg" viewBox="0 0 ' + W + ' ' + H + '" preserveAspectRatio="none">';

        // 网格与纵轴刻度
        for (var i = 0; i <= 4; i++) {
            var value = yMin + (yMax - yMin) * i / 4;
            var py = y(value);
            svg += '<line x1="' + padL + '" y1="' + py + '" x2="' + (W - padR) + '" y2="' + py
                + '" stroke="#e6ecf3" stroke-width="1"/>';
            svg += '<text x="' + (padL - 8) + '" y="' + (py + 4) + '" text-anchor="end"'
                + ' font-size="11" fill="#8395a9">' + value.toFixed(1) + '</text>';
        }

        // 正常区间参考带
        if (trend.minValue !== null && trend.minValue !== undefined) {
            var bandTop = y(trend.maxValue);
            var bandBottom = y(trend.minValue);
            svg += '<rect x="' + padL + '" y="' + bandTop + '" width="' + (W - padL - padR)
                + '" height="' + Math.max(1, bandBottom - bandTop) + '" fill="rgba(15,157,143,0.07)"/>';
        }

        // 折线
        var linePoints = points.map(function (p, index) {
            return x(index) + ',' + y(p.value);
        }).join(' ');
        svg += '<polyline points="' + linePoints + '" fill="none" stroke="#1769aa" stroke-width="2"'
            + ' stroke-linejoin="round" stroke-linecap="round"/>';

        // 数据点
        points.forEach(function (p, index) {
            svg += '<circle cx="' + x(index) + '" cy="' + y(p.value) + '" r="2.5" fill="#1769aa"/>';
        });

        // 横轴时间：首、中、尾
        var ticks = [0, Math.floor(points.length / 2), points.length - 1];
        ticks.forEach(function (index) {
            if (index < 0 || index >= points.length) {
                return;
            }
            svg += '<text x="' + x(index) + '" y="' + (H - 8) + '" text-anchor="middle"'
                + ' font-size="11" fill="#8395a9">' + escapeHtml(points[index].time) + '</text>';
        });

        svg += '</svg>';
        box.innerHTML = svg;

        var latest = points[points.length - 1];
        App.$('trendMeta').textContent = trend.deviceName + ' · ' + trend.sensorName
            + '：最近 ' + points.length + ' 次采集，最新 ' + latest.value.toFixed(1)
            + (trend.unit || '') + '（绿色区间为合理波动范围）';
    }

    function loadTrend() {
        var deviceId = App.$('trendDevice').value;
        if (!deviceId) {
            toast('请先选择设备', 'error');
            return;
        }
        var params = new URLSearchParams();
        params.set('deviceId', deviceId);
        if (App.$('trendSensor').value) {
            params.set('sensorKey', App.$('trendSensor').value);
        }
        params.set('limit', '30');
        request('/api/statistics/trend?' + params.toString())
            .then(drawTrend)
            .catch(function (err) {
                toast(err.message, 'error');
                App.$('trendChart').innerHTML = '<p class="empty">' + escapeHtml(err.message) + '</p>';
            });
    }

    function bind() {
        App.$('btnRefreshStat').addEventListener('click', loadStats);
        App.$('btnTrend').addEventListener('click', loadTrend);
        App.$('trendDevice').addEventListener('change', function () {
            fillSensorSelect();
        });

        document.addEventListener('tab:shown', function (e) {
            if (e.detail === 'analysis') {
                loadStats();
                if (state.devices.length === 0) {
                    fillDeviceSelect();
                }
            }
        });

        document.addEventListener('data:changed', function () {
            if (!App.$('tab-analysis').classList.contains('hidden')) {
                loadStats();
            }
        });

        loadStats();
        fillDeviceSelect();
    }

    document.addEventListener('DOMContentLoaded', bind);
})();
