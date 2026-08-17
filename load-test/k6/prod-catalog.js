import http from "k6/http";
import { check, sleep } from "k6";

/**
 * Production-style catalog browse (no auth).
 * Use only against a dedicated staging / load-test environment — not live tenants.
 *
 *   k6 run prod-catalog.js
 *   k6 run -e BASE_URL=https://staging.example.com prod-catalog.js
 *   k6 run -e PROFILE=soak prod-catalog.js
 */
const BASE = __ENV.BASE_URL || "http://localhost:5000";
const PROFILE = __ENV.PROFILE || "ramp";
const ciStack = __ENV.CI_STACK === "true";

const profiles = {
  // CI gate on GitHub Actions (768m / 0.5 CPU) — lower VUs, relaxed latency SLOs
  ci_ramp: {
    scenarios: {
      browse: {
        executor: "ramping-vus",
        startVUs: 0,
        stages: [
          { duration: "45s", target: 10 },
          { duration: "1m", target: 20 },
          { duration: "30s", target: 0 },
        ],
        gracefulRampDown: "20s",
      },
    },
    thresholds: {
      http_req_failed: ["rate<0.02"],
      http_req_duration: ["p(95)<2500", "p(99)<4000"],
    },
  },
  // Quick pre-release gate (~4 min): ramp 0→50, hold, ramp down
  ramp: {
    scenarios: {
      browse: {
        executor: "ramping-vus",
        startVUs: 0,
        stages: [
          { duration: "1m", target: 25 },
          { duration: "2m", target: 50 },
          { duration: "30s", target: 50 },
          { duration: "30s", target: 0 },
        ],
        gracefulRampDown: "30s",
      },
    },
    thresholds: {
      http_req_failed: ["rate<0.01"],
      http_req_duration: ["p(95)<500", "p(99)<1200"],
    },
  },
  // Sustained traffic (~15 min): find connection-pool / GC / Redis issues
  soak: {
    scenarios: {
      browse: {
        executor: "constant-vus",
        vus: 30,
        duration: "15m",
      },
    },
    thresholds: {
      http_req_failed: ["rate<0.005"],
      http_req_duration: ["p(95)<600", "p(99)<1500"],
    },
  },
  // Find breaking point — stop when errors climb; run manually with care
  stress: {
    scenarios: {
      browse: {
        executor: "ramping-vus",
        startVUs: 0,
        stages: [
          { duration: "2m", target: 50 },
          { duration: "3m", target: 100 },
          { duration: "3m", target: 150 },
          { duration: "2m", target: 0 },
        ],
        gracefulRampDown: "1m",
      },
    },
    thresholds: {
      http_req_failed: ["rate<0.05"],
      http_req_duration: ["p(95)<2000"],
    },
  },
};

const effectiveProfile =
  ciStack && PROFILE === "ramp" ? "ci_ramp" : PROFILE;

export const options = profiles[effectiveProfile] || profiles.ramp;

export default function () {
  const stores = http.get(`${BASE}/api/catalog/stores?size=20`);
  check(stores, { "stores 200": (r) => r.status === 200 });

  const products = http.get(`${BASE}/api/catalog/products?page=0&size=12&inStock=true`);
  check(products, { "products 200": (r) => r.status === 200 });

  const branches = http.get(`${BASE}/api/catalog/branches?size=20`);
  check(branches, { "branches 200": (r) => r.status === 200 });

  sleep(Math.random() * 2 + 0.5);
}
