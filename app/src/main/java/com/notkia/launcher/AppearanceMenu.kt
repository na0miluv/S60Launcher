package com.notkia.launcher

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppearanceMenu(navController: NavController) {
    val context = LocalContext.current
    val wallpaperDrawable = remember { WallpaperManager.getWallpaperDrawableForUI(context) }
    val accentColor = remember(wallpaperDrawable) {
        getDarkAccentColorFromWallpaper(context, wallpaperDrawable)
    }
    
    val menuItems = remember {
        listOf(
            R.string.app_menu_style,
            R.string.scroll_indicator_style,
            R.string.app_theme,
            R.string.theme_settings
        )
    }
    var currentMenuStyle by remember { mutableStateOf(CustomAppInfoManager.getMenuStyle(context)) }
    var showScrollbar by remember { mutableStateOf(ScrollIndicatorManager.getShowScrollbar(context)) }
    var showArrows by remember { mutableStateOf(ScrollIndicatorManager.getShowArrows(context)) }
    // Usar rememberTheme para que se actualice dinámicamente
    val currentTheme = ThemeManager.rememberTheme(context)
    var currentThemeName by remember { mutableStateOf<String>(detectCurrentTheme(context)) }
    
    // Actualizar valores cuando el composable se monte o se vuelva a mostrar
    LaunchedEffect(Unit) {
        currentMenuStyle = CustomAppInfoManager.getMenuStyle(context)
        showScrollbar = ScrollIndicatorManager.getShowScrollbar(context)
        showArrows = ScrollIndicatorManager.getShowArrows(context)
        currentThemeName = detectCurrentTheme(context)
    }
    
    // Actualizar cuando cambie el tema o cuando se regrese de los submenús
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentTheme, navBackStackEntry) {
        // Actualizar cuando cambie el tema o cuando cambie la ruta (al regresar)
        currentMenuStyle = CustomAppInfoManager.getMenuStyle(context)
        showScrollbar = ScrollIndicatorManager.getShowScrollbar(context)
        showArrows = ScrollIndicatorManager.getShowArrows(context)
        currentThemeName = detectCurrentTheme(context)
    }

    val focusRequesters = remember(menuItems.size) { List(menuItems.size) { FocusRequester() } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }

    LaunchedEffect(menuItems) {
        if (menuItems.isNotEmpty() && hasPhysicalKeyboard) {
            listState.scrollToItemSmoothly(0, menuItems.size)
            focusRequesters[0].requestFocus()
        }
    }

    // Registrar el estado de scroll para que NavBar pueda acceder a él
    RegisterScrollState(listState = listState)
    
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        // Scrollbar del lado derecho
        ScrollBar(
            listState = listState,
            modifier = Modifier.fillMaxSize()
        )
        
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(menuItems) { index, itemRes ->
            val goBack = { navController.popBackStack() }

            val onClick = {
                when (index) {
                    0 -> {
                        // Es la opción de estilo de menú de apps
                        navController.navigate("menu_style_menu")
                    }
                    1 -> {
                        // Es la opción de estilo de indicador de scroll
                        navController.navigate("scroll_indicator_style_menu")
                    }
                    2 -> {
                        // Es la opción de tema de la app
                        navController.navigate("app_theme_menu")
                    }
                    else -> {
                        // Es la opción de configuración de temas
                        navController.navigate("theme_settings_menu")
                    }
                }
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
                                        totalItems = menuItems.size,
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
                                        totalItems = menuItems.size,
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
            
            when (index) {
                0 -> {
                    // Mostrar estilo de configuración para "App menu style"
                    val menuStyleText = when (currentMenuStyle) {
                        CustomAppInfoManager.MENU_STYLE_LIST -> stringResource(R.string.menu_style_list)
                        else -> stringResource(R.string.drawer_style_grid) // Cambiar "Default" a "Grid"
                    }
                    MenuConfigItem(
                        label = stringResource(itemRes),
                        currentValue = menuStyleText,
                        accentColor = accentColor,
                        textColor = getTextColor(contentColor),
                        modifier = menuModifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onClick() }
                            )
                    )
                }
                1 -> {
                    // Mostrar estilo de configuración para "Scroll indicator style"
                    val scrollIndicatorText = when {
                        showScrollbar && showArrows -> stringResource(R.string.scroll_indicator_both)
                        showScrollbar -> stringResource(R.string.scroll_indicator_scrollbar)
                        showArrows -> stringResource(R.string.scroll_indicator_arrows)
                        else -> stringResource(R.string.scroll_indicator_arrows) // Por defecto
                    }
                    MenuConfigItem(
                        label = stringResource(itemRes),
                        currentValue = scrollIndicatorText,
                        accentColor = accentColor,
                        textColor = getTextColor(contentColor),
                        modifier = menuModifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onClick() }
                            )
                    )
                }
                2 -> {
                    // Mostrar vista previa para "App theme"
                    val themeText = when (currentThemeName) {
                        "nokia" -> stringResource(R.string.theme_nokia)
                        "classic_white" -> stringResource(R.string.theme_classic_white)
                        else -> stringResource(R.string.theme_nokia)
                    }
                    // Ya tenemos currentTheme cargado arriba
                    MenuConfigItem(
                        label = stringResource(itemRes),
                        currentValue = themeText,
                        accentColor = accentColor,
                        textColor = getTextColor(contentColor),
                        modifier = menuModifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onClick() }
                            )
                    )
                }
                else -> {
                    Row(
                        modifier = menuModifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onClick() }
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollingText(
                            text = stringResource(itemRes),
                            color = getTextColor(contentColor),
                            isFocused = isFocused,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
    }
}

