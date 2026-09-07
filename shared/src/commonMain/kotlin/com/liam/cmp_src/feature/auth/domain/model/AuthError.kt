package com.liam.cmp_src.feature.auth.domain.model

/**
 * Everything that can go wrong while signing in, as a closed set.
 *
 * These carry no user-facing text on purpose — the presentation layer maps each case to a
 * string resource, so the domain stays localization-agnostic and testable without resources.
 */
sealed interface AuthError {

    /** Email/password pair did not match an account. */
    data object InvalidCredentials : AuthError

    /**
     * The current password given to a change-password attempt was wrong.
     *
     * Distinct from [InvalidCredentials]: the caller is already signed in and never typed an
     * email, so telling them "that email and password don't match" would name a field they did
     * not fill in.
     */
    data object WrongPassword : AuthError

    /**
     * The account signs in through a provider only, so it has no password to replace. Reaching a
     * password from here needs a set-password flow, which this build does not have.
     */
    data object PasswordNotSet : AuthError

    /** Request could not reach the auth backend. */
    data object Network : AuthError

    /** User dismissed the provider's sign-in sheet. Not an error to shout about. */
    data object Cancelled : AuthError

    /**
     * The provider cannot be used here: either no SDK is wired up on this platform yet, or the
     * deployment the app is talking to has no credentials for it.
     */
    data class ProviderUnavailable(val provider: SocialProvider) : AuthError

    /** Anything unclassified. [message] is for logs, never for the UI. */
    data class Unknown(val message: String? = null) : AuthError
}
