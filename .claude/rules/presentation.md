---
paths:
  - "core/ui/**"
  - "feature/**/presentation/**"
  - "feature/**/component/**"
  - "feature/**/*{Screen,ViewModel,UiState,Action,Event,Dialog,Route}.kt"
  - "feature/**/*ViewModelTest.kt"
  - "shared/src/commonMain/**/App.kt"
---

# Presentation layer

Screens, ViewModels and their state in each `feature:*` module, and the shared UI in `core:ui`.

- Composables render state and forward events. Business logic lives behind the ViewModel, where a test can reach it.
- A ViewModel exposes its state as one `StateFlow<UiState>` and one-off events as a `SharedFlow` — `MutableSharedFlow(extraBufferCapacity = 1)`, as the customers ViewModels do.
- One `UiState` per screen: a `sealed interface` with at least `Loading`, `Success` and `Error` for a screen that loads data; a `data class` whose inputs are `FormField`s for a form.
- ViewModels call use cases, never repositories. The use case is where the business rule lives and gets its own test.
- Screens never navigate. The route composable reports what happened (`onSignedIn`, `onSignedOut`) and `AppRoot` changes the back stack.
- Every new composable has a `@Preview`. Text comes from string resources.

## Testing

- ViewModels: build them with real use cases over a fake repository (`core:testing`'s `FakeAuthRepository` is the model), type with `TextFieldState.type()`, and assert the state transitions.
- Critical user flows get a Compose UI test (`runComposeUiTest`) in `commonTest`. None exists yet — the first one sets the pattern.
