package com.liam.cmp_src

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.liam.cmp_src.image.setAvatarImageLoaderFactory
import com.liam.cmp_src.navigation.AppRoute
import com.liam.cmp_src.navigation.appNavConfiguration
import com.liam.cmp_src.navigation.resetTo
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.feature.auth.presentation.login.LoginRoute
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpRoute
import com.liam.cmp_src.feature.customers.editor.CustomerEditorRoute
import com.liam.cmp_src.feature.customers.list.CustomersRoute
import com.liam.cmp_src.feature.home.HomeRoute
import com.liam.cmp_src.feature.profile.ProfileRoute
import io.ktor.client.HttpClient
import org.koin.compose.koinInject

private const val ROOT_TRANSITION_MILLIS = 420
private const val ROOT_ENTER_SCALE = 0.94f
private const val ROOT_EXIT_SCALE = 1.04f

/**
 * Entry point shared by both platform shells (Android `MainActivity`, iOS
 * `MainViewController`).
 *
 * Expects the Koin graph to be running already — see `initKoin`, which each platform's entry point
 * calls before any UI. It is not started here because the customer sync can run with no UI at
 * all, and needs the same graph when it does.
 */
@Composable
fun App() {
    // Before anything can compose an avatar: Coil resolves its singleton loader on the first
    // image request and keeps whatever it found, so a later call would be ignored.
    setAvatarImageLoaderFactory(client = koinInject<HttpClient>(), config = koinInject())

    AppTheme {
        AppRoot()
    }
}

/**
 * The navigation host. The back stack is the single source of truth for what is on screen;
 * screens never navigate themselves, they report what happened and this decides where it goes.
 */
@Composable
private fun AppRoot() {
    val backStack = rememberNavBackStack(appNavConfiguration, AppRoute.Login)

    NavDisplay(
        backStack = backStack,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        entryDecorators = listOf(
            // Order matters: entry-scoped ViewModels need the saveable state holder in place to
            // hand out SavedStateHandles. Together they scope each screen's ViewModel to its
            // back-stack entry, so signing out disposes the login ViewModel with the entry.
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        // One cross-fade for every change, pushes and pops alike: it reads the same in both
        // directions, so the editor opening and closing needs no transition of its own.
        transitionSpec = { rootTransition() },
        popTransitionSpec = { rootTransition() },
        predictivePopTransitionSpec = { rootTransition() },
        entryProvider = entryProvider {
            entry<AppRoute.Login> {
                LoginRoute(
                    onSignedIn = { user -> backStack.resetTo(AppRoute.Home(user)) },
                    onSignUp = { backStack.add(AppRoute.SignUp) },
                )
            }

            entry<AppRoute.SignUp> {
                SignUpRoute(
                    goHome = { user -> backStack.resetTo(AppRoute.Home(user)) },
                    backToLogin = { backStack.removeLastOrNull() },
                )
            }

            entry<AppRoute.Home> { route ->
                // Signing out from the header or from the profile tab ends in the same place.
                val onSignedOut = { backStack.resetTo(AppRoute.Login) }
                HomeRoute(
                    user = route.user,
                    customersTab = {
                        CustomersRoute(
                            ownerId = route.user.id,
                            onOpenEditor = { customerId ->
                                backStack.add(AppRoute.CustomerEditor(route.user.id, customerId))
                            },
                        )
                    },
                    profileTab = { onProfileUpdated ->
                        ProfileRoute(onLogout = onSignedOut, onProfileUpdated = onProfileUpdated)
                    },
                )
            }

            entry<AppRoute.CustomerEditor> { route ->
                CustomerEditorRoute(
                    ownerId = route.ownerId,
                    customerId = route.customerId,
                    onDone = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

/** The one transition the app uses: the outgoing screen recedes as the incoming one settles in. */
private fun rootTransition(): ContentTransform =
    (fadeIn(tween(ROOT_TRANSITION_MILLIS)) +
            scaleIn(tween(ROOT_TRANSITION_MILLIS), initialScale = ROOT_ENTER_SCALE))
        .togetherWith(
            fadeOut(tween(ROOT_TRANSITION_MILLIS)) +
                    scaleOut(tween(ROOT_TRANSITION_MILLIS), targetScale = ROOT_EXIT_SCALE),
        )
