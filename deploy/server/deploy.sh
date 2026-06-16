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
kubectl apply -k "${APP_ROOT}/k8s"

echo "==> Rolling out image: ${IMAGE}"
kubectl set image "deployment/${DEPLOYMENT}" \
  "${CONTAINER}=${IMAGE}" \
  -n "${NAMESPACE}"

kubectl rollout status "deployment/${DEPLOYMENT}" -n "${NAMESPACE}" --timeout=600s

echo ""
echo "Done. Pods:"
kubectl get pods,svc,ingress -n "${NAMESPACE}"
echo ""
echo "Swagger: http://msp.185.181.10.165.nip.io/swagger-ui.html"
echo "Health:  http://msp.185.181.10.165.nip.io/actuator/health"
