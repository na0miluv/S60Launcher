package com.notkia.launcher

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.ArrayList

class NotificationService : NotificationListenerService() {

    private var isListenerConnected = false

    companion object {
        const val ACTION_UPDATE = "com.notkia.launcher.NOTIFICATION_UPDATE"
        const val EXTRA_NOTIFICATION_PACKAGES = "com.notkia.launcher.EXTRA_NOTIFICATION_PACKAGES"
        var currentPackages: List<String> = emptyList()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isListenerConnected = true
        updateNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isListenerConnected = false
        updateNotifications() // This will clear the notifications
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    private fun updateNotifications() {
        currentPackages = if (isListenerConnected) {
            getActiveNotifications()?.filter { it.isClearable }?.map { it.packageName } ?: emptyList()
        } else {
            emptyList()
        }
        val intent = Intent(ACTION_UPDATE).apply {
            putStringArrayListExtra(EXTRA_NOTIFICATION_PACKAGES, ArrayList(currentPackages))
        }
        sendBroadcast(intent)
    }
}
