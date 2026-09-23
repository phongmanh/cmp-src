package com.liam.cmp_src.core.utils.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAuthorizationStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaType
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Composable
actual fun rememberPermissionController(): PermissionController = remember { IosPermissionController() }

/**
 * iOS prompts once per permission: after that, a refusal is [PermissionStatus.DENIED_ALWAYS] and
 * only Settings can change it, so this never reports [PermissionStatus.DENIED].
 */
private class IosPermissionController : PermissionController {

    /**
     * The location prompt in flight. `CLLocationManager` holds its delegate weakly and nothing else
     * holds the manager, so both have to be kept here or they are collected before the user answers.
     */
    private var locationRequest: Pair<CLLocationManager, LocationAuthorizationDelegate>? = null

    override suspend fun status(permission: Permission): PermissionStatus = when (permission) {
        Permission.CAMERA -> mediaStatus(videoMediaType())
        Permission.MICROPHONE -> mediaStatus(audioMediaType())
        Permission.LOCATION -> withContext(Dispatchers.Main) { CLLocationManager().authorizationStatus.toLocationStatus() }
        Permission.NOTIFICATIONS -> notificationStatus()
    }

    override suspend fun request(permission: Permission): PermissionStatus {
        val current = status(permission)
        if (current != PermissionStatus.NOT_DETERMINED) return current
        return when (permission) {
            Permission.CAMERA -> requestMedia(videoMediaType())
            Permission.MICROPHONE -> requestMedia(audioMediaType())
            Permission.LOCATION -> requestLocation()
            Permission.NOTIFICATIONS -> requestNotifications()
        }
    }

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    private fun mediaStatus(type: AVMediaType): PermissionStatus =
        AVCaptureDevice.authorizationStatusForMediaType(type).toMediaStatus()

    private suspend fun requestMedia(type: AVMediaType): PermissionStatus =
        suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(type) { granted ->
                continuation.resume(granted.toStatus())
            }
        }

    private suspend fun notificationStatus(): PermissionStatus =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.authorizationStatus?.toNotificationStatus() ?: PermissionStatus.NOT_DETERMINED)
            }
        }

    private suspend fun requestNotifications(): PermissionStatus =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
            ) { granted, _ ->
                continuation.resume(granted.toStatus())
            }
        }

    /** `CLLocationManager` wants the main thread, and answers through its delegate. */
    private suspend fun requestLocation(): PermissionStatus = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val manager = CLLocationManager()
            val delegate = LocationAuthorizationDelegate { status ->
                // The delegate is also told the current status the moment it is set; that one is
                // still "not determined" and is not the answer.
                if (status != kCLAuthorizationStatusNotDetermined && continuation.isActive) {
                    locationRequest = null
                    continuation.resume(status.toLocationStatus())
                }
            }
            locationRequest = manager to delegate
            continuation.invokeOnCancellation { locationRequest = null }
            manager.delegate = delegate
            manager.requestWhenInUseAuthorization()
        }
    }
}

private class LocationAuthorizationDelegate(
    private val onChange: (CLAuthorizationStatus) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        onChange(manager.authorizationStatus)
    }
}

private fun videoMediaType(): AVMediaType = requireNotNull(AVMediaTypeVideo) { "AVMediaTypeVideo is unavailable" }

private fun audioMediaType(): AVMediaType = requireNotNull(AVMediaTypeAudio) { "AVMediaTypeAudio is unavailable" }

/** Only "yes" or "no" comes back from a prompt, and on iOS "no" is final. */
private fun Boolean.toStatus(): PermissionStatus =
    if (this) PermissionStatus.GRANTED else PermissionStatus.DENIED_ALWAYS

// The platform statuses are all plain integer typealiases, hence one name per kind.

/** Refused and restricted (parental controls, device management) both leave only Settings. */
private fun AVAuthorizationStatus.toMediaStatus(): PermissionStatus = when (this) {
    AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
    AVAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
    else -> PermissionStatus.DENIED_ALWAYS
}

private fun CLAuthorizationStatus.toLocationStatus(): PermissionStatus = when (this) {
    kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.GRANTED
    kCLAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
    else -> PermissionStatus.DENIED_ALWAYS
}

/** Provisional and ephemeral authorisations can post, which is all a caller asks. */
private fun UNAuthorizationStatus.toNotificationStatus(): PermissionStatus = when (this) {
    UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral ->
        PermissionStatus.GRANTED
    UNAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
    else -> PermissionStatus.DENIED_ALWAYS
}
