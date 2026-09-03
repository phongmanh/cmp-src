package com.liam.cmp_src.feature.auth.data

import com.example.api.auth.TokenResponse
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.ApiResult
import com.liam.cmp_src.core.network.getOrNull
import com.liam.cmp_src.feature.auth.data.remote.AuthApi
import com.liam.cmp_src.feature.auth.data.social.SocialAuthClient
import com.liam.cmp_src.feature.auth.data.social.SocialCredential
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * The auth session, backed by the API.
 *
 * Two sources feed it: [authApi] for everything the server decides, and [socialAuthClient] for
 * the one step only the device can perform — getting a credential out of a provider's SDK. The
 * credential is never treated as a session; it is exchanged for one at `POST /auth/social`, so
 * both sign-in paths end at the same place and the server is the only issuer of tokens.
 *
 * Nothing here throws: [AuthApi] returns `ApiResult`, and each failure is mapped to the domain's
 * [com.liam.cmp_src.feature.auth.domain.model.AuthError] by [toAuthError].
 *
 * The [dispatcher] is injected rather than named here so tests can run the flow on their own
 * scheduler.
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val socialAuthClient: SocialAuthClient,
    private val dispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): AuthResult = withContext(dispatcher) {
        authApi.login(email = email, password = password).toAuthResult()
    }

    override suspend fun signInWith(provider: SocialProvider): AuthResult = withContext(dispatcher) {
        when (val credential = socialAuthClient.requestCredential(provider)) {
            // The sheet was dismissed or the SDK failed: nothing to exchange, and no call
            // worth making.
            is SocialCredential.Denied -> AuthResult.Failure(credential.error)

            is SocialCredential.Granted -> authApi.signInWithSocial(
                    provider = provider.toContract(),
                    token = credential.token
                ).toAuthResult(provider)
        }
    }

    /**
     * Ends the session, and reports nothing back: [AuthApi.logout] drops the local tokens whether
     * or not the server answered, so from the caller's point of view sign-out always succeeds.
     *
     * [NonCancellable] because the caller is typically a screen that is on its way out. Without
     * it, navigating away mid-call would cancel the request before the tokens were cleared and
     * leave a live refresh token on the device.
     */
    override suspend fun signOut() {
        withContext(dispatcher + NonCancellable) {
            authApi.logout()
        }
    }

    override suspend fun register(
        email: String, password: String
    ): AuthResult = withContext(dispatcher) {
        authApi.register(email = email, password = password).toAuthResult()
    }

    /**
     * Re-reads the signed-in user. Unlike the sign-in paths this needs no `withLinkedProviders`
     * hop — `GET /users/me` is the call that fills that list in.
     */
    override suspend fun currentUser(): AuthResult = withContext(dispatcher) {
        when (val result = authApi.currentUser()) {
            is ApiResult.Success -> AuthResult.Success(result.data)
            is ApiResult.Failure -> AuthResult.Failure(result.error.toSessionError())
        }
    }


    private suspend fun ApiResult<TokenResponse>.toAuthResult(
        provider: SocialProvider? = null,
    ): AuthResult = when (this) {
        is ApiResult.Success -> AuthResult.Success(data.user.withLinkedProviders())
        is ApiResult.Failure -> AuthResult.Failure(error.toAuthError(provider))
    }

    /**
     * Fills in what a token response leaves out.
     *
     * The contract publishes `UserResponse.linkedProviders` as empty inside a `TokenResponse` and
     * filled by `GET /users/me`, and the home screen reads it to show which provider signed the
     * user in — so a sign-in is followed by one call for the full profile.
     *
     * A failure there is not a failed sign-in: the tokens are already stored and the session is
     * open, so the response's own copy of the user stands in.
     */
    private suspend fun UserResponse.withLinkedProviders(): UserResponse = authApi.currentUser().getOrNull() ?: this
}
