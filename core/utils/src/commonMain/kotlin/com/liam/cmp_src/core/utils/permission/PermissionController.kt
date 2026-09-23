package com.liam.cmp_src.core.utils.permission

import androidx.compose.runtime.Composable

/**
 * Reads and requests runtime [Permission]s.
 *
 * Obtained from [rememberPermissionController] in the screen that needs it and handed to the code
 * that decides when to ask — a ViewModel takes it as a parameter to the action that needs it, and
 * never keeps it, since it is tied to the screen that created it.
 */
interface PermissionController {

    /**
     * The current status, without prompting.
     *
     * Android cannot tell a permission that was never asked for from one refused with "Don't ask
     * again" until it asks, so both read [PermissionStatus.NOT_DETERMINED] here; [request] then
     * answers [PermissionStatus.DENIED_ALWAYS] at once, without a prompt, for the second kind.
     */
    suspend fun status(permission: Permission): PermissionStatus

    /**
     * Shows the system prompt when the platform still allows one, and suspends until the user
     * answers. A permission already granted returns [PermissionStatus.GRANTED] without prompting.
     * Concurrent requests are asked one after another.
     */
    suspend fun request(permission: Permission): PermissionStatus

    /** Opens this app's page in the system settings — the only way back from [PermissionStatus.DENIED_ALWAYS]. */
    fun openAppSettings()
}

/** A [PermissionController] bound to the screen currently composing. */
@Composable
expect fun rememberPermissionController(): PermissionController
