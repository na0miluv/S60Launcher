package com.notkia.launcher

import android.Manifest
import android.app.WallpaperManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.MediaStore
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notkia.launcher.ui.theme.S60LauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.launcher.StatusBar
import java.util.Calendar

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val installTime: Long,
    val lastUsedTime: Long
)

data class EventInfo(
    val title: String
)

data class NotificationInfo(
    val appName: String,
    val icon: Drawable,
    val count: Int
)

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private var carrierName by mutableStateOf("No SIM")
    private var wallpaperDrawable by mutableStateOf<Drawable?>(null)
    private var batteryLevel by mutableIntStateOf(100)
    private var isCharging by mutableStateOf(false)
    private var signalLevel by mutableIntStateOf(0)
    private var networkType by mutableStateOf("")
    private var isWifiConnected by mutableStateOf(false)
    private var wifiSignalLevel by mutableIntStateOf(0)
    private var wifiSsid by mutableStateOf("")
    private var appDrawerFocusedIndex by mutableIntStateOf(0)
    private var optionsMenuFocusedIndex by mutableIntStateOf(0)
    private var hasWindowFocus by mutableStateOf(true)
    private var navigateToHome by mutableStateOf(false)
    private var apps by mutableStateOf<List<AppInfo>>(emptyList())
    private var calendarEvents by mutableStateOf<List<EventInfo>>(emptyList())
    private var notifications by mutableStateOf<List<NotificationInfo>>(emptyList())
    private var packageToEdit by mutableStateOf<String?>(null)
    private var lastNotificationPackages by mutableStateOf<List<String>>(emptyList())
    private var isContentReady by mutableStateOf(false)

    // Callback para cuando se selecciona una imagen (se pasa desde EditMenu)
    private var imagePickerCallback: ((String?) -> Unit)? = null
    
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            packageToEdit?.let { packageName ->
                val contentResolver = contentResolver
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Llamar al callback en lugar de guardar inmediatamente
                imagePickerCallback?.invoke(it.toString())
            }
        } ?: run {
            // Si se cancela, pasar null al callback
            imagePickerCallback?.invoke(null)
        }
        // Limpiar el callback después de usarlo
        imagePickerCallback = null
        packageToEdit = null
    }

    private var batteryReceiver: BroadcastReceiver? = null
    
    private val appsUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CustomAppInfoManager.ACTION_APPS_UPDATED) {
                apps = getInstalledApps(context)
                notifications = processNotificationPackages(lastNotificationPackages)
            }
        }
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == NotificationService.ACTION_UPDATE) {
                val notificationPackages = intent.getStringArrayListExtra(NotificationService.EXTRA_NOTIFICATION_PACKAGES)
                lastNotificationPackages = notificationPackages ?: emptyList()
                notifications = processNotificationPackages(lastNotificationPackages)
            }
        }
    }

    private fun processNotificationPackages(notificationPackages: List<String>): List<NotificationInfo> {
        return notificationPackages.groupingBy { it }.eachCount().mapNotNull { (packageName, count) ->
            val app = apps.find { it.packageName == packageName }
            if (app != null) {
                NotificationInfo(
                    appName = app.appName,
                    icon = app.icon,
                    count = count
                )
            } else {
                null
            }
        }
    }

    /**
     * Intenta cargar el wallpaper del sistema de forma agresiva
     * Se llama múltiples veces para asegurar que se cargue incluso en versiones modernas
     */
    private fun loadWallpaper() {
        try {
            val newWallpaper = com.notkia.launcher.WallpaperManager.getWallpaperDrawableForUI(this)
            if (newWallpaper != null) {
                wallpaperDrawable = newWallpaper
                com.notkia.launcher.WallpaperManager.updateTheme(this)
                android.util.Log.d("MainActivity", "Wallpaper loaded successfully")
            } else {
                android.util.Log.w("MainActivity", "Wallpaper is null, will retry later")
                // Reintentar después de un breve delay si no se cargó
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(500)
                    val retryWallpaper = com.notkia.launcher.WallpaperManager.getWallpaperDrawableForUI(this@MainActivity)
                    if (retryWallpaper != null && wallpaperDrawable == null) {
                        wallpaperDrawable = retryWallpaper
                        com.notkia.launcher.WallpaperManager.updateTheme(this@MainActivity)
                        android.util.Log.d("MainActivity", "Wallpaper loaded successfully on retry")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading wallpaper", e)
            // Reintentar después de un delay
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                try {
                    val retryWallpaper = com.notkia.launcher.WallpaperManager.getWallpaperDrawableForUI(this@MainActivity)
                    if (retryWallpaper != null) {
                        wallpaperDrawable = retryWallpaper
                        com.notkia.launcher.WallpaperManager.updateTheme(this@MainActivity)
                        android.util.Log.d("MainActivity", "Wallpaper loaded successfully after error retry")
                    }
                } catch (e2: Exception) {
                    android.util.Log.w("MainActivity", "Failed to load wallpaper on retry", e2)
                }
            }
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_PHONE_STATE] == true) {
            carrierName = fetchCarrierName()
            listenToSignalStrength()
        }
        // Intentar cargar el wallpaper nuevamente con permisos completos
        // En Android 16+ (API 36+), se requiere READ_EXTERNAL_STORAGE explícitamente
        // incluso si se tiene READ_MEDIA_IMAGES
        loadWallpaper()
        if (permissions[Manifest.permission.READ_CALENDAR] == true) {
            calendarEvents = getCalendarEvents(this)
        }
        if (permissions[Manifest.permission.PACKAGE_USAGE_STATS] == true) {
            apps = getInstalledApps(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restaurar el tema guardado al iniciar la aplicación
        ThemeManager.restoreSavedTheme(this)

        PinnedAppsManager.pinnedApps.clear()
        PinnedAppsManager.initializeDefaultApps(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Hacer transparentes las barras del sistema (notch y navegación)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Ocultar la barra de estado de forma global
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Cargar el wallpaper inmediatamente, sin esperar permisos
        // El wallpaper del sistema puede requerir permisos en Android 13+, pero intentamos de todas formas
        loadWallpaper()

        apps = getInstalledApps(this)
        
        // Marcar el contenido como listo después de un breve delay para permitir que el wallpaper se renderice
        lifecycleScope.launch {
            delay(100)
            isContentReady = true
        }

        // Construir la lista de permisos según la versión de Android
        val permissionsToRequest = mutableListOf<String>().apply {
            add(Manifest.permission.READ_PHONE_STATE)
            // En Android 13+ (API 33+) se usa READ_MEDIA_IMAGES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            // IMPORTANTE: En Android 16+ (API 36+), READ_EXTERNAL_STORAGE es REQUERIDO explícitamente
            // para acceder al wallpaper, incluso si se tiene READ_MEDIA_IMAGES
            // Por lo tanto, siempre lo solicitamos para garantizar compatibilidad
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.READ_CALENDAR)
            add(Manifest.permission.PACKAGE_USAGE_STATS)
        }
        
        requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        batteryReceiver?.let { registerReceiver(it, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }

        setupNetworkCallback()
        startService(Intent(this, NotificationService::class.java))
        registerReceiver(notificationReceiver, IntentFilter(NotificationService.ACTION_UPDATE), RECEIVER_NOT_EXPORTED)
        registerReceiver(appsUpdateReceiver, IntentFilter(CustomAppInfoManager.ACTION_APPS_UPDATED), RECEIVER_NOT_EXPORTED)


        setContent {
            S60LauncherTheme {
                NokiaHomeScreen(
                    carrierName = if (isWifiConnected) wifiSsid else carrierName,
                    wallpaperDrawable = wallpaperDrawable,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    signalLevel = signalLevel,
                    networkType = networkType,
                    isWifiConnected = isWifiConnected,
                    wifiSignalLevel = wifiSignalLevel,
                    isHomeScreen = true,
                    apps = apps,
                    appDrawerFocusedIndex = appDrawerFocusedIndex,
                    setAppDrawerFocusedIndex = { appDrawerFocusedIndex = it },
                    optionsMenuFocusedIndex = optionsMenuFocusedIndex,
                    setOptionsMenuFocusedIndex = { optionsMenuFocusedIndex = it },
                    hasWindowFocus = hasWindowFocus,
                    navigateToHome = navigateToHome,
                    onNavigateToHome = { navigateToHome = false },
                    calendarEvents = calendarEvents,
                    notifications = notifications,
                    onLaunchImagePicker = { packageName: String, callback: (String?) -> Unit ->
                        packageToEdit = packageName
                        imagePickerCallback = callback
                        imagePickerLauncher.launch("image/*")
                    },
                    isContentReady = isContentReady
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        try {
            unregisterReceiver(appsUpdateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        try {
            batteryReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    private fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val showHiddenApps = CustomAppInfoManager.getShowHiddenApps(context)
        val sortOrder = CustomAppInfoManager.getSortOrder(context)

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, cal.timeInMillis, System.currentTimeMillis())

        val appList = packageManager.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            if (CustomAppInfoManager.isAppHidden(context, packageName) && !showHiddenApps) {
                return@mapNotNull null
            }
            val originalName = resolveInfo.loadLabel(packageManager).toString()
            val customName = CustomAppInfoManager.getCustomName(context, packageName)
            val customIconUri = CustomAppInfoManager.getCustomIconUri(context, packageName)?.toUri()
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val installTime = packageInfo.firstInstallTime
            val lastUsedTime = usageStats.find { it.packageName == packageName }?.lastTimeUsed ?: 0

            val icon = if (customIconUri != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, customIconUri)
                        val bitmap = ImageDecoder.decodeBitmap(source)
                        BitmapDrawable(resources, bitmap)
                    } else {
                        @Suppress("DEPRECATION")
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, customIconUri)
                        BitmapDrawable(resources, bitmap)
                    }
                } catch (_: Exception) {
                    resolveInfo.loadIcon(packageManager)
                }
            } else {
                resolveInfo.loadIcon(packageManager)
            }

            AppInfo(
                appName = customName ?: originalName,
                packageName = packageName,
                icon = icon,
                installTime = installTime,
                lastUsedTime = lastUsedTime
            )
        }

        return when (sortOrder) {
            CustomAppInfoManager.SORT_A_Z -> appList.sortedBy { it.appName.lowercase() }
            CustomAppInfoManager.SORT_Z_A -> appList.sortedByDescending { it.appName.lowercase() }
            CustomAppInfoManager.SORT_INSTALL_DATE -> appList.sortedByDescending { it.installTime }
            CustomAppInfoManager.SORT_RECENTLY_USED -> appList.sortedByDescending { it.lastUsedTime }
            else -> appList.sortedBy { it.appName.lowercase() }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Establecer el intent actual para evitar que se cree una nueva instancia
        setIntent(intent)
        // Solo navegar al home si el intent es ACTION_MAIN
        // Con singleTask, esto debería reutilizar la instancia existente
        if (intent?.action == Intent.ACTION_MAIN) {
            navigateToHome = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Asegurarse de que el intent actual esté configurado correctamente
        // para evitar que se cree una nueva instancia
        intent?.let {
            if (it.action == Intent.ACTION_MAIN) {
                setIntent(it)
            }
        }
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasWindowFocus = hasFocus
        if (hasFocus) {
            hideSystemUI()
            // Configurar la apariencia de las barras del sistema cuando la ventana tiene foco
            configureSystemBarsAppearance()
        }
    }

    private fun configureSystemBarsAppearance() {
        // Configurar la apariencia de las barras del sistema de forma segura
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                window.insetsController?.setSystemBarsAppearance(
                    0,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } catch (e: Exception) {
                // Si falla, usar WindowCompat como alternativa
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        } else {
            // Para versiones anteriores, usar WindowCompat
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    private fun hideSystemUI() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Ocultar la barra de estado de forma global
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        // Mantener la barra de navegación visible pero transparente
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupNetworkCallback() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                isWifiConnected = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                if (isWifiConnected) {
                    wifiSignalLevel = getWifiSignalLevel()
                    wifiSsid = fetchWifiSsid()
                } else {
                    carrierName = fetchCarrierName()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isWifiConnected = false
                carrierName = fetchCarrierName()
            }
        }
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun fetchWifiSsid(): String {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo.ssid.replace("\"", "")
        return if (ssid == "<unknown ssid>") "Internet" else ssid
    }

    internal fun getWifiSignalLevel(): Int {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
    }

    @Suppress("DEPRECATION")
    private fun listenToSignalStrength() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val phoneStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            @RequiresApi(Build.VERSION_CODES.N)
            @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                super.onSignalStrengthsChanged(signalStrength)
                signalLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    (signalStrength.level / 4.0 * 7).toInt().coerceIn(0, 7)
                } else {
                    calculateSignalLevelPreApi29(signalStrength)
                }
                networkType = getNetworkTypeString(telephonyManager.dataNetworkType)
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    private fun calculateSignalLevelPreApi29(signalStrength: SignalStrength): Int {
        if (signalStrength.isGsm) {
            val asu = signalStrength.gsmSignalStrength
            return when {
                asu >= 27 -> 7
                asu >= 22 -> 6
                asu >= 17 -> 5
                asu >= 13 -> 4
                asu >= 9 -> 3
                asu >= 5 -> 2
                asu >= 2 -> 1
                else -> 0
            }
        }
        return 0
    }

    private fun getNetworkTypeString(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> ""
        }
    }

    private fun fetchCarrierName(): String {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.simOperatorName ?: "No SIM"
    }

    private fun getCalendarEvents(context: Context): List<EventInfo> {
        val eventList = mutableListOf<EventInfo>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(CalendarContract.Events.TITLE)

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        tomorrow.set(Calendar.HOUR_OF_DAY, 0)
        tomorrow.set(Calendar.MINUTE, 0)
        tomorrow.set(Calendar.SECOND, 0)

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
        val selectionArgs = arrayOf(today.timeInMillis.toString(), tomorrow.timeInMillis.toString())

        val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                    eventList.add(EventInfo(title))
                } while (it.moveToNext())
            }
        }
        return eventList
    }
}


@Composable
fun DottedLine() {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect
        )
    }
}

@Composable
fun WallpaperBackground(wallpaperDrawable: Drawable?) {
    // Convertir el drawable a bitmap de forma segura fuera de la composición
    val wallpaperBitmap = remember(wallpaperDrawable) {
        when (wallpaperDrawable) {
            is BitmapDrawable -> {
                wallpaperDrawable.bitmap?.asImageBitmap()
            }
            is ColorDrawable -> {
                null // Se manejará como color
            }
            null -> {
                null // Se manejará como fallback
            }
            else -> {
                // Intentar convertir cualquier otro tipo de Drawable a Bitmap
                try {
                    wallpaperDrawable.toBitmap().asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    val wallpaperColor = remember(wallpaperDrawable) {
        when (wallpaperDrawable) {
            is ColorDrawable -> Color(wallpaperDrawable.color)
            else -> null
        }
    }
    
    when {
        wallpaperBitmap != null -> {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        wallpaperColor != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(wallpaperColor)
            )
        }
        else -> {
            // Sin fallback: si no hay wallpaper, mostrar fondo transparente
            // El wallpaper se cargará automáticamente cuando esté disponible
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun NokiaHomeScreen(
    carrierName: String,
    wallpaperDrawable: Drawable?,
    batteryLevel: Int,
    isCharging: Boolean,
    signalLevel: Int,
    networkType: String,
    isWifiConnected: Boolean,
    wifiSignalLevel: Int,
    isHomeScreen: Boolean,
    apps: List<AppInfo>,
    appDrawerFocusedIndex: Int,
    setAppDrawerFocusedIndex: (Int) -> Unit,
    optionsMenuFocusedIndex: Int,
    setOptionsMenuFocusedIndex: (Int) -> Unit,
    hasWindowFocus: Boolean,
    navigateToHome: Boolean,
    onNavigateToHome: () -> Unit,
    calendarEvents: List<EventInfo>,
    notifications: List<NotificationInfo>,
    onLaunchImagePicker: (String, (String?) -> Unit) -> Unit,
    isContentReady: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val context = LocalContext.current
    val appDrawerState = rememberLazyGridState()
    
    // Rastrear si acabamos de procesar BACK desde un submenú para evitar doble pop
    // Este flag se usa para evitar que MainActivity maneje BACK cuando el componente
    // ya lo manejó y cambió la ruta de un submenú a AppDrawer
    var backFromSubMenu by remember { mutableStateOf(false) }
    
    // Resetear el flag cuando cambia la ruta (después de que se procesó el evento)
    LaunchedEffect(currentRoute) {
        // Resetear el flag después de un pequeño delay para asegurar que el evento se procesó
        kotlinx.coroutines.delay(50)
        if (backFromSubMenu) {
            backFromSubMenu = false
        }
    }
    
    // Animación fade-in para el contenido
    val contentAlpha by animateFloatAsState(
        targetValue = if (isContentReady) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "contentFade"
    )

    LaunchedEffect(navigateToHome) {
        if (navigateToHome) {
            // Solo navegar al home si no estamos ya ahí
            if (currentRoute != "home") {
                navController.navigate("home") { 
                    popUpTo(navController.graph.startDestinationId) { inclusive = true } 
                }
            }
            onNavigateToHome()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            calendar = Calendar.getInstance()
            delay(1000)
        }
    }

    val openAppDrawer = remember(navController) {
        { navController.navigate("app_drawer") }
    }

    val goBack = remember(navController) {
        { navController.popBackStack() }
    }

    val launchApp = remember(context) {
        { packageName: String ->
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let { context.startActivity(it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().onKeyEvent { event ->
        if (!hasWindowFocus) return@onKeyEvent false

        val isHome = currentRoute == "home"
        val isAppDrawer = currentRoute == "app_drawer"
        val isOptionsMenu = currentRoute.startsWith("options_menu")
        val isActionsMenu = currentRoute.startsWith("actions_menu")
        val isEditMenu = currentRoute.startsWith("edit_menu")
        val isAddToMenu = currentRoute.startsWith("add_to_menu")
        val isSortMenu = currentRoute == "sort_menu"
        val isClockStyleMenu = currentRoute == "clock_style_menu"
        val isAppearanceMenu = currentRoute == "appearance_menu"
        val isSubMenu = isOptionsMenu || isActionsMenu || isEditMenu || isAddToMenu || isSortMenu || isClockStyleMenu || isAppearanceMenu

        // Manejar long press de KEYCODE_MENU (KeyDown con isLongPress)
        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.isLongPress) {
            when (event.nativeKeyEvent.keyCode) {
                AndroidKeyEvent.KEYCODE_MENU -> {
                    if (isAppDrawer) {
                        // Al mantener presionada la tecla MENU, abrir el menú genérico
                        navController.navigate("options_menu")
                        return@onKeyEvent true
                    }
                }
            }
        }

        // Manejar presión normal (KeyUp sin long press)
        if (event.type == KeyEventType.KeyUp && !event.nativeKeyEvent.isLongPress) {
            when (event.nativeKeyEvent.keyCode) {
                AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                    if (isHome) {
                        openAppDrawer()
                        return@onKeyEvent true
                    }
                }
                AndroidKeyEvent.KEYCODE_MENU -> {
                    when {
                        isHome -> {
                            SoftKeysManager.getSoftLeft(context)?.let { launchApp(it) }
                        }
                        isAppDrawer -> {
                            // Al presionar y soltar rápidamente, abrir el menú de la app enfocada
                            val focusedApp = apps.getOrNull(appDrawerFocusedIndex)
                            if (focusedApp != null) {
                                navController.navigate("options_menu/${focusedApp.packageName}")
                            } else {
                                navController.navigate("options_menu")
                            }
                        }
                        isSubMenu -> { /* Do nothing */ }
                        else -> SoftKeysManager.getSoftLeft(context)?.let { launchApp(it) }
                    }
                    return@onKeyEvent true
                }
                AndroidKeyEvent.KEYCODE_BACK -> {
                    // Si estamos en un submenú, marcar que estamos procesando BACK desde un submenú
                    // y dejar que los componentes lo manejen
                    if (isSubMenu) {
                        backFromSubMenu = true
                        return@onKeyEvent false
                    }
                    
                    // Si acabamos de procesar BACK desde un submenú y ahora estamos en AppDrawer,
                    // no manejar BACK aquí para evitar doble pop
                    if (isAppDrawer && backFromSubMenu) {
                        // El componente ya manejó el BACK y hizo popBackStack(),
                        // así que no hacer nada más aquí
                        return@onKeyEvent false
                    }
                    
                    when {
                        isHome -> {
                            SoftKeysManager.getSoftRight(context)?.let { launchApp(it) }
                            return@onKeyEvent true
                        }
                        isAppDrawer -> {
                            goBack()
                            return@onKeyEvent true
                        }
                        else -> {
                            SoftKeysManager.getSoftRight(context)?.let { launchApp(it) }
                            return@onKeyEvent true
                        }
                    }
                }
            }
        }
        false
    }) {
        // El wallpaper ocupa toda la pantalla, incluyendo el área del notch
        WallpaperBackground(wallpaperDrawable)
        
        // El contenido respeta los márgenes del notch y las barras del sistema
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    // Combinar statusBars y displayCutout para respetar el notch
                    WindowInsets.statusBars.union(WindowInsets.displayCutout)
                )
        ) {
            // StatusBar sin padding para que los fondos lleguen hasta los bordes
            StatusBar(
                calendar,
                carrierName,
                batteryLevel,
                isCharging,
                signalLevel,
                networkType,
                isWifiConnected,
                wifiSignalLevel,
                isHomeScreen
            )
            // Contenido con padding
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .alpha(contentAlpha)
            ) {
                NavHost(navController = navController, startDestination = "home", modifier = Modifier.weight(1f)) {
                    composable("home") {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            DottedLine()
                            Spacer(modifier = Modifier.height(4.dp))
                            Shortcuts(hasWindowFocus, apps)
                            Spacer(modifier = Modifier.height(4.dp))
                            DottedLine()
                            Spacer(modifier = Modifier.height(8.dp))
                            CalendarNotification(calendarEvents)
                            if (notifications.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                DottedLine()
                                NotificationsList(notifications)
                            }
                        }
                    }
                    composable("app_drawer") { AppDrawer(navController, apps, appDrawerFocusedIndex, setAppDrawerFocusedIndex, appDrawerState) }
                    composable("options_menu") { OptionsMenu(navController, null, optionsMenuFocusedIndex, setOptionsMenuFocusedIndex) }
                    composable(
                        "options_menu/{packageName}",
                        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        OptionsMenu(
                            navController = navController,
                            packageName = backStackEntry.arguments?.getString("packageName"),
                            focusedIndex = optionsMenuFocusedIndex,
                            setFocusedIndex = setOptionsMenuFocusedIndex
                        )
                    }
                    composable(
                        "actions_menu/{packageName}",
                        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            ActionsMenu(
                                navController = navController,
                                packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                            )
                        }
                    }
                    composable(
                        "edit_menu/{packageName}",
                        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        EditMenu(
                            navController = navController,
                            packageName = backStackEntry.arguments?.getString("packageName"),
                            onLaunchImagePicker = onLaunchImagePicker
                        )
                    }
                    composable(
                        "add_to_menu/{packageName}",
                        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        AddToMenu(
                            navController = navController,
                            packageName = backStackEntry.arguments?.getString("packageName")
                        )
                    }
                    composable("sort_menu") { SortMenu(navController) }
                    composable("clock_style_menu") { ClockStyleMenu(navController) }
                    composable("appearance_menu") { AppearanceMenu(navController) }
                    composable("menu_style_menu") { MenuStyleMenu(navController) }
                    composable("scroll_indicator_style_menu") { ScrollIndicatorStyleMenu(navController) }
                    composable("app_theme_menu") { AppThemeMenu(navController) }
                    composable("theme_settings_menu") { ThemeSettingsMenu(navController) }
                }
            }
            NavBar(
                navController = navController,
                currentRoute = currentRoute,
                modifier = Modifier.alpha(contentAlpha)
            )
        }
    }
}

@Composable
fun Shortcuts(hasWindowFocus: Boolean, apps: List<AppInfo>) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val packageManager = context.packageManager
    val coroutineScope = rememberCoroutineScope()
    
    // Crear un mapa para búsqueda rápida de apps por packageName
    val appsByPackageName = apps.associateBy { it.packageName }
    // Mantener el orden de fijación al construir la lista de apps fijadas
    val pinnedApps = PinnedAppsManager.pinnedApps
        .mapNotNull { pinnedApp -> appsByPackageName[pinnedApp.packageName] }

    val focusRequesters = remember(pinnedApps.size) { List(pinnedApps.size) { FocusRequester() } }
    val hasPhysicalKeyboard = remember {
        KeyboardManager.hasPhysicalKeyboard(context)
    }
    
    // Estado del modo de edición - rastrear por packageName para que el highlight se mueva con la app
    var editingPackageName by remember { mutableStateOf<String?>(null) }
    val isEditingMode = editingPackageName != null
    
    // Obtener el color de énfasis del tema
    val theme = ThemeManager.rememberTheme(context)
    val accentColor = if (theme.accentColor != Color.Unspecified) {
        theme.accentColor
    } else {
        theme.focusColor.copy(alpha = theme.focusOpacity)
    }
    
    val lazyListState = rememberLazyListState()
    val itemWidth = with(density) { 68.dp.toPx() } // 60dp + 8dp spacing
    
    // Función helper para encontrar el índice actual de una app por su packageName
    fun findAppIndex(packageName: String?): Int {
        return if (packageName != null) {
            PinnedAppsManager.pinnedApps.indexOfFirst { it.packageName == packageName }
        } else {
            -1
        }
    }
    
    // Función helper para actualizar el focus y hacer scroll si es necesario
    fun updateFocusAndScroll(packageName: String?) {
        val appIndex = findAppIndex(packageName)
        if (appIndex >= 0 && appIndex < focusRequesters.size) {
            coroutineScope.launch {
                delay(100) // Delay para que la UI se actualice después de mover la app
                
                // Verificar nuevamente el índice después del delay (por si cambió)
                val currentIndex = findAppIndex(packageName)
                if (currentIndex >= 0 && currentIndex < focusRequesters.size) {
                    focusRequesters[currentIndex].requestFocus()
                    
                    // Obtener layoutInfo después del delay para asegurar que está actualizado
                    val layoutInfo = lazyListState.layoutInfo
                    val isVisible = layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
                    if (!isVisible) {
                        try {
                            lazyListState.animateScrollToItem(currentIndex)
                        } catch (e: Exception) {
                            // Si falla, hacer scroll suave
                            val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                            if (currentIndex < firstVisibleIndex) {
                                lazyListState.animateScrollBy(-itemWidth, animationSpec = tween(150))
                            } else {
                                lazyListState.animateScrollBy(itemWidth, animationSpec = tween(150))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Función helper para mover la app en modo edición
    fun moveAppInEditMode(direction: Int) {
        val currentIndex = findAppIndex(editingPackageName)
        if (currentIndex >= 0) {
            val newIndex = (currentIndex + direction).coerceIn(0, PinnedAppsManager.pinnedApps.size - 1)
            if (newIndex != currentIndex) {
                PinnedAppsManager.moveApp(context, currentIndex, newIndex)
                updateFocusAndScroll(editingPackageName)
            }
        }
    }

    // Inicializar focus cuando no estamos en modo edición
    LaunchedEffect(hasWindowFocus, pinnedApps.size) {
        if (hasWindowFocus && focusRequesters.isNotEmpty() && hasPhysicalKeyboard && !isEditingMode) {
            focusRequesters[0].requestFocus()
        }
    }
    
    // Mantener el focus en la app que se está editando
    LaunchedEffect(editingPackageName, pinnedApps.size) {
        if (isEditingMode && editingPackageName != null && hasPhysicalKeyboard) {
            updateFocusAndScroll(editingPackageName)
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        itemsIndexed(pinnedApps) { index, app ->
            val isSelectedForEditing = isEditingMode && editingPackageName == app.packageName
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .appItemWithFocus(
                        focusRequester = focusRequesters[index],
                        onKeyEvent = { event ->
                            // Bloquear TODOS los eventos de otras apps cuando estamos en modo edición
                            if (isEditingMode && editingPackageName != app.packageName) {
                                true
                            } else if (isSelectedForEditing) {
                                // Si estamos en modo edición con esta app, manejar eventos especiales
                                when {
                                    // Long press: ya estamos en modo edición, no hacer nada
                                    event.type == KeyEventType.KeyDown && 
                                    event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER &&
                                    event.nativeKeyEvent.isLongPress -> {
                                        true // Ya estamos en modo edición
                                    }
                                    
                                    // Botón central KeyDown en modo edición: esperar KeyUp para salir
                                    event.type == KeyEventType.KeyDown &&
                                    event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                        true // Consumir para evitar lanzar app
                                    }
                                    
                                    // Botón central KeyUp en modo edición: salir del modo edición
                                    event.type == KeyEventType.KeyUp &&
                                    event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                        editingPackageName = null
                                        true
                                    }
                                    
                                    // Flecha izquierda: mover app a la izquierda
                                    event.type == KeyEventType.KeyDown &&
                                    event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                        moveAppInEditMode(-1)
                                        true
                                    }
                                    
                                    // Flecha derecha: mover app a la derecha
                                    event.type == KeyEventType.KeyDown &&
                                    event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        moveAppInEditMode(1)
                                        true
                                    }
                                    
                                    else -> false
                                }
                            } else {
                                // No estamos en modo edición
                                when {
                                    // Long press: entrar en modo edición
                                    event.type == KeyEventType.KeyDown && 
                                    event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER &&
                                    event.nativeKeyEvent.isLongPress -> {
                                        editingPackageName = app.packageName
                                        true
                                    }
                                    
                                    // KeyUp del botón central sin modo edición: lanzar app
                                    event.type == KeyEventType.KeyUp &&
                                    event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                                        intent?.let { context.startActivity(it) }
                                        true
                                    }
                                    
                                    else -> false
                                }
                            }
                        }
                    ).first
                    .pointerInput(index, isEditingMode, editingPackageName, app.packageName) {
                        if (isSelectedForEditing) {
                            // Modo edición: permitir arrastrar
                            var dragStartIndex = findAppIndex(editingPackageName).takeIf { it >= 0 } ?: index
                            var accumulatedDrag = 0f
                            
                            detectDragGestures(
                                onDragStart = { 
                                    dragStartIndex = findAppIndex(editingPackageName).takeIf { it >= 0 } ?: index
                                    accumulatedDrag = 0f
                                },
                                onDragEnd = { 
                                    accumulatedDrag = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount.x
                                    val dragItemWidth = with(density) { 68.dp.toPx() }
                                    val itemsToMove = (accumulatedDrag / dragItemWidth).toInt()
                                    
                                    if (itemsToMove != 0) {
                                        val currentIndex = findAppIndex(editingPackageName)
                                        if (currentIndex >= 0) {
                                            val newIndex = (dragStartIndex + itemsToMove).coerceIn(0, PinnedAppsManager.pinnedApps.size - 1)
                                            if (newIndex != currentIndex) {
                                                PinnedAppsManager.moveApp(context, currentIndex, newIndex)
                                                updateFocusAndScroll(editingPackageName)
                                                accumulatedDrag -= itemsToMove * dragItemWidth
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        
                        // Detectar tap y long press
                        detectTapGestures(
                            onLongPress = {
                                // Bloquear si ya estamos editando otra app
                                if (isEditingMode && editingPackageName != app.packageName) {
                                    return@detectTapGestures
                                }
                                // Entrar en modo edición
                                editingPackageName = app.packageName
                            },
                            onTap = {
                                // Bloquear si estamos editando otra app
                                if (isEditingMode && editingPackageName != app.packageName) {
                                    return@detectTapGestures
                                }
                                
                                if (isSelectedForEditing) {
                                    // Salir del modo edición
                                    editingPackageName = null
                                } else if (!isEditingMode) {
                                    // Lanzar app normalmente
                                    val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                                    intent?.let { context.startActivity(it) }
                                }
                            }
                        )
                    }
            ) {
                // TEMPORALMENTE: Flechas comentadas para probar si causan problemas de enfoque
                // Mostrar flechas si está en modo de edición y es la app seleccionada
                if (isSelectedForEditing) {
                    // val currentAppIndex = findAppIndex(editingPackageName)
                    // Row(
                    //     modifier = Modifier.fillMaxSize(),
                    //     horizontalArrangement = Arrangement.SpaceBetween,
                    //     verticalAlignment = Alignment.CenterVertically
                    // ) {
                    //     // Flecha izquierda
                    //     if (currentAppIndex > 0) {
                    //         Icon(
                    //             painter = painterResource(id = R.drawable.arrow_left),
                    //             contentDescription = null,
                    //             tint = accentColor,
                    //             modifier = Modifier.size(24.dp)
                    //         )
                    //     } else {
                    //         Spacer(modifier = Modifier.size(24.dp))
                    //     }
                    //     
                    //     // Icono de la app
                    //     Image(
                    //         bitmap = app.icon.toBitmap().asImageBitmap(),
                    //         contentDescription = app.appName,
                    //         modifier = Modifier.size(52.dp)
                    //     )
                    //     
                    //     // Flecha derecha
                    //     if (currentAppIndex >= 0 && currentAppIndex < pinnedApps.size - 1) {
                    //         Icon(
                    //             painter = painterResource(id = R.drawable.arrow_right),
                    //             contentDescription = null,
                    //             tint = accentColor,
                    //             modifier = Modifier.size(24.dp)
                    //         )
                    //     } else {
                    //         Spacer(modifier = Modifier.size(24.dp))
                    //     }
                    // }
                    // Mostrar solo el icono temporalmente (sin flechas)
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier.size(52.dp)
                    )
                } else {
                    // Mostrar solo el icono si no está en modo de edición
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarNotification(events: List<EventInfo>) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Calendar Icon",
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = when {
                events.isEmpty() -> stringResource(R.string.events_none)
                events.size == 1 -> events[0].title
                else -> "${events.size} " + stringResource(R.string.events_multiple)
            },
            color = contentColor
        )
    }
}

@Composable
fun NotificationsList(notifications: List<NotificationInfo>) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        itemsIndexed(notifications) { index, notification ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        bitmap = notification.icon.toBitmap().asImageBitmap(),
                        contentDescription = notification.appName,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "${notification.count} notifications from ${notification.appName}",
                        color = contentColor
                    )
                }
                if (index < notifications.lastIndex) {
                    DottedLine()
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 240, heightDp = 320)
@Composable
fun NokiaHomeScreenPreview() {
    S60LauncherTheme {
        NokiaHomeScreen(
            "VINAPHONE",
            null,
            80,
            isCharging = false,
            4,
            "4G",
            isWifiConnected = true,
            wifiSignalLevel = 3,
            isHomeScreen = true,
            apps = emptyList(),
            appDrawerFocusedIndex = 0,
            setAppDrawerFocusedIndex = {},
            optionsMenuFocusedIndex = 0,
            setOptionsMenuFocusedIndex = {},
            hasWindowFocus = true,
            navigateToHome = false,
            onNavigateToHome = {},
            calendarEvents = listOf(EventInfo("Team Meeting")),
            notifications = emptyList(),
            onLaunchImagePicker = { _: String, _: (String?) -> Unit -> },
            isContentReady = true
        )
    }
}
