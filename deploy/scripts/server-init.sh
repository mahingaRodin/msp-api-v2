#!/usr/bin/env bash
# One-time server bootstrap for MSP API on a shared K3s host.
# Run as root on pack-dev-01 (185.181.10.165):
#   curl -fsSL <raw-url>/deploy/scripts/server-init.sh | bash
# Or copy this repo and run: bash deploy/scripts/server-init.sh
set -euo pipefail

APP_NAME="msp-api"
APP_ROOT="/opt/apps/${APP_NAME}"
REPO_URL="${REPO_URL:-}"   # e.g. https://github.com/you/msp-api.git
REPO_BRANCH="${REPO_BRANCH:-main}"

echo "==> Creating app directory layout at ${APP_ROOT}"
mkdir -p "${APP_ROOT}/repo" "${APP_ROOT}/secrets" "${APP_ROOT}/logs"

if [[ ! -d "${APP_ROOT}/repo/.git" ]]; then
  if [[ -z "${REPO_URL}" ]]; then
    echo "Set REPO_URL to your git remote, then re-run."
    echo "Example:"
    echo "  REPO_URL=https://github.com/you/msp-api.git bash deploy/scripts/server-init.sh"
    exit 1
  fi
  echo "==> Cloning repository"
  git clone --branch "${REPO_BRANCH}" "${REPO_URL}" "${APP_ROOT}/repo"
else
  echo "==> Repository already cloned"
fi

if [[ ! -f "${APP_ROOT}/secrets/secrets.yaml" ]]; then
  echo "==> Creating secrets template"
  cp "${APP_ROOT}/repo/k8s/secret.example.yaml" "${APP_ROOT}/secrets/secrets.yaml"
  JWT_SECRET="$(openssl rand -hex 32)"
  DB_PASSWORD="$(openssl rand -hex 16)"
  sed -i "s/CHANGE_ME_db_user/msp_user/" "${APP_ROOT}/secrets/secrets.yaml"
  sed -i "s/CHANGE_ME_db_password/${DB_PASSWORD}/" "${APP_ROOT}/secrets/secrets.yaml"
  sed -i "s/CHANGE_ME_use_openssl_rand_hex_32/${JWT_SECRET}/" "${APP_ROOT}/secrets/secrets.yaml"
  echo "Generated secrets at ${APP_ROOT}/secrets/secrets.yaml"
  echo "Review the file, then apply with:"
  echo "  kubectl apply -f ${APP_ROOT}/secrets/secrets.yaml"
else
  echo "==> Secrets file already exists at ${APP_ROOT}/secrets/secrets.yaml"
fi

chmod +x "${APP_ROOT}/repo/deploy/scripts/deploy.sh"

echo ""
echo "Server init complete."
echo ""
echo "Next steps:"
echo "  1. kubectl apply -f ${APP_ROOT}/secrets/secrets.yaml"
echo "  2. bash ${APP_ROOT}/repo/deploy/scripts/deploy.sh"
echo ""
echo "Swagger will be at: http://msp.185.181.10.165.nip.io/swagger-ui.html"
