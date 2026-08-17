# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

CMPsrc is a Kotlin Multiplatform / Compose Multiplatform project generated from the JetBrains KMP wizard. All shared UI and logic lives in a single `shared` module; each platform module is a thin shell that just hosts the shared `App()` composable. Package/namespace across all modules is `com.liam.cmp_src`.

## Module layout

- `shared` — the core module. Contains the shared Compose UI (`App.kt`), shared logic, and `expect`/`actual` platform declarations. Targets: `androidLibrary`, `jvm`, `js` (browser), `wasmJs` (browser), `iosArm64`/`iosSimulatorArm64`.
- `androidApp` — Android application shell. `MainActivity` just calls `setContent { App() }`.
- `desktopApp` — JVM application shell using `androidx.compose.ui.window.application`/`Window`, entry point `com.liam.cmp_src.MainKt`.
- `webApp` — JS + Wasm browser application shell using `ComposeViewport`.
- `iosApp` — native Xcode/SwiftUI project. `iOSApp.swift` hosts `ContentView`, which wraps the `Shared.framework` produced by the `shared` module's iOS targets via `MainViewController()` (`shared/src/iosMain/.../MainViewController.kt`).

The app modules have essentially no logic of their own — new features and platform-specific behavior belong in `shared`, with app-module changes limited to wiring/entry points.

### expect/actual pattern

`shared/src/commonMain/.../Platform.kt` declares `expect fun getPlatform(): Platform`. Each target provides an actual in a sibling source set named `Platform.<target>.kt`:
- `androidMain/Platform.android.kt`, `iosMain/Platform.ios.kt`, `jsMain/Platform.js.kt`, `jvmMain/Platform.jvm.kt`, `wasmJsMain/Platform.wasmJs.kt`

Follow this naming/source-set convention when adding new expect/actual declarations.

### Compose resources

Shared drawables/resources live under `shared/src/commonMain/composeResources/` and are accessed via generated accessors imported from `cmpsrc.shared.generated.resources.Res` (e.g. `Res.drawable.compose_multiplatform`).

## Gradle/toolchain notes

- Type-safe project accessors are enabled (`settings.gradle.kts`); reference other modules as `projects.shared`, not string paths.
- Dependency versions are centralized in `gradle/libs.versions.toml`; add new dependencies there rather than hardcoding coordinates in a module's `build.gradle.kts`.
- The `shared` module's Android target uses the newer `com.android.kotlin.multiplatform.library` plugin (`androidLibrary { ... }` DSL in `shared/build.gradle.kts`), **not** the classic `com.android.library` plugin with an `android { }` block. Its unit-test source set is `androidHostTest` (configured via `withHostTest { }`), which is the KMP-library equivalent of the old `androidTest`/unit-test setup — don't confuse it with instrumented tests.
- Versions in use are bleeding-edge (AGP 9.0.1, Kotlin 2.4.0, Compose Multiplatform 1.11.1) — this is intentional for this project, not a mistake to "fix" by downgrading or reverting to older APIs.

## Common commands

Build everything:
```
./gradlew build
```

Run an app:
```
./gradlew :desktopApp:run                       # Desktop (JVM)
./gradlew :webApp:jsBrowserDevelopmentRun        # Web (JS, webpack dev server)
./gradlew :webApp:wasmJsBrowserDevelopmentRun    # Web (Wasm, webpack dev server)
./gradlew :androidApp:installDebug               # Android (installs to connected device/emulator)
```
iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run — the shared framework is built/embedded automatically as part of the Xcode build.

Lint (Android):
```
./gradlew :androidApp:lintDebug
```

### Tests

Test sources live per-target under `shared/src/{commonTest,androidHostTest,jvmTest,iosTest}`.

```
./gradlew :shared:allTests                                   # every shared-module target
./gradlew :shared:jvmTest                                    # commonTest + jvmTest, JVM-executed
./gradlew :shared:testAndroidHostTest                        # commonTest + androidHostTest, JVM-executed
./gradlew :shared:iosSimulatorArm64Test                       # commonTest + iosTest, iOS simulator
./gradlew :shared:jsBrowserTest                               # commonTest + jsMain tests, in-browser
./gradlew :shared:wasmJsBrowserTest                           # commonTest + wasmJsMain tests, in-browser
./gradlew :androidApp:testDebugUnitTest                       # androidApp module's own unit tests
```

Run a single test class (works with `jvmTest`/`testAndroidHostTest`/`testDebugUnitTest`):
```
./gradlew :shared:jvmTest --tests "com.liam.cmp_src.SharedLogicDesktopTest"
```

## Global rules

- Kotlin only for new code; prefer `val` over `var` and immutability; avoid `!!` (use `?.`, `?:`, `requireNotNull` with a message).
- No hardcoded user-facing strings, colors, or dimensions — use Compose resources / constants (see "Compose resources" above).
- No secrets or API keys in source or version control.
- Shared business logic must be unit-testable by design; add tests in the appropriate per-target source set (see "Tests" above).
- Follow SOLID, DRY, KISS, YAGNI — details in `.claude/rules/GUIDELINES.md`.

## When making changes

- Match existing patterns in the file/module before introducing new ones.
- Prefer editing existing files over creating new ones unless the change clearly belongs in a new class.
- New features and platform behavior belong in `shared`; keep app modules limited to wiring/entry points.
- Run the relevant build/test commands above before considering a task done.

## Team conventions (.claude/rules)

Additional coding conventions live under `.claude/rules/` and are auto-loaded by Claude Code:

- Core principles & conventions (SOLID/DRY/KISS/YAGNI, Kotlin, testing, security) — `.claude/rules/GUIDELINES.md`
- Presentation-layer rules — `.claude/rules/app/CLAUDE.md`
- Domain-layer rules — `.claude/rules/domain/CLAUDE.md`
- Data-layer rules — `.claude/rules/data/CLAUDE.md`
- Naming conventions — `.claude/rules/naming.md`
- Git & PR process — `.claude/rules/git.md`

Note: these rules describe a layered Android app (`app`/`domain`/`data` modules, Hilt, Room/Retrofit,
ktlint/detekt) that this KMP project does not currently use — its code lives in `shared` with thin
platform shells, and Hilt/ktlint/detekt are not configured. Treat them as general team conventions and
adapt to the KMP structure; do not restructure this project to match unless explicitly asked.
