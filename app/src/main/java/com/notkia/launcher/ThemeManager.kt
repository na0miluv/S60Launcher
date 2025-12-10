package com.notkia.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gestor para guardar y cargar temas de Symbian desde SharedPreferences.
 */
object ThemeManager {
    private const val PREFS_NAME = "SymbianThemePreferences"
    const val ACTION_THEME_UPDATED = "com.notkia.launcher.THEME_UPDATED"
    
    private var currentThemeState = mutableStateOf<SymbianTheme?>(null)
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Guarda un color en formato ARGB (Long)
     */
    private fun saveColor(prefs: SharedPreferences.Editor, key: String, color: Color) {
        if (color != Color.Unspecified) {
            val argb = (color.alpha * 255).toInt().shl(24) or
                    (color.red * 255).toInt().shl(16) or
                    (color.green * 255).toInt().shl(8) or
                    (color.blue * 255).toInt()
            prefs.putLong(key, argb.toLong())
        } else {
            prefs.remove(key)
        }
    }
    
    /**
     * Carga un color desde formato ARGB (Long)
     */
    private fun loadColor(prefs: SharedPreferences, key: String): Color? {
        val argb = prefs.getLong(key, -1L)
        if (argb == -1L) return null
        
        val a = ((argb shr 24) and 0xFF) / 255f
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        
        return Color(r, g, b, a)
    }
    
    /**
     * Guarda el nombre del tema activo
     */
    fun saveActiveThemeName(context: Context, themeName: String) {
        getPrefs(context).edit()
            .putString("active_theme_name", themeName)
            .apply()
    }
    
    /**
     * Carga el nombre del tema activo
     */
    fun loadActiveThemeName(context: Context): String? {
        return getPrefs(context).getString("active_theme_name", null)
    }
    
    /**
     * Guarda un tema completo
     */
    fun saveTheme(context: Context, theme: SymbianTheme, themeName: String? = null) {
        val prefs = getPrefs(context).edit()
        
        // Guardar nombre del tema si se proporciona
        if (themeName != null) {
            prefs.putString("active_theme_name", themeName)
        }
        
        // Segmentos StatusBar
        saveColor(prefs, "top_segment_bg", theme.topSegmentBackground)
        saveColor(prefs, "bottom_segment_bg", theme.bottomSegmentBackground)
        
        // Textos StatusBar
        saveColor(prefs, "top_segment_text", theme.topSegmentTextColor)
        saveColor(prefs, "bottom_segment_text", theme.bottomSegmentTextColor)
        saveColor(prefs, "clock_text", theme.clockTextColor)
        
        // Barras indicadoras
        saveColor(prefs, "signal_bars_bg", theme.signalBarsBackground)
        saveColor(prefs, "battery_bars_bg", theme.batteryBarsBackground)
        
        // Iconos
        saveColor(prefs, "signal_icon", theme.signalIconColor)
        saveColor(prefs, "battery_icon", theme.batteryIconColor)
        
        // Anchos
        prefs.putFloat("signal_bars_width", theme.signalBarsWidth.value)
        prefs.putFloat("battery_bars_width", theme.batteryBarsWidth.value)
        
        // NavBar
        saveColor(prefs, "nav_bar_bg", theme.navBarBackground)
        saveColor(prefs, "nav_bar_text", theme.navBarTextColor)
        
        // AppDrawer
        saveColor(prefs, "app_drawer_bg", theme.appDrawerBackground)
        saveColor(prefs, "app_drawer_text", theme.appDrawerTextColor)
        
        // Menús
        saveColor(prefs, "menu_bg", theme.menuBackground)
        saveColor(prefs, "menu_text", theme.menuTextColor)
        saveColor(prefs, "submenu_bg", theme.subMenuBackground)
        saveColor(prefs, "submenu_text", theme.subMenuTextColor)
        
        // Focus
        saveColor(prefs, "focus_color", theme.focusColor)
        prefs.putFloat("focus_opacity", theme.focusOpacity)
        prefs.putFloat("focus_radius", theme.focusRadius.value)
        saveColor(prefs, "focus_text", theme.focusTextColor)
        saveColor(prefs, "focus_border_color", theme.focusBorderColor)
        prefs.putFloat("focus_border_width", theme.focusBorderWidth.value)
        prefs.putBoolean("focus_has_fill", theme.focusHasFill)
        
        // Colores de énfasis
        saveColor(prefs, "accent_color", theme.accentColor)
        saveColor(prefs, "secondary_accent_color", theme.secondaryAccentColor)
        
        // Scrollbar
        saveColor(prefs, "scrollbar_color", theme.scrollbarColor)
        
        // Opciones adicionales
        prefs.putBoolean("use_gradient_opacity_indicators", theme.useGradientOpacityForIndicators)
        prefs.putFloat("list_item_corner_radius", theme.listItemCornerRadius.value)
        
        prefs.apply()
        
        // Notificar cambio de tema
        currentThemeState.value = theme
        context.sendBroadcast(Intent(ACTION_THEME_UPDATED))
    }
    
    /**
     * Carga un tema completo, usando valores por defecto si no existe
     */
    fun loadTheme(context: Context): SymbianTheme {
        val prefs = getPrefs(context)
        
        return SymbianTheme(
            topSegmentBackground = loadColor(prefs, "top_segment_bg") ?: Color.Transparent,
            bottomSegmentBackground = loadColor(prefs, "bottom_segment_bg") ?: Color.Transparent,
            topSegmentTextColor = loadColor(prefs, "top_segment_text") ?: Color.Unspecified,
            bottomSegmentTextColor = loadColor(prefs, "bottom_segment_text") ?: Color.Unspecified,
            clockTextColor = loadColor(prefs, "clock_text") ?: Color.Unspecified,
            signalBarsBackground = loadColor(prefs, "signal_bars_bg") ?: Color.Transparent,
            batteryBarsBackground = loadColor(prefs, "battery_bars_bg") ?: Color.Transparent,
            signalIconColor = loadColor(prefs, "signal_icon") ?: Color.Unspecified,
            batteryIconColor = loadColor(prefs, "battery_icon") ?: Color.Unspecified,
            signalBarsWidth = prefs.getFloat("signal_bars_width", 20f).dp,
            batteryBarsWidth = prefs.getFloat("battery_bars_width", 20f).dp,
            navBarBackground = loadColor(prefs, "nav_bar_bg") ?: Color.Unspecified,
            navBarTextColor = loadColor(prefs, "nav_bar_text") ?: Color(0xFF000040),
            appDrawerBackground = loadColor(prefs, "app_drawer_bg") ?: Color.Unspecified,
            appDrawerTextColor = loadColor(prefs, "app_drawer_text") ?: Color.Unspecified,
            menuBackground = loadColor(prefs, "menu_bg") ?: Color.Unspecified,
            menuTextColor = loadColor(prefs, "menu_text") ?: Color.Unspecified,
            subMenuBackground = loadColor(prefs, "submenu_bg") ?: Color.Unspecified,
            subMenuTextColor = loadColor(prefs, "submenu_text") ?: Color.Unspecified,
            focusColor = loadColor(prefs, "focus_color") ?: Color.White.copy(alpha = 0.5f),
            focusOpacity = prefs.getFloat("focus_opacity", 0.5f),
            focusRadius = prefs.getFloat("focus_radius", 12f).dp,
            focusTextColor = loadColor(prefs, "focus_text") ?: Color.Unspecified,
            focusBorderColor = loadColor(prefs, "focus_border_color") ?: Color.Unspecified,
            focusBorderWidth = prefs.getFloat("focus_border_width", 0f).dp,
            focusHasFill = prefs.getBoolean("focus_has_fill", true),
            accentColor = loadColor(prefs, "accent_color") ?: Color.Unspecified,
            secondaryAccentColor = loadColor(prefs, "secondary_accent_color") ?: Color.Unspecified,
            scrollbarColor = loadColor(prefs, "scrollbar_color") ?: Color.Unspecified,
            useGradientOpacityForIndicators = prefs.getBoolean("use_gradient_opacity_indicators", false),
            listItemCornerRadius = prefs.getFloat("list_item_corner_radius", 0f).dp
        )
    }
    
    /**
     * Resetea el tema a los valores por defecto (Nokia)
     */
    fun resetTheme(context: Context) {
        val prefs = getPrefs(context).edit()
        // Limpiar todas las preferencias excepto el nombre del tema activo
        prefs.clear()
        // Guardar que el tema activo es Nokia
        prefs.putString("active_theme_name", "nokia")
        prefs.apply()
        
        // Notificar cambio de tema
        currentThemeState.value = null
        context.sendBroadcast(Intent(ACTION_THEME_UPDATED))
    }
    
    /**
     * Restaura el tema guardado al iniciar la aplicación
     */
    fun restoreSavedTheme(context: Context) {
        val activeThemeName = loadActiveThemeName(context)
        if (activeThemeName != null) {
            when (activeThemeName) {
                "nokia" -> {
                    // Para Nokia, solo asegurarse de que las preferencias estén limpias
                    resetTheme(context)
                }
                "classic_white" -> {
                    // Para Classic White, cargar y aplicar el tema
                    val theme = com.notkia.launcher.ui.theme.ClassicWhiteTheme.default()
                    saveTheme(context, theme, "classic_white")
                }
                // Agregar más temas aquí cuando se agreguen
            }
        }
    }
    
    /**
     * Composable que observa cambios en el tema y actualiza automáticamente
     */
    @Composable
    fun rememberTheme(context: Context = LocalContext.current): SymbianTheme {
        var theme by remember { mutableStateOf(loadTheme(context)) }
        
        DisposableEffect(context) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_THEME_UPDATED) {
                        theme = loadTheme(context ?: return)
                    }
                }
            }
            
            val filter = IntentFilter(ACTION_THEME_UPDATED)
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            
            onDispose {
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    // El receiver ya fue desregistrado
                }
            }
        }
        
        return theme
    }
}

