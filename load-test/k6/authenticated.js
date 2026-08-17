import http from "k6/http";
import { check, sleep } from "k6";
import { BASE, jsonHeaders, login } from "./helpers.js";

/** Alias of store-admin.js for older command examples. */
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
  const stores = http.get(`${BASE}/api/portal/business/stores?page=0&size=10`, opts);
  check(stores, { "my stores ok": (r) => r.status === 200 || r.status === 403 });
  sleep(1);
}
