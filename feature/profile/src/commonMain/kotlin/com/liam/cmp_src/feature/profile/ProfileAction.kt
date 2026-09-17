package com.liam.cmp_src.feature.profile

import com.example.api.user.UserResponse

/** Everything the user can do on the profile screen. */
sealed interface ProfileAction {
    data object Logout : ProfileAction
    data object EditProfile : ProfileAction
    data object ChangePassword : ProfileAction

    /**
     * The edit dialog wrote [user], and this is what the server now holds.
     *
     * Adopted rather than re-read: the write already answered with the whole account, so a second
     * `GET /users/me` would spend a round trip to learn what is being handed over here.
     */
    data class UserUpdated(val user: UserResponse) : ProfileAction

    /** Retry the profile load after it failed. */
    data object Retry : ProfileAction
}
