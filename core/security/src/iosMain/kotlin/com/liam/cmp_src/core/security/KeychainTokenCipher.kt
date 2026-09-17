package com.liam.cmp_src.core.security

import cnames.structs.__CFData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyRef
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecKeyAlgorithmECIESEncryptionCofactorVariableIVX963SHA256AESGCM
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import kotlin.io.encoding.Base64

private const val DEFAULT_SERVICE = "com.liam.cmp_src.token-cipher"
private const val ACCOUNT = "p256-private-key"
private const val KEY_BITS = 256

/**
 * iOS encryption using a P-256 key kept in the Keychain.
 *
 * **Why elliptic curve rather than AES**, when Android uses AES-GCM: Kotlin/Native has no
 * AES-GCM primitive. The CommonCrypto bindings expose CBC, CTR, CFB, OFB and ECB and no GCM
 * mode at all, and unauthenticated CBC is not an acceptable way to store a credential. The Security
 * framework's ECIES does authenticated encryption for us — ephemeral ECDH, an X9.63 key derivation
 * and AES-GCM underneath — and it is reachable with no cinterop definition and no dependency.
 *
 * **Why not the Secure Enclave.** An Enclave key cannot be exported, so persisting it would mean a
 * second, genuinely different storage path with no way to test it and no simulator support. It
 * would also buy very little: both the key and the database live inside this app's sandbox, so
 * anything that can read one can read the other, and a `ThisDeviceOnly` Keychain item is already
 * sealed by a device-bound key.
 *
 * **Why `AfterFirstUnlockThisDeviceOnly`.** `WhenUnlocked` would fail during a background token
 * refresh with the screen locked. `ThisDeviceOnly` keeps the item out of iCloud Keychain and out of
 * encrypted backups, which is the behaviour we want: the database *is* backed up, so restoring onto
 * new hardware brings back ciphertext with no key, decryption returns null, and the user signs in
 * again rather than silently carrying a session across devices.
 *
 * [service] is a parameter so tests can isolate themselves from each other and from the app's own
 * key on a shared simulator.
 */
class KeychainTokenCipher(private val service: String = DEFAULT_SERVICE) : TokenCipher {

    /**
     * Created once and intentionally never released: it lives as long as the process, and there is
     * no point at which releasing it would be correct.
     */
    @OptIn(ExperimentalForeignApi::class)
    private val privateKey: SecKeyRef by lazy { loadKey() ?: createAndStoreKey() }

    @OptIn(ExperimentalForeignApi::class)
    override fun encrypt(plaintext: String): String {
        val publicKey = SecKeyCopyPublicKey(privateKey)
            ?: throw TokenCipherException("The Keychain key has no public half")
        try {
            val input = plaintext.encodeToByteArray().toCFData()
            try {
                val output = SecKeyCreateEncryptedData(publicKey, ALGORITHM, input, null)
                    ?: throw TokenCipherException("SecKeyCreateEncryptedData refused the token")
                try {
                    return Base64.encode(output.toByteArray())
                } finally {
                    CFRelease(output)
                }
            } finally {
                CFRelease(input)
            }
        } finally {
            CFRelease(publicKey)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun decrypt(ciphertext: String): String? = runCatching {
        // Base64.decode throws on anything outside the alphabet, which is exactly what a row
        // written by an older build that stored plaintext looks like. Inside the catch on purpose.
        val input = Base64.decode(ciphertext).toCFData()
        try {
            val output = SecKeyCreateDecryptedData(privateKey, ALGORITHM, input, null)
                ?: return@runCatching null
            try {
                output.toByteArray().decodeToString()
            } finally {
                CFRelease(output)
            }
        } finally {
            CFRelease(input)
        }
    }.getOrNull()

    @OptIn(ExperimentalForeignApi::class)
    private fun loadKey(): SecKeyRef? = withServiceStrings { serviceRef, accountRef ->
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to accountRef,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        try {
            memScoped {
                val found = alloc<CFTypeRefVar>()
                if (SecItemCopyMatching(query, found.ptr) != errSecSuccess) return@memScoped null
                val data = found.value?.reinterpret<__CFData>() ?: return@memScoped null
                try {
                    keyFromBytes(data)
                } finally {
                    CFRelease(data)
                }
            }
        } finally {
            CFRelease(query)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createAndStoreKey(): SecKeyRef {
        val keySize = cfNumberOf(KEY_BITS)
        val attributes = cfDictionaryOf(
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits to keySize,
        )
        val key = try {
            memScoped {
                val error = alloc<CFErrorRefVar>()
                val created = SecKeyCreateRandomKey(attributes, error.ptr)
                error.value?.let { CFRelease(it) }
                created ?: throw TokenCipherException("SecKeyCreateRandomKey failed")
            }
        } finally {
            CFRelease(attributes)
            CFRelease(keySize)
        }

        val exported = SecKeyCopyExternalRepresentation(key, null)
            ?: throw TokenCipherException("The new key could not be exported for storage")
        try {
            store(exported)
        } finally {
            CFRelease(exported)
        }
        return key
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun store(keyData: CFDataRef) = withServiceStrings { serviceRef, accountRef ->
        // Delete first: SecItemAdd fails with errSecDuplicateItem rather than replacing.
        val existing = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to accountRef,
        )
        try {
            SecItemDelete(existing)
        } finally {
            CFRelease(existing)
        }

        val attributes = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to accountRef,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData to keyData,
        )
        try {
            val status = SecItemAdd(attributes, null)
            if (status != errSecSuccess) {
                throw TokenCipherException("The Keychain refused the key, status $status")
            }
        } finally {
            CFRelease(attributes)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun keyFromBytes(data: CFDataRef): SecKeyRef? {
        val keySize = cfNumberOf(KEY_BITS)
        val attributes = cfDictionaryOf(
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeyClass to kSecAttrKeyClassPrivate,
            kSecAttrKeySizeInBits to keySize,
        )
        try {
            return memScoped {
                val error = alloc<CFErrorRefVar>()
                val key = SecKeyCreateWithData(data, attributes, error.ptr)
                error.value?.let { CFRelease(it) }
                key
            }
        } finally {
            CFRelease(attributes)
            CFRelease(keySize)
        }
    }

    /** Holds the two `CFString`s alive across a query and releases both afterwards. */
    @OptIn(ExperimentalForeignApi::class)
    private inline fun <T> withServiceStrings(block: (CFStringRef, CFStringRef) -> T): T {
        val serviceRef = service.toCFString()
        try {
            val accountRef = ACCOUNT.toCFString()
            try {
                return block(serviceRef, accountRef)
            } finally {
                CFRelease(accountRef)
            }
        } finally {
            CFRelease(serviceRef)
        }
    }

    private companion object {
        @OptIn(ExperimentalForeignApi::class)
        val ALGORITHM get() = kSecKeyAlgorithmECIESEncryptionCofactorVariableIVX963SHA256AESGCM
    }
}
