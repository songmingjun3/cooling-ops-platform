#!/usr/bin/env node
/*
 * =====================================================================
 *  端到端界面测试（真实浏览器驱动）
 * ---------------------------------------------------------------------
 *  与 scripts/e2e_verify.py 的区别：
 *    - e2e_verify.py 直接打接口，验证「后端契约」；
 *    - 本脚本驱动真实浏览器，点按钮、填表单、读 DOM，验证「用户看到的界面」。
 *
 *  实现方式：Node 内置 WebSocket 直连 Chromium 调试协议（CDP），
 *  不依赖 Playwright / Puppeteer，也不需要 npm install。
 *
 *  用法：
 *    node scripts/e2e-ui.js [--url http://127.0.0.1:18082]
 *                           [--browser "C:\...\msedge.exe"]
 *                           [--port 9336] [--verbose]
 *
 *  被测实例建议单独起一个，避免污染开发数据：
 *    java -jar target/cooling-ops-platform.jar \
 *         --server.port=18082 \
 *         --spring.datasource.url=jdbc:sqlite:./target/e2e-data/e2e-ui.db \
 *         --cooling.acquisition.enabled=false
 *
 *  退出码：0 = 全部通过；1 = 存在失败项或运行异常。
 * =====================================================================
 */
'use strict';

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

/* ------------------------------ 参数与环境 ------------------------------ */

function parseArgs(argv) {
    const out = {};
    for (let i = 0; i < argv.length; i++) {
        const key = argv[i];
        if (!key.startsWith('--')) {
            continue;
        }
        const name = key.slice(2);
        const next = argv[i + 1];
        if (!next || next.startsWith('--')) {
            out[name] = true;
        } else {
            out[name] = next;
            i++;
        }
    }
    return out;
}

const ARGS = parseArgs(process.argv.slice(2));
const BASE = ARGS.url || 'http://127.0.0.1:18082';
const DEBUG_PORT = Number(ARGS.port || 9336);
const VIEW = {width: 1500, height: 1050};
const ARTIFACT_DIR = path.resolve(__dirname, '..', 'target', 'e2e-ui');
const EDGE_CANDIDATES = [
    ARGS.browser,
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'
].filter(Boolean);

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/* ------------------------------ 断言结果 ------------------------------ */

const RESULTS = [];
let sectionName = '';

function section(title) {
    sectionName = title;
    console.log('\n=== ' + title + ' ===');
}

function record(name, ok, detail) {
    RESULTS.push({section: sectionName, name, ok, detail: detail === undefined ? '' : String(detail)});
    const tag = ok ? '[PASS]' : '[FAIL]';
    console.log('  ' + tag + ' ' + name + (detail === undefined || detail === '' ? '' : '  → ' + detail));
    return ok;
}

/* ------------------------------ CDP 客户端 ------------------------------ */

class Cdp {
    constructor(ws) {
        this.ws = ws;
        this.seq = 0;
        this.pending = new Map();
        this.handlers = [];
        ws.addEventListener('message', (event) => {
            const msg = JSON.parse(event.data);
            if (msg.id && this.pending.has(msg.id)) {
                const {resolve, reject} = this.pending.get(msg.id);
                this.pending.delete(msg.id);
                if (msg.error) {
                    reject(new Error(msg.error.message + ' / ' + msg.method));
                } else {
                    resolve(msg.result);
                }
                return;
            }
            if (msg.method) {
                this.handlers.forEach((fn) => fn(msg));
            }
        });
    }

    static async connect(url) {
        const ws = new WebSocket(url);
        await new Promise((resolve, reject) => {
            ws.addEventListener('open', resolve, {once: true});
            ws.addEventListener('error', reject, {once: true});
        });
        return new Cdp(ws);
    }

    send(method, params) {
        const id = ++this.seq;
        return new Promise((resolve, reject) => {
            this.pending.set(id, {resolve, reject});
            this.ws.send(JSON.stringify({id, method, params: params || {}}));
        });
    }

    on(fn) {
        this.handlers.push(fn);
    }

    close() {
        try {
            this.ws.close();
        } catch (e) {
            /* 忽略关闭异常 */
        }
    }
}

/* ------------------------------ 浏览器启动 ------------------------------ */

function resolveBrowser() {
    for (const candidate of EDGE_CANDIDATES) {
        if (fs.existsSync(candidate)) {
            return candidate;
        }
    }
    throw new Error('未找到 Edge / Chrome 可执行文件，请用 --browser 指定路径');
}

async function launchBrowser() {
    const browser = resolveBrowser();
    const profile = path.join(ARTIFACT_DIR, 'profile');
    fs.rmSync(profile, {recursive: true, force: true});
    fs.mkdirSync(profile, {recursive: true});

    const child = spawn(browser, [
        '--headless=new',
        '--disable-gpu',
        '--disable-extensions',
        '--no-first-run',
        '--no-default-browser-check',
        '--hide-scrollbars',
        `--remote-debugging-port=${DEBUG_PORT}`,
        `--user-data-dir=${profile}`,
        `--window-size=${VIEW.width},${VIEW.height}`,
        'about:blank'
    ], {stdio: 'ignore'});

    for (let i = 0; i < 60; i++) {
        await sleep(250);
        try {
            const list = await (await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/list`)).json();
            const page = list.find((t) => t.type === 'page');
            if (page) {
                return {child, wsUrl: page.webSocketDebuggerUrl};
            }
        } catch (e) {
            /* 调试端口尚未就绪，继续等待 */
        }
    }
    child.kill();
    throw new Error('无法连接浏览器调试端口 ' + DEBUG_PORT);
}

/* ------------------------------ 页面工具注入 ------------------------------ */

const PAGE_TOOLKIT = `
window.__t = {
    q: function (s) { return document.querySelector(s); },
    qa: function (s) { return Array.prototype.slice.call(document.querySelectorAll(s)); },
    click: function (s) {
        var el = document.querySelector(s);
        if (!el) { return false; }
        el.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));
        return true;
    },
    clickText: function (selector, keyword) {
        var el = Array.prototype.slice.call(document.querySelectorAll(selector))
            .filter(function (n) { return (n.textContent || '').indexOf(keyword) >= 0; })[0];
        if (!el) { return false; }
        el.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));
        return true;
    },
    setVal: function (s, v) {
        var el = document.querySelector(s);
        if (!el) { return false; }
        el.value = v;
        el.dispatchEvent(new Event('input', {bubbles: true}));
        el.dispatchEvent(new Event('change', {bubbles: true}));
        return true;
    },
    text: function (s) { var el = document.querySelector(s); return el ? el.textContent.trim() : null; },
    val: function (s) { var el = document.querySelector(s); return el ? el.value : null; },
    count: function (s) { return document.querySelectorAll(s).length; },
    visible: function (s) {
        var el = document.querySelector(s);
        if (!el) { return false; }
        if (el.classList.contains('hidden')) { return false; }
        var r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0;
    },
    hidden: function (s) {
        var el = document.querySelector(s);
        return !el || el.classList.contains('hidden');
    },
    toast: function () {
        var el = document.getElementById('toast');
        return el && !el.classList.contains('hidden') ? el.textContent.trim() : '';
    },
    hasText: function (s, keyword) {
        var el = document.querySelector(s);
        return !!el && (el.textContent || '').indexOf(keyword) >= 0;
    }
};
true;
`;

/* ------------------------------ 主流程 ------------------------------ */

async function main() {
    fs.mkdirSync(ARTIFACT_DIR, {recursive: true});

    // 先确认被测实例可用，避免把「服务没起」误报成一堆界面缺陷
    try {
        const probe = await fetch(BASE + '/api/statistics/overview');
        if (!probe.ok) {
            throw new Error('HTTP ' + probe.status);
        }
    } catch (e) {
        console.error('无法访问被测服务 ' + BASE + '：' + e.message);
        console.error('请先启动一个独立实例（端口与库文件都独立，避免污染开发数据）：');
        console.error('  java -jar target/cooling-ops-platform.jar --server.port=18082 \\');
        console.error('       --spring.datasource.url=jdbc:sqlite:./target/e2e-data/e2e-ui.db \\');
        console.error('       --cooling.acquisition.enabled=false');
        process.exit(1);
    }

    const {child, wsUrl} = await launchBrowser();
    const cdp = await Cdp.connect(wsUrl);
    const problems = {exceptions: [], consoleErrors: [], dialogs: []};

    cdp.on((msg) => {
        if (msg.method === 'Runtime.exceptionThrown') {
            const d = msg.params.exceptionDetails;
            problems.exceptions.push(d.text + ' ' + ((d.exception || {}).description || ''));
        } else if (msg.method === 'Runtime.consoleAPICalled' && msg.params.type === 'error') {
            problems.consoleErrors.push(msg.params.args.map((a) => a.value || a.description || '').join(' '));
        } else if (msg.method === 'Page.javascriptDialogOpening') {
            // 页面不应再弹原生对话框，出现即视为缺陷并自动关掉以免阻塞
            problems.dialogs.push(msg.params.type + ': ' + msg.params.message);
            cdp.send('Page.handleJavaScriptDialog', {accept: true}).catch(() => {});
        }
    });

    let currentCheck = '启动';
    const js = async (expression) => {
        const res = await cdp.send('Runtime.evaluate', {
            expression,
            awaitPromise: true,
            returnByValue: true
        });
        if (res.exceptionDetails) {
            const d = res.exceptionDetails;
            throw new Error('页面脚本异常：' + d.text + ' ' +
                ((d.exception || {}).description || '') + '\n表达式：' + expression.slice(0, 200));
        }
        return res.result.value;
    };

    const waitFor = async (expression, timeout = 6000, interval = 120) => {
        const deadline = Date.now() + timeout;
        let last = null;
        while (Date.now() < deadline) {
            last = await js(expression);
            if (last) {
                return last;
            }
            await sleep(interval);
        }
        return last;
    };

    const shot = async (name) => {
        try {
            const data = await cdp.send('Page.captureScreenshot', {format: 'png'});
            const file = path.join(ARTIFACT_DIR, name + '.png');
            fs.writeFileSync(file, Buffer.from(data.data, 'base64'));
            return file;
        } catch (e) {
            return '';
        }
    };

    /** 切到某个页签并等它的面板真正显示出来 */
    const openTab = async (name) => {
        await js(`__t.click('.tab[data-tab="${name}"]')`);
        await waitFor(`__t.visible('#tab-${name}')`);
        await sleep(150);
    };

    try {
        await cdp.send('Page.enable');
        await cdp.send('Runtime.enable');
        await cdp.send('Network.enable');
        await cdp.send('Network.setCacheDisabled', {cacheDisabled: true});
        await cdp.send('Emulation.setDeviceMetricsOverride', {
            width: VIEW.width, height: VIEW.height, deviceScaleFactor: 1, mobile: false
        });

        await cdp.send('Page.navigate', {url: BASE + '/'});
        await waitFor('document.readyState === "complete"', 15000);
        await js(PAGE_TOOLKIT);
        await waitFor('__t.count("#kpiRow .kpi") === 6', 8000);

        // 基线数据：整库设备数，作为后续增删的对照
        const baseline = await (await fetch(BASE + '/api/devices?pageSize=1')).json();
        const baseTotal = baseline.data.total;

        /* ==================== A. 启动与导航 ==================== */
        section('A 启动与导航');

        const landing = await js(`(function () {
            return {
                dashVisible: __t.visible('#tab-dashboard'),
                deviceHidden: __t.hidden('#tab-device'),
                activeTab: (__t.q('.tab.active') || {}).getAttribute
                    ? __t.q('.tab.active').getAttribute('data-tab') : null,
                dashClass: __t.q('#tab-dashboard').className,
                devClass: __t.q('#tab-device').className
            };
        })()`);
        record('A1 默认首页为运行总览',
            landing.dashVisible === true && landing.deviceHidden === true,
            JSON.stringify(landing));

        record('A2 页头标题与总览一致',
            (await js('__t.text("#pageTitle")')) === '运行总览',
            await js('__t.text("#pageTitle")'));

        record('A3 侧栏五个功能入口齐全',
            (await js('__t.count(".tabs .tab")')) === 5,
            await js('__t.count(".tabs .tab")') + ' 个');

        record('A4 功能页不再重复展示设备概况',
            (await js('__t.count("#stats") + __t.count(".stat")')) === 0,
            '页头统计卡已移除，仅保留更新时间');

        const stampOk = await waitFor('/数据更新于 \\d{2}:\\d{2}:\\d{2}/.test(__t.text("#dataUpdated") || "")', 6000);
        record('A5 页头显示数据更新时间', stampOk === true, await js('__t.text("#dataUpdated")'));

        let tabOk = true;
        const tabDetail = [];
        for (const name of ['device', 'monitor', 'alarm', 'analysis', 'dashboard']) {
            await openTab(name);
            const onlyOne = await js(
                `__t.qa('.tab-panel').filter(function (p) { return !p.classList.contains('hidden'); }).length === 1`
                + ` && __t.visible('#tab-${name}')`
                + ` && __t.q('.tab[data-tab="${name}"]').classList.contains('active')`);
            if (!onlyOne) {
                tabOk = false;
                tabDetail.push(name);
            }
        }
        record('A6 五个页签切换后仅显示对应面板', tabOk, tabDetail.length ? '异常：' + tabDetail.join(',') : '逐页校验通过');

        /* ==================== B. 运行总览大屏 ==================== */
        section('B 运行总览大屏');
        await openTab('dashboard');
        await waitFor('__t.count("#topology .tnode") > 0', 8000);

        const kpiValues = await js(
            '["kpiTotal","kpiRunning","kpiAbnormal","kpiMaintenance","kpiAlarm","kpiZone"]'
            + '.map(function (id) { return __t.text("#" + id); })');
        record('B1 六项 KPI 均有数值',
            kpiValues.every((v) => v && v !== '-'), kpiValues.join(' / '));

        record('B2 KPI 设备总数与接口一致',
            Number(kpiValues[0]) === baseTotal, kpiValues[0] + ' vs ' + baseTotal);

        record('B3 拓扑节点数等于设备总数',
            (await js('__t.count("#topology .tnode")')) === baseTotal,
            await js('__t.count("#topology .tnode")') + ' 个节点');

        const colTitles = await js('__t.qa("#topology .topo-col-title").map(function (t) { return t.textContent; })');
        record('B4 拓扑五列按制冷流程排列',
            JSON.stringify(colTitles) === JSON.stringify(['冷却塔', '冷却水泵', '冷水机组', '冷冻水泵', '精密空调']),
            colTitles.join(' → '));

        record('B5 供水管路 4 段、回水管路 2 条',
            (await js('__t.count(".topo-pipe")')) === 4 && (await js('__t.count(".topo-return")')) === 2,
            await js('__t.count(".topo-pipe")') + ' 段供水 / ' + await js('__t.count(".topo-return")') + ' 条回水');

        record('B6 运行中的回路有流动动画',
            (await js('__t.count(".topo-pipe-flow")')) > 0,
            await js('__t.count(".topo-pipe-flow")') + ' 段带动画');

        // 矩阵每行各状态之和必须等于该行总数，防止统计口径写错
        const matrixOk = await js(`(function () {
            var rows = __t.qa('#typeMatrix .matrix tbody tr');
            return rows.every(function (tr) {
                var cells = Array.prototype.slice.call(tr.children).map(function (td) {
                    return Number(td.textContent);
                });
                if (cells.length < 7) { return false; }
                var sum = cells.slice(2).reduce(function (a, b) { return a + b; }, 0);
                return sum === cells[1];
            });
        })()`);
        record('B7 设备概况矩阵每行状态之和等于总数', matrixOk === true, matrixOk);

        const matrixSum = await js(`(function () {
            var tr = __t.q('#typeMatrix .matrix tr.m-sum');
            return tr ? Number(tr.children[1].textContent) : -1;
        })()`);
        record('B8 矩阵合计与 KPI 总数一致', matrixSum === baseTotal, matrixSum + ' vs ' + baseTotal);

        record('B9 区域分布条数与覆盖区域一致',
            (await js('__t.count("#zoneSummary .zone-sum-item")')) === Number(kpiValues[5]),
            await js('__t.count("#zoneSummary .zone-sum-item")') + ' 个区域');

        const alarmCount = await js('__t.count("#recentAlarms .alarm-mini-item")');
        record('B10 最近告警不超过 5 条', alarmCount <= 5, alarmCount + ' 条');

        // 点击拓扑节点 → 设备详情
        const nodeId = await js('__t.q("#topology .tnode").getAttribute("data-device")');
        await js('__t.click("#topology .tnode")');
        const detailShown = await waitFor('__t.visible("#detailModal")', 5000);
        record('B11 点击拓扑节点打开设备详情',
            detailShown === true && (await js('__t.text("#detailTitle")')).indexOf(nodeId) >= 0,
            nodeId + ' → ' + await js('__t.text("#detailTitle")'));
        await js('__t.click("#detailModal [data-close]")');
        await waitFor('__t.hidden("#detailModal")');

        // KPI 点击 → 跳转并带筛选
        await js('__t.click("#kpiRow .kpi[data-kpi=\\"running\\"]")');
        await waitFor('__t.visible("#tab-device")');
        const runningFilter = await waitFor('__t.val("#qStatus") === "RUNNING"');
        await sleep(600);
        const onlyRunning = await js(`(function () {
            var rows = __t.qa('#deviceTbody tr');
            if (!rows.length) { return false; }
            return rows.every(function (tr) {
                var badge = tr.querySelector('.badge');
                return badge && badge.textContent.indexOf('运行中') >= 0;
            });
        })()`);
        record('B12 KPI「运行中」跳转设备页并筛选运行中',
            runningFilter === true && onlyRunning === true,
            '状态筛选=' + await js('__t.val("#qStatus")') + '，全部行均为运行中');

        await openTab('dashboard');
        await js('__t.click("#kpiRow .kpi[data-kpi=\\"alarm\\"]")');
        await waitFor('__t.visible("#tab-alarm")');
        record('B13 KPI「未处理告警」跳转告警页并筛选未处理',
            (await waitFor('__t.val("#qAlarmStatus") === "UNHANDLED"')) === true,
            '处理状态筛选=' + await js('__t.val("#qAlarmStatus")'));

        /* ==================== C. 设备管理 ==================== */
        section('C 设备管理 FM-01~06');
        await openTab('device');
        await js('__t.click("#btnReset")');
        // 表格在请求期间会先渲染「加载中…」占位行，必须等占位消失再计数
        await waitFor('__t.count("#deviceTbody tr") > 0 && !__t.hasText("#deviceTbody", "加载中")', 8000);

        record('C1 列表首屏按分页大小展示',
            (await js('__t.count("#deviceTbody tr")')) === Math.min(10, baseTotal),
            await js('__t.count("#deviceTbody tr")') + ' 行');

        const hasPage2 = await js('__t.count("#pager button[data-page=\\"2\\"]") > 0');
        if (hasPage2) {
            await js('__t.click("#pager button[data-page=\\"2\\"]")');
            await sleep(700);
            record('C2 翻到第 2 页后行数正确',
                (await js('__t.count("#deviceTbody tr")')) === baseTotal - 10,
                await js('__t.count("#deviceTbody tr")') + ' 行');
        } else {
            record('C2 翻到第 2 页后行数正确', true, '设备不足两页，跳过');
        }

        await js('__t.click("#btnReset")');
        await sleep(600);
        await js('__t.setVal("#qKeyword", "CH-001")');
        await js('__t.click("#btnSearch")');
        await sleep(700);
        record('C3 关键字查询命中唯一设备',
            (await js('__t.count("#deviceTbody tr")')) === 1
            && (await js('__t.hasText("#deviceTbody", "CH-001")')) === true,
            await js('__t.text("#listTitle")'));

        await js('__t.setVal("#qKeyword", "")');
        await js('__t.setVal("#qType", "PUMP")');
        await js('__t.click("#btnSearch")');
        await sleep(700);
        record('C4 按设备类型筛选',
            (await js('__t.hasText("#deviceTbody", "水泵")')) === true
            && (await js('__t.hasText("#deviceTbody", "冷水机组")')) === false,
            await js('__t.text("#listTitle")'));

        await js('__t.click("#btnReset")');
        await sleep(700);
        record('C5 重置恢复全量列表',
            (await js('__t.text("#listTitle")')).indexOf('共 ' + baseTotal + ' 台') >= 0,
            await js('__t.text("#listTitle")'));

        await js('__t.click("#tab-device .seg[data-view=\\"zone\\"]")');
        await sleep(400);
        const zoneCards = await js('__t.count("#zoneView .zone-card")');
        record('C6 按区域分组视图',
            zoneCards > 0 && (await js('__t.visible("#zoneView")')) === true,
            zoneCards + ' 个区域卡片');
        await js('__t.click("#tab-device .seg[data-view=\\"list\\"]")');
        await sleep(300);

        // 回归用例：曾经点击监控页的类型分组会把设备列表一起隐藏
        await openTab('monitor');
        await waitFor('__t.count("#monitorCards .mon-card") + __t.count("#monitorCards .empty") > 0');
        await js('__t.click("#monitorTypes .seg[data-mtype=\\"COOLING_TOWER\\"]")');
        await sleep(900);
        const segActiveAfterMonitor = await js('__t.count(".seg.active")');
        await openTab('device');
        const deviceListIntact = await js('__t.visible("#listView")');
        const deviceRows = await js('__t.count("#deviceTbody tr")');
        record('C7 回归：切换监控分组不再影响设备列表',
            deviceListIntact === true && deviceRows > 0,
            '列表可见=' + deviceListIntact + '，行数=' + deviceRows
            + '，全局 active 分段=' + segActiveAfterMonitor);

        // 详情
        await js('__t.click("#deviceTbody [data-action=\\"detail\\"]")');
        await waitFor('__t.visible("#detailModal")', 5000);
        record('C8 设备详情展示基本/规格/运行参数三段',
            (await js('__t.count("#detailBody .detail-section")')) === 3
            && (await js('__t.count("#detailBody .sensor-table tr")')) > 0,
            '分段=' + await js('__t.count("#detailBody .detail-section")'));
        await js('__t.click("#detailModal [data-close]")');
        await waitFor('__t.hidden("#detailModal")');

        // 新增：先验证必填校验
        await js('__t.click("#btnCreate")');
        await waitFor('__t.visible("#formModal")');
        await js('__t.click("#btnSave")');
        const requireName = await waitFor('__t.toast().indexOf("请填写设备名称") >= 0', 3000);
        record('C9 新增设备缺少名称时前端拦截', requireName === true, await js('__t.toast()'));

        await js('__t.setVal("#fDeviceName", "E2E测试机组")');
        await js('__t.setVal("#fZone", "D区")');
        await js('__t.setVal("#fManufacturer", "测试厂商")');
        await js('__t.click("#btnSave")');
        const created = await waitFor('__t.toast().indexOf("新增成功") >= 0', 6000);
        const createToast = await js('__t.toast()');
        const newIdMatch = /设备编号 (\S+)/.exec(createToast || '');
        const newId = newIdMatch ? newIdMatch[1] : '';
        await waitFor('__t.hidden("#formModal")');
        await sleep(600);
        record('C10 新增设备成功并返回设备编号',
            created === true && newId !== '',
            createToast);
        record('C11 新增后列表总数加一',
            (await js('__t.text("#listTitle")')).indexOf('共 ' + (baseTotal + 1) + ' 台') >= 0,
            await js('__t.text("#listTitle")'));

        // 编辑
        await js('__t.setVal("#qKeyword", ' + JSON.stringify(newId) + ')');
        await js('__t.click("#btnSearch")');
        await sleep(700);
        await js('__t.click("#deviceTbody [data-action=\\"edit\\"]")');
        await waitFor('__t.visible("#formModal")');
        const typeDisabled = await js('__t.q("#fDeviceType").disabled === true');
        await js('__t.setVal("#fDeviceName", "E2E改名机组")');
        await js('__t.click("#btnSave")');
        const edited = await waitFor('__t.toast().indexOf("修改成功") >= 0', 6000);
        await sleep(700);
        record('C12 编辑设备成功且类型不可改',
            edited === true && typeDisabled === true
            && (await js('__t.hasText("#deviceTbody", "E2E改名机组")')) === true,
            '类型下拉禁用=' + typeDisabled);

        // 删除：先取消再确认，验证自定义确认弹窗两条分支
        await js('__t.click("#deviceTbody [data-action=\\"delete\\"]")');
        const confirmShown = await waitFor('__t.visible("#confirmModal")', 4000);
        await js('__t.click("#confirmCancel")');
        await waitFor('__t.hidden("#confirmModal")');
        await sleep(400);
        const stillThere = await js('__t.hasText("#deviceTbody", "E2E改名机组")');
        record('C13 删除弹出页面内确认框，取消后不删除',
            confirmShown === true && stillThere === true,
            '确认框可见=' + confirmShown + '，取消后记录仍在=' + stillThere);

        await js('__t.click("#deviceTbody [data-action=\\"delete\\"]")');
        await waitFor('__t.visible("#confirmModal")', 4000);
        await js('__t.click("#confirmOk")');
        const deleted = await waitFor('__t.toast().indexOf("删除成功") >= 0', 6000);
        await js('__t.click("#btnReset")');
        await sleep(800);
        record('C14 确认删除后设备总数还原',
            deleted === true
            && (await js('__t.text("#listTitle")')).indexOf('共 ' + baseTotal + ' 台') >= 0,
            await js('__t.text("#listTitle")'));

        /* ==================== D. 运行监控 ==================== */
        section('D 运行监控 MM-01~05');
        await openTab('monitor');
        // 模块会记住上次选中的设备类型，这里先显式切回冷水机组
        await js('__t.click("#monitorTypes .seg[data-mtype=\\"CHILLER\\"]")');
        await waitFor('__t.count("#monitorCards .mon-card") > 0', 8000);
        record('D1 按设备类型查看实时运行卡片',
            (await js('__t.count("#monitorCards .mon-card")')) === 2,
            await js('__t.text("#monitorTitle")'));

        await js('__t.click("#monitorTypes .seg[data-mtype=\\"COOLING_TOWER\\"]")');
        await sleep(900);
        record('D2 切换到冷却塔后卡片数正确',
            (await js('__t.count("#monitorCards .mon-card")')) === 3,
            await js('__t.text("#monitorTitle")'));

        // 状态机：STOPPED 设备点启动
        const hasStart = await js('__t.count("#monitorCards [data-mact=\\"start\\"]") > 0');
        if (hasStart) {
            const startId = await js('__t.q("#monitorCards [data-mact=\\"start\\"]").getAttribute("data-id")');
            await js('__t.click("#monitorCards [data-mact=\\"start\\"]")');
            const started = await waitFor('__t.toast().indexOf("设备已启动") >= 0', 6000);
            await sleep(900);
            const nowRunning = await js(`(function () {
                var card = __t.qa('#monitorCards .mon-card').filter(function (c) {
                    return c.textContent.indexOf(${JSON.stringify(startId)}) >= 0;
                })[0];
                return !!card && card.textContent.indexOf('运行中') >= 0;
            })()`);
            record('D3 停机设备可启动并变为运行中',
                started === true && nowRunning === true, startId);
        } else {
            record('D3 停机设备可启动并变为运行中', false, '未找到可启动设备');
        }

        // 维护中设备不允许启停
        await js('__t.click("#monitorTypes .seg[data-mtype=\\"PUMP\\"]")');
        await sleep(900);
        const maintHint = await js(`(function () {
            var card = __t.qa('#monitorCards .mon-card').filter(function (c) {
                return c.textContent.indexOf('PM-004') >= 0 || c.textContent.indexOf('维护中') >= 0;
            })[0];
            return !!card && card.textContent.indexOf('当前状态不允许启停') >= 0;
        })()`);
        record('D4 非运行/停机状态给出启停限制提示', maintHint === true, '维护中设备提示生效');

        await js('__t.click("#btnCollect")');
        const collectToast = await waitFor('__t.toast().indexOf("采集完成") >= 0', 10000);
        record('D5 立即采集返回采集结果', collectToast === true, await js('__t.toast()'));

        await sleep(900);
        const collected = await js(`(function () {
            return __t.qa('#monitorCards .mon-card').some(function (c) {
                return c.textContent.indexOf('采集') >= 0 && c.textContent.indexOf('未采集') < 0;
            });
        })()`);
        record('D6 采集后卡片显示采集时间', collected === true);

        /*
         * 回归用例：设备详情走的是 /api/devices/{id}，与监控页的
         * /api/monitor/devices/{id} 是两条链路，曾经漏回查 sensor_data，
         * 导致详情页「采集时间」列恒为空。
         */
        await openTab('device');
        await waitFor('__t.count("#deviceTbody tr") > 0 && !__t.hasText("#deviceTbody", "加载中")', 8000);
        await js('__t.click("#deviceTbody [data-action=\\"detail\\"]")');
        await waitFor('__t.visible("#detailModal")', 5000);
        const detailTimes = await js(`(function () {
            var times = __t.qa('#detailBody .sensor-table tbody tr').map(function (tr) {
                return tr.children[3] ? tr.children[3].textContent.trim() : '';
            });
            var infoTime = '';
            __t.qa('#detailBody .info-item').forEach(function (item) {
                var k = item.querySelector('.k');
                if (k && k.textContent.indexOf('最近采集') >= 0) {
                    infoTime = (item.querySelector('.v') || {}).textContent || '';
                }
            });
            return {times: times, infoTime: infoTime.trim()};
        })()`);
        const timePattern = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/;
        record('D7 设备详情展示每个参数的采集时间',
            detailTimes.times.length > 0
            && detailTimes.times.every((t) => timePattern.test(t))
            && timePattern.test(detailTimes.infoTime),
            JSON.stringify(detailTimes));
        await js('__t.click("#detailModal [data-close]")');
        await waitFor('__t.hidden("#detailModal")');

        /* ==================== E. 告警管理 ==================== */
        section('E 告警管理 AM-01~05');
        await openTab('alarm');
        await waitFor('__t.count("#alarmTbody tr") > 0 && !__t.hasText("#alarmTbody", "加载中")', 8000);
        record('E1 告警列表可加载',
            (await js('__t.count("#alarmTbody tr")')) > 0,
            await js('__t.count("#alarmTbody tr")') + ' 行');

        // 阈值配置
        await js('__t.click("[data-aview=\\"threshold\\"]")');
        await waitFor('__t.count("#thresholdTbody tr") > 1', 8000);
        const thresholdRows = await js('__t.count("#thresholdTbody tr:not(:has(.empty))")');
        record('E2 阈值配置从数据库加载', thresholdRows >= 7, thresholdRows + ' 条阈值');

        // 新增一条必然触发的阈值，让告警链路可确定地跑通
        await js('__t.click("#btnAddThreshold")');
        await waitFor('__t.visible("#thresholdModal")');
        await js('__t.setVal("#tDeviceType", "CHILLER")');
        await js('__t.setVal("#tParamKey", "chilledWaterSupplyTemp")');
        await js('__t.setVal("#tCompareOp", "LT")');
        await js('__t.setVal("#tThresholdValue", "999")');
        await js('__t.setVal("#tAlarmLevel", "WARNING")');
        await js('__t.click("#btnSaveThreshold")');
        const thAdded = await waitFor('__t.toast().indexOf("阈值已新增") >= 0', 6000);
        await sleep(800);
        record('E3 新增阈值成功',
            thAdded === true && (await js('__t.count("#thresholdTbody tr:not(:has(.empty))")')) === thresholdRows + 1,
            await js('__t.toast()'));

        // 触发告警：回到监控页采集一次
        await openTab('monitor');
        await sleep(400);
        await js('__t.click("#btnCollect")');
        await waitFor('__t.toast().indexOf("采集完成") >= 0', 12000);

        await openTab('alarm');
        await js('__t.click("[data-aview=\\"list\\"]")');
        await waitFor('__t.count("#alarmTbody tr") > 0', 8000);
        await js('__t.setVal("#qAlarmStatus", "UNHANDLED")');
        await sleep(900);
        const unhandledRows = await js('__t.count("#alarmTbody [data-alarm-handle]")');
        record('E4 采集后按阈值自动产生未处理告警', unhandledRows > 0, unhandledRows + ' 条待处理');

        await js('__t.click("#alarmTbody [data-alarm-handle]")');
        await waitFor('__t.visible("#handleModal")');
        await js('__t.click("#btnSubmitHandle")');
        const requireHandler = await waitFor('__t.toast().indexOf("均为必填") >= 0', 3000);
        record('E5 处理告警缺少必填项时拦截', requireHandler === true, await js('__t.toast()'));

        await js('__t.setVal("#hHandler", "E2E值班员")');
        await js('__t.setVal("#hMeasure", "E2E自动化：复测并调整运行参数")');
        await js('__t.click("#btnSubmitHandle")');
        const handled = await waitFor('__t.toast().indexOf("告警已处理") >= 0', 8000);
        await sleep(1000);
        const afterHandle = await js('__t.count("#alarmTbody [data-alarm-handle]")');
        record('E6 提交处理后告警从待办列表移除',
            handled === true && afterHandle < unhandledRows,
            unhandledRows + ' → ' + afterHandle);

        // 阈值启停与删除
        await js('__t.click("[data-aview=\\"threshold\\"]")');
        await waitFor('__t.count("#thresholdTbody tr") > 1', 8000);
        const toggled = await js(`(function () {
            var btn = __t.qa('#thresholdTbody [data-th-toggle]').filter(function (b) { return b.textContent === '停用'; })[0];
            if (!btn) { return false; }
            btn.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));
            return true;
        })()`);
        const toggledToast = await waitFor('__t.toast().indexOf("阈值已停用") >= 0', 5000);
        record('E7 阈值可停用', toggled === true && toggledToast === true, await js('__t.toast()'));

        await sleep(500);
        await js('__t.click("#thresholdTbody [data-th-del]")');
        await waitFor('__t.visible("#confirmModal")', 4000);
        await js('__t.click("#confirmOk")');
        const thDeleted = await waitFor('__t.toast().indexOf("阈值已删除") >= 0', 6000);
        await sleep(800);
        record('E8 删除阈值后回到初始条数',
            thDeleted === true
            && (await js('__t.count("#thresholdTbody tr:not(:has(.empty))")')) === thresholdRows,
            await js('__t.count("#thresholdTbody tr:not(:has(.empty))")') + ' 条');

        /* ==================== F. 数据分析 ==================== */
        section('F 数据分析 AN-01~02');
        await openTab('analysis');
        await waitFor('__t.count("#statCards .stat-card") === 7', 8000);
        record('F1 运行统计七项指标齐全',
            (await js('__t.count("#statCards .stat-card")')) === 7,
            await js('__t.count("#statCards .stat-card")') + ' 张');

        record('F2 三类分布图均有数据',
            (await js('__t.count("#barByType .bar-item")')) >= 4
            && (await js('__t.count("#barByStatus .bar-item")')) >= 3
            && (await js('__t.count("#barByZone .bar-item")')) >= 1,
            '类型 ' + await js('__t.count("#barByType .bar-item")')
            + ' / 状态 ' + await js('__t.count("#barByStatus .bar-item")')
            + ' / 区域 ' + await js('__t.count("#barByZone .bar-item")'));

        await js('__t.click("#btnTrend")');
        const chartReady = await waitFor('__t.count(".trend-svg") === 1', 8000);
        record('F3 趋势图成功绘制', chartReady === true);

        if (chartReady) {
            // 早期用固定 viewBox + preserveAspectRatio=none 拉伸，文字会横向变形
            const geo = await js(`(function () {
                var svg = __t.q('.trend-svg');
                var box = __t.q('#trendChart');
                return {
                    par: svg.getAttribute('preserveAspectRatio'),
                    width: Number(svg.getAttribute('width')),
                    boxWidth: box.clientWidth - 24
                };
            })()`);
            record('F4 趋势图按容器宽度等比绘制，文字不变形',
                geo.par !== 'none' && Math.abs(geo.width - geo.boxWidth) <= 2,
                'viewBox 宽=' + geo.width + '，容器内容宽=' + geo.boxWidth);

            const hovered = await js(`(function () {
                var svg = __t.q('.trend-svg');
                var r = svg.getBoundingClientRect();
                svg.dispatchEvent(new MouseEvent('mousemove', {
                    clientX: r.left + r.width * 0.6,
                    clientY: r.top + r.height * 0.5,
                    bubbles: true
                }));
                return __t.visible('#trendTip');
            })()`);
            record('F5 鼠标悬停显示单点数值气泡',
                hovered === true && /\d/.test(await js('__t.text("#trendTip")')),
                await js('__t.text("#trendTip")') || '');
        } else {
            record('F4 趋势图按容器宽度等比绘制，文字不变形', false, '图形未生成');
            record('F5 鼠标悬停显示单点数值气泡', false, '图形未生成');
        }

        /* ==================== G. 全局一致性 ==================== */
        section('G 全局一致性');
        await openTab('dashboard');
        await sleep(900);
        record('G1 总览数字与接口实时一致',
            Number(await js('__t.text("#kpiTotal")')) === baseTotal,
            await js('__t.text("#kpiTotal")') + ' vs ' + baseTotal);

        record('G2 全程没有原生对话框弹出',
            problems.dialogs.length === 0, problems.dialogs.join(' | ') || '无');

        record('G3 全程没有未捕获的脚本异常',
            problems.exceptions.length === 0,
            problems.exceptions.slice(0, 3).join(' | ') || '无');

        record('G4 全程没有 console.error 输出',
            problems.consoleErrors.length === 0,
            problems.consoleErrors.slice(0, 3).join(' | ') || '无');

        /* ==================== H. 响应式 ==================== */
        section('H 响应式布局');

        await cdp.send('Emulation.setDeviceMetricsOverride', {
            width: 900, height: 1000, deviceScaleFactor: 1, mobile: false
        });
        await sleep(500);
        const sideWidth = await js('getComputedStyle(__t.q(".sidebar")).width');
        record('H1 900px 宽度下侧栏收为图标条',
            Math.abs(parseFloat(sideWidth) - 76) <= 1, sideWidth);

        await cdp.send('Emulation.setDeviceMetricsOverride', {
            width: 430, height: 900, deviceScaleFactor: 1, mobile: false
        });
        await sleep(500);
        const narrow = await js(`(function () {
            var tab = __t.q('.tab');
            var kpi = getComputedStyle(__t.q('#kpiRow'));
            var topo = __t.q('.topology-scroll');
            return {
                direction: getComputedStyle(tab).flexDirection,
                tabsVisible: __t.qa('.tab').filter(function (t) {
                    return t.getBoundingClientRect().width > 0;
                }).length,
                kpiCols: kpi.gridTemplateColumns.split(' ').length,
                topoScrollable: topo.scrollWidth > topo.clientWidth
            };
        })()`);
        record('H2 430px 宽度下导航改为图标上文字下',
            narrow.direction === 'column' && narrow.tabsVisible === 5,
            narrow.direction + ' / ' + narrow.tabsVisible + ' 个入口');
        record('H3 430px 宽度下 KPI 两列排布',
            narrow.kpiCols === 2, narrow.kpiCols + ' 列');
        record('H4 窄屏拓扑可横向滚动而不压缩节点',
            narrow.topoScrollable === true, '可滚动=' + narrow.topoScrollable);

        await cdp.send('Emulation.setDeviceMetricsOverride', {
            width: VIEW.width, height: VIEW.height, deviceScaleFactor: 1, mobile: false
        });
        // 窄屏阶段可能留下横向滚动位置，截图前复位
        await js('window.scrollTo(0, 0)');
        await sleep(400);

        await shot('final-dashboard');
    } catch (err) {
        record('执行过程未抛异常', false, err.message);
        const file = await shot('failure');
        if (file) {
            console.log('  失败现场截图：' + file);
        }
    } finally {
        cdp.close();
        child.kill();
    }

    /* ------------------------------ 汇总 ------------------------------ */
    const passed = RESULTS.filter((r) => r.ok).length;
    const failed = RESULTS.length - passed;
    console.log('\n' + '='.repeat(64));
    console.log('界面端到端测试：' + RESULTS.length + ' 项检查，通过 ' + passed + '，失败 ' + failed);
    if (failed > 0) {
        console.log('\n失败明细：');
        RESULTS.filter((r) => !r.ok).forEach((r) => {
            console.log('  - [' + r.section + '] ' + r.name + '：' + r.detail);
        });
    }
    console.log('截图与浏览器档案目录：' + ARTIFACT_DIR);
    process.exit(failed > 0 ? 1 : 0);
}

main().catch((err) => {
    console.error('运行失败：' + err.message);
    process.exit(1);
});
