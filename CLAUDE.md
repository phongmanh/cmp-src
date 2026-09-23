# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

CMPsrc is a Kotlin Multiplatform / Compose Multiplatform project targeting **mobile only — Android and iOS**. The code is split by feature into KMP modules — `core:*` building blocks and one `feature:*` module per feature — assembled by the `shared` app module; each platform module is a thin shell that just hosts the shared `App()` composable. Package/namespace across all modules is `com.liam.cmp_src`.

The desktop (JVM) and browser (JS/Wasm) targets that this project started with have been removed. Adding one back is not a one-line change: it needs the target declared in `cmpsrc.kmp.library` (which gives it to every module, and `cmpsrc.room` then adds the Room compiler for it on its own), actuals for every `expect` — `getPlatform` (`feature:home`), `platformEngine`/`localApiHost` (`core:network`), `createSocialAuthClient` (`feature:auth`), `rememberPlatformModule` (`shared`) — plus a database builder (`core:database`) and token cipher (`core:security`) for the target, and — for JS/Wasm — the Node/Yarn/Binaryen ivy repositories back in `settings.gradle.kts`, because `FAIL_ON_PROJECT_REPOS` rejects the ones the Kotlin plugin registers for itself.

## Module layout

```
androidApp ─┐
iosApp ─────┴─> :shared ──> :feature:auth ────┐
                        ──> :feature:home ────┼─> :core:ui ──> :core:domain ──> api-contract
                        ──> :feature:profile ─┘    (auth and profile also ─> :core:network)
                        ──> :core:database ──> :core:network, :core:security
feature:*, core:ui ──> :core:utils
tests only: feature:*, core:domain ──> :core:testing
```

- `shared` — the app module. `App()`/`AppRoot` (navigation), the Koin graph (`di/AppModule.kt` includes each feature's module; `di/PlatformModule*.kt` binds the database and cipher per target), the Coil avatar `ImageLoader`, and `MainViewController()` for iOS. It links `Shared.framework`, the only framework the iOS app imports. No feature code lives here.
- `feature:auth`, `feature:home`, `feature:profile` — one module per feature, each with its own `data`/`domain`/presentation packages, Compose resources, tests and Koin module (`authModule`, `homeModule`, `profileModule`).
- `core:domain` — the account types every feature shares (`AuthResult`, `AuthError`, `SocialProvider`, `PasswordError`), the `AuthRepository` interface and `SignOutUseCase`. Pure Kotlin.
- `core:ui` — theme, modifiers, the shared components (`GlassCard`, `PrimaryActionButton`, `AuthTextField`, `ErrorBanner`, `UserAvatar`, …), the `asMessage()`/`asLabel()` mappers for the domain types, and the resources those use.
- `core:utils` — platform helpers with no domain meaning: `format` (`formatDecimal`, `formatDate`/`formatDateTime`, `formatByteSize` — always in the device's locale and time zone) and `permission` (`rememberPermissionController()` for camera, microphone, location and notifications). A permission still has to be declared by the app — the manifest entry on Android, the `NS…UsageDescription` key in `iosApp/iosApp/Info.plist` on iOS (see `Permission`'s KDoc) — and none are declared yet.
- `core:network`, `core:database`, `core:security` — Ktor plumbing, Room, and the platform token ciphers (see "Networking").
- `core:testing` — fakes that more than one module's tests use (`FakeAuthRepository`). Only ever a `commonTest` dependency.
- `androidApp` — Android application shell. `MainActivity` just calls `setContent { App() }`.
- `iosApp` — native Xcode/SwiftUI project. `iOSApp.swift` hosts `ContentView`, which wraps `Shared.framework` via `MainViewController()` (`shared/src/iosMain/.../MainViewController.kt`). Its build phase runs `:shared:embedAndSignAppleFrameworkForXcode`.

**Features never depend on one another** — Gradle rejects it, since no feature module declares another. A symbol lives in the module of its only consumer; when a second module needs it, it moves *down* into `core` rather than being imported sideways, and `shared` is the only place features meet. For example, `HomeRoute` takes the profile tab as a slot that `App.kt` fills with `ProfileRoute`, and `ProfileApi` reads `GET /users/me` itself instead of borrowing `AuthApi`.

Packages follow modules (`com.liam.cmp_src.core.network`, `com.liam.cmp_src.feature.auth`, …); `shared` keeps the root package. New features and platform behavior belong in a `feature:*` or `core:*` module, with `shared` and the platform apps limited to wiring/entry points.

### Adding a module

Create `<group>/<name>/build.gradle.kts`, add `include(":<group>:<name>")` to `settings.gradle.kts`, and apply one convention plugin (see "Gradle/toolchain notes"). The Android namespace (`com.liam.cmp_src.<group>.<name>`), the targets, host tests and the resources package all derive from the module path, so a module script never declares `androidLibrary { }`. Keep leaf names unique — Kotlin/Native library names are built from them.

### expect/actual pattern

`feature/home/src/commonMain/.../Platform.kt` declares `expect fun getPlatform(): Platform`. Each target provides an actual in a sibling source set of the **same module**, named `Platform.<target>.kt`:
- `androidMain/.../Platform.android.kt`, `iosMain/.../Platform.ios.kt`

Follow this naming/source-set convention when adding new expect/actual declarations. An `expect` and its actuals never span modules.

### Compose resources

Each UI module keeps its own resources under `<module>/src/commonMain/composeResources/`, and its generated `Res` lives in `cmpsrc.<module path>.generated.resources` (e.g. `cmpsrc.feature.auth.generated.resources.Res`) — `cmpsrc.cmp.library` sets that package. A module's `Res` is internal, except `core:ui`'s (`publicResClass = true`).

A string or drawable lives in the module whose code uses it; when a second module uses it too, it moves to `core:ui`. Keys keep the names they were created with (`login_submit` is the button text in `core:ui`). A file that uses both its own module's resources and `core:ui`'s imports the latter under an alias:

```kotlin
import cmpsrc.core.ui.generated.resources.Res as UiRes
import cmpsrc.core.ui.generated.resources.ic_lock
// UiRes.drawable.ic_lock
```

After moving a resource between modules, run `./gradlew clean`: the Android asset copy task (`copyAndroidMainComposeResourcesToAndroidAssets`) never deletes files, so the old module's copy stays in the APK.

### Navigation

Navigation 3 — `androidx.navigation3:navigation3-runtime` (androidx's own multiplatform publication) plus `org.jetbrains.androidx.navigation3:navigation3-ui` for the multiplatform `NavDisplay`. The back stack is held by `AppRoot` in `shared`'s `App.kt`; destinations are the `@Serializable` keys in `shared/src/commonMain/.../navigation/AppRoute.kt`. Only `shared` knows the routes — feature modules never see `AppRoute`.

Adding a destination is three edits:

1. a new subtype of `AppRoute`, carrying whatever arguments the destination needs;
2. a `subclass(...)` line in `appNavConfiguration` — only Android can resolve back-stack keys reflectively, so iOS needs them registered, and `AppRouteTest` fails if this is missed;
3. an `entry<...>` block in `AppRoot`'s `entryProvider`.

Screens never navigate themselves. A route composable reports what happened (`onSignedIn`, `onSignedOut`) and `AppRoot` decides what that does to the back stack — see `resetTo` for handovers that must not leave the previous screen behind.

### Networking

Ktor client, driven by the shared `api-contract` module (`com.example:api-contract`, resolved from
mavenLocal — `useMavenLocal=true` in `gradle.properties`). That module publishes the routes
(`ApiRoutes`), the DTOs, the error shape (`ErrorResponse`/`ErrorCode`) and the field limits, so the
app never spells a path or a payload by hand. A route the server renames breaks the build here.

`core:network` holds the plumbing:

- `HttpClientFactory.kt` — `createHttpClient(...)` installs ContentNegotiation, bearer `Auth` with
  refresh, `HttpTimeout`, retry (5xx on idempotent methods only) and `Logging`. One client is a
  `single` in `shared`'s `appModule`; it owns a connection pool, so never build one per call.
- `ApiConfig.kt` — base URL and timeouts, injected rather than global. Which backend the app talks
  to is `ApiEnvironment.ACTIVE` (`LOCAL` or `PRODUCTION`) — one line, one file, every target;
  `ApiConfig(ApiEnvironment.PRODUCTION)` or `ApiConfig(baseUrl = ...)` overrides it per caller.
  `PRODUCTION_BASE_URL` is a shared constant; only `localApiHost()` stays `expect`/`actual`,
  because only the Android emulator reaches the host at `10.0.2.2`.
- `ApiResult.kt` / `ApiCall.kt` — every call returns `ApiResult<T>`; `sendRequest` is the one place
  an exception becomes an `ApiError`, so nothing above the data layer catches anything.
- `TokenStore.kt` — what the `Auth` plugin reads and refreshes into. Both targets bind
  `RoomTokenStore` (`core:database`, via `rememberPlatformModule`), which keeps the session in
  SQLite with the row encrypted by the platform key store (`core:security`) — Android Keystore, iOS
  Keychain. `InMemoryTokenStore` is the test double.

The signed-in user is the contract's `UserResponse` end to end — `AuthResult.Success`,
`AppRoute.Home`, and the home UI all carry it, and there is no parallel `AuthUser` model to keep
in sync. Its `displayName`, `email` and `avatarUrl` are nullable, so anything that renders a user
goes through the helpers in `core:ui`'s `component/UserIdentity.kt` (`displayLabel()`,
`signedInProvider()`, and the `sampleUser()` fixture previews and tests share). `SocialProvider`
stays a domain enum, bridged to the wire by its `key`/`fromKey`.

Per-feature API classes live with the feature (`AuthApi` in `feature:auth`, `ProfileApi` in
`feature:profile`), not in `core`. A typed POST must set `contentType(ContentType.Application.Json)`
— ContentNegotiation silently declines to serialize a body without it, and the call fails before
leaving the device.

Engine actuals do **not** follow the one-file-per-target rule exactly: `iosMain` (not `nativeMain`)
provides Darwin, and it covers every Apple target. Declaring it a level up, or once per iOS leaf,
is a duplicate-`actual` compile error.

Android talks to a local dev server over cleartext only in debug: `androidApp/src/debug/` carries a
network security config allowing `10.0.2.2` and `localhost` and nothing else. Release builds keep
the platform's HTTPS-only default.

### Auth flow

`AuthRepositoryImpl` (`feature:auth`) is the whole flow, and it is network-backed — there is no local
demo account. Other features reach it only through the `AuthRepository` interface in `core:domain`.

- **Email/password** — `AuthApi.login`, then one `GET /users/me`. A `TokenResponse`'s copy of the
  user has an empty `linkedProviders` by contract, and the home screen reads that list, so a
  sign-in hydrates it. A failed hydration is not a failed sign-in: the tokens are already stored,
  so the token response's user stands in.
- **Social** — `SocialAuthClient.requestCredential(provider)` returns a `SocialCredential`, never a
  session: the provider token is exchanged at `POST /auth/social` and the server issues the tokens.
  Both platform actuals still delegate to `DemoSocialAuthClient`, which returns a placeholder
  token — the exchange after it is real, so social sign-in works only against a deployment that
  accepts one.
- **Sign-out** — `HomeViewModel` or `ProfileViewModel` → `SignOutUseCase` (`core:domain`) →
  `AuthApi.logout`, which drops the local tokens whether or not the server answered. `signOut()` is
  `NonCancellable`, because the caller is a screen on its way out and a cancelled call would leave a
  live refresh token behind.

Failures are mapped once per feature: `AuthMapping.kt` in `feature:auth`, `ProfileMapping.kt` in
`feature:profile`. Only the email/password path can report `AuthError.InvalidCredentials`; the same
401 on the social path becomes `Unknown`, since the user never typed a password to get wrong.

## Gradle/toolchain notes

- Type-safe project accessors are enabled (`settings.gradle.kts`); reference other modules as `projects.core.network` / `projects.feature.auth`, not string paths.
- Dependency versions are centralized in `gradle/libs.versions.toml`; add new dependencies there rather than hardcoding coordinates in a module's `build.gradle.kts`.
- Build configuration lives in convention plugins in the `build-logic` included build. A module applies one or two ids and lists only its own extra dependencies:
  - `cmpsrc.kmp.library` — non-UI module: Kotlin Multiplatform, the KMP Android library target, both iOS targets, host tests, `kotlin-test` + coroutines-test.
  - `cmpsrc.cmp.library` — the above plus Compose, the Compose artifacts and per-module Compose resources.
  - `cmpsrc.cmp.feature` — a feature module: `cmpsrc.cmp.library` + `cmpsrc.cmp.koin`, `core:domain`, `core:ui`, `core:utils`, the lifecycle Compose APIs, and `core:testing` for tests.
  - `cmpsrc.room` — Room 3 in a module with a `@Database`: KSP, the runtime and bundled driver, the compiler on every target, the schema in `<module>/schemas` (checked in).
  - `cmpsrc.cmp.koin` — Koin core + Compose; `cmpsrc.cmp.application.android` / `cmpsrc.cmp.android.compose` — the `androidApp` shell.
- Use `api(...)` only for what a module's public signatures (or inline function bodies) expose — e.g. `core:network` exposes Ktor core and coroutines, `core:database` exposes `core:network`/`core:security` and the Room runtime.
- Every KMP module's Android target uses the newer `com.android.kotlin.multiplatform.library` plugin, configured by `cmpsrc.kmp.library` — **not** the classic `com.android.library` plugin with an `android { }` block. Its unit-test source set is `androidHostTest`, which is the KMP-library equivalent of the old `androidTest`/unit-test setup — don't confuse it with instrumented tests.
- Versions in use are bleeding-edge (AGP 9.0.1, Kotlin 2.4.0, Compose Multiplatform 1.11.1) — this is intentional for this project, not a mistake to "fix" by downgrading or reverting to older APIs.

## Common commands

Build everything:
```
./gradlew build
```

Run an app:
```
./gradlew :androidApp:installDebug               # Android (installs to connected device/emulator)
```
iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run — the shared framework is built/embedded automatically as part of the Xcode build.

Lint (Android):
```
./gradlew :androidApp:lintDebug
```

### Tests

Test sources live per module and target under `<module>/src/{commonTest,androidHostTest,iosTest}`, next to the code they test.

```
./gradlew allTests                                # every module, every target
./gradlew testAndroidHostTest                     # every module's commonTest + androidHostTest, JVM-executed
./gradlew :feature:auth:testAndroidHostTest       # one module
./gradlew :core:database:iosSimulatorArm64Test    # commonTest + iosTest, iOS simulator
./gradlew :androidApp:testDebugUnitTest           # androidApp module's own unit tests
```

`testAndroidHostTest` is the fast loop for common code — it runs `commonTest` on the host JVM and
needs no device. `commonTest` itself has no SQLite driver to run against, so coverage of the real
schema lives in `core:database`'s `iosTest` (`AppDatabaseMigrationTest`, `CustomerDaoTest`), and the
Keychain cipher's in `core:security`'s.

Run a single test class (works with `testAndroidHostTest`/`testDebugUnitTest`):
```
./gradlew :feature:auth:testAndroidHostTest --tests "com.liam.cmp_src.feature.auth.LoginViewModelTest"
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
- New features belong in a `feature:*` module and building blocks two features share in `core:*`; keep `shared` and the platform apps limited to wiring/entry points.
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
ktlint/detekt) that this KMP project does not currently use — its code is split into KMP modules by
feature rather than by layer (the layers are packages inside each `feature:*` module), with thin
platform shells, and Hilt/ktlint/detekt are not configured. Treat them as general team conventions
and adapt to the KMP structure; do not restructure this project to match unless explicitly asked.
