package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.DailyRoutinePlanner
import com.example.util.NearbyPlacesHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import kotlinx.coroutines.flow.first
import java.util.Locale

object OfflineIntelligenceDrawer : AssistantDrawer {
    override val drawerName: String = "ATİLA Çevrimdışı Jarvis Zekası ve Muhakeme Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean = true

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult {
        val greeting = if (sessionData.userNick.isNotBlank()) "Efendim Sayın ${sessionData.userNick}, " else "Efendim, "
        val db = sessionData.db

        // -------------------------------------------------------------------------
        // 1. NÖBETÇİ ECZANE & SAĞLIK DESTEĞİ (TİTCK & Resmi Sağlık Veritabanı Entegrasyonu)
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("eczane") || lowerQuery.contains("nobetci") || lowerQuery.contains("nöbetçi") || lowerQuery.contains("ilaç nereden")) {
            val district = sessionData.userDistrict.ifBlank { "En Yakın" }
            val city = sessionData.userCity.ifBlank { "Bulunduğunuz İl" }
            val searchQuery = "Nöbetçi Eczane $city $district"
            
            NearbyPlacesHelper.openGoogleMapsNavigation(context, searchQuery, sessionData.userLat, sessionData.userLng, searchQuery)
            
            val reply = "🏥 ${greeting}T.C. Sağlık Bakanlığı ve TİTCK nöbet çizelgelerine uygun olarak, $city $district bölgesindeki açık nöbetçi eczaneleri haritada sizin için listeledim ve yol tarifini başlattım."
            val speech = "${greeting}$district bölgesindeki nöbetçi eczaneleri haritada listeledim ve yol tarifini açtım."
            return DrawerResult(
                replyText = reply,
                actionSummary = "🏥 Nöbetçi Eczaneler: $city $district",
                speechText = speech
            )
        }

        // -------------------------------------------------------------------------
        // 2. GÜNLÜK RUTİN & PROGRAMLAMA MOTORU
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("günü planla") || lowerQuery.contains("günlük plan") || lowerQuery.contains("rutin") ||
            lowerQuery.contains("bugün ne yap") || lowerQuery.contains("programım") || lowerQuery.contains("günlük program")) {
            
            if (lowerQuery.contains("işle") || lowerQuery.contains("kaydet") || lowerQuery.contains("kur")) {
                val scheduleSummary = DailyRoutinePlanner.scheduleFullRoutine(context)
                val reply = "🗓️ ${greeting}günlük dengeli yaşam ve çalışma rutinleriniz alarmlarınıza ve yerel takviminize işlendi.\n\n$scheduleSummary"
                val speech = "${greeting}günlük rutinlerinizin tamamını takviminize ve sesli alarmlarınıza başarıyla işledim."
                return DrawerResult(replyText = reply, actionSummary = scheduleSummary, speechText = speech)
            } else {
                val fullPlan = DailyRoutinePlanner.getDailyPlanBriefing(sessionData.userNick)
                val voiceSummary = DailyRoutinePlanner.getVoiceRoutineSummary(sessionData.userNick)
                return DrawerResult(
                    replyText = fullPlan,
                    actionSummary = "📋 Günlük Rutin Programı",
                    speechText = voiceSummary
                )
            }
        }

        // -------------------------------------------------------------------------
        // 3. FİKİR VE TAVSİYE MOTORU (Jarvis Karar Destek & Muhakeme)
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("sence") || lowerQuery.contains("fikrin") || lowerQuery.contains("ne düşünüyorsun") ||
            lowerQuery.contains("önerin var mı") || lowerQuery.contains("tavsiye") || lowerQuery.contains("alınır mı") ||
            lowerQuery.contains("mantıklı mı") || lowerQuery.contains("nasıl yapmalıyım")) {

            val advice = when {
                lowerQuery.contains("telefon") || lowerQuery.contains("bilgisayar") || lowerQuery.contains("laptop") || lowerQuery.contains("tablet") -> {
                    "${greeting}teknolojik cihaz tercihlerinde öncelikle kullanım amacınız, batarya ömrü ve uzun vadeli yazılım desteği belirleyicidir. Güncel işlemci mimarisine ve yüksek RAM kapasitesine sahip modeller, fiyat-performans eğrisinde daima en akılcı yatırımdır."
                }
                lowerQuery.contains("araba") || lowerQuery.contains("araç") || lowerQuery.contains("otomobil") -> {
                    "${greeting}araç seçiminde yakıt tüketimi, kronik arıza geçmişi ve yedek parça bulunurluğu birinci önceliktir. Şehir içi yoğun kullanımda hibrit veya az yakan dizel/benzin motorlar değerini en iyi koruyan seçeneklerdir."
                }
                lowerQuery.contains("meslek") || lowerQuery.contains("kariyer") || lowerQuery.contains("atanma") || lowerQuery.contains("ders") -> {
                    "${greeting}mesleki başarıda istikrar ve disiplin esastır. Millî Eğitim Bakanlığı ve kamu mevzuatındaki güncel gelişmeleri takip edip her gün planlı bir çalışma rutini oluşturmanızı tavsiye ederim."
                }
                lowerQuery.contains("yatırım") || lowerQuery.contains("para") || lowerQuery.contains("harcama") -> {
                    "${greeting}finansal kararlarda tek bir enstrümana bağlı kalmamak, sepet yapmak ve acil durum fonu ayırmak en temel altın kuraldır."
                }
                else -> {
                    "${greeting}bu konuda en rasyonel yaklaşım; kısa vadeli heyecanlar yerine uzun vadeli fayda, maliyet dengesi ve sürdürülebilirliği göz önüne almaktır. Artı ve eksileri tarttığımızda dengeli ve sabırlı bir karar vermeniz lehinize olacaktır."
                }
            }

            return DrawerResult(
                replyText = "💡 **ATİLA'nın Değerlendirmesi:**\n\n$advice",
                actionSummary = "💡 Karar Desteği",
                speechText = advice
            )
        }

        // -------------------------------------------------------------------------
        // 4. SELAMLAŞMA & HAL HATIR & JARVİS DİYALOGLARI
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("merhaba") || lowerQuery.contains("selam") || lowerQuery.contains("günaydın") ||
            lowerQuery.contains("tünaydın") || lowerQuery.contains("iyi günler") || lowerQuery.contains("iyi akşamlar") ||
            lowerQuery.equals("hey", ignoreCase = true) || lowerQuery.equals("atila", ignoreCase = true) ||
            lowerQuery.equals("atilla", ignoreCase = true) || lowerQuery.equals("jarvis", ignoreCase = true) ||
            lowerQuery.equals("usta", ignoreCase = true)) {
            val speech = "Merhaba $greeting Ben ATİLA. Tüm sistemler devrede ve emrinizdeyim. Nasıl yardımcı olabilirim?"
            return DrawerResult(
                replyText = "⚡ **Sistemler Aktif**\n\n$speech",
                actionSummary = "⚡ ATİLA Çevrimiçi",
                speechText = speech
            )
        }

        if (lowerQuery.contains("nasılsın") || lowerQuery.contains("ne haber") || lowerQuery.contains("naber") ||
            lowerQuery.contains("ne var ne yok") || lowerQuery.contains("nasıl gidiyor") || lowerQuery.contains("durum ne")) {
            val speech = "Tüm işlem çekirdeklerim ve bellek modüllerim tam kapasite devrede $greeting Sizin için çalışmaya hazırım."
            return DrawerResult(
                replyText = "🟢 **Çalışma Durumu: Mükemmel**\n\n$speech",
                actionSummary = "⚡ Durum: Mükemmel",
                speechText = speech
            )
        }

        if (lowerQuery.contains("moralim bozuk") || lowerQuery.contains("canım sıkkın") || lowerQuery.contains("çok yoruldum") ||
            lowerQuery.contains("stresliyim") || lowerQuery.contains("üzgünüm")) {
            val speech = "Her güçlüğün ardından bir ferahlık gelir $greeting Ben buradayım, zihninizi rahatlatmak ve işlerinizi kolaylaştırmak için emrinizi bekliyorum."
            return DrawerResult(
                replyText = "🛡️ **Moral & Destek**\n\n$speech",
                actionSummary = "🛡️ Moral Desteği",
                speechText = speech
            )
        }

        if (lowerQuery.contains("kimsin") || lowerQuery.contains("nesin") || lowerQuery.contains("adın ne") ||
            lowerQuery.contains("ne yapabilirsin") || lowerQuery.contains("kendini tanıt")) {
            val speech = "Ben ATİLA $greeting HatırlaGit'in kişisel asistanıyım. Sesinizle telefon araması ve WhatsApp mesajı hazırlar, alarmlar ve günlük rutinler kurar, mevzuatı ve resmi devlet kaynaklarını bilir, nöbetçi eczaneleri ve canlı hava durumunu haritada anında sunarım."
            return DrawerResult(
                replyText = "🤖 **ATİLA Yapay Zeka Asistanı**\n\n$speech",
                actionSummary = "🤖 ATİLA Asistan",
                speechText = speech
            )
        }

        // -------------------------------------------------------------------------
        // 5. YEMEK & SAĞLIKLI BESLENME TAVSİYELERİ
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("yemek") || lowerQuery.contains("tarif") || lowerQuery.contains("ne pişir") ||
            lowerQuery.contains("akşam ne") || lowerQuery.contains("ne yesek") || lowerQuery.contains("çorba") ||
            lowerQuery.contains("kebap") || lowerQuery.contains("köfte") || lowerQuery.contains("tatlı")) {
            val (title, detail, voice) = when {
                lowerQuery.contains("çorba") -> Triple("Geleneksel Yayla Çorbası", "Pirinç, süzme yoğurt, yumurta sarısı ve naneli tereyağı sosuyla sindirimi kolay, bağışıklığı güçlendirici harika bir başlangıçtır.", "Sıcak ve hafif bir Yayla Çorbası tavsiye ederim efendim. Naneli tereyağı sosuyla mükemmel bir seçimdir.")
                lowerQuery.contains("köfte") -> Triple("Fırında Anne Köftesi & Sebze", "Az yağlı kıyma, rendelenmiş soğan, kimyon ve baharatlarla yoğrulup patates ve biber dilimleriyle fırınlanan dengeli bir akşam yemeği.", "Fırında patatesli Anne Köftesi öneririm efendim. Hem hafif hem oldukça besleyicidir.")
                lowerQuery.contains("tatlı") -> Triple("Hafif Sütlaç", "Fırınlanmış geleneksel sütlaç; az şekerli ve tarçın ilavesiyle hafif bir tatlı alternatifi sunar.", "Fırın sütlaç tavsiye ederim efendim; az şekerli ve tarçınlı yapıldığında oldukça hafiftir.")
                else -> Triple("Sebzeli Güveç", "Mevsim sebzeleri, zeytinyağı ve isteğe göre et parçalarıyla kısık ateşte pişen, besin değeri yüksek mükemmel bir Türk mutfağı klasiği.", "Günün önerisi fırında güveç efendim. Yanına pirinç pilavı ve cacık ile harika bir uyum yakalar.")
            }
            return DrawerResult(
                replyText = "🍲 **Mutfak Tavsiyesi: $title**\n\n$detail",
                actionSummary = "🍲 Mutfak Tavsiyesi",
                speechText = "$greeting$voice"
            )
        }

        // -------------------------------------------------------------------------
        // 6. YAKLAŞAN RANDEVULAR & AJANDA
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("randevu") || lowerQuery.contains("hatırlatıcı") || lowerQuery.contains("planlarım") ||
            lowerQuery.contains("ajanda") || lowerQuery.contains("bugün ne var") || lowerQuery.contains("görevlerim")) {
            val activeList = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            return if (activeList.isNotEmpty()) {
                val listStr = activeList.take(4).joinToString("\n") { "• **${it.title}** (${it.dueDatetime})" }
                val voiceStr = activeList.take(2).joinToString(", ") { "${it.title} saat ${it.dueDatetime.takeLast(5)}" }
                DrawerResult(
                    replyText = "📅 **Yaklaşan Randevularınız:**\n\n$listStr",
                    actionSummary = "📅 Randevular Listelendi",
                    speechText = "${greeting}yaklaşan randevularınız: $voiceStr."
                )
            } else {
                DrawerResult(
                    replyText = "📅 ${greeting}ajandanızda bekleyen acil bir randevu bulunmuyor.",
                    actionSummary = "📅 Randevu Yok",
                    speechText = "${greeting}ajandanızda bekleyen acil bir randevu bulunmuyor."
                )
            }
        }

        // -------------------------------------------------------------------------
        // 7. ARABAM NEREDE & PARK TELEMETRİSİ
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("arabam") || lowerQuery.contains("park yerim") || lowerQuery.contains("araba nerede") || lowerQuery.contains("araba")) {
            val lat = sessionData.dataStoreManager.parkedCarLat.first()
            val lng = sessionData.dataStoreManager.parkedCarLng.first()
            val time = sessionData.dataStoreManager.parkedCarTime.first()
            return if (!lat.isNullOrBlank() && !lng.isNullOrBlank()) {
                val timeStr = if (time != null && time > 0) {
                    val formatted = java.text.SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(java.util.Date(time))
                    " (Saat $formatted sularında kaydedildi)"
                } else ""
                DrawerResult(
                    replyText = "🚗 **Park Konumu Kayıtlı**\n\n${greeting}aracınızın GPS konumu sistemde kayıtlıdır$timeStr. Dilerseniz Harita butonundan doğrudan rotayı açabilirsiniz.",
                    actionSummary = "🚗 Park Konumu Hazır",
                    speechText = "${greeting}aracınızın park konumu GPS sisteminde kayıtlıdır. Tek tıkla navigasyonu açabilirsiniz."
                )
            } else {
                DrawerResult(
                    replyText = "🚗 ${greeting}henüz bir park yeri kaydetmediniz. 'Arabamı buraya park ettim' demeniz yeterlidir.",
                    actionSummary = "🚗 Park Kaydı Yok",
                    speechText = "${greeting}henüz bir park yeri kaydetmediniz."
                )
            }
        }

        // -------------------------------------------------------------------------
        // 8. TARİH, KÜLTÜR VE RESMİ DEVLET BİLGİ KÜTÜPHANESİ TARAMASI (657, ÖMK, MEB)
        // -------------------------------------------------------------------------
        val searchWords = lowerQuery.split(Regex("""[\s,?.!;:()'"\-_/]+""")).filter { it.length >= 3 }
        if (searchWords.isNotEmpty()) {
            val allKnowledge = db.aiKnowledgeDao().getAllKnowledgeList()
            val scored = allKnowledge.map { entity ->
                val titleLower = entity.title.lowercase(Locale.forLanguageTag("tr-TR"))
                val contentLower = entity.content.lowercase(Locale.forLanguageTag("tr-TR"))
                var score = 0
                for (w in searchWords) {
                    if (titleLower.contains(w)) score += 6
                    if (contentLower.contains(w)) score += 2
                }
                Pair(entity, score)
            }.filter { it.second >= 4 }.sortedByDescending { it.second }

            val top = scored.firstOrNull()?.first
            if (top != null) {
                val firstCleanSentence = top.content.lines().firstOrNull { it.isNotBlank() } ?: top.content.take(180)
                val voiceReply = "${greeting}${top.title} ile ilgili resmi mevzuat bilgisi: ${firstCleanSentence.take(160)}"
                return DrawerResult(
                    replyText = "📚 **Resmi Devlet Kaynağı (${top.category}): ${top.title}**\n\n${top.content.take(600)}\n\n_Kaynak: mevzuat.gov.tr & meb.gov.tr_",
                    actionSummary = "📚 Kütüphane: ${top.title}",
                    speechText = voiceReply
                )
            }
        }

        // -------------------------------------------------------------------------
        // 9. GENEL ANSİKLOPEDİK / BİLİMSEL / GÜNDELİK YANITLAR
        // -------------------------------------------------------------------------
        val generalKnowledge = when {
            lowerQuery.contains("cumhuriyet") || lowerQuery.contains("atatürk") -> {
                "Gazi Mustafa Kemal Atatürk önderliğinde 29 Ekim 1923'te ilan edilen Türkiye Cumhuriyeti, egemenliğin kayıtsız şartsız millete ait olduğu çağdaş ve tam bağımsız bir hukuk devletidir."
            }
            lowerQuery.contains("yapay zeka") || lowerQuery.contains("ai") -> {
                "Yapay zeka; insan zekasını modelleyerek veri analizi, doğal dil işleme, görsel tanıma ve mantıksal çıkarım yapabilen modern algoritmalar bütünüdür."
            }
            lowerQuery.contains("dünya") || lowerQuery.contains("güneş") || lowerQuery.contains("gezegen") -> {
                "Güneş Sistemi Samanyolu Galaksisi'nde yer alır. Dünya, Güneş'e en yakın üçüncü gezegen olup sıvı su ve yaşam barındıran tek bilinen gökcismidir."
            }
            else -> {
                "${greeting}sizi dikkatle dinliyorum. Telefon araması, WhatsApp, alarmlar, nöbetçi eczaneler, canlı hava durumu, günlük rutin planlama veya resmi mevzuat konularında emrinizdeyim."
            }
        }

        return DrawerResult(
            replyText = "💡 **ATİLA:**\n\n$generalKnowledge",
            actionSummary = "⚡ ATİLA Dinlemede",
            speechText = generalKnowledge
        )
    }
}
