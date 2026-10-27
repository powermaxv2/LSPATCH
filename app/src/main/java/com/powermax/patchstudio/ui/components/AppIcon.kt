package com.powermax.patchstudio.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.powermax.patchstudio.ui.theme.PsShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Icône réelle d'une application installée (PackageManager), sinon pastille
 * de la couleur de marque avec l'initiale.
 */
@Composable
fun AppIcon(
    packageName: String,
    label: String,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val bitmap by produceState<ImageBitmap?>(null, packageName, px) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = px, height = px)
                    .asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    val icon = bitmap
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier.size(size),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(PsShapes.IconBadge)
                .background(fallbackColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.take(1).uppercase(),
                color = Color.White,
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
