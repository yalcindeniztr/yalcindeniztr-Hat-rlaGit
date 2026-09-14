package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.DailyNewsHelper
import com.example.util.DailyRoutinePdfHelper
import com.example.util.DailyRoutinePlanner
import com.example.util.GeneralKnowledgeHelper
import com.example.util.NearbyPlacesHelper
import com.example.util.WeatherHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
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
        val isChatMode = com.example.util.UstaSessionState.isChatMode
        val greeting = if (isChatMode) "Dostum, " else if (sessionData.userNick.isNotBlank()) "Sayın Patronum ${sessionData.userNick}, " else "Sayın Patronum, "
        val db = sessionData.db

        // -------------------------------------------------------------------------
        // 1. NÖBETÇİ ECZANE & SAĞLIK DESTEĞİ (EN YAKIN 3 ECZANE & NAVİGASYON)
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("eczane") || lowerQuery.contains("nobetci") || lowerQuery.contains("nöbetçi") || lowerQuery.contains("ilaç nereden")) {
            val district = sessionData.userDistrict.ifBlank { "Merkez" }
            val city = sessionData.userCity.ifBlank { "Bulunduğunuz İl" }
            val top3 = NearbyPlacesHelper.getTop3NearbyPlaces(context, sessionData.userLat, sessionData.userLng, query)
            val top1 = top3.firstOrNull()

            // Kullanıcı nöbetçi eczane sorduğunda en yakın eczaneye canlı Google Haritalar navigasyonunu başlat
            if (top1 != null) {
                NearbyPlacesHelper.openGoogleMapsNavigation(context, top1.name, top1.lat, top1.lng, top1.address)
            }

            val reply = buildString {
                append("🏥 **${greeting}T.C. Sağlık Bakanlığı ve TİTCK nöbet çizelgelerine uygun olarak $city $district bölgesindeki en yakın 3 nöbetçi eczane:**\n\n")
                top3.forEachIndexed { idx, p ->
                    append("${idx + 1}. **${p.name}**\n")
                    append("   • ${p.typeLabel} (${p.distanceMeters} metre mesafede)\n")
                    append("   • Adres: ${p.address}\n\n")
                }
                append("🗺️ **İlk sıradaki ${top1?.name ?: "nöbetçi eczane"} için Google Haritalar canlı yol tarifi başlatıldı.**\n")
                append("💡 Diğer eczanelere gitmek için alttaki **'Yol Tarifi Al'** veya doğrudan aramak için **'Telefon'** butonunu kullanabilirsiniz.")
            }

            val speech = "${greeting}$district bölgesinde en yakın nöbetçi eczane olan ${top1?.name ?: "eczaneniz"} için Google Haritalar canlı yol tarifini başlattım. En yakın 3 nöbetçi eczane ekranda hazır."
            return DrawerResult(
                replyText = reply,
                recommendedPlaces = top3,
                actionSummary = "🏥 Nöbetçi Eczane Navigasyonu: ${top1?.name ?: "$city $district"}",
                speechText = speech
            )
        }

        // -------------------------------------------------------------------------
        // 2. GÜNLÜK BRİFİNG & PROGRAMLAMA MOTORU (Jarvis Operasyonel Raporu)
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("brifing") || lowerQuery.contains("günün özeti") || lowerQuery.contains("sabah raporu")) {
            val now = Calendar.getInstance(Locale.forLanguageTag("tr-TR"))
            val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
            val monthName = SimpleDateFormat("MMMM", Locale.forLanguageTag("tr-TR")).format(now.time)
            val year = now.get(Calendar.YEAR)
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.forLanguageTag("tr-TR")).format(now.time)
            val timeStr = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(now.time)
            val timeHeader = "📅 [Sistem Bilgisi: Bugün $dayOfMonth $monthName $year $dayOfWeek, Saat: $timeStr]"

            // Ajanda Maddeleri
            val calendarEvents = NearbyPlacesHelper.readUpcomingDeviceCalendarEvents(context, 3)
            val agendaSummary = if (calendarEvents.isNotEmpty()) {
                calendarEvents.joinToString("\n") { "   • **${it.title}** (${it.formattedDate})" }
            } else {
                "   • Planlı acil randevu veya toplantı bulunmuyor."
            }

            // Canlı Hava Durumu
            val weatherBrief = WeatherHelper.getLiveWeather(context, sessionData.userLat, sessionData.userLng, sessionData.userCity)

            // Kritik 3 Haber Başlığı
            val headlines = DailyNewsHelper.getHeadlinesOnly().lines().filter { it.isNotBlank() }.take(3).joinToString("\n") { "   $it" }

            val briefingText = buildString {
                append("🎖️ **GÜNLÜK OPERASYONEL BRİFİNG**\n")
                append("$timeHeader\n\n")
                append("📌 **Günün Kritik Ajanda Maddeleri:**\n")
                append("$agendaSummary\n\n")
                append("🌤️ **Hava Durumu:**\n")
                append("   ${weatherBrief.lines().firstOrNull() ?: weatherBrief}\n\n")
                append("📰 **Günün Önemli Manşetleri:**\n")
                append("$headlines\n\n")
                append("💡 ${greeting}gününüzü en yüksek verimle yönetmeniz için tüm sistemler devrededir.")
            }

            val voiceBriefing = "${greeting}günün operasyonel brifingi hazır. Bugün $dayOfMonth $monthName $dayOfWeek, saat $timeStr. ${if (calendarEvents.isNotEmpty()) "Ajandanızda ${calendarEvents.size} yaklaşan randevunuz var." else "Bugün için takviminiz açık."} Hava durumu ve haber özetlerini ekranınızda listeledim."

            return DrawerResult(
                replyText = briefingText,
                actionSummary = "🎖️ Günlük Operasyonel Brifing",
                speechText = voiceBriefing
            )
        }

        if (lowerQuery.contains("günü planla") || lowerQuery.contains("günlük plan") || lowerQuery.contains("rutin") ||
            lowerQuery.contains("bugün ne yap") || lowerQuery.contains("programım") || lowerQuery.contains("günlük program") ||
            lowerQuery.contains("günümü planla")) {

            // A4 PDF üretimi ve otomatik ekranda açılması
            val (pdfFile, pdfStatusNote) = DailyRoutinePdfHelper.createDailyPlanPdf(context, sessionData.userNick)

            if (lowerQuery.contains("işle") || lowerQuery.contains("kaydet") || lowerQuery.contains("kur")) {
                val scheduleSummary = DailyRoutinePlanner.scheduleFullRoutine(context)
                val reply = "🗓️ ${greeting}günlük dengeli yaşam ve çalışma rutinleriniz alarmlarınıza ve yerel takviminize işlendi.\n\n$pdfStatusNote\n\n$scheduleSummary"
                val speech = "${greeting}günlük rutinlerinizin A4 PDF programını hazırlayıp ekranda açtım; ayrıca takviminize ve sesli alarmlarınıza işledim."
                return DrawerResult(replyText = reply, actionSummary = "📄 Günlük Plan PDF & Alarmlar Hazır", speechText = speech)
            } else {
                val fullPlan = DailyRoutinePlanner.getDailyPlanBriefing(sessionData.userNick)
                val voiceSummary = "${greeting}günlük çalışma ve yaşam programınızı resmi A4 PDF formatında hazırlayarak ekranda açtım."
                val reply = "$pdfStatusNote\n\n$fullPlan"
                return DrawerResult(
                    replyText = reply,
                    actionSummary = "📄 Günlük Plan PDF Açıldı",
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
            val speech = if (isChatMode) "Selam dostum! Ben ATİLA. Sohbet modundayız, nasılsın, nasıl gidiyor?" else "Merhaba $greeting Ben ATİLA. Tüm sistemler devrede ve emrinizdeyim. Nasıl yardımcı olabilirim?"
            return DrawerResult(
                replyText = "⚡ **Sistemler Aktif**\n\n$speech",
                actionSummary = "⚡ ATİLA Çevrimiçi",
                speechText = speech
            )
        }

        if (lowerQuery.contains("nasılsın") || lowerQuery.contains("ne haber") || lowerQuery.contains("naber") ||
            lowerQuery.contains("ne var ne yok") || lowerQuery.contains("nasıl gidiyor") || lowerQuery.contains("durum ne")) {
            val speech = if (isChatMode) "Bomba gibiyim dostum! İşlemcilerim tam gaz çalışıyor. Sen nasılsın, keyifler yerinde mi?" else "Tüm işlem çekirdeklerim ve bellek modüllerim tam kapasite devrede $greeting Sizin için çalışmaya hazırım."
            return DrawerResult(
                replyText = "🟢 **Çalışma Durumu: Mükemmel**\n\n$speech",
                actionSummary = "⚡ Durum: Mükemmel",
                speechText = speech
            )
        }

        if (lowerQuery.contains("moralim bozuk") || lowerQuery.contains("canım sıkkın") || lowerQuery.contains("çok yoruldum") ||
            lowerQuery.contains("stresliyim") || lowerQuery.contains("üzgünüm")) {
            val speech = if (isChatMode) "Canını sıkma dostum, her zorluğun arkasından güzel günler gelir. Ben buradayım, anlat dinleyeyim!" else "Her güçlüğün ardından bir ferahlık gelir $greeting Ben buradayım, zihninizi rahatlatmak ve işlerinizi kolaylaştırmak için emrinizi bekliyorum."
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
        // 5. YEMEK TARİFLERİ & VİDEOLU REHBER (RECIPE HELPER & YOUTUBE)
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("yemek") || lowerQuery.contains("tarif") || lowerQuery.contains("ne pişir") ||
            lowerQuery.contains("akşam ne") || lowerQuery.contains("ne yesek") || lowerQuery.contains("çorba") ||
            lowerQuery.contains("fasulye") || lowerQuery.contains("karnıyarık") || lowerQuery.contains("menemen") ||
            lowerQuery.contains("kebap") || lowerQuery.contains("köfte") || lowerQuery.contains("tatlı") || lowerQuery.contains("sütlaç")) {
            
            val recipe = com.example.util.RecipeHelper.findRecipeOrRecommend(query)
            com.example.util.UstaSessionState.lastSuggestedRecipe = recipe

            if (lowerQuery.contains("video") || lowerQuery.contains("izle") || lowerQuery.contains("nasıl yapılır video")) {
                val (_, msg) = com.example.util.AppLauncherHelper.searchAndPlayYouTube(context, recipe.youtubeQuery)
                return DrawerResult(
                    replyText = "🎬 **${recipe.title} Videolu Tarifi Açılıyor:**\n\n$msg",
                    actionSummary = "🎬 Video Tarif: ${recipe.title}",
                    speechText = "$greeting${recipe.title} videolu yapılış tarifini YouTube'da açıyorum."
                )
            }

            val (replyText, speechText) = com.example.util.RecipeHelper.getRecipeBriefing(recipe, greeting)
            return DrawerResult(
                replyText = replyText,
                actionSummary = "🍲 Tarif: ${recipe.title}",
                speechText = speechText
            )
        }

        // -------------------------------------------------------------------------
        // 5.5. SİRKADİYEN DÖNGÜ & BİYOLOJİK RİTİM VE SAĞLIK
        // -------------------------------------------------------------------------
        if (lowerQuery.contains("su iç") || lowerQuery.contains("duruş") || lowerQuery.contains("postür") ||
            lowerQuery.contains("mola") || lowerQuery.contains("dinlen") || lowerQuery.contains("göz dinlendir") ||
            lowerQuery.contains("biyolojik") || lowerQuery.contains("ritim") || lowerQuery.contains("sirkadiyen")) {
            val circadianMsg = "${greeting}biyolojik ritminiz ve çalışma veriminiz için: Bir bardak su içmeyi, omuz ve omurga duruşunuzu dikleştirmeyi ve 20 saniye uzağa odaklanarak gözlerinizi dinlendirmeyi unutmayın efendim."
            return DrawerResult(
                replyText = "🧘 **Biyolojik Ritim & Sirkadiyen Sağlık:**\n\n$circadianMsg\n\n• 💧 **Hidrasyon:** 1 Bardak Su\n• 🧘‍♂️ **Postür:** Omuzlar geride, omurga dik\n• 👀 **20-20-20 Kuralı:** Göz dinlendirme molası",
                actionSummary = "🧘 Biyolojik Ritim Desteği",
                speechText = circadianMsg
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
        // 8. KAPSAMLI ANSİKLOPEDİK, TARİH, BİLİM VE MEVZUAT BİLGİ MOTORU
        // -------------------------------------------------------------------------
        // 8.1. GeneralKnowledgeHelper ile Çevrimdışı Nokta Atışı Yanıt
        val staticKnowledge = GeneralKnowledgeHelper.answerQuery(query, greeting)
        if (staticKnowledge != null) {
            val reply = "💡 **${staticKnowledge.title} (${staticKnowledge.category})**\n\n${staticKnowledge.fullContent}"
            return DrawerResult(
                replyText = reply,
                actionSummary = "💡 ${staticKnowledge.title}",
                speechText = staticKnowledge.shortSpeech
            )
        }

        // 8.2. Room Veritabanı Resmi Mevzuat Taraması (657, ÖMK, MEB, Sendika)
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
