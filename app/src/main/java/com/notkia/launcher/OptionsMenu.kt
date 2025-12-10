package com.notkia.launcher

import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
fun OptionsMenu(
    navController: NavController,
    packageName: String?,
    focusedIndex: Int,
    setFocusedIndex: (Int) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    // Cargar tema y usar colores (se actualiza dinámicamente)
    val theme = ThemeManager.rememberTheme(context)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface
    val menuBackground = if (theme.menuBackground != Color.Unspecified) {
        theme.menuBackground // Usar el color del tema directamente (incluye Color.White)
    } else {
        Color.Transparent
    }
    val contentColor = if (theme.menuTextColor != Color.Unspecified) {
        theme.menuTextColor
    } else {
        defaultContentColor
    }

    var showHiddenApps by remember { mutableStateOf(CustomAppInfoManager.getShowHiddenApps(context)) }

    val hasShortcuts = remember(packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && packageName != null) {
            try {
                val launcherApps = context.getSystemService(LauncherApps::class.java)
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
                }
                (launcherApps?.getShortcuts(query, Process.myUserHandle())?.size ?: 0) > 0
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    val appName = remember(packageName) {
        if (packageName != null) {
            try {
                packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    val menuItems = remember(packageName, appName, hasShortcuts, showHiddenApps) {
        if (packageName != null && appName != null) {
            val items = mutableListOf(
                context.getString(R.string.open_app, appName),
                context.getString(R.string.info),
                context.getString(R.string.add_to),
                context.getString(R.string.edit),
                context.getString(R.string.cancel)
            )
            if (hasShortcuts) {
                items.add(1, context.getString(R.string.actions))
            }
            items
        } else {
            val viewHidden = if (showHiddenApps) R.string.hide_hidden_applications else R.string.view_hidden_applications
            listOf(
                context.getString(R.string.settings),
                context.getString(R.string.sort_apps),
                context.getString(viewHidden),
                context.getString(R.string.clock_style),
                context.getString(R.string.appearance),
                context.getString(R.string.cancel)
            )
        }
    }

    val focusRequesters = remember(menuItems.size) { List(menuItems.size) { FocusRequester() } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }

    LaunchedEffect(Unit) {
        if (menuItems.isNotEmpty() && hasPhysicalKeyboard) {
            val safeIndex = if (focusedIndex >= menuItems.size) 0 else focusedIndex
            setFocusedIndex(safeIndex)
            focusRequesters[safeIndex].requestFocus()
        }
    }

    // Registrar el estado de scroll para que NavBar pueda acceder a él
    RegisterScrollState(listState = listState)
    
    Box(modifier = Modifier.fillMaxSize().background(menuBackground)) {
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
            val openApp = {
                if (packageName != null) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    context.startActivity(launchIntent)
                }
            }

            val openActionsMenu = {
                if (packageName != null) {
                    navController.navigate("actions_menu/$packageName")
                }
            }

            val openAddToMenu = {
                if (packageName != null) {
                    navController.navigate("add_to_menu/$packageName")
                }
            }

            val openEditMenu = {
                if (packageName != null) {
                    navController.navigate("edit_menu/$packageName")
                }
            }

            val openAppInfo = {
                if (packageName != null) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    context.startActivity(intent)
                }
            }

            val goBack = {
                navController.popBackStack()
            }

            val toggleHiddenApps = {
                showHiddenApps = !showHiddenApps
                CustomAppInfoManager.setShowHiddenApps(context, showHiddenApps)
            }

            val openSettings = {
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            }

            val openSortMenu = {
                navController.navigate("sort_menu")
            }

            val openClockStyleMenu = {
                navController.navigate("clock_style_menu")
            }

            val openAppearanceMenu = {
                navController.navigate("appearance_menu")
            }

            val onClick: () -> Unit = {
                when (item) {
                    context.getString(R.string.open_app, appName) -> openApp()
                    context.getString(R.string.actions) -> openActionsMenu()
                    context.getString(R.string.info) -> openAppInfo()
                    context.getString(R.string.add_to) -> openAddToMenu()
                    context.getString(R.string.edit) -> openEditMenu()
                    context.getString(if (showHiddenApps) R.string.hide_hidden_applications else R.string.view_hidden_applications) -> toggleHiddenApps()
                    context.getString(R.string.cancel) -> goBack()
                    context.getString(R.string.settings) -> openSettings()
                    context.getString(R.string.sort_apps) -> openSortMenu()
                    context.getString(R.string.clock_style) -> openClockStyleMenu()
                    context.getString(R.string.appearance) -> openAppearanceMenu()
                    else -> { /* No hacer nada para otros casos */ }
                }
            }

            val (menuModifier, isFocused, getTextColor) = Modifier
                .menuItemWithFocus(
                    focusRequester = focusRequesters[index],
                    index = index,
                    onFocusChanged = { focused ->
                        if (focused) {
                            setFocusedIndex(index)
                        }
                    },
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
                        onClick = onClick
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
                if (item == context.getString(R.string.actions) || item == context.getString(R.string.add_to) || item == context.getString(R.string.edit) || item == context.getString(R.string.sort_apps) || item == context.getString(R.string.clock_style) || item == context.getString(R.string.appearance)) {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_right),
                        contentDescription = "$item submenu",
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(getTextColor(contentColor)),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    }
}
