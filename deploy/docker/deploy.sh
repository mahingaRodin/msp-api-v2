#!/usr/bin/env bash
# Pull a pre-built GHCR image and start MSP via Docker Compose.
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/apps/msp-api}"
IMAGE="${IMAGE:-}"

cd "${APP_ROOT}"

if [[ ! -f docker-compose.yml ]]; then
  echo "Missing ${APP_ROOT}/docker-compose.yml"
  exit 1
fi

if [[ ! -x "${APP_ROOT}/bootstrap.sh" ]]; then
  chmod +x "${APP_ROOT}/bootstrap.sh" 2>/dev/null || true
fi
bash "${APP_ROOT}/bootstrap.sh"

if [[ -n "${IMAGE}" ]]; then
  if grep -q '^IMAGE=' .env; then
    sed -i "s|^IMAGE=.*|IMAGE=${IMAGE}|" .env
  else
    echo "IMAGE=${IMAGE}" >> .env
  fi
fi

if grep -qE 'CHANGE_ME_|DB_USERNAME=$|DB_PASSWORD=$|JWT_SECRET=$' .env; then
  echo "Invalid ${APP_ROOT}/.env — secrets are still placeholders or empty"
  exit 1
fi

if [[ -f "${APP_ROOT}/ghcr.token" ]]; then
  echo "==> Logging in to GHCR"
  docker login ghcr.io -u mahingarodin --password-stdin < "${APP_ROOT}/ghcr.token"
fi

set -a
# shellcheck disable=SC1091
source "${APP_ROOT}/.env"
set +a

echo "==> Starting API stack with image ${IMAGE:-from .env}"
docker compose --env-file "${APP_ROOT}/.env" pull postgres redis api
docker compose --env-file "${APP_ROOT}/.env" up -d --remove-orphans postgres redis api

echo "==> Waiting for API health"
for i in $(seq 1 36); do
  if curl -sf --connect-timeout 2 http://127.0.0.1:5000/actuator/health >/dev/null; then
    echo "Health check OK"
    docker compose --env-file "${APP_ROOT}/.env" ps
    echo "API:     http://4.168.192.169:5000/swagger-ui.html"
    echo "UI:      http://4.168.192.169/  (after frontend image is deployed)"
    exit 0
  fi
  sleep 5
done

echo "API did not become healthy in time"
docker compose --env-file "${APP_ROOT}/.env" ps
docker compose --env-file "${APP_ROOT}/.env" logs --tail=80 postgres api
exit 1
