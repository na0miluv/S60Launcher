package com.notkia.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EditMenu(navController: NavController, packageName: String?, onLaunchImagePicker: (String, (String?) -> Unit) -> Unit) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    
    // Estados temporales para los cambios (no se aplican hasta presionar "Accept")
    var tempName by remember(packageName) { 
        mutableStateOf(
            packageName?.let { CustomAppInfoManager.getCustomName(context, it) } ?: ""
        )
    }
    var tempIconUri by remember(packageName) { 
        mutableStateOf(
            packageName?.let { CustomAppInfoManager.getCustomIconUri(context, it) } ?: null
        )
    }
    
    // Estado para el diálogo de cambio de nombre
    var dialogName by remember { mutableStateOf("") }

    val menuItems = remember {
        listOf(
            R.string.name,
            R.string.icon,
            R.string.add_to,
            R.string.hide,
            R.string.reset_app,
            R.string.accept,
            R.string.cancel
        ).map { context.getString(it) }
    }

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
    
    // Obtener accentColor para MenuConfigItem
    val wallpaperDrawable = remember { WallpaperManager.getWallpaperDrawableForUI(context) }
    val accentColor = remember(wallpaperDrawable) {
        getDarkAccentColorFromWallpaper(context, wallpaperDrawable)
    }
    
    // Obtener nombre por defecto de la app
    val defaultAppName = remember(packageName) {
        packageName?.let {
            try {
                context.packageManager.getApplicationInfo(it, 0).loadLabel(context.packageManager).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        } ?: ""
    }
    
    // Obtener nombre del archivo del icono desde el URI (solo nombre y extensión, sin ruta)
    val iconFileName = remember(tempIconUri) {
        tempIconUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                // Obtener el último segmento del path (nombre del archivo con extensión)
                val lastSegment = uri.lastPathSegment
                // Si lastSegment contiene "/", tomar solo la parte después del último "/"
                lastSegment?.substringAfterLast("/") ?: lastSegment
            } catch (e: Exception) {
                null
            }
        }
    }
    val noChangesString = stringResource(R.string.no_changes)
    val displayIconFileName = iconFileName ?: noChangesString
    
    // Obtener nombre a mostrar (personalizado o por defecto)
    val displayName = remember(tempName, defaultAppName) {
        if (tempName.isNotEmpty()) {
            tempName
        } else {
            defaultAppName
        }
    }
    
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                dialogName = tempName // Restaurar el valor temporal
            },
            title = { Text(stringResource(R.string.change_app_name)) },
            text = { TextField(value = dialogName, onValueChange = { dialogName = it }) },
            confirmButton = {
                TextButton(onClick = {
                    // Guardar en estado temporal (no se aplica hasta "Accept")
                    tempName = dialogName
                    showDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    dialogName = tempName // Restaurar el valor temporal
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(menuItems) {
        if (menuItems.isNotEmpty() && hasPhysicalKeyboard) {
            focusRequesters[0].requestFocus()
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
                when (item) {
                    context.getString(R.string.name) -> {
                        dialogName = tempName // Inicializar con el valor temporal actual
                        showDialog = true
                    }
                    context.getString(R.string.icon) -> {
                        packageName?.let { packageName ->
                            onLaunchImagePicker(packageName) { uriString ->
                                // Guardar en estado temporal solo si se seleccionó una imagen (no null)
                                // Si se cancela (null), no cambiar el estado temporal
                                if (uriString != null) {
                                    tempIconUri = uriString
                                }
                            }
                        }
                    }
                    context.getString(R.string.add_to) -> {
                        packageName?.let { navController.navigate("add_to_menu/$it") }
                    }
                    context.getString(R.string.hide) -> {
                        if (packageName != null) {
                            CustomAppInfoManager.setAppHidden(context, packageName, true)
                            PinnedAppsManager.removeApp(context, packageName)
                        }
                        goBack()
                    }
                    context.getString(R.string.reset_app) -> {
                        if (packageName != null) {
                            CustomAppInfoManager.resetApp(context, packageName)
                        }
                        goBack()
                    }
                    context.getString(R.string.accept) -> {
                        // Aplicar todos los cambios temporales
                        if (packageName != null) {
                            // Aplicar nombre
                            if (tempName.isNotEmpty() && tempName != CustomAppInfoManager.getCustomName(context, packageName)) {
                                CustomAppInfoManager.setCustomName(context, packageName, tempName)
                            } else if (tempName.isEmpty() && CustomAppInfoManager.getCustomName(context, packageName) != null) {
                                // Si está vacío y había un nombre personalizado, eliminarlo
                                CustomAppInfoManager.setCustomName(context, packageName, "")
                            }
                            // Aplicar icono
                            val currentIconUri = CustomAppInfoManager.getCustomIconUri(context, packageName)
                            val tempIconUriValue = tempIconUri // Guardar en variable local para evitar smart cast issues
                            if (tempIconUriValue != currentIconUri) {
                                if (tempIconUriValue != null && tempIconUriValue.isNotEmpty()) {
                                    CustomAppInfoManager.setCustomIconUri(context, packageName, tempIconUriValue)
                                } else if (currentIconUri != null) {
                                    // Si se eliminó el icono temporal pero había uno, eliminarlo
                                    // Usar el mismo método que resetApp para eliminar
                                    context.getSharedPreferences("CustomAppIcons", Context.MODE_PRIVATE)
                                        .edit()
                                        .remove(packageName)
                                        .apply()
                                    // Notificar el cambio
                                    context.sendBroadcast(Intent(CustomAppInfoManager.ACTION_APPS_UPDATED))
                                }
                            }
                        }
                        goBack()
                    }
                    context.getString(R.string.cancel) -> {
                        // Descartar cambios: no hacer nada, solo volver
                        goBack()
                    }
                    else -> goBack()
                }
            }

            val (menuModifier, isFocused, getTextColor) = Modifier
                .menuItemWithFocus(
                    focusRequester = focusRequesters[index],
                    index = index,
                    onKeyEvent = { event ->
                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
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
                                    // Descartar cambios al presionar Back
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
                    // "Nombre" - usar MenuConfigItem
                    MenuConfigItem(
                        label = stringResource(R.string.name),
                        currentValue = displayName,
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
                    // "Icono" - usar MenuConfigItem
                    MenuConfigItem(
                        label = stringResource(R.string.icon),
                        currentValue = displayIconFileName,
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
                    // Otros elementos del menú - renderizado normal
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
                        if (item == context.getString(R.string.add_to)) {
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
    }
}
