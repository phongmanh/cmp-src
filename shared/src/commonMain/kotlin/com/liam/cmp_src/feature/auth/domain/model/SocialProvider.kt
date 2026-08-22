package com.liam.cmp_src.feature.auth.domain.model

import com.example.api.auth.SocialProvider as ContractSocialProvider

/** Third-party identity providers the login screen offers. */
enum class SocialProvider {
    GOOGLE,
    FACEBOOK,
    ;

    /**
     * The contract's wire form for this provider — the same value that appears in
     * `UserResponse.linkedProviders`.
     *
     * Spelled through the contract's own enum rather than by lowercasing [name], so a provider
     * the server renames breaks this build instead of silently never matching.
     */
    val key: String
        get() = when (this) {
            GOOGLE -> ContractSocialProvider.GOOGLE.key
            FACEBOOK -> ContractSocialProvider.FACEBOOK.key
        }

    companion object {
        /**
         * The provider a `UserResponse.linkedProviders` entry names, or null when this build
         * does not offer it — an unknown key is skipped rather than shown raw.
         */
        fun fromKey(key: String): SocialProvider? = entries.firstOrNull { it.key == key }
    }
}
