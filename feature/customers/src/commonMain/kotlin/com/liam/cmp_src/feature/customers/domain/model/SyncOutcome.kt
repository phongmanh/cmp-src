package com.liam.cmp_src.feature.customers.domain.model

/**
 * How a sync with the server ended.
 *
 * Whatever the outcome, the device's own copy is intact and still editable — a failed sync only
 * means the server is further behind than it could be.
 */
sealed interface SyncOutcome {

    /** Every waiting change went up, and the device now matches the server. */
    data object Synced : SyncOutcome

    /** The server could not be reached. Changes wait for the connection to come back. */
    data object Offline : SyncOutcome

    /**
     * The stored session is not the account these customers belong to — signed out, expired, or
     * another account signed in since. Nothing was sent, so no customer reached the wrong account.
     */
    data object SessionMismatch : SyncOutcome

    /** The server answered, but not with success. Worth retrying later. */
    data object Failed : SyncOutcome
}
