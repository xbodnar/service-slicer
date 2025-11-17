import http from 'k6/http';
import {sleep} from 'k6';

// --- CONFIG FROM ORCHESTRATOR / GENERATED ---
const BASE_URL = __ENV.BASE_URL;
const TARGET_VUS = parseInt(__ENV.TARGET_VUS);
const DURATION = __ENV.DURATION;
const LOAD_TEST_CONFIG_FILE = __ENV.LOAD_TEST_CONFIG_FILE;

const experiment = JSON.parse(open(LOAD_TEST_CONFIG_FILE));

const behaviorModels = experiment.loadTestConfig.behaviorModels;

export const options = {
    scenarios: {
        main: {
            executor: 'ramping-vus',
            stages: [
                { duration: '20s', target: TARGET_VUS },
                { duration: DURATION, target: TARGET_VUS },
                { duration: '20s', target: 0 },
            ],
            gracefulStop: '10s',
        }
    },
};

function randomThinkTime(thinkFromMs, thinkToMs) {
    const ms = thinkFromMs + Math.random() * (thinkToMs - thinkFromMs);
    return ms / 1000.0; // seconds
}

function pickBehaviorModel() {
    const r = Math.random();
    let cumulative = 0.0;
    for (const bm of behaviorModels) {
        const weight = bm.usageProfile || 0;
        cumulative += weight;
        if (r <= cumulative) return bm;
    }
    // Fallback: first model
    return behaviorModels[0];
}

function applyTemplateToString(str, ctx) {
    if (!str || typeof str !== 'string') return str;
    return str.replace(/\$\{(\w+)\}/g, (_, name) => {
        const value = ctx[name];
        if (value === undefined) {
            // Optionally log or throw
            return '';
        }
        return String(value);
    });
}

function applyTemplate(obj, ctx) {
    if (obj == null) return obj;
    if (typeof obj === 'string') return applyTemplateToString(obj, ctx);
    if (Array.isArray(obj)) return obj.map((v) => applyTemplate(v, ctx));
    if (typeof obj === 'object') {
        const out = {};
        for (const [k, v] of Object.entries(obj)) {
            out[k] = applyTemplate(v, ctx);
        }
        return out;
    }
    return obj;
}

function getFromJsonPath(json, path) {
    if (!path.startsWith('$.')) {
        throw new Error(`Path must start with '$.': ${path}`);
    }
    const parts = path.slice(2).split('.');
    let cur = json;
    for (const part of parts) {
        const m = part.match(/(\w+)\[(\d+)\]/); // e.g. articles[0]
        if (m) {
            const key = m[1];
            const idx = Number(m[2]);
            cur = cur?.[key]?.[idx];
        } else {
            cur = cur?.[part];
        }
        if (cur === undefined || cur === null) break;
    }
    return cur;
}

function executeStep(step, ctx) {
    const path= applyTemplate(step.path, ctx);
    const url = BASE_URL + path;
    const headers = applyTemplate(step.headers || {}, ctx);
    const params = applyTemplate(step.params || {}, ctx);
    const body = applyTemplate(step.body || null, ctx);

    const reqParams = {
        headers: headers,
        params: params,
        tags: {
            behavior_id: ctx.behaviorId,
            actor: ctx.actor,
            method: step.method,
            path: step.path,
        },
    };

    let res;
    switch (step.method) {
        case 'GET':
            res = http.get(url, reqParams);
            break;
        case 'POST':
            res = http.post(url, body, reqParams);
            break;
        case 'PUT':
            res = http.put(url, body, reqParams);
            break;
        case 'DELETE':
            res = http.del(url, body, reqParams);
            break;
        default:
            throw new Error(`Unsupported method: ${step.method}`);
    }

    // Save response fields into context
    if (step.save) {
        let jsonBody;
        try {
            jsonBody = res.json();
        } catch (_) {
            jsonBody = null;
        }

        for (const [varName, selector] of Object.entries(step.save)) {
            // Simple version: assume selector is JSON path in body
            ctx[varName] = jsonBody ? getFromJsonPath(jsonBody, selector) : null;
        }
    }
}

export default function () {
    const bm = pickBehaviorModel();
    const context = {
        behaviorId: bm.id,
        actor: bm.actor,
        // token: ... (if you later add auth)
    };

    for (const step of bm.steps) {
        executeStep(step, context);
        sleep(randomThinkTime(bm.thinkFrom, bm.thinkTo));
    }
}
