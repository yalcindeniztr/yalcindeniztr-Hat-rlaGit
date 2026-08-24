package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.DataStoreManager
import com.example.data.PrayerTimesRepository
import com.example.util.AlarmHelper
import com.example.util.TtsHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for firing reminder alarms,
 * prayer time notifications, speech synthesis (TTS),
 * and rescheduling all active alarms upon device reboot (BOOT_COMPLETED).
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // 1. Device Reboot handling (BOOT_COMPLETED) -> Reschedule all alarms
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device reboot completed: Rescheduling all active reminders and prayer alarms")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()
                    val activeReminders = db.reminderDao().getActiveRemindersList(now)
                    
                    activeReminders.forEach { reminder ->
                        val soundKey = if (reminder.actionStep.startsWith("SOUND_")) {
                            reminder.actionStep.removePrefix("SOUND_")
                        } else {
                            "CLASSIC_BELL"
                        }
                        AlarmHelper.scheduleAlarm(context, reminder, soundKey)
                    }

                    // Reschedule Prayer Times
                    val dataStoreManager = DataStoreManager(context)
                    val notificationsJson = dataStoreManager.prayerNotificationsJson.first()
                    val minutesBefore = dataStoreManager.prayerReminderMinutesBefore.first()
                    val selectedCity = dataStoreManager.prayerSelectedCity.first()

                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val prayerMapType = Types.newParameterizedType(Map::class.java, String::class.java, Boolean::class.javaObjectType)
                    val prayerMapAdapter = moshi.adapter<Map<String, Boolean>>(prayerMapType)
                    val notifMap = prayerMapAdapter.fromJson(notificationsJson) ?: emptyMap()

                    val prayerRepo = PrayerTimesRepository(context)
                    val prayerData = prayerRepo.getPrayerTimes(selectedCity)
                    AlarmHelper.scheduleAllPrayerAlarms(context, prayerData, notifMap, minutesBefore)

                    Log.d(TAG, "Successfully rescheduled ${activeReminders.size} reminders after reboot")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // 2. Normal Reminder / Alarm Trigger Handling
        val isPrayer = intent.getBooleanExtra("IS_PRAYER", false)
        val reminderId = intent.getIntExtra("REMINDER_ID", (System.currentTimeMillis() % 100000).toInt())
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Hatırlatıcı"
        val category = intent.getStringExtra("REMINDER_CATEGORY") ?: "Hatırlatma"
        val note = intent.getStringExtra("REMINDER_NOTE") ?: ""
        val alarmSound = intent.getStringExtra("ALARM_SOUND") ?: "CLASSIC_BELL"
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: ""
        val prayerTime = intent.getStringExtra("PRAYER_TIME") ?: ""
        val minutesBefore = intent.getIntExtra("PRAYER_MINUTES_BEFORE", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val soundUri = when (alarmSound) {
            "DIGITAL_SIREN" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "CALM_MELODY" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        val channelId = when {
            isPrayer -> "ezan_vakti_channel_v2"
            alarmSound == "DIGITAL_SIREN" -> "hatirlagit_sound_siren_v1"
            alarmSound == "CALM_MELODY" -> "hatirlagit_sound_melody_v1"
            else -> "hatirlagit_sound_classic_v1"
        }

        val channelName = when {
            isPrayer -> "Ezan Vakti Bildirimleri"
            alarmSound == "DIGITAL_SIREN" -> "🚨 Güçlü Siren & Dijital Alarm"
            alarmSound == "CALM_MELODY" -> "🎵 Sakin Huzur Melodisi"
            else -> "🔔 Klasik Çan & Standart Zil"
        }

        val vibrationPattern = when {
            isPrayer -> longArrayOf(0, 500, 200, 500, 200, 800)
            alarmSound == "DIGITAL_SIREN" -> longArrayOf(0, 600, 200, 600, 200, 600, 200, 1000)
            alarmSound == "CALM_MELODY" -> longArrayOf(0, 200, 200, 200, 200, 400)
            else -> longArrayOf(0, 350, 150, 350)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(if (alarmSound == "DIGITAL_SIREN") AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "HatırlaGit çoklu saat ve özel alarm bildirimleri"
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (isPrayer) {
                putExtra("NAVIGATE_TAB", 2) // Ezan vakti sekmesi
            } else {
                putExtra("NAVIGATE_TAB", 0) // Ana sayfa
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifTitle: String
        val notifText: String

        if (isPrayer) {
            notifTitle = "🕌 Ezan Vakti: $prayerName ($prayerTime)"
            notifText = if (minutesBefore > 0) {
                "$prayerName ezan vaktine $minutesBefore dakika kaldı ($prayerTime)."
            } else {
                "$prayerName ezanı vakti girdi ($prayerTime). Haydi namaza!"
            }
        } else {
            notifTitle = if (category.isNotBlank() && !title.startsWith("[$category]")) "[$category] $title" else title
            notifText = if (note.isNotBlank()) "⏰ Zamanı geldi! $note" else "⏰ Zamanı geldi! Lütfen kontrol edin."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(notifTitle)
            .setContentText(notifText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (isPrayer || alarmSound == "DIGITAL_SIREN") NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(reminderId, notification)

        // Read out loud with Text-To-Speech if enabled
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataStoreManager = DataStoreManager(context)
                val isVoiceEnabled = dataStoreManager.isVoiceSpeakingEnabled.first()
                if (isVoiceEnabled) {
                    val speechPhrase = if (isPrayer) {
                        if (minutesBefore > 0) {
                            "$prayerName ezan vaktine $minutesBefore dakika kaldı."
                        } else {
                            "$prayerName ezanı vakti girdi. Haydi namaza!"
                        }
                    } else {
                        val cleanTitle = if (category.isNotBlank()) "$category: $title" else title
                        val cleanNote = if (note.isNotBlank()) ". $note" else ""
                        "Hatırlatma zamanı: $cleanTitle$cleanNote"
                    }
                    TtsHelper.speak(context, speechPhrase)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error speaking reminder with TTS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
