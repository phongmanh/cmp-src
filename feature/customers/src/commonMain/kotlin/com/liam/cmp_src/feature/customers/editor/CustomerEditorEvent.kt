package com.liam.cmp_src.feature.customers.editor

/** One-shot effects the customer editor reports upwards. */
sealed interface CustomerEditorEvent {
    /** The editor is finished — saved, deleted or abandoned — and should be closed. */
    data object Done : CustomerEditorEvent
}
