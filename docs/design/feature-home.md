# feature:home

The signed-in shell: a glass top bar, a floating bottom navigation pill and whichever tab is
selected. The Customers and Profile tabs are slots `shared`'s `App.kt` fills, so this module never
imports another feature. Depends on `core:domain`, `core:ui` and `core:utils`.

## Classes

```mermaid
classDiagram
    direction TB
    class HomeRoute {
        <<composable>>
        +HomeRoute(user: UserResponse, customersTab: @Composable () -> Unit, profileTab: @Composable (onProfileUpdated: (UserResponse) -> Unit) -> Unit, modifier, viewModel: HomeViewModel)
        -currentUser: UserResponse (rememberSaveable, UserResponseSaver)
    }
    class HomeScreen {
        <<composable>>
        +HomeScreen(user: UserResponse, customersTab, profileTab, modifier)
        -selectedTab: HomeTab (rememberSaveable)
    }
    class HomeTopBar {
        <<composable>>
    }
    class HomeBottomBar {
        <<composable>>
    }
    class HomeViewModel {
        +events: SharedFlow~HomeEvent~
    }
    class HomeEvent {
        <<sealed>>
        no variants yet
    }
    class HomeTab {
        <<enumeration>>
        HOME
        CUSTOMERS
        ACTIVITY
        PROFILE
        +label: StringResource
        +icon: DrawableResource
        +isList: Boolean
        +Saver: Saver~HomeTab, String~$
    }
    class Platform {
        <<interface>>
        +name: String
    }
    class getPlatform {
        <<expect/actual>>
        +getPlatform() Platform
        AndroidPlatform : "Android SDK_INT"
        IOSPlatform : systemName + systemVersion
    }
    class homeModule {
        <<koin module>>
        viewModel HomeViewModel
    }
    class UserResponse {
        <<api-contract>>
    }
    class UserAvatar {
        <<core:ui>>
    }

    HomeRoute --> HomeViewModel : collects events (none handled yet)
    HomeRoute --> HomeScreen : renders with currentUser
    HomeScreen --> HomeTopBar
    HomeScreen --> HomeBottomBar : selects
    HomeScreen --> HomeTab : selectedTab
    HomeTopBar --> UserAvatar : shows user
    HomeViewModel ..> HomeEvent
    getPlatform ..> Platform : returns
    homeModule ..> HomeViewModel : binds
    HomeRoute --> UserResponse : seeded from AppRoute.Home
```

## Sequences

### Switch tabs and keep the account current

The `AppRoute.Home` key holds the user as it was at sign-in. `HomeRoute` keeps its own saveable
copy that the Profile tab updates, so a rename doesn't rebuild the entry and lose the tab.

```mermaid
sequenceDiagram
    actor U as User
    participant AR as AppRoot (shared)
    participant HR as HomeRoute
    participant HS as HomeScreen
    participant PT as profileTab slot (ProfileRoute)
    AR->>HR: HomeRoute(route.user, customersTab, profileTab)
    HR->>HR: currentUser = rememberSaveable(user)
    HR->>HS: HomeScreen(currentUser, customersTab, profileTab)
    U->>HS: tap Profile in the bottom bar
    HS->>HS: selectedTab = PROFILE (rememberSaveable)
    HS->>PT: compose profileTab()
    PT-->>HR: onProfileUpdated(updated)
    HR->>HR: currentUser = updated
    HR->>HS: recompose, top bar shows the new name and avatar
```
