# Reuse Improvement Review

Project: `14 - event-sourcing-orders`

## Review Points

- [x] after scaffold
- [x] after architecture decision
- [x] after first working slice
- [x] after benchmark result
- [x] before publication
- [ ] after CI failure, if applicable

## Findings

| Finding | Classification | Kit Area | Action | Status |
|---|---|---|---|---|
| Sealed interface for events maps directly to Java 21 sealed types | `patch_now` | `language-profiles` | Add sealed interface pattern to Java language profile | backlog |
| Gradle version catalog template reusable across projects | `patch_now` | `templates` | Extract gradle/libs.versions.toml as template | backlog |
| Spring Boot layered Dockerfile pattern | `patch_now` | `templates` | Add multi-stage Dockerfile with jar extraction to templates | backlog |

## Patch Now Decisions

- Sealed interface pattern for domain events documented in portfolio-reuse-kit/language-profiles/java.md

## Backlog Decisions

- Gradle version catalog template
- Spring Boot layered Dockerfile with jar extraction

## Rejected Improvements

- None

## Final Gate

- [x] Reusable improvements were patched or recorded.
- [x] Project-specific implementation was not moved into the kit.
- [x] Validation reflects any repeated mistake discovered during the project.
