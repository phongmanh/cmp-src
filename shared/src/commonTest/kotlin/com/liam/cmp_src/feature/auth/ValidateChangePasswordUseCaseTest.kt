package com.liam.cmp_src.feature.auth

import com.example.api.common.FieldLimits
import com.liam.cmp_src.feature.auth.domain.model.PasswordError
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateChangePasswordUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val CURRENT = "a-long-enough-password"
private const val REPLACEMENT = "an-entirely-different-one"

class ValidateChangePasswordUseCaseTest {

    private val validate = ValidateChangePasswordUseCase()

    @Test
    fun validInputHasNoErrors() {
        val result = validate(CURRENT, REPLACEMENT, REPLACEMENT)

        assertNull(result.currentPassword)
        assertNull(result.newPassword)
        assertNull(result.confirmPassword)
        assertFalse(result.hasErrors)
    }

    @Test
    fun blankCurrentPasswordIsRejected() {
        assertEquals(PasswordError.Blank, validate("", REPLACEMENT, REPLACEMENT).currentPassword)
    }

    @Test
    fun blankNewPasswordIsBlankNotTooShort() {
        assertEquals(PasswordError.Blank, validate(CURRENT, "", "").newPassword)
    }

    @Test
    fun shortNewPasswordReportsTheRequiredLength() {
        assertEquals(
            PasswordError.TooShort(FieldLimits.MIN_PASSWORD_LENGTH),
            validate(CURRENT, "short", "short").newPassword,
        )
    }

    @Test
    fun overLongNewPasswordReportsTheCharacterCeiling() {
        val tooLong = "a".repeat(FieldLimits.MAX_PASSWORD_LENGTH + 1)

        assertEquals(
            PasswordError.TooLong(FieldLimits.MAX_PASSWORD_LENGTH),
            validate(CURRENT, tooLong, tooLong).newPassword,
        )
    }

    /** BCrypt ignores everything past 72 bytes, so multi-byte characters cross the line sooner. */
    @Test
    fun newPasswordOverTheByteCeilingIsRejectedEvenWhenShortEnoughInCharacters() {
        // Well under MAX_PASSWORD_LENGTH characters, but three bytes each in UTF-8.
        val multiByte = "☂".repeat(30)
        assertTrue(multiByte.length < FieldLimits.MAX_PASSWORD_LENGTH)

        assertEquals(
            PasswordError.TooLong(FieldLimits.MAX_PASSWORD_BYTES),
            validate(CURRENT, multiByte, multiByte).newPassword,
        )
    }

    @Test
    fun newPasswordIdenticalToTheCurrentOneIsRejected() {
        assertEquals(
            PasswordError.SameAsCurrent,
            validate(CURRENT, CURRENT, CURRENT).newPassword,
        )
    }

    /** Case is part of a password, so a differently-cased copy is a genuinely different one. */
    @Test
    fun aDifferentlyCasedNewPasswordIsNotTheSameAsTheCurrentOne() {
        assertNull(validate(CURRENT, CURRENT.uppercase(), CURRENT.uppercase()).newPassword)
    }

    @Test
    fun confirmationThatDoesNotMatchIsRejected() {
        val result = validate(CURRENT, REPLACEMENT, "${REPLACEMENT}x")

        assertEquals(PasswordError.Mismatch, result.confirmPassword)
        // The new password itself is fine — only the copy of it is wrong.
        assertNull(result.newPassword)
    }

    /**
     * Each field is judged on its own. Reporting only the first problem would have the user fix
     * one, submit, and be told about the next.
     */
    @Test
    fun aBlankCurrentPasswordDoesNotHideTheOtherFieldsProblems() {
        val result = validate("", "short", "different")

        assertEquals(PasswordError.Blank, result.currentPassword)
        assertEquals(PasswordError.TooShort(FieldLimits.MIN_PASSWORD_LENGTH), result.newPassword)
        assertEquals(PasswordError.Mismatch, result.confirmPassword)
        assertTrue(result.hasErrors)
    }
}
