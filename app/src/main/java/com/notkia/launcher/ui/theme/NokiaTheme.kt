package com.notkia.launcher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notkia.launcher.SymbianTheme

/**
 * Tema "Nokia" - Tema por defecto original
 * 
 * Este tema restablece todos los valores a los predeterminados del sistema,
 * que es el comportamiento original antes de la personalización de temas.
 * 
 * Características:
 * - Todos los valores usan los defaults de SymbianTheme
 * - Sin personalizaciones visibles (fondos transparentes)
 * - Colores de texto basados en MaterialTheme
 * - Focus con relleno blanco semitransparente y esquinas redondeadas
 */
object NokiaTheme {
    /**
     * Crea el tema Nokia con todos los valores por defecto
     * Esto restablece el tema a su estado original
     */
    fun create(): SymbianTheme {
        return SymbianTheme(
            // Todos los valores por defecto
            topSegmentBackground = Color.Transparent,
            bottomSegmentBackground = Color.Transparent,
            topSegmentTextColor = Color.Unspecified,
            bottomSegmentTextColor = Color.Unspecified,
            clockTextColor = Color.Unspecified,
            signalBarsBackground = Color.Transparent,
            batteryBarsBackground = Color.Transparent,
            signalIconColor = Color.Unspecified,
            batteryIconColor = Color.Unspecified,
            signalBarsWidth = 20.dp,
            batteryBarsWidth = 20.dp,
            navBarBackground = Color.Unspecified,
            navBarTextColor = Color(0xFF000040),
            appDrawerBackground = Color.Unspecified,
            appDrawerTextColor = Color.Unspecified,
            menuBackground = Color.Unspecified,
            menuTextColor = Color.Unspecified,
            subMenuBackground = Color.Unspecified,
            subMenuTextColor = Color.Unspecified,
            focusColor = Color.White.copy(alpha = 0.5f),
            focusOpacity = 0.5f,
            focusRadius = 12.dp,
            focusTextColor = Color.Unspecified,
            focusBorderColor = Color.Unspecified,
            focusBorderWidth = 0.dp,
            focusHasFill = true,
            accentColor = Color.Unspecified,
            secondaryAccentColor = Color.Unspecified,
            scrollbarColor = Color.Unspecified,
            useGradientOpacityForIndicators = false,
            listItemCornerRadius = 0.dp
        )
    }
    
    /**
     * Tema por defecto Nokia (alias para create())
     */
    fun default(): SymbianTheme {
        return create()
    }
}


