package com.liam.cmp_src.feature.auth.presentation.signup

sealed interface SignUpAction {
    data class Submit(val email: String, val password: String) : SignUpAction
    data object NavigateBack : SignUpAction
    data class EmailChanged(val value: String) : SignUpAction
    data class PasswordChanged(val value: String) : SignUpAction
}