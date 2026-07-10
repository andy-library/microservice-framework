import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        constant_rate: {
            executor: 'constant-arrival-rate',
            rate: parseInt(__ENV.RPS || '5000'),
            timeUnit: '1s',
            duration: __ENV.DURATION || '30s',
            preAllocatedVUs: parseInt(__ENV.VUS || '100'),
            maxVUs: parseInt(__ENV.VUS || '100') * 2,
        },
    },
    thresholds: {
        http_req_duration: ['p(99)<60'],
        http_req_failed: ['rate<0.0001'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    const res = http.get(`${BASE_URL}/perf/cache-hit`);

    check(res, {
        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
        'has requestId': (r) => typeof r.json('requestId') === 'string' && r.json('requestId').length > 0,
        'code is 0': (r) => r.json('code') === 0,
        'has source field': (r) => typeof r.json('data.source') === 'string',
        'has hit field': (r) => r.json('data.hit') === true,
    });
}
