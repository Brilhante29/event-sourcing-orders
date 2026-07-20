# Agent Handoff

Project: `14 - event-sourcing-orders`

## Principal Agent Summary

- Objective: Implement event sourcing e CQRS order demo to benchmarked status
- Portfolio program: backend-reliability-platform
- Public proof claim: event sourcing e CQRS — 776 events/s
- Primary benchmark: events_per_second
- Default runnable path: `docker run --rm event-sourcing-orders`

## Subagent Decisions

| Role | Decision | Evidence Path | Status |
|---|---|---|---|
| `program-planner` | Backend reliability platform — event sourcing pattern | `project.yaml` | done |
| `architecture-selector` | CQRS + Event Sourcing with Java 21 sealed types | `sdd/architecture-decision.md` | done |
| `engineering-principles-reviewer` | Domain zero framework imports; DIP via repository interface | `project.yaml`, `sdd/technical-decision.md` | done |
| `stack-decision-agent` | Java 21 + Spring Boot 3.4 + Gradle 8.12 | `project.yaml`, `sdd/technical-decision.md` | done |
| `api-style-agent` | REST HTTP — 6 endpoints for order lifecycle | OrderController | done |
| `cloud-local-first-agent` | Docker only | Dockerfile | done |
| `messaging-agent` | None (in-memory) | `sdd/technical-decision.md` | done |
| `language-profile-agent` | Java 21, sealed interfaces, records | src layout, tests | done |
| `benchmark-harness-agent` | `docker run --rm event-sourcing-orders` | `benchmarks/results/benchmark-20260720.json` | done |
| `design-system-agent` | Minimal — README with benchmark card | README.md | done |
| `security-reuse-reviewer` | No secrets, no paid credentials | `REFERENCES.md`, release checklist | done |
| `release-ci-publisher` | GitHub Actions CI workflow | `.github/workflows/ci.yml` | done |

## Local-First Runtime

- Docker command: `docker run --rm event-sourcing-orders`
- Local services: none
- Kumo services: none
- Real cloud adapter target: none
- Config switch: CLOUD_PROVIDER=docker
- Default path requires paid secret: no

## Architecture Boundaries

- Domain boundaries: OrderEvent, OrderCommand, Order, OrderService, OrderEventRepository
- Use-case boundaries: OrderController handles REST -> delegates to OrderService
- Ports: OrderEventRepository (interface in domain)
- Adapters: EventStore (implements OrderEventRepository)
- Dependency direction rule: domain <- application <- infrastructure

## Benchmark Handoff

- Metric: events_per_second
- Unit: events/s
- Higher or lower is better: higher
- Command: `docker run --rm event-sourcing-orders`
- Result path: `benchmarks/results/benchmark-20260720.json`
- Dataset or fixture: synthetic — 10,000 orders, 90,000 events

## Open Risks

- None

## Publication Gates

- [x] Docker path works
- [x] benchmark result exists
- [x] README starts with number, claim, and benchmark
- [x] references are documented
- [x] no secret in files or git remote
- [x] validation passes
