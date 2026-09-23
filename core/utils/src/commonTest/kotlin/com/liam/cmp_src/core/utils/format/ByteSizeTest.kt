package com.liam.cmp_src.core.utils.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Which unit a byte count lands in. How the number is then written is the platform's job. */
class ByteSizeTest {

    @Test
    fun `under a kilobyte stays in bytes`() {
        assertEquals(ByteSize(0.0, ByteUnit.B), 0L.toByteSize())
        assertEquals(ByteSize(1023.0, ByteUnit.B), 1023L.toByteSize())
    }

    @Test
    fun `each unit starts at exactly 1024 of the one below`() {
        assertEquals(ByteSize(1.0, ByteUnit.KB), 1024L.toByteSize())
        assertEquals(ByteSize(1.0, ByteUnit.MB), (1024L * 1024).toByteSize())
        assertEquals(ByteSize(1.0, ByteUnit.GB), (1024L * 1024 * 1024).toByteSize())
    }

    @Test
    fun `a partial unit keeps its fraction`() {
        assertEquals(ByteSize(1.5, ByteUnit.KB), 1536L.toByteSize())
        assertEquals(ByteSize(2.5, ByteUnit.MB), (5L * 1024 * 1024 / 2).toByteSize())
    }

    @Test
    fun `past a terabyte the count stays in terabytes`() {
        val twoThousandTerabytes = 2048L * 1024 * 1024 * 1024 * 1024
        assertEquals(ByteSize(2048.0, ByteUnit.TB), twoThousandTerabytes.toByteSize())
    }

    @Test
    fun `a negative count is a bug rather than a size`() {
        assertFailsWith<IllegalArgumentException> { (-1L).toByteSize() }
    }
}
