package com.notkia.launcher

import android.content.Context
import android.content.res.Configuration
import android.os.Build

object KeyboardManager {
    /**
     * Detecta si el dispositivo tiene un teclado físico integrado.
     * Para dispositivos con teclado físico integrado (como Qin F21 Pro con teclado T9),
     * verificamos múltiples indicadores del sistema.
     * 
     * @param context El contexto de la aplicación
     * @return true si el dispositivo tiene un teclado físico, false en caso contrario
     */
    fun hasPhysicalKeyboard(context: Context): Boolean {
        val config = context.resources.configuration
        val keyboard = config.keyboard
        val hardKeyboardHidden = config.hardKeyboardHidden
        
        // Método 1: Verificar tipo de teclado
        // Si el dispositivo reporta un tipo de teclado que no es "sin teclas" ni "indefinido",
        // entonces tiene un teclado físico
        val hasKeyboardType = keyboard != Configuration.KEYBOARD_NOKEYS && 
                             keyboard != Configuration.KEYBOARD_UNDEFINED
        
        // Método 2: Verificar si el teclado físico está presente (no oculto)
        // Si hardKeyboardHidden == HARDKEYBOARDHIDDEN_NO, significa que hay un teclado físico presente
        val isKeyboardPresent = hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
        
        // Método 3: Verificar el modelo del dispositivo para casos conocidos
        // Algunos dispositivos con teclado físico pueden reportar incorrectamente
        val isKnownPhysicalKeyboardDevice = isKnownPhysicalKeyboardDevice()
        
        // Un dispositivo tiene teclado físico si:
        // 1. Es un dispositivo conocido con teclado físico (independientemente de la configuración), O
        // 2. Tiene un tipo de teclado definido Y el teclado está presente (no oculto)
        // 
        // NOTA: No usamos isKeyboardHidden (HARDKEYBOARDHIDDEN_YES) solo porque en dispositivos
        // sin teclado físico también puede ser YES, lo que causaría falsos positivos.
        // Solo consideramos que hay teclado físico si está presente (visible) o si es un dispositivo conocido.
        return isKnownPhysicalKeyboardDevice || 
               (hasKeyboardType && isKeyboardPresent)
    }
    
    /**
     * Verifica si el dispositivo es un modelo conocido que tiene teclado físico
     * pero puede reportar incorrectamente en Configuration
     */
    private fun isKnownPhysicalKeyboardDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()
        
        // Lista de dispositivos conocidos con teclado físico
        // Qin F21 Pro y otros dispositivos similares
        return manufacturer.contains("qin") || 
               model.contains("f21") ||
               model.contains("f22") ||
               device.contains("f21") ||
               device.contains("f22") ||
               // Otros dispositivos con teclado T9 conocidos
               model.contains("blackberry") ||
               model.contains("keyone") ||
               model.contains("key2")
    }
}

