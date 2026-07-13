package com.liam.cmp_src

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CMPsrc",
    ) {
        App()
    }
}