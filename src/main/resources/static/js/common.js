/* =====================================================================
   公共工具：请求封装、提示、确认弹窗、转义、传感器参数表
   供 dashboard.js / device.js / monitor.js / alarm.js / analysis.js 共用
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

    /** 补零，用于时间显示 */
    function pad2(num) {
        return num < 10 ? '0' + num : String(num);
    }

    /** 当前时间的 HH:mm:ss，用于页头「数据更新于」提示 */
    function timeNow() {
        var now = new Date();
        return pad2(now.getHours()) + ':' + pad2(now.getMinutes()) + ':' + pad2(now.getSeconds());
    }

    /**
     * 在页头标记数据最近一次刷新的时刻。
     * 页头不再展示设备概况数字，只保留这个全局状态提示。
     */
    function markUpdated() {
        var el = $('dataUpdated');
        if (el) {
            el.textContent = '数据更新于 ' + timeNow();
        }
    }

    /*
     * 轻提示：保存定时器句柄，连续调用时先清掉上一个。
     * 否则前一条的定时器会把后一条提前隐藏，看起来像提示闪一下就没了。
     */
    var toastTimer = null;

    function toast(message, type) {
        var el = $('toast');
        if (!el) {
            return;
        }
        el.textContent = message;
        el.className = 'toast ' + (type || '');
        if (toastTimer) {
            clearTimeout(toastTimer);
        }
        toastTimer = setTimeout(function () {
            el.className = 'toast hidden';
            toastTimer = null;
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

    /**
     * 二次确认弹窗，返回 Promise<boolean>。
     *
     * <p>替代原生 window.confirm：原生弹窗样式无法与页面统一，
     * 且会阻塞渲染线程，自动化测试也难以稳定驱动。</p>
     *
     * @param {string} message 提示正文
     * @param {Object} [options] 可选配置：title / okText / okStyle
     * @return {Promise<boolean>} 点击「确定」为 true
     */
    function confirmDialog(message, options) {
        var config = options || {};
        return new Promise(function (resolve) {
            var modal = $('confirmModal');
            var okBtn = $('confirmOk');
            var cancelBtn = $('confirmCancel');
            var closeBtn = $('confirmClose');
            if (!modal || !okBtn || !cancelBtn || !closeBtn) {
                // 兜底：页面缺失弹窗节点时退回原生确认
                resolve(global.confirm(message));
                return;
            }

            $('confirmText').textContent = message;
            $('confirmTitle').textContent = config.title || '操作确认';
            okBtn.textContent = config.okText || '确定';
            okBtn.className = 'btn ' + (config.okStyle || 'btn-danger');
            modal.classList.remove('hidden');
            okBtn.focus();

            function finish(result) {
                modal.classList.add('hidden');
                okBtn.removeEventListener('click', onOk);
                cancelBtn.removeEventListener('click', onCancel);
                closeBtn.removeEventListener('click', onCancel);
                document.removeEventListener('keydown', onKey);
                resolve(result);
            }

            function onOk() {
                finish(true);
            }

            function onCancel() {
                finish(false);
            }

            function onKey(e) {
                if (e.key === 'Escape') {
                    finish(false);
                } else if (e.key === 'Enter') {
                    finish(true);
                }
            }

            okBtn.addEventListener('click', onOk);
            cancelBtn.addEventListener('click', onCancel);
            closeBtn.addEventListener('click', onCancel);
            document.addEventListener('keydown', onKey);
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

    var STATUS_LABEL = {
        RUNNING: '运行中',
        STOPPED: '已停机',
        FAULT: '故障',
        MAINTENANCE: '维护中',
        OFFLINE: '离线'
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

    /**
     * 数据发生变更后的统一广播：刷新角标、页头时间，并通知各模块自行刷新。
     * 新增/删除设备、启停、采集、处理告警后都应调用，避免各页面数据不同步。
     */
    function notifyChange() {
        markUpdated();
        refreshAlarmBadge();
        document.dispatchEvent(new CustomEvent('data:changed'));
    }

    global.App = {
        $: $,
        escapeHtml: escapeHtml,
        dash: dash,
        timeNow: timeNow,
        markUpdated: markUpdated,
        toast: toast,
        confirm: confirmDialog,
        request: request,
        refreshAlarmBadge: refreshAlarmBadge,
        notifyChange: notifyChange,
        SENSOR_OPTIONS: SENSOR_OPTIONS,
        TYPE_LABEL: TYPE_LABEL,
        STATUS_LABEL: STATUS_LABEL
    };

    /* ---------------- 页签切换 ---------------- */

    /** 记住上次浏览的页签，刷新后回到原位置 */
    var TAB_STORAGE_KEY = 'cooling.activeTab';
    var TAB_NAMES = ['dashboard', 'device', 'monitor', 'alarm', 'analysis'];

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
        if (!tab) {
            return;
        }
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

    /**
     * 切换到指定页签，供总览页的指标卡、快捷链接调用。
     *
     * @param {string} name 页签名
     */
    function showTab(name) {
        activateTab(document.querySelector('.tab[data-tab="' + name + '"]'));
    }

    /* ---------------- 全局弹窗行为 ---------------- */

    /**
     * 弹窗关闭：遮罩点击与带 [data-close] 的按钮统一定义在公共层，
     * 各业务模块只管打开自己的弹窗。
     */
    function bindModals() {
        Array.prototype.forEach.call(document.querySelectorAll('[data-close]'), function (el) {
            el.addEventListener('click', function () {
                var id = el.getAttribute('data-close');
                if (id === 'confirmModal') {
                    return; // 确认弹窗由 confirmDialog 自行收尾，需先决议 Promise
                }
                var modal = $(id);
                if (modal) {
                    modal.classList.add('hidden');
                }
            });
        });
        Array.prototype.forEach.call(document.querySelectorAll('.modal'), function (modal) {
            modal.addEventListener('click', function (e) {
                if (e.target === modal && modal.id !== 'confirmModal') {
                    modal.classList.add('hidden');
                }
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var tabs = document.querySelectorAll('.tab');
        Array.prototype.forEach.call(tabs, function (tab) {
            tab.addEventListener('click', function () {
                activateTab(tab);
            });
        });

        bindModals();

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
            } else {
                // 没有历史记录时停留在默认的总览页，并把它的数据拉起来
                document.dispatchEvent(new CustomEvent('tab:shown', {detail: 'dashboard'}));
            }
            refreshAlarmBadge();
        }, 0);
    });

    global.App.showTab = showTab;
})(window);
