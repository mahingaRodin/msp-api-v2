#!/usr/bin/env bash
# One-time Azure Docker host setup. No git clone, no Java, no K3s.
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/apps/msp-api}"

echo "==> Creating ${APP_ROOT}"
mkdir -p "${APP_ROOT}"

if [[ ! -f "${APP_ROOT}/.env" ]]; then
  if [[ -f "${APP_ROOT}/env.example" ]]; then
    JWT_SECRET="$(openssl rand -hex 32)"
    DB_PASSWORD="$(openssl rand -hex 16)"
    sed \
      -e "s/CHANGE_ME_db_password/${DB_PASSWORD}/" \
      -e "s/CHANGE_ME_use_openssl_rand_hex_32/${JWT_SECRET}/" \
      "${APP_ROOT}/env.example" > "${APP_ROOT}/.env"
    chmod 600 "${APP_ROOT}/.env"
    echo "Generated ${APP_ROOT}/.env"
  else
    echo "Missing ${APP_ROOT}/env.example"
    exit 1
  fi
fi

chmod +x "${APP_ROOT}/deploy.sh" 2>/dev/null || true

echo ""
echo "Bootstrap complete."
echo "Next: IMAGE=ghcr.io/mahingarodin/msp-api:latest bash ${APP_ROOT}/deploy.sh"
echo "Swagger: http://4.168.192.169:5000/swagger-ui.html"
echo "UI (after frontend deploy): http://4.168.192.169/"
