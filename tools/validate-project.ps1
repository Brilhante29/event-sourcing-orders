param(
  [switch]$SkipDocker
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure([string]$Message) {
  $script:failures.Add($Message)
}

function Require-File([string]$RelativePath) {
  if (-not (Test-Path -LiteralPath (Join-Path $root $RelativePath) -PathType Leaf)) {
    Add-Failure "Missing file: $RelativePath"
  }
}

function Invoke-Checked([string]$Label, [scriptblock]$Command) {
  & $Command
  if ($LASTEXITCODE -ne 0) {
    Add-Failure "$Label failed with exit code $LASTEXITCODE"
  }
  $global:LASTEXITCODE = 0
}

$requiredFiles = @(
  "README.md",
  "project.yaml",
  "REFERENCES.md",
  "AGENTS.md",
  "compose.yaml",
  "contracts/commerce-event-v1.schema.json",
  ".portfolio/contracts/benchmark-result-v2.schema.json",
  "benchmarks/results/event-sourcing-orders-v2.json",
  "sdd/spec.md",
  "sdd/benchmark-plan.md",
  "sdd/architecture-decision.md",
  "sdd/technical-decision.md",
  "sdd/agent-handoff.md",
  "sdd/reuse-improvement-review.md"
)
foreach ($file in $requiredFiles) { Require-File $file }

$readmePath = Join-Path $root "README.md"
if (Test-Path $readmePath) {
  $readme = Get-Content -Raw $readmePath
  if ($readme -notmatch "(?m)^# #14 event-sourcing-orders: [0-9.]+ events/s and [0-9.]+ ms rebuild$") {
    Add-Failure "README must open with project number and both benchmark numbers"
  }
}

$projectPath = Join-Path $root "project.yaml"
if (Test-Path $projectPath) {
  $project = Get-Content -Raw $projectPath
  foreach ($pattern in @(
    "(?m)^  messaging: none$",
    "(?m)^    engine: postgresql$",
    "(?m)^  primary: java$",
    "(?m)^  result_path: benchmarks/results/event-sourcing-orders-v2.json$"
  )) {
    if ($project -notmatch $pattern) {
      Add-Failure "project.yaml is missing required decision: $pattern"
    }
  }
}

$reuseReviewPath = Join-Path $root "sdd/reuse-improvement-review.md"
if (Test-Path $reuseReviewPath) {
  $reuseReview = Get-Content -Raw $reuseReviewPath
  foreach ($line in @(
    "- [x] Reusable improvements were patched or recorded.",
    "- [x] Project-specific implementation was not moved into the kit.",
    "- [x] Validation reflects the repeated V2 producer and artifact-mount mistakes."
  )) {
    if (-not $reuseReview.Contains($line)) {
      Add-Failure "Reuse review final gate is incomplete: $line"
    }
  }
}

Push-Location $root
try {
  foreach ($schema in @(
    "contracts/commerce-event-v1.schema.json",
    ".portfolio/contracts/benchmark-result-v2.schema.json"
  )) {
    if (Test-Path $schema) {
      Invoke-Checked "JSON syntax: $schema" { python -m json.tool $schema | Out-Null }
    }
  }

  $resultPath = "benchmarks/results/event-sourcing-orders-v2.json"
  if (Test-Path $resultPath) {
    Invoke-Checked "benchmark V2 schema" {
      python -m jsonschema -i $resultPath ".portfolio/contracts/benchmark-result-v2.schema.json"
    }
    try {
      $result = Get-Content -Raw $resultPath | ConvertFrom-Json
      if ($result.schema_version -ne 2) { Add-Failure "Benchmark schema_version must be 2" }
      if ($result.execution.repeat -lt 3) { Add-Failure "Benchmark requires at least 3 repetitions" }
      foreach ($metric in $result.metrics) {
        if (@($metric.samples).Count -lt 3) {
          Add-Failure "Metric $($metric.name) requires at least 3 samples"
        }
        if ($metric.failures -ne 0) { Add-Failure "Metric $($metric.name) reports failures" }
      }
      if ($result.provenance.source_commit -eq ("0" * 40)) {
        Add-Failure "Benchmark source_commit is a placeholder"
      }
      if ($result.provenance.image_digest -eq ("sha256:" + ("0" * 64))) {
        Add-Failure "Benchmark image_digest is a placeholder"
      }
      if ($result.provenance.clean_tree -ne $true) {
        Add-Failure "Benchmark must come from a clean tree"
      }
    } catch {
      Add-Failure "Benchmark V2 could not be inspected: $($_.Exception.Message)"
    }
  }

  $legacy = ("ro" + "che" + "do")
  $searchFiles = Get-ChildItem -Path $root -Recurse -File | Where-Object {
    $normalized = $_.FullName -replace "\\", "/"
    $normalized -notmatch "/.git/" -and
    $normalized -notmatch "/.gradle/" -and
    $_.Extension -in @(".md", ".yaml", ".yml", ".json", ".ps1", ".sh", ".java", ".kt")
  }
  $forbidden = Select-String -Path $searchFiles.FullName -Pattern @(
    $legacy,
    ($legacy.Substring(0,1).ToUpper() + $legacy.Substring(1))
  ) -SimpleMatch -ErrorAction SilentlyContinue
  if ($forbidden) { Add-Failure "Forbidden legacy project nickname found" }

  if (-not $SkipDocker) {
    Invoke-Checked "docker build" { docker compose build benchmark | Out-Null }
  }
} finally {
  Pop-Location
}

if ($failures.Count -gt 0) {
  Write-Host "portfolio project validation failed with $($failures.Count) issue(s):"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
    if ($env:GITHUB_ACTIONS -eq "true") { Write-Host "::error::$failure" }
  }
  exit 1
}

Write-Host "portfolio project validation passed"
