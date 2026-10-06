# shared

The app module, and the only place features meet: `App()` and `AppRoot` (Navigation 3 back stack
and routes), the Koin graph (`appModule` plus a platform module that builds Room and picks the
token cipher), the Coil avatar `ImageLoader`, and the iOS entry points. Depends on every `core`
and `feature` module. The platform shells — `androidApp` (`CmpApplication`, `MainActivity`) and
`iosApp` (`iOSApp`, `ContentView`) — only call into it, and are drawn here too.

## Classes

```mermaid
classDiagram
    direction TB
    class App {
        <<composable>>
        +App()
        -AppRoot()
        -rootTransition() ContentTransform
    }
    class AppRoute {
        <<sealed NavKey>>
        Login
        SignUp
        Home(user: UserResponse)
        CustomerEditor(ownerId: String, customerId: String?)
    }
    class Navigation {
        <<file>>
        ~appNavConfiguration: SavedStateConfiguration
        ~NavBackStack.resetTo(route: AppRoute)
    }
    class appModule {
        <<koin module>>
        includes authModule, homeModule, profileModule, customersModule
        single CoroutineDispatcher = Dispatchers.Default
        single ApiConfig
        single HttpClient = createHttpClient(tokenStore, config)
        factory SignOutUseCase
    }
    class PlatformModule {
        <<file>>
        +initKoin(platformModule: Module)
        +Module.roomPersistence(builder: () -> Builder~AppDatabase~, cipher: () -> TokenCipher)
        +androidPlatformModule(context: Context) Module — androidMain
        +initAndroidApp(context: Context) — androidMain
        +iosPlatformModule() Module — iosMain
    }
    class AvatarImageLoader {
        <<file>>
        +setAvatarImageLoaderFactory(client: HttpClient, config: ApiConfig)
        ~avatarImageLoaderFactory(context, client, config) ImageLoader
    }
    class AppSetup {
        <<iosMain file>>
        +setUpApp()
        +MainViewController() UIViewController
    }
    class CmpApplication {
        <<androidApp>>
        +onCreate()
    }
    class MainActivity {
        <<androidApp>>
        +onCreate(savedInstanceState: Bundle?)
    }
    class iOSApp {
        <<iosApp Swift>>
        init() calls setUpApp
        ContentView wraps MainViewController
    }
    class LoginRoute {
        <<feature:auth>>
    }
    class SignUpRoute {
        <<feature:auth>>
    }
    class HomeRoute {
        <<feature:home>>
    }
    class CustomersRoute {
        <<feature:customers>>
    }
    class CustomerEditorRoute {
        <<feature:customers>>
    }
    class ProfileRoute {
        <<feature:profile>>
    }
    class RoomTokenStore {
        <<core:database>>
    }
    class TokenCipher {
        <<core:security>>
    }

    CmpApplication --> PlatformModule : initAndroidApp
    MainActivity --> App : setContent
    iOSApp --> AppSetup : setUpApp, MainViewController
    AppSetup --> PlatformModule : initKoin(iosPlatformModule())
    PlatformModule --> appModule : startKoin with
    PlatformModule ..> RoomTokenStore : binds TokenStore
    PlatformModule ..> TokenCipher : Keystore or Keychain
    App --> AvatarImageLoader : before any avatar
    App --> AppRoute : back stack of
    App --> Navigation : config, resetTo
    App --> LoginRoute : entry Login
    App --> SignUpRoute : entry SignUp
    App --> HomeRoute : entry Home
    App --> CustomersRoute : fills customersTab
    App --> ProfileRoute : fills profileTab
    App --> CustomerEditorRoute : entry CustomerEditor
```

## Sequences

### Start the app

```mermaid
sequenceDiagram
    participant OS as Android or iOS
    participant SH as CmpApplication / iOSApp.init
    participant PM as PlatformModule
    participant K as Koin
    participant UI as MainActivity / ContentView
    participant A as App()
    OS->>SH: launch
    alt Android
        SH->>PM: initAndroidApp(context)
        PM->>K: initKoin(androidPlatformModule(appContext)), Room + KeystoreTokenCipher
    else iOS
        SH->>PM: setUpApp()
        PM->>K: initKoin(iosPlatformModule()), Room + KeychainTokenCipher
        SH->>SH: registerCustomerBackgroundSync()
    end
    Note over K: startKoin(appModule, platformModule), skipped if already started
    OS->>UI: first screen
    UI->>A: App()
    A->>A: setAvatarImageLoaderFactory(HttpClient, ApiConfig)
    A->>A: AppTheme, then AppRoot with back stack [Login]
```

### Navigate

Screens report outcomes; `AppRoot` owns the back stack. Each entry gets its own saveable state and
`ViewModelStore`, so leaving an entry disposes its ViewModels.

```mermaid
sequenceDiagram
    participant AR as AppRoot
    participant L as LoginRoute
    participant SU as SignUpRoute
    participant H as HomeRoute
    participant C as CustomersRoute
    participant E as CustomerEditorRoute
    participant P as ProfileRoute
    AR->>L: entry Login
    alt signed in
        L-->>AR: onSignedIn(user)
        AR->>AR: resetTo(Home(user))
    else sign up
        L-->>AR: onSignUp()
        AR->>AR: add(SignUp)
        SU-->>AR: goHome(user), resetTo(Home(user)) or backToLogin(), removeLast
    end
    AR->>H: entry Home(user), customersTab = CustomersRoute(user.id), profileTab = ProfileRoute
    C-->>AR: onOpenEditor(customerId or null)
    AR->>AR: add(CustomerEditor(user.id, customerId))
    AR->>E: entry CustomerEditor
    E-->>AR: onDone()
    AR->>AR: removeLastOrNull()
    P-->>AR: onLogout()
    AR->>AR: resetTo(Login)
```
