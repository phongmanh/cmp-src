package com.liam.cmp_src.feature.profile

/** One-shot effects the profile screen reports upwards. */
sealed interface ProfileEvent {
    /** The session has ended; the app should go back to sign-in. */
    data object GoToLogin : ProfileEvent

    /** The user tapped something this build does not implement yet. */
    data object ShowNotImplemented : ProfileEvent
}
