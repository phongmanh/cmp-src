package com.liam.cmp_src.core.utils.format

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

private const val MILLIS_PER_SECOND = 1000.0

actual fun formatDate(epochMillis: Long, style: FormatStyle): String =
    format(epochMillis, style.toNative(), NSDateFormatterNoStyle)

actual fun formatDateTime(epochMillis: Long, dateStyle: FormatStyle, timeStyle: FormatStyle): String =
    format(epochMillis, dateStyle.toNative(), timeStyle.toNative())

private fun format(epochMillis: Long, dateStyle: NSDateFormatterStyle, timeStyle: NSDateFormatterStyle): String {
    val formatter = NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        this.dateStyle = dateStyle
        this.timeStyle = timeStyle
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / MILLIS_PER_SECOND))
}

private fun FormatStyle.toNative(): NSDateFormatterStyle = when (this) {
    FormatStyle.SHORT -> NSDateFormatterShortStyle
    FormatStyle.MEDIUM -> NSDateFormatterMediumStyle
    FormatStyle.LONG -> NSDateFormatterLongStyle
}
