package com.liam.cmp_src.feature.customers.component

import androidx.compose.runtime.Composable
import cmpsrc.feature.customers.generated.resources.Res
import cmpsrc.feature.customers.generated.resources.customer_status_active
import cmpsrc.feature.customers.generated.resources.customer_status_inactive
import cmpsrc.feature.customers.generated.resources.customer_status_lead
import cmpsrc.feature.customers.generated.resources.customers_unnamed
import cmpsrc.feature.customers.generated.resources.validation_customer_country_code
import cmpsrc.feature.customers.generated.resources.validation_customer_email
import cmpsrc.feature.customers.generated.resources.validation_customer_phone
import cmpsrc.feature.customers.generated.resources.validation_customer_required
import cmpsrc.feature.customers.generated.resources.validation_customer_too_long
import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.CustomerFieldError
import org.jetbrains.compose.resources.stringResource

/**
 * The customer feature's domain values in words, resolved through string resources — the same job
 * `asMessage()`/`asLabel()` in `core:ui` do for the account types.
 */

/** "First Last", else the company, else a placeholder — never blank. */
@Composable
fun CustomerDraft.displayName(): String =
    listOf(firstName, lastName.orEmpty())
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifEmpty { companyName.orEmpty() }
        .ifEmpty { stringResource(Res.string.customers_unnamed) }

@Composable
fun CustomerStatus.asLabel(): String = stringResource(
    when (this) {
        CustomerStatus.LEAD -> Res.string.customer_status_lead
        CustomerStatus.ACTIVE -> Res.string.customer_status_active
        CustomerStatus.INACTIVE -> Res.string.customer_status_inactive
    },
)

@Composable
fun CustomerFieldError.asMessage(): String = when (this) {
    CustomerFieldError.Required -> stringResource(Res.string.validation_customer_required)
    is CustomerFieldError.TooLong -> stringResource(Res.string.validation_customer_too_long, maxLength)
    CustomerFieldError.InvalidEmail -> stringResource(Res.string.validation_customer_email)
    CustomerFieldError.InvalidPhone -> stringResource(Res.string.validation_customer_phone)
    CustomerFieldError.InvalidCountryCode -> stringResource(Res.string.validation_customer_country_code)
}
