package com.powermax.patchstudio.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes
import com.powermax.patchstudio.ui.theme.PsType

/** Bouton principal : plein, accent, 52dp, coins 14dp. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = PsShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = PsColors.Accent,
            contentColor = PsColors.OnAccent,
            disabledContainerColor = PsColors.SurfaceHigh,
            disabledContentColor = PsColors.TextTertiary,
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = PsType.Button, maxLines = 1)
    }
}

/** Bouton texte secondaire, pleine largeur, 48dp. */
@Composable
fun SecondaryTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = PsShapes.Button,
        colors = ButtonDefaults.textButtonColors(contentColor = PsColors.TextSecondary),
    ) {
        Text(text, style = PsType.RowSubtitle.copy(fontWeight = PsType.Button.fontWeight), maxLines = 1)
    }
}
