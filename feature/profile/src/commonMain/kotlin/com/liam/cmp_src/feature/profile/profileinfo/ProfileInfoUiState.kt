package com.liam.cmp_src.feature.profile.profileinfo

import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.isStoredImageUrl
import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.feature.profile.domain.model.DisplayNameError

/**
 * Where the edit-profile dialog's work currently stands.
 *
 * The three in-flight cases are kept apart rather than collapsed into one "submitting", because
 * the dialog has three buttons and only the one that was pressed should show it is working.
 */
sealed interface ProfileInfoStatus {
    data object Idle : ProfileInfoStatus
    data object SavingName : ProfileInfoStatus
    data object UploadingPhoto : ProfileInfoStatus
    data object RemovingPhoto : ProfileInfoStatus
    data object Succeeded : ProfileInfoStatus
    data class Failed(val error: AuthError) : ProfileInfoStatus
}

/**
 * Everything the edit-profile dialog renders.
 *
 * Same shape as `ChangePasswordUiState`, and for the same reason: the form keeps rendering its
 * field and its buttons in every state, so this is a data class carrying a sealed status rather
 * than being a sealed hierarchy itself.
 *
 * [user] is the account as last written, so the avatar preview and the warning below both follow
 * a save without the dialog having to be reopened.
 *
 * [typedName] is a read-only copy of [ProfileInfoViewModel.displayName], the field's own state, kept
 * so that whether there is anything to save can be worked out here rather than in the dialog.
 */
data class ProfileInfoUiState(
    val user: UserResponse? = null,
    val typedName: String = "",
    val nameError: DisplayNameError? = null,
    val status: ProfileInfoStatus = ProfileInfoStatus.Idle,
) {
    /** True from the moment a call starts until its confirmation has been shown. */
    val isBusy: Boolean
        get() = status != ProfileInfoStatus.Idle && status !is ProfileInfoStatus.Failed

    val error: AuthError?
        get() = (status as? ProfileInfoStatus.Failed)?.error

    /** Nothing to save while the field still holds what the server already has. */
    val isNameDirty: Boolean
        get() = typedName.trim() != user?.displayName.orEmpty()

    /** Whether there is a picture to take away. */
    val hasPhoto: Boolean
        get() = !user?.avatarUrl.isNullOrBlank()

    /**
     * Whether saving the name would also destroy the picture.
     *
     * True only for a picture uploaded to this backend: `PUT /users/me` retires one of those
     * whatever it is sent, while an address from a social provider is echoed back and survives.
     * The warning is shown only when it is actually true, so it never cries wolf.
     */
    val willClearPhoto: Boolean
        get() = user?.avatarUrl?.let(::isStoredImageUrl) == true
}
