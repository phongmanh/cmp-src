package com.liam.cmp_src.feature.profile

import com.example.api.common.FieldLimits
import com.liam.cmp_src.feature.auth.domain.model.DisplayNameError
import com.liam.cmp_src.feature.profile.domain.usecase.ValidateDisplayNameUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidateDisplayNameUseCaseTest {

    private val validate = ValidateDisplayNameUseCase()

    @Test
    fun `an ordinary name passes`() {
        assertNull(validate("Ada Lovelace"))
    }

    /** Not an error: the contract clears a name by sending null, so emptying the field is a choice. */
    @Test
    fun `an empty name passes because clearing one is allowed`() {
        assertNull(validate(""))
        assertNull(validate("   "))
    }

    @Test
    fun `a name at the limit passes`() {
        assertNull(validate("a".repeat(FieldLimits.MAX_DISPLAY_NAME_LENGTH)))
    }

    @Test
    fun `a name past the limit is reported with the limit it broke`() {
        val error = validate("a".repeat(FieldLimits.MAX_DISPLAY_NAME_LENGTH + 1))

        assertEquals(DisplayNameError.TooLong(FieldLimits.MAX_DISPLAY_NAME_LENGTH), error)
    }

    /** The name is trimmed before it is sent, so it is trimmed before it is measured. */
    @Test
    fun `surrounding whitespace does not count against the limit`() {
        val name = " ${"a".repeat(FieldLimits.MAX_DISPLAY_NAME_LENGTH)} "

        assertNull(validate(name))
    }
}
