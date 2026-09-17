package com.liam.cmp_src.feature.profile.domain.model

/**
 * Why a display name failed validation.
 *
 * Only one case: the contract lets a name be cleared by sending `null`, so an empty field is a
 * deliberate choice rather than a mistake, and length is the single bound left to check.
 */
sealed interface DisplayNameError {
    data class TooLong(val maxLength: Int) : DisplayNameError
}
