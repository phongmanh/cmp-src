package com.liam.cmp_src.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.example.api.user.UserResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Every destination in the app, as Navigation 3 back-stack keys.
 *
 * Keys carry the arguments a destination needs, so a destination is fully described by the key
 * alone and nothing has to be threaded through the composable tree. They are saved and restored
 * with the back stack, which is why each one is `@Serializable` and registered in
 * [appNavConfiguration].
 */
sealed interface AppRoute : NavKey {

    /** Sign-in. The start destination, and the only one reachable while signed out. */
    @Serializable
    data object Login : AppRoute

    /** Where a successful sign-in lands, showing the [user] it signed in as. */
    @Serializable
    data class Home(val user: UserResponse) : AppRoute
}

/**
 * Serialization setup for saving and restoring the back stack across process death.
 *
 * `rememberNavBackStack` stores keys polymorphically as [NavKey], and only Android can resolve
 * the concrete types by reflection — every other target needs them registered up front. Adding
 * a destination to [AppRoute] means adding it here too, or restoring the back stack fails at
 * runtime on those targets (`AppRouteTest` guards this).
 */
internal val appNavConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Login::class)
            subclass(AppRoute.Home::class)
        }
    }
}

/**
 * Makes [route] the only entry on the back stack.
 *
 * Both of this app's transitions are handovers rather than pushes: signing in must not leave the
 * login screen behind to go back to, and signing out must not leave the home screen.
 */
internal fun NavBackStack<NavKey>.resetTo(route: AppRoute) {
    clear()
    add(route)
}
