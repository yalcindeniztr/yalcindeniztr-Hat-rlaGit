package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import com.example.receiver.ReminderReceiver

/**
 * ATİLLA Wear OS & Smart Watch HUD Synchronizer.
 * Bridges Android Wear OS smartwatches with ATİLLA:
 * - Rich wrist notifications with wearable actions (Haptic, Voice Reply, Quick Tile data)
 * - RemoteInput speech capture from smartwatch microphone directly into ATİLLA.
 */
object AtillaWearSyncHelper {

    private const val CHANNEL_ID = "ATILLA_WEAR_HUD_CHANNEL"
    private const val CHANNEL_NAME = "ATİLLA Akıllı Saat HUD Bildirimleri"
    const val EXTRA_VOICE_REPLY = "extra_wear_voice_reply"
    private const val WEAR_NOTIF_ID = 30301

    data class WearTileData(
        val patronTitle: String,
        val nextClassOrTask: String,
        val activeStatus: String,
        val timestamp: Long
    )

    /**
     * Akıllı saate (Wear OS) özel zenginleştirilmiş bildirim ve sesli yanıt düğmesi gönderir.
     */
    fun sendWearableHudNotification(
        context: Context,
        title: String,
        body: String,
        patronPrefix: String = "Sayın Patronum"
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wear OS akıllı saat bilek bildirimleri ve sesli yanıt protokolü"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 80, 150)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 1. Saatten sesli yanıt alabilmek için RemoteInput
        val remoteInput = RemoteInput.Builder(EXTRA_VOICE_REPLY)
            .setLabel("ATİLLA'ya sesli cevap verin...")
            .build()

        // 2. Yanıt intent'i (MainActivity'ye veya Receiver'a döner)
        val replyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("FROM_WEARABLE", true)
            putExtra("action", "OPEN_AI_ASSISTANT")
        }
        val replyPendingIntent = PendingIntent.getActivity(
            context,
            30302,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // 3. Bilekten Sesli Yanıt Eylemi
        val voiceAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_btn_speak_now,
            "🎤 Sesle Yanıtla",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // 4. Hızlı Eylem: Fener Aç/Kapa
        val torchIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_WEAR_QUICK_TORCH"
        }
        val torchPendingIntent = PendingIntent.getBroadcast(
            context,
            30303,
            torchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val torchAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_dialog_info,
            "💡 Fener",
            torchPendingIntent
        ).build()

        // 5. WearableExtender oluşturma (Sayfalandırma, bilek titreşimi ve arka plan)
        val wearableExtender = NotificationCompat.WearableExtender()
            .addAction(voiceAction)
            .addAction(torchAction)
            .setHintShowBackgroundOnly(false)

        val mainIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            30304,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⌚ $title")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .extend(wearableExtender)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        notificationManager.notify(WEAR_NOTIF_ID, notification)
    }

    /**
     * Saatten gelen sesli yanıtı Intent'ten ayıklar.
     */
    fun extractVoiceReplyFromIntent(intent: Intent): String? {
        val remoteInputBundle: Bundle? = RemoteInput.getResultsFromIntent(intent)
        return remoteInputBundle?.getCharSequence(EXTRA_VOICE_REPLY)?.toString()
    }

    /**
     * Akıllı saat kadranı (Wear OS Tile) için hızlı telemetri ve ders brifing paketi üretir.
     */
    fun getWearTileData(context: Context, patronPrefix: String = "Sayın Patronum"): WearTileData {
        return WearTileData(
            patronTitle = patronPrefix,
            nextClassOrTask = "10. Sınıf Maarif Tarih Dersi",
            activeStatus = "ATİLLA Canlı & Hazır",
            timestamp = System.currentTimeMillis()
        )
    }
}
