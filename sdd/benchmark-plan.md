# Benchmark Plan: event-sourcing-orders

## Hypothesis

event sourcing e CQRS, measured by events_per_second.

## Command

```bash
docker run --rm event-sourcing-orders
```

## Environment

- OS: Linux (Docker container, eclipse-temurin:21-jdk)
- CPU: host-dependent
- RAM: host-dependent
- Container runtime: Docker
- Java: 21.0.11
- Date: 2026-07-20

## Inputs

- fixture: synthetic — 10,000 orders
- dataset size: 90,000 events (9 events per order)
- repetitions: 1
- warmup: none (first run includes JIT compilation)

## Metrics

| Metric | Unit | Source | Why it matters |
|---|---|---|---:|
| events_per_second | events/s | BenchmarkRunner | proves the repo claim |

## Result schema

Output must be JSON and include project, metric, value, unit, timestamp, environment, and command.

## Post angle

#14 event-sourcing-orders: 776 events/s — event sourcing e CQRS with Java 21 sealed types and Spring Boot.
