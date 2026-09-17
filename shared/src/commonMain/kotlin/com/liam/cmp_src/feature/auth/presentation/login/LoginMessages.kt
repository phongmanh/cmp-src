package com.liam.cmp_src.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import com.liam.cmp_src.feature.auth.domain.model.EmailError
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.validation_email_blank
import cmpsrc.shared.generated.resources.validation_email_malformed
import org.jetbrains.compose.resources.stringResource

/**
 * Display text for the validation errors only the auth screens produce. The shared domain types
 * (`AuthError`, `PasswordError`, `SocialProvider`) are mapped in `core.ui.message`.
 */
@Composable
fun EmailError.asMessage(): String = when (this) {
    EmailError.Blank -> stringResource(Res.string.validation_email_blank)
    EmailError.Malformed -> stringResource(Res.string.validation_email_malformed)
}
