package com.liam.cmp_src.feature.profile.changepassword

/**
 * Everything the change-password dialog sends to its ViewModel, other than typing — the fields'
 * text lives in the ViewModel's `TextFieldState`s, which the fields edit directly.
 */
sealed interface ChangePasswordAction {

    /**
     * The dialog has appeared. Fired on entry, not on every recomposition.
     *
     * This ViewModel outlives the dialog — it is scoped to the enclosing back-stack entry, not to
     * the dialog window — so without this a dismissed form would come back holding whatever was
     * typed into it, including a plaintext current password.
     */
    data object Opened : ChangePasswordAction

    data object ToggleCurrentVisibility : ChangePasswordAction
    data object ToggleNewVisibility : ChangePasswordAction
    data object Submit : ChangePasswordAction
    data object Cancel : ChangePasswordAction
}
