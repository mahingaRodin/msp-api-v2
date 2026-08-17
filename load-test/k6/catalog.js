import http from "k6/http";
import { check, sleep } from "k6";

/**
 * Public catalog load — no login.
 *
 * Run (from this folder, with API up on :5000):
 *   k6 run catalog.js
 *
 * Stronger local soak:
 *   k6 run --vus 20 --duration 2m catalog.js
 */
export const options = {
  scenarios: {
    browse: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: 10 },
        { duration: "40s", target: 10 },
        { duration: "20s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<800"],
  },
};

const BASE = __ENV.BASE_URL || "http://localhost:5000";

export default function () {
  const health = http.get(`${BASE}/actuator/health`);
  check(health, { "health 200": (r) => r.status === 200 });

  const stores = http.get(`${BASE}/api/catalog/stores?size=20`);
  check(stores, { "stores 200": (r) => r.status === 200 });

  const products = http.get(`${BASE}/api/catalog/products?page=0&size=12`);
  check(products, { "products 200": (r) => r.status === 200 });

  const branches = http.get(`${BASE}/api/catalog/branches?size=20`);
  check(branches, { "branches 200": (r) => r.status === 200 });

  sleep(1);
}
