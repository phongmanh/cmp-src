package com.liam.cmp_src.core.testing

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Replaces this field's text as if the user had typed [text], and reports the change.
 *
 * In the app Compose applies snapshot changes every frame, which is what `FormField` listens
 * for. With no UI running nothing does, so this applies them here. The collector still has to run:
 * advance the test dispatcher before asserting on what an edit did.
 */
fun TextFieldState.type(text: String) {
    setTextAndPlaceCursorAtEnd(text)
    Snapshot.sendApplyNotifications()
}
