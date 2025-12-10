package com.notkia.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Manager para manejar los resaltados de foco y la navegación con teclado.
 * Solo muestra highlights cuando el dispositivo tiene teclado físico.
 */

/**
 * Obtiene el color del highlight desde el tema o usa el valor por defecto.
 */
private fun getFocusHighlightColor(theme: SymbianTheme): Color {
    return theme.focusColor.copy(alpha = theme.focusOpacity)
}

/**
 * Obtiene el radio del focus desde el tema o usa el valor por defecto.
 */
private fun getFocusRadius(theme: SymbianTheme): androidx.compose.ui.unit.Dp {
    return theme.focusRadius
}

/**
 * Calcula la luminancia relativa de un color (usando la fórmula WCAG).
 * Retorna un valor entre 0 (oscuro) y 1 (claro).
 */
private fun Color.getLuminance(): Float {
    // Convertir a valores lineales (sRGB gamma correction)
    fun toLinear(component: Float): Float {
        return if (component <= 0.03928f) {
            component / 12.92f
        } else {
            ((component + 0.055f) / 1.055f).pow(2.4f)
        }
    }
    
    val r = toLinear(red)
    val g = toLinear(green)
    val b = toLinear(blue)
    
    // Fórmula de luminancia relativa
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/**
 * Determina si un color es lo suficientemente claro para usar texto negro.
 * Usa un umbral de luminancia de 0.5 (basado en WCAG).
 */
private fun Color.isLightEnoughForBlackText(): Boolean {
    return this.getLuminance() > 0.5f
}

/**
 * Obtiene el color del texto apropiado basado en el estado de foco y el color del highlight.
 * Si el elemento está enfocado y el highlight es claro, retorna negro o el color del tema.
 * De lo contrario, retorna el color por defecto.
 */
@Composable
fun getFocusedTextColor(
    isFocused: Boolean, 
    hasPhysicalKeyboard: Boolean, 
    defaultColor: Color,
    context: android.content.Context
): Color {
    val theme = ThemeManager.rememberTheme(context)
    val focusColor = getFocusHighlightColor(theme)
    
    // Si hay un color de texto de focus especificado en el tema, usarlo
    if (isFocused && hasPhysicalKeyboard && theme.focusTextColor != Color.Unspecified) {
        return theme.focusTextColor
    }
    
    // Si no, usar la lógica automática basada en la luminancia
    return if (isFocused && hasPhysicalKeyboard && focusColor.isLightEnoughForBlackText()) {
        Color.Black
    } else {
        defaultColor
    }
}

/**
 * Modifier reutilizable para el estilo de foco de elementos de aplicaciones.
 * Aplica un fondo con el color del tema con esquinas redondeadas cuando el elemento está enfocado.
 * Solo muestra el highlight si el dispositivo tiene teclado físico.
 */
@Composable
fun Modifier.focusedAppItemStyle(
    isFocused: Boolean, 
    hasPhysicalKeyboard: Boolean = true,
    context: android.content.Context
): Modifier {
    val theme = ThemeManager.rememberTheme(context)
    val focusColor = getFocusHighlightColor(theme)
    val focusRadius = getFocusRadius(theme)
    
    return if (isFocused && hasPhysicalKeyboard) {
        if (theme.focusHasFill) {
            // Modo relleno tradicional
            this.background(
                color = focusColor,
                shape = RoundedCornerShape(focusRadius)
            )
        } else if (theme.focusBorderColor != Color.Unspecified) {
            // Modo borde sin relleno
            this.border(
                width = theme.focusBorderWidth,
                color = theme.focusBorderColor,
                shape = RoundedCornerShape(focusRadius)
            )
        } else {
            // Fallback: sin estilo de focus
            this
        }
    } else {
        this
    }
}

/**
 * Modifier condicional para hacer un elemento focusable solo si hay teclado físico.
 */
fun Modifier.conditionalFocusable(hasPhysicalKeyboard: Boolean): Modifier {
    return if (hasPhysicalKeyboard) {
        this.focusable()
    } else {
        this
    }
}

/**
 * Modifier condicional para manejar eventos de teclado solo si hay teclado físico.
 */
fun Modifier.conditionalKeyEvents(
    hasPhysicalKeyboard: Boolean,
    onKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean
): Modifier {
    return if (hasPhysicalKeyboard) {
        this.onKeyEvent(onKeyEvent)
    } else {
        this
    }
}

/**
 * Estado de foco para elementos de menú con detección automática de teclado físico.
 */
@Composable
fun rememberMenuFocusState(): Pair<Boolean, (Boolean) -> Unit> {
    val context = LocalContext.current
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }
    var isFocused by remember { mutableStateOf(false) }
    
    return Pair(isFocused && hasPhysicalKeyboard) { focused ->
        isFocused = focused
    }
}

/**
 * Estado de foco para elementos de aplicación con detección automática de teclado físico.
 */
@Composable
fun rememberAppFocusState(): Triple<Boolean, Boolean, (Boolean) -> Unit> {
    val context = LocalContext.current
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }
    var isFocused by remember { mutableStateOf(false) }
    
    return Triple(isFocused, hasPhysicalKeyboard) { focused ->
        isFocused = focused
    }
}

/**
 * Helper para crear un modifier completo para elementos de menú con foco condicional.
 * Maneja automáticamente el highlight, el foco y los eventos de teclado.
 * Usa el mismo estilo de resaltado que los elementos de aplicación (fondo blanco semitransparente con esquinas redondeadas).
 * Retorna el modifier, el estado de foco y una función para obtener el color del texto apropiado.
 */
@Composable
fun Modifier.menuItemWithFocus(
    focusRequester: FocusRequester,
    index: Int = 0,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onKeyEvent: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean)? = null
): Triple<Modifier, Boolean, (Color) -> Color> {
    val context = LocalContext.current
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }
    var isFocused by remember { mutableStateOf(false) }
    val isFocusedAndHasKeyboard = isFocused && hasPhysicalKeyboard

    val theme = ThemeManager.rememberTheme(context)
    val focusColor = getFocusHighlightColor(theme)
    
    val modifier = this
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged?.invoke(focusState.isFocused)
        }
        .focusedAppItemStyle(isFocused, hasPhysicalKeyboard, context)
        .then(if (hasPhysicalKeyboard) Modifier.focusable() else Modifier)
        .then(if (hasPhysicalKeyboard && onKeyEvent != null) {
            Modifier.onKeyEvent { event -> onKeyEvent(event) }
        } else Modifier)
    
    val getTextColor: (Color) -> Color = { defaultColor ->
        // Calcular el color del texto sin usar @Composable
        if (isFocusedAndHasKeyboard && theme.focusTextColor != Color.Unspecified) {
            theme.focusTextColor
        } else if (isFocusedAndHasKeyboard && focusColor.isLightEnoughForBlackText()) {
            Color.Black
        } else {
            defaultColor
        }
    }
    
    return Triple(modifier, isFocusedAndHasKeyboard, getTextColor)
}

/**
 * Helper para crear un modifier completo para elementos de aplicación con foco condicional.
 * Retorna el modifier, el estado de foco y una función para obtener el color del texto apropiado.
 */
@Composable
fun Modifier.appItemWithFocus(
    focusRequester: FocusRequester,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onKeyEvent: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean)? = null
): Triple<Modifier, Boolean, (Color) -> Color> {
    val context = LocalContext.current
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }
    var isFocused by remember { mutableStateOf(false) }
    val isFocusedAndHasKeyboard = isFocused && hasPhysicalKeyboard

    val theme = ThemeManager.rememberTheme(context)
    val focusColor = getFocusHighlightColor(theme)
    
    val modifier = this
        .focusRequester(focusRequester)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged?.invoke(focusState.isFocused)
        }
        .focusedAppItemStyle(isFocused, hasPhysicalKeyboard, context)
        .conditionalFocusable(hasPhysicalKeyboard)
        .conditionalKeyEvents(hasPhysicalKeyboard) { event ->
            onKeyEvent?.invoke(event) ?: false
        }
    
    val getTextColor: (Color) -> Color = { defaultColor ->
        // Calcular el color del texto sin usar @Composable
        if (isFocusedAndHasKeyboard && theme.focusTextColor != Color.Unspecified) {
            theme.focusTextColor
        } else if (isFocusedAndHasKeyboard && focusColor.isLightEnoughForBlackText()) {
            Color.Black
        } else {
            defaultColor
        }
    }
    
    return Triple(modifier, isFocusedAndHasKeyboard, getTextColor)
}

