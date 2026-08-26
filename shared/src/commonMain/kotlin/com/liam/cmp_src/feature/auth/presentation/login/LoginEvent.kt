package com.liam.cmp_src.feature.auth.presentation.login

import com.example.api.user.UserResponse


/**
 * One-shot effects, delivered over a `SharedFlow` so they fire once and are not replayed
 * when the screen recomposes or the app is rotated.
 */
sealed interface LoginEvent {
    data class NavigateToHome(val user: UserResponse) : LoginEvent

    /** A secondary action ("forgot password", "sign up") that has no destination yet. */
    data object ShowNotImplemented : LoginEvent

    /**
     * Navigate to sign up screen
     */
    data object SignUpClicked : LoginEvent
}
