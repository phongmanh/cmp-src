package com.liam.cmp_src.feature.profile.domain.usecase

import com.example.api.common.FieldLimits
import com.liam.cmp_src.feature.auth.domain.model.DisplayNameError

/**
 * Checks a display name against the bound the server enforces, before anything is sent.
 *
 * The limit comes from the contract's [FieldLimits] rather than being restated here, so the two
 * sides cannot disagree about where the line is.
 *
 * An empty name is legal and is *not* an error: the contract clears a display name by sending
 * `null`, and the server refuses only a blank string, which this app never sends — see
 * [UpdateDisplayNameUseCase]. Length is therefore the one thing left to check.
 */
class ValidateDisplayNameUseCase {

    operator fun invoke(displayName: String): DisplayNameError? =
        if (displayName.trim().length > FieldLimits.MAX_DISPLAY_NAME_LENGTH) {
            DisplayNameError.TooLong(FieldLimits.MAX_DISPLAY_NAME_LENGTH)
        } else {
            null
        }
}
