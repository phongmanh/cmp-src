package com.liam.cmp_src.core.utils.format

/** Bytes in each step from one [ByteUnit] to the next. */
private const val BYTES_PER_STEP = 1024.0

/** The fraction digits [formatByteSize] keeps unless asked for another number. */
const val DEFAULT_BYTE_SIZE_FRACTION_DIGITS = 1

/**
 * The units a size is written in, 1024 apart. The symbols are the ones people read on every
 * platform (`MB`, not `MiB`), and are not translated.
 */
enum class ByteUnit(val symbol: String) {
    B("B"),
    KB("KB"),
    MB("MB"),
    GB("GB"),
    TB("TB"),
}

/** A size scaled to the largest unit it fills at least one of, e.g. `1.5` [ByteUnit.MB]. */
data class ByteSize(val value: Double, val unit: ByteUnit)

/**
 * This many bytes in the largest unit that keeps the value at 1 or more — so `1536` is
 * `1.5 KB` and `1023` stays `1023 B`. Anything past a terabyte is still counted in terabytes.
 */
fun Long.toByteSize(): ByteSize {
    require(this >= 0) { "A byte count cannot be negative, was $this" }
    var value = toDouble()
    var unit = ByteUnit.B
    while (value >= BYTES_PER_STEP && unit != ByteUnit.entries.last()) {
        value /= BYTES_PER_STEP
        unit = ByteUnit.entries[unit.ordinal + 1]
    }
    return ByteSize(value, unit)
}

/**
 * This many bytes for a person to read: `5 MB`, `1.5 KB`, `1,023 B` — the number in the device's
 * locale (see [formatDecimal]), the unit after it.
 */
fun Long.formatByteSize(maxFractionDigits: Int = DEFAULT_BYTE_SIZE_FRACTION_DIGITS): String {
    val size = toByteSize()
    return "${formatDecimal(size.value, maxFractionDigits)} ${size.unit.symbol}"
}
