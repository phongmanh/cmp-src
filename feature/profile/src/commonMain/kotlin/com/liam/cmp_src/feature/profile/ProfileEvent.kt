package com.liam.cmp_src.feature.profile

/** One-shot effects the profile screen reports upwards. */
sealed interface ProfileEvent {
    /** The session has ended; the app should go back to sign-in. */
    data object GoToLogin : ProfileEvent

    /** Put the change-password dialog on screen. */
    data object OpenChangePassword : ProfileEvent

    /** Put the edit-profile dialog on screen. */
    data object OpenEditProfile : ProfileEvent
}
