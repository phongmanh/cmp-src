package com.liam.cmp_src.core.utils.format

/** The fraction digits [formatDecimal] keeps unless asked for another number. */
const val DEFAULT_FRACTION_DIGITS = 2

/**
 * [value] written the way the device's locale writes numbers — grouping separators and the
 * decimal mark included, so `1234.5` is `1,234.5` in the US and `1.234,5` in Germany.
 *
 * At most [maxFractionDigits] digits follow the decimal mark, and trailing zeros are dropped:
 * `2.0` is `2`, not `2.00`. Rounding is half-even on both platforms.
 */
expect fun formatDecimal(value: Double, maxFractionDigits: Int = DEFAULT_FRACTION_DIGITS): String
