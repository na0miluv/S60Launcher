package com.notkia.launcher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notkia.launcher.SymbianTheme

/**
 * Tema "Classic White" basado en Symbian S60 1st Edition
 * 
 * Características:
 * - Status bar: segmento superior blanco, inferior con color de énfasis oscuro
 * - NavBar: segundo color de énfasis con tonos fuertes
 * - AppDrawer/Menús: fondo blanco con textos negros
 * - Focus: borde negro sin relleno
 * - Indicadores con opacidad degradada
 */
object ClassicWhiteTheme {
    // Colores de énfasis disponibles
    enum class AccentColor(val color: Color, val displayName: String) {
        BLUE(Color(0xFF003366), "Blue"),
        RED(Color(0xFF660000), "Red"),
        GREEN(Color(0xFF003300), "Green"),
        PURPLE(Color(0xFF330033), "Purple"),
        YELLOW(Color(0xFF666600), "Yellow"),
        BROWN(Color(0xFF331100), "Brown"),
        GRAY(Color(0xFF333333), "Gray"),
        WHITE(Color(0xFFFFFFFF), "White"),
        BLACK(Color(0xFF000000), "Black")
    }
    
    enum class SecondaryAccentColor(val color: Color, val displayName: String) {
        WHITE(Color(0xFFFFFEFF), "White"), // Casi blanco para evitar problemas de renderizado
        BLUE(Color(0xFF003366), "Blue"),
        RED(Color(0xFF660000), "Red"),
        GREEN(Color(0xFF003300), "Green"),
        PURPLE(Color(0xFF330033), "Purple"),
        YELLOW(Color(0xFFFFFF00), "Yellow"),
        BROWN(Color(0xFF331100), "Brown"),
        GRAY(Color(0xFF333333), "Gray"),
        BLACK(Color(0xFF000000), "Black")
    }
    
    /**
     * Crea el tema Classic White con los colores de énfasis especificados
     */
    fun create(
        accentColor: AccentColor = AccentColor.BLUE,
        secondaryAccentColor: SecondaryAccentColor = SecondaryAccentColor.WHITE
    ): SymbianTheme {
        val accent = accentColor.color
        val secondaryAccent = secondaryAccentColor.color
        
        // Determinar colores de texto según el color de énfasis
        val bottomSegmentTextColor = if (accentColor == AccentColor.WHITE) {
            Color.Black
        } else {
            Color.White
        }
        
        val signalBarsColor = if (accentColor == AccentColor.WHITE) {
            Color.Black
        } else {
            Color.White
        }
        
        val batteryBarsColor = signalBarsColor
        
        // Determinar color de texto de NavBar
        val navBarTextColor = when (secondaryAccentColor) {
            SecondaryAccentColor.WHITE, SecondaryAccentColor.YELLOW -> Color.Black
            else -> Color.White
        }
        
        // Usar un color casi blanco en lugar de Color.White puro para evitar problemas de renderizado
        val almostWhite = Color(0xFFFFFEFF) // Blanco con un toque casi imperceptible de azul
        
        return SymbianTheme(
            // Status bar - segmento superior blanco (casi blanco para evitar problemas)
            topSegmentBackground = almostWhite,
            topSegmentTextColor = Color.Black,
            
            // Status bar - segmento inferior con color de énfasis
            bottomSegmentBackground = accent,
            bottomSegmentTextColor = bottomSegmentTextColor,
            clockTextColor = Color.Black, // Reloj en segmento superior
            
            // Iconos del segmento inferior
            signalIconColor = bottomSegmentTextColor,
            batteryIconColor = bottomSegmentTextColor,
            
            // Barras indicadoras (mismo color que texto del segmento inferior)
            signalBarsBackground = Color.Transparent,
            batteryBarsBackground = Color.Transparent,
            
            // NavBar - segundo color de énfasis
            navBarBackground = secondaryAccent,
            navBarTextColor = navBarTextColor,
            
            // AppDrawer y menús - fondo blanco con textos negros
            appDrawerBackground = almostWhite,
            appDrawerTextColor = Color.Black,
            menuBackground = almostWhite,
            menuTextColor = Color.Black,
            subMenuBackground = almostWhite,
            subMenuTextColor = Color.Black,
            
            // Focus - borde negro sin relleno
            focusColor = Color.Transparent,
            focusOpacity = 0f,
            focusRadius = 0.dp,
            focusBorderColor = Color.Black,
            focusBorderWidth = 2.dp,
            focusHasFill = false,
            focusTextColor = Color.Unspecified, // Sin cambio de color de texto
            
            // Colores de énfasis
            accentColor = accent,
            secondaryAccentColor = secondaryAccent,
            
            // Scrollbar del mismo color de énfasis
            scrollbarColor = accent,
            
            // Opacidad degradada para indicadores
            useGradientOpacityForIndicators = true,
            
            // Elementos de lista sin redondeo
            listItemCornerRadius = 0.dp
        )
    }
    
    /**
     * Tema por defecto Classic White con azul como color de énfasis
     */
    fun default(): SymbianTheme {
        return create()
    }
}

