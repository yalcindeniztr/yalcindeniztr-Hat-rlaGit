package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.receiver.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ATİLLA Proactive Routine Engine for Maarif Education & Daily Intelligence.
 * Proactively wakes up, prepares curriculum plans, historical trivia ("Bugün Tarihte Ne Oldu?"),
 * and delivers morning/evening briefings for the History Teacher.
 */
object AtillaProactiveEngine {

    private const val CHANNEL_ID = "ATILLA_PROACTIVE_CHANNEL"
    private const val CHANNEL_NAME = "ATİLLA Maarif Lideri Bildirimleri"
    private const val MORNING_ALARM_REQ_CODE = 99101
    private const val EVENING_ALARM_REQ_CODE = 99102

    // Tarihte Bugün veri havuzu (Örnek referans olaylar)
    private val HISTORICAL_EVENTS = mapOf(
        "01-01" to "1923: Türkiye Cumhuriyeti ilk nüfus sayımı hazırlıklarına başladı.",
        "18-03" to "1915: Çanakkale Deniz Zaferi kazanıldı; milletin diriliş destanı yazıldı.",
        "23-04" to "1920: Türkiye Büyük Millet Meclisi Ankara'da açıldı.",
        "19-05" to "1919: Mustafa Kemal Paşa Samsun'a ayak basarak Millî Mücadele meşalesini yaktı.",
        "29-05" to "1453: Fatih Sultan Mehmed İstanbul'u fethederek Orta Çağ'ı kapattı, Yeni Çağ'ı açtı.",
        "26-08" to "1071: Sultan Alparslan Malazgirt Zaferi ile Anadolu'nun kapılarını Türklere açtı.",
        "30-08" to "1922: Başkomutanlık Meydan Muharebesi ile Büyük Zafer kazanıldı.",
        "29-10" to "1923: Cumhuriyet ilan edildi; egemenlik kayıtsız şartsız millete verildi.",
        "10-11" to "1938: Gazi Mustafa Kemal Atatürk ebediyete intikal etti."
    )

    // Maarif Modeli Sınıf Düzeyi Kazanım Havuzu
    private val MAARIF_GRADE_TOPICS = mapOf(
        "9" to "Tarih ve Zaman, İlk Çağ Medeniyetleri ve İlk Türk Devletlerinde Teşkilat Yapısı.",
        "10" to "Beylikten Devlete Osmanlı Siyaseti, Balkan Fetihleri ve Ahilik Teşkilatının Toplumsal Rolü.",
        "11" to "Değişen Dünya Dengeleri Karşısında Osmanlı Devleti ve 17-18. Yüzyıl Islahat Hareketleri.",
        "12" to "20. Yüzyıl Başlarında Osmanlı Devleti, Trablusgarp, Balkan Savaşları ve Millî Mücadele Ruhu."
    )

    /**
     * Sabah Brifingi Üretir (Konuşma Metni, Ayrıntılı UI Markdown)
     */
    fun generateMorningBriefing(context: Context, patronPrefix: String = "Sayın Patronum"): Pair<String, String> {
        val today = Date()
        val dayFormat = SimpleDateFormat("EEEE", Locale.forLanguageTag("tr-TR")).format(today)
        val monthDayFormat = SimpleDateFormat("MM-dd", Locale.ROOT).format(today)
        val dateDisplayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR")).format(today)

        val historyFact = HISTORICAL_EVENTS[monthDayFormat] 
            ?: "Bugün tarihte; Türk milletinin kadim medeniyet yolculuğunda sayısız onurlu sayfa kaydedilmiştir."

        val speech = buildString {
            append("Hayırlı sabahlar $patronPrefix. ")
            append("Bugün $dateDisplayFormat $dayFormat. ")
            append("Günün Maarif brifingi hazır. ")
            append("Bugün tarihte: $historyFact ")
            append("10 ve 11. sınıflarınız için Maarif Modeli ders notlarını ve kritik analiz sorularını hazırladım. Harika bir ders günü dilerim.")
        }

        val display = buildString {
            append("🏛️ **ATİLLA MAARİF LİDERİ SABAH BRİFİNGİ**\n\n")
            append("📅 **Tarih:** $dateDisplayFormat ($dayFormat)\n")
            append("⏳ **Bugün Tarihte Ne Oldu?**\n• $historyFact\n\n")
            append("📚 **Maarif Modeli Günlük Ders Odakları:**\n")
            MAARIF_GRADE_TOPICS.forEach { (grade, topic) ->
                append("• **$grade. Sınıf:** $topic\n")
            }
            append("\n💡 **Pedagojik İpucu:** Derste öğrencilere 'Sebep-Sonuç ve Süreklilik' ilişkisi üzerinden bir açık uçlu soru yöneltmek derse katılımı %40 artıracaktır.\n")
            append("⚡ *Tüm ders sunumları ve PDF dökümleri emrinizdedir.*")
        }

        return Pair(speech, display)
    }

    /**
     * Akşam Değerlendirmesi Üretir
     */
    fun generateEveningDebrief(context: Context, patronPrefix: String = "Sayın Patronum"): Pair<String, String> {
        val speech = buildString {
            append("İyi akşamlar $patronPrefix. ")
            append("Ders gününün başarıyla tamamlandığını umuyorum. ")
            append("Yarının ders planını ve sınav soru havuzunu şimdiden hazırlamamı ister misiniz? Şimdi biraz dinlenme vakti.")
        }

        val display = buildString {
            append("🌙 **GÜN SONU MAARİF RAPORU & DİNLENME**\n\n")
            append("Patron, bugünkü yoğun çalışma temposundan sonra sistemleriniz stabil.\n")
            append("• Yarın için ders planları arşivde hazır.\n")
            append("• İsterseniz tek komutla test veya çalışma kâğıdı çıkarabilirim.\n\n")
            append("🍵 *Günün yorgunluğunu atmak için dinlenmenizi tavsiye ederim.*")
        }

        return Pair(speech, display)
    }

    /**
     * AlarmManager ile günlük sabah (07:45) ve akşam (18:30) brifinglerini kurar
     */
    fun scheduleDailyProactiveBriefings(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Sabah Alarmı (07:45)
        scheduleTimeAlarm(context, alarmManager, 7, 45, MORNING_ALARM_REQ_CODE, "ACTION_ATILLA_MORNING_BRIEF")

        // 2. Akşam Alarmı (18:30)
        scheduleTimeAlarm(context, alarmManager, 18, 30, EVENING_ALARM_REQ_CODE, "ACTION_ATILLA_EVENING_BRIEF")
    }

    private fun scheduleTimeAlarm(
        context: Context,
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        requestCode: Int,
        action: String
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra("IS_PROACTIVE_BRIEF", true)
            putExtra("BRIEF_ACTION", action)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    /**
     * Proaktif bildirim oluşturur
     */
    fun showProactiveNotification(context: Context, title: String, content: String, isMorning: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ATİLLA Maarif Lideri günlük rutin ve ders bilgilendirmeleri"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "ASSISTANT")
            putExtra("PROACTIVE_PROMPT", if (isMorning) "sabah brifingi" else "akşam değerlendirmesi")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isMorning) 1010 else 1020,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(if (isMorning) 901 else 902, notification)
    }
}
