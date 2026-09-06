package com.example.util

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserRoutine(
    val time: String,
    val title: String,
    val description: String,
    val category: String = "RUTİN"
)

object UserRoutineHelper {

    private const val PREFS_NAME = "user_routines_prefs"
    private const val KEY_ROUTINES = "saved_routines_json"

    /**
     * Kullanıcının özel bir rutinini hafızaya ekler (Öğrenme kabiliyeti)
     */
    fun learnRoutine(context: Context, time: String, title: String, description: String = ""): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_ROUTINES, "[]") ?: "[]"
        val array = JSONArray(currentJson)

        val newObj = JSONObject().apply {
            put("time", time)
            put("title", title)
            put("description", description)
            put("learned_at", System.currentTimeMillis())
        }
        array.put(newObj)
        prefs.edit().putString(KEY_ROUTINES, array.toString()).apply()

        return "🧠 Yeni rutininiz öğrenildi ve hafızaya kaydedildi: Saat " + time + " - " + title
    }

    /**
     * Kayıtlı ve öğrenilmiş tüm rutinleri listeler
     */
    fun getLearnedRoutines(context: Context): List<UserRoutine> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_ROUTINES, "[]") ?: "[]"
        val array = JSONArray(currentJson)
        val list = mutableListOf<UserRoutine>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                UserRoutine(
                    time = obj.optString("time", "09:00"),
                    title = obj.optString("title", "Genel Rutin"),
                    description = obj.optString("description", "")
                )
            )
        }

        // Eğer henüz özel rutin yoksa varsayılan akıllı günlük rutinleri öner
        if (list.isEmpty()) {
            list.add(UserRoutine("07:30", "Güne Başlangıç & Canlı Hava Durumu", "Günün meteoroloji ve iş akışı kontrolü"))
            list.add(UserRoutine("08:30", "Ders / İş Hazırlığı & Ajanda Takibi", "Ders planları veya günlük görevlerin gözden geçirilmesi"))
            list.add(UserRoutine("12:30", "Öğle Molası & Gün Ortası Değerlendirmesi", "Beslenme ve dinlenme zamanı"))
            list.add(UserRoutine("17:30", "Gün Sonu Tamamlama & Notların Arşivlenmesi", "Alınan hızlı ve sesli notların toparlanması"))
            list.add(UserRoutine("20:00", "Akşam Kültür & Dinlenme", "Kitap okuma, TV dizi rehberi veya aile vakti"))
        }

        return list.sortedBy { it.time }
    }

    /**
     * Kullanıcının geçmiş alarmlarından ve randevularından alışkanlıklarını analiz eder
     */
    suspend fun analyzeUserHabits(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val allReminders = db.reminderDao().getActiveRemindersList(0L)
        val learnedRoutines = getLearnedRoutines(context)

        val sb = StringBuilder()
        sb.append("🧠 **Usta'nın Rutin & Alışkanlık Analiz Raporu:**\n\n")

        if (learnedRoutines.isNotEmpty()) {
            sb.append("📋 **Öğrenilmiş Günlük Rutinleriniz:**\n")
            for (r in learnedRoutines) {
                sb.append("• Saat ").append(r.time).append(": ").append(r.title)
                if (r.description.isNotBlank()) {
                    sb.append(" (").append(r.description).append(")")
                }
                sb.append("\n")
            }
            sb.append("\n")
        }

        if (allReminders.isNotEmpty()) {
            val count = allReminders.size
            sb.append("⚡ **Aktif Hatırlatıcı Yoğunluğu:** Toplam ").append(count).append(" adet aktif göreviniz bulunuyor.\n")
        }

        sb.append("💡 *İpucu:* Bana dilediğiniz zaman 'Rutinime ekle: Saat 15:00 kitap okuma' diyerek yeni alışkanlıklarınızı öğretebilirsiniz!")
        return@withContext sb.toString()
    }
}
