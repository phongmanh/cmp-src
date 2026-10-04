---
name: add-destination
description: Adds a Navigation 3 destination to the app — the AppRoute key, its back-stack serialization registration, the AppRouteTest entry and the AppRoot entry. Use when adding a screen someone navigates to, or a new argument-carrying route.
argument-hint: <RouteName> [arguments]
---

# Add a navigation destination

Only `shared` knows the routes. The feature module provides a route composable that **reports** what happened (`onDone`, `onSignedOut`, …); `AppRoot` decides what that does to the back stack.

All three navigation files are in `shared/src/`:

- `commonMain/kotlin/com/liam/cmp_src/navigation/AppRoute.kt`
- `commonTest/kotlin/com/liam/cmp_src/navigation/AppRouteTest.kt`
- `commonMain/kotlin/com/liam/cmp_src/App.kt`

## Steps

1. **Key** — add a subtype to `sealed interface AppRoute` in `AppRoute.kt`:
   - `@Serializable data object` with no arguments, `@Serializable data class` with them. Every argument must be serializable (primitives, or `api-contract` DTOs such as `UserResponse`).
   - KDoc: where it is reached from, and whether it is **pushed** (back returns) or **handed over to** (nothing to go back to).

2. **Register it** — add `subclass(AppRoute.<Name>::class)` to `appNavConfiguration` in the same file. Only Android resolves back-stack keys by reflection; on iOS a missing line crashes when the back stack is restored after process death.

3. **Guard it** — add one instance to `ALL_ROUTES` in `AppRouteTest.kt`, with every optional argument populated. The test only checks routes listed there, so this line is what makes step 2 enforced.

4. **Entry** — add `entry<AppRoute.<Name>> { route -> … }` to `AppRoot`'s `entryProvider` in `App.kt`, calling the feature's `…Route` composable. Pick the transition by intent:

   | Intent | Call |
   |---|---|
   | Open on top, back returns | `backStack.add(AppRoute.<Name>(…))` |
   | Close this screen | `backStack.removeLastOrNull()` |
   | Handover — sign-in, sign-out | `backStack.resetTo(AppRoute.<Name>(…))` |

5. **Arguments into the ViewModel** — pass them to the route composable, which resolves its ViewModel with `koinViewModel { parametersOf(a, b) }`; the feature's Koin module declares `viewModel { (a: String, b: String?) -> …ViewModel(a, b, get(), …) }` (see `feature/customers/.../di/CustomersModule.kt`).

## Verify

```
./gradlew :shared:testAndroidHostTest --tests "com.liam.cmp_src.navigation.AppRouteTest"
./gradlew :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug
```
