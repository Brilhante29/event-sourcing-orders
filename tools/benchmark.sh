#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Benchmark provenance requires a clean Git worktree. Commit implementation changes first." >&2
  exit 1
fi

export SOURCE_COMMIT="$(git rev-parse HEAD)"
export CLEAN_TREE=true
export BENCHMARK_ORDERS="${BENCHMARK_ORDERS:-250}"
export BENCHMARK_WARMUP_ORDERS="${BENCHMARK_WARMUP_ORDERS:-25}"
export BENCHMARK_REPETITIONS="${BENCHMARK_REPETITIONS:-3}"
export BENCHMARK_PRODUCER="${BENCHMARK_PRODUCER:-local}"

cleanup() {
  docker compose down --volumes --remove-orphans
}
trap cleanup EXIT

docker compose build benchmark
export IMAGE_DIGEST="$(docker image inspect event-sourcing-orders:benchmark --format '{{.Id}}')"
docker compose up --no-build --abort-on-container-exit --exit-code-from benchmark benchmark
