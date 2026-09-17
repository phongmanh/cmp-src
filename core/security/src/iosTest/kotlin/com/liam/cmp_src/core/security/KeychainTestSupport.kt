package com.liam.cmp_src.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFRelease
import platform.Security.SecItemAdd
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecValueData
import kotlin.random.Random

/**
 * Whether this process can reach the Keychain at all.
 *
 * It cannot, under `./gradlew :shared:iosSimulatorArm64Test`. Gradle runs the test binary as a bare
 * Mach-O executable rather than inside an app bundle, so it carries no code signature and no
 * keychain access group, and every `SecItemAdd` comes back `errSecNotAvailable` (-25291). The
 * Keychain is not something that can be stubbed around: without an entitlement there is no keychain
 * to talk to.
 *
 * So [KeychainTokenCipherTest] skips itself here rather than failing, and lights up on its own if
 * anyone later runs these through an XCTest host application. Until then `KeychainTokenCipher` is
 * verified by hand against the real app, the same as `KeystoreTokenCipher` on Android.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun keychainIsUsable(): Boolean {
    val service = "com.liam.cmp_src.test.probe.${Random.nextLong()}"
    val serviceRef = service.toCFString()
    val accountRef = "probe".toCFString()
    val dataRef = byteArrayOf(1).toCFData()
    try {
        val attributes = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to accountRef,
            kSecValueData to dataRef,
        )
        try {
            if (SecItemAdd(attributes, null) != errSecSuccess) return false
        } finally {
            CFRelease(attributes)
        }
    } finally {
        CFRelease(dataRef)
        CFRelease(accountRef)
        CFRelease(serviceRef)
    }
    deleteKeychainItem(service)
    return true
}

/**
 * Removes everything a test stored under [service].
 *
 * Keychain items outlive both the process and the app that wrote them, so without this a test run
 * would leave keys behind for the next one to trip over.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun deleteKeychainItem(service: String) {
    val serviceRef = service.toCFString()
    try {
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
        )
        try {
            SecItemDelete(query)
        } finally {
            CFRelease(query)
        }
    } finally {
        CFRelease(serviceRef)
    }
}
