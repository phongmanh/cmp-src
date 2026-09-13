package com.liam.cmp_src

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit

private const val APP_ID = "com.liam.cmp_src"

/**
 * The desktop shell.
 *
 * [FileKit.init] runs before the window because the photo picker the profile screen opens needs an
 * application id to resolve this platform's per-user directories. Android and iOS derive the same
 * thing from the bundle, and the browser has no filesystem to name, so this is the one target that
 * has to be told.
 */
fun main() {
    FileKit.init(appId = APP_ID)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "CMPsrc",
        ) {
            App()
        }
    }
}
