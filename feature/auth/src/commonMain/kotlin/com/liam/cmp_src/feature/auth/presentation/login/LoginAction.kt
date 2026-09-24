package com.liam.cmp_src.feature.auth.presentation.login

import com.liam.cmp_src.core.domain.model.SocialProvider

/**
 * Everything the login screen sends to its ViewModel, other than typing — the fields' text lives in
 * the ViewModel's `TextFieldState`s, which the fields edit directly.
 */
sealed interface LoginAction {
    /** The screen has (re-)appeared. Fired on entry, not on every recomposition. */
    data object ScreenEntered : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data object Submit : LoginAction
    data class SocialSignInClicked(val provider: SocialProvider) : LoginAction
    data object ForgotPasswordClicked : LoginAction
    data object SignUpClicked : LoginAction
}
