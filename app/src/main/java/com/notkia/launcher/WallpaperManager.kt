package com.notkia.launcher

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException

/**
 * Gestor de wallpaper del sistema compatible con Android 5 (API 21) hasta Android 16 (API 36)
 * 
 * Este objeto maneja la obtención del wallpaper del sistema usando diferentes métodos
 * según la versión de Android, garantizando compatibilidad en todas las versiones.
 */
object WallpaperManager {
    val isDarkTheme = mutableStateOf(false)

    /**
     * Convierte un Drawable a BitmapDrawable de forma segura
     */
    private fun drawableToBitmapDrawable(drawable: Drawable?, context: Context): BitmapDrawable? {
        if (drawable == null) return null
        
        return try {
            // Si ya es BitmapDrawable y tiene bitmap válido, retornarlo directamente
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable
            }
            
            // Intentar convertir usando la extensión de AndroidX
            val bitmap = drawable.toBitmap()
            if (bitmap != null && !bitmap.isRecycled) {
                return BitmapDrawable(context.resources, bitmap)
            }
            
            // Método alternativo: crear bitmap manualmente desde el drawable
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            
            // Si no tiene dimensiones intrínsecas, usar dimensiones por defecto
            val finalWidth = if (width <= 0 || width == Int.MAX_VALUE) 1920 else width
            val finalHeight = if (height <= 0 || height == Int.MAX_VALUE) 1080 else height
            
            val bitmap2 = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap2)
            drawable.setBounds(0, 0, finalWidth, finalHeight)
            drawable.draw(canvas)
            BitmapDrawable(context.resources, bitmap2)
        } catch (e: Exception) {
            Log.e("WallpaperManager", "Error converting drawable to bitmap", e)
            null
        }
    }

    /**
     * Verifica si la app es el launcher predeterminado
     * Usa múltiples métodos para verificar esto de forma más confiable
     */
    private fun isDefaultLauncher(context: Context): Boolean {
        return try {
            // Método 1: Verificar el launcher predeterminado
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val isDefault = resolveInfo?.activityInfo?.packageName == context.packageName
            
            // Método 2: Verificar si la app tiene la categoría HOME en el manifest
            // Esto puede ayudar a identificar launchers incluso si no están configurados como predeterminados
            val hasHomeCategory = try {
                val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                activities.any { it.activityInfo.packageName == context.packageName }
            } catch (e: Exception) {
                false
            }
            
            val result = isDefault || hasHomeCategory
            Log.d("WallpaperManager", "isDefaultLauncher check: isDefault=$isDefault, hasHomeCategory=$hasHomeCategory, result=$result")
            result
        } catch (e: Exception) {
            Log.w("WallpaperManager", "Error checking if app is default launcher", e)
            false
        }
    }

    /**
     * Verifica si la app tiene permisos para acceder al wallpaper
     * 
     * En Android 16+ (API 36+), el sistema requiere READ_EXTERNAL_STORAGE incluso si se tiene READ_MEDIA_IMAGES
     */
    private fun hasWallpaperPermission(context: Context): Boolean {
        val isDefault = isDefaultLauncher(context)
        
        // Los launchers predeterminados tienen acceso automático al wallpaper
        if (isDefault) {
            Log.d("WallpaperManager", "App is default launcher, has automatic wallpaper access")
            return true
        }
        
        // Verificar permisos según la versión de Android
        val hasMediaImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        
        val hasExternalStorage = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        // En Android 16+ (API 36+), el sistema puede requerir READ_EXTERNAL_STORAGE
        // pero también intentaremos con READ_MEDIA_IMAGES ya que READ_EXTERNAL_STORAGE está deprecado
        val requiresBoth = Build.VERSION.SDK_INT >= 36
        
        val hasPermission = if (requiresBoth) {
            // En Android 16+, intentar con READ_EXTERNAL_STORAGE primero, pero también aceptar READ_MEDIA_IMAGES
            // ya que READ_EXTERNAL_STORAGE puede no estar disponible o funcionar correctamente
            hasExternalStorage || hasMediaImages
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // En Android 13-15, READ_MEDIA_IMAGES es suficiente
            hasMediaImages || hasExternalStorage
        } else {
            // En versiones anteriores, READ_EXTERNAL_STORAGE es suficiente
            hasExternalStorage
        }
        
        Log.d("WallpaperManager", "Permission check: READ_MEDIA_IMAGES=$hasMediaImages, READ_EXTERNAL_STORAGE=$hasExternalStorage, SDK=${Build.VERSION.SDK_INT}, requiresBoth=$requiresBoth, result=$hasPermission")
        
        return hasPermission
    }

    /**
     * Obtiene el wallpaper usando getWallpaperFile (Android N+ / API 24+)
     */
    private fun getWallpaperFromFile(wallpaperManager: WallpaperManager, context: Context, flag: Int): BitmapDrawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return null
        }
        
        return try {
            val wallpaperFile = wallpaperManager.getWallpaperFile(flag)
            wallpaperFile?.let { file ->
                var inputStream: FileInputStream? = null
                try {
                    // file.fileDescriptor devuelve FileDescriptor, no ParcelFileDescriptor
                    inputStream = FileInputStream(file.fileDescriptor)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    bitmap?.let { bmp ->
                        if (!bmp.isRecycled) {
                            Log.d("WallpaperManager", "Wallpaper loaded from getWallpaperFile with flag $flag")
                            return BitmapDrawable(context.resources, bmp)
                        }
                    }
                } catch (e: IOException) {
                    Log.w("WallpaperManager", "IOException reading wallpaper file with flag $flag", e)
                } catch (e: SecurityException) {
                    Log.w("WallpaperManager", "SecurityException reading wallpaper file with flag $flag", e)
                } catch (e: Exception) {
                    Log.e("WallpaperManager", "Exception reading wallpaper file with flag $flag", e)
                } finally {
                    try {
                        inputStream?.close()
                        file.close() // Cerrar el ParcelFileDescriptor
                    } catch (e: Exception) {
                        // Ignorar errores al cerrar
                    }
                }
            }
            null
        } catch (e: SecurityException) {
            Log.w("WallpaperManager", "SecurityException getting wallpaper file with flag $flag", e)
            null
        } catch (e: Exception) {
            Log.e("WallpaperManager", "Exception getting wallpaper file with flag $flag", e)
            null
        }
    }

    /**
     * Obtiene el wallpaper usando peekDrawable (Android 5+ / API 21+)
     */
    private fun getWallpaperFromPeekDrawable(wallpaperManager: WallpaperManager, context: Context): Drawable? {
        return try {
            val peekDrawable = wallpaperManager.peekDrawable()
            if (peekDrawable != null) {
                Log.d("WallpaperManager", "Wallpaper loaded from peekDrawable")
                return drawableToBitmapDrawable(peekDrawable, context) ?: peekDrawable
            }
            null
        } catch (e: SecurityException) {
            Log.w("WallpaperManager", "SecurityException in peekDrawable", e)
            null
        } catch (e: Exception) {
            Log.e("WallpaperManager", "Exception in peekDrawable", e)
            null
        }
    }

    /**
     * Obtiene el wallpaper usando la propiedad drawable (Android 5+ / API 21+)
     */
    private fun getWallpaperFromDrawable(wallpaperManager: WallpaperManager, context: Context): Drawable? {
        return try {
            val drawable = wallpaperManager.drawable
            if (drawable != null) {
                Log.d("WallpaperManager", "Wallpaper loaded from drawable property")
                return drawableToBitmapDrawable(drawable, context) ?: drawable
            }
            null
        } catch (e: SecurityException) {
            Log.w("WallpaperManager", "SecurityException getting drawable", e)
            null
        } catch (e: Exception) {
            Log.e("WallpaperManager", "Exception getting drawable", e)
            null
        }
    }

    /**
     * Obtiene el wallpaper usando getDrawable (método obsoleto pero funcional en versiones antiguas)
     */
    @Suppress("DEPRECATION")
    private fun getWallpaperFromGetDrawable(wallpaperManager: WallpaperManager, context: Context): Drawable? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return null // Este método está deprecado en N+, usar otros métodos
        }
        
        return try {
            val drawable = wallpaperManager.getDrawable()
            if (drawable != null) {
                Log.d("WallpaperManager", "Wallpaper loaded from getDrawable (deprecated method)")
                return drawableToBitmapDrawable(drawable, context) ?: drawable
            }
            null
        } catch (e: SecurityException) {
            Log.w("WallpaperManager", "SecurityException in getDrawable", e)
            null
        } catch (e: Exception) {
            Log.e("WallpaperManager", "Exception in getDrawable", e)
            null
        }
    }

    /**
     * Obtiene el wallpaper del sistema usando métodos compatibles con todas las versiones de Android
     * 
     * Estrategia mejorada para versiones modernas:
     * 1. Intentar TODOS los métodos disponibles, incluso sin permisos explícitos
     * 2. En Android N+ (API 24+): Intentar getWallpaperFile con FLAG_SYSTEM y FLAG_LOCK
     * 3. En Android 5+ (API 21+): Intentar peekDrawable() y drawable property (múltiples intentos)
     * 4. En Android 5-6 (API 21-23): Intentar getDrawable() como último recurso
     * 
     * Nota: En versiones modernas, algunos métodos pueden funcionar sin permisos explícitos
     * si la app es el launcher predeterminado o tiene ciertos privilegios del sistema.
     */
    private fun getWallpaperDrawable(context: Context): Drawable? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val isDefault = isDefaultLauncher(context)
            val hasPermission = hasWallpaperPermission(context)
            
            Log.d("WallpaperManager", "Attempting to load wallpaper: isDefault=$isDefault, hasPermission=$hasPermission, SDK=${Build.VERSION.SDK_INT}")
            
            // INTENTAR TODOS LOS MÉTODOS DISPONIBLES
            // En Android 16, el sistema puede requerir READ_EXTERNAL_STORAGE, pero intentamos de todas formas
            // ya que algunos métodos pueden funcionar sin permisos explícitos para launchers
            
            // ESTRATEGIA: Intentar primero métodos que pueden funcionar sin permisos explícitos
            // (peekDrawable y drawable property), luego métodos que requieren permisos
            
            // MÉTODO 1: peekDrawable() (Android 5+ / API 21+)
            // Este método a menudo funciona sin permisos explícitos, especialmente para launchers
            Log.d("WallpaperManager", "Trying peekDrawable() (may work without explicit permissions)...")
            getWallpaperFromPeekDrawable(wallpaperManager, context)?.let {
                Log.i("WallpaperManager", "✓ Wallpaper loaded successfully from peekDrawable()")
                return it
            }
            
            // MÉTODO 2: drawable property (Android 5+ / API 21+)
            // Propiedad directa del WallpaperManager, puede funcionar sin permisos explícitos
            Log.d("WallpaperManager", "Trying drawable property (may work without explicit permissions)...")
            getWallpaperFromDrawable(wallpaperManager, context)?.let {
                Log.i("WallpaperManager", "✓ Wallpaper loaded successfully from drawable property")
                return it
            }
            
            // MÉTODO 3: getWallpaperFile (Android N+ / API 24+)
            // Este método puede requerir permisos explícitos, pero intentamos de todas formas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d("WallpaperManager", "Trying getWallpaperFile(FLAG_SYSTEM)...")
                getWallpaperFromFile(wallpaperManager, context, WallpaperManager.FLAG_SYSTEM)?.let {
                    Log.i("WallpaperManager", "✓ Wallpaper loaded successfully from getWallpaperFile(FLAG_SYSTEM)")
                    return it
                }
                
                Log.d("WallpaperManager", "Trying getWallpaperFile(FLAG_LOCK)...")
                getWallpaperFromFile(wallpaperManager, context, WallpaperManager.FLAG_LOCK)?.let {
                    Log.i("WallpaperManager", "✓ Wallpaper loaded successfully from getWallpaperFile(FLAG_LOCK)")
                    return it
                }
            }
            
            // MÉTODO 4: getDrawable() (Android 5-6 / API 21-23, deprecado)
            // Solo para versiones muy antiguas donde otros métodos fallan
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Log.d("WallpaperManager", "Trying getDrawable() (deprecated)...")
                getWallpaperFromGetDrawable(wallpaperManager, context)?.let {
                    Log.i("WallpaperManager", "✓ Wallpaper loaded successfully from getDrawable()")
                    return it
                }
            }
            
            // Si todos los métodos fallan, intentar peekDrawable() una vez más
            // A veces funciona en el segundo intento en versiones modernas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d("WallpaperManager", "Retrying peekDrawable() as last attempt...")
                getWallpaperFromPeekDrawable(wallpaperManager, context)?.let {
                    Log.i("WallpaperManager", "✓ Wallpaper loaded successfully from peekDrawable() (retry)")
                    return it
                }
            }
            
            Log.e("WallpaperManager", "✗ All methods failed to load wallpaper. isDefault=$isDefault, hasPermission=$hasPermission")
            
            // Información detallada sobre los permisos para ayudar con el debugging
            val hasMediaImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
            val hasExtStorage = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            Log.e("WallpaperManager", "Permission status: READ_MEDIA_IMAGES=$hasMediaImages, READ_EXTERNAL_STORAGE=$hasExtStorage")
            
            // En Android 16+, el sistema puede requerir READ_EXTERNAL_STORAGE, pero está deprecado
            // Intentar sugerir una solución alternativa
            if (Build.VERSION.SDK_INT >= 36) {
                if (!hasExtStorage && hasMediaImages) {
                    Log.w("WallpaperManager", "⚠️ Android 16+ may require READ_EXTERNAL_STORAGE for wallpaper access, but this permission is deprecated. Try granting 'Storage' permission in app settings.")
                } else if (!hasExtStorage && !hasMediaImages) {
                    Log.e("WallpaperManager", "⚠️ No wallpaper permissions granted. Please grant 'Photos and videos' or 'Storage' permission in app settings.")
                }
            }
            
            null
        } catch (e: SecurityException) {
            Log.e("WallpaperManager", "SecurityException getting wallpaper - may need permissions or default launcher status", e)
            null
        } catch (e: Exception) {
            Log.e("WallpaperManager", "Fatal error getting wallpaper", e)
            null
        }
    }

    /**
     * Actualiza el tema (oscuro/claro) basado en el wallpaper del sistema
     */
    fun updateTheme(context: Context) {
        // Ejecutar en un hilo de fondo para no bloquear la UI
        CoroutineScope(Dispatchers.Default).launch {
            val wallpaperDrawable = getWallpaperDrawable(context)

            if (wallpaperDrawable == null) {
                isDarkTheme.value = true // Default to dark theme if wallpaper is not available
                Log.w("WallpaperManager", "Wallpaper not available, defaulting to dark theme")
                return@launch
            }

            try {
                val wallpaperBitmap = wallpaperDrawable.toBitmap()
                
                if (wallpaperBitmap == null || wallpaperBitmap.isRecycled) {
                    isDarkTheme.value = true
                    Log.w("WallpaperManager", "Failed to convert wallpaper to bitmap, defaulting to dark theme")
                    return@launch
                }

                Palette.from(wallpaperBitmap).generate { palette ->
                    val dominantSwatch = palette?.dominantSwatch
                    if (dominantSwatch != null) {
                        val luminance = ColorUtils.calculateLuminance(dominantSwatch.rgb)
                        val isDark = luminance < 0.4
                        isDarkTheme.value = isDark
                        Log.d("WallpaperManager", "Theme updated: isDark=$isDark, luminance=$luminance")
                    } else {
                        isDarkTheme.value = true // Default to dark theme if palette fails
                        Log.w("WallpaperManager", "Palette generation failed, defaulting to dark theme")
                    }
                }
            } catch (e: Exception) {
                // Si falla el procesamiento, usar tema oscuro por defecto
                isDarkTheme.value = true
                Log.e("WallpaperManager", "Error processing wallpaper for theme", e)
            }
        }
    }
    
    /**
     * Obtiene el drawable del wallpaper para usar en la UI
     * 
     * @param context El contexto de la aplicación
     * @return El drawable del wallpaper o null si no se puede obtener
     */
    fun getWallpaperDrawableForUI(context: Context): Drawable? {
        return getWallpaperDrawable(context)
    }
}
