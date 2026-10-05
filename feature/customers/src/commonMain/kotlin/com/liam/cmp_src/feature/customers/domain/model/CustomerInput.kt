package com.liam.cmp_src.feature.customers.domain.model

import com.example.api.customer.CustomerStatus

/**
 * The editor's fields exactly as typed, before anything is trimmed or checked.
 *
 * Validation turns this into a [CustomerDraft] or into [CustomerFieldError]s — see
 * `ValidateCustomerUseCase`.
 */
data class CustomerInput(
    val firstName: String = "",
    val lastName: String = "",
    val companyName: String = "",
    val email: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val region: String = "",
    val postalCode: String = "",
    val countryCode: String = "",
    val notes: String = "",
    val status: CustomerStatus? = null,
)

/** Each input on the customer form, for keying the errors validation raises against it. */
enum class CustomerField {
    FIRST_NAME,
    LAST_NAME,
    COMPANY_NAME,
    EMAIL,
    PHONE,
    ADDRESS_LINE1,
    ADDRESS_LINE2,
    CITY,
    REGION,
    POSTAL_CODE,
    COUNTRY_CODE,
    NOTES,
    STATUS,
}

/**
 * Why a field was refused. Carries no text — the editor resolves each one through a string
 * resource, the way `AuthError.asMessage()` does.
 */
sealed interface CustomerFieldError {
    data object Required : CustomerFieldError
    data class TooLong(val maxLength: Int) : CustomerFieldError
    data object InvalidEmail : CustomerFieldError

    /** Not E.164 once spaces and punctuation are taken out. */
    data object InvalidPhone : CustomerFieldError

    /** Not two letters. */
    data object InvalidCountryCode : CustomerFieldError
}

/** What validating a [CustomerInput] came to. */
sealed interface CustomerValidation {
    data class Valid(val draft: CustomerDraft) : CustomerValidation
    data class Invalid(val errors: Map<CustomerField, CustomerFieldError>) : CustomerValidation
}
