# Domain Module (Business Logic Layer)

Scope: everything under `domain/`. Merged with root `CLAUDE.md`.

## Rules
- Zero Android framework dependencies (no `Context`, no `android.*` imports). Pure Kotlin only.
- One UseCase = one business action. Name as verb phrases: `GetUserProfileUseCase`, not `UserManager`.
- UseCases depend on Repository interfaces defined in this module, never on concrete implementations.
- Domain models are distinct from data/DTO models — map at the `data` boundary, never leak DTOs upward.
- No coroutine dispatchers hardcoded here — inject `CoroutineDispatcher` for testability.
- Every public UseCase must have a corresponding unit test with no Android dependencies (fast, JVM-only tests).

## Testing
- 100% of UseCases must be unit-tested — this layer has no excuse for untested logic.
- No Robolectric, no instrumentation tests here — if a test needs Android, it's in the wrong module.
