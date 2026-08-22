package com.liam.cmp_src.feature.auth.domain.model

import com.example.api.user.UserResponse

/**
 * Outcome of a sign-in attempt. Every data-layer entry point returns one of these rather
 * than throwing, so no raw exception ever escapes into the domain or presentation layers.
 */
sealed interface AuthResult {
    data class Success(val user: UserResponse) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}
