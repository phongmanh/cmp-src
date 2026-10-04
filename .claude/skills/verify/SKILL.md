---
name: verify
description: Runs the right build, test and lint commands for the modules the current change touches, and reports a pass/fail table. Use before calling any task done, before a commit or PR, or when asked to "check", "verify" or "run the tests".
---

# Verify the current change

Run the checks that cover what changed — no more, no less — and report them faithfully.

## 1. Find the touched modules

```
git status --short
```

Map each changed path to a Gradle module by its first segments: `feature/<name>/…` → `:feature:<name>`, `core/<name>/…` → `:core:<name>`, `shared/…` → `:shared`, `androidApp/…` → `:androidApp`.

## 2. Pick the commands

| What changed | Run |
|---|---|
| Any KMP module | `:<module>:testAndroidHostTest :<module>:compileKotlinIosSimulatorArm64` for each, in one invocation |
| Anything a feature or `shared` depends on | also `:shared:compileKotlinIosSimulatorArm64` — the iOS framework is where a broken `expect`/`actual` or a missing Koin binding's types surface |
| `core/database` entities, DAOs, migrations or schemas | also `:core:database:iosSimulatorArm64Test` (the real-SQLite tests live in `iosTest`) |
| `shared/.../navigation/` | also `:shared:testAndroidHostTest` (`AppRouteTest`) |
| `androidApp`, a manifest, or any Kotlin at all | `:androidApp:assembleDebug :androidApp:lintDebug` |
| `build-logic/`, `gradle/libs.versions.toml`, `settings.gradle.kts` | `testAndroidHostTest` for every module, then `:androidApp:assembleDebug` |

Example for a change in `feature/profile`:

```
./gradlew :feature:profile:testAndroidHostTest :feature:profile:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
```

## 3. Report

```
| Check | Command | Result |
|---|---|---|
| Host tests | ./gradlew :feature:profile:testAndroidHostTest | ✅ 42 passed |
| iOS compile | … | ❌ <first error, file:line> |
```

- On failure, quote the first real error (not Gradle's summary), with `file:line`.
- Say what you did **not** run and why — on-device UI, a real iOS launch from Xcode, the Keychain tests that need an entitlement.
- Never report a check as passing that you didn't run.
