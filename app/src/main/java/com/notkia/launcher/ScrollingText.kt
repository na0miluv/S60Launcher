package com.notkia.launcher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Composable para mostrar texto con animación de scroll horizontal cuando está enfocado
 * Si el texto es demasiado largo, muestra puntos suspensivos cuando no está enfocado
 * Cuando está enfocado, anima el texto de izquierda a derecha
 */
@Composable
fun ScrollingText(
    text: String,
    color: Color,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = androidx.compose.ui.text.TextStyle.Default
) {
    val density = LocalDensity.current
    var containerWidth by remember { mutableStateOf(0f) }
    var textWidth by remember { mutableStateOf(0f) }
    var needsScrolling by remember { mutableStateOf(false) }
    
    // Verificar si necesita scroll
    LaunchedEffect(containerWidth, textWidth) {
        needsScrolling = textWidth > containerWidth && containerWidth > 0
    }
    
    // Animación de scroll cuando está enfocado y necesita scroll
    val shouldAnimate = isFocused && needsScrolling
    val infiniteTransition = rememberInfiniteTransition(label = "scroll")
    
    // Calcular la distancia a recorrer
    val scrollDistance = remember(textWidth, containerWidth) {
        (textWidth - containerWidth).coerceAtLeast(0f)
    }
    
    val offsetX by if (shouldAnimate && scrollDistance > 0) {
        // Animación con pausas: 
        // 0-2000ms: deslizar de izquierda a derecha
        // 2000-3000ms: pausa
        // 3000-5000ms: deslizar de derecha a izquierda
        // 5000-6000ms: pausa
        // Repetir
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 6000, // Ciclo completo: 6 segundos
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "scrollX"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    
    // Calcular el offset basado en la fase de la animación
    val currentOffset = remember(offsetX, scrollDistance) {
        when {
            !shouldAnimate || scrollDistance <= 0 -> 0f
            offsetX < 0.333f -> {
                // Fase 1: Deslizar de izquierda a derecha (0-2000ms)
                offsetX / 0.333f * scrollDistance
            }
            offsetX < 0.5f -> {
                // Fase 2: Pausa en la derecha (2000-3000ms)
                scrollDistance
            }
            offsetX < 0.833f -> {
                // Fase 3: Deslizar de derecha a izquierda (3000-5000ms)
                scrollDistance - ((offsetX - 0.5f) / 0.333f * scrollDistance)
            }
            else -> {
                // Fase 4: Pausa en la izquierda (5000-6000ms)
                0f
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                containerWidth = with(density) { coordinates.size.width.toDp().value }
            }
    ) {
        // Texto invisible para medir el ancho real
        Text(
            text = text,
            style = style,
            modifier = Modifier
                .alpha(0f)
                .onGloballyPositioned { coordinates ->
                    textWidth = with(density) { coordinates.size.width.toDp().value }
                },
            maxLines = 1
        )
        
        if (shouldAnimate) {
            // Texto animado cuando está enfocado
            Text(
                text = text,
                color = color,
                style = style,
                modifier = Modifier.offset(x = (-currentOffset).dp),
                maxLines = 1
            )
        } else {
            // Texto normal con puntos suspensivos cuando no está enfocado o no necesita scroll
            Text(
                text = text,
                color = color,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

