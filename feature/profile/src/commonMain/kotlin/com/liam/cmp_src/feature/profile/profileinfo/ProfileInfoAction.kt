package com.liam.cmp_src.feature.profile.profileinfo

import com.example.api.user.UserResponse

/**
 * Everything the edit-profile dialog sends to its ViewModel, other than typing — the name field's
 * text lives in [ProfileInfoViewModel.displayName], which the field edits directly.
 */
sealed interface ProfileInfoAction {

    /**
     * The dialog has appeared, carrying the account the profile screen has already loaded.
     *
     * Fired on entry, not on every recomposition. This ViewModel outlives the dialog — it is
     * scoped to the enclosing back-stack entry, not to the dialog window — so without this a
     * reopened form would come back holding whatever the last one was left in.
     */
    data class Opened(val user: UserResponse) : ProfileInfoAction

    /** Write the typed name. This is the action that also clears an uploaded picture. */
    data object SaveName : ProfileInfoAction

    /**
     * A file came back from the picker.
     *
     * A plain class rather than a `data class`: [bytes] is an array, and generated equality over
     * one compares references, which would quietly mean something other than it appears to.
     */
    class PhotoPicked(val bytes: ByteArray, val fileName: String) : ProfileInfoAction

    data object RemovePhoto : ProfileInfoAction

    /** The user is finished. Everything they did has already been saved. */
    data object Close : ProfileInfoAction
}
