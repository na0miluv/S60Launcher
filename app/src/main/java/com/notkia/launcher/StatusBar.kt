package com.example.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.SignalCellularOff
import androidx.compose.material.icons.outlined.Wifi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notkia.launcher.CustomAppInfoManager
import com.notkia.launcher.ThemeManager
import com.notkia.launcher.ui.theme.GlobalFontSize
import com.notkia.launcher.ui.theme.robotoCondensed
import com.notkia.launcher.ui.theme.StatusBarThemes
import com.notkia.launcher.ui.theme.StatusBarTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.notkia.launcher.*

@Composable
fun N70Clock_RetroScaled(
    calendar: Calendar,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fmt.format(calendar.time),
            color = contentColor,
            fontFamily = robotoCondensed,
            fontSize = 46.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 42.sp,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.graphicsLayer {
                scaleX = 0.80f
            }
        )
    }
}



@Composable
fun StatusBar(
    calendar: Calendar,
    carrierName: String,
    batteryLevel: Int,
    isCharging: Boolean,
    signalLevel: Int,
    networkType: String,
    isWifiConnected: Boolean,
    wifiSignalLevel: Int,
    isHomeScreen: Boolean,
    theme: StatusBarTheme = StatusBarThemes.S60v3
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val context = LocalContext.current
    val clockStyle = remember { CustomAppInfoManager.getClockStyle(context) }
    
    // Cargar el tema personalizado (se actualiza dinámicamente)
    val symbianTheme = ThemeManager.rememberTheme(context)
    
    // Colores del tema o valores por defecto
    val defaultContentColor = MaterialTheme.colorScheme.onSurface
    val topSegmentTextColor = if (symbianTheme.topSegmentTextColor != Color.Unspecified) 
        symbianTheme.topSegmentTextColor else defaultContentColor
    val bottomSegmentTextColor = if (symbianTheme.bottomSegmentTextColor != Color.Unspecified) 
        symbianTheme.bottomSegmentTextColor else defaultContentColor
    val clockTextColor = if (symbianTheme.clockTextColor != Color.Unspecified) 
        symbianTheme.clockTextColor else defaultContentColor
    val signalIconColor = if (symbianTheme.signalIconColor != Color.Unspecified) 
        symbianTheme.signalIconColor else bottomSegmentTextColor
    val batteryIconColor = if (symbianTheme.batteryIconColor != Color.Unspecified) 
        symbianTheme.batteryIconColor else bottomSegmentTextColor
    
    // Colores de fondo con opacidad completa si el tema los especifica
    val topSegmentBg = if (symbianTheme.topSegmentBackground != Color.Transparent && 
                           symbianTheme.topSegmentBackground != Color.Unspecified) {
        symbianTheme.topSegmentBackground.copy(alpha = 1f)
    } else {
        Color.Transparent
    }
    val bottomSegmentBg = if (symbianTheme.bottomSegmentBackground != Color.Transparent && 
                              symbianTheme.bottomSegmentBackground != Color.Unspecified) {
        symbianTheme.bottomSegmentBackground.copy(alpha = 1f)
    } else {
        Color.Transparent
    }

    // Dimensiones fijas para mantener estructura estable
    // Altura del segmento superior: debe ser suficiente para las 7 barras sin cortarse
    // 7 barras * 5dp + 6 espacios * 2dp = 35dp + 12dp = 47dp mínimo
    // Aumentado a 58dp para que las barras no se vean aplastadas y el segmento inferior se mueva hacia abajo
    val topSegmentHeight = 58.dp
    val bottomSegmentHeight = 28.dp // Duplicado del tamaño anterior (14dp * 2)
    val statusBarHeight = topSegmentHeight + bottomSegmentHeight // 86dp total
    val clockSize = 96.dp
    val clockAreaLeft = symbianTheme.signalBarsWidth + 8.dp
    val textAreaLeft = clockAreaLeft + clockSize + 10.dp
    val textAreaWidth = 200.dp // Ancho fijo para el área de texto

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(statusBarHeight)
    ) {
        // Padding horizontal interno para evitar cortes en esquinas curvas
        val horizontalPadding = 8.dp
        
        // Elemento de fondo superior (para barras de señal y batería)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topSegmentHeight)
                .background(topSegmentBg)
        )

        // Elemento de fondo inferior (para iconos, fecha y texto)
        Box(
            modifier = Modifier
                .offset(y = topSegmentHeight)
                .fillMaxWidth()
                .height(bottomSegmentHeight)
                .background(bottomSegmentBg)
        )

        // Barras de señal (verde) - segmento superior, izquierda
        Box(
            modifier = Modifier
                .offset(x = horizontalPadding, y = 0.dp) // Padding horizontal
                .width(symbianTheme.signalBarsWidth)
                .height(topSegmentHeight)
                .background(symbianTheme.signalBarsBackground),
            contentAlignment = Alignment.Center
        ) {
            SignalBars(
                signalLevel, 
                isWifiConnected, 
                wifiSignalLevel, 
                barColor = topSegmentTextColor,
                useGradientOpacity = symbianTheme.useGradientOpacityForIndicators
            )
        }

        // Barras de batería (rojo) - segmento superior, derecha
        Box(
            modifier = Modifier
                .offset(x = -horizontalPadding, y = 0.dp) // Padding horizontal desde la derecha
                .align(Alignment.TopEnd)
                .width(symbianTheme.batteryBarsWidth)
                .height(topSegmentHeight)
                .background(symbianTheme.batteryBarsBackground),
            contentAlignment = Alignment.Center
        ) {
            BatteryBars(
                batteryLevel = batteryLevel, 
                isCharging = isCharging,
                barColor = topSegmentTextColor,
                useGradientOpacity = symbianTheme.useGradientOpacityForIndicators
            )
        }

        // Reloj superpuesto sobre ambos segmentos (amarillo) - alineado en la parte superior con los primeros segmentos
        // Área del reloj: cuadrado perfecto (mismo ancho y alto)
        Box(
            modifier = Modifier
                .offset(x = clockAreaLeft + horizontalPadding, y = 0.dp) // Padding horizontal
                .width(clockSize)
                .height(clockSize)
                .background(theme.clockBackground),
            contentAlignment = Alignment.TopCenter
        ) {
            if (clockStyle == CustomAppInfoManager.CLOCK_STYLE_1) {
                AnalogueClock(calendar = calendar, contentColor = clockTextColor)
            } else {
                N70Clock_RetroScaled(
                    calendar = calendar,
                    contentColor = clockTextColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Nombre del carrier/Menu - segmento superior, centro-izquierda (después del reloj)
        Box(
            modifier = Modifier
                .offset(x = textAreaLeft + horizontalPadding, y = 0.dp) // Padding horizontal
                .width(textAreaWidth)
                .height(topSegmentHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isHomeScreen) {
                Text(
                    text = carrierName,
                    color = topSegmentTextColor,
                    fontSize = (GlobalFontSize.value + 12).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                Text(
                    text = "Menu",
                    color = topSegmentTextColor,
                    fontSize = (GlobalFontSize.value + 12).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Fecha - segmento inferior, alineada a la izquierda (igual que el nombre de la red)
        Box(
            modifier = Modifier
                .offset(x = textAreaLeft + horizontalPadding, y = topSegmentHeight) // Padding horizontal
                .width(textAreaWidth)
                .height(bottomSegmentHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isHomeScreen) {
                DateDisplay(calendar, textColor = bottomSegmentTextColor, modifier = Modifier.padding(start = 4.dp))
            }
        }

        // Icono de señal/WiFi/Tipo de red - segmento inferior, izquierda (debajo de las barras de señal)
        Box(
            modifier = Modifier
                .offset(x = horizontalPadding, y = topSegmentHeight) // Padding horizontal
                .width(symbianTheme.signalBarsWidth)
                .height(bottomSegmentHeight),
            contentAlignment = Alignment.Center
        ) {
            SignalIcon(signalLevel, networkType, isWifiConnected, signalIconColor)
        }

        // Icono de batería - segmento inferior, derecha
        Box(
            modifier = Modifier
                .offset(x = -horizontalPadding, y = topSegmentHeight) // Padding horizontal desde la derecha
                .align(Alignment.TopEnd)
                .width(symbianTheme.batteryBarsWidth)
                .height(bottomSegmentHeight),
            contentAlignment = Alignment.Center
        ) {
            BatteryIcon(batteryIconColor)
        }
    }
}

@Composable
fun SignalBars(
    signalLevel: Int,
    isWifiConnected: Boolean,
    wifiSignalLevel: Int,
    barColor: Color = MaterialTheme.colorScheme.onSurface,
    useGradientOpacity: Boolean = false
) {
    val segments = 7
    val filledSegments = if (isWifiConnected) {
        (wifiSignalLevel / 4f * 7f).toInt().coerceIn(0, 7)
    } else {
        signalLevel
    }
    val barWidths = listOf(10.dp, 11.dp, 12.dp, 13.dp, 14.dp, 15.dp, 16.dp)
    val contentColor = barColor

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isWifiConnected || signalLevel > 0) {
            (segments - 1 downTo 0).forEach { index ->
                // Calcular opacidad degradada si está habilitada
                val opacity = if (useGradientOpacity && index < filledSegments) {
                    // Opacidad más alta arriba (índice más alto), más baja abajo
                    val progress = index.toFloat() / (segments - 1)
                    0.3f + (progress * 0.7f) // Rango de 0.3 a 1.0
                } else {
                    1f
                }
                
                val barColorWithOpacity = contentColor.copy(alpha = opacity)
                
                Box(
                    modifier = Modifier
                        .size(width = barWidths[index], height = 5.dp)
                        .then(
                            if (index < filledSegments) Modifier.background(barColorWithOpacity, RoundedCornerShape(2.dp))
                            else Modifier.border(width = 1.dp, color = barColorWithOpacity, shape = RoundedCornerShape(2.dp))
                        )
                )
            }
        } else {
            // Si no hay señal, mostrar icono de sin señal en el segmento superior
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.SignalCellularOff,
                    contentDescription = "No Signal",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SignalIcon(
    signalLevel: Int,
    networkType: String,
    isWifiConnected: Boolean,
    iconColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val contentColor = iconColor

    if (isWifiConnected) {
        Icon(
            imageVector = Icons.Outlined.Wifi,
            contentDescription = "WiFi Signal",
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    } else {
        Text(
            text = networkType,
            color = contentColor,
            fontSize = 20.sp
        )
    }
}

@Composable
fun SignalIndicator(
    signalLevel: Int,
    networkType: String,
    isWifiConnected: Boolean,
    wifiSignalLevel: Int,
    barColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    useGradientOpacity: Boolean = false
) {
    SignalBars(signalLevel, isWifiConnected, wifiSignalLevel, barColor, useGradientOpacity)
    Spacer(modifier = Modifier.height(4.dp))
    SignalIcon(signalLevel, networkType, isWifiConnected, iconColor)
}

@Composable
fun BatteryBars(
    batteryLevel: Int,
    isCharging: Boolean = false,
    barColor: Color = MaterialTheme.colorScheme.onSurface,
    useGradientOpacity: Boolean = false
) {
    val segments = 7
    val barWidths = listOf(10.dp, 11.dp, 12.dp, 13.dp, 14.dp, 15.dp, 16.dp)
    val contentColor = barColor

    // estado animado para carga estilo Symbian
    var animatedSegments by remember { mutableStateOf(0) }

    // animación de carga: barras llenándose desde abajo hacia arriba y reiniciando (estilo Symbian)
    LaunchedEffect(isCharging) {
        if (isCharging) {
            while (true) {
                // Llenar desde el segmento más bajo (0) hasta el más alto (segments-1)
                // animatedSegments representa cuántos segmentos están llenos (1 a 7)
                for (i in 0 until segments) {
                    animatedSegments = i + 1
                    delay(500) // Timing similar a Symbian
                }
                // Pausa breve cuando está completamente lleno
                delay(1000)
                // Reiniciar la animación (vuelve a 0 y luego empieza de nuevo)
                animatedSegments = 0
                delay(40)
            }
        } else {
            // Reset cuando deja de cargar
            animatedSegments = 0
        }
    }

    // lógica: si está cargando → usar animación, si no → porcentaje real
    val filledSegments =
        if (isCharging) animatedSegments
        else (batteryLevel / 100f * segments).toInt().coerceIn(0, segments)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // segmentos (importante: orden invertido para llenar desde abajo)
        for (index in 0 until segments) {
            val fromBottom = segments - 1 - index
            val isFilled = fromBottom < filledSegments
            
            // Calcular opacidad degradada si está habilitada
            val opacity = if (useGradientOpacity && isFilled) {
                // Opacidad más alta arriba (índice más alto), más baja abajo
                val progress = (segments - 1 - index).toFloat() / (segments - 1)
                0.3f + (progress * 0.7f) // Rango de 0.3 a 1.0
            } else {
                1f
            }
            
            val barColorWithOpacity = contentColor.copy(alpha = opacity)

            Box(
                modifier = Modifier
                    .size(width = barWidths[segments - 1 - index], height = 5.dp)
                    .then(
                        if (isFilled)
                            Modifier.background(barColorWithOpacity, RoundedCornerShape(2.dp))
                        else
                            Modifier.border(
                                1.dp,
                                barColorWithOpacity,
                                RoundedCornerShape(2.dp)
                            )
                    )
            )
        }
    }
}

@Composable
fun BatteryIcon(iconColor: Color = MaterialTheme.colorScheme.onSurface) {
    val contentColor = iconColor

    // icono outline, siempre el mismo (como en Symbian)
    Icon(
        imageVector = Icons.Outlined.BatteryStd,
        contentDescription = "Battery",
        tint = contentColor,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
fun BatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean = false,
    barColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    useGradientOpacity: Boolean = false
) {
    BatteryBars(batteryLevel, isCharging, barColor, useGradientOpacity)
    Spacer(modifier = Modifier.height(2.dp))
    BatteryIcon(iconColor)
}



@Composable
fun DateDisplay(
    calendar: Calendar,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val dayFormat = remember { SimpleDateFormat("E", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val dayName = dayFormat.format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Text(
        text = "${dayName.take(2)} ${dateFormat.format(calendar.time)}",
        color = textColor,
        fontSize = (GlobalFontSize.value + 4).sp,
        modifier = modifier
    )
}
