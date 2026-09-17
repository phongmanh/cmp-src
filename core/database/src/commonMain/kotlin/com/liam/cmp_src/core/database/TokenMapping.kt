package com.liam.cmp_src.core.database

import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import com.liam.cmp_src.core.network.AuthTokens
import com.liam.cmp_src.core.security.TokenCipher

/**
 * The crossing between the credential pair the app passes around and the blob the table holds.
 *
 * Both directions live here rather than inside `RoomTokenStore` so the store reads as the flow it
 * implements, and so the encoding can be tested without a database.
 */

/** The character that separates the two tokens inside the encrypted blob. */
private const val FIELD_SEPARATOR = '\n'

/**
 * Encrypts the pair as one value.
 *
 * A newline separates them because neither token can contain one: both are base64url, which has no
 * newline in its alphabet, and a JWT is only base64url segments joined by dots.
 *
 * @throws com.liam.cmp_src.core.security.TokenCipherException when the platform cannot encrypt.
 */
internal fun AuthTokens.encryptWith(cipher: TokenCipher): EncryptedAuthTokens =
    EncryptedAuthTokens(payload = cipher.encrypt("$accessToken$FIELD_SEPARATOR$refreshToken"))

/**
 * Reads the pair back, or `null` if this row cannot be trusted.
 *
 * Null covers both halves of the problem: a blob the cipher refuses, and a blob it accepts that
 * does not hold exactly two fields. The second is only reachable if something other than
 * [encryptWith] wrote the column, which is exactly when guessing would be worst.
 */
internal fun EncryptedAuthTokens.decryptWith(cipher: TokenCipher): AuthTokens? {
    val fields = cipher.decrypt(payload)?.split(FIELD_SEPARATOR) ?: return null
    if (fields.size != 2) return null
    return AuthTokens(accessToken = fields[0], refreshToken = fields[1])
}
