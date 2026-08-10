# Spec: #14 event-sourcing-orders

## Public Claim

PostgreSQL keeps an append-only order history across restarts, rejects stale
stream versions, and rebuilds a persistent CQRS projection.

## Functional Scope

- Create, authorize payment, ship, cancel, and deliver an order through commands.
- Store every accepted transition as an immutable, versioned event.
- Rebuild the complete read model from PostgreSQL history.
- Use a local `PaymentAuthorizer` by default or call #11 through HTTP.
- Export the versioned commerce event envelope under `contracts/`.

## Invariants

- `(aggregate_id, sequence)` is unique and sequence starts at one.
- Append succeeds only when the current stream version equals `expectedVersion`.
- `OrderPaymentAuthorized` is appended only after payment returns HTTP 200/201.
- Payment idempotency key is `order:{orderId}:authorize:v1`.
- Orders and payments never share a database.
- Projection replacement and checkpoint update are one PostgreSQL transaction.

## Out Of Scope

- Kafka, Redpanda, RabbitMQ, event publication, inventory, refunds, authentication, and multi-region failover.
- Distributed exactly-once claims between payments and orders.

## Definition Of Done

- [x] Java 21 domain has no Spring, JDBC, Flyway, or HTTP imports.
- [x] Flyway creates the event store, unique stream sequence, projection, and checkpoint.
- [x] Real PostgreSQL tests cover restart, rebuild, and stale-version conflict.
- [x] Local and HTTP payment adapters satisfy the same output port.
- [x] Docker Compose runs API, integration tests, and benchmark without secrets.
- [x] Benchmark V2 has warm-up, three repetitions, throughput, rebuild latency, and provenance.
