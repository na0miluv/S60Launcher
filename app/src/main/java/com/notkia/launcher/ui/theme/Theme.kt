package com.notkia.launcher.ui.theme

import com.notkia.launcher.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import com.notkia.launcher.WallpaperManager

val RobotoCondensed = FontFamily(
    Font(R.font.robotocondensed_light, weight = FontWeight.Light),
    Font(R.font.robotocondensed_regular, weight = FontWeight.Normal),
    Font(R.font.robotocondensed_bold, weight = FontWeight.Bold)
)

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Light
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Bold
    )
)

private val SymbianLightColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,

    background = Color.White,
    onBackground = Color(0xFF001A33),

    surface = Color.White,
    onSurface = Color(0xFF001A33),

    secondary = Color(0xFFE6F0FF),
    onSecondary = Color(0xFF001A33),

    outline = Color(0xFF445566)
)

private val SymbianDarkColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),

    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF131313),
    onSurface = Color(0xFFEFEFEF),

    secondary = Color(0xFF222222),
    onSecondary = Color(0xFFEFEFEF),

    outline = Color(0xFF666666)
)

val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
)

object SymbianExtras {
    val highlightFill = Color(0x33FFFFFF)
    val highlightBorder = Color(0x88FFFFFF)

    val highlightFillLight = Color(0x22001A33)
    val highlightBorderLight = Color(0x88001A33)

    val nokiaBlue = Color(0xFF0082CA)
    val gridFocus = Color(0x55FFFFFF)
}

/**
 * Sistema de temas para la StatusBar que permite replicar diferentes estilos de Symbian.
 * Por defecto usa S60v3, pero puede extenderse para otros estilos (S60v5, S^3, etc.)
 * 
 * @deprecated Usar SymbianTheme y ThemeManager en su lugar
 */
data class StatusBarTheme(
    val signalBackground: Color = Color.Transparent,      // Verde - Indicador de señal
    val clockBackground: Color = Color.Transparent,      // Amarillo - Área de reloj
    val topTextBackground: Color = Color.Transparent,    // Blanco - Texto superior
    val bottomTextBackground: Color = Color.Transparent,  // Rosa - Texto inferior
    val batteryBackground: Color = Color.Transparent     // Rojo - Indicador de batería
)

object StatusBarThemes {
    /**
     * Tema S60v3 (actual) - Sin fondos de color visibles por defecto
     */
    val S60v3 = StatusBarTheme()

    /**
     * Tema con fondos de color para visualización/debug
     */
    val S60v3Debug = StatusBarTheme(
        signalBackground = Color.Green.copy(alpha = 0.1f),
        clockBackground = Color.Yellow.copy(alpha = 0.1f),
        topTextBackground = Color.White.copy(alpha = 0.1f),
        bottomTextBackground = Color.Magenta.copy(alpha = 0.1f),
        batteryBackground = Color.Red.copy(alpha = 0.1f)
    )

    // Aquí se pueden agregar más temas en el futuro:
    // val S60v5 = StatusBarTheme(...)
    // val S3 = StatusBarTheme(...)
    
    /**
     * Convierte un SymbianTheme a StatusBarTheme para compatibilidad
     */
    fun fromSymbianTheme(theme: com.notkia.launcher.SymbianTheme): StatusBarTheme {
        return StatusBarTheme(
            signalBackground = theme.signalBarsBackground,
            clockBackground = Color.Transparent, // El reloj no tiene fondo en el nuevo sistema
            topTextBackground = theme.topSegmentBackground,
            bottomTextBackground = theme.bottomSegmentBackground,
            batteryBackground = theme.batteryBarsBackground
        )
    }
}

@Composable
fun S60LauncherTheme(content: @Composable () -> Unit) {
    val isDark by WallpaperManager.isDarkTheme
    val colors = if (isDark) SymbianDarkColors else SymbianLightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
