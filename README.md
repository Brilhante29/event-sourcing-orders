# #14 event-sourcing-orders

**Proves:** event sourcing e CQRS.

**Benchmark:** 776 events/s (90k events, 10k orders)

**Stack:** java21, spring-boot, postgresql, redpanda, docker

## Run

```bash
docker build -t event-sourcing-orders .
docker run --rm event-sourcing-orders
```

## Test

```bash
docker run --rm -v $(pwd):/app -w /app gradle:8.12-jdk21 ./gradlew test --no-daemon
```

## Benchmark

```bash
docker run --rm event-sourcing-orders
```

| Metric | Value | Unit |
|---|---|---:|
| events_per_second | 776 | events/s |

## Architecture

Event sourcing with CQRS. Commands produce events stored in an append-only
in-memory event store. A read-model projection rebuilds order state by
replaying events. Domain, application, and infrastructure layers are separated.

- `domain/` — sealed event interface, aggregate root, commands, domain service
- `application/` — event store, CQRS projection, REST controller
- `benchmark/` — throughput measurement (create N orders, apply M events)
- `infrastructure/` — JSON event serializer

## References

See REFERENCES.md.
