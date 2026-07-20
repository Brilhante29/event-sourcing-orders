# Architecture Decision

## Status

Accepted

## Context

Project: event-sourcing-orders
Claim: event sourcing e CQRS
Benchmark: events_per_second

Problem forces:

- Domain complexity: low
- Integration pressure: low
- UI state complexity: none
- Data/ML reproducibility: low
- Auditability/event history: high
- Throughput/async pressure: low
- Independent deployability need: low

## Decision

Chosen architecture: CQRS + Event Sourcing

Reason:

Commands produce events appended to an immutable event store. A separate
read-model projection replays events to rebuild current order state. This
directly proves the claim: every state change is recorded as an event, and
the read model is derived independently from the write model.

Dependency rule:

domain/application do not depend on infra; adapters depend inward through ports.

## Rejected Alternatives

| Alternative | Why rejected |
|---|---|
| Layered (controller -> service -> repository) | Doesn't demonstrate event sourcing — state updates would be in-place mutations |
| Event-driven with Kafka | Adds infrastructure without improving the benchmark claim for a single-JVM demo |

## Folder Layout

```
src/
  main/java/com/portfolio/eventsourcing/
    EventsourcingApplication.java
    domain/       — sealed OrderEvent, Order aggregate, OrderCommand, OrderService
    application/  — EventStore, CqrsProjection, OrderController
    benchmark/    — BenchmarkRunner
    infrastructure/ — EventSerializer
test/
  java/com/portfolio/eventsourcing/
    domain/OrderTest.java
    application/EventStoreTest.java
    benchmark/BenchmarkRunnerTest.java
benchmarks/results/
```

## Testing Strategy

- Unit tests: domain aggregate rebuild, event store append/read, benchmark JSON output
- Integration tests: (in-memory, no Spring context needed)
- Benchmark: RunBenchmarkRunner with 10k orders, 5 events each, measure events/second

## Consequences

Positive:

- Event sourcing and CQRS patterns are directly visible in the code
- Domain has zero framework imports
- In-memory store keeps the demo self-contained

Tradeoffs:

- No persistence across restarts (intentional — keeps benchmark simple)
- No distributed messaging (not needed for single-JVM throughput measurement)

Migration path:

- Replace EventStore with PostgreSQL-backed or Redpanda-backed adapter
- Add outbox pattern for production-grade event publishing
