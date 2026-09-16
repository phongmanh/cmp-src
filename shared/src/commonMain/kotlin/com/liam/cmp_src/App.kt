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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.liam.cmp_src.core.image.setAvatarImageLoaderFactory
import com.liam.cmp_src.core.navigation.AppRoute
import com.liam.cmp_src.core.navigation.appNavConfiguration
import com.liam.cmp_src.core.navigation.resetTo
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.di.appModule
import com.liam.cmp_src.di.rememberPlatformModule
import com.liam.cmp_src.feature.auth.presentation.login.LoginRoute
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpRoute
import com.liam.cmp_src.feature.home.HomeRoute
import io.ktor.client.HttpClient
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

private const val ROOT_TRANSITION_MILLIS = 420
private const val ROOT_ENTER_SCALE = 0.94f
private const val ROOT_EXIT_SCALE = 1.04f

/**
 * Entry point shared by both platform shells (Android `MainActivity`, iOS
 * `MainViewController`).
 *
 * Koin is started here rather than in each platform's entry point, so the two shells stay
 * identical and need no DI wiring of their own.
 */
@Composable
@Preview
fun App() {
    // The platform module carries the Android database builder, which needs a `Context` only a
    // composition can supply. Both it and the configuration are remembered, so Koin starts once.
    val platformModule = rememberPlatformModule()
    val configuration = remember(platformModule) {
        koinConfiguration { modules(appModule, platformModule) }
    }

    KoinApplication(configuration) {
        // Before anything can compose an avatar: Coil resolves its singleton loader on the first
        // image request and keeps whatever it found, so a later call would be ignored.
        setAvatarImageLoaderFactory(client = koinInject<HttpClient>(), config = koinInject())

        AppTheme {
            AppRoot()
        }
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
        // Every destination change here replaces the stack rather than pushing onto it, so the
        // same cross-fade reads correctly in both directions.
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
                HomeRoute(
                    user = route.user,
                    onSignedOut = { backStack.resetTo(AppRoute.Login) },
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
