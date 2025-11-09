import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import exec from 'k6/execution';

// ======== CONFIG LOADING ========
const cfg = JSON.parse(open(__ENV.CONFIG || 'config.json'));
const thresholds = __ENV.THRESHOLDS ? JSON.parse(open(__ENV.THRESHOLDS)) : null;

const BASE_URL = cfg.baseUrl.replace(/\/$/, '');
const ARCH = __ENV.ARCH || cfg.architecture || 'arch';
const MODE = (__ENV.MODE || 'target').toLowerCase(); // 'baseline' | 'target'
const LOAD = Number(__ENV.LOAD || cfg.operationalProfile.loads?.[0] || 10);
const DURATION = Number(__ENV.DURATION || cfg.durationSec || 900);
const RAMP_UP = Number(__ENV.RAMPUP || cfg.rampUpSec || 60);
const SEED = Number(__ENV.SEED || cfg.randomSeed || 1);

// ======== METRICS ========
// Per-op response time trend (we'll compute μ, σ ourselves in handleSummary)
const rt = new Trend('rt', true); // time, tags: {op, actor, behavior}
const invCount = new Counter('invocations'); // per-op invocations
const errCount = new Counter('errors');

// Keep raw samples for mean/stddev by op (within steady-state window)
const samples = {}; // op -> array of durations (ms)
const invByOp = {}; // op -> count
const invTotal = { n: 0 };

// ======== HELPERS ========
function randint(a, b) { return Math.floor(Math.random() * (b - a + 1)) + a; }
function pick(array, weights) {
    // weights same length, sum ~1
    const r = Math.random();
    let acc = 0;
    for (let i = 0; i < array.length; i++) {
        acc += weights[i];
        if (r <= acc) return array[i];
    }
    return array[array.length - 1];
}
function templ(path, params = {}) {
    return path.replace(/{(\w+)}/g, (_, k) => encodeURIComponent(params[k] ?? `{${k}}`));
}
function nowSec() { return Math.floor(exec.instance.currentTestRunDuration); }

// Index ops by id for quick lookup
const opIndex = {};
for (const o of cfg.operations) opIndex[o.id] = o;

// Normalize behavior weights to sum=1
const beh = cfg.behaviors;
const sumW = beh.reduce((s, b) => s + b.usageProfile, 0) || 1;
const behWeights = beh.map(b => b.usageProfile / sumW);

// ======== OPTIONS ========
export const options = {
    scenarios: {
        main: {
            executor: 'constant-vus',
            vus: LOAD,
            duration: `${DURATION}s`,
            gracefulStop: '0s'
        }
    },
    thresholds: {} // we compute paper metrics in handleSummary
};

// ======== VU LOGIC ========
export default function () {
    // Pick a behavior for this VU at start
    const b = pick(beh, behWeights);
    exec.vu.tags['actor'] = b.actor;
    exec.vu.tags['behavior'] = b.id;
    exec.vu.tags['arch'] = ARCH;
    exec.vu.tags['mode'] = MODE;
    exec.vu.tags['load'] = String(LOAD);

    // Walk the steps forever until duration ends
    for (let i = 0; i < b.steps.length; i++) {
        const step = b.steps[i];
        // step is ["oX"] (single op) — can be extended to multiple ops per step if you like
        const opId = step[0];
        const op = opIndex[opId];
        if (!op) continue;

        const params = (cfg.params && cfg.params[opId]) || {};
        const url = `${BASE_URL}${templ(op.path, params)}`;
        const res = http.request(op.method, url, null, {
            tags: { op: opId, actor: b.actor, behavior: b.id, arch: ARCH, mode: MODE, load: String(LOAD) }
        });

        const t = res.timings.duration;
        const tSec = nowSec();

        // exclude ramp-up window from paper stats
        if (tSec >= RAMP_UP) {
            rt.add(t, { op: opId, actor: b.actor, behavior: b.id, arch: ARCH, mode: MODE, load: String(LOAD) });
            invCount.add(1, { op: opId });
            invByOp[opId] = (invByOp[opId] || 0) + 1;
            invTotal.n++;
            (samples[opId] ||= []).push(t);
        }

        if (!check(res, { 'http 2xx/3xx': r => r.status >= 200 && r.status < 400 })) {
            errCount.add(1, { op: opId });
        }

        const think = randint(b.thinkFrom, b.thinkTo);
        sleep(think / 1000);
    }
}

// ======== SUMMARY & PAPER METRICS ========
function mean(arr){ return arr.reduce((s,x)=>s+x,0)/arr.length; }
function stddev(arr){
    const m = mean(arr);
    const v = arr.reduce((s,x)=>s+(x-m)*(x-m),0)/(arr.length||1);
    return Math.sqrt(v);
}

export function handleSummary(data) {
    // Compute per-op μ, σ, ν
    const ops = Object.keys(samples);
    const perOp = {};
    for (const op of ops) {
        const arr = samples[op] || [];
        const mu = arr.length ? mean(arr) : 0;
        const sd = arr.length ? stddev(arr) : 0;
        const nu = invTotal.n ? (invByOp[op] || 0) / invTotal.n : 0;
        perOp[op] = { mu, sigma: sd, nu, n: arr.length };
    }

    // Baseline thresholds Γ^0 = μ^0 + 3σ^0  (paper Eq. (1))
    let gamma = null, passFail = null, sShare = null, dm = null, failures = null, sgpo = null;

    if (MODE === 'baseline') {
        gamma = {};
        for (const [op, s] of Object.entries(perOp)) gamma[op] = s.mu + 3*s.sigma; // Γ_j^0
    }

    // Target metrics: δ, s_j = ν·δ, DM_a(λ)= f(λ)*Σ s_j
    if (MODE === 'target' && thresholds) {
        passFail = {};
        sShare = {};
        failures = {};
        sgpo = {};
        for (const [op, s] of Object.entries(perOp)) {
            const g = thresholds[op] ?? null;
            if (g == null) continue;
            const delta = s.mu <= g ? 1 : 0;    // Eq. (2)
            passFail[op] = delta;
            sShare[op] = s.nu * delta;          // Eq. (3)
            if (!delta) {
                // Scalability gap SG_j = ν at first failing λ (approximated here at this λ)
                const SG = s.nu;
                const PO = g ? (s.mu - g)/g : null; // performance offset (paper Eq. (14))
                failures[op] = true;
                sgpo[op] = { SG, PO };
            }
        }
        const f = (() => {
            const idx = cfg.operationalProfile.loads.findIndex(l => Number(l) === Number(LOAD));
            return idx >= 0 ? cfg.operationalProfile.frequencies[idx] : 0;
        })();
        const sumS = Object.values(sShare).reduce((a,b)=>a+b,0);
        dm = f * sumS; // relative DM at this load (paper Eq. (8))
    }

    const out = {
        meta: {
            arch: ARCH, mode: MODE, load: LOAD,
            durationSec: DURATION, rampUpSec: RAMP_UP, seed: SEED,
            timestamp: new Date().toISOString()
        },
        counts: { totalInvocations: invTotal.n },
        perOperation: perOp,
        baselineThresholds: gamma,
        target: MODE === 'target' ? {
            passFail, sShare, dm, failures: sgpo
        } : null
    };

    const prefix = `${ARCH}_${MODE}_L${LOAD}`;
    const files = {};
    files[`${prefix}_summary.json`] = JSON.stringify(out, null, 2);

    if (MODE === 'baseline' && gamma) {
        files[`thresholds_${ARCH}.json`] = JSON.stringify(gamma, null, 2);
    }

    // Let k6 also dump its default JSON
    files[`${prefix}_k6.json`] = JSON.stringify(data, null, 2);
    return files;
}
