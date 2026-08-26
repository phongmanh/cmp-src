package com.liam.cmp_src.feature.auth.presentation.login

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/** Everything the login screen sends to its ViewModel — user input, plus screen entry. */
sealed interface LoginAction {
    /** The screen has (re-)appeared. Fired on entry, not on every recomposition. */
    data object ScreenEntered : LoginAction
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data object Submit : LoginAction
    data class SocialSignInClicked(val provider: SocialProvider) : LoginAction
    data object ForgotPasswordClicked : LoginAction
    data object SignUpClicked : LoginAction
}
