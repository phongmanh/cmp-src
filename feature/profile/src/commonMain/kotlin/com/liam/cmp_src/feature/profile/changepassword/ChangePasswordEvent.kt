package com.liam.cmp_src.feature.profile.changepassword

/** One-shot effects the change-password dialog reports upwards. */
sealed interface ChangePasswordEvent {

    /** The user backed out. Nothing was changed. */
    data object Dismissed : ChangePasswordEvent

    /**
     * The password was replaced. The session survives — the server issued a fresh token pair and
     * the data layer adopted it — so this closes the dialog rather than ending the session.
     */
    data object Changed : ChangePasswordEvent
}
