import http from 'k6/http';
import {check, sleep} from 'k6';

// --- CONFIG FROM ORCHESTRATOR / GENERATED ---
const BASE_URL = __ENV.BASE_URL;
const CONFIG_URL = __ENV.CONFIG_URL;

// Use per-vu-iterations executor to run all behavior models once
export const options = {
    scenarios: {
        validation: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '5m',
        }
    }
};

// Random value generators for creating unique test data
const randomGenerators = {
    'uuid': () => crypto.randomUUID(),
    'string': (length) => {
        const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        const len = length || 10;
        return Array.from({length: len}, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    },
    'number': (min, max) => {
        const minVal = min !== undefined ? min : 0;
        const maxVal = max !== undefined ? max : 100;
        return Math.floor(Math.random() * (maxVal - minVal + 1)) + minVal;
    },
    'alphanumeric': (length) => {
        const len = length || 10;
        return Math.random().toString(36).substring(2, len + 2).padEnd(len, '0');
    },
    'timestamp': () => Date.now(),
    'timestampSeconds': () => Math.floor(Date.now() / 1000)
};

function applyTemplateToString(str, ctx) {
    if (!str || typeof str !== 'string') return str;

    // Replace template variables: ${varName} or ${random.func(args)}
    return str.replace(/\$\{([^}]+)\}/g, (match, expr) => {
        expr = expr.trim();

        // Check if it's a function call (contains parentheses)
        const functionCallMatch = expr.match(/^random\.(\w+)\((.*)\)$/);
        if (functionCallMatch) {
            const funcName = functionCallMatch[1];
            const argsStr = functionCallMatch[2].trim();

            if (randomGenerators[funcName]) {
                // Parse arguments (simple comma-separated values)
                const args = argsStr ? argsStr.split(',').map(arg => {
                    arg = arg.trim();
                    // Try to parse as number, otherwise keep as string
                    const num = Number(arg);
                    return isNaN(num) ? arg.replace(/['"]/g, '') : num;
                }) : [];

                return String(randomGenerators[funcName](...args));
            } else {
                console.warn(`Unknown random function: ${funcName}`);
                return match;
            }
        }

        // Otherwise, treat as context variable
        const value = ctx[expr];
        if (value === undefined) {
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
    console.log(`[DEBUG] Executing step ${step.operationId} with context:`, JSON.stringify(ctx));
    const url = BASE_URL + applyTemplate(step.path, ctx);
    console.log(`[DEBUG] Templated URL: ${url}`);
    const headers = applyTemplate(step.headers || {}, ctx);
    const params = applyTemplate(step.params || {}, ctx);
    const body = applyTemplate(step.body || null, ctx);

    // Add Content-Type header for JSON requests if not already set
    if (body && !headers['Content-Type'] && !headers['content-type']) {
        headers['Content-Type'] = 'application/json';
    }

    console.log(`[DEBUG] Final headers:`, JSON.stringify(headers));
    if (body) {
        console.log(`[DEBUG] Templated body:`, JSON.stringify(body));
    }

    const reqParams = {
        headers: headers,
        params: params,
        tags: {
            behavior_id: ctx.behaviorId,
            actor: ctx.actor,
            component: step.component || 'unknown',
            operation: step.operationId,
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
            res = http.post(url, JSON.stringify(body), reqParams);
            break;
        case 'PUT':
            res = http.put(url, JSON.stringify(body), reqParams);
            break;
        case 'DELETE':
            res = http.del(url, reqParams);
            break;
        default:
            throw new Error(`Unsupported method: ${step.method}`);
    }

    const ok = check(res, {
        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });

    if (!ok) {
        console.error(
            `❌ Request failed: ${step.method} ${url}\n` +
            `   Status: ${res.status}\n` +
            `   Response Body: "${res.body}"\n`
        );
    }

    // Save response fields into context
    if (step.save) {
        console.log(`[DEBUG] Save configuration found for step ${step.operationId}:`, JSON.stringify(step.save));
        console.log(`[DEBUG] Response status: ${res.status}`);
        console.log(`[DEBUG] Response body (raw): ${res.body}`);

        let jsonBody;
        try {
            jsonBody = res.json();
            console.log(`[DEBUG] Parsed JSON body:`, JSON.stringify(jsonBody));
        } catch (e) {
            console.log(`[DEBUG] Failed to parse response as JSON:`, e);
            jsonBody = null;
        }

        for (const [varName, selector] of Object.entries(step.save)) {
            const value = jsonBody ? getFromJsonPath(jsonBody, selector) : null;
            console.log(`[DEBUG] Extracting '${varName}' using selector '${selector}': ${value}`);
            ctx[varName] = value;
        }

        console.log(`[DEBUG] Updated context:`, JSON.stringify(ctx));
    }
}

function executeBehaviorModel(bm) {
    console.log(`Executing behavior model: ${bm.id} (${bm.actor})`);

    const context = {
        behaviorId: bm.id,
        actor: bm.actor,
    };

    for (const step of bm.steps) {
        executeStep(step, context);
        // Small sleep between steps to avoid overwhelming the system
        sleep(0.1);
    }
}

// Setup function - fetch configuration from CONFIG_URL
export function setup() {
    console.log(`Fetching configuration from: ${CONFIG_URL}`);

    const res = http.get(CONFIG_URL);

    if (res.status !== 200) {
        throw new Error(`Failed to fetch config from ${CONFIG_URL}: status ${res.status}, body: ${res.body}`);
    }

    const operationalSetting = res.json();
    console.log(`Configuration loaded successfully`);

    return operationalSetting;
}

// Main test function - run all behavior models sequentially
export default function (operationalSetting) {
    const usageProfile = operationalSetting.usageProfile;
    console.log(`Starting validation run for ${usageProfile.length} behavior models`);

    for (const bm of usageProfile) {
        executeBehaviorModel(bm);
    }

    console.log('Validation run completed for all behavior models');
}
