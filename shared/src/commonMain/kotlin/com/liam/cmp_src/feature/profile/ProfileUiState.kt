package com.liam.cmp_src.feature.profile

import com.example.api.user.UserResponse
import com.liam.cmp_src.feature.auth.domain.model.AuthError

/**
 * What the profile screen is showing.
 *
 * [Error] carries the domain error rather than a sentence, so the wording stays in the
 * presentation layer where it can resolve through a string resource — see `AuthError.asMessage()`.
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: UserResponse) : ProfileUiState
    data class Error(val error: AuthError) : ProfileUiState
}
