# Technical Decision

## Status

Accepted

## Decision Type

stack

## Context

Project: event-sourcing-orders
Problem: Prove event sourcing e CQRS with a measurable throughput benchmark
Portfolio program: backend-reliability-platform
Public signal: Event sourcing with Java 21 sealed types + Spring Boot
Benchmark: events_per_second

## Selected Option

Selected: Java 21 + Spring Boot 3.4 + Gradle 8.12

Reason:

Java 21 sealed interfaces and records map directly to the event-sourcing domain
(sealed event hierarchy, immutable records for commands and events). Spring Boot
provides DI and REST without overhead. Gradle version catalog keeps dependencies
manageable.

## Decision Brain Fields

- Stack profile: spring-kotlin-backend
- API style: rest-http
- Messaging: none
- Cloud mode: none
- Database/runtime: in-memory / docker
- Library policy: Jackson for event serialization; Spring Boot for DI and REST wiring

## Engineering Principles

Coupling boundary:

Domain/use cases must not depend on framework, DB, broker, cloud SDK, transport, or UI.

SOLID application:

- SRP: Each sealed record captures one concern (event type, command type, aggregate)
- OCP: New event types extend the sealed interface without changing existing handlers
- LSP: EventStore (fake) and any future adapter are interchangeable via OrderEventRepository
- ISP: OrderEventRepository has 3 small methods; sealed interface is split by event kind
- DIP: OrderService depends on OrderEventRepository interface, not on EventStore

Simplicity:

- KISS: In-memory event store, no DB, no broker — the simplest setup that proves the claim
- YAGNI: No persistence, no outbox, no distributed tracing — not needed for throughput benchmark
- DRY: Event application logic lives in Order.apply() — single point for state rebuild

Testability evidence:

- OrderTest creates events in-memory and rebuilds the aggregate without any Spring context
- EventStoreTest tests the repository contract directly
- BenchmarkRunnerTest captures stdout and asserts JSON schema

## Rejected Options

| Option | Why rejected |
|---|---|
| Kotlin | Java 21 sealed types/records suffice; team uses Java |
| Maven | Gradle Kotlin DSL gives more concise build config |

## API Contract

Contract artifact: OpenAPI (implicit — 6 REST endpoints on /api/orders)

## Cloud Local-First

Local provider: docker

Real provider target: none

Config switch:

```
CLOUD_PROVIDER=docker
```

Unsupported local behaviors: none

## Benchmark Impact

Expected impact: 776 events/s baseline on Docker (Linux, JVM 21)

Validation command:

```bash
docker build -t event-sourcing-orders . && docker run --rm event-sourcing-orders
```

## Operational Cost

- Docker services added: none
- Local demo complexity: low
- Failure case required: no

## Follow-up

- If benchmark drops below 500 events/s, investigate UUID generation or CopyOnWriteArrayList contention
