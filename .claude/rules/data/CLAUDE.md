# Data Module (Data Layer)

Scope: the `data` package of each `feature:*` module, and `core:network` / `core:database`. Merged with root `CLAUDE.md`.

## Rules
- Repository implementations live here; their interfaces live in the feature's `domain` package, or in `core:domain` when another feature uses them.
- Remote sources: Ktor + kotlinx.serialization, through the shared client in `core:network`.
- Local sources: Room. All DAOs return `Flow` for observable queries.
- Map DTOs → domain models explicitly with a `toDomain()` extension function; never expose DTOs outside this module.
- All network calls wrapped in a `Result`/`ApiResult` type — no raw exceptions escaping to the domain layer.
- Cache-then-network or network-then-cache strategy must be explicit per repository, not implicit.
- Base URLs come from `ApiConfig` / `ApiEnvironment` in `core:network`, never hardcoded at a call site; API keys never go in source.

## Testing
- Repositories: unit test with fake remote/local data sources.
- DAOs: test against the real schema in `core:database`'s `iosTest` (`commonTest` has no SQLite driver).
