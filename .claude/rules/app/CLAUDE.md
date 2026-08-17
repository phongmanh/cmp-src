# App Module (Presentation Layer)

Scope: everything under `app/`. Merged with root `CLAUDE.md`.

## Rules
- Activities/Fragments/Composables contain NO business logic — delegate to ViewModel.
- ViewModel exposes state via `StateFlow`, one-time events via `SharedFlow` (never `LiveData` in new code).
- UI state is a single `sealed interface UiState` per screen: `Loading / Success / Error` at minimum.
- Navigation logic lives in the navigation graph or a dedicated Navigator class, never inline in Composables.
- Do not inject Repositories directly into ViewModels — always go through a UseCase.
- Preview functions (`@Preview`) required for all new Composables.
- String resources only — never `Text("literal")`.

## Testing
- ViewModels: unit test with fake/mocked UseCases, verify state transitions.
- Critical user flows: instrumentation test with Compose testing APIs.
