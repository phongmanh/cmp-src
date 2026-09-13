package com.liam.cmp_src.core.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Reading the format from the leading bytes, the way the server does.
 *
 * The case that matters is the last one: a file the picker and the filename both call a PNG, which
 * is nothing of the sort.
 */
class ImageBytesTest {

    @Test
    fun `a jpeg header reads as a jpeg`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00)

        assertEquals("image/jpeg", bytes.avatarContentType())
    }

    @Test
    fun `a png header reads as a png`() {
        val bytes = PNG_HEADER + byteArrayOf(0x00, 0x01)

        assertEquals("image/png", bytes.avatarContentType())
    }

    @Test
    fun `a gif is neither`() {
        assertNull("GIF89a".encodeToByteArray().avatarContentType())
    }

    @Test
    fun `an svg wearing a png name is still neither`() {
        assertNull("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".encodeToByteArray().avatarContentType())
    }

    @Test
    fun `a file too short to have a header is neither`() {
        assertNull(byteArrayOf(0xFF.toByte(), 0xD8.toByte()).avatarContentType())
        assertNull(ByteArray(0).avatarContentType())
    }

    /** The first bytes match a PNG but the rest does not; only the header is claimed to be read. */
    @Test
    fun `a truncated png still reads as a png`() {
        assertEquals("image/png", PNG_HEADER.avatarContentType())
    }

    private companion object {
        val PNG_HEADER = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
