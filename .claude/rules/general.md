# General conventions

These apply to every module. The architecture, module boundaries and the global rules are in the root `CLAUDE.md`; the layer rules (`presentation.md`, `domain.md`, `data.md`) load when you work on files in that layer.

## Design

- SOLID, DRY, KISS and YAGNI, applied to this codebase: a ViewModel owns UI state, a use case one business action, a repository one data boundary. Dependencies come through Koin, never constructed in place. An abstraction earns its place when a second implementation or a test double needs it — not in advance.
- Presentation → domain → data, with unidirectional data flow and one source of truth for each piece of state.
- Leave the code you touch cleaner than you found it — within the task's scope.

## Kotlin

- `data class` for models; `sealed interface` for states and events.
- Coroutines and Flow for async work. I/O, parsing and database work run off the main thread.
- Small, single-purpose functions; past about 30 lines, look for the split.
- No magic numbers or strings: a named constant, a resource or a theme token.
- KDoc on every class, function and property — clear and concise.
- Log the start and end of each network and database call, so a failure can be traced. HTTP is covered by the `Logging` plugin `createHttpClient` installs; database calls log their own. Nothing logs inside composables or pure domain code, and no log carries a token or personal data.

## UI

- Colours and dimensions come from `core:ui`'s theme (`Color.kt`, `Dimens.kt`), and dark mode works.
- State that matters survives rotation and process death — a ViewModel, or `SavedStateHandle`.
- Layouts adapt to the screen; prefer flexible sizing to fixed dimensions.
- Images load through Coil (the `ImageLoader` `shared` provides), sized for where they're shown.
- Lazy lists get stable `key`s; long lists paginate.

## Errors and offline

- Never swallow an exception. The data layer turns failures into `ApiResult` errors once; the screen shows a message the user can act on.
- Slow and missing networks are normal: rely on the client's timeouts and retries, and on the cached fallback where a repository has one.

## Testing

- ViewModels, use cases and repositories are unit-testable by construction — dependencies injected, fakes in `commonTest`, shared fakes in `core:testing`.
- Arrange–Act–Assert, one behaviour per test.

## Quality and security

- Fix Android Lint warnings (`:androidApp:lintDebug`). Suppress one only with a comment that says why.
- Sensitive data is stored through `core:security` (Android Keystore, iOS Keychain).
- Validate every input from outside the app — the server, deep links, the user.
- HTTPS only. The one exception is the debug-only cleartext config for the local dev server (`CLAUDE.md` → Networking).
