# Architecture Decision: Hexagonal CQRS Event Sourcing

## Status

Accepted.

## Context

The claim requires restart durability, ordered aggregate histories, conflict
detection, replay, a separate read model, and integration with an independently
deployed payments service. The previous in-memory implementation could not
prove any restart or persistence behavior.

## Decision

Use event sourcing as the write model, CQRS as the persistent read model, and
hexagonal ports around PostgreSQL and payment authorization.

```text
REST -> OrderService -> OrderEventRepository -> JDBC/PostgreSQL
                    -> PaymentAuthorizer -> local | HTTP #11
event history -> CqrsProjection -> OrderProjectionStore -> PostgreSQL
```

The write and read models use separate tables. They share one orders database
because projection rebuild is local to this bounded context. The payments
service owns a different database and is reached only by HTTP.

## Dependency Rule

- Domain: commands, sealed events, aggregate, event-store port, use-case service.
- Application: read model, projection use case, controller, payment/projection ports.
- Infrastructure: JDBC, Flyway, PostgreSQL, Spring conditions, and HTTP client.
- Dependencies point inward; adapters are replaceable at composition time.

## Rejected Alternatives

| Alternative | Reason |
|---|---|
| In-memory event list | Loses history on restart and makes replay durability untestable. |
| CRUD order table as source of truth | Erases the immutable transition history central to the claim. |
| Kafka/Redpanda event log | Adds a broker without a publication or consumer claim; #20 owns delivery. |
| Shared payments/orders database | Couples bounded contexts and invalidates independent deployment. |
| JPA/Hibernate | Hides append SQL and optimistic sequence enforcement that this repo must prove. |

## Consequences

- PostgreSQL is required because durability is intentional, not incidental.
- A payment can succeed before the authorization event append. The stable
  idempotency key supports a retry, but no cross-service atomicity is claimed.
- Projection rebuild is full-table and single-process; incremental consumers are future work only if benchmark evidence requires them.
