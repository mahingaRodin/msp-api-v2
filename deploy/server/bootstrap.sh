#!/usr/bin/env bash
# One-time server setup — creates minimal /opt/apps/msp-api layout (no application source).
set -euo pipefail

APP_ROOT="/opt/apps/msp-api"

echo "==> Creating ${APP_ROOT}"
mkdir -p "${APP_ROOT}/k8s" "${APP_ROOT}/secrets"

if [[ ! -f "${APP_ROOT}/secrets/secrets.yaml" ]]; then
  if [[ -f "${APP_ROOT}/secrets.example.yaml" ]]; then
    cp "${APP_ROOT}/secrets.example.yaml" "${APP_ROOT}/secrets/secrets.yaml"
    JWT_SECRET="$(openssl rand -hex 32)"
    DB_PASSWORD="$(openssl rand -hex 16)"
    sed -i "s/CHANGE_ME_db_user/msp_user/" "${APP_ROOT}/secrets/secrets.yaml"
    sed -i "s/CHANGE_ME_db_password/${DB_PASSWORD}/" "${APP_ROOT}/secrets/secrets.yaml"
    sed -i "s/CHANGE_ME_use_openssl_rand_hex_32/${JWT_SECRET}/" "${APP_ROOT}/secrets/secrets.yaml"
    echo "Generated ${APP_ROOT}/secrets/secrets.yaml — review before applying."
  else
    echo "Copy secrets.example.yaml to ${APP_ROOT}/ first, then re-run."
    exit 1
  fi
fi

chmod +x "${APP_ROOT}/deploy.sh" 2>/dev/null || true

if [[ -f "${APP_ROOT}/k8s/namespace.yaml" ]]; then
  echo "==> Creating Kubernetes namespace"
  kubectl apply -f "${APP_ROOT}/k8s/namespace.yaml"
fi

if [[ -f "${APP_ROOT}/secrets/secrets.yaml" ]]; then
  echo "==> Applying secrets"
  kubectl apply -f "${APP_ROOT}/secrets/secrets.yaml"
fi

echo ""
echo "Bootstrap complete."
echo "Next:"
echo "  Push to main (GitHub Actions deploys), or run:"
echo "  IMAGE=ghcr.io/YOUR_USER/msp-api:tag bash ${APP_ROOT}/deploy.sh"
