import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const confirmed = new Counter('orders_confirmed');
const pending = new Counter('orders_pending');
const queued = new Counter('orders_queued');

export const options = {
    stages: [
        { duration: '20s', target: 50 },
        { duration: '20s', target: 150 },
        { duration: '20s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.5'],
    },
};

const payload = JSON.stringify({
    productId: 'PROD-001',
    quantity: 1,
    amount: 100.00,
    customerId: '1',
});

export default function () {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${__ENV.TEST_JWT}`,
        },
    };
    const res = http.post(`${BASE_URL}/api/v1/orders`, payload, params);

    check(res, { 'status 200/202': (r) => [200, 202].includes(r.status) });

    if (res.status === 200) {
        const status = res.json('status');
        if (status === 'CONFIRMED') confirmed.add(1);
        if (status === 'PENDING') pending.add(1);
        if (status === 'QUEUED') queued.add(1);
    }
}
