package com.notkia.launcher

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun NavBar(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null
) {
    val context = LocalContext.current
    val theme = ThemeManager.rememberTheme(context)
    
    // Usar colores del tema o valores por defecto
    // Si el tema especifica un color (incluso si es Transparent), usar color sólido opaco
    val navBarBackground = if (theme.navBarBackground != Color.Unspecified) {
        // Si es Transparent, mantenerlo transparente, sino asegurar opacidad completa
        if (theme.navBarBackground == Color.Transparent) {
            Color.Transparent
        } else {
            theme.navBarBackground.copy(alpha = 1f) // Asegurar opacidad completa
        }
    } else {
        Color.White.copy(alpha = 0.8f)
    }
    val textColor = theme.navBarTextColor

    // Detectar si el dispositivo tiene un teclado físico usando KeyboardManager
    // Usamos remember con la configuración como dependencia para que se recalcule
    // si la configuración cambia (por ejemplo, después de un crash o cambio de orientación)
    val config = context.resources.configuration
    val hasPhysicalKeyboard = remember(config.keyboard, config.hardKeyboardHidden) {
        KeyboardManager.hasPhysicalKeyboard(context)
    }

    val isAppDrawer = currentRoute == "app_drawer"
    val isOptionsMenu = currentRoute.startsWith("options_menu")
    val isActionsMenu = currentRoute.startsWith("actions_menu")
    val isEditMenu = currentRoute.startsWith("edit_menu")
    val isAddToMenu = currentRoute.startsWith("add_to_menu")

    val softLeftPackage = SoftKeysManager.getSoftLeft(context)
    val softRightPackage = SoftKeysManager.getSoftRight(context)

    val packageManager = context.packageManager

    val softLeftAppName = remember(softLeftPackage) {
        softLeftPackage?.let {
            try {
                val appInfo = packageManager.getApplicationInfo(it, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
    val softRightAppName = remember(softRightPackage) {
        softRightPackage?.let {
            try {
                val appInfo = packageManager.getApplicationInfo(it, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    val settingsText = when {
        isAppDrawer -> stringResource(R.string.options)
        isOptionsMenu || isActionsMenu || isEditMenu || isAddToMenu -> ""
        else -> softLeftAppName ?: ""
    }

    val galleryText = when {
        isAppDrawer || isOptionsMenu || isActionsMenu || isEditMenu || isAddToMenu -> stringResource(R.string.back)
        else -> softRightAppName ?: ""
    }

    val goBack = {
        navController.popBackStack()
    }

    val launchApp = { packageName: String ->
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let { context.startActivity(it) }
    }

    val openAppDrawer = {
        navController.navigate("app_drawer")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (theme.navBarBackground != Color.Unspecified) {
                    // Si hay un color especificado, usar color sólido (sin gradiente)
                    // Asegurar que sea opaco si no es Transparent
                    Modifier.background(navBarBackground)
                } else {
                    // Solo usar gradiente si no hay color especificado
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                navBarBackground
                            )
                        )
                    )
                }
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = settingsText,
            color = textColor,
            fontSize = 20.sp,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        when {
                            isAppDrawer -> navController.navigate("options_menu")
                            isOptionsMenu || isActionsMenu || isEditMenu || isAddToMenu -> { /* Do nothing */ }
                            else -> softLeftPackage?.let { launchApp(it) }
                        }
                    }
                ),
            textAlign = TextAlign.Start,
            maxLines = 1
        )
        
        // Mostrar flechas de scroll o botón "Menu" en el centro
        if (currentRoute != "home") {
            // Mostrar flechas de scroll (usará el estado compartido si no se pasa explícitamente)
            ScrollArrows(
                listState = listState,
                gridState = gridState,
                textColor = textColor,
                modifier = Modifier.weight(1f)
            )
        } else if (!hasPhysicalKeyboard && currentRoute == "home") {
            Text(
                text = stringResource(R.string.menu),
                color = textColor,
                fontSize = 20.sp,
                fontFamily = FontFamily.Default,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { openAppDrawer() }
                    ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        
        Text(
            text = galleryText,
            color = textColor,
            fontSize = 20.sp,
            fontFamily = FontFamily.Default,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        when {
                            isAppDrawer || isOptionsMenu || isActionsMenu || isEditMenu || isAddToMenu -> goBack()
                            else -> softRightPackage?.let { launchApp(it) }
                        }
                    }
                ),
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}
