# Release Checklist

- [x] Java 21 main and test sources compile in Docker.
- [x] Unit and PostgreSQL integration tests pass.
- [x] Restart, projection rebuild, and stale-version conflict are tested.
- [x] Local payment adapter is the no-secret default.
- [x] HTTP adapter sends the exact #11 idempotency and payload contract.
- [x] Orders and payments do not share a database.
- [x] Benchmark has warm-up, three repetitions, and V2 output.
- [x] README opens with project number, claim, and numeric result.
- [x] Messaging is explicitly `none`; no Kafka/Redpanda claim remains.
- [ ] Clean-tree benchmark provenance and final validation recorded.
