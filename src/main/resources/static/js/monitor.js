/* =====================================================================
   运行监控模块（MM-01 ~ MM-05）
   ===================================================================== */
(function () {
    'use strict';

    var App = window.App;
    var request = App.request;
    var escapeHtml = App.escapeHtml;
    var toast = App.toast;

    var state = {
        deviceType: 'CHILLER'
    };

    function statusBadge(status, label) {
        return '<span class="badge badge-' + escapeHtml(status) + '">' + escapeHtml(label || status) + '</span>';
    }

    function renderCards(list) {
        var container = App.$('monitorCards');
        if (!list || list.length === 0) {
            container.innerHTML = '<p class="empty">该类型下暂无设备</p>';
            return;
        }
        container.innerHTML = list.map(function (d) {
            var sensors = (d.sensors || []).map(function (s) {
                return '<div class="mon-item">'
                    + '<span class="k">' + escapeHtml(s.name) + '</span>'
                    + '<span class="v' + (s.outOfRange ? ' out' : '') + '">'
                    + s.value.toFixed(1) + '<small>' + escapeHtml(s.unit) + '</small></span>'
                    + '</div>';
            }).join('');
            var actions = '';
            if (d.status === 'RUNNING') {
                actions = '<button class="btn btn-sm btn-danger" data-mact="stop" data-id="'
                    + escapeHtml(d.deviceId) + '">停机</button>';
            } else if (d.status === 'STOPPED') {
                actions = '<button class="btn btn-sm btn-primary" data-mact="start" data-id="'
                    + escapeHtml(d.deviceId) + '">启动</button>';
            } else {
                actions = '<span class="hint">当前状态不允许启停</span>';
            }
            return '<div class="mon-card">'
                + '<div class="mon-head">'
                + '<span class="dev-id">' + escapeHtml(d.deviceId) + '</span>'
                + '<span class="dev-name">' + escapeHtml(d.deviceName) + '</span>'
                + statusBadge(d.status, d.statusLabel)
                + '</div>'
                + '<div class="mon-body">' + sensors + '</div>'
                + '<div class="mon-foot">' + actions
                + '<span class="hint" style="margin-left:auto">'
                + escapeHtml(d.lastCollectTime ? d.lastCollectTime + ' 采集' : '未采集')
                + '</span>'
                + '</div>'
                + '</div>';
        }).join('');
    }

    function load() {
        App.$('monitorTitle').textContent = '设备运行状态 · ' + (App.TYPE_LABEL[state.deviceType] || '');
        App.$('monitorCards').innerHTML = '<p class="empty">加载中…</p>';
        request('/api/monitor/devices?deviceType=' + encodeURIComponent(state.deviceType))
            .then(function (list) {
                renderCards(list);
                App.markUpdated();
            })
            .catch(function (err) {
                App.$('monitorCards').innerHTML = '<p class="empty">' + escapeHtml(err.message) + '</p>';
                toast(err.message, 'error');
            });
    }

    function changeStatus(deviceId, action) {
        request('/api/monitor/devices/' + encodeURIComponent(deviceId) + '/' + action, {method: 'POST'})
            .then(function () {
                toast(action === 'start' ? '设备已启动' : '设备已停机', 'success');
                load();
                // 通知总览与设备台账：状态变了，首页数字与列表都要跟着变
                App.notifyChange();
            })
            .catch(function (err) {
                toast(err.message, 'error');
            });
    }

    function collectOnce() {
        App.$('btnCollect').disabled = true;
        request('/api/monitor/collect', {method: 'POST'})
            .then(function (result) {
                toast('采集完成：' + result.deviceCount + ' 台设备 / ' + result.dataPoints
                    + ' 个数据点 / 新告警 ' + result.newAlarms + ' 条', 'success');
                load();
                App.notifyChange();
            })
            .catch(function (err) {
                toast(err.message, 'error');
            })
            .then(function () {
                App.$('btnCollect').disabled = false;
            });
    }

    function bind() {
        Array.prototype.forEach.call(document.querySelectorAll('#monitorTypes .seg'), function (seg) {
            seg.addEventListener('click', function () {
                Array.prototype.forEach.call(document.querySelectorAll('#monitorTypes .seg'), function (item) {
                    item.classList.toggle('active', item === seg);
                });
                state.deviceType = seg.getAttribute('data-mtype');
                load();
            });
        });

        App.$('btnRefreshMonitor').addEventListener('click', load);
        App.$('btnCollect').addEventListener('click', collectOnce);

        document.addEventListener('click', function (e) {
            var target = e.target.closest('[data-mact]');
            if (!target) {
                return;
            }
            changeStatus(target.getAttribute('data-id'), target.getAttribute('data-mact'));
        });

        document.addEventListener('tab:shown', function (e) {
            if (e.detail === 'monitor') {
                load();
            }
        });
    }

    document.addEventListener('DOMContentLoaded', bind);
})();
