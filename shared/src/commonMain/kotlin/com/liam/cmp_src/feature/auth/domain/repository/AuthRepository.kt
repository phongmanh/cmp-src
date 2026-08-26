package com.liam.cmp_src.feature.auth.domain.repository

import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * The auth boundary the domain layer depends on. The implementation lives in the data
 * layer (`feature.auth.data.AuthRepositoryImpl`) and is bound in the DI module.
 */
interface AuthRepository {

    suspend fun signInWithEmail(email: String, password: String): AuthResult

    suspend fun signInWith(provider: SocialProvider): AuthResult

    /**
     * Ends the current session. Returns nothing: the local session is dropped either way, so
     * there is no outcome a caller could usefully act on.
     */
    suspend fun signOut()

    /**
     * Register an account with email and password
     */
    suspend fun register(email: String, password: String): AuthResult
}
