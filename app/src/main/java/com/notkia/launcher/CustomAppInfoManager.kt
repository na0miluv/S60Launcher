package com.notkia.launcher

import android.content.Context
import android.content.Intent

object CustomAppInfoManager {

    private const val PREFS_NAMES = "CustomAppNames"
    private const val PREFS_ICONS = "CustomAppIcons"
    private const val PREFS_HIDDEN_APPS = "HiddenApps"
    private const val PREFS_SETTINGS = "Settings"
    private const val KEY_SHOW_HIDDEN_APPS = "show_hidden_apps"
    private const val KEY_SORT_ORDER = "sort_order"
    private const val KEY_CLOCK_STYLE = "clock_style"
    const val ACTION_APPS_UPDATED = "com.notkia.launcher.ACTION_APPS_UPDATED"

    const val SORT_A_Z = "a_z"
    const val SORT_Z_A = "z_a"
    const val SORT_INSTALL_DATE = "install_date"
    const val SORT_RECENTLY_USED = "recently_used"

    const val CLOCK_DIGITAL = "digital"
    const val CLOCK_STYLE_1 = "style1"

    const val DRAWER_STYLE_GRID = "grid"
    const val DRAWER_STYLE_LIST = "list"
    private const val KEY_DRAWER_STYLE = "drawer_style"

    const val MENU_STYLE_DEFAULT = "default"
    const val MENU_STYLE_LIST = "list"
    private const val KEY_MENU_STYLE = "menu_style"

    private fun namesPrefs(context: Context) = context.getSharedPreferences(PREFS_NAMES, Context.MODE_PRIVATE)
    private fun iconsPrefs(context: Context) = context.getSharedPreferences(PREFS_ICONS, Context.MODE_PRIVATE)
    private fun hiddenAppsPrefs(context: Context) = context.getSharedPreferences(PREFS_HIDDEN_APPS, Context.MODE_PRIVATE)
    private fun settingsPrefs(context: Context) = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)

    fun getCustomName(context: Context, packageName: String): String? {
        return namesPrefs(context).getString(packageName, null)
    }

    fun setCustomName(context: Context, packageName: String, name: String) {
        namesPrefs(context).edit().putString(packageName, name).apply()
        notifyUpdate(context)
    }

    fun getCustomIconUri(context: Context, packageName: String): String? {
        return iconsPrefs(context).getString(packageName, null)
    }

    fun setCustomIconUri(context: Context, packageName: String, uriString: String) {
        iconsPrefs(context).edit().putString(packageName, uriString).apply()
        notifyUpdate(context)
    }

    fun setAppHidden(context: Context, packageName: String, isHidden: Boolean) {
        hiddenAppsPrefs(context).edit().putBoolean(packageName, isHidden).apply()
        notifyUpdate(context)
    }

    fun isAppHidden(context: Context, packageName: String): Boolean {
        return hiddenAppsPrefs(context).getBoolean(packageName, false)
    }

    fun setShowHiddenApps(context: Context, show: Boolean) {
        settingsPrefs(context).edit().putBoolean(KEY_SHOW_HIDDEN_APPS, show).apply()
        notifyUpdate(context)
    }

    fun getShowHiddenApps(context: Context): Boolean {
        return settingsPrefs(context).getBoolean(KEY_SHOW_HIDDEN_APPS, false)
    }

    fun setSortOrder(context: Context, sortOrder: String) {
        settingsPrefs(context).edit().putString(KEY_SORT_ORDER, sortOrder).apply()
        notifyUpdate(context)
    }

    fun getSortOrder(context: Context): String {
        return settingsPrefs(context).getString(KEY_SORT_ORDER, SORT_A_Z) ?: SORT_A_Z
    }

    fun setClockStyle(context: Context, style: String) {
        settingsPrefs(context).edit().putString(KEY_CLOCK_STYLE, style).apply()
        notifyUpdate(context)
    }

    fun getClockStyle(context: Context): String {
        return settingsPrefs(context).getString(KEY_CLOCK_STYLE, CLOCK_DIGITAL) ?: CLOCK_DIGITAL
    }

    fun setDrawerStyle(context: Context, style: String) {
        settingsPrefs(context).edit().putString(KEY_DRAWER_STYLE, style).apply()
        notifyUpdate(context)
    }

    fun getDrawerStyle(context: Context): String {
        return settingsPrefs(context).getString(KEY_DRAWER_STYLE, DRAWER_STYLE_GRID) ?: DRAWER_STYLE_GRID
    }

    fun setMenuStyle(context: Context, style: String) {
        settingsPrefs(context).edit().putString(KEY_MENU_STYLE, style).apply()
        notifyUpdate(context)
    }

    fun getMenuStyle(context: Context): String {
        return settingsPrefs(context).getString(KEY_MENU_STYLE, MENU_STYLE_DEFAULT) ?: MENU_STYLE_DEFAULT
    }

    fun resetApp(context: Context, packageName: String) {
        namesPrefs(context).edit().remove(packageName).apply()
        iconsPrefs(context).edit().remove(packageName).apply()
        hiddenAppsPrefs(context).edit().remove(packageName).apply()
        notifyUpdate(context)
    }

    private fun notifyUpdate(context: Context) {
        val intent = Intent(ACTION_APPS_UPDATED)
        context.sendBroadcast(intent)
    }
}
