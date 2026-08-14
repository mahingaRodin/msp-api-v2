#!/usr/bin/env bash
# One-time Azure Docker host setup. No git clone, no Java, no K3s.
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/apps/msp-api}"

echo "==> Creating ${APP_ROOT}"
mkdir -p "${APP_ROOT}"

if [[ ! -f "${APP_ROOT}/env.example" ]]; then
  echo "Missing ${APP_ROOT}/env.example"
  exit 1
fi

touch "${APP_ROOT}/.env"
chmod 600 "${APP_ROOT}/.env"

while IFS= read -r line || [[ -n "${line}" ]]; do
  [[ -z "${line}" || "${line}" == \#* ]] && continue
  key="${line%%=*}"
  if ! grep -q "^${key}=" "${APP_ROOT}/.env"; then
    echo "${line}" >> "${APP_ROOT}/.env"
  fi
done < "${APP_ROOT}/env.example"

if grep -q "CHANGE_ME_db_password" "${APP_ROOT}/.env"; then
  sed -i "s/CHANGE_ME_db_password/$(openssl rand -hex 16)/" "${APP_ROOT}/.env"
fi
if grep -q "CHANGE_ME_use_openssl_rand_hex_32" "${APP_ROOT}/.env"; then
  sed -i "s/CHANGE_ME_use_openssl_rand_hex_32/$(openssl rand -hex 32)/" "${APP_ROOT}/.env"
fi

chmod +x "${APP_ROOT}/deploy.sh" 2>/dev/null || true

echo "Env ready at ${APP_ROOT}/.env"
echo "Swagger: http://4.168.192.169:5000/swagger-ui.html"
