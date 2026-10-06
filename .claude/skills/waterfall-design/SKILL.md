---
name: waterfall-design
description: How to write and gate 02-design.md in a waterfall delivery — the template, grounding every decision in the existing code, the class and sequence diagrams with the change highlighted (saved to docs/design/), the changes with a fixed shape (new module, destination, moved resource, schema change), and the gate. Preloaded by waterfall-architect; not for direct use.
user-invocable: false
---

# Phase 2 — Design

Decide *how*, against the code that exists. Read the modules you'll touch, and their files in `docs/design/`, before deciding anything, and cite `file:line` for every pattern you reuse. The implementer is a different agent and model: anything you leave open, it must not decide — so leave nothing open.

## Template — `02-design.md`

````markdown
# Design: <task>

## Placement
| Piece | Module / package | Why there |
|---|---|---|

## System design

### Classes
<One `classDiagram` per module the change touches, with the change highlighted.>

### Sequences
#### <Flow name>
<One line: what starts it and what it ends in.>
<One `sequenceDiagram` per flow the change adds or alters, with the change highlighted.>

### Change summary
| Element | Module | Change | Note |
|---|---|---|---|
| `ExportCustomersUseCase` | `feature:customers` | added | |
| `CustomersViewModel` | `feature:customers` | changed | `+ onAction(Export)`, `~ state` |

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
````

## System design

The diagrams are how a reader understands the change in a minute, and what `docs/design/` keeps once it lands. Its `README.md` sets the conventions — what a class diagram includes, the arrow meanings, and the highlight markers — follow them exactly.

- **Start from the baseline.** Copy the affected part of the module's `docs/design/<module>.md`, then mark it up. Anything unmarked must already exist in the code; anything you add is `added`, anything you alter is `changed` with a `note for` listing each member, anything you delete stays in the diagram as `removed`.
- **Draw the slice, in detail.** Every class added, changed or removed, plus each one's direct collaborators unmarked, so the reader sees where the change plugs in. Members are the real Kotlin signatures from **Contracts**; a name the diagram uses and **Contracts** doesn't define is a defect.
- **One sequence per flow the change adds or alters**, trigger to server or database, with each **Failure paths** row as an `alt` branch. A flow that only gains a new class in the middle still gets redrawn, with the new steps highlighted.
- **No baseline file for a module you touch** — draw that module's current classes and the flows it already has, unmarked, alongside the change. The implementer creates the file from it.
- **Change summary** lists every highlighted element once. Diagrams, summary and **Contracts** must agree.

The last build step saves the diagrams to `docs/design/`:

```markdown
### Step N — Update the system design
- **Files:** `docs/design/<module>.md` *(create if missing)*, `docs/design/README.md` (index row, only for a new file)
- **Serves:** —
- **Do:** Replace `## Classes` and the `### <Flow>` sections named below with the diagrams from **System design**, highlights stripped as `docs/design/README.md` describes, and adjusted to anything logged under **Deviations**. Sections: <list each one, and whether it is added, replaced or removed>.
- **Verify:** every class and signature in the updated diagrams exists in the diff or the code — `grep` each added name.
```

## Changes with a fixed shape

These have one safe sequence. Spell every edit out as build steps — the implementer has no other source for them.

| Situation | The steps must cover |
|---|---|
| A feature that belongs in no existing module | CLAUDE.md → Adding a module, plus the feature's Koin module included in `shared`'s `appModule` and its route wired in `App.kt`, and a new `docs/design/<module>.md` with its index row |
| A screen someone navigates to | CLAUDE.md → Navigation: the `AppRoute` subtype, its `subclass(...)` registration and `AppRouteTest` entry, and the `AppRoot` entry |
| A resource a second module needs | CLAUDE.md → Compose resources: move it to `core:ui` under the same key, fix imports, then `./gradlew clean` |
| Any change to an `@Entity`, column, table or index | Migrate, never wipe: bump `DATABASE_VERSION` (with its KDoc history line); compile `core:database` to generate the schema JSON, which is checked in and never hand-edited; a new `MIGRATION_N_<N+1>` in `Migrations.kt` whose SQL comes from that JSON, appended to `APP_MIGRATIONS`; never edit a shipped migration; a test in `AppDatabaseMigrationTest` (`iosTest`) that seeds version N and asserts existing rows survive |

## Build steps

- Ordered by dependency: `core` before features, domain before data before presentation, wiring next, and the system design update last.
- Each one small enough that its **Verify** isolates a failure to it. A **Verify** is a command and an expected result, usually `./gradlew :<module>:testAndroidHostTest` or `:<module>:compileKotlinIosSimulatorArm64`.
- Tests go in the step that introduces the code they test, not in a final "add tests" step.

## Gate

PASSED when every `R` maps to a design element and a test in the plan, and every `N` to a test or a named check — lint, a mobile-checklist item, or a manual step for Handover → How to try it; every path exists or is marked *(create)*; every build step has a **Verify**; **System design** has a class diagram for every module touched and a sequence for every flow added or altered, each change highlighted and agreeing with **Contracts** and the **Change summary**, and the last step saves them to `docs/design/`; and no decision is left to the implementer.

- **Several defensible designs with real trade-offs** — write each one briefly under **Alternatives considered** with a recommendation, and `FAILED — options`. The user picks.
- **A requirement that can't be met, or contradicts the code** — `FAILED — requirements`, naming it.
- **Answers to a previous `options` gate, or a revision request** — revise in place, and move the losing options to one line each.
