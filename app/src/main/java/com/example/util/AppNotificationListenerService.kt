package com.example.util

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isHighPriority: Boolean
)

object CapturedNotificationCache {
    private val notifications = CopyOnWriteArrayList<CapturedNotification>()
    private const val MAX_CACHE_SIZE = 50

    fun add(notification: CapturedNotification) {
        // En başa ekle, yinelenen bildirimleri önle
        notifications.removeIf { it.id == notification.id || (it.packageName == notification.packageName && it.title == notification.title && it.text == notification.text) }
        notifications.add(0, notification)
        while (notifications.size > MAX_CACHE_SIZE) {
            notifications.removeAt(notifications.size - 1)
        }
    }

    fun getAll(): List<CapturedNotification> = notifications.toList()

    fun getGardropsAlerts(): List<CapturedNotification> {
        return notifications.filter { it.packageName.contains("gardrops", ignoreCase = true) }
    }

    fun getHighPriorityAlerts(): List<CapturedNotification> {
        return notifications.filter { it.isHighPriority }
    }

    fun getSummaryText(): String {
        if (notifications.isEmpty()) {
            return "Şu an kaydedilmiş önemli bir harici bildirim bulunmuyor. Gardrops, WhatsApp veya alışveriş mesajlarınız geldiğinde Usta size hatırlatacaktır."
        }

        val gardropsList = getGardropsAlerts()
        val sdf = SimpleDateFormat("HH:mm", Locale("tr", "TR"))

        val gardropsBlock = if (gardropsList.isNotEmpty()) {
            val items = gardropsList.take(3).joinToString("\n") {
                "• [${sdf.format(Date(it.timestamp))}] **${it.title}:** ${it.text}"
            }
            "👗 **Gardrops Bildirimleri (${gardropsList.size}):**\n$items\n\n"
        } else ""

        val otherHighPriority = notifications.filter { !it.packageName.contains("gardrops", ignoreCase = true) && it.isHighPriority }
        val otherBlock = if (otherHighPriority.isNotEmpty()) {
            val items = otherHighPriority.take(4).joinToString("\n") {
                "• **${it.appName}** [${sdf.format(Date(it.timestamp))}]: ${it.title} - ${it.text}"
            }
            "⚡ **Diğer Önemli Bildirimler (${otherHighPriority.size}):**\n$items"
        } else ""

        return ("🔔 **BİLDİRİM VE MESAJ ÖZETİ**\n\n" + gardropsBlock + otherBlock).trim()
    }

    fun clear() {
        notifications.clear()
    }
}

class AppNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        try {
            val pkg = sbn.packageName ?: return
            val extras = sbn.notification?.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

            val combinedContent = "$title $text $subText".lowercase(Locale("tr", "TR"))

            // Hedeflenen uygulamalar
            val isGardrops = pkg.contains("gardrops", ignoreCase = true)
            val isDolap = pkg.contains("dolap", ignoreCase = true)
            val isWhatsApp = pkg.contains("whatsapp", ignoreCase = true)
            val isGmail = pkg.contains("google.android.gm", ignoreCase = true)
            val isSms = pkg.contains("messaging", ignoreCase = true) || pkg.contains("mms", ignoreCase = true)

            if (!isGardrops && !isDolap && !isWhatsApp && !isGmail && !isSms) {
                return
            }

            val appName = when {
                isGardrops -> "Gardrops"
                isDolap -> "Dolap"
                isWhatsApp -> "WhatsApp"
                isGmail -> "Gmail"
                isSms -> "SMS"
                else -> pkg
            }

            // Önemli anahtar kelimeler
            val isCritical = combinedContent.contains("teklif") ||
                    combinedContent.contains("indirim") ||
                    combinedContent.contains("mesaj") ||
                    combinedContent.contains("sipariş") ||
                    combinedContent.contains("satıldı") ||
                    combinedContent.contains("kargo") ||
                    combinedContent.contains("randevu") ||
                    combinedContent.contains("acil") ||
                    isGardrops || isDolap

            val captured = CapturedNotification(
                id = "${sbn.packageName}_${sbn.id}_${sbn.postTime}",
                packageName = pkg,
                appName = appName,
                title = title.ifBlank { appName },
                text = text.ifBlank { subText },
                timestamp = sbn.postTime,
                isHighPriority = isCritical
            )

            CapturedNotificationCache.add(captured)
            Log.d(TAG, "Notification captured: $appName -> $title : $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification: ${e.message}")
        }
    }
}
