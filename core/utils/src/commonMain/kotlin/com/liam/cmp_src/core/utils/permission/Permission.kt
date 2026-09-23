package com.liam.cmp_src.core.utils.permission

/**
 * A runtime permission the app can ask for.
 *
 * Asking is not enough on its own: the app has to declare each one it uses, or the platform
 * refuses without showing anything (Android) or terminates the app (iOS).
 *
 * | Permission      | Android manifest                                  | iOS Info.plist                          |
 * |-----------------|---------------------------------------------------|-----------------------------------------|
 * | [CAMERA]        | `CAMERA`                                          | `NSCameraUsageDescription`              |
 * | [MICROPHONE]    | `RECORD_AUDIO`                                    | `NSMicrophoneUsageDescription`          |
 * | [LOCATION]      | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`  | `NSLocationWhenInUseUsageDescription`   |
 * | [NOTIFICATIONS] | `POST_NOTIFICATIONS`                              | —                                       |
 *
 * Picking a photo needs none of these: both platforms' pickers hand over only what the user chose.
 */
enum class Permission {
    CAMERA,
    MICROPHONE,

    /** While the app is in use; precise or approximate, whichever the user allows. */
    LOCATION,

    /** Always granted below Android 13, where posting needs no permission. */
    NOTIFICATIONS,
}

/** Where a [Permission] stands for this app. */
enum class PermissionStatus {
    GRANTED,

    /** Never asked — or, on Android, possibly refused for good; see [PermissionController.status]. */
    NOT_DETERMINED,

    /** Refused, but asking again still shows the system prompt. Android only. */
    DENIED,

    /**
     * Refused, and the system will not ask again: only the app's settings page can change it
     * ([PermissionController.openAppSettings]). Also what a permission blocked by parental controls
     * or device management reports.
     */
    DENIED_ALWAYS,
}
