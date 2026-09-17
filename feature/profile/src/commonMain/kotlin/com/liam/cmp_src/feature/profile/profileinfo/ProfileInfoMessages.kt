package com.liam.cmp_src.feature.profile.profileinfo

import androidx.compose.runtime.Composable
import com.liam.cmp_src.feature.profile.domain.model.DisplayNameError
import cmpsrc.feature.profile.generated.resources.Res
import cmpsrc.feature.profile.generated.resources.validation_display_name_too_long
import org.jetbrains.compose.resources.stringResource

/** Display text for a display name that failed validation. */
@Composable
fun DisplayNameError.asMessage(): String = when (this) {
    is DisplayNameError.TooLong ->
        stringResource(Res.string.validation_display_name_too_long, maxLength)
}
