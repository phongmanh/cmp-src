package com.liam.cmp_src.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the signed-in account, and ends the session on request.
 *
 * The user is re-read rather than taken from the sign-in handover, so the screen shows what the
 * server currently holds — including `linkedProviders`, which a token response leaves empty.
 */
class ProfileViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    /** Guards the second tap: the button stays on screen until the call comes back. */
    private var isSigningOut = false

    init {
        load()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Retry -> load()
            ProfileAction.Logout -> logout()
            ProfileAction.EditProfile,
            ProfileAction.ChangePassword,
                -> viewModelScope.launch { _events.emit(ProfileEvent.ShowNotImplemented) }
        }
    }

    /** Back to [ProfileUiState.Loading] first, so a retry puts the skeleton back on screen. */
    private fun load() {
        _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = getCurrentUser()) {
                is AuthResult.Success -> ProfileUiState.Success(result.user)
                is AuthResult.Failure -> ProfileUiState.Error(result.error)
            }
        }
    }

    /**
     * Ends the session before reporting it. Emitting first would navigate away while the tokens
     * were still live, and the sign-out call would be cancelled with the screen.
     */
    private fun logout() {
        if (isSigningOut) return
        isSigningOut = true
        viewModelScope.launch {
            signOut()
            _events.emit(ProfileEvent.GoToLogin)
        }
    }
}
