# Reuse Improvement Review

Project: `#14 event-sourcing-orders`

## Review Points

- [x] after architecture correction
- [x] after PostgreSQL slice
- [x] after payment integration
- [x] after real-database tests
- [x] after benchmark harness
- [x] before publication

## Findings

| Finding | Classification | Kit area | Resolution | Status |
|---|---|---|---|---|
| Copied V2 schema did not make the producer V2-compliant | `backlog` | benchmark harness | Add a generator/validator gate, not only schema sync | recorded |
| Container benchmark could discard its artifact | `backlog` | Docker templates | Standardize host result mount and CI artifact upload | recorded |
| Cross-repo events need one versioned envelope | `backlog` | contracts | Promote `commerce-event-v1.schema.json` after #20 consumes it | recorded |
| Docker-socket test nesting is unnecessarily privileged | `backlog` | JVM templates | Prefer Compose test service plus external DB environment | recorded |
| Stack claims can diverge from dependencies | `backlog` | project validator | Compare `project.yaml` database/messaging claims with build and Compose evidence | recorded |

## Project-Specific Decisions Kept Local

- Order event payloads, projection schema, benchmark fixture, and #11 endpoint mapping.
- PostgreSQL is justified by this repository's durability claim; it is not a mandatory kit default.

## Final Gate

- [x] Reusable improvements were patched or recorded.
- [x] Project-specific implementation was not moved into the kit.
- [x] Validation reflects the repeated V2 producer and artifact-mount mistakes.
