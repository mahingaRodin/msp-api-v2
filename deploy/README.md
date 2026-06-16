# MSP API — Deployment Guide

Server: **185.181.10.165** (K3s) · App folder: **`/opt/apps/msp-api`**

## K3s vs Docker Compose — recommendation

| | **K3s (recommended)** | Docker Compose |
|---|---------------------|----------------|
| Your server | Already installed | Would run alongside K3s |
| Multi-app on one IP | Each app = namespace + ingress host | Harder to share one IP cleanly |
| Server load | Pulls **pre-built image** only (~200MB) | Same, but no built-in ingress routing |
| DB/Redis | In-cluster (manifests included) | You manage compose stack yourself |
| Auto-deploy from `main` | ✅ This setup | Possible, but not configured here |

**Use K3s** — you already have it, Traefik ingress works, and each future app gets its own folder + namespace.

---

## What lives on the server (minimal)

**No source code. No Maven. No full git repo.**

```
/opt/apps/msp-api/
  deploy.sh              ← pull image + kubectl rollout
  bootstrap.sh           ← one-time setup helper
  secrets.example.yaml
  secrets/
    secrets.yaml         ← DB + JWT (manual, never in git)
  k8s/                   ← manifests only (~10 small YAML files)
```

**Built in CI (GitHub Actions):** JAR → Docker image → pushed to **GHCR**  
`ghcr.io/YOUR_GITHUB_USER/msp-api:latest`

---

## Published URLs

| | URL |
|---|-----|
| Swagger | http://msp.185.181.10.165.nip.io/swagger-ui.html |
| Health | http://msp.185.181.10.165.nip.io/actuator/health |

---

## Step 1 — Manual upload via SCP (one time)

From your PC (PowerShell), in the project root:

```powershell
$KEY = "C:\Users\user\.ssh\kamatera_pack_dev"
$SERVER = "root@185.181.10.165"
$APP = "/opt/apps/msp-api"

ssh -i $KEY $SERVER "mkdir -p $APP/k8s $APP/secrets"

scp -i $KEY deploy/server/deploy.sh deploy/server/bootstrap.sh deploy/server/secrets.example.yaml "${SERVER}:${APP}/"

scp -i $KEY k8s/*.yaml "${SERVER}:${APP}/k8s/"
```

On the server:

```bash
# If scripts fail with "set: pipefail", fix Windows line endings:
sed -i 's/\r$//' /opt/apps/msp-api/*.sh

chmod +x /opt/apps/msp-api/deploy.sh /opt/apps/msp-api/bootstrap.sh

# Namespace must exist before secrets (bootstrap does this automatically)
kubectl apply -f /opt/apps/msp-api/k8s/namespace.yaml
bash /opt/apps/msp-api/bootstrap.sh
```

---

## Step 2 — Make GHCR image pullable on the server

After the **first** GitHub Actions run, a package appears at:  
`https://github.com/YOUR_USER?tab=packages`

**Option A — Public package (simplest)**  
GitHub → Packages → `msp-api` → Package settings → **Change visibility → Public**  
No pull secret needed on the server.

**Option B — Private package**  
Create a GitHub PAT with `read:packages`, then on the server:

```bash
kubectl create secret docker-registry ghcr-pull \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_PAT \
  -n msp

# Patch deployment to use it (one time):
kubectl patch deployment msp-backend -n msp --type=json \
  -p='[{"op":"add","path":"/spec/template/spec/imagePullSecrets","value":[{"name":"ghcr-pull"}]}]'
```

---

## Step 3 — Auto-deploy on every push to `main`

Workflow: `.github/workflows/deploy.yml`

On each push/merge to **`main`**:

1. **Build** — Maven + Docker image in GitHub (not on your server)
2. **Push** — `ghcr.io/YOUR_USER/msp-api:<commit-sha>`
3. **SCP** — only `deploy.sh` + `k8s/*.yaml` to `/opt/apps/msp-api`
4. **SSH** — `deploy.sh` pulls the new image and rolls out K3s

### GitHub secrets required

| Secret | Value |
|--------|-------|
| `SERVER_HOST` | `185.181.10.165` |
| `SERVER_USERNAME` | `root` |
| `SERVER_SSH_KEY` | Private key (`kamatera_pack_dev`) |

`GITHUB_TOKEN` is provided automatically for GHCR push.

### Trigger manually

GitHub → **Actions** → **Build and Deploy to K3s** → **Run workflow**

---

## Step 4 — Open firewall

Allow **port 80** on Kamatera firewall and on the server (`ufw allow 80/tcp`).

---

## Verify

```bash
kubectl get pods -n msp
curl http://msp.185.181.10.165.nip.io/actuator/health
```

---

## Multi-app pattern

```
/opt/apps/msp-api/     → msp.185.181.10.165.nip.io
/opt/apps/other-app/   → other.185.181.10.165.nip.io
```

Each app: own folder, own k8s namespace, own GHCR image, own ingress host.
