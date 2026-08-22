package com.liam.cmp_src.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.feature.auth.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** One-shot effects the home screen reports upwards. */
sealed interface HomeEvent {
    /** The session has ended; the app should go back to sign-in. */
    data object SignedOut : HomeEvent
}

/**
 * Runs the signed-in screen's one piece of logic: ending the session.
 *
 * The screen has no loading or error state of its own — sign-out cannot fail from the user's
 * point of view (see `AuthRepository.signOut`), so there is nothing to render while it runs and
 * nothing to report when it finishes but "you are signed out".
 */
class HomeViewModel(
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    /** Guards the second tap: the button stays on screen until the call comes back. */
    private var isSigningOut = false

    fun onSignOut() {
        if (isSigningOut) return
        isSigningOut = true
        viewModelScope.launch {
            signOut()
            _events.emit(HomeEvent.SignedOut)
        }
    }
}
