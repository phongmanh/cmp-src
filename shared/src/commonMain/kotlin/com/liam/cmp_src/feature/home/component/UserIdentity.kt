package com.liam.cmp_src.feature.home.component

import androidx.compose.runtime.Composable
import com.example.api.user.UserResponse
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.home_user_unnamed
import org.jetbrains.compose.resources.stringResource

/** Stands in for the account's creation time wherever a `@Preview` or a test needs one. */
private const val SAMPLE_CREATED_AT = "2026-01-01T00:00:00Z"

/**
 * What to call the signed-in user on screen.
 *
 * The contract publishes both the display name and the email as nullable — a social account can
 * arrive with neither — so every place that names the user needs the same fallback chain, and it
 * lives here rather than at each call site.
 */
@Composable
internal fun UserResponse.displayLabel(): String =
    displayName?.takeIf { it.isNotBlank() }
        ?: email?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.home_user_unnamed)

/**
 * The provider this account signed in through, or null for an email/password account.
 *
 * `linkedProviders` carries contract wire keys and is filled by `GET /users/me` rather than by a
 * token response, so an empty list means "not known here", not "no linked provider".
 */
internal fun UserResponse.signedInProvider(): SocialProvider? =
    linkedProviders.firstNotNullOfOrNull(SocialProvider::fromKey)

/**
 * A stand-in user for `@Preview`s and tests, which have no repository to sign in against.
 *
 * `UserResponse` has seven required fields and no defaults — deliberately, since it mirrors what
 * the server sends — so building one inline four times over would bury what each case is actually
 * varying.
 */
internal fun sampleUser(
    id: String = "demo-user",
    displayName: String? = "Demo User",
    email: String? = "demo@cmpsrc.dev",
    avatarUrl: String? = null,
    linkedProviders: List<String> = emptyList(),
): UserResponse = UserResponse(
    id = id,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    isEmailVerified = true,
    createdAt = SAMPLE_CREATED_AT,
    linkedProviders = linkedProviders,
)
