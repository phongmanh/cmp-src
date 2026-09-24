package com.liam.cmp_src.feature.profile.profileinfo

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.SUCCESS_HOLD_MILLIS
import com.liam.cmp_src.core.ui.input.FormField
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.domain.usecase.RemoveAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UpdateDisplayNameUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UploadAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.ValidateDisplayNameUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds the edit-profile dialog's state and runs its three writes.
 *
 * The name and the picture are saved independently, because the API gives no way to save them
 * together: `PUT /users/me` replaces the whole profile and retires an uploaded picture on the way
 * past, while the picture has routes of its own that leave the name alone. Pretending they were
 * one form would mean silently destroying a photo every time somebody fixed a typo in their name.
 *
 * So this ViewModel never closes the dialog on success. Each write lands, is confirmed in place,
 * and is reported upward with [ProfileInfoEvent.Updated] so the screen behind can refresh; the
 * user closes when they are finished.
 *
 * Depends only on use cases. Its one Compose type is the `TextFieldState` inside the name's
 * [FormField] — plain state with no UI behind it — so it still runs on every target.
 */
class ProfileInfoViewModel(
    private val updateDisplayName: UpdateDisplayNameUseCase,
    private val uploadAvatar: UploadAvatarUseCase,
    private val removeAvatar: RemoveAvatarUseCase,
    private val validateDisplayName: ValidateDisplayNameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileInfoUiState())
    val uiState: StateFlow<ProfileInfoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileInfoEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfileInfoEvent> = _events.asSharedFlow()

    val displayName = FormField(viewModelScope) { name ->
        _uiState.update {
            it.copy(typedName = name, nameError = null, status = it.status.clearedOnEdit())
        }
    }

    fun onAction(action: ProfileInfoAction) {
        when (action) {
            // A reopened dialog starts from the account as it is now, not from the last attempt.
            is ProfileInfoAction.Opened -> {
                val name = action.user.displayName.orEmpty()
                displayName.state.setTextAndPlaceCursorAtEnd(name)
                _uiState.value = ProfileInfoUiState(user = action.user, typedName = name)
            }

            ProfileInfoAction.SaveName -> saveName()

            is ProfileInfoAction.PhotoPicked -> write(ProfileInfoStatus.UploadingPhoto) {
                uploadAvatar(bytes = action.bytes, fileName = action.fileName)
            }

            ProfileInfoAction.RemovePhoto -> write(ProfileInfoStatus.RemovingPhoto) { removeAvatar() }

            ProfileInfoAction.Close ->
                viewModelScope.launch { _events.emit(ProfileInfoEvent.Dismissed) }
        }
    }

    /**
     * Validation runs before the call rather than after, so a name the server would refuse costs
     * no round trip and lands under the field instead of in the banner.
     */
    private fun saveName() {
        val user = _uiState.value.user ?: return
        val typedName = displayName.submit()

        val nameError = validateDisplayName(typedName)
        if (nameError != null) {
            _uiState.update { it.copy(nameError = nameError, status = ProfileInfoStatus.Idle) }
            return
        }

        _uiState.update { it.copy(nameError = null) }
        write(ProfileInfoStatus.SavingName) {
            updateDisplayName(displayName = typedName, current = user)
        }
    }

    /**
     * Runs one write, guarding against a second while it is in flight and reporting whatever the
     * server answered with.
     *
     * [running] is which button should show it is working, which is why each caller names its own
     * rather than sharing one "submitting".
     */
    private fun write(running: ProfileInfoStatus, call: suspend () -> AuthResult) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(status = running) }

        viewModelScope.launch {
            when (val result = call()) {
                is AuthResult.Success ->
                    onSaved(result.user, wroteTheName = running == ProfileInfoStatus.SavingName)
                is AuthResult.Failure -> _uiState.update {
                    it.copy(status = ProfileInfoStatus.Failed(result.error))
                }
            }
        }
    }

    /**
     * Adopts what the server wrote, tells the screen behind, and settles back to idle.
     *
     * The typed name is only reset from the response when the name is what was written. A photo
     * upload also answers with the whole account, and taking its `displayName` would wipe out a
     * rename the user had typed but not yet saved.
     *
     * The event goes out before the confirmation is held, so the profile header and the top bar
     * change while the tick is still on screen rather than a beat after it.
     */
    private suspend fun onSaved(user: UserResponse, wroteTheName: Boolean) {
        if (wroteTheName) displayName.state.setTextAndPlaceCursorAtEnd(user.displayName.orEmpty())
        _uiState.update {
            it.copy(
                user = user,
                typedName = displayName.state.text.toString(),
                status = ProfileInfoStatus.Succeeded,
            )
        }
        _events.emit(ProfileInfoEvent.Updated(user))

        delay(SUCCESS_HOLD_MILLIS.milliseconds)
        // Guard against a status a later action has already moved on from.
        _uiState.update {
            if (it.status == ProfileInfoStatus.Succeeded) it.copy(status = ProfileInfoStatus.Idle) else it
        }
    }

    /** Editing the name dismisses a previous failure, but must not interrupt one in flight. */
    private fun ProfileInfoStatus.clearedOnEdit(): ProfileInfoStatus =
        if (this is ProfileInfoStatus.Failed) ProfileInfoStatus.Idle else this
}
