/* =====================================================================
   公共工具：请求封装、提示、转义、传感器参数表
   供 device.js / monitor.js / alarm.js / analysis.js 共用
   ===================================================================== */
(function (global) {
    'use strict';

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
        if (!el) {
            return;
        }
        el.textContent = message;
        el.className = 'toast ' + (type || '');
        setTimeout(function () {
            el.className = 'toast hidden';
        }, 2600);
    }

    /**
     * 统一请求封装：解析 Result 结构，code 非 0 时抛出业务提示。
     * 后端异常响应同时设置 HTTP 状态码，因此这里先尝试解析响应体，
     * 解析得到 message 就优先展示中文提示，避免只看到 HTTP 代号。
     */
    function request(url, options) {
        var opts = options || {};
        opts.headers = Object.assign({'Content-Type': 'application/json'}, opts.headers || {});
        return fetch(url, opts).then(function (resp) {
            return resp.json().catch(function () {
                return null;
            }).then(function (body) {
                if (body && body.code === 0) {
                    return body.data;
                }
                if (body && body.message) {
                    throw new Error(body.message);
                }
                throw new Error('请求失败：HTTP ' + resp.status);
            });
        });
    }

    /** 各设备类型参与采集的传感器参数（与后端 SensorCatalog 保持一致） */
    var SENSOR_OPTIONS = {
        CHILLER: [
            {key: 'chilledWaterSupplyTemp', label: '冷冻水供水温度'},
            {key: 'chilledWaterReturnTemp', label: '冷冻水回水温度'}
        ],
        COOLING_TOWER: [
            {key: 'waterFlowRate', label: '水流量'},
            {key: 'ambientTemp', label: '环境温度'},
            {key: 'outletTemp', label: '出水温度'}
        ],
        PUMP: [
            {key: 'flowRate', label: '当前流量'}
        ],
        COOLING_UNIT: [
            {key: 'supplyTemp', label: '送风温度'},
            {key: 'returnTemp', label: '回风温度'}
        ]
    };

    var TYPE_LABEL = {
        CHILLER: '冷水机组',
        COOLING_TOWER: '冷却塔',
        PUMP: '水泵',
        COOLING_UNIT: '精密空调'
    };

    global.App = {
        $: $,
        escapeHtml: escapeHtml,
        dash: dash,
        toast: toast,
        request: request,
        SENSOR_OPTIONS: SENSOR_OPTIONS,
        TYPE_LABEL: TYPE_LABEL
    };

    /* 页签切换：切换后广播 tab:shown 事件，各模块按需刷新自己的数据 */
    document.addEventListener('DOMContentLoaded', function () {
        var tabs = document.querySelectorAll('.tab');
        Array.prototype.forEach.call(tabs, function (tab) {
            tab.addEventListener('click', function () {
                Array.prototype.forEach.call(tabs, function (item) {
                    item.classList.toggle('active', item === tab);
                });
                var name = tab.getAttribute('data-tab');
                Array.prototype.forEach.call(document.querySelectorAll('.tab-panel'), function (panel) {
                    panel.classList.toggle('hidden', panel.id !== 'tab-' + name);
                });
                document.dispatchEvent(new CustomEvent('tab:shown', {detail: name}));
            });
        });
    });
})(window);
