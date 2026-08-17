import http from "k6/http";
import { check, sleep } from "k6";
import { BASE, jsonHeaders, login } from "./helpers.js";

/**
 * Staging CI gate — smoke + moderate concurrency (production-like SLOs, shorter than prod soak).
 * Runs inside GitHub Actions against the docker-compose.ci stack.
 */
export const options = {
  scenarios: {
    catalog_ramp: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 15 },
        { duration: "1m", target: 30 },
        { duration: "30s", target: 0 },
      ],
      exec: "catalogBrowse",
      gracefulRampDown: "20s",
    },
    customer_reads: {
      executor: "constant-vus",
      vus: 8,
      duration: "45s",
      exec: "customerReads",
      startTime: "15s",
    },
    store_admin: {
      executor: "constant-vus",
      vus: 5,
      duration: "45s",
      exec: "storeAdminReads",
      startTime: "15s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<800", "p(99)<2000"],
    "http_req_duration{flow:catalog}": ["p(95)<600"],
    "http_req_duration{flow:customer}": ["p(95)<900"],
    "http_req_duration{flow:admin}": ["p(95)<700"],
  },
};

const customerEmail = __ENV.EMAIL || "customer@posify.demo";
const customerPassword = __ENV.PASSWORD || "Demo!123";
const adminEmail = __ENV.ADMIN_EMAIL || "manager@posify.demo";
const adminPassword = __ENV.ADMIN_PASSWORD || "Demo!123";

export function setup() {
  const health = http.get(`${BASE}/actuator/health`);
  if (health.status !== 200) {
    throw new Error(`API not healthy: ${health.status} ${health.body}`);
  }
  return {
    customer: login(customerEmail, customerPassword),
    admin: login(adminEmail, adminPassword),
  };
}

export function catalogBrowse() {
  const tags = { tags: { flow: "catalog" } };
  check(http.get(`${BASE}/api/catalog/stores?size=20`, tags), {
    stores: (r) => r.status === 200,
  });
  check(http.get(`${BASE}/api/catalog/products?page=0&size=12&inStock=true`, tags), {
    products: (r) => r.status === 200,
  });
  check(http.get(`${BASE}/api/catalog/branches?size=20`, tags), {
    branches: (r) => r.status === 200,
  });
  sleep(Math.random() * 1.5 + 0.5);
}

export function customerReads() {
  const session = login(customerEmail, customerPassword);
  const opts = { ...jsonHeaders(session.token), tags: { flow: "customer" } };
  check(http.get(`${BASE}/api/shop/cart`, opts), { cart: (r) => r.status === 200 });
  check(http.get(`${BASE}/api/shop/favorites?page=0&size=8`, opts), {
    favorites: (r) => r.status === 200,
  });
  check(http.get(`${BASE}/api/shop/orders?page=0&size=8`, opts), {
    orders: (r) => r.status === 200,
  });
  sleep(1);
}

export function storeAdminReads() {
  const session = login(adminEmail, adminPassword);
  const opts = { ...jsonHeaders(session.token), tags: { flow: "admin" } };
  check(http.get(`${BASE}/api/profile/me`, opts), { profile: (r) => r.status === 200 });
  check(http.get(`${BASE}/api/portal/business/stores?page=0&size=12`, opts), {
    stores: (r) => r.status === 200,
  });
  sleep(1);
}
