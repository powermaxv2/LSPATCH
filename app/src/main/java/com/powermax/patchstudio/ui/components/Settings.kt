package com.powermax.patchstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes
import com.powermax.patchstudio.ui.theme.PsType

/** Retrait des séparateurs pour une ligne avec pastille d'icône (16 + 32 + 16). */
val RowDividerIndentWithIcon: Dp = 64.dp

/** Titre de section en capitales, style Réglages. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = PsType.SectionLabel,
        color = PsColors.TextTertiary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/** Texte d'aide discret sous un groupe. */
@Composable
fun SectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PsType.Caption,
        color = PsColors.TextTertiary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    )
}

/** Carte arrondie qui regroupe des lignes. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PsShapes.Group)
            .background(PsColors.Surface),
        content = content,
    )
}

/** Séparateur 1dp, indenté pour s'aligner sur le texte des lignes. */
@Composable
fun RowDivider(indent: Dp = RowDividerIndentWithIcon) {
    HorizontalDivider(
        modifier = Modifier.padding(start = indent),
        thickness = 1.dp,
        color = PsColors.Hairline,
    )
}

/** Pastille d'icône 32dp arrondie 8dp. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    background: Color = PsColors.SurfaceHigh,
    tint: Color = PsColors.TextPrimary,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(PsShapes.IconBadge)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Point de statut. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * Ligne de réglage générique : [leading] (ou [icon]), titre, sous-titre, contrôle à droite.
 * [rowModifier] porte l'interaction (clickable, toggleable…) sur toute la ligne.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    rowModifier: Modifier = Modifier,
    titleColor: Color = PsColors.TextPrimary,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(rowModifier)
            .heightIn(min = if (subtitle != null) 64.dp else 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> {
                leading()
                Spacer(Modifier.width(16.dp))
            }
            icon != null -> {
                IconBadge(icon)
                Spacer(Modifier.width(16.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                style = PsType.RowTitle,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PsType.RowSubtitle,
                    color = PsColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** Ligne de navigation (chevron à droite, valeur optionnelle). */
@Composable
fun NavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        leading = leading,
        modifier = modifier,
        rowModifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        if (value != null) {
            Text(value, style = PsType.RowSubtitle, color = PsColors.TextSecondary, maxLines = 1)
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PsColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun psSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = PsColors.OnAccent,
    checkedTrackColor = PsColors.Accent,
    checkedBorderColor = PsColors.Accent,
    checkedIconColor = PsColors.Accent,
    uncheckedThumbColor = PsColors.TextSecondary,
    uncheckedTrackColor = PsColors.Control,
    uncheckedBorderColor = PsColors.Control,
    uncheckedIconColor = PsColors.Control,
)

/** Ligne avec interrupteur ; toute la ligne est cliquable. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        rowModifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = psSwitchColors(),
        )
    }
}

/** Ligne avec curseur fin et valeur affichée à droite du titre. */
@Composable
fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    steps: Int = 0,
) {
    Column(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconBadge(icon)
                Spacer(Modifier.width(16.dp))
            }
            Text(title, style = PsType.RowTitle, color = PsColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(valueLabel(value), style = PsType.RowSubtitle, color = PsColors.TextSecondary)
        }
        ThinSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            contentDescription = title,
            stateLabel = valueLabel(value),
            modifier = Modifier.padding(
                start = if (icon != null) RowDividerIndentWithIcon - 10.dp else 6.dp,
                end = 6.dp,
            ),
        )
    }
}
