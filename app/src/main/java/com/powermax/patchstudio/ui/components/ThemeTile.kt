package com.powermax.patchstudio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.motion.pressScale
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes
import com.powermax.patchstudio.ui.theme.PsType

/**
 * Vignette de thème : carré 64dp montrant le fond, une surface et la pastille d'accent.
 * Anneau 2dp accent quand sélectionnée.
 */
@Composable
fun ThemeTile(
    name: String,
    background: Color,
    surface: Color,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val ring by animateColorAsState(
        targetValue = if (selected) PsColors.Accent else Color.Transparent,
        animationSpec = tween(PsMotion.DurationShort),
        label = "tileRing",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) PsColors.TextPrimary else PsColors.TextSecondary,
        animationSpec = tween(PsMotion.DurationShort),
        label = "tileLabel",
    )
    Column(
        modifier = modifier
            .width(72.dp)
            .pressScale(interaction)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(2.dp, ring, RoundedCornerShape(20.dp))
                .padding(4.dp)
                .clip(PsShapes.Tile)
                .background(background)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PsShapes.Tile),
        ) {
            // Une « carte » de surface et la pastille d'accent.
            Box(
                Modifier
                    .padding(start = 8.dp, top = 10.dp)
                    .size(width = 28.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(surface),
            )
            Box(
                Modifier
                    .padding(start = 8.dp, top = 20.dp)
                    .size(width = 18.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(surface),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = PsType.Caption,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
