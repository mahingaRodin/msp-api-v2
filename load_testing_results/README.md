# Load testing

k6 scripts for the POSify API (`msp-api`) plus an optional Lighthouse pass on public frontend pages.

These runs measure **local correctness and latency** under light concurrent traffic. They are not a production capacity, soak, or breakpoint test.

## What we conclude about the application

From the 17 Aug 2026 local run (Spring Boot on `:5000`, PostgreSQL 17, Redis, demo seed, k6 v2.2):

- **The core product paths work under concurrent use.** Public catalog, JWT login, customer cart/favorites/orders, store-admin profile/stores, and store analytics all returned successfully at 5–10 virtual users.
- **Latency is comfortable on a developer machine.** Median responses were about **15–20 ms**. p95 stayed **well under 100 ms**, against script budgets of 800–1500 ms. Throughput was roughly **14–27 requests/s** on this laptop — the API was not the bottleneck at this scale.
- **First-hit / max latency is much higher than the median** (catalog max ~1.5 s vs median ~18 ms). That is typical Spring warmup (JIT, Hikari, Redis, Hibernate), not a sustained-load problem.
- **Shop orders need a `customers` row, not only a `users` row.** The demo account `customer@posify.demo` could log in and use cart/favorites, but `GET /api/shop/orders` failed with `Customer profile missing` until that profile was backfilled. `DemoDataSeeder` now creates (or repairs) the Customer record.
- **Concurrent cart writes for the same user are slightly racy.** Checkout (3 VUs, one shared demo customer, no real orders) had **2 / 129** failed add-to-cart calls (~0.5%). Reads stayed green. Fine for a demo; worth a unique-constraint / upsert look if many tabs share one cart.
- **This does not prove production scale.** Ten local VUs with ~1 s think time is a smoke/load check, not multi-tenant isolation, Redis eviction, connection-pool saturation, or peak-hour POS traffic.

## Latest local results (17 Aug 2026)

| Script | VUs / duration | Requests | Failed | p95 | Checks |
|---|---|---|---|---|---|
| `catalog.js` (health + stores + products + branches) | ramp to 10 / 80s | 2,120 | **0%** | 80 ms | 100% |
| `customer.js` (catalog, cart, favorites, orders) | 8 / 1m | 1,665 | **0%** | 85 ms | 100% |
| `checkout.js` (add cart + read cart + list orders; no `PLACE_ORDER`) | 3 / 45s | 390 | **0.51%** | 33 ms | 99.5% |
| `store-admin.js` (profile, portal stores, analytics) | 5 / 1m | 856 | **0%** | 35 ms | 100% |

Thresholds used: catalog fail rate `<2%` and p95 `<800 ms`; customer/admin fail rate `<5%` and p95 `<1200 ms`; checkout fail rate `<8%` and p95 `<1500 ms`. All four scripts passed after the Customer-profile fix.

## Prerequisites

1. PostgreSQL on `localhost:5432` (database `tenant_pos_sys`) and Redis on `localhost:6379`.
2. **msp-api running on port 5000** with the `local` profile (`app.demo.seed=true`). LocalStack is not required for these scripts.
3. [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) on `PATH` (Chocolatey: `choco install k6`).

Health check the API before a run:

```text
GET http://localhost:5000/actuator/health  →  { "status": "UP" }
```

Demo logins (password `Demo!123`):

| Role | Email |
|---|---|
| Customer | `customer@posify.demo` |
| Store admin | `manager@posify.demo` |
| Branch manager | `branch@posify.demo` |
| Cashier | `cashier@posify.demo` |

Super admin is `mahingarodin@gmail.com` / `admin!123`.

## How to run

From the repo root, with the API already up:

```powershell
.\load-test\run-all.ps1 `
  -CustomerEmail "customer@posify.demo" `
  -CustomerPassword "Demo!123" `
  -AdminEmail "manager@posify.demo" `
  -AdminPassword "Demo!123"
```

- Catalog always runs (no login).
- Customer + checkout run only if customer credentials are passed.
- Store-admin runs only if admin credentials are passed.
- Add `-PlaceOrders` to have checkout **write real orders**. Use a test account only.

Individual scripts (from `load-test/k6`):

```powershell
k6 run -e BASE_URL=http://localhost:5000 catalog.js
k6 run -e EMAIL=customer@posify.demo -e PASSWORD=Demo!123 customer.js
k6 run -e EMAIL=customer@posify.demo -e PASSWORD=Demo!123 checkout.js
k6 run -e EMAIL=manager@posify.demo -e PASSWORD=Demo!123 store-admin.js
```

Heavier local soak (example):

```powershell
k6 run --vus 20 --duration 2m catalog.js
```

### Frontend (optional)

Public-page Lighthouse (no login). Frontend must be running (`npm run dev` or `npm run preview`):

```powershell
node load-test/lighthouse.mjs
$env:FE_URL='http://localhost:4173'; node load-test/lighthouse.mjs
```

## Scripts

| File | What it hits |
|---|---|
| `k6/catalog.js` | `/actuator/health`, `/api/catalog/stores`, `/products`, `/branches` |
| `k6/customer.js` | Catalog + `/api/shop/cart`, `/favorites`, `/orders` |
| `k6/checkout.js` | Add to cart; optional `POST /api/shop/checkout` if `PLACE_ORDER=1` |
| `k6/store-admin.js` | `/api/profile/me`, `/api/portal/business/stores`, `/api/analytics/store/{id}` |
| `k6/helpers.js` | `BASE_URL`, login, catalog snapshot |
| `lighthouse.mjs` | `/`, `/login`, `/apply-store` |

Do not point a high-VU run at production unless that is an explicit, scheduled test.

## What these tests do *not* cover

- Placing orders at volume (`-PlaceOrders` was not used in the baseline)
- Cashier POS, refunds, billing/trial, or super-admin flows
- Multi-tenant isolation under load
- Redis cache hit rate, DB pool exhaustion, or JVM GC
- The Vite/Vercel frontend (except optional Lighthouse)

Next useful steps if you want a stronger claim: more VUs, distinct users per VU (so carts do not collide), a timed soak (10–30 min), and p99 / error-budget tracking — still against a dedicated test database.
