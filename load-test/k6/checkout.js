import http from "k6/http";
import { check, sleep } from "k6";
import { BASE, jsonHeaders, login, catalogSnapshot } from "./helpers.js";

/**
 * Customer checkout: catalog → add to cart → (optional) place order with demo card → list orders.
 *
 * Dry run (no orders written):
 *   k6 run -e EMAIL=customer@example.com -e PASSWORD=secret checkout.js
 *
 * Actually place orders (writes to DB — use a test account):
 *   k6 run -e EMAIL=... -e PASSWORD=... -e PLACE_ORDER=1 checkout.js
 */
export const options = {
  vus: 3,
  duration: "45s",
  thresholds: {
    http_req_failed: ["rate<0.08"],
    http_req_duration: ["p(95)<1500"],
  },
};

const PLACE = __ENV.PLACE_ORDER === "1";

export function setup() {
  const session = login(__ENV.EMAIL, __ENV.PASSWORD);
  const snap = catalogSnapshot();
  if (!snap.productId || !snap.branchId) {
    throw new Error(
      "Catalog has no product or branch. Seed demo data before checkout load tests.",
    );
  }
  return { ...session, ...snap };
}

export default function (data) {
  const opts = jsonHeaders(data.token);

  const add = http.post(
    `${BASE}/api/shop/cart`,
    JSON.stringify({
      productId: data.productId,
      quantity: 1,
      branchId: data.branchId,
    }),
    opts,
  );
  check(add, { "add cart": (r) => r.status === 200 });

  const cart = http.get(`${BASE}/api/shop/cart`, opts);
  check(cart, { "read cart": (r) => r.status === 200 });

  if (PLACE) {
    const order = http.post(
      `${BASE}/api/shop/checkout`,
      JSON.stringify({
        branchId: data.branchId,
        paymentType: "CARD",
        cardBrand: "VISA",
        cardHolderName: "Load Test",
        cardNumber: "4242424242424242",
        cardExpiry: "12/30",
        cardCvv: "123",
      }),
      opts,
    );
    check(order, { "checkout 200": (r) => r.status === 200 });
  }

  const orders = http.get(`${BASE}/api/shop/orders?page=0&size=8`, opts);
  check(orders, { "orders after": (r) => r.status === 200 });

  sleep(1);
}
