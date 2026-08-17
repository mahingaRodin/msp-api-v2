import http from "k6/http";
import { check, sleep } from "k6";
import { BASE, jsonHeaders, login } from "./helpers.js";

/**
 * Customer read path: cart, favorites, orders, catalog.
 *
 *   k6 run -e EMAIL=customer@example.com -e PASSWORD=secret customer.js
 */
export const options = {
  vus: 8,
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

  const products = http.get(`${BASE}/api/catalog/products?page=0&size=12`, opts);
  check(products, { "catalog products": (r) => r.status === 200 });

  const cart = http.get(`${BASE}/api/shop/cart`, opts);
  check(cart, { "cart 200": (r) => r.status === 200 });

  const favs = http.get(`${BASE}/api/shop/favorites?page=0&size=12`, opts);
  check(favs, { "favorites 200": (r) => r.status === 200 });

  const orders = http.get(`${BASE}/api/shop/orders?page=0&size=12`, opts);
  check(orders, { "my orders 200": (r) => r.status === 200 });

  sleep(1);
}
