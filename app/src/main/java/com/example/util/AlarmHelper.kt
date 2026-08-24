package com.example.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.PrayerTimingData
import com.example.data.ReminderEntity
import com.example.receiver.ReminderReceiver
import java.util.Calendar

object AlarmHelper {

    private val PRAYER_IDS = mapOf(
        "İmsak" to 9001,
        "Güneş" to 9002,
        "Öğle" to 9003,
        "İkindi" to 9004,
        "Akşam" to 9005,
        "Yatsı" to 9006
    )

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, reminder: ReminderEntity, alarmSound: String = "CLASSIC_BELL") {
        if (reminder.dueDateMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_CATEGORY", reminder.category)
            putExtra("REMINDER_NOTE", reminder.customNote)
            putExtra("ALARM_SOUND", alarmSound)
            putExtra("IS_PRAYER", false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.dueDateMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.dueDateMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.dueDateMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.dueDateMillis, pendingIntent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleCustomAlarm(
        context: Context,
        alarmId: Int,
        title: String,
        category: String,
        note: String = "",
        triggerAtMillis: Long,
        alarmSound: String = "CLASSIC_BELL"
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", alarmId)
            putExtra("REMINDER_TITLE", title)
            putExtra("REMINDER_CATEGORY", category)
            putExtra("REMINDER_NOTE", note)
            putExtra("ALARM_SOUND", alarmSound)
            putExtra("IS_PRAYER", false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        // Cancel the primary alarm
        var pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Cancel the early warning alarm
        val earlyAlarmId = (reminderId * 1000) + 999
        pendingIntent = PendingIntent.getBroadcast(
            context,
            earlyAlarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Cancel up to 15 sub-alarms (0..15)
        for (i in 0..15) {
            val subAlarmId = (reminderId * 1000) + i
            pendingIntent = PendingIntent.getBroadcast(
                context,
                subAlarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun schedulePrayerAlarm(
        context: Context,
        prayerName: String,
        prayerTimeStr: String,
        minutesBefore: Int = 15
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prayerId = PRAYER_IDS[prayerName] ?: 9000

        val parts = prayerTimeStr.split(":")
        if (parts.size < 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -minutesBefore)
        }

        // Eğer bugünün vakti geçtiyse bir sonraki güne planla
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerAtMillis = cal.timeInMillis

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", prayerId)
            putExtra("IS_PRAYER", true)
            putExtra("PRAYER_NAME", prayerName)
            putExtra("PRAYER_TIME", prayerTimeStr)
            putExtra("PRAYER_MINUTES_BEFORE", minutesBefore)
            putExtra("REMINDER_TITLE", "Ezan Vakti: $prayerName ($prayerTimeStr)")
            putExtra("REMINDER_CATEGORY", "Ezan Vakti")
            putExtra("ALARM_SOUND", "CLASSIC_BELL")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelPrayerAlarm(context: Context, prayerName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prayerId = PRAYER_IDS[prayerName] ?: 9000
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleAllPrayerAlarms(
        context: Context,
        prayerData: PrayerTimingData,
        notificationsMap: Map<String, Boolean>,
        minutesBefore: Int = 15
    ) {
        val prayerTimes = mapOf(
            "İmsak" to prayerData.imsak,
            "Güneş" to prayerData.gunes,
            "Öğle" to prayerData.ogle,
            "İkindi" to prayerData.ikindi,
            "Akşam" to prayerData.aksam,
            "Yatsı" to prayerData.yatsi
        )

        prayerTimes.forEach { (name, time) ->
            val isEnabled = notificationsMap[name] ?: true
            if (isEnabled && time.isNotBlank()) {
                schedulePrayerAlarm(context, name, time, minutesBefore)
            } else {
                cancelPrayerAlarm(context, name)
            }
        }
    }
}
