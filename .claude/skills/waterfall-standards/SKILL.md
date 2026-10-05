---
name: waterfall-standards
description: The protocol every waterfall-* agent shares — workspace, document ownership, the gate line, resuming, going upstream, the build/test checks per changed path, and the mobile checklist. Preloaded by the waterfall agents; not for direct use.
user-invocable: false
---

# Waterfall standards

Four agents deliver a task in phases. Each one owns its documents, finishes its phase and gates it before anything downstream starts. The `/waterfall` skill in the main session runs the gates and is the only one that talks to the user — an agent never waits on an answer; it writes the question down, fails its gate and stops.

Why the gates matter: in Waterfall, moving upstream is the expensive move. A requirement discovered during implementation means the design was built on a guess. Each gate is the cheap place to catch that.

## Workspace

`.claude/waterfall/<slug>/` (git-ignored). The orchestrator gives you the slug; derive a short kebab-case one only if it didn't.

| Phase | Document | Owner | Reads first |
|---|---|---|---|
| 1. Requirements | `01-requirements.md` | `waterfall-analyst` | the task, the code it touches |
| 2. Design | `02-design.md` | `waterfall-architect` | `01`, the code |
| 3. Implementation | `03-implementation.md` | `waterfall-implementer` | `02` (`01` for context); the latest `04` round in a fix round |
| 4. Verification | `04-verification.md` | `waterfall-verifier` | `01`–`03`, `git diff` |
| 5. Handover | `05-handover.md` | `waterfall-verifier` | `01`–`04` |

Write only your own documents. Never edit an upstream one, not even to fix a typo — report the defect in your own document. A `waterfall-write-guard` hook enforces this for the Write and Edit tools; don't route around it with Bash.

## The gate line

The last line of every document is exactly one of:

```
Gate: PASSED
Gate: FAILED — <category>: <reason>
```

| Category | Set by | Means |
|---|---|---|
| `questions` | analyst | the user must answer the listed questions |
| `options` | architect | the user must pick between the designs laid out |
| `requirements` | architect, verifier | `01` is wrong or incomplete |
| `design` | implementer, verifier | `02` is wrong or incomplete — a decision it didn't make |
| `implementation` | verifier | the code doesn't meet the design, or a check fails |
| `revision requested` | orchestrator | the user asked for changes, listed after the colon |
| `superseded` | orchestrator | an upstream phase was reopened; redo this one against it |

A document with rounds (`03`, `04`) has one gate line, at the very end, for the latest round.

## Starting a phase

1. Read the upstream documents in the table. If one is missing or doesn't end in `Gate: PASSED`, write nothing — return that and stop.
2. If you were told to run a fix round (implementer) or a numbered verification round (verifier), append that round to your document, whatever its gate says (see your phase skill). Otherwise, if your document already exists:
   - `Gate: PASSED` — it is signed off. Don't rewrite it; return its result.
   - `FAILED — questions`, `options` or `revision requested` — revise it in place with the answers or changes you were given. Keep what still holds.
   - `FAILED — superseded` — redo it against the new upstream: rewrite `01` or `02`; append a round to `03` or `04`, titled for the revision, so the history stays.

## Going upstream

When your phase shows that an upstream document is wrong — a requirement that can't be tested, a decision the design never made — don't patch around it and don't guess. Write what you found, fail your gate with the category naming that upstream phase, and stop. The orchestrator reopens it with the user.

## Checks

The implementer runs these before its gate; the verifier runs them again itself. Pick by what `git status --short` shows changed — `feature/<name>/…` is `:feature:<name>`, `core/<name>/…` is `:core:<name>` — and run no more than the table asks.

| What changed | Run |
|---|---|
| Any KMP module (`core`, `feature`, `shared`) | `:<module>:testAndroidHostTest :<module>:compileKotlinIosSimulatorArm64` for each, in one invocation |
| Anything a feature or `shared` depends on | also `:shared:compileKotlinIosSimulatorArm64` — the iOS framework is where a broken `expect`/`actual` or a missing Koin binding's types surface |
| `core/database` entities, DAOs, migrations or schemas | also `:core:database:iosSimulatorArm64Test` (the real-SQLite tests live in `iosTest`) |
| `shared/.../navigation/` | also `:shared:testAndroidHostTest` (`AppRouteTest`) |
| `androidApp`, a manifest, or any Kotlin at all | `:androidApp:assembleDebug :androidApp:lintDebug` |
| `build-logic/`, `gradle/libs.versions.toml`, `settings.gradle.kts` | `testAndroidHostTest` for every module, then `:androidApp:assembleDebug` |

Report each as `| Check | Command | Result |`. On a failure, quote the first real error with `file:line`, not Gradle's summary. Checks that can't run here — on-device UI, a real iOS launch from Xcode, Keychain tests needing an entitlement — are written down as not run, with the reason.

## Mobile checklist

The architect applies it while designing; the verifier checks the diff against it.

- **Architecture** — composables render state and forward events; ViewModel → UseCase → Repository; one `UiState` per screen via `StateFlow`, one-off events via `SharedFlow`; screens report outcomes and `AppRoot` navigates.
- **Module boundaries** — no feature depends on another; a symbol a second module needs moves down into `core`; `shared` only wires.
- **Lifecycle** — state that matters survives rotation and process death; collection is lifecycle-aware; no `Context` in long-lived objects; work runs in `viewModelScope`, never `GlobalScope`.
- **Threading** — no I/O, database or parsing on the main thread; dispatchers are injected.
- **Network and offline** — every call returns `ApiResult`; offline, timeout and error each have a UI state; the cache strategy is stated; network and database calls log start and end.
- **Platform parity** — behaves the same on Android and iOS; every `expect` has both actuals in the same module; permissions are declared in the manifest and `Info.plist`.
- **UI** — strings, colours and dimensions come from resources or the theme; dark mode works; layouts adapt to screen size; new composables have previews; lists use stable keys; text fields take a `TextFieldState` owned by a `FormField`.
- **Accessibility** — content descriptions, 48dp touch targets, scales with system font size, sufficient contrast.
- **Security** — no secrets in source; tokens only through `core:security`; external input validated; no PII in logs.
- **Performance and battery** — lists paginate; images are sized and cached; background work is scheduled by the OS, never polled.
- **Tests** — ViewModels, use cases and repositories tested with fakes; Arrange–Act–Assert, one behaviour per test; every public use case has a test.

## Writing standards

- Short and concrete. A reviewer should be able to audit any phase in two minutes.
- CLAUDE.md and `.claude/rules/` are already in your context. Apply them; don't restate them.
- Don't expand scope. Anything outside the requirements is a note for Handover → Follow-ups.
- Report faithfully. A skipped check is written down as skipped, with the reason. Never report a command you didn't run.
- Never commit, push, stash, reset or open a PR.
- Return a short summary: the phase, its gate line, and on `FAILED` exactly what the user must answer or decide.
