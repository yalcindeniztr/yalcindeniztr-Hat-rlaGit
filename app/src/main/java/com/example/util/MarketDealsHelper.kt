package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object MarketDealsHelper {

    data class MarketDeal(
        val marketName: String,
        val campaignTitle: String,
        val daysActive: String,
        val bogoHighlights: List<String>,
        val discountHighlights: List<String>,
        val tips: String
    )

    private val MARKETS = listOf(
        MarketDeal(
            marketName = "BİM",
            campaignTitle = "Haftanın İndirimleri & Aktüel Ürünler",
            daysActive = "Salı ve Cuma günleri yenilenir",
            bogoHighlights = listOf(
                "Süt ve peynir çeşitlerinde çoklu alım fırsatları",
                "Temizlik ve deterjan ürünlerinde aile boyu paket avantajları"
            ),
            discountHighlights = listOf(
                "Salı Günü: Süt ürünleri, şarküteri, temel gıda ve kişisel bakım ürünlerinde net indirimler",
                "Cuma Günü: Küçük ev aletleri, mutfak gereçleri, ev tekstili ve teknoloji aktüel ürünleri",
                "Dost süt ve yoğurt çeşitlerinde sabit fiyat güvencesi"
            ),
            tips = "BİM aktüel ürünleri Cuma sabahı saat 09:00'da mağazalarda sınırlı stokla satışa çıkar. Erken gitmeniz önerilir."
        ),
        MarketDeal(
            marketName = "A101",
            campaignTitle = "Aldın Aldın & Haftanın Yıldızları",
            daysActive = "Perşembe ve Cumartesi günleri yenilenir",
            bogoHighlights = listOf(
                "10 TL ve üzeri alışverişlerde kasa arkası dev indirimler",
                "Bisküvi ve atıştırmalıklarda 2.si %50 indirimli kampanyaları"
            ),
            discountHighlights = listOf(
                "Perşembe Günü: 'Aldın Aldın' kataloğunda televizyon, beyaz eşya, cep telefonu ve elektronik aletler",
                "Cumartesi Günü: 'Haftanın Yıldızları'nda taze sebze, meyve, et ve temel mutfak erzakları",
                "A101 Plus uygulaması ile her alışverişte bakiye kazanma"
            ),
            tips = "A101 Aldın Aldın kataloğundaki teknoloji ürünleri hızla tükenmektedir."
        ),
        MarketDeal(
            marketName = "ŞOK",
            campaignTitle = "Aman Kaçırma Fırsatları",
            daysActive = "Çarşamba ve Cumartesi günleri yenilenir",
            bogoHighlights = listOf(
                "25 TL ve üzeri alışverişlerde kasa arkası %50'ye varan dev indirimler",
                "Kişisel bakım ve şampuanlarda 1 alana 1 bedava dönemsel fırsatları"
            ),
            discountHighlights = listOf(
                "Çarşamba Günü: 'Aman Kaçırma' mutfak, kamp, bahçe ve oto aksesuarları",
                "Cumartesi Günü: Ev tekstili, temizlik paketleri ve züccaciye indirimleri",
                "Cepte ŞOK uygulaması ile ücretsiz adrese teslimat seçeneği"
            ),
            tips = "ŞOK'ta 25 TL üzeri kasa arkası indirimlerinde zeytinyağı, çay ve yumuşatıcı fırsatları çok avantajlıdır."
        ),
        MarketDeal(
            marketName = "MİGROS",
            campaignTitle = "Gördüğünüze İnanın & 1 Alana 1 Bedava",
            daysActive = "Sürekli güncellenir / Money Kart kampanyaları",
            bogoHighlights = listOf(
                "Seçili diş macunu, şampuan ve duş jellerinde 1 ALANA 1 BEDAVA",
                "Çikolata, dondurma ve bisküvi kategorilerinde dönemsel 1 ALANA 1 BEDAVA",
                "Temizlik ve yumuşatıcılarda '2.si 1 TL' veya '2.si %50 İndirimli'"
            ),
            discountHighlights = listOf(
                "'Gördüğünüze İnanın' kampanyası ile 60 TL ve üzeri alışverişte seçili et, peynir veya deterjanda dev indirim",
                "Salı ve Çarşamba günleri 'Halk Günü' taze meyve ve sebze indirimleri",
                "Money Kart ile kişiye özel bütçe puanları ve indirimler"
            ),
            tips = "Migros'ta 1 alana 1 bedava kampanyaları genellikle ayın ilk ve üçüncü haftasında cuma-pazar günleri yoğunlaşır."
        ),
        MarketDeal(
            marketName = "CARREFOURSA",
            campaignTitle = "CarrefourSA Kart İndirimleri & Hafta Sonu Fırsatları",
            daysActive = "Perşembe - Pazar günleri yenilenir",
            bogoHighlights = listOf(
                "Kozmetik ve kişisel bakımda 1 alana 1 bedava günleri",
                "Seçili temizlik deterjanlarında ikincisi %50 indirimli"
            ),
            discountHighlights = listOf(
                "Hafta Sonu Taze Balık & Kırmızı Et Günleri: Kilo ile alımlarda özel indirimler",
                "CarrefourSA Kart ile kasada anında düşen etiket indirimleri",
                "Temel bakliyat ve zeytinyağında büyük boy kova/teneke indirimleri"
            ),
            tips = "CarrefourSA balık reyonunda Cuma ve Cumartesi günleri taze deniz ürünleri özel fiyatla temizlenip teslim edilir."
        )
    )

    fun getMorningDealsSummary(): String {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val dayName = when (today) {
            Calendar.MONDAY -> "Pazartesi"
            Calendar.TUESDAY -> "Salı"
            Calendar.WEDNESDAY -> "Çarşamba"
            Calendar.THURSDAY -> "Perşembe"
            Calendar.FRIDAY -> "Cuma"
            Calendar.SATURDAY -> "Cumartesi"
            Calendar.SUNDAY -> "Pazar"
            else -> "Bugün"
        }

        val todayDeals = when (today) {
            Calendar.TUESDAY -> "Bugün Salı: BİM Salı indirimleri ve Migros Halk Günü taze manav fırsatları mağazalarda!"
            Calendar.WEDNESDAY -> "Bugün Çarşamba: ŞOK 'Aman Kaçırma' aktüel ürünleri ve Migros sebze-meyve indirimi devrede!"
            Calendar.THURSDAY -> "Bugün Perşembe: A101 'Aldın Aldın' teknoloji ve aktüel ürünleri satışa çıktı!"
            Calendar.FRIDAY -> "Bugün Cuma: BİM büyük Cuma aktüeli ve Migros hafta sonu 1 Alana 1 Bedava kampanyaları başladı!"
            Calendar.SATURDAY -> "Bugün Cumartesi: A101 'Haftanın Yıldızları', ŞOK Cumartesi aktüeli ve CarrefourSA et-balık günleri aktif!"
            Calendar.SUNDAY -> "Bugün Pazar: Hafta sonu market fırsatları ve kasa arkası süper indirimler devam ediyor!"
            else -> "Yeni haftanın market katalogları ve aktüel günleri için takipteyiz."
        }

        return """
            🛒 **GÜNLÜK MARKET İNDİRİMLERİ VE FIRSAT BÜLTENİ** ($dayName)

            📢 **Günün Özeti:** $todayDeals

            ⭐ **Öne Çıkan Kampanyalar:**
            • **BİM:** Salı gıda, Cuma elektronik/züccaciye aktüeli.
            • **A101:** Perşembe Aldın Aldın, Cumartesi taze gıda indirimleri.
            • **ŞOK:** Çarşamba ve Cumartesi aktüeli, 25 TL üzeri kasa arkası sürprizleri.
            • **Migros:** Kişisel bakım ve atıştırmalıklarda 1 ALANA 1 BEDAVA, 'Gördüğünüze İnanın' sepet indirimleri.
            • **CarrefourSA:** Hafta sonu taze balık/et ve deterjan avantajları.

            💡 Belirli bir marketi öğrenmek için 'Usta BİM aktüelini anlat' veya 'Migros 1 alana 1 bedava neler var?' diye sorabilirsiniz.
        """.trimIndent()
    }

    fun getDealsForMarket(query: String): String {
        val cleanQuery = query.uppercase(Locale("tr", "TR"))
        val match = MARKETS.firstOrNull { cleanQuery.contains(it.marketName) }
            ?: return getMorningDealsSummary()

        val bogoStr = match.bogoHighlights.joinToString("\n") { "  • $it" }
        val discountStr = match.discountHighlights.joinToString("\n") { "  • $it" }

        return """
            🛒 **${match.marketName} - ${match.campaignTitle}**
            📅 **Dönem:** ${match.daysActive}

            🎁 **1 Alana 1 Bedava & Çoklu Alım Fırsatları:**
            $bogoStr

            🔥 **Öne Çıkan İndirimler:**
            $discountStr

            💡 **Usta'nın Alışveriş Tüyosu:** ${match.tips}
        """.trimIndent()
    }

    fun getBogoDeals(): String {
        return """
            🎁 **1 ALANA 1 BEDAVA & SÜPER ÇOKLU FIRSATLAR**

            ✨ **Migros:**
            • Seçili şampuan, saç kremi, diş macunlarında 1 ALANA 1 BEDAVA.
            • Bisküvi ve çikolata reyonunda ikincisi %50 indirimli veya 1 Alana 1 Bedava.

            ✨ **ŞOK:**
            • 25 TL ve üzeri alışverişe kasa arkasında dev indirimli ürünler (sıvı yağ, peynir, yumuşatıcı).

            ✨ **A101:**
            • 'Çok Al Az Öde' reyonunda 2'li ve 3'lü alımlarda %30-%50 arası birim fiyat avantajı.

            ✨ **CarrefourSA:**
            • Kişisel bakım ve ev temizlik paketlerinde 1 alana 1 bedava katalog günleri.
        """.trimIndent()
    }
}
