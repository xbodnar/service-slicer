import http from 'k6/http';
import {check, sleep} from 'k6';
import jsonpath from 'https://jslib.k6.io/jsonpath/1.0.2/index.js';

// --- CONFIG FROM ORCHESTRATOR / GENERATED ---
const BASE_URL = __ENV.BASE_URL;
const USAGE_PROFILE_URL = __ENV.USAGE_PROFILE_URL;

const TARGET_VUS = parseInt(__ENV.TARGET_VUS);
const DURATION = __ENV.DURATION;
const TEST_CASE_ID = __ENV.TEST_CASE_ID;

export const options = {
    vus: TARGET_VUS,
    duration: DURATION,
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

function randomThinkTime(thinkFromMs, thinkToMs) {
    const ms = thinkFromMs + Math.random() * (thinkToMs - thinkFromMs);
    return ms / 1000.0; // seconds
}

function pickBehaviorModel(usageProfile) {
    const r = Math.random();
    let cumulative = 0.0;
    for (const bm of usageProfile) {
        const weight = bm.frequency || 0;
        cumulative += weight;
        if (r <= cumulative) return bm;
    }
    // Fallback: first model
    return usageProfile[0];
}

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
    const result = jsonpath.query(json, path);
    // jsonpath.query returns an array of matches, return the first match or null
    return result.length > 0 ? result[0] : null;
}

function executeStep(step, ctx) {
    // Normalize URL to avoid double slashes
    const path = applyTemplate(step.path, ctx);
    const normalizedPath = path.startsWith('/') ? path : '/' + path;
    const baseUrl = BASE_URL.endsWith('/') ? BASE_URL.slice(0, -1) : BASE_URL;
    let url = baseUrl + normalizedPath;

    // Append query parameters to URL
    const queryParams = applyTemplate(step.params || {}, ctx);
    if (queryParams && Object.keys(queryParams).length > 0) {
        const queryString = Object.entries(queryParams)
            .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
            .join('&');
        url += (url.includes('?') ? '&' : '?') + queryString;
    }

    const headers = applyTemplate(step.headers || {}, ctx);
    const body = applyTemplate(step.body || null, ctx);

    // Add Content-Type header for JSON requests if not already set
    if (body && !headers['Content-Type'] && !headers['content-type']) {
        headers['Content-Type'] = 'application/json';
    }

    const reqParams = {
        headers: headers,
        tags: {
            test_case_id: TEST_CASE_ID,
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

    return ok;
}

// Setup function - fetch UsageProfile from USAGE_PROFILE_URL
export function setup() {
    if (BASE_URL === undefined) {
        throw new Error('BASE_URL is not defined');
    }
    if (USAGE_PROFILE_URL === undefined) {
        throw new Error('USAGE_PROFILE_URL is not defined');
    }
    if (TARGET_VUS === undefined) {
        throw new Error('TARGET_VUS is not defined');
    }
    if (DURATION === undefined) {
        throw new Error('DURATION is not defined');
    }
    if (TEST_CASE_ID === undefined) {
        throw new Error('TEST_CASE_ID is not defined');
    }

    console.log(`Fetching usageProfile from: ${USAGE_PROFILE_URL}`);

    const res = http.get(USAGE_PROFILE_URL);

    if (res.status !== 200) {
        throw new Error(`Failed to fetch usageProfile from ${USAGE_PROFILE_URL}: status ${res.status}, body: ${res.body}`);
    }

    const usageProfile = res.json();
    console.log(`Usage Profile loaded successfully`);

    return usageProfile;
}

export default function (usageProfile) {
    const bm = pickBehaviorModel(usageProfile);

    const context = {
        behaviorId: bm.id,
        actor: bm.actor,
    };

    for (const step of bm.steps) {
        const success = executeStep(step, context);
        if (!success) {
            // Stop executing remaining steps and move to next iteration
            return;
        }
        sleep(randomThinkTime(step.waitMsFrom, step.waitMsTo));
    }
}
