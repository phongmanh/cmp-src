package com.liam.cmp_src.core.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * Shows the hand cursor over something clickable, and the plain arrow when it is not.
 *
 * Only a pointing device has a cursor to answer to — an Android device in desktop mode or on
 * ChromeOS, an iPad with a trackpad. Under a finger this is inert, which is why every clickable
 * in the shared UI can carry it unconditionally.
 *
 * Pass the same flag that drives the control's `enabled`, so the cursor cannot promise a click
 * the control will not accept.
 */
fun Modifier.handCursor(enabled: Boolean = true): Modifier =
    pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
