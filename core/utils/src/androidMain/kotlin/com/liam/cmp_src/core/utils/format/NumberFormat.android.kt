package com.liam.cmp_src.core.utils.format

import java.text.NumberFormat
import java.util.Locale

actual fun formatDecimal(value: Double, maxFractionDigits: Int): String =
    NumberFormat.getNumberInstance(Locale.getDefault())
        .apply {
            minimumFractionDigits = 0
            maximumFractionDigits = maxFractionDigits
        }
        .format(value)
