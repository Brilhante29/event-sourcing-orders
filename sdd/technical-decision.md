# Technical Decision

## Selected Stack

- Java 21 records, sealed interfaces, and pattern matching.
- Spring Boot 3.4 with Spring MVC and Spring JDBC.
- PostgreSQL 16.4 and Flyway 10.20.
- Gradle 8.12 Kotlin DSL and version catalog.
- Docker Compose for API, PostgreSQL, tests, and benchmark.

Java remains intentional in #14: the macro already proves Kotlin/Spring in #11,
and the HTTP edge demonstrates Java/Kotlin JVM interoperability rather than
duplicating one language everywhere.

## Persistence

`order_events` has a global position, event UUID, aggregate UUID, positive
stream sequence, event type/version, correlation/causation IDs, timestamp, and
JSONB payload. `UNIQUE (aggregate_id, sequence)` is the final concurrency guard.
The append SQL also compares current stream version with `expectedVersion`.

`order_projection` is derived state. `projection_checkpoint` records how many
events produced it. Rebuild deletes/inserts the read model and updates the
checkpoint inside one transaction.

## Payment Port

`PaymentAuthorizer` accepts order ID, amount in minor units, and currency.
The local adapter deterministically derives a payment UUID from the required
idempotency key. The HTTP adapter sends the #11 snake-case contract and accepts
only 200/201 before the service appends `OrderPaymentAuthorized`.

## Principles

- SRP: event storage, projection, payment integration, REST, and benchmarking are separate components.
- OCP/LSP: local and HTTP payment adapters substitute behind one port.
- ISP: three focused output ports replace one infrastructure-heavy service interface.
- DIP: use cases depend on ports, never JDBC or HTTP clients.
- KISS: one orders service and PostgreSQL; no broker, ORM, cloud SDK, or generic framework.
- DRY: `Order.apply` is the sole state reconstruction path for commands and projection replay.
- YAGNI: no event publication until #20 supplies a measured delivery requirement.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/orders` | Orders database endpoint |
| `PAYMENTS_ADAPTER` | `local` | Select local or HTTP adapter |
| `PAYMENTS_BASE_URL` | `http://localhost:8081` | #11 endpoint base URL |

No secret or paid cloud account is required by the default path.
