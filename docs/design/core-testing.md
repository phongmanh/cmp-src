# core:testing

Test doubles more than one module's tests use. Only ever a `commonTest` dependency. Exposes
`core:domain` and Compose Foundation (for `TextFieldState`).

## Classes

```mermaid
classDiagram
    direction TB
    class FakeAuthRepository {
        +emailResult: AuthResult
        +socialResult: AuthResult
        +registerResult: AuthResult
        +currentUserResult: AuthResult
        +changePasswordResult: AuthResult
        +lastEmail: String?
        +lastPassword: String?
        +lastProvider: SocialProvider?
        +lastCurrentPassword: String?
        +lastNewPassword: String?
        +emailCallCount: Int
        +socialCallCount: Int
        +signOutCallCount: Int
        +registerCallCount: Int
        +currentUserCallCount: Int
        +changePasswordCallCount: Int
        +TEST_USER: UserResponse$
    }
    class TextFieldStates {
        <<file>>
        +TextFieldState.type(text: String)
    }
    class AuthRepository {
        <<core:domain>>
    }
    class FormField {
        <<core:ui>>
    }

    FakeAuthRepository ..|> AuthRepository : implements
    TextFieldStates ..> FormField : types into its state, then sends apply notifications
```

## Sequences

### Typing in a ViewModel test

```mermaid
sequenceDiagram
    participant T as Test
    participant TS as TextFieldState (viewModel.email.state)
    participant FF as FormField
    participant VM as ViewModel
    T->>TS: type("a@b.co")
    TS->>TS: setTextAndPlaceCursorAtEnd
    TS->>FF: Snapshot.sendApplyNotifications() delivers the edit
    FF->>VM: onEdit(text)
    T->>VM: onAction(Submit)
    T->>T: assert on uiState and FakeAuthRepository.last*
```
