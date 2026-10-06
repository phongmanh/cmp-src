# core:domain

The account types every feature shares, the `AuthRepository` boundary and `SignOutUseCase`. Pure
Kotlin. Depends only on `api-contract`, whose `UserResponse` is the signed-in user end to end.
`AuthRepositoryImpl` lives in `feature:auth`; `FakeAuthRepository` in `core:testing`.

## Classes

```mermaid
classDiagram
    direction TB
    class AuthRepository {
        <<interface>>
        +signInWithEmail(email: String, password: String) AuthResult
        +signInWith(provider: SocialProvider) AuthResult
        +signOut()
        +register(email: String, password: String) AuthResult
        +currentUser() AuthResult
        +changePassword(currentPassword: String, newPassword: String) AuthResult
    }
    class SignOutUseCase {
        -authRepository: AuthRepository
        +invoke()
    }
    class AuthResult {
        <<sealed>>
        Success(user: UserResponse)
        Failure(error: AuthError)
    }
    class AuthError {
        <<sealed>>
        InvalidCredentials
        WrongPassword
        PasswordNotSet
        UnsupportedImage
        ImageTooLarge(maxBytes: Long)
        TooManyUploads
        Network
        Cancelled
        ProviderUnavailable(provider: SocialProvider)
        Unknown(message: String?)
    }
    class PasswordError {
        <<sealed>>
        Blank
        TooShort(minLength: Int)
        TooLong(maxLength: Int)
        SameAsCurrent
        Mismatch
    }
    class SocialProvider {
        <<enumeration>>
        GOOGLE
        FACEBOOK
        +key: String
        +fromKey(key: String)$ SocialProvider?
    }
    class UserResponse {
        <<api-contract>>
        +id: String
        +email: String?
        +displayName: String?
        +avatarUrl: String?
        +isEmailVerified: Boolean
        +createdAt: String
        +linkedProviders: List~String~
    }
    class ContractSocialProvider {
        <<api-contract>>
    }

    SignOutUseCase --> AuthRepository : calls signOut
    AuthRepository ..> AuthResult : returns
    AuthResult *-- UserResponse : Success carries
    AuthResult *-- AuthError : Failure carries
    AuthError --> SocialProvider : ProviderUnavailable names
    SocialProvider --> ContractSocialProvider : key maps to
```

## Sequences

### Sign out

`ProfileViewModel` (or any caller) signs out; the local session is gone whether or not the server
answered. The call continues even if the calling screen is torn down.

```mermaid
sequenceDiagram
    participant C as Caller (ProfileViewModel)
    participant UC as SignOutUseCase
    participant R as AuthRepository
    C->>UC: invoke()
    UC->>R: signOut()
    Note over R: AuthRepositoryImpl runs it on the injected dispatcher + NonCancellable
    R-->>UC: Unit
    UC-->>C: Unit
```
