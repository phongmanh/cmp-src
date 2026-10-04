---
name: room-migration
description: Changes the Room schema in core:database safely — entity change, version bump, a hand-written Migration, the exported schema JSON, and the migration test on real SQLite. Use whenever an @Entity, a column, a table or an index in core:database changes.
argument-hint: <what changes in the schema>
---

# Change the Room schema

The database keeps users' customer rows, so a schema change must **migrate**, never wipe. There is no blanket destructive fallback: `getRoomDatabase` only recreates a version 1 file, and a missing migration fails at open time.

Files, under `core/database/src/`:

- `commonMain/.../core/database/Database.kt` — `DATABASE_VERSION`, `@Database(entities = …)`
- `commonMain/.../core/database/Migrations.kt` — `MIGRATION_N_M`, `APP_MIGRATIONS`
- `commonMain/.../core/database/entities/`, `.../dto/` — entities and DAOs
- `iosTest/.../core/database/AppDatabaseMigrationTest.kt`, `CustomerDaoTest.kt`

## Steps

1. **Change the entity or DAO.** A new column with a default needs `@ColumnInfo(defaultValue = "…")`, and the migration's SQL `DEFAULT` must match it exactly.

2. **Bump `DATABASE_VERSION`** to N+1 and add a line to the version history in its KDoc.

3. **Generate the schema** — `./gradlew :core:database:compileKotlinIosSimulatorArm64` writes `core/database/schemas/com.liam.cmp_src.core.database.AppDatabase/<N+1>.json`. It's generated: never hand-edit it (a hook blocks it). It's checked in.

4. **Write `MIGRATION_N_<N+1>`** in `Migrations.kt`, below the existing ones:
   - Take the SQL from the new schema JSON (`createSql` for a new table) so a migrated database matches a fresh install byte for byte — Room validates the schema after migrating.
   - KDoc: what changes and what happens to existing rows.
   - **Never edit a migration that has shipped** — a device that already ran it never runs it again. A further change is a new migration.

5. **Register it** — append it to `APP_MIGRATIONS`, which stays declared *below* every migration (top-level properties initialise in file order).

6. **Test it on real SQLite** in `AppDatabaseMigrationTest` (`iosTest`; `commonTest` has no driver):
   - Seed a version N file. Reuse the `seedVersion…Database()` helpers and run the earlier migrations on the connection, as `seedVersion3Database()` does.
   - Open it through `openDatabase()`, then assert existing rows survive and new columns have the values the migration promises.
   - Update `CustomerDaoTest` for any new DAO behaviour.

## Verify

```
./gradlew :core:database:iosSimulatorArm64Test :core:database:testAndroidHostTest
./gradlew :androidApp:assembleDebug
```

Then confirm `git status` shows the new `<N+1>.json`, and commit it with the code.
