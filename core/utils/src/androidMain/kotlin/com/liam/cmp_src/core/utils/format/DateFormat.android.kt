package com.liam.cmp_src.core.utils.format

import java.text.DateFormat
import java.util.Date
import java.util.Locale

actual fun formatDate(epochMillis: Long, style: FormatStyle): String =
    DateFormat.getDateInstance(style.toJava(), Locale.getDefault()).format(Date(epochMillis))

actual fun formatDateTime(epochMillis: Long, dateStyle: FormatStyle, timeStyle: FormatStyle): String =
    DateFormat.getDateTimeInstance(dateStyle.toJava(), timeStyle.toJava(), Locale.getDefault())
        .format(Date(epochMillis))

private fun FormatStyle.toJava(): Int = when (this) {
    FormatStyle.SHORT -> DateFormat.SHORT
    FormatStyle.MEDIUM -> DateFormat.MEDIUM
    FormatStyle.LONG -> DateFormat.LONG
}
