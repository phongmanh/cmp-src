package com.liam.cmp_src.feature.profile.changepassword

/** Everything the change-password dialog sends to its ViewModel — user input, plus being opened. */
sealed interface ChangePasswordAction {

    /**
     * The dialog has appeared. Fired on entry, not on every recomposition.
     *
     * This ViewModel outlives the dialog — it is scoped to the enclosing back-stack entry, not to
     * the dialog window — so without this a dismissed form would come back holding whatever was
     * typed into it, including a plaintext current password.
     */
    data object Opened : ChangePasswordAction

    data class CurrentPasswordChanged(val value: String) : ChangePasswordAction
    data class NewPasswordChanged(val value: String) : ChangePasswordAction
    data class ConfirmPasswordChanged(val value: String) : ChangePasswordAction
    data object ToggleCurrentVisibility : ChangePasswordAction
    data object ToggleNewVisibility : ChangePasswordAction
    data object Submit : ChangePasswordAction
    data object Cancel : ChangePasswordAction
}
