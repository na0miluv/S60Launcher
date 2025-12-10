package com.notkia.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf

data class PinnedAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

object PinnedAppsManager {
    val pinnedApps = mutableStateListOf<PinnedAppInfo>()

    private const val PREFS_NAME = "PinnedAppsPreferences"
    private const val KEY_PINNED_APPS = "pinned_apps"

    private val defaultIntents = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun initializeDefaultApps(context: Context) {
        if (pinnedApps.isNotEmpty()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val packageManager = context.packageManager
        
        // Intentar leer el formato antiguo (StringSet) primero para migración
        var pinnedAppPackagesString: String? = null
        try {
            val pinnedAppPackagesSet = prefs.getStringSet(KEY_PINNED_APPS, null)
            if (pinnedAppPackagesSet != null && pinnedAppPackagesSet.isNotEmpty()) {
                // Migrar del formato antiguo al nuevo: convertir Set a String separado por comas
                pinnedAppPackagesString = pinnedAppPackagesSet.joinToString(",")
                // Guardar en el nuevo formato (esto sobrescribirá el StringSet)
                prefs.edit()
                    .remove(KEY_PINNED_APPS) // Eliminar el StringSet
                    .putString(KEY_PINNED_APPS, pinnedAppPackagesString) // Guardar como String
                    .apply()
            }
        } catch (e: ClassCastException) {
            // No es un StringSet, intentar leer como String
        }
        
        // Si no se migró desde StringSet, intentar leer el nuevo formato (String)
        if (pinnedAppPackagesString == null) {
            pinnedAppPackagesString = prefs.getString(KEY_PINNED_APPS, null)
        }

        if (pinnedAppPackagesString != null && pinnedAppPackagesString.isNotEmpty()) {
            // Cargar manteniendo el orden de fijación
            val pinnedAppPackages = pinnedAppPackagesString.split(",").filter { it.isNotEmpty() }
            pinnedAppPackages.forEach { packageName ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    pinnedApps.add(
                        PinnedAppInfo(
                            packageName = packageName,
                            label = appInfo.loadLabel(packageManager).toString(),
                            icon = appInfo.loadIcon(packageManager)
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // App might have been uninstalled
                }
            }
        } else {
            defaultIntents.forEach { intent ->
                val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolveInfo != null) {
                    val packageName = resolveInfo.activityInfo.packageName
                    if (pinnedApps.none { it.packageName == packageName }) {
                        val appInfo = try {
                            packageManager.getApplicationInfo(packageName, 0)
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                        if (appInfo != null) {
                            pinnedApps.add(
                                PinnedAppInfo(
                                    packageName = packageName,
                                    label = appInfo.loadLabel(packageManager).toString(),
                                    icon = appInfo.loadIcon(packageManager)
                                )
                            )
                        }
                    }
                }
            }
            savePinnedApps(context)
        }
    }

    fun addApp(context: Context, packageName: String) {
        if (pinnedApps.any { it.packageName == packageName }) return

        val packageManager = context.packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            pinnedApps.add(
                PinnedAppInfo(
                    packageName = packageName,
                    label = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager)
                )
            )
            savePinnedApps(context)
        } catch (e: PackageManager.NameNotFoundException) {
            // App not found
        }
    }

    fun removeApp(context: Context, packageName: String) {
        pinnedApps.removeAll { it.packageName == packageName }
        savePinnedApps(context)
    }

    fun moveApp(context: Context, fromIndex: Int, toIndex: Int) {
        if (fromIndex < 0 || fromIndex >= pinnedApps.size || toIndex < 0 || toIndex >= pinnedApps.size) {
            return
        }
        val app = pinnedApps.removeAt(fromIndex)
        pinnedApps.add(toIndex, app)
        savePinnedApps(context)
    }

    private fun savePinnedApps(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // Guardar como lista separada por comas para mantener el orden de fijación
        val packageNames = pinnedApps.map { it.packageName }.joinToString(",")
        editor.putString(KEY_PINNED_APPS, packageNames)
        editor.apply()
    }
}
