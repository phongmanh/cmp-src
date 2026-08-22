package com.liam.cmp_src.feature.auth

import com.liam.cmp_src.feature.auth.domain.model.EmailError
import com.liam.cmp_src.feature.auth.domain.model.PasswordError
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidateCredentialsUseCaseTest {

    private val validate = ValidateCredentialsUseCase()

    @Test
    fun validInputHasNoErrors() {
        val result = validate("demo@cmpsrc.dev", "password123")

        assertNull(result.email)
        assertNull(result.password)
        assertFalse(result.hasErrors)
    }

    @Test
    fun blankEmailIsRejected() {
        assertEquals(EmailError.Blank, validate("", "password123").email)
    }

    @Test
    fun whitespaceOnlyEmailIsBlankNotMalformed() {
        assertEquals(EmailError.Blank, validate("   ", "password123").email)
    }

    @Test
    fun emailWithoutDomainIsMalformed() {
        assertEquals(EmailError.Malformed, validate("demo@cmpsrc", "password123").email)
    }

    @Test
    fun emailWithoutAtSignIsMalformed() {
        assertEquals(EmailError.Malformed, validate("demo.cmpsrc.dev", "password123").email)
    }

    @Test
    fun surroundingWhitespaceDoesNotMakeAValidEmailMalformed() {
        assertNull(validate("  demo@cmpsrc.dev  ", "password123").email)
    }

    @Test
    fun blankPasswordIsRejected() {
        assertEquals(PasswordError.Blank, validate("demo@cmpsrc.dev", "").password)
    }

    @Test
    fun shortPasswordReportsTheRequiredLength() {
        val result = validate("demo@cmpsrc.dev", "short")

        assertEquals(
            PasswordError.TooShort(ValidateCredentialsUseCase.MIN_PASSWORD_LENGTH),
            result.password,
        )
    }

    @Test
    fun bothFieldsCanFailAtOnce() {
        val result = validate("", "")

        assertEquals(EmailError.Blank, result.email)
        assertEquals(PasswordError.Blank, result.password)
        assertTrue(result.hasErrors)
    }
}
