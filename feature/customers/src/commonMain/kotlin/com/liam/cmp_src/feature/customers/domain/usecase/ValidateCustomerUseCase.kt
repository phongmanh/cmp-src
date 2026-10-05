package com.liam.cmp_src.feature.customers.domain.usecase

import com.example.api.common.FieldLimits
import com.example.api.customer.CustomerAddress
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.CustomerField
import com.liam.cmp_src.feature.customers.domain.model.CustomerFieldError
import com.liam.cmp_src.feature.customers.domain.model.CustomerInput
import com.liam.cmp_src.feature.customers.domain.model.CustomerValidation

/**
 * Checks the customer form against every rule the server enforces, and builds the draft to save.
 *
 * This matters more here than on any other form in the app: a save is stored and only sent later,
 * so a value the server would refuse is not an error message now but a customer stuck unsynced
 * later. The limits and patterns come from the contract's [FieldLimits] so the two sides cannot
 * disagree.
 *
 * Also normalises what it accepts: text is trimmed and blanks become `null`, a phone number loses
 * the spaces and punctuation people type into one (the server wants bare E.164), and a country
 * code is upper-cased.
 */
class ValidateCustomerUseCase {

    operator fun invoke(input: CustomerInput): CustomerValidation {
        val errors = mutableMapOf<CustomerField, CustomerFieldError>()

        val firstName = input.firstName.trim()
        if (firstName.isEmpty()) errors[CustomerField.FIRST_NAME] = CustomerFieldError.Required
        errors.checkLength(CustomerField.FIRST_NAME, firstName, FieldLimits.MAX_CUSTOMER_NAME_LENGTH)

        val lastName = input.lastName.trimToNull()
        errors.checkLength(CustomerField.LAST_NAME, lastName, FieldLimits.MAX_CUSTOMER_NAME_LENGTH)

        val companyName = input.companyName.trimToNull()
        errors.checkLength(CustomerField.COMPANY_NAME, companyName, FieldLimits.MAX_COMPANY_NAME_LENGTH)

        val email = input.email.trimToNull()
        if (email != null && !FieldLimits.EMAIL_PATTERN.matches(email)) {
            errors[CustomerField.EMAIL] = CustomerFieldError.InvalidEmail
        }
        errors.checkLength(CustomerField.EMAIL, email, FieldLimits.MAX_EMAIL_LENGTH)

        val phone = input.phone.filterNot { it in PHONE_PUNCTUATION }.trimToNull()
        if (phone != null && !FieldLimits.PHONE_PATTERN.matches(phone)) {
            errors[CustomerField.PHONE] = CustomerFieldError.InvalidPhone
        }

        val notes = input.notes.trimToNull()
        errors.checkLength(CustomerField.NOTES, notes, FieldLimits.MAX_CUSTOMER_NOTES_LENGTH)

        if (input.status == null) errors[CustomerField.STATUS] = CustomerFieldError.Required

        val address = validateAddress(input, errors)

        if (errors.isNotEmpty()) return CustomerValidation.Invalid(errors)
        return CustomerValidation.Valid(
            CustomerDraft(
                firstName = firstName,
                lastName = lastName,
                companyName = companyName,
                email = email,
                phone = phone,
                address = address,
                notes = notes,
                status = input.status,
            ),
        )
    }

    /**
     * The address is all or nothing: leaving every field empty means no address, but once any is
     * filled the server needs a first line, a city and a country to post anything to.
     */
    private fun validateAddress(
        input: CustomerInput,
        errors: MutableMap<CustomerField, CustomerFieldError>,
    ): CustomerAddress? {
        val line1 = input.addressLine1.trimToNull()
        val line2 = input.addressLine2.trimToNull()
        val city = input.city.trimToNull()
        val region = input.region.trimToNull()
        val postalCode = input.postalCode.trimToNull()
        val countryCode = input.countryCode.trimToNull()?.uppercase()

        if (listOf(line1, line2, city, region, postalCode, countryCode).all { it == null }) return null

        if (line1 == null) errors[CustomerField.ADDRESS_LINE1] = CustomerFieldError.Required
        if (city == null) errors[CustomerField.CITY] = CustomerFieldError.Required
        when {
            countryCode == null -> errors[CustomerField.COUNTRY_CODE] = CustomerFieldError.Required
            !FieldLimits.COUNTRY_CODE_PATTERN.matches(countryCode) ->
                errors[CustomerField.COUNTRY_CODE] = CustomerFieldError.InvalidCountryCode
        }
        errors.checkLength(CustomerField.ADDRESS_LINE1, line1, FieldLimits.MAX_ADDRESS_LINE_LENGTH)
        errors.checkLength(CustomerField.ADDRESS_LINE2, line2, FieldLimits.MAX_ADDRESS_LINE_LENGTH)
        errors.checkLength(CustomerField.CITY, city, FieldLimits.MAX_CITY_LENGTH)
        errors.checkLength(CustomerField.REGION, region, FieldLimits.MAX_REGION_LENGTH)
        errors.checkLength(CustomerField.POSTAL_CODE, postalCode, FieldLimits.MAX_POSTAL_CODE_LENGTH)

        if (line1 == null || city == null || countryCode == null) return null
        return CustomerAddress(
            line1 = line1,
            line2 = line2,
            city = city,
            region = region,
            postalCode = postalCode,
            countryCode = countryCode,
        )
    }

    /** Records [CustomerFieldError.TooLong] for [field], unless it already has an error. */
    private fun MutableMap<CustomerField, CustomerFieldError>.checkLength(
        field: CustomerField,
        value: String?,
        maxLength: Int,
    ) {
        if (value != null && value.length > maxLength) {
            getOrPut(field) { CustomerFieldError.TooLong(maxLength) }
        }
    }

    private fun String.trimToNull(): String? = trim().ifEmpty { null }

    private companion object {
        /** What people type between the digits of a phone number, and E.164 leaves out. */
        val PHONE_PUNCTUATION = setOf(' ', '-', '(', ')', '.')
    }
}
