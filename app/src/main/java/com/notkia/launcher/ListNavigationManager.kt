package com.notkia.launcher

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manager para manejar la navegación y el scroll en listas LazyColumn.
 * Proporciona funciones globales para navegación con wrap around y scroll centrado.
 */

/**
 * Hace scroll suave hasta un elemento en una lista LazyColumn.
 * Solo hace scroll si el elemento no está visible, moviendo un elemento a la vez.
 * 
 * @param index El índice del elemento al que hacer scroll
 * @param totalItems El número total de elementos en la lista
 * @param useAnimation Si es true, usa animación. Si es false, hace scroll instantáneo (para scroll rápido)
 */
suspend fun LazyListState.scrollToItemSmoothly(
    index: Int,
    totalItems: Int,
    useAnimation: Boolean = true
) {
    if (index < 0 || index >= totalItems) return
    
    val layoutInfo = this.layoutInfo
    
    // Buscar el elemento objetivo en los items visibles
    val targetItem = layoutInfo.visibleItemsInfo.find { it.index == index }
    
    if (targetItem == null) {
        // El elemento no está visible, necesitamos hacer scroll hasta él
        val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        
        // Calcular altura promedio de los elementos visibles
        val visibleItems = layoutInfo.visibleItemsInfo
        val averageItemHeight = if (visibleItems.isNotEmpty()) {
            visibleItems.map { it.size }.average().toFloat()
        } else {
            60f // Altura por defecto si no hay elementos visibles
        }
        
        if (index < firstVisibleIndex) {
            // El elemento está antes del primer visible - scroll hacia arriba
            // Solo scrollear un elemento hacia arriba
            if (useAnimation) {
                animateScrollBy(-averageItemHeight, animationSpec = tween(150))
            } else {
                scrollBy(-averageItemHeight)
            }
        } else {
            // El elemento está después del último visible - scroll hacia abajo
            // Solo scrollear un elemento hacia abajo para que el siguiente sea visible
            if (useAnimation) {
                animateScrollBy(averageItemHeight, animationSpec = tween(150))
            } else {
                scrollBy(averageItemHeight)
            }
        }
    }
    // Si el elemento ya está visible, no hacer nada
}

/**
 * Maneja la navegación con wrap around en listas LazyColumn.
 * Calcula el índice objetivo con wrap around, verifica visibilidad y hace scroll centrado si es necesario.
 * El wrap around es instantáneo (sin animación) para evitar problemas de rendimiento.
 * 
 * @param currentIndex El índice actual del elemento enfocado
 * @param direction La dirección de navegación: 1 para abajo, -1 para arriba
 * @param totalItems El número total de elementos en la lista
 * @param listState El estado de la lista LazyColumn
 * @param focusRequesters Lista de FocusRequester para cada elemento
 * @param scope El CoroutineScope para lanzar la coroutine de scroll
 * @param useAnimation Si es true, usa animación. Si es false, hace scroll instantáneo (para scroll rápido)
 */
fun handleListNavigationWithWrapAround(
    currentIndex: Int,
    direction: Int,
    totalItems: Int,
    listState: LazyListState,
    focusRequesters: List<FocusRequester>,
    scope: CoroutineScope,
    useAnimation: Boolean = true
) {
    if (totalItems == 0) return
    
    // Calcular índice objetivo con wrap around
    val targetIndex = when {
        direction > 0 -> (currentIndex + 1) % totalItems // Abajo: wrap al inicio si estamos al final
        direction < 0 -> (currentIndex - 1 + totalItems) % totalItems // Arriba: wrap al final si estamos al inicio
        else -> currentIndex
    }
    
    // Detectar si hay wrap around (del último al primero o del primero al último)
    val isWrapAround = when {
        direction > 0 -> currentIndex == totalItems - 1 && targetIndex == 0 // Del último al primero
        direction < 0 -> currentIndex == 0 && targetIndex == totalItems - 1 // Del primero al último
        else -> false
    }
    
    // Verificar si el elemento objetivo está visible
    val targetItem = listState.layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
    val isTargetVisible = targetItem != null
    
    scope.launch {
        if (isTargetVisible) {
            // Si está visible, solo enfocar sin hacer scroll
            focusRequesters[targetIndex].requestFocus()
        } else {
            // Si no está visible, hacer scroll suave y luego enfocar
            if (isWrapAround) {
                // Wrap around: scroll instantáneo sin animación
                listState.scrollToItem(targetIndex)
            } else {
                // Navegación normal: scroll suave sin centrado
                listState.scrollToItemSmoothly(targetIndex, totalItems, useAnimation)
            }
            focusRequesters[targetIndex].requestFocus()
        }
    }
}

