package com.liam.cmp_src.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors

/**
 * The glass treatment itself: a translucent fill over the aurora backdrop with a hairline border
 * catching the light, in whatever [shape] the caller needs.
 *
 * [GlassCard] is this with a padded column inside it — the shape the signed-in screens sit their
 * content in. Everything that is glass but *not* a card (the header panel, the navigation pill,
 * the round icon buttons, the status chips) comes through here, so the fill and the border are
 * spelled once rather than per component.
 *
 * Passing [onClick] gives the clickable [Surface], which brings the ripple and the semantics with
 * it. Pair it with [com.liam.cmp_src.core.ui.modifier.pressScale] and the [interactionSource]
 * given here to add the squeeze.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.radiusXl),
    contentColor: Color = LocalContentColor.current,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val glass = auroraColors
    val border = BorderStroke(Dimens.hairline, glass.glassBorder)

    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = glass.glassFill,
            contentColor = contentColor,
            border = border,
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = glass.glassFill,
            contentColor = contentColor,
            border = border,
            interactionSource = interactionSource,
            content = content,
        )
    }
}
