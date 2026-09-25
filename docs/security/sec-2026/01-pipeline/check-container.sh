#!/usr/bin/env bash
# docs/security/sec-2026/01-pipeline/check-container.sh
# Smoke test for container gate, safe save/load transfer, and image ID verification.
# ponytail: bash-only smoke check using local docker/trivy cli; add cosign attestation check when OIDC available
set -euo pipefail

IMAGE_REF="${1:-carsync-api:sec-2026}"
OUT_DIR="${2:-$(mktemp -d -t container-smoke-XXXXXX)}"
mkdir -p "$OUT_DIR"

echo "=== Container Security Pipeline Smoke Test ==="
echo "Target Image Ref: $IMAGE_REF"
echo "Output Directory: $OUT_DIR"

# 1. Assert rejection of divergent image ID
echo -n "[TEST 1] Assert rejection of divergent image ID... "
verify_id() { [ -n "${2:-}" ] && [ "$1" = "$2" ]; }

if verify_id "sha256:expected" "sha256:divergent" || ! verify_id "sha256:same" "sha256:same"; then
  echo "FAIL" >&2
  exit 1
fi
echo "PASS"

# 2. Precondition: Docker CLI and daemon access
echo -n "[CHECK] Docker daemon accessibility... "
if ! command -v docker >/dev/null 2>&1; then
  echo "BLOCKED: docker CLI missing" | tee "$OUT_DIR/blocked.log"
  exit 2
fi
if ! docker info > "$OUT_DIR/docker-info.log" 2>&1; then
  echo "BLOCKED: Docker socket inaccessible (permission denied or daemon stopped)" | tee "$OUT_DIR/blocked.log"
  exit 2
fi
echo "AVAILABLE"

# 3. Negative test: non-existent image reference must fail inspect
echo -n "[TEST 2] Negative test with non-existent image ref... "
if docker image inspect "nonexistent-image:invalid-tag" > "$OUT_DIR/negative-inspect.log" 2>&1; then
  echo "FAIL: inspect succeeded on missing image" >&2
  exit 1
fi
echo "PASS"

# 4. Precondition: Target image exists
echo -n "[CHECK] Local target image existence... "
if ! docker image inspect "$IMAGE_REF" > "$OUT_DIR/target-inspect.log" 2>&1; then
  echo "BLOCKED: image '$IMAGE_REF' not found locally" | tee "$OUT_DIR/blocked.log"
  exit 2
fi
LOCAL_ID=$(docker image inspect --format='{{.Id}}' "$IMAGE_REF")
echo "$LOCAL_ID" > "$OUT_DIR/image-id.txt"
echo "FOUND ($LOCAL_ID)"

# 5. Trivy scan: propagate status immediately on failure, preserve report
echo -n "[CHECK] Trivy CLI availability... "
if ! command -v trivy >/dev/null 2>&1; then
  echo "BLOCKED: trivy CLI missing" | tee -a "$OUT_DIR/blocked.log"
  exit 2
fi
echo "AVAILABLE"

echo -n "[TEST 3] Running Trivy vulnerability scan... "
set +e
trivy image \
  --severity HIGH,CRITICAL \
  --ignore-unfixed \
  --exit-code 1 \
  --format table \
  --output "$OUT_DIR/trivy-report.txt" \
  "$IMAGE_REF" > "$OUT_DIR/trivy-console.log" 2>&1
TRIVY_STATUS=$?
set -e

if [ "$TRIVY_STATUS" -ne 0 ]; then
  echo "GATE FAILED (exit code $TRIVY_STATUS; report in $OUT_DIR/trivy-report.txt)" >&2
  exit "$TRIVY_STATUS"
fi
echo "PASS (gate HIGH/CRITICAL with fixes available; unfixed findings excluded)"

# 6. Save/load transfer & ID equality test (no deleting existing images)
echo -n "[TEST 4] Docker save/load and ID verification... "
TAR_FILE="$OUT_DIR/image.tar"
docker save -o "$TAR_FILE" "$IMAGE_REF"
docker load -i "$TAR_FILE" > "$OUT_DIR/docker-load.log" 2>&1
LOADED_ID=$(docker image inspect --format='{{.Id}}' "$IMAGE_REF")

if ! verify_id "$LOCAL_ID" "$LOADED_ID"; then
  echo "FAIL: loaded ID ($LOADED_ID) does not match original ($LOCAL_ID)" >&2
  exit 1
fi
echo "PASS"

echo "=== All Checks Passed ==="
