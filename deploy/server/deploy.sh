#!/usr/bin/env bash
# Pull a pre-built image and roll out on K3s. No source code or Maven on the server.
# Called by GitHub Actions or manually:
#   IMAGE=ghcr.io/user/msp-api:abc123 bash /opt/apps/msp-api/deploy.sh
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/apps/msp-api}"
NAMESPACE="${NAMESPACE:-msp}"
DEPLOYMENT="${DEPLOYMENT:-msp-backend}"
CONTAINER="${CONTAINER:-msp-backend}"
IMAGE="${IMAGE:?Set IMAGE to the full image reference, e.g. ghcr.io/user/msp-api:sha}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-300s}"

dump_rollout_debug() {
  echo ""
  echo "==> Rollout diagnostics"
  kubectl get pods -n "${NAMESPACE}" -o wide || true
  kubectl describe deployment/"${DEPLOYMENT}" -n "${NAMESPACE}" | tail -n 40 || true
  kubectl get events -n "${NAMESPACE}" --sort-by=.lastTimestamp | tail -n 20 || true
  kubectl logs "deployment/${DEPLOYMENT}" -n "${NAMESPACE}" --tail=80 || true
}

pull_image() {
  echo "==> Pre-pulling image: ${IMAGE}"
  if command -v k3s >/dev/null 2>&1; then
    k3s ctr images pull "${IMAGE}" || true
  elif command -v crictl >/dev/null 2>&1; then
    crictl pull "${IMAGE}" || true
  elif command -v docker >/dev/null 2>&1; then
    docker pull "${IMAGE}" || true
  fi
}

cd "${APP_ROOT}"

echo "==> Ensuring namespace exists"
kubectl apply -f "${APP_ROOT}/k8s/namespace.yaml"

if [[ -f "${APP_ROOT}/secrets/secrets.yaml" ]]; then
  echo "==> Applying Kubernetes secrets"
  kubectl apply -f "${APP_ROOT}/secrets/secrets.yaml"
fi

if [[ -f "${APP_ROOT}/k8s/ghcr-pull.secret.yaml" ]]; then
  echo "==> Applying registry pull secret"
  kubectl apply -f "${APP_ROOT}/k8s/ghcr-pull.secret.yaml"
fi

echo "==> Applying manifests"
kubectl kustomize "${APP_ROOT}/k8s" \
  | sed "s|IMAGE_PLACEHOLDER|${IMAGE}|g" \
  | kubectl apply -f -

echo "==> Waiting for infra (postgres, redis)"
kubectl wait --for=condition=available deployment/msp-postgres deployment/redis \
  -n "${NAMESPACE}" --timeout=120s

pull_image

echo "==> Rolling out image: ${IMAGE}"
if ! kubectl rollout status "deployment/${DEPLOYMENT}" -n "${NAMESPACE}" --timeout="${ROLLOUT_TIMEOUT}"; then
  dump_rollout_debug
  exit 1
fi

echo ""
echo "Done. Pods:"
kubectl get pods,svc,ingress -n "${NAMESPACE}"
echo ""
echo "Swagger: http://msp.185.181.10.165.nip.io/swagger-ui.html"
echo "Health:  http://msp.185.181.10.165.nip.io/actuator/health"
