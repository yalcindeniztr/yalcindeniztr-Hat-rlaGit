package com.example.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ATİLLA Türkçe İmla, Konuşma Dili, Deyimler ve Atasözleri Çözümleme Motoru.
 * Normalizes speech/keyboard typos ("yarımı planla" ➔ "yarını planla"),
 * understands Turkish idioms & proverbs, and maps conversational requests
 * to precise assistant intents without erroneous web/movie searches.
 */
object AtillaTurkishLinguisticsHelper {

    enum class NormalizedIntent {
        PLAN_TOMORROW,   // Yarını planla, yarımı planla, yarın ne yapayım
        PLAN_TODAY,      // Günümü planla, bugün için program
        PLAN_WEEK,       // Bu haftayı planla, haftalık program
        IDIOM_OR_PROVERB,// Deyim veya atasözü açıklaması/kullanımı
        RECIPE_OR_FOOD,  // Yemek tarifi, buzdolabında ne var
        NONE             // Standart akış
    }

    data class TurkishLinguisticAnalysis(
        val intent: NormalizedIntent,
        val correctedQuery: String,
        val matchedIdiomOrProverb: String?,
        val explanation: String?,
        val directReply: Pair<String, String>? // Pair(Speech, DisplayMarkdown)
    )

    // Türkçede sık kullanılan deyimler ve anlamları
    private val IDIOMS_MAP = mapOf(
        "etekleri zil çalmak" to "Çok sevinmek, işlerin yolunda gitmesinden ötürü büyük bir heyecan ve mutluluk duymak.",
        "can kulağıyla dinlemek" to "Tüm dikkatini vererek, büyük bir dikkat ve özenle dinlemek.",
        "küplere binmek" to "Aşırı derecede öfkelenmek, çok kızmak.",
        "pabucu dama atılmak" to "Kendinden üstün birinin gelmesiyle eski değerini ve itibarını kaybetmek.",
        "gözden düşmek" to "Eskiden duyulan sevgi, saygı veya güveni kaybetmek.",
        "göz boyamak" to "Kötü veya eksik bir şeyi hileyle iyi ve eksiksiz göstermeye çalışmak.",
        "iki ayağı bir pabuca girmek" to "Bir işi yetiştirmek için telaşa kapılmak, çok acele etmek.",
        "çantada keklik" to "Zahmetsizce, kesin olarak elde edilecek gözüyle bakılan şey.",
        "pireyi deve yapmak" to "Önemsiz, küçük bir olayı aşırı derecede büyüterek sorun haline getirmek."
    )

    // Türkçede sık kullanılan atasözleri ve anlamları
    private val PROVERBS_MAP = mapOf(
        "damlaya damlaya göl olur" to "Küçük tasarruflar ve birikimler zamanla büyük kazançlara dönüşür.",
        "ak akçe kara gün içindir" to "Dürüstçe kazanılan para, zor ve sıkıntılı zamanlarda insanı kurtarır.",
        "ayağını yorganına göre uzat" to "Harcamalarını ve planlarını kendi gelirine ve imkânlarına göre ayarla.",
        "tatlı dil yılanı deliğinden çıkarır" to "Güler yüzlü, nazik ve samimi konuşma en zor engelleri bile aşar.",
        "sakla samanı gelir zamanı" to "Bugün değersiz görünen şeyler ileride çok değerli ve gerekli olabilir.",
        "bir elin nesi var iki elin sesi var" to "Birlik ve beraberlik içinde yapılan işler daima başarıya ulaşır.",
        "işleyen demir pas tutmaz" to "Sürekli çalışan, zihnini ve bedenini geliştiren insan her zaman zinde ve üretken kalır."
    )

    /**
     * Kullanıcı girdisini Türkçe imla, klavye/ses hatası ve deyim filtrelerinden geçirir.
     */
    fun analyze(
        context: Context,
        rawQuery: String,
        patronPrefix: String = "Sayın Patronum"
    ): TurkishLinguisticAnalysis {
        val lower = rawQuery.lowercase(Locale.forLanguageTag("tr-TR")).trim()

        // 1. "Yarımı planla" / "Yarını planla" / "Yarınımı programla" Düzeltmesi (Typo Tolerance)
        if (lower.contains("yarımı planla") || lower.contains("yarını planla") || 
            lower.contains("yarınımı planla") || lower.contains("yarınki günümü planla") ||
            lower.contains("yarın ne yapayım") || lower.contains("yarın için plan") ||
            lower.contains("yarınki plan") || lower.contains("yarın program")) {
            
            val plan = generateTomorrowPlan(patronPrefix)
            return TurkishLinguisticAnalysis(
                intent = NormalizedIntent.PLAN_TOMORROW,
                correctedQuery = "yarını planla",
                matchedIdiomOrProverb = null,
                explanation = "İmla/ses kayması ('yarımı planla') tespit edildi ve 'yarını planla' hedefine düzeltildi.",
                directReply = plan
            )
        }

        // 2. "Günümü planla" / "Bugünü planla"
        if (lower.contains("günümü planla") || lower.contains("bugünümü planla") ||
            lower.contains("günün planı") || lower.contains("günlük program") ||
            lower.contains("günlük plan yap") || lower.contains("bugün ne yapayım")) {
            
            val plan = generateTodayPlan(patronPrefix)
            return TurkishLinguisticAnalysis(
                intent = NormalizedIntent.PLAN_TODAY,
                correctedQuery = "günümü planla",
                matchedIdiomOrProverb = null,
                explanation = "Günlük dengeli yaşam ve öğretmenlik programı hazırlandı.",
                directReply = plan
            )
        }

        // 3. Deyimler Analizi
        for ((idiom, meaning) in IDIOMS_MAP) {
            if (lower.contains(idiom)) {
                val speech = "$patronPrefix, '$idiom' güzel Türkçemizin köklü bir deyimidir. Anlamı: $meaning"
                val md = buildString {
                    append("📖 **TÜRKÇE DEYİM ANALİZİ: \"${idiom.uppercase(Locale.forLanguageTag("tr-TR"))}\"**\n\n")
                    append("• **Anlamı:** $meaning\n")
                    append("• **Kullanım Yeri:** Günlük hayatta duyguları, durumları mecaz yoluyla çarpıcı ve samimi biçimde ifade etmek için kullanılır.\n\n")
                    append("💡 *ATİLLA, Türkçemizin tüm imla, nükte ve deyim zenginliğine tam hakimdir.*")
                }
                return TurkishLinguisticAnalysis(
                    intent = NormalizedIntent.IDIOM_OR_PROVERB,
                    correctedQuery = idiom,
                    matchedIdiomOrProverb = idiom,
                    explanation = "Türkçe deyim tespit edildi ve anlamsal açıklaması yapıldı.",
                    directReply = Pair(speech, md)
                )
            }
        }

        // 4. Atasözleri Analizi
        for ((proverb, meaning) in PROVERBS_MAP) {
            if (lower.contains(proverb)) {
                val speech = "$patronPrefix, '$proverb' atalarımızın asırlık tecrübesini yansıtan bir atasözüdür. Anlamı: $meaning"
                val md = buildString {
                    append("🏛️ **TÜRKÇE ATASÖZÜ ANALİZİ: \"${proverb.uppercase(Locale.forLanguageTag("tr-TR"))}\"**\n\n")
                    append("• **Öğüdü & Anlamı:** $meaning\n")
                    append("• **Tarihsel Bağlam:** Kadim Türk kültürünün tasarruf, yardımlaşma ve hayat rehberliği ilkelerini yansıtır.\n")
                }
                return TurkishLinguisticAnalysis(
                    intent = NormalizedIntent.IDIOM_OR_PROVERB,
                    correctedQuery = proverb,
                    matchedIdiomOrProverb = proverb,
                    explanation = "Türkçe atasözü tespit edildi ve anlamsal rehberliği sunuldu.",
                    directReply = Pair(speech, md)
                )
            }
        }

        return TurkishLinguisticAnalysis(
            intent = NormalizedIntent.NONE,
            correctedQuery = rawQuery,
            matchedIdiomOrProverb = null,
            explanation = null,
            directReply = null
        )
    }

    private fun generateTomorrowPlan(patronPrefix: String): Pair<String, String> {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val dateStr = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR")).format(cal.time)

        val speech = buildString {
            append("Yarınınızı büyük bir titizlikle planladım $patronPrefix. ")
            append("Yarın $dateStr. ")
            append("Sabah 07:30'da güne zinde başlangıç, 08:30'da okul ve Maarif Tarih dersleri, 12:30'da öğle molası, 15:30'da öğrenci performans değerlendirmesi ve akşam 19:30'da dinlenme bloğunuz bulunuyor. Programı ekranınıza getirdim.")
        }

        val display = buildString {
            append("📅 **$patronPrefix İÇİN YARININ KAPSAMLI GÜNLÜK PLANI**\n\n")
            append("🗓️ **Tarih:** $dateStr\n")
            append("🎯 **Öncelikli Odak:** MEB Maarif Modeli Tarih Dersi & Yüksek Zihinsel Verimlilik\n\n")
            append("⏰ **Saatlik Zaman Çizelgesi:**\n")
            append("• **07:00 - 07:30** 🌅 *Uyanış, 1 Bardak Ilık Su & Zindelik Egzersizi*\n")
            append("• **07:30 - 08:15** 🍳 *Dengeli Kahvaltı & Günün Haber/Tarih Brifingi*\n")
            append("• **08:30 - 12:00** 🏫 *Okul Bloğu 1: 10. ve 11. Sınıf Tarih Dersleri & Soru Çözümü*\n")
            append("• **12:00 - 13:00** 🥗 *Öğle Molası, Hafif Yemek & Zihin Dinlendirme*\n")
            append("• **13:00 - 15:30** 📚 *Okul Bloğu 2: Zümre Toplantısı, Performans & Yazılı Notlandırma*\n")
            append("• **16:00 - 17:00** 🏃 *Okul Çıkışı Yürüyüş & Temiz Hava Molası*\n")
            append("• **17:30 - 19:00** ☕ *Kişisel Araştırma, Tarih Okumaları & Dinlenme*\n")
            append("• **19:00 - 20:00** 🍲 *Akşam Yemeği & Aile Vakti*\n")
            append("• **20:30 - 22:00** 📖 *Kitap Okuma & Gece Rutini*\n\n")
            append("💡 *Sayın Hocam, isterseniz bu saatler için telefonunuza ve akıllı saatinize otomatik hatırlatıcı alarmlar kurabilirim.*")
        }

        return Pair(speech, display)
    }

    private fun generateTodayPlan(patronPrefix: String): Pair<String, String> {
        val todayStr = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR")).format(Date())

        val speech = buildString {
            append("Gününüzü yüksek verim ve dengeyle planladım $patronPrefix. ")
            append("Bugün $todayStr. Sabah odaklanma bloğu, öğleden sonra ders ve etüt saatleri, akşam için ise dinlenme ve okuma periyotları planlandı.")
        }

        val display = buildString {
            append("📋 **$patronPrefix İÇİN GÜNÜN AKILLI YAŞAM VE ÇALIŞMA PLANI**\n\n")
            append("🗓️ **Tarih:** $todayStr\n\n")
            append("• **Sabah (08:30 - 12:00):** Öncelikli ders hedefleri ve odaklanma\n")
            append("• **Öğle (12:30 - 13:30):** Dengeli beslenme ve kısa yürüyüş\n")
            append("• **Öğleden Sonra (14:00 - 17:00):** Eğitim çalışmaları, sınav değerlendirme ve analiz\n")
            append("• **Akşam (19:00 - 22:00):** Zihinsel dinlenme ve kitap okuma\n\n")
            append("⚡ *Tüm gününüzü kontrol altında tutmak için emirlerinizi bekliyorum.*")
        }

        return Pair(speech, display)
    }
}
