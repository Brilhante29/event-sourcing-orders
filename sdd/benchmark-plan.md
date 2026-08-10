# Benchmark Plan: PostgreSQL Append And CQRS Rebuild

## Hypothesis

The real PostgreSQL implementation can append versioned order events and rebuild
the complete persistent read model with zero count/checkpoint divergence.

## Reproducible Command

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/benchmark.ps1
```

The script refuses a dirty source tree, builds the image, captures its digest,
starts PostgreSQL, executes the workload, writes V2 JSON, and removes containers
and volumes.

## Workload

- Warm-up: 25 orders, excluded from measurements.
- Measured: 250 orders per repetition.
- Events per order: create, payment-authorized, shipped, delivered.
- Repetitions: 3 isolated database resets.
- Concurrency: 1; no parallel throughput claim.
- Fixture: deterministic order UUIDs and product/customer values.

## Metrics

| Metric | Unit | Statistic | Invariant |
|---|---|---|---|
| `append_throughput` | events/s | median of 3 | higher is better |
| `projection_rebuild_latency` | ms | median of 3 | lower is better |
| `replayed_events` | events | each sample | exactly 1,000 |

Each repetition fails when event count, projection checkpoint, or order count
diverges.

## Evidence

- Schema: `.portfolio/contracts/benchmark-result-v2.schema.json`
- Result: `benchmarks/results/event-sourcing-orders-v2.json`
- Comparability key: `event-sourcing-orders:postgres16:orders250:events4:repeat3`
- Artifact digest: digest of the raw sample evidence, avoiding a self-referential file hash.

Final clean-tree baseline: `287.47 events/s` append throughput and `48.31 ms`
projection rebuild. The number is local Docker evidence, not a production
capacity claim.
