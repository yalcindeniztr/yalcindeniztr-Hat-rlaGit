package com.example.util.assistant.drawers

import android.content.Context
import com.example.data.DataStoreManager
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import kotlinx.coroutines.flow.first
import java.util.Locale

object OfflineIntelligenceDrawer : AssistantDrawer {
    override val drawerName: String = "Zengin Çevrimdışı Zeka ve Sohbet Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean = true

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult {
        val greeting = if (sessionData.userNick.isNotBlank()) "${sessionData.userNick} dostum, " else "Dostum, "
        val db = sessionData.db

        // 1. Selamlaşma & Hal Hatır
        if (lowerQuery.contains("merhaba") || lowerQuery.contains("selam") || lowerQuery.contains("günaydın") ||
            lowerQuery.contains("tünaydın") || lowerQuery.contains("iyi günler") || lowerQuery.contains("iyi akşamlar") ||
            lowerQuery.equals("hey", ignoreCase = true) || lowerQuery.equals("atilla", ignoreCase = true) || lowerQuery.equals("usta", ignoreCase = true)) {
            return DrawerResult(
                replyText = "Merhaba $greeting Ben Atilla. Size sesle alarm ve takvim kurabilir, MEB mevzuatını anlatabilir, nöbetçi eczane bulabilir veya gezi rotaları önerebilirim. Size nasıl yardımcı olabilirim?"
            )
        }

        if (lowerQuery.contains("nasılsın") || lowerQuery.contains("ne haber") || lowerQuery.contains("naber") ||
            lowerQuery.contains("ne var ne yok") || lowerQuery.contains("nasıl gidiyor") || lowerQuery.contains("keyifler nasıl")) {
            return DrawerResult(
                replyText = "Çok şükür iyiyim $greeting Akıl ve hafıza tam devrede, işlerinizi kolaylaştırmak için hazırım. Siz nasılsınız, bugün ne yapıyoruz?"
            )
        }

        if (lowerQuery.contains("moralim bozuk") || lowerQuery.contains("canım sıkkın") || lowerQuery.contains("çok yoruldum") ||
            lowerQuery.contains("stresliyim") || lowerQuery.contains("üzgünüm") || lowerQuery.contains("dertliyim")) {
            return DrawerResult(
                replyText = "'Sabreden derviş muradına ermiş' derler $greeting Hayat inişli çıkışlı bir yoldur, mühim olan dik durmaktır. Bir yudum çay veya kahve alıp derin bir nefeslenin. Ben her zaman buradayım, yanınızdayım."
            )
        }

        // 2. Kimlik, Tanıtım ve Yetenekler
        if (lowerQuery.contains("kimsin") || lowerQuery.contains("nesin") || lowerQuery.contains("adın ne") ||
            lowerQuery.contains("ne yapabilirsin") || lowerQuery.contains("sen kimsin") || lowerQuery.contains("kendini tanıt")) {
            return DrawerResult(
                replyText = "Ben Atilla; HatırlaGit'in akıllı kişisel asistanıyım. Telefonunuzda doğrudan çalışırım. Sesle alarm ve takvim kurar, arama ve WhatsApp mesajı hazırlar, 657 DMK ve ÖMK mevzuatını bilir, ŞÖK tutanağı ve sınav kağıdı üretir, canlı harita navigasyonu başlatırım."
            )
        }

        // 3. Yemek Tarifleri ve Mutfak
        if (lowerQuery.contains("yemek") || lowerQuery.contains("tarif") || lowerQuery.contains("ne pişir") ||
            lowerQuery.contains("akşam ne") || lowerQuery.contains("ne yesek") || lowerQuery.contains("ne yesem") ||
            lowerQuery.contains("çorba") || lowerQuery.contains("kebap") || lowerQuery.contains("köfte") || lowerQuery.contains("pilav")) {
            val recipeReply = when {
                lowerQuery.contains("çorba") -> "Şöyle sıcacık, şifa dolu bir Yayla Çorbası tavsiye ederim $greeting Pirinci haşlayın; yoğurt, un ve yumurta sarısını çırpıp terbiyesini ekleyin. Üzerine tereyağında kızdırılmış nane gezdirdiniz mi nefis olur. Afiyet olsun!"
                lowerQuery.contains("köfte") -> "Lokum gibi bir Anne Köftesi öneririm $greeting Kıyma, rendelenmiş soğan, bayat ekmek içi, sarımsak, kimyon ve karabiberle en az 10 dakika yoğurun. Yanına da fırında elma dilim patates kondurun. Ziyafet hazır!"
                else -> "Atalarımız 'Can boğazdan gelir' demiş $greeting Bugün size lokum gibi bir Sulu Tas Kebabı tavsiye ederim. Kuşbaşı etleri mühürleyin; arpacık soğan, bir tatlı kaşığı domates salçası ve küp patatesle kısık ateşte 45 dakika pişirin. Yanına tane tane pirinç pilavı ile harika gider. Afiyet olsun!"
            }
            return DrawerResult(replyText = recipeReply)
        }

        // 4. Randevular & Planlar & Görevler
        if (lowerQuery.contains("randevu") || lowerQuery.contains("hatırlatıcı") || lowerQuery.contains("planlarım") ||
            lowerQuery.contains("ajanda") || lowerQuery.contains("bugün ne var") || lowerQuery.contains("görevlerim") || lowerQuery.contains("nelerim var")) {
            val activeList = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            return if (activeList.isNotEmpty()) {
                val listStr = activeList.take(4).joinToString("\n") { "• ${it.title} (${it.dueDatetime})" }
                DrawerResult(
                    replyText = "${greeting}yaklaşan plan ve görevleriniz şunlardır:\n\n$listStr\n\nYeni bir randevu veya alarm isterseniz 'Yarın 14:00'e toplantı ekle' demeniz yeterlidir."
                )
            } else {
                DrawerResult(
                    replyText = "${greeting}şu an bekleyen acil bir randevunuz görünmüyor. 'Bugünün işini yarına bırakma' derler, dilerseniz yeni bir hatırlatıcı kuralım!"
                )
            }
        }

        // 5. Ezan & Namaz Vakitleri
        if (lowerQuery.contains("ezan") || lowerQuery.contains("namaz") || lowerQuery.contains("vakit") || lowerQuery.contains("iftar") || lowerQuery.contains("sahur")) {
            return DrawerResult(
                replyText = "${greeting}günün ezan vakitlerini ve sıradaki vakti Ana Sayfa'daki Ezan Vakti kartından veya Ezan Vakitleri sekmesinden canlı olarak takip edebilirsiniz."
            )
        }

        // 6. Arabam Nerede & Park Konumu
        if (lowerQuery.contains("arabam") || lowerQuery.contains("park yerim") || lowerQuery.contains("araba nerede") || lowerQuery.contains("araba")) {
            val lat = sessionData.dataStoreManager.parkedCarLat.first()
            val lng = sessionData.dataStoreManager.parkedCarLng.first()
            val time = sessionData.dataStoreManager.parkedCarTime.first()
            return if (!lat.isNullOrBlank() && !lng.isNullOrBlank()) {
                val timeStr = if (time != null && time > 0) {
                    val formatted = java.text.SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(java.util.Date(time))
                    " (Saat $formatted civarında kaydedildi)"
                } else ""
                DrawerResult(
                    replyText = "🚗 ${greeting}aracınızın son park konumu hafızamızda kayıtlıdır$timeStr. Ana Sayfa'daki 'Park Halinde' kartından tek tıkla rotanızı başlatabilirsiniz."
                )
            } else {
                DrawerResult(
                    replyText = "${greeting}henüz bir araç park yeri kaydetmediniz. Park ettiğinizde 'Arabamı buraya park ettim' demeniz yeterlidir."
                )
            }
        }

        // 7. Türk Tarihi ve Kültürü
        if (lowerQuery.contains("tarih") || lowerQuery.contains("kurtuluş") || lowerQuery.contains("selçuklu") ||
            lowerQuery.contains("osmanlı") || lowerQuery.contains("atatürk") || lowerQuery.contains("malazgirt") || lowerQuery.contains("çanakkale")) {
            return DrawerResult(
                replyText = "Tarihini bilmeyen milletlerin coğrafyasını başkaları çizer $greeting 1071 Malazgirt ile Anadolu'yu yurt kılan Sultan Alparslan'dan, 1919'da Samsun'da Millî Mücadele'yi ateşleyip cumhuriyeti kuran Gazi Mustafa Kemal Atatürk'e kadar hepsi altın harflerle yazılıdır. Hangi dönemi öğrenmek istersiniz?"
            )
        }

        // 8. Bilim, Evren ve Doğa
        if (lowerQuery.contains("neden") || lowerQuery.contains("nasıl oluşur") || lowerQuery.contains("bilim") ||
            lowerQuery.contains("uzay") || lowerQuery.contains("fizik") || lowerQuery.contains("biyoloji")) {
            return DrawerResult(
                replyText = "Evren muazzam bir nizam ve sebep-sonuç bağıyla işler $greeting Merak ettiğiniz soruyu biraz daha detaylandırırsanız hikmetini ve mantığını beraber çözebiliriz."
            )
        }

        // 9. Kütüphane Taraması (Esnek Eşleşme - 657, ÖMK, Sendika, MEB, UNESCO vb.)
        val searchWords = lowerQuery.split(Regex("""[\s,?.!;:()'"\-_/]+""")).filter { it.length >= 3 }
        if (searchWords.isNotEmpty()) {
            val allKnowledge = db.aiKnowledgeDao().getAllKnowledgeList()
            val scored = allKnowledge.map { entity ->
                val titleLower = entity.title.lowercase(Locale("tr", "TR"))
                val contentLower = entity.content.lowercase(Locale("tr", "TR"))
                var score = 0
                for (w in searchWords) {
                    if (titleLower.contains(w)) score += 5
                    if (contentLower.contains(w)) score += 2
                }
                Pair(entity, score)
            }.filter { it.second >= 4 }.sortedByDescending { it.second }

            val top = scored.firstOrNull()?.first
            if (top != null) {
                val matchedParagraph = top.content.lines()
                    .filter { line -> searchWords.any { w -> line.contains(w, ignoreCase = true) } }
                    .take(3)
                    .joinToString("\n")
                val text = if (matchedParagraph.isNotBlank()) {
                    "📌 ${top.title}:\n$matchedParagraph"
                } else {
                    "📌 ${top.title}:\n${top.content.take(350)}..."
                }
                return DrawerResult(
                    replyText = "${greeting}kütüphanemizden ilgili maddeyi buldum:\n\n$text",
                    actionSummary = "📚 Kütüphane: ${top.title}"
                )
            }
        }

        // 10. Bilgece ve Yapıcı Genel Çevrimdışı Yanıt (Asla tek tip 'Buyrun dostum' döngüsüne girmez)
        return DrawerResult(
            replyText = "${greeting}sizi dinliyorum. Bana sesle alarm ve takvim kurdurabilir, 'Gezilecek yerler nerede?' diye sorabilir veya MEB, 657 ve ÖMK mevzuatıyla ilgili sorular yöneltebilirsiniz. Ne yapmak istersiniz?"
        )
    }
}
