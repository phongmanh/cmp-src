package com.liam.cmp_src.core.ui.input

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * One text field of a form a ViewModel owns: the [state] the field edits, and what editing it does.
 *
 * [onEdit] runs with the new text each time it changes, for as long as [scope] lives — the hook for
 * clearing an error once the user starts fixing it. Only the text counts: moving the cursor or the
 * selection is not an edit, and neither is the text the field starts with.
 *
 * Read the text a form submits through [submit], not straight from [state]. An edit is reported
 * once the snapshot it was made in is applied, which Compose does every frame, so a keystroke and
 * a tap on "Done" inside one frame reach the ViewModel in the wrong order: without [submit], the
 * keystroke's report would land after validation and wipe the errors validation had just raised.
 * With no UI running nothing applies snapshots, so a test does it itself with
 * `Snapshot.sendApplyNotifications()`.
 */
class FormField(scope: CoroutineScope, onEdit: (String) -> Unit) {

    val state = TextFieldState()

    /** The text last handed out by [submit], until the user changes it. */
    private var submitted: String? = null

    init {
        // Undispatched, so the starting text is read now rather than whenever [scope]'s dispatcher
        // gets round to it — otherwise an edit made in between would be taken for it and missed.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            snapshotFlow { state.text.toString() }
                .drop(1)
                .collect { text ->
                    // A late report of the text that was just submitted is not a new edit.
                    if (text != submitted) {
                        submitted = null
                        onEdit(text)
                    }
                }
        }
    }

    /** The text as it stands, for validating and sending. */
    fun submit(): String = state.text.toString().also { submitted = it }
}
