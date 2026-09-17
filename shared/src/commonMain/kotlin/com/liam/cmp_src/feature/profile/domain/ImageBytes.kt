package com.liam.cmp_src.feature.profile.domain

import com.example.api.common.FieldLimits

private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val PNG_MAGIC =
    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

/**
 * What kind of picture these bytes are, as one of [FieldLimits.ALLOWED_AVATAR_UPLOAD_TYPES], or
 * `null` when they are neither.
 *
 * Read from the leading bytes rather than from a file extension or a picker's declared type,
 * because the server decides the same way and for the same reason: both of those are written by
 * the client, and an SVG named `.png` is the case that matters. Doing it here as well only saves
 * an upload that was going to be refused.
 */
internal fun ByteArray.avatarContentType(): String? = when {
    startsWith(JPEG_MAGIC) -> "image/jpeg"
    startsWith(PNG_MAGIC) -> "image/png"
    else -> null
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
