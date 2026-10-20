/* =====================================================================
   告警管理模块（AM-01 ~ AM-05）
   ===================================================================== */
(function () {
    'use strict';

    var App = window.App;
    var request = App.request;
    var escapeHtml = App.escapeHtml;
    var dash = App.dash;
    var toast = App.toast;

    var state = {
        pageNum: 1,
        pageSize: 10,
        handlingId: null,
        editingThresholdId: null,
        thresholds: []
    };

    /* ------------------------------ 告警列表 ------------------------------ */

    function loadAlarms() {
        var params = new URLSearchParams();
        if (App.$('qAlarmStatus').value) {
            params.set('status', App.$('qAlarmStatus').value);
        }
        if (App.$('qAlarmLevel').value) {
            params.set('alarmLevel', App.$('qAlarmLevel').value);
        }
        params.set('pageNum', state.pageNum);
        params.set('pageSize', state.pageSize);

        var tbody = App.$('alarmTbody');
        tbody.innerHTML = '<tr><td colspan="8" class="empty">加载中…</td></tr>';
        request('/api/alarms?' + params.toString())
            .then(function (page) {
                renderAlarmRows(page.list);
                renderAlarmPager(page);
            })
            .catch(function (err) {
                tbody.innerHTML = '<tr><td colspan="8" class="empty">' + escapeHtml(err.message) + '</td></tr>';
                toast(err.message, 'error');
            });
    }

    function renderAlarmRows(list) {
        var tbody = App.$('alarmTbody');
        if (!list || list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="empty">暂无告警记录</td></tr>';
            return;
        }
        tbody.innerHTML = list.map(function (a) {
            var action = a.status === 'UNHANDLED'
                ? '<button class="btn btn-sm btn-primary" data-alarm-handle="' + a.id + '">处理</button>'
                : '<span class="hint">' + escapeHtml(dash(a.handler)) + '</span>';
            return '<tr>'
                + '<td class="mono">' + escapeHtml(a.alarmNo) + '</td>'
                + '<td>' + escapeHtml(a.deviceName) + '<span class="hint"> ' + escapeHtml(a.deviceId) + '</span></td>'
                + '<td>' + escapeHtml(a.paramName) + '</td>'
                + '<td>' + (a.actualValue === null ? '-' : a.actualValue.toFixed(1)) + '</td>'
                + '<td><span class="badge badge-' + escapeHtml(a.alarmLevel) + '">'
                + escapeHtml(a.alarmLevelLabel) + '</span></td>'
                + '<td><span class="badge badge-' + escapeHtml(a.status) + '">'
                + escapeHtml(a.statusLabel) + '</span></td>'
                + '<td>' + escapeHtml(dash(a.triggerTime)) + '</td>'
                + '<td class="col-actions">' + action + '</td>'
                + '</tr>';
        }).join('');
    }

    function renderAlarmPager(page) {
        var pager = App.$('alarmPager');
        if (!page || page.total === 0) {
            pager.innerHTML = '';
            return;
        }
        pager.innerHTML = '<button data-apage="prev"' + (page.pageNum <= 1 ? ' disabled' : '') + '>上一页</button>'
            + '<span>第 ' + page.pageNum + ' / ' + page.pages + ' 页，共 ' + page.total + ' 条</span>'
            + '<button data-apage="next"' + (page.pageNum >= page.pages ? ' disabled' : '') + '>下一页</button>';
    }

    function openHandle(id) {
        state.handlingId = id;
        App.$('handleAlarmInfo').textContent = '告警 ID：' + id + '，请填写处理人与处理措施';
        App.$('hHandler').value = '';
        App.$('hMeasure').value = '';
        App.$('handleModal').classList.remove('hidden');
    }

    function submitHandle() {
        var handler = App.$('hHandler').value.trim();
        var measure = App.$('hMeasure').value.trim();
        if (!handler || !measure) {
            toast('处理人与处理措施均为必填', 'error');
            return;
        }
        request('/api/alarms/' + state.handlingId + '/handle', {
            method: 'PATCH',
            body: JSON.stringify({handler: handler, measure: measure})
        }).then(function () {
            App.$('handleModal').classList.add('hidden');
            toast('告警已处理', 'success');
            loadAlarms();
            document.dispatchEvent(new CustomEvent('data:changed'));
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    /* ------------------------------ 阈值配置 ------------------------------ */

    function fillParamOptions(deviceType, selectedKey) {
        var options = App.SENSOR_OPTIONS[deviceType] || [];
        App.$('tParamKey').innerHTML = options.map(function (item) {
            return '<option value="' + item.key + '"'
                + (item.key === selectedKey ? ' selected' : '') + '>' + item.label + '</option>';
        }).join('');
    }

    function loadThresholds() {
        var tbody = App.$('thresholdTbody');
        tbody.innerHTML = '<tr><td colspan="6" class="empty">加载中…</td></tr>';
        request('/api/alarm-thresholds')
            .then(function (list) {
                state.thresholds = list;
                if (!list || list.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" class="empty">暂无阈值配置</td></tr>';
                    return;
                }
                tbody.innerHTML = list.map(function (t) {
                    return '<tr>'
                        + '<td>' + escapeHtml(t.deviceTypeLabel) + '</td>'
                        + '<td>' + escapeHtml(t.expression) + '</td>'
                        + '<td><span class="badge badge-' + escapeHtml(t.alarmLevel) + '">'
                        + escapeHtml(t.alarmLevelLabel) + '</span></td>'
                        + '<td>' + (t.enabled ? '<span class="badge badge-RUNNING">启用</span>'
                            : '<span class="badge badge-STOPPED">停用</span>') + '</td>'
                        + '<td class="hint">' + escapeHtml(dash(t.remark)) + '</td>'
                        + '<td class="col-actions">'
                        + '<button class="btn btn-sm" data-th-edit="' + t.id + '">编辑</button>'
                        + '<button class="btn btn-sm" data-th-toggle="' + t.id + '" data-enabled="'
                        + (t.enabled ? 'false' : 'true') + '">' + (t.enabled ? '停用' : '启用') + '</button>'
                        + '<button class="btn btn-sm btn-danger" data-th-del="' + t.id + '">删除</button>'
                        + '</td>'
                        + '</tr>';
                }).join('');
            })
            .catch(function (err) {
                tbody.innerHTML = '<tr><td colspan="6" class="empty">' + escapeHtml(err.message) + '</td></tr>';
                toast(err.message, 'error');
            });
    }

    function openThreshold(threshold) {
        state.editingThresholdId = threshold ? threshold.id : null;
        App.$('thresholdTitle').textContent = threshold ? '编辑阈值' : '新增阈值';
        App.$('tDeviceType').value = threshold ? threshold.deviceType : 'CHILLER';
        App.$('tDeviceType').disabled = !!threshold;
        App.$('tCompareOp').value = threshold ? threshold.compareOp : 'GT';
        App.$('tThresholdValue').value = threshold ? threshold.thresholdValue : '';
        App.$('tDeviationPercent').value = threshold && threshold.deviationPercent ? threshold.deviationPercent : '';
        App.$('tAlarmLevel').value = threshold ? threshold.alarmLevel : 'WARNING';
        App.$('tEnabled').value = threshold ? String(threshold.enabled) : 'true';
        App.$('tRemark').value = threshold ? (threshold.remark || '') : '';
        fillParamOptions(App.$('tDeviceType').value, threshold ? threshold.paramKey : null);
        App.$('thresholdModal').classList.remove('hidden');
    }

    function saveThreshold() {
        var deviceType = App.$('tDeviceType').value;
        var compareOp = App.$('tCompareOp').value;
        var payload = {
            deviceType: deviceType,
            paramKey: App.$('tParamKey').value,
            compareOp: compareOp,
            thresholdValue: Number(App.$('tThresholdValue').value),
            deviationPercent: App.$('tDeviationPercent').value === ''
                ? null : Number(App.$('tDeviationPercent').value),
            alarmLevel: App.$('tAlarmLevel').value,
            enabled: App.$('tEnabled').value === 'true',
            remark: App.$('tRemark').value.trim()
        };
        if (App.$('tThresholdValue').value === '' || isNaN(payload.thresholdValue)) {
            toast('请填写有效的阈值', 'error');
            return;
        }
        var isEdit = state.editingThresholdId !== null;
        var url = isEdit ? '/api/alarm-thresholds/' + state.editingThresholdId : '/api/alarm-thresholds';
        request(url, {
            method: isEdit ? 'PUT' : 'POST',
            body: JSON.stringify(payload)
        }).then(function () {
            App.$('thresholdModal').classList.add('hidden');
            toast(isEdit ? '阈值已更新' : '阈值已新增', 'success');
            loadThresholds();
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    /* ------------------------------ 初始化 ------------------------------ */

    function bind() {
        App.$('qAlarmStatus').addEventListener('change', function () {
            state.pageNum = 1;
            loadAlarms();
        });
        App.$('qAlarmLevel').addEventListener('change', function () {
            state.pageNum = 1;
            loadAlarms();
        });
        App.$('btnRefreshAlarm').addEventListener('click', loadAlarms);
        App.$('btnSubmitHandle').addEventListener('click', submitHandle);
        App.$('btnAddThreshold').addEventListener('click', function () {
            openThreshold(null);
        });
        App.$('btnSaveThreshold').addEventListener('click', saveThreshold);
        App.$('tDeviceType').addEventListener('change', function () {
            fillParamOptions(this.value, null);
        });

        Array.prototype.forEach.call(document.querySelectorAll('[data-aview]'), function (seg) {
            seg.addEventListener('click', function () {
                Array.prototype.forEach.call(document.querySelectorAll('[data-aview]'), function (item) {
                    item.classList.toggle('active', item === seg);
                });
                var view = seg.getAttribute('data-aview');
                App.$('alarmListView').classList.toggle('hidden', view !== 'list');
                App.$('thresholdView').classList.toggle('hidden', view !== 'threshold');
                if (view === 'threshold') {
                    loadThresholds();
                } else {
                    loadAlarms();
                }
            });
        });

        App.$('alarmPager').addEventListener('click', function (e) {
            var btn = e.target.closest('button[data-apage]');
            if (!btn || btn.disabled) {
                return;
            }
            state.pageNum += btn.getAttribute('data-apage') === 'next' ? 1 : -1;
            loadAlarms();
        });

        document.addEventListener('click', function (e) {
            var handleBtn = e.target.closest('[data-alarm-handle]');
            if (handleBtn) {
                openHandle(handleBtn.getAttribute('data-alarm-handle'));
                return;
            }
            var editBtn = e.target.closest('[data-th-edit]');
            if (editBtn) {
                var id = Number(editBtn.getAttribute('data-th-edit'));
                var found = state.thresholds.filter(function (t) {
                    return t.id === id;
                })[0];
                openThreshold(found);
                return;
            }
            var toggleBtn = e.target.closest('[data-th-toggle]');
            if (toggleBtn) {
                var tid = Number(toggleBtn.getAttribute('data-th-toggle'));
                var enabled = toggleBtn.getAttribute('data-enabled') === 'true';
                request('/api/alarm-thresholds/' + tid + '/enabled?enabled=' + enabled, {method: 'PATCH'})
                    .then(function () {
                        toast(enabled ? '阈值已启用' : '阈值已停用', 'success');
                        loadThresholds();
                    })
                    .catch(function (err) {
                        toast(err.message, 'error');
                    });
                return;
            }
            var delBtn = e.target.closest('[data-th-del]');
            if (delBtn) {
                if (!window.confirm('确认删除该阈值配置吗？')) {
                    return;
                }
                request('/api/alarm-thresholds/' + delBtn.getAttribute('data-th-del'), {method: 'DELETE'})
                    .then(function () {
                        toast('阈值已删除', 'success');
                        loadThresholds();
                    })
                    .catch(function (err) {
                        toast(err.message, 'error');
                    });
            }
        });

        document.addEventListener('tab:shown', function (e) {
            if (e.detail === 'alarm') {
                loadAlarms();
            }
        });

        // 设备启停、采集后同步刷新告警数量
        document.addEventListener('data:changed', function () {
            if (!App.$('tab-alarm').classList.contains('hidden')) {
                loadAlarms();
            }
        });

        loadAlarms();
    }

    document.addEventListener('DOMContentLoaded', bind);
})();
