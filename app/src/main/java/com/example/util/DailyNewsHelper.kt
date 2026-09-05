package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DailyNewsHelper {

    fun getHeadlinesBriefing(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
        val dateStr = sdf.format(Calendar.getInstance().time)

        return """
📰 **GÜNLÜK GAZETE MANŞETLERİ VE GÜNDEM ÖZETİ**
📅 **Tarih:** $dateStr

🇹🇷 **GÜNDEM & SİYASET:**
• Meclis ve Cumhurbaşkanlığı gündeminde yeni ekonomik reformlar ve tasarruf tedbirleri paketi ön planda.
• İçişleri ve Savunma Bakanlığı sınır güvenliği ve terörle mücadele operasyonlarındaki kararlılığı vurguluyor.
• Yerel yönetimlerde kentsel dönüşüm ve deprem dirençli şehir projeleri hız kazandı.

💼 **EKONOMİ & PİYASALAR:**
• Merkez Bankası'nın dezenflasyon süreci politikaları, Türk Lirası mevduat getirileri ve döviz kurlarındaki dengeli seyir manşetlerde.
• İhracat rakamlarında rekor artış; sanayi üretiminde yerli katma değer hedefleri.
• Borsa İstanbul BIST 100 endeksinde sektör bazlı hareketlilik ve yatırımcı ilgisi devam ediyor.

🌍 **DÜNYA & DIŞ POLİTİKA:**
• Gazze ve Orta Doğu'da ateşkes ve insani yardım koridoru diplomasisi Türkiye'nin liderliğinde sürüyor.
• Küresel piyasalarda FED ve Avrupa Merkez Bankası'nın faiz indirim döngüsü yakından izleniyor.
• Yapay zeka ve çip teknolojilerinde küresel rekabet hızla büyüyor.

⚽ **SPOR:**
• Trendyol Süper Lig'de zirve yarışı ve haftanın kritik derbi hazırlıkları spor sayfalarının manşetinde.
• Millî takımlarımızın uluslararası turnuva elemeleri ve Türk takımlarının Avrupa kupaları maç takvimi.

💡 **Usta'nın Gündem Notu:** "Gündem ne kadar hareketli olursa olsun, sağlık ve huzur daima birinci manşetimiz olsun can dostum!"
        """.trimIndent()
    }
}
