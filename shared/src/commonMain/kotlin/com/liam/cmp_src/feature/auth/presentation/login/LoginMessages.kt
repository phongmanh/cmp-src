package com.liam.cmp_src.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.DisplayNameError
import com.liam.cmp_src.feature.auth.domain.model.EmailError
import com.liam.cmp_src.feature.auth.domain.model.PasswordError
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.error_cancelled
import cmpsrc.shared.generated.resources.error_image_too_large
import cmpsrc.shared.generated.resources.error_invalid_credentials
import cmpsrc.shared.generated.resources.error_network
import cmpsrc.shared.generated.resources.error_password_not_set
import cmpsrc.shared.generated.resources.error_provider_unavailable
import cmpsrc.shared.generated.resources.error_too_many_uploads
import cmpsrc.shared.generated.resources.error_unknown
import cmpsrc.shared.generated.resources.error_unsupported_image
import cmpsrc.shared.generated.resources.error_wrong_current_password
import cmpsrc.shared.generated.resources.login_facebook
import cmpsrc.shared.generated.resources.login_google
import cmpsrc.shared.generated.resources.validation_display_name_too_long
import cmpsrc.shared.generated.resources.validation_email_blank
import cmpsrc.shared.generated.resources.validation_email_malformed
import cmpsrc.shared.generated.resources.validation_password_blank
import cmpsrc.shared.generated.resources.validation_password_mismatch
import cmpsrc.shared.generated.resources.validation_password_same_as_current
import cmpsrc.shared.generated.resources.validation_password_too_long
import cmpsrc.shared.generated.resources.validation_password_too_short
import org.jetbrains.compose.resources.stringResource

/** The cap on an upload is published in bytes; nobody reads a size in bytes. */
private const val BYTES_PER_MEGABYTE = 1024 * 1024

/**
 * Turns the domain's error types into display text.
 *
 * This mapping lives in the presentation layer on purpose: the domain stays free of
 * localization concerns, and every user-facing sentence resolves through a string resource.
 */

@Composable
fun AuthError.asMessage(): String = when (this) {
    AuthError.InvalidCredentials -> stringResource(Res.string.error_invalid_credentials)
    AuthError.WrongPassword -> stringResource(Res.string.error_wrong_current_password)
    AuthError.PasswordNotSet -> stringResource(Res.string.error_password_not_set)
    AuthError.UnsupportedImage -> stringResource(Res.string.error_unsupported_image)
    is AuthError.ImageTooLarge ->
        stringResource(Res.string.error_image_too_large, (maxBytes / BYTES_PER_MEGABYTE).toInt())

    AuthError.TooManyUploads -> stringResource(Res.string.error_too_many_uploads)
    AuthError.Network -> stringResource(Res.string.error_network)
    AuthError.Cancelled -> stringResource(Res.string.error_cancelled)
    is AuthError.ProviderUnavailable ->
        stringResource(Res.string.error_provider_unavailable, provider.asLabel())

    is AuthError.Unknown -> message ?: stringResource(Res.string.error_unknown)
}

@Composable
fun DisplayNameError.asMessage(): String = when (this) {
    is DisplayNameError.TooLong ->
        stringResource(Res.string.validation_display_name_too_long, maxLength)
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
    is PasswordError.TooLong -> stringResource(Res.string.validation_password_too_long, maxLength)
    PasswordError.SameAsCurrent -> stringResource(Res.string.validation_password_same_as_current)
    PasswordError.Mismatch -> stringResource(Res.string.validation_password_mismatch)
}

@Composable
fun SocialProvider.asLabel(): String = when (this) {
    SocialProvider.GOOGLE -> stringResource(Res.string.login_google)
    SocialProvider.FACEBOOK -> stringResource(Res.string.login_facebook)
}
