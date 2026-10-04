---
name: new-feature-module
description: Scaffolds a new feature:* KMP module — build script, settings include, data/domain/presentation packages, Koin module, resources and tests — and wires it into the shared app module. Use when a feature doesn't belong in any existing feature module.
argument-hint: <feature-name>
---

# Create a feature module

`feature:customers` is the newest complete example — copy its shape, not its content.

**Features never depend on one another.** Anything two features need moves down into `core:*`, and `shared` is the only place features meet.

## Steps

1. **Build script** — `feature/<name>/build.gradle.kts`:
   ```kotlin
   plugins {
       id("cmpsrc.cmp.feature")
   }
   ```
   The convention brings Compose, Koin, `core:domain`, `core:ui`, `core:utils`, the lifecycle APIs and `core:testing` for tests. Add a `kotlin { sourceSets { … } }` block only for extras — e.g. `projects.core.network` for an API, `projects.core.database` for a DAO, and `libs.ktor.client.mock` + `libs.kotlinx.serializationJson` in `commonTest` for API tests. Don't add `androidLibrary { }`: the namespace (`com.liam.cmp_src.feature.<name>`) comes from the path.

2. **Include it** — `include(":feature:<name>")` in `settings.gradle.kts`, next to the other features. The leaf name must be unique across all modules (Kotlin/Native library names are built from it).

3. **Packages** under `src/commonMain/kotlin/com/liam/cmp_src/feature/<name>/`:

   | Package | Holds |
   |---|---|
   | `data/` | `<Name>RepositoryImpl`, `<Name>Mapping.kt` (DTO → domain via `toDomain()`, `ApiError` → domain error) |
   | `data/remote/` | `<Name>Api` — Ktor through the injected `HttpClient`, routes from `ApiRoutes`, returning `ApiResult` |
   | `domain/model/`, `domain/repository/`, `domain/usecase/` | Pure Kotlin: models, the repository interface, one action per `…UseCase` |
   | `<screen>/` (e.g. `list/`, `editor/`) | `<Screen>Screen.kt` (the `…Route` with Koin, plus a Koin-free `…Screen` with previews), `…ViewModel`, `…UiState`, `…Action`, `…Event` |
   | `di/` | `<name>Module` |

4. **Koin module** — `di/<Name>Module.kt` declares `val <name>Module = module { … }`: `single` for the API and repository, `factoryOf` for use cases, `viewModelOf` (or `viewModel { (arg: String) -> … }` for navigation arguments). Its KDoc says what it expects from the app graph (`HttpClient`, `CoroutineDispatcher`, …).

5. **Resources** — `src/commonMain/composeResources/values/strings.xml`, keys prefixed `<name>_`. The generated `Res` is `cmpsrc.feature.<name>.generated.resources.Res` (internal to the module).

6. **Wire it into `shared`:**
   - `implementation(projects.feature.<name>)` in `shared/build.gradle.kts`
   - `<name>Module` in `includes(…)` in `shared/src/commonMain/kotlin/com/liam/cmp_src/di/AppModule.kt`
   - Reach it from the UI: either a new destination (the `add-destination` skill) or a slot filled in `App.kt`, as `HomeRoute`'s `customersTab`/`profileTab` are.

7. **Tests** in `src/commonTest/kotlin/com/liam/cmp_src/feature/<name>/`: a test per use case, ViewModel state transitions against a `Fake<Name>Repository` (with `Dispatchers.setMain(StandardTestDispatcher())`), and the API against Ktor's `MockEngine`. A fake that another module also needs belongs in `core:testing`.

8. **Docs** — add the module to the layout diagram and module list in `CLAUDE.md`.

## Verify

```
./gradlew :feature:<name>:testAndroidHostTest :feature:<name>:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
```
