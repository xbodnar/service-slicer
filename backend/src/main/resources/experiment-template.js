import http from 'k6/http';
import { sleep, check } from 'k6';

const BASE_URL = __ENV.BASE_URL;
const VUS = parseInt(__ENV.VUS);
const DURATION = __ENV.DURATION;

// {{BEHAVIOR_MODELS_JSON}}
const behaviorModels = {{BEHAVIOR_MODELS_JSON}};

// {{USAGE_PROFILE_JSON}}
const usageProfile = {{USAGE_PROFILE_JSON}};

export const options = {
    scenarios: {
        load_test: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
        }
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count', 'std'],
};

function randomThinkTime(thinkFromMs, thinkToMs) {
    const ms = thinkFromMs + Math.random() * (thinkToMs - thinkFromMs);
    return ms / 1000.0; // seconds
}

function selectBehaviorModel() {
    const r = Math.random();
    let cumulative = 0.0;
    for (const bm of behaviorModels) {
        const weight = usageProfile[bm.id] || 0;
        cumulative += weight;
        if (r <= cumulative) return bm;
    }
    // Fallback: first model
    return behaviorModels[0];
}

function executeStep(step, context) {
    const url = `${BASE_URL}${step.path}`;
    const params = {
        headers: step.headers || {},
        tags: {
            behavior_id: context.behaviorId,
            actor: context.actor,
            method: step.method,
            path: step.path,
        },
    };

    // query params if any
    if (step.params && Object.keys(step.params).length > 0) {
        params.params = step.params;
    }

    let res;
    switch (step.method) {
        case 'GET':
            res = http.get(url, params);
            break;
        case 'POST':
            res = http.post(url, null, params);
            break;
        case 'PUT':
            res = http.put(url, null, params);
            break;
        case 'DELETE':
            res = http.del(url, null, params);
            break;
        default:
            // unsupported method - skip
            return;
    }

    check(res, {
        [`${step.method} ${step.path} status OK`]: (r) =>
            r.status >= 200 && r.status < 300,
    });
}

export default function () {
    const bm = selectBehaviorModel();
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
