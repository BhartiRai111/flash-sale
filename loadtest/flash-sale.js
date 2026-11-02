// k6 load test: many buyers hit one limited product at the same moment.
//
// Run (k6 installed):  k6 run loadtest/flash-sale.js
// Before running, raise the per-user rate limit, otherwise most requests get 429:
//   RATE_LIMIT_REPLENISH=1000 RATE_LIMIT_BURST=2000 docker compose up -d api-gateway
//
// After the test check:
//   - GET /api/products/{id}  -> stock must be >= 0 (never negative)
//   - confirmed orders <= starting stock
//   - Grafana: p95 latency, error rate

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || 3); // "Smart Watch" seed product, 50 units

const accepted = new Counter('orders_accepted');
const soldOut = new Counter('orders_sold_out');
const rateLimited = new Counter('orders_rate_limited');

export const options = {
  scenarios: {
    flash_sale: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 300,
      stages: [
        { target: 200, duration: '10s' }, // sale opens: ramp to 200 orders/sec
        { target: 200, duration: '30s' }, // hold the peak
        { target: 0, duration: '5s' },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],           // 95% of requests under 500 ms
    http_req_failed: ['rate<0.01'],             // 409/429 are marked as expected below
  },
};

export function setup() {
  const res = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ username: 'alice', password: 'alice123' }),
    { headers: { 'Content-Type': 'application/json' } });
  check(res, { 'logged in': (r) => r.status === 200 });
  return { token: res.json('accessToken') };
}

export default function (data) {
  const res = http.post(`${BASE_URL}/api/orders`,
    JSON.stringify({ productId: PRODUCT_ID, quantity: 1, customerEmail: 'alice@example.com' }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${data.token}`,
        // A fresh key per request = a new purchase attempt
        'Idempotency-Key': `k6-${__VU}-${__ITER}-${Date.now()}`,
      },
      // 409 (sold out) and 429 (rate limited) are expected business answers, not errors
      responseCallback: http.expectedStatuses(200, 202, 409, 429),
    });

  if (res.status === 202) accepted.add(1);
  if (res.status === 409) soldOut.add(1);
  if (res.status === 429) rateLimited.add(1);

  check(res, { 'no server error': (r) => r.status < 500 });
}

export function teardown() {
  const product = http.get(`${BASE_URL}/api/products/${PRODUCT_ID}`);
  console.log(`Final stock for product ${PRODUCT_ID}: ${product.json('availableStock')} (must be >= 0)`);
}
