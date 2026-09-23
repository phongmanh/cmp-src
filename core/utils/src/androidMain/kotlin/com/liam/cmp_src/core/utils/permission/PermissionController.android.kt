package com.liam.cmp_src.core.utils.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
actual fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { PendingPermissionRequest.complete() }
    return remember(context, activity, launcher) {
        AndroidPermissionController(context.applicationContext, activity, launcher)
    }
}

/**
 * Only one system permission dialog can be up at a time, so the request in flight is held
 * process-wide rather than per controller: when the Activity is recreated mid-dialog, the result
 * arrives at the new composition's launcher, and still has to wake the coroutine the old one
 * suspended.
 *
 * Touched on the main thread only.
 */
private object PendingPermissionRequest {
    private val mutex = Mutex()
    private var pending: CompletableDeferred<Unit>? = null

    suspend fun launchAndAwait(launch: () -> Unit) = mutex.withLock {
        val answer = CompletableDeferred<Unit>()
        pending = answer
        launch()
        answer.await()
    }

    fun complete() {
        pending?.complete(Unit)
        pending = null
    }
}

private class AndroidPermissionController(
    private val context: Context,
    private val activity: Activity?,
    private val launcher: ActivityResultLauncher<Array<String>>,
) : PermissionController {

    override suspend fun status(permission: Permission): PermissionStatus {
        val names = permission.manifestNames()
        return when {
            isGranted(names) -> PermissionStatus.GRANTED
            shouldShowRationale(names) -> PermissionStatus.DENIED
            else -> PermissionStatus.NOT_DETERMINED
        }
    }

    /**
     * The status after the prompt is read back from the platform rather than from the result
     * map, since for [Permission.LOCATION] approximate alone is a grant. A prompt dismissed without
     * an answer reads as [PermissionStatus.DENIED_ALWAYS] until the next request shows it again —
     * Android reports the two the same way.
     */
    override suspend fun request(permission: Permission): PermissionStatus = withContext(Dispatchers.Main.immediate) {
        val names = permission.manifestNames()
        if (isGranted(names)) return@withContext PermissionStatus.GRANTED
        requireNotNull(activity) { "Requesting $permission needs an Activity, and this composition has none" }

        PendingPermissionRequest.launchAndAwait { launcher.launch(names.toTypedArray()) }
        when {
            isGranted(names) -> PermissionStatus.GRANTED
            shouldShowRationale(names) -> PermissionStatus.DENIED
            else -> PermissionStatus.DENIED_ALWAYS
        }
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Any one is enough: every group below is alternatives, e.g. precise or approximate location. */
    private fun isGranted(names: List<String>): Boolean =
        names.isEmpty() || names.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun shouldShowRationale(names: List<String>): Boolean =
        activity != null && names.any { activity.shouldShowRequestPermissionRationale(it) }
}

/** The manifest permissions behind [this]; empty where this Android version needs none. */
private fun Permission.manifestNames(): List<String> = when (this) {
    Permission.CAMERA -> listOf(Manifest.permission.CAMERA)
    Permission.MICROPHONE -> listOf(Manifest.permission.RECORD_AUDIO)
    Permission.LOCATION -> listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    Permission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyList()
    }
}
