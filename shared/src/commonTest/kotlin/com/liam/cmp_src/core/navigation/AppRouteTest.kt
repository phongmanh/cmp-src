package com.liam.cmp_src.core.navigation

import androidx.navigation3.runtime.NavKey
import com.example.api.user.UserResponse
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Guards the back-stack serialization setup. Registration in `appNavConfiguration` is manual on
 * every target but Android, and a missing route only fails when a user restores the app onto that
 * destination — far from the change that caused it.
 *
 * Every [AppRoute] must appear in [ALL_ROUTES]; adding one here is the reminder to register it.
 */
@OptIn(ExperimentalSerializationApi::class)
class AppRouteTest {

    @Test
    fun `every route can be encoded as a NavKey`() {
        val module = appNavConfiguration.serializersModule

        ALL_ROUTES.forEach { route ->
            assertNotNull(
                module.getPolymorphic(NavKey::class, route),
                "${route::class.simpleName} is not registered in appNavConfiguration",
            )
        }
    }

    @Test
    fun `every route can be decoded back from its serial name`() {
        val module = appNavConfiguration.serializersModule

        ALL_ROUTES.forEach { route ->
            val serialName = module.getPolymorphic(NavKey::class, route)?.descriptor?.serialName
            assertNotNull(
                serialName?.let { module.getPolymorphic(NavKey::class, it) },
                "${route::class.simpleName} cannot be restored from the saved back stack",
            )
        }
    }

    private companion object {
        /** One instance of each [AppRoute], with every optional argument populated. */
        val ALL_ROUTES: List<AppRoute> = listOf(
            AppRoute.Login,
            AppRoute.Home(
                user = UserResponse(
                    id = "demo-user",
                    email = "demo@cmpsrc.dev",
                    displayName = "Demo User",
                    avatarUrl = "https://cmpsrc.dev/avatar.png",
                    isEmailVerified = true,
                    createdAt = "2026-01-01T00:00:00Z",
                    linkedProviders = listOf(SocialProvider.GOOGLE.key),
                ),
            ),
        )
    }
}
