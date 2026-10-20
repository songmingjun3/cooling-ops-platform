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

    /**
     * 刷新左侧导航上的未处理告警计数。
     * 属于装饰性提示，请求失败时保持原样，不打扰用户。
     */
    function refreshAlarmBadge() {
        return request('/api/statistics/overview').then(function (stat) {
            var badge = $('navAlarmBadge');
            if (!badge) {
                return null;
            }
            var count = stat.unhandledAlarms || 0;
            badge.textContent = count > 99 ? '99+' : String(count);
            badge.classList.toggle('hidden', count === 0);
            return null;
        }).catch(function () {
            return null;
        });
    }

    global.App = {
        $: $,
        escapeHtml: escapeHtml,
        dash: dash,
        toast: toast,
        request: request,
        refreshAlarmBadge: refreshAlarmBadge,
        SENSOR_OPTIONS: SENSOR_OPTIONS,
        TYPE_LABEL: TYPE_LABEL
    };

    /* ---------------- 页签切换 ---------------- */

    /** 记住上次浏览的页签，刷新后回到原位置 */
    var TAB_STORAGE_KEY = 'cooling.activeTab';
    var TAB_NAMES = ['device', 'monitor', 'alarm', 'analysis'];

    /**
     * 从地址栏读取页签，支持把某个页面作为链接直接分享。
     *
     * @return {string|null} 合法页签名，否则 null
     */
    function tabFromUrl() {
        var match = /[?&]tab=([a-z]+)/.exec(window.location.search);
        var name = match ? match[1] : null;
        return TAB_NAMES.indexOf(name) >= 0 ? name : null;
    }

    /**
     * 激活指定页签：切换高亮、显隐面板、同步顶部标题、广播 tab:shown。
     *
     * @param {HTMLElement} tab 目标页签按钮
     */
    function activateTab(tab) {
        var tabs = document.querySelectorAll('.tab');
        Array.prototype.forEach.call(tabs, function (item) {
            item.classList.toggle('active', item === tab);
        });

        var name = tab.getAttribute('data-tab');
        Array.prototype.forEach.call(document.querySelectorAll('.tab-panel'), function (panel) {
            panel.classList.toggle('hidden', panel.id !== 'tab-' + name);
        });

        var title = $('pageTitle');
        var sub = $('pageSub');
        if (title && tab.getAttribute('data-title')) {
            title.textContent = tab.getAttribute('data-title');
        }
        if (sub && tab.getAttribute('data-sub')) {
            sub.textContent = tab.getAttribute('data-sub');
        }

        try {
            window.localStorage.setItem(TAB_STORAGE_KEY, name);
        } catch (err) {
            // 隐私模式下 localStorage 不可写，忽略即可
        }

        document.dispatchEvent(new CustomEvent('tab:shown', {detail: name}));
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    document.addEventListener('DOMContentLoaded', function () {
        var tabs = document.querySelectorAll('.tab');
        Array.prototype.forEach.call(tabs, function (tab) {
            tab.addEventListener('click', function () {
                activateTab(tab);
            });
        });

        /*
         * 恢复上次页签放在下一个宏任务执行：
         * 各模块是在自己的 DOMContentLoaded 回调里注册 tab:shown 监听的，
         * 若此处同步广播，事件会无人接收，页面数据就不会加载。
         */
        setTimeout(function () {
            var saved = tabFromUrl();
            if (!saved) {
                try {
                    saved = window.localStorage.getItem(TAB_STORAGE_KEY);
                } catch (err) {
                    saved = null;
                }
            }
            if (TAB_NAMES.indexOf(saved) < 0) {
                saved = null;
            }
            var target = saved ? document.querySelector('.tab[data-tab="' + saved + '"]') : null;
            if (target) {
                activateTab(target);
            }
            refreshAlarmBadge();
        }, 0);
    });
})(window);
