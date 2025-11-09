import http from 'k6/http';
import { sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { randomSeed } from 'k6';
import exec from 'k6/execution';

// ----- Config (init) -----
const cfg = JSON.parse(open(__ENV.CONFIG || 'config.json'));
const BASE = cfg.baseUrl.replace(/\/$/, '');
const OPS = Object.fromEntries(cfg.operations.map(o => [o.id, o]));
const BEH = cfg.behavior;

// runtime toggles
const LOAD = Number(__ENV.LOAD || 10);        // VUs
const DURATION = Number(__ENV.DURATION || 30); // seconds
const RAMP_UP = Number(__ENV.RAMPUP || 5);     // seconds to exclude from stats
const SEED = Number(__ENV.SEED || 1);
randomSeed(SEED);

// ----- Metrics (init) -----
const rt = new Trend('rt', true);         // response time, tagged by {op}
const inv = new Counter('invocations');   // call counts per op

// ----- Options (init) -----
export const options = {
    scenarios: {
        main: { executor: 'constant-vus', vus: LOAD, duration: `${DURATION}s`, gracefulStop: '0s' }
    },
    thresholds: {} // none for now
};

// ----- Helpers -----
function randint(a, b) { return Math.floor(Math.random() * (b - a + 1)) + a; }
function tmpl(path, params = {}) { return path.replace(/{(\w+)}/g, (_, k) => encodeURIComponent(params[k] ?? '')); }
function tNow() { return Math.floor(exec.instance.currentTestRunDuration); }

// ----- VU logic -----
export default function () {
    // Walk the minimal behavior once per iteration
    for (const step of BEH.steps) {
        const opId = step[0];
        const op = OPS[opId];
        if (!op) continue;

        const url = `${BASE}${tmpl(op.path)}`;
        const res = http.request(op.method, url, null, { tags: { op: opId } });

        // exclude ramp-up from metrics
        if (tNow() >= RAMP_UP) {
            rt.add(res.timings.duration, { op: opId });
            inv.add(1, { op: opId });
        }

        sleep(randint(BEH.thinkFrom, BEH.thinkTo) / 1000);
    }
}

// ----- Summary (engine, once) -----
export function handleSummary(data) {
    // Pull per-op aggregates from Trend submetrics
    const m = data.metrics.rt;
    const subs = m?.submetrics || {};
    const perOp = {};
    let totalCount = 0;

    for (const key in subs) {
        const sm = subs[key];
        if (!sm.tags || !sm.tags.op) continue;
        const v = sm.values;
        perOp[sm.tags.op] = {
            count: v.count,
            mean_ms: v.avg,
            stdev_ms: v.stdev,
            p95_ms: v['p(95)']
        };
        totalCount += v.count;
    }

    // compute ν (share) per op
    for (const op in perOp) {
        perOp[op].share_nu = totalCount ? perOp[op].count / totalCount : 0;
    }

    const out = {
        meta: { vus: LOAD, durationSec: DURATION, rampUpSec: RAMP_UP, seed: SEED, ts: new Date().toISOString() },
        totals: { totalInvocations: totalCount },
        perOperation: perOp
    };

    return { 'mini_summary.json': JSON.stringify(out, null, 2) };
}
