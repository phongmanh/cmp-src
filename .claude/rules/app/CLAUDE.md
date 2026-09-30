# App Module (Presentation Layer)

Scope: the presentation code in each `feature:*` module (screens, ViewModels) and `core:ui`. Merged with root `CLAUDE.md`.

## Rules
- Activities/Fragments/Composables contain NO business logic — delegate to ViewModel.
- ViewModel exposes state via `StateFlow`, one-time events via `SharedFlow` (never `LiveData` in new code).
- UI state is a single `UiState` type per screen: a `sealed interface` (`Loading / Success / Error` at minimum) for a screen that loads data, a `data class` for a form screen whose inputs are `FormField`s.
- Screens never navigate themselves: a route composable reports what happened and `AppRoot` in `shared` changes the back stack (root `CLAUDE.md` → Navigation).
- Do not inject Repositories directly into ViewModels — always go through a UseCase.
- Preview functions (`@Preview`) required for all new Composables.
- String resources only — never `Text("literal")`.

## Testing
- ViewModels: unit test with fake/mocked UseCases, verify state transitions.
- Critical user flows: instrumentation test with Compose testing APIs.
