package com.notkia.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.Calendar
import android.view.KeyEvent as AndroidKeyEvent

@Composable
fun AnalogueClock(
    calendar: Calendar,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    useClassicWhiteAssets: Boolean = false
) {
    val clockSize = 96.dp
    
    // Obtener la hora y minuto actuales del Calendar para asegurar que se actualice
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    // Seleccionar los recursos según el tema
    val backResId = if (useClassicWhiteAssets) R.drawable.cwst1_back else R.drawable.analog1_back
    val hourResId = if (useClassicWhiteAssets) R.drawable.cwst1_hour else R.drawable.analog1_hour
    val minResId = if (useClassicWhiteAssets) R.drawable.cwst1_min else R.drawable.analog1_min

    Box(
        modifier = Modifier
            .size(clockSize),
        contentAlignment = Alignment.Center
    ) {
        // Fondo
        Image(
            painter = painterResource(id = backResId),
            contentDescription = "Clock background",
            modifier = Modifier.size(clockSize),
            colorFilter = if (useClassicWhiteAssets) null else ColorFilter.tint(contentColor)
        )

        // Hora - punto de pivote en el borde inferior con margen para la colilla
        Image(
            painter = painterResource(id = hourResId),
            contentDescription = "Hour hand",
            modifier = Modifier
                .offset(x = (2).dp, y=(-12).dp) // Margen hacia arriba para mostrar la colilla
                .graphicsLayer(
                    rotationZ = (hour % 12) * 30f + minute * 0.5f,
                    transformOrigin = TransformOrigin(0.5f, 1.0f) // Pivote en el borde inferior
                ),
            colorFilter = if (useClassicWhiteAssets) null else ColorFilter.tint(contentColor)
        )

        // Minuto - punto de pivote en el borde derecho con margen para la colilla
        // El SVG está horizontal (apuntando a las 3), necesitamos compensar -90 grados
        // Cada minuto son 6 grados (360 / 60 = 6)
        Image(
            painter = painterResource(id = minResId),
            contentDescription = "Minute hand",
            modifier = Modifier
                .offset(x = (-20).dp, y = 0.dp) // Margen hacia la izquierda para mostrar la colilla
                .graphicsLayer(
                    rotationZ = minute * 6f + 90f, // Convertir minutos a grados y compensar orientación horizontal del SVG
                    transformOrigin = TransformOrigin(1.0f, 0.5f) // Pivote en el borde derecho
                ),
            colorFilter = if (useClassicWhiteAssets) null else ColorFilter.tint(contentColor)
        )
    }
}
