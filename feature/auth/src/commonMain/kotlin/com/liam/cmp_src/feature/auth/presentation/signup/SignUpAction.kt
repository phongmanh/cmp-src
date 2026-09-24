package com.liam.cmp_src.feature.auth.presentation.signup

/**
 * Everything the sign-up screen sends to its ViewModel, other than typing — the fields' text lives
 * in the ViewModel's `TextFieldState`s, which the fields edit directly.
 */
sealed interface SignUpAction {
    data object Submit : SignUpAction
    data object NavigateBack : SignUpAction
    data object TogglePasswordVisibility : SignUpAction
}
