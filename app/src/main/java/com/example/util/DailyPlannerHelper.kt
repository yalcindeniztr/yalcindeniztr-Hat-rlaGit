package com.example.util

import android.content.Context
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DailyPlannerHelper {

    suspend fun generateDailySchedule(
        context: Context,
        userNick: String,
        userCity: String,
        userDistrict: String,
        realLat: Double = 0.0,
        realLng: Double = 0.0
    ): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val allReminders = db.reminderDao().getAllRemindersList()

        val calNow = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val todayTasks = allReminders.filter {
            it.dueDateMillis in todayStart..todayEnd ||
            (it.dueDateMillis == 0L && it.dueDatetime.isNotBlank())
        }.sortedBy { it.dueDateMillis }

        val sdfDate = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
        val dateString = sdfDate.format(Date())

        val morningTasks = mutableListOf<String>()
        val afternoonTasks = mutableListOf<String>()
        val eveningTasks = mutableListOf<String>()

        for (task in todayTasks) {
            val taskCal = Calendar.getInstance()
            if (task.dueDateMillis > 0) {
                taskCal.timeInMillis = task.dueDateMillis
                val hour = taskCal.get(Calendar.HOUR_OF_DAY)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.ROOT)
                val timeStr = sdfTime.format(Date(task.dueDateMillis))
                val entry = "• [$timeStr] ${task.title} (${task.category})"

                when {
                    hour < 12 -> morningTasks.add(entry)
                    hour < 17 -> afternoonTasks.add(entry)
                    else -> eveningTasks.add(entry)
                }
            } else {
                afternoonTasks.add("• ${task.title} (${task.category})")
            }
        }

        // Canlı Hava Durumu Özeti
        val weatherBrief = WeatherHelper.getWeatherBriefing(context, realLat, realLng, userCity)
        val weatherShort = weatherBrief.lines().take(4).joinToString("\n")

        val greeting = if (userNick.isNotBlank()) "$userNick dostum," else "Can dostum,"

        val sb = StringBuilder()
        sb.appendLine("📅 **GÜNLÜK İŞ AKIŞI VE GÜN PROGRAMI**")
        sb.appendLine("🗓️ **Tarih:** $dateString")
        sb.appendLine("📍 **Konum:** $userCity, $userDistrict")
        sb.appendLine()
        sb.appendLine(weatherShort)
        sb.appendLine()

        sb.appendLine("🌅 **SABAH / GÜNE BAŞLANGIÇ (08:00 - 12:00):**")
        if (morningTasks.isNotEmpty()) {
            morningTasks.forEach { sb.appendLine(it) }
        } else {
            sb.appendLine("• Planlı bir sabah randevun yok; güne enerjik başlama ve zihinsel hazırlık vakti.")
        }
        sb.appendLine()

        sb.appendLine("☀️ **ÖĞLE / ODAKLANMA VE İŞLER (12:00 - 17:00):**")
        if (afternoonTasks.isNotEmpty()) {
            afternoonTasks.forEach { sb.appendLine(it) }
        } else {
            sb.appendLine("• Rutin işlerini tamamlama, çalışma ve kısa bir kahve molası vakti.")
        }
        sb.appendLine()

        sb.appendLine("🌆 **AKŞAM / GÜNÜ TOPARLAMA (17:00 - 22:00):**")
        if (eveningTasks.isNotEmpty()) {
            eveningTasks.forEach { sb.appendLine(it) }
        } else {
            sb.appendLine("• Akşam görevlerin temiz; sevdiklerinle vakit geçirme ve dinlenme zamanı.")
        }
        sb.appendLine()

        sb.appendLine("💡 **Usta'nın Tavsiyesi:** $greeting günün ne kadar hareketli olursa olsun, işleri sıraya koyup sakinlikle yönetirsen her şey tıkır tıkır işler. Başarılar dilerim!")

        sb.toString().trim()
    }
}
