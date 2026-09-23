package com.liam.cmp_src.core.utils.format

/**
 * How much of a date or time to show, from the platform's own presets — which is what keeps the
 * order, the separators and the month names right for the user's locale.
 *
 * For `en_US`, a date is `1/2/24`, `Jan 2, 2024` and `January 2, 2024` respectively.
 */
enum class FormatStyle { SHORT, MEDIUM, LONG }

/**
 * The calendar date of [epochMillis] in the device's time zone and locale, with no time of day.
 */
expect fun formatDate(epochMillis: Long, style: FormatStyle = FormatStyle.MEDIUM): String

/**
 * The date and time of [epochMillis] in the device's time zone and locale. The time follows the
 * device's 12/24-hour setting.
 */
expect fun formatDateTime(
    epochMillis: Long,
    dateStyle: FormatStyle = FormatStyle.MEDIUM,
    timeStyle: FormatStyle = FormatStyle.SHORT,
): String
