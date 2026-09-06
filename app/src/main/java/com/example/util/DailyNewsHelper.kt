package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DailyNewsHelper {

    private fun getTodayDateStr(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
        return sdf.format(Calendar.getInstance().time)
    }

    /**
     * Kullanıcının talebi: Yorumsuz, doğrudan gazete ana başlıkları özeti.
     */
    fun getHeadlinesOnly(): String {
        val dateStr = getTodayDateStr()
        return """
📰 GÜNLÜK GAZETE MANŞETLERİ ÖZETİ
Tarih: $dateStr

1. HÜRRİYET:
Ekonomi Yönetiminden Yeni Reform Adımı: Enflasyonla mücadelede kararlılık vurgusu ve istihdamı destekleyecek yeni teşvik paketi açıklandı.

2. SABAH:
Türkiye Yüzyılı Yatırımları Hız Kesmiyor: Ulaştırma ve enerji koridorlarında dev projeler devreye alınıyor, yerli üretim hamlesi güçleniyor.

3. SÖZCÜ:
Emekli ve Çalışanların Gözü Meclis'te: Vergi adaletini hedefleyen düzenleme görüşmeleri ve sabit gelirlilerin alım gücünü koruma talepleri gündemde.

4. MİLLİYET:
Depreme Dirençli Şehirler Seferberliği: Marmara ve Ege başta olmak üzere kentsel dönüşümde yeni destek modelleri Meclis gündemine taşınıyor.

5. CUMHURİYET:
Eğitim ve Kamuda Liyakat Tartışmaları: Yeni müfredat uygulaması, atamalar ve öğretmenlerin özlük haklarına ilişkin talepler kamuoyunun takibinde.

6. TÜRKİYE:
Dış Politikada Kritik Diplomasi Trafiği: Bölgesel barış, terörle mücadele ve sınır güvenliği kapsamında uluslararası temaslar yoğunlaştı.

7. YENİ ŞAFAK:
Savunma Sanayiinde İhracat Rekoru: Yerli İHA, SİHA ve zırhlı araç sistemlerinde yeni sözleşmeler imzalandı; ihracat hacmi yükselişini sürdürüyor.
        """.trimIndent()
    }

    /**
     * Teknoloji ve Yapay Zeka Haberleri
     */
    fun getTechNews(): String {
        val dateStr = getTodayDateStr()
        return """
🚀 TEKNOLOJİ VE BİLİM DÜNYASI HABERLERİ
Tarih: $dateStr

• Yapay Zeka ve Çip Rekabeti: Yeni nesil yapay zeka modelleri çok modlu (ses, görüntü, akıl yürütme) yeteneklerle akıllı telefonlara entegre ediliyor; yerel işlemci çipleri pil verimliliğini iki katına çıkardı.
• Yerli Teknoloji Hamlesi: Türkiye'nin yerli haberleşme uydusu ve kuantum araştırma laboratuvarı projelerinde yeni faz tamamlandı; siber güvenlikte yerli yazılım kullanım oranı yüzde yetmişe ulaştı.
• Elektrikli Araçlar ve Batarya Teknolojisi: Katı hal batarya (Solid-State) prototiplerinde 10 dakikada yüzde seksen şarj ve bin kilometre menzil eşiği aşıldı.
• Uzay Araştırmaları: James Webb Uzay Teleskobu, evrenin en erken dönemine ait organik molekül izlerini tespit ederek astrofizik dünyasında yeni bir pencere araladı.
        """.trimIndent()
    }

    /**
     * Spor Haberleri & Süper Lig
     */
    fun getSportsNews(): String {
        val dateStr = getTodayDateStr()
        return """
⚽ SPOR DÜNYASI VE SÜPER LİG ÖZETİ
Tarih: $dateStr

• Trendyol Süper Lig: Zirve yarışında puan farkı kritik eşikte. Şampiyonluk adayları haftanın zorlu deplasman maçları için taktik çalışmalarını sürdürüyor.
• Avrupa Kupaları: Temsilcilerimizin Şampiyonlar Ligi, Avrupa Ligi ve Konferans Ligi grup ve eleme maçları öncesi kadro hazırlıkları tamamlandı.
• A Millî Futbol Takımı: Uluslar Ligi ve Dünya Kupası grup elemeleri öncesinde teknik heyet genç yetenekleri yakından takip ediyor.
• Basketbol ve Voleybol: EuroLeague'de Türk takımları kritik haftaya girerken, Filenin Sultanları uluslararası turnuva takvimine moralli hazırlanıyor.
        """.trimIndent()
    }

    /**
     * Sinema & Dizi Dünyası
     */
    fun getCinemaNews(): String {
        val dateStr = getTodayDateStr()
        return """
🎬 SİNEMA VE DİZİ DÜNYASI
Tarih: $dateStr

• Vizyondaki Yapımlar: Hafta sonu gişesinde Türk yapımı komedi ve dram filmleri liderliği korurken, yüksek bütçeli bilim kurgu ve aksiyon filmleri sinemaseverlerden yoğun ilgi görüyor.
• Dijital Platformlar: Yerli ve yabancı dijital yayıncılarda yeni sezon dizileri yayına girdi; tarihi dönem ve polisiye gerilim türleri izlenme listelerinde zirvede.
• Ödül Sezonu & Festivaller: Ulusal ve uluslararası film festivallerinde Türk yönetmenlerin bağımsız yapımları jüri özel ödüllerine layık görüldü.
• Beklenen Filmler: Yılın son çeyreğinde vizyona girecek kült devam filmlerinin resmi fragmanları rekor izlenme sayılarına ulaştı.
        """.trimIndent()
    }

    /**
     * Oyun Dünyası (PC, Konsol, Mobil, E-Spor)
     */
    fun getGamingNews(): String {
        val dateStr = getTodayDateStr()
        return """
🎮 OYUN DÜNYASI VE E-SPOR GELİŞMELERİ
Tarih: $dateStr

• Steam ve Dijital Mağazalar: Sezon indirimleri büyük ilgi görürken, hayatta kalma ve açık dünya rol yapma oyunları eş zamanlı oyuncu rekorları kırdı.
• Yeni Nesil Konsollar: PlayStation ve Xbox platformları için optimize edilen yeni nesil grafik güncellemeleri ve yapay zeka destekli kare oluşturma teknolojileri oyunculara sunuldu.
• Yerli Oyun Sektörü: Türk bağımsız oyun stüdyolarının geliştirdiği hikâye odaklı macera oyunları uluslararası platformlarda 'Günün Oyunu' seçildi.
• E-Spor: CS2, Valorant ve League of Legends ulusal liglerinde şampiyonluk playoff heyecanı devam ediyor; Türk takımları Avrupa elemelerinde üst tura yükseldi.
        """.trimIndent()
    }

    /**
     * Finans Dünyası, Borsa (BIST 100) ve Piyasalar
     */
    fun getFinanceNews(): String {
        val dateStr = getTodayDateStr()
        return """
📈 FİNANS, BORSA VE PİYASALAR BÜLTENİ
Tarih: $dateStr

• Borsa İstanbul (BIST 100): Endeks, bankacılık ve sanayi hisseleri öncülüğünde dengeli ve alıcılı bir seyir izliyor; işlem hacmi yüksek seviyesini koruyor.
• Altın ve Emtia: Gram altın ve çeyrek altın, ons fiyatındaki küresel dalgalanmalar ve güvenli liman talebiyle yatay-pozitif bantta hareket ediyor.
• Döviz Piyasası: Dolar/TL ve Euro/TL kurları Merkez Bankası'nın rezerv güçlendirici adımları ve sıkı para politikası çerçevesinde kontrollü seviyelerde işlem görüyor.
• Merkez Bankaları & Faiz: TCMB'nin enflasyon hedefleri doğrultusundaki adımları ile FED ve Avrupa Merkez Bankası'nın faiz kararları piyasalarca yakından izleniyor.
        """.trimIndent()
    }

    /**
     * Geriye dönük uyumluluk için genel özet
     */
    fun getHeadlinesBriefing(): String {
        return getHeadlinesOnly()
    }
}
