# Load testing

k6 scripts for the POSify API (`msp-api`) plus an optional Lighthouse pass on public frontend pages.

## CI on `staging` branch

GitHub Actions (`.github/workflows/staging.yml`) runs on every push/PR to **`staging`**:

| Step | What |
|---|---|
| Unit tests | Maven (`*Test`, excludes full `@SpringBootTest` context) |
| Flyway | Fresh Postgres, migrations through **V16** |
| Load gate | `docker-compose.ci.yml` (768m / 0.5 CPU) + `ci-smoke.js` + `prod-catalog.js` ramp (50 VU) |

**`main`** runs **`deploy.yml` only** (no tests). Merge `staging → main` after CI is green.

Optional **15m soak**: Actions → Staging workflow → Run workflow → enable **Run 15m soak**.

See [`BRANCHING.md`](../BRANCHING.md) for the full release flow.

---

These local runs measure **correctness and latency** under light concurrent traffic unless you use the production-style scripts below.

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

Do not point a high-VU run at production unless that is an explicit, scheduled test.

## Production readiness — what light tests do *not* prove

The smoke suite (5–10 VUs, ~1 s think time) answers: **“Do the main API paths work concurrently on a dev machine?”**  
It does **not** answer: **“How many real tenants / cashiers / shoppers can this stack carry in production?”**

### What the codebase already has (good foundations)

| Area | Status |
|---|---|
| **Schema migrations** | Flyway; prod uses `ddl-auto=validate` |
| **Caching** | Redis-backed Spring Cache (10 min TTL on entities/orders) |
| **Health / K8s** | Liveness + readiness probes on `:5000/actuator/health/*` |
| **Container limits** | Docker compose: API ~768 MB / 0.5 CPU; Postgres 256 MB |
| **Security model** | JWT, role-based routes, tenant-scoped data |
| **Multi-tenant product** | Business → store → branch isolation in domain layer |
| **Deploy path** | GHCR image, compose on server, optional K8s manifests |

That is **production-oriented architecture**, not a **production capacity certificate**.

### Gaps to close before calling it “production ready”

1. **No capacity baseline** — Hikari pool size, Tomcat threads, and Redis memory are mostly defaults; nothing is sized from measured RPS or concurrent POS terminals.
2. **Single API replica** — K8s deployment uses `replicas: 1` and `strategy: Recreate`; no horizontal scale test yet.
3. **Observability** — Actuator exposes health/info/mappings only; no Prometheus metrics, APM, or structured SLO dashboards in-repo.
4. **Write-path contention** — Shared demo customer under load showed ~0.5% cart add failures; production needs per-user cart upserts / unique constraints and checkout idempotency.
5. **Data model edge case** — Shop orders require a `customers` row; signup paths must always stay in sync (demo seed now backfills).
6. **Untested hot paths** — Cashier POS checkout, refunds, trial/billing block, SQS/email, and super-admin flows were not load-tested.
7. **Secrets & config** — Rotate any credentials committed to `application.properties`; use env/secrets in prod; set `DEMO_SEED=false` on real deployments (compose default is still `true` — change for production).
8. **Frontend** — Vercel FE is separate; API load tests do not cover CDN, bundle size, or authenticated SPA routes at scale.

### Suggested production SLOs (targets — validate on staging)

Use these as **pass/fail gates** on a **staging clone** with production-like CPU/RAM and `DEMO_SEED=false`:

| Flow | Traffic | Error budget | Latency (p95 / p99) |
|---|---|---|---|
| Public catalog browse | 50 concurrent shoppers | &lt; 1% failed | &lt; 500 ms / &lt; 1.2 s |
| Customer shop reads (cart, orders) | 40 concurrent | &lt; 2% failed | &lt; 800 ms / &lt; 2 s |
| Store-admin dashboard reads | 10 concurrent | &lt; 2% failed | &lt; 700 ms / &lt; 1.5 s |
| Sustained soak | 30 catalog VUs, 15 min | &lt; 0.5% failed | p95 stable (no drift &gt; 2×) |

Adjust numbers after the first staging run and your actual tenant size.

### Production-style k6 scripts

| Script | Purpose |
|---|---|
| `k6/prod-catalog.js` | Ramp (50 VU), soak (30 VU / 15m), or stress (→150 VU) — public catalog |
| `k6/prod-mixed.js` | 40 customer + 10 admin VUs; use **comma-separated account pools** so carts do not collide |
| `run-prod.ps1` | Safety wrapper — refuses localhost unless `-AllowLocal` |

```powershell
# Staging gate (recommended before each release)
.\load-test\run-prod.ps1 -BaseUrl https://your-staging-api.example.com -Profile ramp

# Soak — connection pools, GC, Redis
.\load-test\run-prod.ps1 -BaseUrl https://your-staging-api.example.com -Profile soak

# Mixed authenticated (multiple test customers required)
.\load-test\run-prod.ps1 -BaseUrl https://staging.example.com -Mixed `
  -CustomerEmails "c1@test.com,c2@test.com,c3@test.com" `
  -CustomerPasswords "pass1,pass2,pass3" `
  -AdminEmails "owner@test.com" `
  -AdminPasswords "pass"
```

While the test runs, watch **Postgres connections**, **Redis memory**, **API CPU/RAM**, and **application logs** — k6 alone will not show pool exhaustion until errors spike.

### Practical “production ready” checklist

- [ ] Staging environment mirrors prod sizing (not a laptop)
- [ ] `DEMO_SEED=false`, secrets in vault/env, mail/AWS configured
- [ ] `run-prod.ps1 -Profile ramp` and `soak` pass on staging
- [ ] `prod-mixed.js` with ≥10 distinct customer test accounts
- [ ] Cashier order-create script added and run at expected peak terminals/branch
- [ ] Prometheus/Grafana or host metrics + alerts (5xx rate, p99, DB connections)
- [ ] Runbook: scale replicas, rollback, Redis flush policy
- [ ] Load test scheduled off-hours; never surprise live tenants

**Bottom line:** The app is **architecturally deployable** and **functionally solid at low concurrency**. Calling it **production ready at scale** still requires staging load tests, observability, write-path hardening, and sizing tuned from those results — not from the 10-VU smoke run alone.

## What these tests do *not* cover

- Placing orders at volume (`-PlaceOrders` was not used in the baseline)
- Cashier POS, refunds, billing/trial, or super-admin flows
- Multi-tenant isolation under load
- Redis cache hit rate, DB pool exhaustion, or JVM GC
- The Vite/Vercel frontend (except optional Lighthouse)

Next useful steps if you want a stronger claim: more VUs, distinct users per VU (so carts do not collide), a timed soak (10–30 min), and p99 / error-budget tracking — still against a dedicated test database.
