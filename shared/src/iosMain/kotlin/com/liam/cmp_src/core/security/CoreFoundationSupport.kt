package com.liam.cmp_src.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks

/**
 * The Core Foundation bridging the Keychain and `SecKey` calls need.
 *
 * Every function here follows the Create rule: what it returns is owned by the caller and must be
 * released. `KeychainTokenCipher` does that in `finally` blocks — see its KDoc.
 *
 * The allocator argument is passed as `null` throughout, which Core Foundation reads as the default
 * allocator.
 */

/** Builds a `CFDictionary` for a `Sec*` query or attribute set. Caller releases it. */
@OptIn(ExperimentalForeignApi::class)
internal fun cfDictionaryOf(vararg entries: Pair<CFTypeRef?, CFTypeRef?>): CFMutableDictionaryRef {
    val dictionary = CFDictionaryCreateMutable(
        null,
        entries.size.convert(),
        kCFTypeDictionaryKeyCallBacks.ptr,
        kCFTypeDictionaryValueCallBacks.ptr,
    ) ?: error("CFDictionaryCreateMutable returned null")
    for ((key, value) in entries) {
        CFDictionarySetValue(dictionary, key, value)
    }
    return dictionary
}

/** Caller releases the result. */
@OptIn(ExperimentalForeignApi::class)
internal fun cfNumberOf(value: Int): CFNumberRef = memScoped {
    val holder = alloc<IntVar>().apply { this.value = value }
    CFNumberCreate(null, kCFNumberIntType, holder.ptr)
} ?: error("CFNumberCreate returned null")

/** Caller releases the result. */
@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toCFData(): CFDataRef {
    if (isEmpty()) return CFDataCreate(null, null, 0) ?: error("CFDataCreate returned null")
    return usePinned { pinned ->
        CFDataCreate(null, pinned.addressOf(0).reinterpret(), size.convert())
    } ?: error("CFDataCreate returned null")
}

@OptIn(ExperimentalForeignApi::class)
internal fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this).toInt()
    if (length == 0) return ByteArray(0)
    val bytes = CFDataGetBytePtr(this) ?: return ByteArray(0)
    return ByteArray(length) { bytes[it].toByte() }
}

/** Caller releases the result. cinterop maps the `const char *` parameter straight to `String?`. */
@OptIn(ExperimentalForeignApi::class)
internal fun String.toCFString(): CFStringRef =
    CFStringCreateWithCString(null, this, kCFStringEncodingUTF8.convert())
        ?: error("CFStringCreateWithCString returned null")
