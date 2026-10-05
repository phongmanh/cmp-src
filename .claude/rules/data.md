---
paths:
  - "core/network/**"
  - "core/database/**"
  - "feature/**/data/**"
  - "feature/**/*{RepositoryImpl,Api,Dao}Test.kt"
---

# Data layer

The `data` package of each `feature:*` module, `core:network` and `core:database`.

- Repository implementations live here; their interfaces live in the feature's `domain` package, or in `core:domain` when another feature uses them.
- Remote sources use the shared Ktor client from `core:network`. Every call returns an `ApiResult` through `sendRequest`, so no exception reaches the domain.
- Local sources use Room in `core:database`. A DAO query a screen observes returns a `Flow`.
- DTOs map to domain models through an explicit `toDomain()` (`CustomerMapping.kt` shows the shape) and stay in this layer — except `UserResponse` (`domain.md`).
- Each repository states its strategy — offline-first, cache-then-network, network-only — in its KDoc, as `CustomerRepositoryImpl` does.
- Base URLs come from `ApiConfig` / `ApiEnvironment`, never a call site.

## Room schema changes

The database holds users' rows, so a schema change migrates — it never wipes.

- Bump `DATABASE_VERSION` and add a line to its KDoc history.
- Compile `core:database` to regenerate the schema JSON. It's checked in and never hand-edited.
- Write `MIGRATION_N_<N+1>` in `Migrations.kt` with SQL taken from that JSON, so a migrated database matches a fresh install. A new column's `@ColumnInfo(defaultValue)` must equal the SQL `DEFAULT`.
- Append it to `APP_MIGRATIONS`, which stays declared below every migration — top-level properties initialise in file order.
- Never edit a migration that has shipped; a further change is a new migration.

## Testing

- Repositories: unit tests with fake remote and local sources (`FakeCustomerDao`).
- DAOs and migrations: against real SQLite in `core:database`'s `iosTest` — `commonTest` has no driver. `AppDatabaseMigrationTest` seeds the previous version with the `seedVersion…Database()` helpers and asserts existing rows survive.
