package com.liam.cmp_src.feature.profile.domain.repository

import com.liam.cmp_src.feature.auth.domain.model.AuthResult

/**
 * The profile boundary the domain layer depends on. The implementation lives in the data layer
 * (`feature.profile.data.ProfileRepositoryImpl`) and is bound in the DI module.
 *
 * Every function answers with the whole account as the server now holds it, so a caller never has
 * to reason about which fields its request happened to touch.
 */
interface ProfileRepository {

    /**
     * Writes the editable half of the profile.
     *
     * Replace semantics: both values are stored exactly as given, and `null` clears one. The
     * server also retires an uploaded avatar on every call, whatever [avatarUrl] holds — choosing
     * what to send in the face of that is [com.liam.cmp_src.feature.profile.domain.usecase.UpdateDisplayNameUseCase]'s
     * job, not this interface's.
     */
    suspend fun updateProfile(displayName: String?, avatarUrl: String?): AuthResult

    /**
     * Replaces the account's picture with [bytes].
     *
     * [fileName] and [contentType] describe the file for the request only. The server decides what
     * it actually is by reading the bytes, so neither is load-bearing.
     */
    suspend fun uploadAvatar(bytes: ByteArray, fileName: String, contentType: String): AuthResult

    /** Drops the account's picture, however it was set. Succeeds even when there was none. */
    suspend fun removeAvatar(): AuthResult
}
