param(
  [int]$Orders = 250,
  [int]$WarmupOrders = 25,
  [int]$Repetitions = 3
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
  $changes = @(git status --porcelain)
  if ($changes.Count -gt 0) {
    throw "Benchmark provenance requires a clean Git worktree. Commit implementation changes first."
  }
  $env:SOURCE_COMMIT = (git rev-parse HEAD).Trim()
  $env:CLEAN_TREE = "true"
  $env:BENCHMARK_ORDERS = $Orders.ToString()
  $env:BENCHMARK_WARMUP_ORDERS = $WarmupOrders.ToString()
  $env:BENCHMARK_REPETITIONS = $Repetitions.ToString()
  $env:BENCHMARK_PRODUCER = if ($env:GITHUB_ACTIONS -eq "true") { "github-actions" } else { "local" }

  docker compose build benchmark
  if ($LASTEXITCODE -ne 0) { throw "docker compose build failed" }
  $env:IMAGE_DIGEST = (docker image inspect event-sourcing-orders:benchmark --format "{{.Id}}").Trim()
  docker compose up --no-build --abort-on-container-exit --exit-code-from benchmark benchmark
  if ($LASTEXITCODE -ne 0) { throw "benchmark failed" }
} finally {
  docker compose down --volumes --remove-orphans
  Pop-Location
}
