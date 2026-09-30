#!/usr/bin/env sh
set -eu

BASE_URL="${1:-http://localhost:8080}"
curl --fail --silent "$BASE_URL/api/v1/system/liveness" >/dev/null
curl --fail --silent "$BASE_URL/api/v1/system/readiness" >/dev/null
curl --fail --silent "$BASE_URL/api/v1/setup/status" >/dev/null
echo "backend packaged smoke check passed: $BASE_URL"
