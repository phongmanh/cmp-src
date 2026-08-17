# Data Module (Data Layer)

Scope: everything under `data/`. Merged with root `CLAUDE.md`.

## Rules
- Repository implementations live here; interfaces live in `domain/`.
- Remote sources: Retrofit + kotlinx.serialization (or Moshi — match existing project choice, do not mix).
- Local sources: Room. All DAOs return `Flow` for observable queries.
- Map DTOs → domain models explicitly with a `toDomain()` extension function; never expose DTOs outside this module.
- All network calls wrapped in a `Result`/`ApiResult` type — no raw exceptions escaping to the domain layer.
- Cache-then-network or network-then-cache strategy must be explicit per repository, not implicit.
- API keys and base URLs come from `BuildConfig` / secrets manager — never hardcoded.

## Testing
- Repositories: unit test with fake remote/local data sources.
- DAOs: instrumentation test against an in-memory Room database.
