package com.liam.cmp_src.feature.profile

/** Everything the user can do on the profile screen. */
sealed interface ProfileAction {
    data object Logout : ProfileAction
    data object EditProfile : ProfileAction
    data object ChangePassword : ProfileAction

    /** Retry the profile load after it failed. */
    data object Retry : ProfileAction
}
