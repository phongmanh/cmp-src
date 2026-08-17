# Mobile Development Guidelines

Detailed conventions for all mobile projects (Java / Kotlin). Referenced from root `CLAUDE.md` via `@GUIDELINES.md` — do not duplicate this content there.

## 1. Core Principles

- **SOLID**
  - **S** — Single Responsibility: one class, one reason to change (e.g., ViewModel handles UI state, Repository handles data).
  - **O** — Open/Closed: extend behavior via interfaces/abstraction, don't modify existing code.
  - **L** — Liskov Substitution: subclasses must be usable wherever the parent is expected.
  - **I** — Interface Segregation: prefer small, focused interfaces over fat ones.
  - **D** — Dependency Inversion: depend on abstractions; inject dependencies (Hilt).
- **DRY** — Don't Repeat Yourself: extract shared logic into utils, extensions, or base classes. Avoid premature abstraction.
- **KISS** — Keep It Simple: the simplest solution that works wins.
- **YAGNI** — You Aren't Gonna Need It: don't build for imaginary future requirements.
- **Separation of Concerns** — UI, business logic, and data layers must not leak into each other.

## 2. Architecture

- Layered: **Presentation → Domain → Data** (Clean Architecture + MVVM).
- UI layer is dumb: render state, forward events. No business logic in Activities/Fragments/Composables.
- Single source of truth for state; unidirectional data flow (UDF).
- Repository pattern for data access; abstract remote/local sources behind it.
- Dependency Injection everywhere — no manual instantiation of dependencies.

## 3. Kotlin / Java Conventions

- Prefer Kotlin for new code; `val` over `var`, immutability by default.
- Null safety: avoid `!!`; use `?.`, `?:`, `requireNotNull` with a message.
- `data class` for models, `sealed class`/`sealed interface` for state and events.
- Coroutines + Flow for async work; never block the main thread.
- Naming: `PascalCase` classes, `camelCase` functions/variables, `SCREAMING_SNAKE_CASE` constants.
- Keep functions small (< 30 lines as a guideline); one function, one job.
- No magic numbers/strings — extract to constants or resources.

## 4. UI & Resources

- All user-facing strings in `strings.xml` (localization-ready). No hardcoded text.
- Dimensions, colors in resources or design system tokens; support dark mode.
- Handle configuration changes properly — state survives rotation (ViewModel, SavedStateHandle).
- Design for all screen sizes; avoid fixed dimensions where flexible layouts work.

## 5. Error Handling & State

- Model UI state explicitly: `Loading / Success / Error` (sealed classes).
- Never swallow exceptions silently; log and surface user-friendly messages.
- Handle offline/poor network gracefully — timeouts, retries, cached fallbacks.

## 6. Performance

- No heavy work on the main thread (I/O, parsing, DB).
- Avoid memory leaks: no Activity/Context references in long-lived objects; cancel coroutines with lifecycle scopes.
- Lazy-load and paginate lists; use `DiffUtil` / stable keys in Compose.
- Optimize images (proper sizing, caching via Glide/Coil).

## 7. Testing

- Business logic (ViewModels, UseCases, Repositories) must be unit-testable — this is a design constraint.
- Unit tests for logic, instrumentation/UI tests for critical flows.
- Follow Arrange–Act–Assert; one behavior per test.

## 8. Code Quality & Process

- Enforce static analysis: ktlint/detekt, Lint. Fix warnings, don't suppress blindly.
- Small, focused commits and PRs; meaningful commit messages.
- Code review required before merge; no direct pushes to main.
- Boy Scout Rule: leave the code cleaner than you found it.

## 9. Security

- No secrets/API keys in source code or version control.
- Encrypted storage for sensitive data (EncryptedSharedPreferences, Keystore).
- HTTPS only; validate all input from external sources.
- Obfuscate release builds (R8/ProGuard).
