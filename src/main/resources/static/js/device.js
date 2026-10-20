/* =====================================================================
   设备管理模块（FM-01 ~ FM-06）前端逻辑
   依赖：原生 fetch，无第三方框架
   ===================================================================== */
(function () {
    'use strict';

    var API = '/api/devices';

    /** 页面状态 */
    var state = {
        pageNum: 1,
        pageSize: 10,
        total: 0,
        view: 'list',
        editId: null
    };

    /** 各设备类型的专有参数定义，用于新增时动态生成表单 */
    var PARAM_DEFS = {
        CHILLER: [
            {key: 'coolingCapacity', label: '制冷量 (kW)', type: 'number', step: '0.1'},
            {key: 'currentLoad', label: '当前负载率 (0–1)', type: 'number', step: '0.01'},
            {key: 'refrigerantType', label: '制冷剂类型', type: 'text', placeholder: '如 R134a'},
            {key: 'chilledWaterSupplyTemp', label: '冷冻水供水温度 (℃)', type: 'number', step: '0.1'},
            {key: 'chilledWaterReturnTemp', label: '冷冻水回水温度 (℃)', type: 'number', step: '0.1'}
        ],
        COOLING_TOWER: [
            {
                key: 'towerType', label: '冷却塔类型', type: 'select',
                options: [{value: 'OPEN', label: '开式'}, {value: 'CLOSED', label: '闭式'}]
            },
            {key: 'fanSpeed', label: '风机转速 (0–100)', type: 'number', step: '1'},
            {key: 'waterFlowRate', label: '水流量 (m³/h)', type: 'number', step: '0.1'},
            {key: 'ambientTemp', label: '环境温度 (℃)', type: 'number', step: '0.1'},
            {key: 'outletTemp', label: '出水温度 (℃)', type: 'number', step: '0.1'}
        ],
        PUMP: [
            {
                key: 'pumpType', label: '水泵类型', type: 'select',
                options: [{value: 'CHILLED_WATER', label: '冷冻水泵'}, {value: 'COOLING_WATER', label: '冷却水泵'}]
            },
            {key: 'flowRate', label: '当前流量 (m³/h)', type: 'number', step: '0.1'},
            {key: 'head', label: '扬程 (m)', type: 'number', step: '0.1'},
            {key: 'power', label: '当前功率 (kW)', type: 'number', step: '0.1'},
            {key: 'speed', label: '转速 (0–100)', type: 'number', step: '1'}
        ],
        COOLING_UNIT: [
            {key: 'supplyTemp', label: '送风温度 (℃)', type: 'number', step: '0.1'},
            {key: 'returnTemp', label: '回风温度 (℃)', type: 'number', step: '0.1'},
            {key: 'speed', label: '转速 (0–100)', type: 'number', step: '1'}
        ]
    };

    /* ----------------------------- 工具函数 ----------------------------- */

    function $(id) {
        return document.getElementById(id);
    }

    function escapeHtml(text) {
        if (text === null || text === undefined) {
            return '';
        }
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function dash(value) {
        return value === null || value === undefined || value === '' ? '-' : value;
    }

    function toast(message, type) {
        var el = $('toast');
        el.textContent = message;
        el.className = 'toast ' + (type || '');
        setTimeout(function () {
            el.className = 'toast hidden';
        }, 2600);
    }

    /**
     * 统一请求封装：解析 Result 结构，code 非 0 时抛出业务提示。
     */
    function request(url, options) {
        var opts = options || {};
        opts.headers = Object.assign({'Content-Type': 'application/json'}, opts.headers || {});
        return fetch(url, opts).then(function (resp) {
            if (!resp.ok) {
                throw new Error('请求失败：HTTP ' + resp.status);
            }
            return resp.json();
        }).then(function (body) {
            if (body.code !== 0) {
                throw new Error(body.message || '操作失败');
            }
            return body.data;
        });
    }

    /* ----------------------------- 设备清单 ----------------------------- */

    function buildQuery() {
        var params = new URLSearchParams();
        var keyword = $('qKeyword').value.trim();
        if (keyword) {
            params.set('keyword', keyword);
        }
        if ($('qType').value) {
            params.set('deviceType', $('qType').value);
        }
        if ($('qZone').value) {
            params.set('zone', $('qZone').value);
        }
        if ($('qStatus').value) {
            params.set('status', $('qStatus').value);
        }
        params.set('pageNum', state.pageNum);
        params.set('pageSize', state.pageSize);
        return params.toString();
    }

    function loadList() {
        var tbody = $('deviceTbody');
        tbody.innerHTML = '<tr><td colspan="7" class="empty">加载中…</td></tr>';
        request(API + '?' + buildQuery())
            .then(function (page) {
                state.total = page.total;
                state.pageNum = page.pageNum;
                renderTable(page.list);
                renderPager(page);
                $('listTitle').textContent = '设备清单（共 ' + page.total + ' 台）';
            })
            .catch(function (err) {
                tbody.innerHTML = '<tr><td colspan="7" class="empty">' + escapeHtml(err.message) + '</td></tr>';
                toast(err.message, 'error');
            });
    }

    function renderTable(list) {
        var tbody = $('deviceTbody');
        if (!list || list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="empty">没有符合条件的设备</td></tr>';
            return;
        }
        var html = list.map(function (d) {
            return '<tr>'
                + '<td class="mono">' + escapeHtml(d.deviceId) + '</td>'
                + '<td>' + escapeHtml(d.deviceName) + '</td>'
                + '<td>' + escapeHtml(d.deviceTypeLabel) + '</td>'
                + '<td>' + escapeHtml(dash(d.zone)) + '</td>'
                + '<td><span class="badge badge-' + escapeHtml(d.status) + '">'
                + escapeHtml(dash(d.statusLabel)) + '</span></td>'
                + '<td>' + escapeHtml(dash(d.keyParameterSummary)) + '</td>'
                + '<td class="col-actions">'
                + '<button class="btn btn-sm" data-action="detail" data-id="' + escapeHtml(d.deviceId) + '">详情</button>'
                + '<button class="btn btn-sm" data-action="edit" data-id="' + escapeHtml(d.deviceId) + '">编辑</button>'
                + '<button class="btn btn-sm btn-danger" data-action="delete" data-id="' + escapeHtml(d.deviceId) + '">删除</button>'
                + '</td>'
                + '</tr>';
        }).join('');
        tbody.innerHTML = html;
    }

    function renderPager(page) {
        var pager = $('pager');
        if (!page || page.total === 0) {
            pager.innerHTML = '';
            return;
        }
        var html = '<button data-page="prev"' + (page.pageNum <= 1 ? ' disabled' : '') + '>上一页</button>';
        for (var i = 1; i <= page.pages; i++) {
            if (page.pages > 7 && Math.abs(i - page.pageNum) > 2 && i !== 1 && i !== page.pages) {
                if (i === 2 || i === page.pages - 1) {
                    html += '<span>…</span>';
                }
                continue;
            }
            html += '<button data-page="' + i + '"' + (i === page.pageNum ? ' class="active"' : '') + '>' + i + '</button>';
        }
        html += '<button data-page="next"' + (page.pageNum >= page.pages ? ' disabled' : '') + '>下一页</button>';
        html += '<span>共 ' + page.total + ' 条 / ' + page.pages + ' 页</span>';
        pager.innerHTML = html;
    }

    /* ----------------------------- 区域分组与统计 ----------------------------- */

    function loadZones() {
        return request(API + '/group-by-zone').then(function (groups) {
            renderZones(groups);
            renderStats(groups);
            fillZoneOptions(groups);
            return groups;
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    function renderStats(groups) {
        var total = 0;
        var running = 0;
        var abnormal = 0;
        groups.forEach(function (group) {
            group.devices.forEach(function (d) {
                total++;
                if (d.status === 'RUNNING') {
                    running++;
                }
                if (d.status === 'FAULT' || d.status === 'OFFLINE') {
                    abnormal++;
                }
            });
        });
        $('statTotal').textContent = total;
        $('statRunning').textContent = running;
        $('statAbnormal').textContent = abnormal;
        $('statZone').textContent = groups.length;
    }

    function fillZoneOptions(groups) {
        var select = $('qZone');
        var current = select.value;
        var html = '<option value="">全部</option>';
        groups.forEach(function (group) {
            html += '<option value="' + escapeHtml(group.zone) + '">' + escapeHtml(group.zone) + '</option>';
        });
        select.innerHTML = html;
        select.value = current;
    }

    function renderZones(groups) {
        var container = $('zoneView');
        if (!groups || groups.length === 0) {
            container.innerHTML = '<p class="empty">暂无设备</p>';
            return;
        }
        container.innerHTML = '<div class="zone-grid">' + groups.map(function (group) {
            var items = group.devices.map(function (d) {
                return '<li data-action="detail" data-id="' + escapeHtml(d.deviceId) + '">'
                    + '<span class="dev-id">' + escapeHtml(d.deviceId) + '</span>'
                    + '<span class="dev-name">' + escapeHtml(d.deviceName) + '</span>'
                    + '<span class="badge badge-' + escapeHtml(d.status) + '">' + escapeHtml(dash(d.statusLabel)) + '</span>'
                    + '<span class="dev-sum">' + escapeHtml(dash(d.keyParameterSummary)) + '</span>'
                    + '</li>';
            }).join('');
            return '<div class="zone-card">'
                + '<div class="zone-head"><h4>' + escapeHtml(group.zone) + '</h4>'
                + '<span class="zone-count">' + group.deviceCount + ' 台设备</span></div>'
                + '<ul class="zone-list">' + items + '</ul>'
                + '</div>';
        }).join('') + '</div>';
    }

    /* ----------------------------- 新增 / 编辑 ----------------------------- */

    function renderParamFields(type) {
        var container = $('paramFields');
        var defs = PARAM_DEFS[type] || [];
        container.innerHTML = defs.map(function (def) {
            var input;
            if (def.type === 'select') {
                input = '<select id="p_' + def.key + '">' + def.options.map(function (opt) {
                    return '<option value="' + opt.value + '">' + opt.label + '</option>';
                }).join('') + '</select>';
            } else {
                input = '<input type="' + def.type + '" id="p_' + def.key + '"'
                    + (def.step ? ' step="' + def.step + '"' : '')
                    + (def.placeholder ? ' placeholder="' + def.placeholder + '"' : '') + '>';
            }
            return '<div class="field"><label for="p_' + def.key + '">' + def.label + '</label>' + input + '</div>';
        }).join('');
    }

    function resetForm() {
        $('fDeviceName').value = '';
        $('fManufacturer').value = '';
        $('fModel').value = '';
        $('fInstallDate').value = '';
        $('fZone').value = '';
        $('fDeviceType').disabled = false;
    }

    function openCreate() {
        state.editId = null;
        $('formTitle').textContent = '新增设备';
        $('paramBlock').classList.remove('hidden');
        resetForm();
        renderParamFields($('fDeviceType').value);
        $('formModal').classList.remove('hidden');
        $('fDeviceName').focus();
    }

    function openEdit(deviceId) {
        request(API + '/' + encodeURIComponent(deviceId)).then(function (detail) {
            state.editId = deviceId;
            $('formTitle').textContent = '编辑设备 ' + detail.deviceId;
            $('paramBlock').classList.add('hidden');
            resetForm();
            $('fDeviceType').value = detail.deviceType;
            $('fDeviceType').disabled = true;
            $('fDeviceName').value = detail.deviceName || '';
            $('fManufacturer').value = detail.manufacturer || '';
            $('fModel').value = detail.model || '';
            $('fInstallDate').value = detail.installDate || '';
            $('fZone').value = detail.zone || '';
            $('formModal').classList.remove('hidden');
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    function collectParams(type) {
        var defs = PARAM_DEFS[type] || [];
        var params = {};
        var invalid = null;
        defs.forEach(function (def) {
            var el = $('p_' + def.key);
            if (!el) {
                return;
            }
            var value = el.value.trim();
            if (value === '') {
                return;
            }
            if (def.type === 'number') {
                var num = Number(value);
                if (isNaN(num)) {
                    invalid = def.label;
                    return;
                }
                params[def.key] = num;
            } else {
                params[def.key] = value;
            }
        });
        if (invalid) {
            throw new Error('参数「' + invalid + '」必须为数值');
        }
        return params;
    }

    function save() {
        var payload = {
            deviceType: $('fDeviceType').value,
            deviceName: $('fDeviceName').value.trim(),
            manufacturer: $('fManufacturer').value.trim(),
            model: $('fModel').value.trim(),
            installDate: $('fInstallDate').value || null,
            zone: $('fZone').value.trim()
        };
        if (!payload.deviceName) {
            toast('请填写设备名称', 'error');
            return;
        }
        if (!payload.zone) {
            toast('请填写所属区域', 'error');
            return;
        }

        var isEdit = !!state.editId;
        var promise;
        if (isEdit) {
            promise = request(API + '/' + encodeURIComponent(state.editId), {
                method: 'PUT',
                body: JSON.stringify({
                    deviceName: payload.deviceName,
                    manufacturer: payload.manufacturer,
                    model: payload.model,
                    installDate: payload.installDate,
                    zone: payload.zone
                })
            });
        } else {
            try {
                payload.params = collectParams(payload.deviceType);
            } catch (e) {
                toast(e.message, 'error');
                return;
            }
            promise = request(API, {method: 'POST', body: JSON.stringify(payload)});
        }

        promise.then(function (deviceId) {
            $('formModal').classList.add('hidden');
            toast(isEdit ? '修改成功' : '新增成功，设备编号 ' + deviceId, 'success');
            refresh();
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    /* ----------------------------- 详情 / 删除 ----------------------------- */

    function openDetail(deviceId) {
        request(API + '/' + encodeURIComponent(deviceId)).then(function (d) {
            $('detailTitle').textContent = '设备详情 ' + d.deviceId;
            var info = [
                ['设备编号', d.deviceId],
                ['设备名称', d.deviceName],
                ['设备类型', d.deviceTypeLabel],
                ['所属区域', d.zone],
                ['运行状态', d.statusLabel],
                ['生产厂商', d.manufacturer],
                ['设备型号', d.model],
                ['安装日期', d.installDate],
                ['建档时间', d.createTime],
                ['更新时间', d.updateTime]
            ].map(function (pair) {
                return '<div class="info-item"><span class="k">' + pair[0] + '</span>'
                    + '<span class="v">' + escapeHtml(dash(pair[1])) + '</span></div>';
            }).join('');

            var specs = Object.keys(d.specs || {}).map(function (key) {
                return '<div class="info-item"><span class="k">' + escapeHtml(key) + '</span>'
                    + '<span class="v">' + escapeHtml(dash(d.specs[key])) + '</span></div>';
            }).join('');

            var sensorRows = (d.sensors || []).map(function (s) {
                return '<tr>'
                    + '<td>' + escapeHtml(s.name) + '</td>'
                    + '<td class="' + (s.outOfRange ? 'out-of-range' : '') + '">'
                    + s.value.toFixed(1) + ' ' + escapeHtml(s.unit) + '</td>'
                    + '<td>' + s.minValue + ' ~ ' + s.maxValue + ' ' + escapeHtml(s.unit) + '</td>'
                    + '<td>' + escapeHtml(dash(s.collectTime)) + '</td>'
                    + '</tr>';
            }).join('');

            $('detailBody').innerHTML =
                '<div class="detail-section"><h4>基本信息</h4><div class="info-grid">' + info + '</div></div>'
                + '<div class="detail-section"><h4>规格参数</h4><div class="info-grid">' + (specs || '<span class="v">-</span>') + '</div></div>'
                + '<div class="detail-section"><h4>运行参数</h4>'
                + '<table class="sensor-table"><thead><tr><th>参数</th><th>当前值</th><th>合理区间</th><th>采集时间</th></tr></thead>'
                + '<tbody>' + (sensorRows || '<tr><td colspan="4" class="empty">暂无采集数据</td></tr>') + '</tbody>'
                + '</table></div>';
            $('detailModal').classList.remove('hidden');
        }).catch(function (err) {
            toast(err.message, 'error');
        });
    }

    function removeDevice(deviceId) {
        if (!window.confirm('确认删除设备 ' + deviceId + ' 吗？\n删除后该设备的采集数据将一并清理，操作不可恢复。')) {
            return;
        }
        request(API + '/' + encodeURIComponent(deviceId), {method: 'DELETE'})
            .then(function () {
                toast('删除成功', 'success');
                refresh();
            })
            .catch(function (err) {
                toast(err.message, 'error');
            });
    }

    /* ----------------------------- 视图与事件 ----------------------------- */

    function refresh() {
        loadZones();
        loadList();
    }

    function switchView(view) {
        state.view = view;
        var segs = document.querySelectorAll('.seg');
        Array.prototype.forEach.call(segs, function (seg) {
            seg.classList.toggle('active', seg.getAttribute('data-view') === view);
        });
        $('listView').classList.toggle('hidden', view !== 'list');
        $('zoneView').classList.toggle('hidden', view !== 'zone');
    }

    function bindEvents() {
        $('btnSearch').addEventListener('click', function () {
            state.pageNum = 1;
            loadList();
        });
        $('btnReset').addEventListener('click', function () {
            $('qKeyword').value = '';
            $('qType').value = '';
            $('qZone').value = '';
            $('qStatus').value = '';
            state.pageNum = 1;
            loadList();
        });
        $('qKeyword').addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                state.pageNum = 1;
                loadList();
            }
        });
        $('btnCreate').addEventListener('click', openCreate);
        $('fDeviceType').addEventListener('change', function () {
            renderParamFields(this.value);
        });
        $('btnSave').addEventListener('click', save);

        // 表格与分组中的操作按钮（事件委托）
        document.addEventListener('click', function (e) {
            var target = e.target.closest('[data-action]');
            if (!target) {
                return;
            }
            var action = target.getAttribute('data-action');
            var id = target.getAttribute('data-id');
            if (action === 'detail') {
                openDetail(id);
            } else if (action === 'edit') {
                openEdit(id);
            } else if (action === 'delete') {
                removeDevice(id);
            }
        });

        // 分页
        $('pager').addEventListener('click', function (e) {
            var btn = e.target.closest('button[data-page]');
            if (!btn || btn.disabled) {
                return;
            }
            var page = btn.getAttribute('data-page');
            if (page === 'prev') {
                state.pageNum = Math.max(1, state.pageNum - 1);
            } else if (page === 'next') {
                state.pageNum = state.pageNum + 1;
            } else {
                state.pageNum = parseInt(page, 10);
            }
            loadList();
        });

        // 视图切换
        document.querySelectorAll('.seg').forEach(function (seg) {
            seg.addEventListener('click', function () {
                switchView(seg.getAttribute('data-view'));
            });
        });

        // 弹窗关闭
        document.querySelectorAll('[data-close]').forEach(function (el) {
            el.addEventListener('click', function () {
                $(el.getAttribute('data-close')).classList.add('hidden');
            });
        });
        document.querySelectorAll('.modal').forEach(function (modal) {
            modal.addEventListener('click', function (e) {
                if (e.target === modal) {
                    modal.classList.add('hidden');
                }
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        bindEvents();
        refresh();
    });
})();
