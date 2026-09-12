package com.example.util

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DailyRoutinePlanner {

    data class RoutineBlock(
        val hour: Int,
        val minute: Int,
        val title: String,
        val category: String,
        val note: String
    )

    val DEFAULT_ROUTINES = listOf(
        RoutineBlock(7, 0, "🌅 Güne Başlangıç & Su", "RUTİN", "1 bardak ılık su iç ve güne zinde başla."),
        RoutineBlock(7, 30, "🍳 Kahvaltı & İlaç/Vitamin", "RUTİN", "Dengeli kahvaltı ve sabah destekleri."),
        RoutineBlock(8, 30, "📚 Odaklanma & Çalışma Bloğu 1", "RUTİN", "Günün en önemli ve öncelikli görevlerini tamamla."),
        RoutineBlock(12, 30, "🥗 Öğle Molası & Kısa Yürüyüş", "RUTİN", "Hafif öğle yemeği ve temiz hava molası."),
        RoutineBlock(14, 0, "💼 Odaklanma & Çalışma Bloğu 2", "RUTİN", "Toplantılar, yazışmalar ve ikincil hedefler."),
        RoutineBlock(17, 30, "🏃 Gün Sonu & Egzersiz", "RUTİN", "30 dakika yürüyüş, esneme veya spor."),
        RoutineBlock(19, 0, "🍲 Akşam Yemeği & Dinlenme", "RUTİN", "Aile vakti, zihinsel rahatlama."),
        RoutineBlock(22, 0, "📖 Kitap & Gece Rutini", "RUTİN", "Ekranlardan uzaklaşma, ertesi gün hedeflerini gözden geçirme.")
    )

    fun getDailyPlanBriefing(userName: String = ""): String {
        val greeting = if (userName.isNotBlank()) "Efendim Sayın $userName, " else "Efendim, "
        val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
        val todayStr = sdf.format(Date())

        val planText = StringBuilder()
        planText.append("📋 **GÜNLÜK DENGELİ RUTİN VE ÇALIŞMA PROGRAMI**\n")
        planText.append("🗓️ **Tarih:** $todayStr\n\n")
        planText.append("$greeting gününüzün yüksek verim ve zindelikle geçmesi için hazırladığım akıllı program:\n\n")

        for (block in DEFAULT_ROUTINES) {
            val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", block.hour, block.minute)
            planText.append("• **$timeFormatted** ── ${block.title}\n  _${block.note}_\n\n")
        }

        planText.append("💡 **ATİLA'nın Notu:** İsterseniz 'Rutinlerimi takvime ve alarmlara işle' diyerek bu saatler için otomatik sesli alarmlar oluşturabilirsiniz.")
        return planText.toString().trim()
    }

    fun getVoiceRoutineSummary(userName: String = ""): String {
        val greeting = if (userName.isNotBlank()) "Efendim $userName, " else "Efendim, "
        return "${greeting}gününüz için dengeli bir program hazırladım. Sabah 7'de güne başlangıç ve su, 8:30'da ilk çalışma bloğu, 12:30'da öğle molası, 17:30'da egzersiz ve akşam kitap vakti bulunuyor. Detayları ekranınızda listeledim."
    }

    suspend fun scheduleFullRoutine(context: Context): String {
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        var addedCount = 0

        for (block in DEFAULT_ROUTINES) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, block.hour)
                set(Calendar.MINUTE, block.minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(cal.time)
            val rem = ReminderEntity(
                category = "RUTİN",
                title = block.title,
                customNote = block.note,
                dueDatetime = dateStr,
                dueDateMillis = cal.timeInMillis,
                isFavorite = true,
                encryptedMetadata = "{}",
                actionStep = "SOUND_CLASSIC_BELL"
            )
            val id = db.reminderDao().insertReminder(rem)
            AlarmHelper.scheduleAlarm(context, rem.copy(id = id.toInt()))
            LocalStorageManager.saveLocalReminder(context, block.title, dateStr, cal.timeInMillis, "RUTİN")
            addedCount++
        }

        return "🗓️ Toplam $addedCount adet günlük rutin alarmı ve hatırlatıcısı başarıyla planlandı."
    }
}
