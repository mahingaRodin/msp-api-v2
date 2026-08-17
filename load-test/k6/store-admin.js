import http from "k6/http";
import { check, sleep } from "k6";
import { BASE, jsonHeaders, login } from "./helpers.js";

/**
 * Store admin / manager: profile + tenant stores list.
 *
 *   k6 run -e EMAIL=owner@example.com -e PASSWORD=secret store-admin.js
 */
export const options = {
  vus: 5,
  duration: "1m",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1200"],
  },
};

export function setup() {
  return login(__ENV.EMAIL, __ENV.PASSWORD);
}

export default function (data) {
  const opts = jsonHeaders(data.token);

  const me = http.get(`${BASE}/api/profile/me`, opts);
  check(me, { "profile 200": (r) => r.status === 200 });

  const stores = http.get(`${BASE}/api/portal/business/stores?page=0&size=12`, opts);
  check(stores, { "stores 200": (r) => r.status === 200 });

  const storeId = data.user && data.user.storeId;
  if (storeId) {
    const analytics = http.get(`${BASE}/api/analytics/store/${storeId}`, opts);
    check(analytics, {
      "store analytics": (r) => r.status === 200 || r.status === 403,
    });
  }

  sleep(1);
}
