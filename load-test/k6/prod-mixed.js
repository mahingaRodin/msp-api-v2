import http from "k6/http";
import { check, sleep } from "k6";
import { BASE, jsonHeaders, login, catalogSnapshot } from "./helpers.js";

/**
 * Mixed authenticated traffic (customer reads + store-admin reads).
 * Each VU logs in once in setup() — avoids hammering /api/auth/login every iteration.
 *
 * For realistic cart writes, give each VU its own account (comma-separated pools):
 *   CUSTOMER_EMAILS=a1@t.com,a2@t.com  CUSTOMER_PASSWORDS=p1,p2
 *
 *   k6 run -e BASE_URL=https://staging.example.com prod-mixed.js
 */
const customerEmails = (__ENV.CUSTOMER_EMAILS || __ENV.EMAIL || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);
const customerPasswords = (__ENV.CUSTOMER_PASSWORDS || __ENV.PASSWORD || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);
const adminEmails = (__ENV.ADMIN_EMAILS || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);
const adminPasswords = (__ENV.ADMIN_PASSWORDS || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

function pickCreds(emails, passwords, vu) {
  if (emails.length === 0) {
    throw new Error("Set CUSTOMER_EMAILS + CUSTOMER_PASSWORDS (or EMAIL + PASSWORD)");
  }
  const idx = (vu - 1) % emails.length;
  const password = passwords[idx] || passwords[0];
  return { email: emails[idx], password };
}

export const options = {
  scenarios: {
    customers: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "1m", target: 20 },
        { duration: "3m", target: 40 },
        { duration: "1m", target: 0 },
      ],
      exec: "customerFlow",
      gracefulRampDown: "30s",
    },
    admins: {
      executor: "constant-vus",
      vus: 10,
      duration: "5m",
      exec: "adminFlow",
      startTime: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<800", "p(99)<2000"],
    "http_req_duration{flow:customer}": ["p(95)<900"],
    "http_req_duration{flow:admin}": ["p(95)<700"],
  },
};

export function setup() {
  return catalogSnapshot();
}

export function customerFlow(data) {
  const creds = pickCreds(customerEmails, customerPasswords, __VU);
  const session = login(creds.email, creds.password);
  const opts = { ...jsonHeaders(session.token), tags: { flow: "customer" } };

  check(http.get(`${BASE}/api/catalog/products?page=0&size=12`, opts), {
    "catalog": (r) => r.status === 200,
  });
  check(http.get(`${BASE}/api/shop/cart`, opts), {
    "cart": (r) => r.status === 200,
  });
  check(http.get(`${BASE}/api/shop/favorites?page=0&size=8`, opts), {
    "favorites": (r) => r.status === 200,
  });
  check(http.get(`${BASE}/api/shop/orders?page=0&size=8`, opts), {
    "orders": (r) => r.status === 200,
  });

  if (data.productId && data.branchId && __ITER % 5 === 0) {
    check(
      http.post(
        `${BASE}/api/shop/cart`,
        JSON.stringify({
          productId: data.productId,
          quantity: 1,
          branchId: data.branchId,
        }),
        opts,
      ),
      { "add cart": (r) => r.status === 200 },
    );
  }

  sleep(Math.random() * 1.5 + 0.5);
}

export function adminFlow() {
  if (adminEmails.length === 0) {
    sleep(1);
    return;
  }
  const creds = pickCreds(adminEmails, adminPasswords, __VU);
  const session = login(creds.email, creds.password);
  const opts = { ...jsonHeaders(session.token), tags: { flow: "admin" } };

  check(http.get(`${BASE}/api/profile/me`, opts), {
    "profile": (r) => r.status === 200,
  });
  check(http.get(`${BASE}/api/portal/business/stores?page=0&size=12`, opts), {
    "stores": (r) => r.status === 200,
  });

  const storeId = session.user && session.user.storeId;
  if (storeId) {
    const analytics = http.get(`${BASE}/api/analytics/store/${storeId}`, opts);
    check(analytics, {
      "analytics": (r) => r.status === 200 || r.status === 403,
    });
  }

  sleep(Math.random() * 1.5 + 0.5);
}
