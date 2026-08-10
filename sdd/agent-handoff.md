# Agent Handoff

Project: `#14 event-sourcing-orders`
Branch: `codex/backend-reliability-close`
Push policy: do not push from this implementation task.

## Current State

- PostgreSQL/Flyway event store replaces the removed in-memory store.
- Expected-version append and unique stream sequence are implemented.
- Persistent projection rebuild and checkpoint are implemented transactionally.
- PaymentAuthorizer has local default and optional #11 HTTP adapters.
- Compose integration tests pass against PostgreSQL 16.4.
- V2 harness completed a clean-tree 3-run workload from source commit `ffc61b689791b0b54fa9ca4f6b201dea69370ef0`.

## Accepted Decisions

| Area | Decision | Evidence |
|---|---|---|
| Architecture | Hexagonal CQRS event sourcing | `sdd/architecture-decision.md` |
| Language | Java 21 for interop with #11 Kotlin | `sdd/technical-decision.md` |
| Database | PostgreSQL 16.4 + explicit append SQL | Flyway migration, JDBC adapter |
| Messaging | None; replay reads event store | `project.yaml`, README |
| Payments | HTTP output port, no shared DB | payment adapters and tests |
| Benchmark | V2, warm-up + 3 repetitions | `sdd/benchmark-plan.md` |

## Verification Commands

```powershell
docker compose up --build --abort-on-container-exit --exit-code-from integration-test integration-test
powershell -NoProfile -ExecutionPolicy Bypass -File tools/benchmark.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate-project.ps1 -SkipDocker
```

## Continuation Boundary

The technical close is complete. A publishing agent may push the two local
commits and verify exact-head CI. Do not add a broker or shared payments
database. No internal reasoning is needed; the accepted decisions and evidence
paths above are sufficient.
