package com.notkia.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.MediaStore

object SoftKeysManager {
    private const val PREFS_NAME = "soft_keys_prefs"
    private const val SOFT_LEFT_KEY = "soft_left_package"
    private const val SOFT_RIGHT_KEY = "soft_right_package"
    private const val DEFAULT_LEFT_PACKAGE = "com.android.settings"


    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSoftLeft(context: Context, packageName: String) {
        getPreferences(context).edit().putString(SOFT_LEFT_KEY, packageName).apply()
    }

    fun getSoftLeft(context: Context): String? {
        return getPreferences(context).getString(SOFT_LEFT_KEY, DEFAULT_LEFT_PACKAGE)
    }

    fun setSoftRight(context: Context, packageName: String) {
        getPreferences(context).edit().putString(SOFT_RIGHT_KEY, packageName).apply()
    }

    fun getSoftRight(context: Context): String? {
        val prefs = getPreferences(context)
        var rightPackage = prefs.getString(SOFT_RIGHT_KEY, null)
        if (rightPackage == null) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            rightPackage = resolveInfo?.activityInfo?.packageName
            rightPackage?.let { setSoftRight(context, it) }
        }
        return rightPackage
    }

    fun clearSoftLeft(context: Context) {
        getPreferences(context).edit().remove(SOFT_LEFT_KEY).apply()
    }

    fun clearSoftRight(context: Context) {
        getPreferences(context).edit().remove(SOFT_RIGHT_KEY).apply()
    }
}
