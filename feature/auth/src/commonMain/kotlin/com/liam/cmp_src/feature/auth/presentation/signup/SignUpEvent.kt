package com.liam.cmp_src.feature.auth.presentation.signup

import com.example.api.user.UserResponse

sealed interface SignUpEvent {
    data class NavigateToHome(val user: UserResponse) : SignUpEvent
    data object NavigateBackToLogin : SignUpEvent
}