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

    /**
     * The user the stored tokens belong to, re-read from the server.
     *
     * Returns an [AuthResult] rather than a bare user because this can fail the same ways a
     * sign-in can — the session may have expired, or the device may be offline — and the caller
     * has to be able to say so.
     */
    suspend fun currentUser(): AuthResult

    /**
     * Replaces the signed-in user's password.
     *
     * [currentPassword] is required on top of the access token, so a device somebody walked up to
     * cannot be used to lock its owner out.
     *
     * Succeeding ends every *other* session and leaves this one open on a fresh token pair, which
     * the data layer adopts. The returned [AuthResult.Success] therefore carries a user whose
     * `linkedProviders` is empty, as it is in any token response — nothing reads it on this path.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult
}
