package com.notkia.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sistema de temas completo para simular diferentes versiones de Symbian.
 * Permite personalizar todos los aspectos visuales de la interfaz.
 */
data class SymbianTheme(
    // Colores de los segmentos de la StatusBar
    val topSegmentBackground: Color = Color.Transparent,
    val bottomSegmentBackground: Color = Color.Transparent,
    
    // Colores de texto de la StatusBar
    val topSegmentTextColor: Color = Color.Unspecified, // Si no se especifica, usa MaterialTheme
    val bottomSegmentTextColor: Color = Color.Unspecified,
    val clockTextColor: Color = Color.Unspecified,
    
    // Colores de las barras indicadoras
    val signalBarsBackground: Color = Color.Transparent,
    val batteryBarsBackground: Color = Color.Transparent,
    
    // Colores de los iconos de señal y batería (segmento inferior)
    val signalIconColor: Color = Color.Unspecified,
    val batteryIconColor: Color = Color.Unspecified,
    
    // Ancho de los segmentos de las barras
    val signalBarsWidth: Dp = 20.dp,
    val batteryBarsWidth: Dp = 20.dp,
    
    // Colores de la NavBar
    val navBarBackground: Color = Color.Unspecified,
    val navBarTextColor: Color = Color(0xFF000040),
    
    // Colores del AppDrawer
    val appDrawerBackground: Color = Color.Unspecified,
    val appDrawerTextColor: Color = Color.Unspecified,
    
    // Colores de los menús y submenús
    val menuBackground: Color = Color.Unspecified,
    val menuTextColor: Color = Color.Unspecified,
    val subMenuBackground: Color = Color.Unspecified,
    val subMenuTextColor: Color = Color.Unspecified,
    
    // Configuración del elemento enfocado
    val focusColor: Color = Color.White.copy(alpha = 0.5f),
    val focusOpacity: Float = 0.5f,
    val focusRadius: Dp = 12.dp,
    val focusTextColor: Color = Color.Unspecified, // Si no se especifica, usa lógica automática
    val focusBorderColor: Color = Color.Unspecified, // Si se especifica, usa borde en lugar de relleno
    val focusBorderWidth: Dp = 0.dp,
    val focusHasFill: Boolean = true, // Si false, solo muestra borde sin relleno
    
    // Colores de énfasis (para temas como Classic White)
    val accentColor: Color = Color.Unspecified, // Color de énfasis principal (StatusBar inferior)
    val secondaryAccentColor: Color = Color.Unspecified, // Color de énfasis secundario (NavBar)
    
    // Scrollbar
    val scrollbarColor: Color = Color.Unspecified,
    
    // Opacidad degradada para indicadores
    val useGradientOpacityForIndicators: Boolean = false, // Si true, los indicadores tienen opacidad degradada
    
    // Radio de esquinas para elementos de lista
    val listItemCornerRadius: Dp = 0.dp
) {
    companion object {
        /**
         * Tema por defecto S60v3 - Sin personalizaciones visibles
         */
        fun default(): SymbianTheme {
            return SymbianTheme()
        }
        
        /**
         * Tema S60v3 con fondos de color para debug
         */
        fun debug(): SymbianTheme {
            return SymbianTheme(
                topSegmentBackground = Color.White.copy(alpha = 0.1f),
                bottomSegmentBackground = Color.Magenta.copy(alpha = 0.1f),
                signalBarsBackground = Color.Green.copy(alpha = 0.1f),
                batteryBarsBackground = Color.Red.copy(alpha = 0.1f),
                clockTextColor = Color.Yellow.copy(alpha = 0.1f)
            )
        }
    }
}

