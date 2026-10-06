# feature:auth

Sign-in (email/password and social), sign-up, and `AuthRepositoryImpl` — the network-backed
implementation of `core:domain`'s `AuthRepository` that every other feature reaches through the
interface. Depends on `core:domain`, `core:ui`, `core:utils` and `core:network`. Screens report
`onSignedIn`/`onSignUp`/`goHome`/`backToLogin`; `shared`'s `AppRoot` navigates.

## Classes

### Presentation

```mermaid
classDiagram
    direction TB
    class LoginRoute {
        <<composable>>
        +LoginRoute(onSignedIn: (UserResponse) -> Unit, onSignUp: () -> Unit, modifier, viewModel: LoginViewModel)
    }
    class LoginScreen {
        <<composable>>
        +LoginScreen(state: LoginUiState, email: TextFieldState, password: TextFieldState, onAction, snackbarHostState, modifier)
    }
    class LoginViewModel {
        +uiState: StateFlow~LoginUiState~
        +events: SharedFlow~LoginEvent~
        +email: FormField
        +password: FormField
        +onAction(action: LoginAction)
        -submitEmailAndPassword()
        -submitSocial(provider: SocialProvider)
        -handle(result: AuthResult)
        -clearAfterSignIn()
    }
    class LoginUiState {
        <<data>>
        +isPasswordVisible: Boolean
        +fieldErrors: CredentialErrors
        +status: LoginStatus
        +isBusy: Boolean
        +isSubmittingEmail: Boolean
        +submittingProvider: SocialProvider?
        +error: AuthError?
    }
    class LoginStatus {
        <<sealed>>
        Idle
        Submitting(provider: SocialProvider?)
        Failed(error: AuthError)
        Succeeded
    }
    class LoginAction {
        <<sealed>>
        ScreenEntered
        TogglePasswordVisibility
        Submit
        SocialSignInClicked(provider: SocialProvider)
        ForgotPasswordClicked
        SignUpClicked
    }
    class LoginEvent {
        <<sealed>>
        NavigateToHome(user: UserResponse)
        ShowNotImplemented
        SignUpClicked
    }
    class SignUpRoute {
        <<composable>>
        +SignUpRoute(goHome: (UserResponse) -> Unit, backToLogin: () -> Unit, modifier, viewModel: SignUpViewModel)
    }
    class SignUpScreen {
        <<composable>>
        +SignUpScreen(state: SignUpUiState, email: TextFieldState, password: TextFieldState, onAction, modifier)
    }
    class SignUpViewModel {
        +state: StateFlow~SignUpUiState~
        +effect: Flow~SignUpEvent~
        +email: FormField
        +password: FormField
        +onAction(action: SignUpAction)
        -onSubmit()
    }
    class SignUpUiState {
        <<data>>
        +status: SignUpUiStatus
        +isPasswordVisible: Boolean
        +fieldErrors: CredentialErrors
        +error: AuthError?
        +isBusy: Boolean
    }
    class SignUpUiStatus {
        <<sealed>>
        Idle
        Submitted
        Succeeded
        Failed(authError: AuthError)
    }
    class SignUpAction {
        <<sealed>>
        Submit
        NavigateBack
        TogglePasswordVisibility
    }
    class SignUpEvent {
        <<sealed>>
        NavigateToHome(user: UserResponse)
        NavigateBackToLogin
    }
    class LoginMessages {
        <<composable file>>
        +EmailError.asMessage() String
    }
    class FormField {
        <<core:ui>>
    }
    class SignInWithEmailUseCase
    class SignInWithSocialUseCase
    class SignUpUseCase
    class ValidateCredentialsUseCase

    LoginRoute --> LoginViewModel : collects uiState, events
    LoginRoute --> LoginScreen : renders
    LoginScreen ..> LoginAction : sends
    LoginScreen --> LoginMessages : field errors
    LoginViewModel *-- LoginUiState : owns
    LoginUiState *-- LoginStatus
    LoginViewModel ..> LoginEvent : emits
    LoginViewModel *-- FormField : email, password
    LoginViewModel --> SignInWithEmailUseCase : calls
    LoginViewModel --> SignInWithSocialUseCase : calls
    LoginViewModel --> ValidateCredentialsUseCase : calls
    SignUpRoute --> SignUpViewModel : collects state, effect
    SignUpRoute --> SignUpScreen : renders
    SignUpScreen ..> SignUpAction : sends
    SignUpViewModel *-- SignUpUiState : owns
    SignUpUiState *-- SignUpUiStatus
    SignUpViewModel ..> SignUpEvent : sends (Channel)
    SignUpViewModel *-- FormField : email, password
    SignUpViewModel --> SignUpUseCase : calls
    SignUpViewModel --> ValidateCredentialsUseCase : calls
```

### Domain and data

```mermaid
classDiagram
    direction TB
    class SignInWithEmailUseCase {
        -authRepository: AuthRepository
        +invoke(email: String, password: String) AuthResult
    }
    class SignInWithSocialUseCase {
        -authRepository: AuthRepository
        +invoke(provider: SocialProvider) AuthResult
    }
    class SignUpUseCase {
        -authRepository: AuthRepository
        +invoke(email: String, password: String) AuthResult
    }
    class ValidateCredentialsUseCase {
        +invoke(email: String, password: String) CredentialErrors
        +MIN_PASSWORD_LENGTH = 8$
    }
    class CredentialErrors {
        <<data>>
        +email: EmailError?
        +password: PasswordError?
        +hasErrors: Boolean
        +NONE$
    }
    class EmailError {
        <<sealed>>
        Blank
        Malformed
    }
    class AuthRepository {
        <<core:domain>>
    }
    class AuthRepositoryImpl {
        -authApi: AuthApi
        -socialAuthClient: SocialAuthClient
        -dispatcher: CoroutineDispatcher
        +signInWithEmail(email, password) AuthResult
        +signInWith(provider) AuthResult
        +signOut()
        +register(email, password) AuthResult
        +currentUser() AuthResult
        +changePassword(currentPassword, newPassword) AuthResult
        -ApiResult~TokenResponse~.toAuthResult(provider) AuthResult
        -UserResponse.withLinkedProviders() UserResponse
    }
    class AuthApi {
        -client: HttpClient
        -tokenStore: TokenStore
        +register(email, password, displayName) ApiResult~TokenResponse~
        +login(email, password) ApiResult~TokenResponse~
        +signInWithSocial(provider: ContractSocialProvider, token: String) ApiResult~TokenResponse~
        +currentUser() ApiResult~UserResponse~
        +logout() ApiResult~Unit~
        +changePassword(currentPassword, newPassword) ApiResult~TokenResponse~
        -authCall(request) ApiResult~TokenResponse~
    }
    class AuthMapping {
        <<file>>
        ~ApiError.toAuthError(provider: SocialProvider?) AuthError
        ~ApiError.toSessionError() AuthError
        ~ApiError.toChangePasswordError() AuthError
        ~SocialProvider.toContract() ContractSocialProvider
    }
    class SocialAuthClient {
        <<interface>>
        +requestCredential(provider: SocialProvider) SocialCredential
    }
    class SocialCredential {
        <<sealed>>
        Granted(token: String)
        Denied(error: AuthError)
    }
    class DemoSocialAuthClient {
        -signInDelayMillis: Long
        +DEMO_TOKEN_PREFIX$
    }
    class PlatformSocialAuthClients {
        <<expect/actual>>
        +createSocialAuthClient() SocialAuthClient
        AndroidSocialAuthClient, IosSocialAuthClient : delegate to Demo
    }
    class authModule {
        <<koin module>>
        single AuthApi, SocialAuthClient, AuthRepository
        factory use cases
        viewModel LoginViewModel, SignUpViewModel
    }
    class TokenStore {
        <<core:network>>
    }
    class HttpClient {
        <<core:network>>
    }

    SignInWithEmailUseCase --> AuthRepository : trims, lowercases email
    SignInWithSocialUseCase --> AuthRepository
    SignUpUseCase --> AuthRepository : trims, lowercases email
    ValidateCredentialsUseCase ..> CredentialErrors : returns
    CredentialErrors *-- EmailError
    AuthRepositoryImpl ..|> AuthRepository : implements
    AuthRepositoryImpl --> AuthApi : calls
    AuthRepositoryImpl --> SocialAuthClient : requests credential
    AuthRepositoryImpl --> AuthMapping : maps errors
    SocialAuthClient ..> SocialCredential : returns
    DemoSocialAuthClient ..|> SocialAuthClient : implements
    PlatformSocialAuthClients ..> DemoSocialAuthClient : delegates to
    AuthApi --> HttpClient : sends
    AuthApi --> TokenStore : save on success, clear on logout
    authModule ..> AuthRepositoryImpl : binds
```

## Sequences

### Sign in with email

From the Submit tap to `AppRoot` showing Home. Validation happens before any call; a failed
`GET /api/v1/users/me` after a successful login is not a failed sign-in.

```mermaid
sequenceDiagram
    actor U as User
    participant S as LoginRoute / LoginScreen
    participant VM as LoginViewModel
    participant V as ValidateCredentialsUseCase
    participant UC as SignInWithEmailUseCase
    participant R as AuthRepositoryImpl
    participant API as AuthApi
    participant TS as TokenStore
    participant SV as Server
    U->>S: tap Sign in
    S->>VM: onAction(Submit)
    VM->>VM: email.submit(), password.submit()
    VM->>V: invoke(email, password)
    alt invalid
        V-->>VM: CredentialErrors(hasErrors)
        VM-->>S: uiState.fieldErrors
    else valid
        VM-->>S: status = Submitting(null)
        Note over VM: viewModelScope.launch
        VM->>UC: invoke(email, password)
        UC->>R: signInWithEmail(trimmed lowercase email, password)
        Note over R: withContext(injected dispatcher)
        R->>API: login(email, password)
        API->>SV: POST /api/v1/auth/login LoginRequest
        alt 200
            SV-->>API: TokenResponse
            API->>TS: save(AuthTokens)
            API->>API: client.invalidateAuthCache()
            API-->>R: Success(TokenResponse)
            R->>API: currentUser()
            API->>SV: GET /api/v1/users/me
            SV-->>API: UserResponse with linkedProviders
            Note over R: on failure the token response's user stands in
            R-->>UC: AuthResult.Success(user)
            UC-->>VM: Success(user)
            VM-->>S: status = Succeeded
            VM->>VM: delay(SUCCESS_HOLD_MILLIS)
            VM-->>S: emit NavigateToHome(user)
            S->>S: onSignedIn(user), AppRoot resets to Home(user)
        else 401
            SV-->>API: 401 ErrorResponse
            API-->>R: Failure(Http 401)
            R-->>VM: Failure(InvalidCredentials)
            VM-->>S: status = Failed, ErrorBanner
        else offline or timeout
            API-->>R: Failure(Network or Timeout)
            R-->>VM: Failure(AuthError.Network)
            VM-->>S: status = Failed, ErrorBanner
        end
    end
```

### Sign in with a social provider

```mermaid
sequenceDiagram
    actor U as User
    participant VM as LoginViewModel
    participant UC as SignInWithSocialUseCase
    participant R as AuthRepositoryImpl
    participant SC as SocialAuthClient
    participant API as AuthApi
    participant SV as Server
    U->>VM: onAction(SocialSignInClicked(GOOGLE))
    VM->>VM: status = Submitting(GOOGLE)
    VM->>UC: invoke(GOOGLE)
    UC->>R: signInWith(GOOGLE)
    R->>SC: requestCredential(GOOGLE)
    Note over SC: both platforms delegate to DemoSocialAuthClient, a placeholder token
    alt Denied
        SC-->>R: Denied(error)
        R-->>VM: Failure(error)
    else Granted
        SC-->>R: Granted(token)
        R->>API: signInWithSocial(GOOGLE, token)
        API->>SV: POST /api/v1/auth/social SocialSignInRequest
        alt 200
            SV-->>API: TokenResponse (tokens saved as for email)
            R->>API: currentUser()
            R-->>VM: Success(user)
            VM-->>VM: Succeeded, then NavigateToHome(user)
        else PROVIDER_NOT_ENABLED
            R-->>VM: Failure(ProviderUnavailable(GOOGLE))
        else other HTTP, including 401
            R-->>VM: Failure(Unknown)
        end
    end
```

### Sign up

```mermaid
sequenceDiagram
    actor U as User
    participant S as SignUpRoute
    participant VM as SignUpViewModel
    participant V as ValidateCredentialsUseCase
    participant UC as SignUpUseCase
    participant R as AuthRepositoryImpl
    participant API as AuthApi
    participant SV as Server
    U->>S: tap Create account
    S->>VM: onAction(Submit)
    VM->>V: invoke(email.submit(), password.submit())
    alt invalid
        VM-->>S: fieldErrors
    else valid
        VM-->>S: status = Submitted
        VM->>UC: invoke(email, password)
        UC->>R: register(trimmed lowercase email, password)
        R->>API: register(email, password)
        API->>SV: POST /api/v1/auth/register RegisterRequest
        alt 2xx
            SV-->>API: TokenResponse, tokens saved
            R->>API: currentUser()
            R-->>VM: Success(user)
            VM-->>S: Succeeded, delay, effect NavigateToHome(user)
            S->>S: goHome(user), AppRoot resets to Home(user)
        else failure
            R-->>VM: Failure(error)
            VM-->>S: status = Failed(error)
        end
    end
    U->>S: tap back
    S->>VM: onAction(NavigateBack)
    VM-->>S: effect NavigateBackToLogin
    S->>S: backToLogin(), AppRoot pops
```

### Sign out (repository side)

```mermaid
sequenceDiagram
    participant UC as SignOutUseCase
    participant R as AuthRepositoryImpl
    participant API as AuthApi
    participant SV as Server
    participant TS as TokenStore
    UC->>R: signOut()
    Note over R: withContext(dispatcher + NonCancellable)
    R->>API: logout()
    API->>SV: POST /api/v1/auth/logout
    SV-->>API: any result, ignored
    API->>TS: clear()
    API->>API: client.invalidateAuthCache()
```
