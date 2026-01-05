package com.notkia.launcher

import android.content.Context
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.rememberCoroutineScope
import com.notkia.launcher.ui.theme.ClassicWhiteTheme
import com.notkia.launcher.ui.theme.NokiaTheme

/**
 * Detecta qué tema está actualmente activo
 */
fun detectCurrentTheme(context: Context): String {
    val currentTheme = ThemeManager.loadTheme(context)
    val nokiaTheme = NokiaTheme.create()
    val classicWhiteTheme = ClassicWhiteTheme.default()
    
    // Comparar con Nokia (valores por defecto)
    if (themesMatch(currentTheme, nokiaTheme)) {
        return "nokia"
    }
    
    // Comparar con Classic White
    if (themesMatch(currentTheme, classicWhiteTheme)) {
        return "classic_white"
    }
    
    // Si no coincide con ninguno, asumir Nokia (por defecto)
    return "nokia"
}

/**
 * Compara dos temas para ver si son iguales
 */
private fun themesMatch(theme1: SymbianTheme, theme2: SymbianTheme): Boolean {
    return theme1.topSegmentBackground == theme2.topSegmentBackground &&
            theme1.bottomSegmentBackground == theme2.bottomSegmentBackground &&
            theme1.topSegmentTextColor == theme2.topSegmentTextColor &&
            theme1.bottomSegmentTextColor == theme2.bottomSegmentTextColor &&
            theme1.navBarBackground == theme2.navBarBackground &&
            theme1.navBarTextColor == theme2.navBarTextColor &&
            theme1.appDrawerBackground == theme2.appDrawerBackground &&
            theme1.appDrawerTextColor == theme2.appDrawerTextColor &&
            theme1.menuBackground == theme2.menuBackground &&
            theme1.menuTextColor == theme2.menuTextColor &&
            theme1.focusHasFill == theme2.focusHasFill &&
            theme1.focusBorderColor == theme2.focusBorderColor &&
            theme1.useGradientOpacityForIndicators == theme2.useGradientOpacityForIndicators
}

@Composable
fun AppThemeMenu(navController: NavController) {
    val context = LocalContext.current
    var currentThemeName by remember { mutableStateOf<String>(detectCurrentTheme(context)) }
    
    val themes = remember {
        listOf(
            ThemeInfo("nokia", R.string.theme_nokia, NokiaTheme.create()),
            ThemeInfo("classic_white", R.string.theme_classic_white, ClassicWhiteTheme.default())
        )
    }
    
    // Cargar tema y usar colores (se actualiza dinámicamente)
    val theme = ThemeManager.rememberTheme(context)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface
    val menuBackground = if (theme.subMenuBackground != Color.Unspecified) {
        theme.subMenuBackground
    } else {
        Color.Transparent
    }
    val contentColor = if (theme.subMenuTextColor != Color.Unspecified) {
        theme.subMenuTextColor
    } else {
        defaultContentColor
    }

    val focusRequesters = remember(themes.size) { List(themes.size) { FocusRequester() } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }
    
    // Encontrar el índice del tema actual
    val currentIndex = themes.indexOfFirst { it.id == currentThemeName }.coerceAtLeast(0)

    LaunchedEffect(themes) {
        if (themes.isNotEmpty() && hasPhysicalKeyboard) {
            listState.scrollToItemSmoothly(currentIndex, themes.size)
            focusRequesters[currentIndex].requestFocus()
        }
    }

    // Registrar el estado de scroll
    RegisterScrollState(listState = listState)
    
    Box(modifier = Modifier.fillMaxSize().background(menuBackground)) {
        ScrollBar(
            listState = listState,
            modifier = Modifier.fillMaxSize()
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(themes) { index, themeInfo ->
                val goBack = { navController.popBackStack() }
                val isSelected = themeInfo.id == currentThemeName

                val onClick = {
                    if (themeInfo.id == "nokia") {
                        // Para el tema Nokia, limpiar todas las preferencias para usar valores por defecto
                        ThemeManager.resetTheme(context)
                    } else {
                        // Para otros temas, guardar el tema normalmente con su nombre
                        ThemeManager.saveTheme(context, themeInfo.theme, themeInfo.id)
                    }
                    currentThemeName = themeInfo.id
                    goBack()
                }

                val (menuModifier, isFocused, getTextColor) = Modifier
                    .menuItemWithFocus(
                        focusRequester = focusRequesters[index],
                        index = index,
                        onKeyEvent = { event ->
                            if (event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN) {
                                when (event.nativeKeyEvent.keyCode) {
                                    AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                        onClick()
                                        true
                                    }
                                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                        handleListNavigationWithWrapAround(
                                            currentIndex = index,
                                            direction = 1,
                                            totalItems = themes.size,
                                            listState = listState,
                                            focusRequesters = focusRequesters,
                                            scope = coroutineScope
                                        )
                                        true
                                    }
                                    AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                        handleListNavigationWithWrapAround(
                                            currentIndex = index,
                                            direction = -1,
                                            totalItems = themes.size,
                                            listState = listState,
                                            focusRequesters = focusRequesters,
                                            scope = coroutineScope
                                        )
                                        true
                                    }
                                    AndroidKeyEvent.KEYCODE_BACK -> {
                                        goBack()
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                    )
                getTextColor
                // Elemento de lista con vista previa
                ThemePreviewItem(
                    themeName = stringResource(themeInfo.nameRes),
                    theme = themeInfo.theme,
                    isSelected = isSelected,
                    textColor = getTextColor(contentColor),
                    modifier = menuModifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onClick() }
                        )
                )
            }
        }
    }
}

/**
 * Información de un tema
 */
private data class ThemeInfo(
    val id: String,
    val nameRes: Int,
    val theme: SymbianTheme
)

/**
 * Elemento de lista con vista previa del tema
 */
@Composable
fun ThemePreviewItem(
    themeName: String,
    theme: SymbianTheme,
    isSelected: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Nombre del tema
        Text(
            text = themeName,
            color = textColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Vista previa del tema
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
                    .background(
                        color = if (theme.accentColor != Color.Unspecified) {
                            theme.accentColor
                        } else {
                            Color(0xFF4A4A4A) // Color por defecto
                        },
                        shape = RoundedCornerShape(theme.listItemCornerRadius)
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 2.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(theme.listItemCornerRadius)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(8.dp)
            ) {
                Column {
                    // Vista previa de StatusBar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Segmento superior
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .background(
                                    if (theme.topSegmentBackground != Color.Transparent) {
                                        theme.topSegmentBackground
                                    } else {
                                        Color.White
                                    }
                                )
                        )
                        // Segmento inferior
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .background(
                                    if (theme.bottomSegmentBackground != Color.Transparent) {
                                        theme.bottomSegmentBackground
                                    } else {
                                        if (theme.accentColor != Color.Unspecified) {
                                            theme.accentColor
                                        } else {
                                            Color(0xFF003366)
                                        }
                                    }
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Vista previa de NavBar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                if (theme.navBarBackground != Color.Unspecified) {
                                    theme.navBarBackground
                                } else {
                                    Color.White.copy(alpha = 0.8f)
                                }
                            )
                    )
                }
            }
        }
    }
}

