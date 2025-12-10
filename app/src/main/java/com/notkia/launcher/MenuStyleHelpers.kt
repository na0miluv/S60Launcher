package com.notkia.launcher

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette

/**
 * Obtiene un color de énfasis oscuro basado en el wallpaper
 * Usa generate() síncrono para obtener el color inmediatamente
 */
fun getDarkAccentColorFromWallpaper(context: Context, wallpaperDrawable: Drawable?): Color {
    if (wallpaperDrawable == null) {
        // Color por defecto oscuro si no hay wallpaper
        return Color(0xFF4A4A4A)
    }
    
    return try {
        val bitmap = wallpaperDrawable.toBitmap()
        if (bitmap == null || bitmap.isRecycled) {
            return Color(0xFF4A4A4A)
        }
        
        // Generar paleta de forma síncrona
        val palette = Palette.from(bitmap).generate()
        
        // Intentar obtener un color oscuro y vibrante
        val darkVibrant = palette.darkVibrantSwatch
        val darkMuted = palette.darkMutedSwatch
        val vibrant = palette.vibrantSwatch
        
        val swatch = darkVibrant ?: darkMuted ?: vibrant
        
        if (swatch != null) {
            // Oscurecer el color aún más para que sea un buen color de énfasis
            val rgb = swatch.rgb
            val hsl = FloatArray(3)
            ColorUtils.RGBToHSL(
                android.graphics.Color.red(rgb),
                android.graphics.Color.green(rgb),
                android.graphics.Color.blue(rgb),
                hsl
            )
            // Reducir la luminosidad para hacerlo más oscuro
            hsl[2] = hsl[2].coerceAtMost(0.3f) // Máximo 30% de luminosidad
            val darkenedRgb = ColorUtils.HSLToColor(hsl)
            Color(darkenedRgb)
        } else {
            Color(0xFF4A4A4A)
        }
    } catch (e: Exception) {
        android.util.Log.e("MenuStyleHelpers", "Error getting accent color from wallpaper", e)
        Color(0xFF4A4A4A)
    }
}

/**
 * Composable para mostrar un elemento de menú con estilo de configuración
 * Muestra el nombre del submenú en el primer renglón y la vista previa del ajuste activo en el segundo renglón
 */
@Composable
fun MenuConfigItem(
    label: String,
    currentValue: String,
    accentColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Primer renglón: nombre del submenú
        Text(
            text = label,
            color = textColor
        )
        // Segundo renglón: vista previa del ajuste activo (alineado a la derecha)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp) // Ancho fijo
                    .background(
                        color = accentColor,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentValue,
                    color = Color.White, // Siempre blanco, no afectado por el highlight
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

