package com.notkia.launcher

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed 
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    navController: NavController,
    apps: List<AppInfo>,
    focusedIndex: Int,
    setFocusedIndex: (Int) -> Unit,
    gridState: LazyGridState
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }

    // Leer el estilo directamente para que se actualice cuando cambie
    val drawerStyle = CustomAppInfoManager.getDrawerStyle(context)
    val isListMode = drawerStyle == CustomAppInfoManager.DRAWER_STYLE_LIST
    
    val columns = 3
    val focusRequesters = remember(apps.size) { List(apps.size) { FocusRequester() } }
    val coroutineScope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    
    // Cargar tema y usar colores (se actualiza dinámicamente)
    val theme = ThemeManager.rememberTheme(context)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface
    val appDrawerBackground = if (theme.appDrawerBackground != Color.Unspecified) {
        theme.appDrawerBackground // Usar el color del tema directamente (incluye Color.White)
    } else {
        Color.Transparent
    }
    val contentColor = if (theme.appDrawerTextColor != Color.Unspecified) {
        theme.appDrawerTextColor
    } else {
        defaultContentColor
    }
    var hasInitialized by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex, apps.size, hasPhysicalKeyboard, isListMode) {
        if (apps.isEmpty() || !hasPhysicalKeyboard) return@LaunchedEffect
        
        val targetIndex = if (focusedIndex >= 0 && focusedIndex < apps.size) {
            focusedIndex
        } else {
            0
        }
        
        if (isListMode) {
            // Modo lista: usar listState
            val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            
            if (!hasInitialized) {
                scrollJob?.cancel()
                scrollJob = coroutineScope.launch {
                    if (!isVisible) {
                        listState.scrollToItemSmoothly(targetIndex, apps.size)
                    }
                    focusRequesters[targetIndex].requestFocus()
                    hasInitialized = true
                }
            } else if (!isVisible) {
                scrollJob?.cancel()
                scrollJob = coroutineScope.launch {
                    listState.scrollToItemSmoothly(targetIndex, apps.size)
                    focusRequesters[targetIndex].requestFocus()
                }
            } else {
                focusRequesters[targetIndex].requestFocus()
            }
        } else {
            // Modo grid: usar gridState
            val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            
            if (!hasInitialized) {
                scrollJob?.cancel()
                scrollJob = coroutineScope.launch {
                    if (!isVisible) {
                        gridState.scrollToItem(targetIndex)
                    }
                    focusRequesters[targetIndex].requestFocus()
                    hasInitialized = true
                }
            } else if (!isVisible) {
                scrollJob?.cancel()
                scrollJob = coroutineScope.launch {
                    gridState.scrollToItem(targetIndex)
                    focusRequesters[targetIndex].requestFocus()
                }
            } else {
                focusRequesters[targetIndex].requestFocus()
            }
        }
    }

    // Registrar el estado de scroll para que NavBar pueda acceder a él
    RegisterScrollState(listState = if (isListMode) listState else null, gridState = if (!isListMode) gridState else null)
    
    Box(modifier = Modifier.fillMaxSize().background(appDrawerBackground)) {
        // Scrollbar del lado derecho
        ScrollBar(
            listState = if (isListMode) listState else null,
            gridState = if (!isListMode) gridState else null,
            modifier = Modifier.fillMaxSize()
        )
        
    if (isListMode) {
        // Modo lista
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            itemsIndexed(items = apps, key = { index, app -> "${app.packageName}_$index" }git diff
            ) { index, app ->
                AppDrawerItem(
                    index = index,
                    app = app,
                    packageManager = packageManager,
                    context = context,
                    navController = navController,
                    focusRequesters = focusRequesters,
                    setFocusedIndex = setFocusedIndex,
                    coroutineScope = coroutineScope,
                    listState = listState,
                    gridState = null,
                    apps = apps,
                    columns = 1,
                    isListMode = true,
                    contentColor = contentColor
                )
            }
        }
    } else {
        // Modo grid
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            gridItemsIndexed(items = apps, key = { index, app -> "${app.packageName}_$index" }) { index, app ->
                AppDrawerItem(
                    index = index,
                    app = app,
                    packageManager = packageManager,
                    context = context,
                    navController = navController,
                    focusRequesters = focusRequesters,
                    setFocusedIndex = setFocusedIndex,
                    coroutineScope = coroutineScope,
                    listState = null,
                    gridState = gridState,
                    apps = apps,
                    columns = columns,
                    isListMode = false,
                    contentColor = contentColor
                )
            }
        }
    }
    }
}

@Composable
private fun AppDrawerItem(
    index: Int,
    app: AppInfo,
    packageManager: android.content.pm.PackageManager,
    context: android.content.Context,
    navController: androidx.navigation.NavController,
    focusRequesters: List<FocusRequester>,
    setFocusedIndex: (Int) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    listState: LazyListState?,
    gridState: LazyGridState?,
    apps: List<AppInfo>,
    columns: Int,
    isListMode: Boolean,
    contentColor: Color
) {
    val openApp = {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        context.startActivity(launchIntent)
        navController.navigate("home") {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
        }
    }

    val openAppOptionsMenu = {
        navController.navigate("options_menu/${app.packageName}")
    }

    val (modifier, _, getTextColor) = if (isListMode) {
        // Modo lista: menos padding para highlight más compacto
        Modifier
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .appItemWithFocus(
                focusRequester = focusRequesters[index],
                onFocusChanged = { focused ->
                    if (focused) {
                        setFocusedIndex(index)
                    }
                },
                onKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                            openApp()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (isListMode) {
                                // Modo lista: navegación con wrap around
                                if (listState != null) {
                                    val useAnimation = event.nativeKeyEvent.repeatCount == 0
                                    handleListNavigationWithWrapAround(
                                        currentIndex = index,
                                        direction = 1,
                                        totalItems = apps.size,
                                        listState = listState,
                                        focusRequesters = focusRequesters,
                                        scope = coroutineScope,
                                        useAnimation = useAnimation
                                    )
                                }
                            } else {
                                // Modo grid: navegación en grid
                                var targetIndex = index + columns
                                if (targetIndex >= apps.size) {
                                    targetIndex = index % columns
                                    coroutineScope.launch {
                                        gridState?.scrollToItem(0)
                                        focusRequesters[targetIndex].requestFocus()
                                    }
                                } else {
                                    val isTargetVisible = gridState?.layoutInfo?.visibleItemsInfo?.any { it.index == targetIndex } ?: false
                                    if (isTargetVisible) {
                                        focusRequesters[targetIndex].requestFocus()
                                    } else {
                                        coroutineScope.launch {
                                            val itemHeight = gridState?.layoutInfo?.visibleItemsInfo?.firstOrNull()?.size?.height?.toFloat() ?: 0f
                                            if (itemHeight > 0) {
                                                if (event.nativeKeyEvent.repeatCount > 0) {
                                                    gridState?.scrollBy(itemHeight)
                                                } else {
                                                    gridState?.animateScrollBy(itemHeight, animationSpec = tween(100))
                                                }
                                            }
                                            focusRequesters[targetIndex].requestFocus()
                                        }
                                    }
                                }
                            }
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                            if (isListMode) {
                                // Modo lista: navegación con wrap around
                                if (listState != null) {
                                    val useAnimation = event.nativeKeyEvent.repeatCount == 0
                                    handleListNavigationWithWrapAround(
                                        currentIndex = index,
                                        direction = -1,
                                        totalItems = apps.size,
                                        listState = listState,
                                        focusRequesters = focusRequesters,
                                        scope = coroutineScope,
                                        useAnimation = useAnimation
                                    )
                                }
                            } else {
                                // Modo grid: navegación en grid
                                var targetIndex = index - columns
                                if (targetIndex < 0) {
                                    val col = index % columns
                                    targetIndex = apps.lastIndex - ((apps.lastIndex % columns - col + columns) % columns)
                                    coroutineScope.launch {
                                        gridState?.scrollToItem(apps.lastIndex)
                                        focusRequesters[targetIndex].requestFocus()
                                    }
                                } else {
                                    val isTargetVisible = gridState?.layoutInfo?.visibleItemsInfo?.any { it.index == targetIndex } ?: false
                                    if (isTargetVisible) {
                                        focusRequesters[targetIndex].requestFocus()
                                    } else {
                                        coroutineScope.launch {
                                            val itemHeight = gridState?.layoutInfo?.visibleItemsInfo?.firstOrNull()?.size?.height?.toFloat() ?: 0f
                                            if (itemHeight > 0) {
                                                if (event.nativeKeyEvent.repeatCount > 0) {
                                                    gridState?.scrollBy(-itemHeight)
                                                } else {
                                                    gridState?.animateScrollBy(-itemHeight, animationSpec = tween(100))
                                                }
                                            }
                                            focusRequesters[targetIndex].requestFocus()
                                        }
                                    }
                                }
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
        )
    } else {
        // Modo grid: padding normal
        Modifier
            .padding(2.dp)
            .padding(vertical = 4.dp)
            .appItemWithFocus(
                focusRequester = focusRequesters[index],
                onFocusChanged = { focused ->
                    if (focused) {
                        setFocusedIndex(index)
                    }
                },
                onKeyEvent = { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.nativeKeyEvent.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                openApp()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                // Modo grid: navegación en grid
                                var targetIndex = index + columns
                                if (targetIndex >= apps.size) {
                                    targetIndex = index % columns
                                    coroutineScope.launch {
                                        gridState?.scrollToItem(0)
                                        focusRequesters[targetIndex].requestFocus()
                                    }
                                } else {
                                    val isTargetVisible = gridState?.layoutInfo?.visibleItemsInfo?.any { it.index == targetIndex } ?: false
                                    if (isTargetVisible) {
                                        focusRequesters[targetIndex].requestFocus()
                                    } else {
                                        coroutineScope.launch {
                                            val itemHeight = gridState?.layoutInfo?.visibleItemsInfo?.firstOrNull()?.size?.height?.toFloat() ?: 0f
                                            if (itemHeight > 0) {
                                                if (event.nativeKeyEvent.repeatCount > 0) {
                                                    gridState?.scrollBy(itemHeight)
                                                } else {
                                                    gridState?.animateScrollBy(itemHeight, animationSpec = tween(100))
                                                }
                                            }
                                            focusRequesters[targetIndex].requestFocus()
                                        }
                                    }
                                }
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                // Modo grid: navegación en grid
                                var targetIndex = index - columns
                                if (targetIndex < 0) {
                                    val col = index % columns
                                    targetIndex = apps.lastIndex - ((apps.lastIndex % columns - col + columns) % columns)
                                    coroutineScope.launch {
                                        gridState?.scrollToItem(apps.lastIndex)
                                        focusRequesters[targetIndex].requestFocus()
                                    }
                                } else {
                                    val isTargetVisible = gridState?.layoutInfo?.visibleItemsInfo?.any { it.index == targetIndex } ?: false
                                    if (isTargetVisible) {
                                        focusRequesters[targetIndex].requestFocus()
                                    } else {
                                        coroutineScope.launch {
                                            val itemHeight = gridState?.layoutInfo?.visibleItemsInfo?.firstOrNull()?.size?.height?.toFloat() ?: 0f
                                            if (itemHeight > 0) {
                                                if (event.nativeKeyEvent.repeatCount > 0) {
                                                    gridState?.scrollBy(-itemHeight)
                                                } else {
                                                    gridState?.animateScrollBy(-itemHeight, animationSpec = tween(100))
                                                }
                                            }
                                            focusRequesters[targetIndex].requestFocus()
                                        }
                                    }
                                }
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
            )
    }

    if (isListMode) {
        // Modo lista: layout horizontal con icono a la izquierda
        Row(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { openApp() },
                    onLongClick = { openAppOptionsMenu() }
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap(width = 100, height = 100).asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 12.dp))
            Text(
                text = app.appName,
                color = getTextColor(contentColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        // Modo grid: layout vertical centrado
        Column(
            modifier = modifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { openApp() },
                    onLongClick = { openAppOptionsMenu() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                bitmap = app.icon.toBitmap(width = 100, height = 100).asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(50.dp)
            )
            Text(
                text = app.appName,
                color = getTextColor(contentColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
