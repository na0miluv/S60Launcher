package com.notkia.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState

/**
 * Estado compartido para el estado de scroll actual
 * Esto permite que NavBar acceda al estado de scroll sin necesidad de pasarlo explícitamente
 */
object ScrollStateHolder {
    var currentListState: LazyListState? = null
    var currentGridState: LazyGridState? = null
}

/**
 * Gestor para guardar y cargar preferencias de indicadores de scroll
 */
object ScrollIndicatorManager {
    private const val PREFS_NAME = "ScrollIndicatorPreferences"
    private const val KEY_SHOW_SCROLLBAR = "show_scrollbar"
    private const val KEY_SHOW_ARROWS = "show_arrows"
    
    // Valores por defecto: flechas activadas, scrollbar desactivada
    private const val DEFAULT_SHOW_SCROLLBAR = false
    private const val DEFAULT_SHOW_ARROWS = true
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getShowScrollbar(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_SCROLLBAR, DEFAULT_SHOW_SCROLLBAR)
    }
    
    fun setShowScrollbar(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_SCROLLBAR, value).apply()
    }
    
    fun getShowArrows(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_ARROWS, DEFAULT_SHOW_ARROWS)
    }
    
    fun setShowArrows(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_ARROWS, value).apply()
    }
}

/**
 * Detecta si se puede hacer scroll hacia arriba
 */
fun canScrollUp(listState: LazyListState): Boolean {
    return listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
}

/**
 * Detecta si se puede hacer scroll hacia abajo
 */
fun canScrollDown(listState: LazyListState): Boolean {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
    
    return if (lastVisibleItem != null) {
        lastVisibleItem.index < totalItems - 1 || 
        (lastVisibleItem.index == totalItems - 1 && 
         lastVisibleItem.offset + lastVisibleItem.size < layoutInfo.viewportEndOffset)
    } else {
        false
    }
}

/**
 * Detecta si se puede hacer scroll hacia arriba en un LazyGrid
 */
fun canScrollUp(gridState: LazyGridState): Boolean {
    val layoutInfo = gridState.layoutInfo
    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
    
    return if (firstVisibleItem != null) {
        firstVisibleItem.index > 0 || firstVisibleItem.offset.y < 0
    } else {
        false
    }
}

/**
 * Detecta si se puede hacer scroll hacia abajo en un LazyGrid
 */
fun canScrollDown(gridState: LazyGridState): Boolean {
    val layoutInfo = gridState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
    
    return if (lastVisibleItem != null) {
        lastVisibleItem.index < totalItems - 1 || 
        (lastVisibleItem.index == totalItems - 1 && 
         lastVisibleItem.offset.y + lastVisibleItem.size.height < layoutInfo.viewportSize.height)
    } else {
        false
    }
}

/**
 * Calcula la posición y tamaño de la scrollbar basado en el estado de scroll
 */
private fun calculateScrollbarMetrics(
    listState: LazyListState?,
    gridState: LazyGridState?,
    viewportHeight: Float
): Pair<Float, Float>? {
    when {
        listState != null -> {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return null
            
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return null
            
            val firstVisible = visibleItems.first()
            val lastVisible = visibleItems.last()
            
            // Calcular el rango visible actual
            val visibleStart = firstVisible.offset.toFloat()
            
            // Calcular el tamaño promedio de los elementos visibles
            val averageItemHeight = visibleItems.map { it.size }.average().toFloat()
            
            // Calcular el contenido total: tamaño promedio * número total de elementos
            val totalContentHeight = averageItemHeight * totalItems
            
            if (totalContentHeight <= viewportHeight) return null // No hay scroll
            
            // Calcular la posición de scroll actual
            // El offset del primer elemento visible más su índice * altura promedio
            val currentScrollPosition = firstVisible.index * averageItemHeight + visibleStart
            
            // Calcular posición y altura de la scrollbar
            val maxScroll = totalContentHeight - viewportHeight
            val scrollRatio = (currentScrollPosition / maxScroll.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val thumbHeight = (viewportHeight / totalContentHeight) * viewportHeight
            
            val thumbTop = scrollRatio * (viewportHeight - thumbHeight)
            
            return Pair(thumbTop, thumbHeight)
        }
        gridState != null -> {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return null
            
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return null
            
            val firstVisible = visibleItems.first()
            
            // Calcular el rango visible actual
            val visibleStart = firstVisible.offset.y.toFloat()
            
            // Calcular el tamaño promedio de los elementos visibles (altura)
            val averageItemHeight = visibleItems.map { it.size.height }.average().toFloat()
            
            // Calcular el número de columnas contando elementos en la misma fila (mismo offset.y)
            val itemsByRow = visibleItems.groupBy { it.offset.y }
            val columns = itemsByRow.values.maxOfOrNull { it.size } ?: 1
            
            // Calcular el número total de filas
            val rows = (totalItems + columns - 1) / columns // Redondear hacia arriba
            
            // Calcular el contenido total: altura promedio * número de filas
            val totalContentHeight = averageItemHeight * rows
            
            if (totalContentHeight <= viewportHeight) return null // No hay scroll
            
            // Calcular la posición de scroll actual
            val currentRow = firstVisible.index / columns
            val currentScrollPosition = currentRow * averageItemHeight + visibleStart
            
            // Calcular posición y altura de la scrollbar
            val maxScroll = totalContentHeight - viewportHeight
            val scrollRatio = (currentScrollPosition / maxScroll.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val thumbHeight = (viewportHeight / totalContentHeight) * viewportHeight
            
            val thumbTop = scrollRatio * (viewportHeight - thumbHeight)
            
            return Pair(thumbTop, thumbHeight)
        }
        else -> return null
    }
}

/**
 * Hook para registrar el estado de scroll actual
 * Debe ser llamado en cada pantalla que tenga scroll
 */
@Composable
fun RegisterScrollState(listState: LazyListState? = null, gridState: LazyGridState? = null) {
    LaunchedEffect(listState, gridState) {
        ScrollStateHolder.currentListState = listState
        ScrollStateHolder.currentGridState = gridState
    }
    
    // Limpiar cuando se desmonte
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            ScrollStateHolder.currentListState = null
            ScrollStateHolder.currentGridState = null
        }
    }
}

/**
 * Scrollbar vertical del lado derecho
 */
@Composable
fun ScrollBar(
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    modifier: Modifier = Modifier,
    color: Color? = null, // Si es null, usa el color del tema
    width: androidx.compose.ui.unit.Dp = 4.dp
) {
    val context = LocalContext.current
    val showScrollbar = remember { ScrollIndicatorManager.getShowScrollbar(context) }
    
    if (!showScrollbar) return
    
    // Cargar color del tema si no se especifica
    val theme = ThemeManager.rememberTheme(context)
    val scrollbarColor = color ?: if (theme.scrollbarColor != Color.Unspecified) {
        theme.scrollbarColor
    } else {
        Color.White.copy(alpha = 0.6f)
    }
    
    // Usar el estado pasado como parámetro o el estado compartido
    val effectiveListState = listState ?: ScrollStateHolder.currentListState
    val effectiveGridState = gridState ?: ScrollStateHolder.currentGridState
    
    var scrollbarMetrics by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    
    LaunchedEffect(effectiveListState, effectiveGridState) {
        // Actualizar métricas cuando cambia el scroll
        kotlinx.coroutines.delay(16) // ~60fps
        val viewportHeight = when {
            effectiveListState != null -> {
                val layoutInfo = effectiveListState.layoutInfo
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
            }
            effectiveGridState != null -> {
                val layoutInfo = effectiveGridState.layoutInfo
                layoutInfo.viewportSize.height.toFloat()
            }
            else -> return@LaunchedEffect
        }
        scrollbarMetrics = calculateScrollbarMetrics(effectiveListState, effectiveGridState, viewportHeight)
    }
    
    // Observar cambios en el scroll
    LaunchedEffect(
        effectiveListState?.firstVisibleItemIndex,
        effectiveListState?.firstVisibleItemScrollOffset,
        effectiveGridState?.firstVisibleItemIndex,
        effectiveGridState?.firstVisibleItemScrollOffset
    ) {
        kotlinx.coroutines.delay(16)
        val viewportHeight = when {
            effectiveListState != null -> {
                val layoutInfo = effectiveListState.layoutInfo
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
            }
            effectiveGridState != null -> {
                val layoutInfo = effectiveGridState.layoutInfo
                layoutInfo.viewportSize.height.toFloat()
            }
            else -> return@LaunchedEffect
        }
        scrollbarMetrics = calculateScrollbarMetrics(effectiveListState, effectiveGridState, viewportHeight)
    }
    
    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxHeight()
            .width(width),
        contentAlignment = Alignment.CenterEnd
    ) {
        scrollbarMetrics?.let { (thumbTop, thumbHeight) ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Posicionar la scrollbar en el borde derecho de la pantalla (sin margen)
                val scrollbarX = size.width - width.toPx()
                drawRoundRect(
                    color = scrollbarColor,
                    topLeft = Offset(scrollbarX.coerceAtLeast(0f), thumbTop.coerceAtLeast(0f)),
                    size = Size(width.toPx(), thumbHeight.coerceAtMost(size.height - thumbTop)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(width.toPx() / 2, width.toPx() / 2)
                )
            }
        }
    }
}

/**
 * Flechas de scroll para la NavBar
 */
@Composable
fun ScrollArrows(
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showArrows = remember { ScrollIndicatorManager.getShowArrows(context) }
    
    if (!showArrows) return
    
    // Usar el estado pasado como parámetro o el estado compartido
    val effectiveListState = listState ?: ScrollStateHolder.currentListState
    val effectiveGridState = gridState ?: ScrollStateHolder.currentGridState
    
    // Observar cambios en el scroll
    var canScrollUpState by remember { mutableStateOf(false) }
    var canScrollDownState by remember { mutableStateOf(false) }
    
    LaunchedEffect(
        effectiveListState?.firstVisibleItemIndex,
        effectiveListState?.firstVisibleItemScrollOffset,
        effectiveGridState?.firstVisibleItemIndex,
        effectiveGridState?.firstVisibleItemScrollOffset
    ) {
        kotlinx.coroutines.delay(16)
        canScrollUpState = when {
            effectiveListState != null -> canScrollUp(effectiveListState)
            effectiveGridState != null -> canScrollUp(effectiveGridState)
            else -> false
        }
        canScrollDownState = when {
            effectiveListState != null -> canScrollDown(effectiveListState)
            effectiveGridState != null -> canScrollDown(effectiveGridState)
            else -> false
        }
    }
    
    // Inicializar el estado
    LaunchedEffect(effectiveListState, effectiveGridState) {
        canScrollUpState = when {
            effectiveListState != null -> canScrollUp(effectiveListState)
            effectiveGridState != null -> canScrollUp(effectiveGridState)
            else -> false
        }
        canScrollDownState = when {
            effectiveListState != null -> canScrollDown(effectiveListState)
            effectiveGridState != null -> canScrollDown(effectiveGridState)
            else -> false
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.arrow_up),
            contentDescription = "Scroll up",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(textColor),
            modifier = Modifier
                .size(16.dp)
                .alpha(if (canScrollUpState) 1f else 0.25f)
        )
        Image(
            painter = painterResource(id = R.drawable.arrow_down),
            contentDescription = "Scroll down",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(textColor),
            modifier = Modifier
                .size(16.dp)
                .alpha(if (canScrollDownState) 1f else 0.25f)
        )
    }
}

