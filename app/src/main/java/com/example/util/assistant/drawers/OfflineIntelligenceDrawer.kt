package com.example.util.assistant.drawers

import android.content.Context
import com.example.data.DataStoreManager
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import kotlinx.coroutines.flow.first
import java.util.Locale

object OfflineIntelligenceDrawer : AssistantDrawer {
    override val drawerName: String = "Jarvis Çevrimdışı Zeka ve Sadık Asistan Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean = true

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult {
        val greeting = if (sessionData.userNick.isNotBlank()) "Efendim ${sessionData.userNick}, " else "Efendim, "
        val db = sessionData.db

        // 1. Selamlaşma & Hal Hatır
        if (lowerQuery.contains("merhaba") || lowerQuery.contains("selam") || lowerQuery.contains("günaydın") ||
            lowerQuery.contains("tünaydın") || lowerQuery.contains("iyi günler") || lowerQuery.contains("iyi akşamlar") ||
            lowerQuery.equals("hey", ignoreCase = true) || lowerQuery.equals("jarvis", ignoreCase = true) ||
            lowerQuery.equals("atilla", ignoreCase = true) || lowerQuery.equals("usta", ignoreCase = true)) {
            return DrawerResult(
                replyText = "Merhaba $greeting Sistemler aktif ve emrinizdeyim. Bir isteğiniz var mı?",
                actionSummary = "⚡ Jarvis Çevrimiçi"
            )
        }

        if (lowerQuery.contains("nasılsın") || lowerQuery.contains("ne haber") || lowerQuery.contains("naber") ||
            lowerQuery.contains("ne var ne yok") || lowerQuery.contains("nasıl gidiyor") || lowerQuery.contains("durum ne")) {
            return DrawerResult(
                replyText = "Tüm alt sistemler ve bellek modülleri tam kapasite devrede $greeting Emrinizi yerine getirmeye hazırım.",
                actionSummary = "⚡ Sistem Durumu: Mükemmel"
            )
        }

        if (lowerQuery.contains("moralim bozuk") || lowerQuery.contains("canım sıkkın") || lowerQuery.contains("çok yoruldum") ||
            lowerQuery.contains("stresliyim") || lowerQuery.contains("üzgünüm")) {
            return DrawerResult(
                replyText = "Her zorluğun üstesinden gelecek iradeye sahipsiniz $greeting Ben buradayım, işlerinizi hafifletmek için bir komutunuz yeterli.",
                actionSummary = "🛡️ Moral Desteği"
            )
        }

        // 2. Kimlik ve Yetenekler
        if (lowerQuery.contains("kimsin") || lowerQuery.contains("nesin") || lowerQuery.contains("adın ne") ||
            lowerQuery.contains("ne yapabilirsin") || lowerQuery.contains("kendini tanıt")) {
            return DrawerResult(
                replyText = "Ben Jarvis $greeting HatırlaGit'in yüksek teknolojili kişisel asistanıyım. Alarm, takvim, arama, MEB mevzuatı ve navigasyon yönetiminiz için buradayım.",
                actionSummary = "🤖 Jarvis Asistan"
            )
        }

        // 3. Yemek & Mutfak
        if (lowerQuery.contains("yemek") || lowerQuery.contains("tarif") || lowerQuery.contains("ne pişir") ||
            lowerQuery.contains("akşam ne") || lowerQuery.contains("ne yesek") || lowerQuery.contains("çorba") ||
            lowerQuery.contains("kebap") || lowerQuery.contains("köfte")) {
            val recipeReply = when {
                lowerQuery.contains("çorba") -> "Sıcak bir Yayla Çorbası tavsiye ederim $greeting Pirinç, süzme yoğurt ve naneli tereyağı sosuyla mükemmel bir tercihtir."
                lowerQuery.contains("köfte") -> "Geleneksel Anne Köftesi öneririm $greeting İyi yoğrulmuş kıyma, soğan, ekmek içi ve kimyon ile hızlıca hazırlayabilirsiniz."
                else -> "Günün önerisi Sulu Tas Kebabı $greeting Arpacık soğan, patates ve küp etlerle kısık ateşte hazırlanan besleyici bir seçenek."
            }
            return DrawerResult(replyText = recipeReply, actionSummary = "🍲 Mutfak Tavsiyesi")
        }

        // 4. Randevular & Planlar
        if (lowerQuery.contains("randevu") || lowerQuery.contains("hatırlatıcı") || lowerQuery.contains("planlarım") ||
            lowerQuery.contains("ajanda") || lowerQuery.contains("bugün ne var") || lowerQuery.contains("görevlerim")) {
            val activeList = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            return if (activeList.isNotEmpty()) {
                val listStr = activeList.take(3).joinToString("; ") { "${it.title} (${it.dueDatetime})" }
                DrawerResult(
                    replyText = "${greeting}yaklaşan randevularınız: $listStr.",
                    actionSummary = "📅 Randevular Listelendi"
                )
            } else {
                DrawerResult(
                    replyText = "${greeting}bekleyen acil bir randevunuz bulunmuyor.",
                    actionSummary = "📅 Randevu Yok"
                )
            }
        }

        // 5. Ezan & Namaz Vakitleri
        if (lowerQuery.contains("ezan") || lowerQuery.contains("namaz") || lowerQuery.contains("vakit") || lowerQuery.contains("iftar") || lowerQuery.contains("sahur")) {
            return DrawerResult(
                replyText = "${greeting}canlı ezan vakitlerini ve kalan süreyi Ana Sayfa'daki Ezan Vakti kartından anlık görebilirsiniz.",
                actionSummary = "🕌 Ezan Vakti Bilgisi"
            )
        }

        // 6. Arabam Nerede & Park Konumu
        if (lowerQuery.contains("arabam") || lowerQuery.contains("park yerim") || lowerQuery.contains("araba nerede") || lowerQuery.contains("araba")) {
            val lat = sessionData.dataStoreManager.parkedCarLat.first()
            val lng = sessionData.dataStoreManager.parkedCarLng.first()
            val time = sessionData.dataStoreManager.parkedCarTime.first()
            return if (!lat.isNullOrBlank() && !lng.isNullOrBlank()) {
                val timeStr = if (time != null && time > 0) {
                    val formatted = java.text.SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(java.util.Date(time))
                    " (Saat $formatted civarında)"
                } else ""
                DrawerResult(
                    replyText = "🚗 ${greeting}aracınızın son konumu GPS telemetrisinde kayıtlıdır$timeStr. Tek tıkla navigasyon başlatabilirsiniz.",
                    actionSummary = "🚗 Park Konumu Hazır"
                )
            } else {
                DrawerResult(
                    replyText = "${greeting}henüz bir park yeri kaydetmediniz. 'Arabamı buraya park ettim' demeniz yeterlidir.",
                    actionSummary = "🚗 Park Kaydı Yok"
                )
            }
        }

        // 7. Tarih ve Kültür
        if (lowerQuery.contains("tarih") || lowerQuery.contains("kurtuluş") || lowerQuery.contains("atatürk") || lowerQuery.contains("çanakkale") || lowerQuery.contains("malazgirt")) {
            return DrawerResult(
                replyText = "${greeting}tarihimiz 1071 Malazgirt'ten 1923 Cumhuriyetimize kadar büyük zaferlerle doludur. İncelemek istediğiniz özel bir konu var mı?",
                actionSummary = "🏛️ Tarih Bilgisi"
            )
        }

        // 8. Kütüphane Taraması (657, ÖMK, MEB vb.)
        val searchWords = lowerQuery.split(Regex("""[\s,?.!;:()'"\-_/]+""")).filter { it.length >= 3 }
        if (searchWords.isNotEmpty()) {
            val allKnowledge = db.aiKnowledgeDao().getAllKnowledgeList()
            val scored = allKnowledge.map { entity ->
                val titleLower = entity.title.lowercase(Locale.forLanguageTag("tr-TR"))
                val contentLower = entity.content.lowercase(Locale.forLanguageTag("tr-TR"))
                var score = 0
                for (w in searchWords) {
                    if (titleLower.contains(w)) score += 5
                    if (contentLower.contains(w)) score += 2
                }
                Pair(entity, score)
            }.filter { it.second >= 4 }.sortedByDescending { it.second }

            val top = scored.firstOrNull()?.first
            if (top != null) {
                val firstSentence = top.content.lines().firstOrNull { it.isNotBlank() } ?: top.content.take(180)
                return DrawerResult(
                    replyText = "${greeting}${top.title} maddesi gereğince: ${firstSentence.take(200)}",
                    actionSummary = "📚 Kütüphane: ${top.title}"
                )
            }
        }

        // 9. Sadık ve Net Sonuç
        return DrawerResult(
            replyText = "${greeting}emrinizi dinliyorum. Saat alarmı, takvim kaydı, harita konumu veya mevzuat hakkında yardımcı olabilirim.",
            actionSummary = "⚡ Jarvis Dinlemede"
        )
    }
}
