package com.liam.cmp_src.feature.auth.presentation

import androidx.compose.runtime.Composable
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.EmailError
import com.liam.cmp_src.feature.auth.domain.model.PasswordError
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.error_cancelled
import cmpsrc.shared.generated.resources.error_invalid_credentials
import cmpsrc.shared.generated.resources.error_network
import cmpsrc.shared.generated.resources.error_provider_unavailable
import cmpsrc.shared.generated.resources.error_unknown
import cmpsrc.shared.generated.resources.login_facebook
import cmpsrc.shared.generated.resources.login_google
import cmpsrc.shared.generated.resources.validation_email_blank
import cmpsrc.shared.generated.resources.validation_email_malformed
import cmpsrc.shared.generated.resources.validation_password_blank
import cmpsrc.shared.generated.resources.validation_password_too_short
import org.jetbrains.compose.resources.stringResource

/**
 * Turns the domain's error types into display text.
 *
 * This mapping lives in the presentation layer on purpose: the domain stays free of
 * localization concerns, and every user-facing sentence resolves through a string resource.
 */

@Composable
fun AuthError.asMessage(): String = when (this) {
    AuthError.InvalidCredentials -> stringResource(Res.string.error_invalid_credentials)
    AuthError.Network -> stringResource(Res.string.error_network)
    AuthError.Cancelled -> stringResource(Res.string.error_cancelled)
    is AuthError.ProviderUnavailable ->
        stringResource(Res.string.error_provider_unavailable, provider.asLabel())

    is AuthError.Unknown -> stringResource(Res.string.error_unknown)
}

@Composable
fun EmailError.asMessage(): String = when (this) {
    EmailError.Blank -> stringResource(Res.string.validation_email_blank)
    EmailError.Malformed -> stringResource(Res.string.validation_email_malformed)
}

@Composable
fun PasswordError.asMessage(): String = when (this) {
    PasswordError.Blank -> stringResource(Res.string.validation_password_blank)
    is PasswordError.TooShort -> stringResource(Res.string.validation_password_too_short, minLength)
}

@Composable
fun SocialProvider.asLabel(): String = when (this) {
    SocialProvider.GOOGLE -> stringResource(Res.string.login_google)
    SocialProvider.FACEBOOK -> stringResource(Res.string.login_facebook)
}
