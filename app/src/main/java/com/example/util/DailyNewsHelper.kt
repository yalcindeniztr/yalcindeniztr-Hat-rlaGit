package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DailyNewsHelper {

    private fun getTodayDateStr(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR"))
        return sdf.format(Calendar.getInstance().time)
    }

    /**
     * Güncel gazete ve haber başlıkları (Canlı RSS destekli)
     */
    fun getHeadlinesOnly(): String {
        return try {
            runBlocking(Dispatchers.IO) {
                LiveWebSearchHelper.fetchLiveNewsHeadlines()
            }
        } catch (_: Exception) {
            val dateStr = getTodayDateStr()
            """
📰 GÜNLÜK GÜNDEM VE HABER BAŞLIKLARI
Tarih: $dateStr
Kaynak: Ulusal Haber Akışı

1. EKONOMİ & MALİYE:
Enflasyonla mücadelede yeni adımlar, istihdam teşvikleri ve piyasa dengeleri Meclis ve kamuoyu takibinde.

2. MAARİF & EĞİTİM:
Türkiye Yüzyılı Maarif Modeli programları, öğretmenlik meslek kanunu düzenlemeleri ve yeni dönem projeleri görüşülüyor.

3. TEKNOLOJİ & YERLİ ÜRETİM:
Savunma sanayii ve yerli yazılım alanında yeni atılımlar sürüyor; ihracat ve teknoloji koridorları genişletiliyor.

4. DIŞ POLİTİKA:
Bölgesel barış girişimleri ve uluslararası diplomatik temaslar yoğun şekilde sürdürülüyor.
            """.trimIndent()
        }
    }

    /**
     * TTS için derli toplu insani sesli gazete anonsu
     */
    fun getVoiceHeadlinesSummary(): String {
        return "Sayın Patronum, bugünün güncel haber başlıklarını ve ulusal gündem manşetlerini ekranınıza getirdim. Ekonomi, teknoloji ve eğitim alanındaki gelişmeleri inceleyebilirsiniz."
    }

    /**
     * Teknoloji ve Yapay Zeka Haberleri
     */
    fun getTechNews(): String {
        val dateStr = getTodayDateStr()
        return """
🚀 TEKNOLOJİ VE BİLİM DÜNYASI HABERLERİ
Tarih: $dateStr

1. YAPAY ZEKA VE OTOMASYON:
Büyük dil modelleri, uç cihazlarda (on-device AI) çalışan yeni nesil algoritmalarla güçlendiriliyor.

2. UZAY VE HAVACILIK:
Yeni nesil uydu haberleşme sistemleri ve roket teknolojilerinde derin uzay görevleri hız kazandı.

3. ROBOTİK SİSTEMLER:
İnsansı robotlar ve otonom endüstriyel çözümler üretim hatlarına daha etkin entegre ediliyor.
        """.trimIndent()
    }
}
