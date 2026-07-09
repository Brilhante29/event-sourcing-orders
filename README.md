# #14 event-sourcing-orders

**Status:** scaffold

**Proves:** event sourcing e CQRS.

**Benchmark target:** events_per_second.

**Stack:** java21, spring-boot, postgresql, redpanda, docker.

## Next milestone

Implement the smallest Docker-runnable version and produce the first JSON benchmark under enchmarks/results/.

## Run

`ash
docker build -t event-sourcing-orders .
docker run --rm event-sourcing-orders
`

## Benchmark

`ash
docker run --rm event-sourcing-orders benchmark
`

| Metric | Value | Unit |
|---|---:|---|
| events_per_second | pending | pending |

## Architecture

Defined in sdd/spec.md before implementation.

## References

See REFERENCES.md.