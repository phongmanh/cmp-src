package com.liam.cmp_src.feature.customers

import com.example.api.common.FieldLimits
import com.example.api.customer.CustomerAddress
import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.feature.customers.domain.model.CustomerField
import com.liam.cmp_src.feature.customers.domain.model.CustomerFieldError
import com.liam.cmp_src.feature.customers.domain.model.CustomerInput
import com.liam.cmp_src.feature.customers.domain.model.CustomerValidation
import com.liam.cmp_src.feature.customers.domain.usecase.ValidateCustomerUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ValidateCustomerUseCaseTest {

    private val validate = ValidateCustomerUseCase()

    @Test
    fun `a first name and a status are all a customer needs`() {
        val result = validate(CustomerInput(firstName = "Ada", status = CustomerStatus.LEAD))

        val draft = assertIs<CustomerValidation.Valid>(result).draft
        assertEquals("Ada", draft.firstName)
        assertNull(draft.address)
    }

    @Test
    fun `text is trimmed and blank optional fields become null`() {
        val result = validate(VALID.copy(firstName = "  Ada ", lastName = "   ", notes = " hi "))

        val draft = assertIs<CustomerValidation.Valid>(result).draft
        assertEquals("Ada", draft.firstName)
        assertNull(draft.lastName)
        assertEquals("hi", draft.notes)
    }

    @Test
    fun `a missing first name and status are both reported`() {
        val result = validate(CustomerInput(firstName = " "))

        assertEquals(
            mapOf(
                CustomerField.FIRST_NAME to CustomerFieldError.Required,
                CustomerField.STATUS to CustomerFieldError.Required,
            ),
            assertIs<CustomerValidation.Invalid>(result).errors,
        )
    }

    @Test
    fun `a name longer than the server allows is refused`() {
        val result = validate(VALID.copy(firstName = "a".repeat(FieldLimits.MAX_CUSTOMER_NAME_LENGTH + 1)))

        assertEquals(
            CustomerFieldError.TooLong(FieldLimits.MAX_CUSTOMER_NAME_LENGTH),
            assertIs<CustomerValidation.Invalid>(result).errors[CustomerField.FIRST_NAME],
        )
    }

    @Test
    fun `a malformed email is refused`() {
        val result = validate(VALID.copy(email = "ada@"))

        assertEquals(
            CustomerFieldError.InvalidEmail,
            assertIs<CustomerValidation.Invalid>(result).errors[CustomerField.EMAIL],
        )
    }

    @Test
    fun `a phone number loses its spaces and punctuation before it is checked`() {
        val result = validate(VALID.copy(phone = "+44 (20) 7946-0958"))

        assertEquals("+442079460958", assertIs<CustomerValidation.Valid>(result).draft.phone)
    }

    @Test
    fun `a phone number without a country code is refused`() {
        val result = validate(VALID.copy(phone = "020 7946 0958"))

        assertEquals(
            CustomerFieldError.InvalidPhone,
            assertIs<CustomerValidation.Invalid>(result).errors[CustomerField.PHONE],
        )
    }

    @Test
    fun `half an address names every part it is missing`() {
        val result = validate(VALID.copy(postalCode = "WC1N 3AR"))

        val errors = assertIs<CustomerValidation.Invalid>(result).errors
        assertEquals(
            setOf(CustomerField.ADDRESS_LINE1, CustomerField.CITY, CustomerField.COUNTRY_CODE),
            errors.keys,
        )
    }

    @Test
    fun `a whole address is built and its country code upper-cased`() {
        val result = validate(
            VALID.copy(addressLine1 = "12 Queen Square", city = "London", countryCode = "gb"),
        )

        assertEquals(
            CustomerAddress(line1 = "12 Queen Square", city = "London", countryCode = "GB"),
            assertIs<CustomerValidation.Valid>(result).draft.address,
        )
    }

    @Test
    fun `a country code that is not two letters is refused`() {
        val result = validate(VALID.copy(addressLine1 = "1 Road", city = "Town", countryCode = "G1"))

        assertEquals(
            CustomerFieldError.InvalidCountryCode,
            assertIs<CustomerValidation.Invalid>(result).errors[CustomerField.COUNTRY_CODE],
        )
    }

    private companion object {
        val VALID = CustomerInput(firstName = "Ada", status = CustomerStatus.ACTIVE)
    }
}
