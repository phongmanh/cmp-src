package com.liam.cmp_src.core.utils.format

import java.util.Locale
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Android actuals under a pinned locale and time zone — the JVM's defaults are whatever the
 * machine running the build has, so every test here sets both.
 */
class PlatformFormatTest {

    private lateinit var originalLocale: Locale
    private lateinit var originalTimeZone: TimeZone

    @BeforeTest
    fun pinDefaults() {
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun restoreDefaults() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `a decimal is grouped and rounded, and loses its trailing zeros`() {
        assertEquals("1,234.57", formatDecimal(1234.567))
        assertEquals("2", formatDecimal(2.0))
        assertEquals("0.5", formatDecimal(0.5, maxFractionDigits = 1))
    }

    @Test
    fun `a decimal follows the locale's separators`() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("1.234,5", formatDecimal(1234.5))
    }

    @Test
    fun `a byte size is the scaled number and its unit`() {
        assertEquals("5 MB", (5L * 1024 * 1024).formatByteSize())
        assertEquals("1.5 KB", 1536L.formatByteSize())
        assertEquals("1,023 B", 1023L.formatByteSize())
    }

    @Test
    fun `a date uses the locale's preset for each style`() {
        val secondOfJanuary2024 = 1_704_153_600_000L
        assertEquals("1/2/24", formatDate(secondOfJanuary2024, FormatStyle.SHORT))
        assertEquals("Jan 2, 2024", formatDate(secondOfJanuary2024, FormatStyle.MEDIUM))
        assertEquals("January 2, 2024", formatDate(secondOfJanuary2024, FormatStyle.LONG))
    }

    @Test
    fun `a date falls on the day it is in the device's time zone`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val secondOfJanuary2024 = 1_704_153_600_000L
        assertEquals("Jan 1, 2024", formatDate(secondOfJanuary2024))
    }
}
