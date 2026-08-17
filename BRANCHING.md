# Branching and release flow

This repo uses two long-lived branches:

| Branch | GitHub Actions | Purpose |
|---|---|---|
| **`staging`** | `staging.yml` | Unit tests, Flyway check, k6 load gate (prod-like Docker stack) |
| **`main`** | `deploy.yml` only | Build image → push GHCR → deploy Azure VM |

Feature work merges into **`staging` first**. Only after staging is green do you merge **`staging` → `main`** to release.

```
feature/*  ──PR──►  staging  ──(CI green)──►  main  ──►  Azure deploy
```

## What staging proves

On every push/PR to `staging`, CI runs:

1. **Unit tests** — Mockito + `@WebMvcTest` suite  
2. **Flyway integration** — fresh DB, migrations through **V16**  
3. **Load gate** — Docker stack sized like production (`768m` API / `0.5` CPU):
   - `ci-smoke.js` — catalog ramp (30 VU) + customer + store-admin reads  
   - `prod-catalog.js` **ramp** profile — up to **50 VU** catalog browse  
   - SLO thresholds: fail rate &lt; 1–2%, p95 &lt; 500–800 ms  

That is the **automated path toward a production-load claim**. It is not a substitute for a dedicated staging server soak if you outgrow one GitHub runner.

### Optional: 15-minute soak (manual)

In GitHub → **Actions** → **Staging — tests and load gate** → **Run workflow**, enable **Run 15m soak**. Use before a major release or when tuning pool sizes.

## Frontend (`multi-tenant-pos-fe.v2`)

Same idea: merge to **`staging`** for CI (`npm ci`, lint, build). Merge **`staging` → `main`** for production (Vercel/production deploy if configured).

## Setup (one time)

### 1. Create `staging` from your current dev branch

```powershell
cd msp-api
git checkout dev-test          # or your active dev branch
git pull origin dev-test
git checkout -b staging
git push -u origin staging
```

Repeat for frontend:

```powershell
cd msp-fe
git checkout dev-pc
git pull origin dev-pc
git checkout -b staging
git push -u origin staging
```

### 2. Protect branches on GitHub

**Settings → Branches → Branch protection rules**

**`main`**

- Require pull request before merging  
- Require status checks: *(none from staging workflow — deploy is separate)*  
- Restrict who can push (optional)  
- Do **not** run tests on direct push if you only merge from staging via PR  

**`staging`**

- Require status checks before merge:
  - `Unit tests`
  - `Flyway integration (optional)` → rename job is `Flyway integration (optional)` - actually required in staging-summary
  - `Load gate (k6 + prod-like stack)`

### 3. Daily developer flow

```powershell
git checkout staging
git pull
git checkout -b feature/my-change
# ... commit ...
git push -u origin feature/my-change
# Open PR → staging
```

When staging CI is green:

```powershell
# GitHub PR: staging → main  (backend + frontend separately)
```

`main` push triggers **deploy only** (no k6 on main).

## Local load tests (same scripts as CI)

```powershell
cd load-test
docker compose -f docker-compose.ci.yml up -d --build --wait
cd k6
k6 run -e BASE_URL=http://127.0.0.1:5000 ci-smoke.js
k6 run -e BASE_URL=http://127.0.0.1:5000 -e PROFILE=ramp prod-catalog.js
```

See [`load-test/README.md`](load-test/README.md) for SLO tables and production checklist.

## Honest capacity statement

| Level | Evidence |
|---|---|
| **Smoke** | Local 5–10 VU (`run-all.ps1`) |
| **Release gate** | Staging CI: 30–50 VU catalog + authenticated reads on prod-like container |
| **Production claim** | Staging CI green **+** optional soak **+** (recommended) same tests on Azure staging clone with `DEMO_SEED=false` |

Until staging CI is green on every merge, treat **`main` deploys as functional releases, not capacity-certified releases**.
