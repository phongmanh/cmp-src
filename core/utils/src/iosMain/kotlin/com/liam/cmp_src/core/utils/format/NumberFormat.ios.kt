package com.liam.cmp_src.core.utils.format

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale

actual fun formatDecimal(value: Double, maxFractionDigits: Int): String {
    val formatter = NSNumberFormatter().apply {
        locale = NSLocale.currentLocale
        numberStyle = NSNumberFormatterDecimalStyle
        minimumFractionDigits = 0u
        maximumFractionDigits = maxFractionDigits.toULong()
    }
    // `stringFromNumber` is only nil for a number the style cannot express; a Double never is one.
    return formatter.stringFromNumber(NSNumber(value)) ?: value.toString()
}
