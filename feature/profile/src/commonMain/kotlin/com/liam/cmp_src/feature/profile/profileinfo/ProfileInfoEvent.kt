package com.liam.cmp_src.feature.profile.profileinfo

import com.example.api.user.UserResponse

/** One-shot effects the edit-profile dialog reports upwards. */
sealed interface ProfileInfoEvent {

    /**
     * Something was saved, and [user] is the account as the server now holds it.
     *
     * Reported for every successful change rather than once at the end, because each of this
     * dialog's actions writes on its own. The dialog stays open: the caller's job here is to
     * refresh what is behind it, not to close it.
     */
    data class Updated(val user: UserResponse) : ProfileInfoEvent

    /** The user is finished with the dialog. Nothing is pending, and nothing is discarded. */
    data object Dismissed : ProfileInfoEvent
}
