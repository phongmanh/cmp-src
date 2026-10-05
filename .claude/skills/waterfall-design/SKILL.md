---
name: waterfall-design
description: How to write and gate 02-design.md in a waterfall delivery — the template, grounding every decision in the existing code, the changes with a fixed shape (new module, destination, moved resource, schema change), and the gate. Preloaded by waterfall-architect; not for direct use.
user-invocable: false
---

# Phase 2 — Design

Decide *how*, against the code that exists. Read the modules you'll touch before deciding anything, and cite `file:line` for every pattern you reuse. The implementer is a different agent and model: anything you leave open, it must not decide — so leave nothing open.

## Template — `02-design.md`

```markdown
# Design: <task>

## Placement
| Piece | Module / package | Why there |
|---|---|---|

## Layers
- **Data** — API or DAO, DTO → domain mapping (`toDomain()`), cache strategy (cache-then-network, network-only, …).
- **Domain** — use cases, repository interfaces.
- **Presentation** — `UiState`, ViewModel, events, composables.

## Contracts
<Public signatures and type shapes in Kotlin. No bodies.>

## Wiring
- **Navigation** — `AppRoute` subtype and arguments, or none.
- **Koin** — which module binds what.
- **Resources** — new string and drawable keys, and the module they live in.
- **Schema** — entities, version bump, migration — or none.

## State and lifecycle
<What survives rotation and process death; which scope owns each coroutine; which dispatcher.>

## Failure paths
| Failure | Where it's mapped | What the user sees |
|---|---|---|
| Offline | | |
| Timeout | | |
| 401 | | |
| Empty | | |

## Test plan
| Requirement | Test (file → case) | Kind |
|---|---|---|
| R1 | `feature/x/src/commonTest/.../XViewModelTest.kt` → `emits Error when offline` *(create)* | ViewModel |

## Build steps
### Step 1 — <title>
- **Files:** `path/File.kt` *(create)*, `path/Other.kt`
- **Serves:** R1, R3
- **Do:** <what changes, precisely>
- **Verify:** `<command>` → <expected result>

### Step 2 — ...

## Alternatives considered
<One line each: the option and why it lost. Omit if there was only one sensible design.>

Gate: <PASSED | FAILED — options: <one line> | FAILED — requirements: <defect>>
```

## Changes with a fixed shape

These have one safe sequence. Spell every edit out as build steps — the implementer has no other source for them.

| Situation | The steps must cover |
|---|---|
| A feature that belongs in no existing module | CLAUDE.md → Adding a module, plus the feature's Koin module included in `shared`'s `appModule` and its route wired in `App.kt` |
| A screen someone navigates to | CLAUDE.md → Navigation: the `AppRoute` subtype, its `subclass(...)` registration and `AppRouteTest` entry, and the `AppRoot` entry |
| A resource a second module needs | CLAUDE.md → Compose resources: move it to `core:ui` under the same key, fix imports, then `./gradlew clean` |
| Any change to an `@Entity`, column, table or index | Migrate, never wipe: bump `DATABASE_VERSION` (with its KDoc history line); compile `core:database` to generate the schema JSON, which is checked in and never hand-edited; a new `MIGRATION_N_<N+1>` in `Migrations.kt` whose SQL comes from that JSON, appended to `APP_MIGRATIONS`; never edit a shipped migration; a test in `AppDatabaseMigrationTest` (`iosTest`) that seeds version N and asserts existing rows survive |

## Build steps

- Ordered by dependency: `core` before features, domain before data before presentation, wiring last.
- Each one small enough that its **Verify** isolates a failure to it. A **Verify** is a command and an expected result, usually `./gradlew :<module>:testAndroidHostTest` or `:<module>:compileKotlinIosSimulatorArm64`.
- Tests go in the step that introduces the code they test, not in a final "add tests" step.

## Gate

PASSED when every `R` maps to a design element and a test in the plan, and every `N` to a test or a named check — lint, a mobile-checklist item, or a manual step for Handover → How to try it; every path exists or is marked *(create)*; every build step has a **Verify**; and no decision is left to the implementer.

- **Several defensible designs with real trade-offs** — write each one briefly under **Alternatives considered** with a recommendation, and `FAILED — options`. The user picks.
- **A requirement that can't be met, or contradicts the code** — `FAILED — requirements`, naming it.
- **Answers to a previous `options` gate, or a revision request** — revise in place, and move the losing options to one line each.
