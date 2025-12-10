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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ClockStyleMenu(navController: NavController) {
    val context = LocalContext.current
    val menuItems = remember {
        listOf("Digital", "Analogue 1")
    }
    val clockStyles = remember {
        listOf(CustomAppInfoManager.CLOCK_DIGITAL, CustomAppInfoManager.CLOCK_STYLE_1)
    }
    val currentClockStyle = remember { CustomAppInfoManager.getClockStyle(context) }

    val focusRequesters = remember(menuItems.size) { List(menuItems.size) { FocusRequester() } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Cargar tema y usar colores (se actualiza dinámicamente)
    val theme = ThemeManager.rememberTheme(context)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface
    val menuBackground = if (theme.menuBackground != Color.Unspecified) {
        theme.menuBackground
    } else {
        Color.Transparent
    }
    val contentColor = if (theme.menuTextColor != Color.Unspecified) {
        theme.menuTextColor
    } else {
        defaultContentColor
    }
    
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }

    LaunchedEffect(menuItems) {
        if (menuItems.isNotEmpty() && hasPhysicalKeyboard) {
            val initialFocusedIndex = clockStyles.indexOf(currentClockStyle).coerceAtLeast(0)
            listState.scrollToItemSmoothly(initialFocusedIndex, menuItems.size)
            focusRequesters[initialFocusedIndex].requestFocus()
        }
    }

    // Registrar el estado de scroll para que NavBar pueda acceder a él
    RegisterScrollState(listState = listState)
    
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(menuBackground)) {
        // Scrollbar del lado derecho
        ScrollBar(
            listState = listState,
            modifier = Modifier.fillMaxSize()
        )
        
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(menuItems) { index, item ->
            val goBack = { navController.popBackStack() }

            val onClick = {
                CustomAppInfoManager.setClockStyle(context, clockStyles[index])
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
                    text = item,
                    color = getTextColor(contentColor),
                    isFocused = isFocused,
                    modifier = Modifier.weight(1f)
                )
                if (currentClockStyle == clockStyles[index]) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = getTextColor(contentColor)
                    )
                }
            }
        }
    }
    }
}
