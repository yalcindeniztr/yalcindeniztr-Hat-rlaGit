package com.example.util

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.AlarmClock
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.DataStoreManager
import com.example.data.ReminderEntity
import com.example.data.SavedLocationEntity
import com.example.ui.tabs.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AiResponse(
    val replyText: String,
    val recommendedPlaces: List<NearbyPlace> = emptyList(),
    val actionSummary: String? = null,
    val isSpeechReady: Boolean = true,
    val speechText: String? = null,
    val generatedPdfFile: java.io.File? = null
)

object UstaSessionState {
    var pendingPlaceToSave: NearbyPlace? = null
    var isWaitingForLessonPlanCourse: Boolean = false
    var isWaitingForLessonPlanGrade: Boolean = false
    var pendingLessonPlanTopic: String = "Beylikten Devlete Osmanlı Siyaseti"
    var pendingLocationCoords: Pair<Double, Double>? = null
    var pendingNoteContent: String? = null
    var isWaitingForNoteBody: Boolean = false
    var isVoiceNote: Boolean = false

    // Alarm İnteraktif Durumu
    var isWaitingForAlarmTime: Boolean = false
    var pendingAlarmHour: Int? = null
    var pendingAlarmMinute: Int? = null
    var isWaitingForAlarmLabel: Boolean = false

    // Hatırlatma İnteraktif Durumu
    var isWaitingForReminderTitle: Boolean = false
    var pendingReminderTitle: String? = null
    var isWaitingForReminderTime: Boolean = false
    var pendingReminderTimeMillis: Long? = null
    var pendingReminderDateStr: String? = null
    var isWaitingForReminderNote: Boolean = false

    // Park Yeri Notu İnteraktif Durumu
    var isWaitingForParkNote: Boolean = false
    var pendingParkCoords: Pair<Double, Double>? = null

    // Patron - Asistan vs Sohbet Modu & Yemek İstişaresi
    var isChatMode: Boolean = false
    var isWaitingForFoodPreference: Boolean = false
    var lastSuggestedRecipe: RecipeItem? = null
    var pendingShoppingItems: List<String> = emptyList()

    // Telefon Arama Teyit Durumu
    var isWaitingForCallConfirmation: Boolean = false
    var pendingCallPhoneNumber: String? = null
    var pendingCallPlaceName: String? = null

    // Uygulama Erişim İzni ve Onay Kilidi Durumu
    var isWaitingForAppLaunchConfirmation: Boolean = false
    var pendingAppToLaunchKeyword: String? = null
    var pendingAppToLaunchLabel: String? = null

    // Son Asistan Yanıtı & Üretilen PDF (Arayüz ve Bildirim Köprüsü)
    var lastAssistantResponse: AiResponse? = null
    var lastGeneratedPdfFile: java.io.File? = null
}

object AiAssistantService {

    private fun setSystemAlarm(context: Context, hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                val fallback = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private const val SECURE_KEY_MASK = 0x5A
    private val SECURE_KEY_BYTES = byteArrayOf(
        27, 11, 116, 27, 56, 98, 8, 20, 108, 22, 35, 27, 11, 104, 62, 109, 12, 53, 15, 32, 
        17, 59, 104, 5, 25, 104, 44, 50, 0, 56, 3, 30, 27, 30, 54, 104, 54, 52, 48, 11, 
        60, 46, 105, 46, 16, 44, 55, 119, 99, 12, 56, 23, 27
    )

    private fun getSecureDefaultKey(): String {
        return try {
            val decoded = ByteArray(SECURE_KEY_BYTES.size)
            for (i in SECURE_KEY_BYTES.indices) {
                decoded[i] = (SECURE_KEY_BYTES[i].toInt() xor SECURE_KEY_MASK).toByte()
            }
            String(decoded, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "ATİLA",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val dataStoreManager = DataStoreManager(context)
        val db = AppDatabase.getDatabase(context)

        AiKnowledgeSeeder.seedIfNeeded(context)

        val resolvedAssistantName = "ATİLLA"
        val currentNick = dataStoreManager.userNick.first()?.trim() ?: ""

        val isChatMode = UstaSessionState.isChatMode
        val patronPrefix = if (isChatMode) "Dostum" else if (currentNick.isNotBlank()) "Sayın Hocam $currentNick" else "Sayın Hocam"

        var cleanMsg = userMessage.trim()
        val triggerRegex = Regex("""(?i)^(hey\s+)?(atilla|atila|jarvis|usta|asistan|atilla\s+dinle|atila\s+dinle|usta\s+dinle)[,\s!.:]*""")
        val hadTriggerWord = triggerRegex.find(cleanMsg) != null || cleanMsg.equals("atilla", ignoreCase = true) || cleanMsg.equals("atila", ignoreCase = true)
        cleanMsg = cleanMsg.replace(triggerRegex, "").trim()
        if (cleanMsg.isBlank() || (hadTriggerWord && cleanMsg.isBlank())) {
            val speech = if (isChatMode) "Efendim dostum, seni dinliyorum." else "Buyrun $patronPrefix, emrinizdeyim."
            return@withContext AiResponse(replyText = speech, speechText = speech)
        }

        val lowerMsg = cleanMsg.lowercase(Locale.forLanguageTag("tr-TR"))

        // =========================================================================
        // 0. TELEFON ARAMA TEYİT KONTROLÜ (İNTERAKTİF PATRON DİYALOĞU)
        // =========================================================================
        if (UstaSessionState.isWaitingForCallConfirmation) {
            val phone = UstaSessionState.pendingCallPhoneNumber ?: ""
            val placeName = UstaSessionState.pendingCallPlaceName ?: "İlgili numara"
            val isAffirmative = lowerMsg.contains("evet") || lowerMsg.contains("ara") || lowerMsg.contains("lütfen") || lowerMsg.contains("tamam") || lowerMsg.contains("olur")
            val isNegative = lowerMsg.contains("hayır") || lowerMsg.contains("iptal") || lowerMsg.contains("arama") || lowerMsg.contains("vazgeç")

            if (isAffirmative && phone.isNotBlank()) {
                UstaSessionState.isWaitingForCallConfirmation = false
                UstaSessionState.pendingCallPhoneNumber = null
                UstaSessionState.pendingCallPlaceName = null
                NearbyPlacesHelper.makePhoneCall(context, phone)
                val speech = "$patronPrefix, $placeName için telefon araması başlatılıyor."
                val resp = AiResponse(
                    replyText = "📞 **$placeName ($phone)** aranıyor...",
                    actionSummary = "📞 Aranıyor: $placeName",
                    speechText = speech
                )
                UstaSessionState.lastAssistantResponse = resp
                return@withContext resp
            } else if (isNegative) {
                UstaSessionState.isWaitingForCallConfirmation = false
                UstaSessionState.pendingCallPhoneNumber = null
                UstaSessionState.pendingCallPlaceName = null
                val speech = "Emredersiniz $patronPrefix, arama işlemi iptal edildi."
                val resp = AiResponse(
                    replyText = "❌ Arama işlemi iptal edildi.",
                    actionSummary = "❌ Arama İptal",
                    speechText = speech
                )
                UstaSessionState.lastAssistantResponse = resp
                return@withContext resp
            }
        }

        // =========================================================================
        // 0.0. UYGULAMA ERİŞİM İZNİ VE PATRON TEYİT KONTROLÜ (GÜVENLİK KİLİDİ)
        // =========================================================================
        if (UstaSessionState.isWaitingForAppLaunchConfirmation) {
            val appKeyword = UstaSessionState.pendingAppToLaunchKeyword ?: ""
            val appLabel = UstaSessionState.pendingAppToLaunchLabel ?: appKeyword.ifBlank { "Uygulama" }
            val isNegative = lowerMsg.contains("hayır") || lowerMsg.contains("iptal") || lowerMsg.contains("açma") || lowerMsg.contains("vazgeç")
            val isAffirmative = !isNegative && (lowerMsg.contains("evet") || lowerMsg.contains("aç") || lowerMsg.contains("lütfen") || 
                    lowerMsg.contains("tamam") || lowerMsg.contains("olur") || lowerMsg.contains("izin") || lowerMsg.contains("onay"))

            if (isAffirmative && appKeyword.isNotBlank()) {
                UstaSessionState.isWaitingForAppLaunchConfirmation = false
                UstaSessionState.pendingAppToLaunchKeyword = null
                UstaSessionState.pendingAppToLaunchLabel = null
                val (success, msg) = AppLauncherHelper.openApplicationByVoice(context, appKeyword)
                val speech = if (success) "İzniniz doğrultusunda $appLabel uygulaması açılıyor $patronPrefix." else "$appLabel uygulaması açılamadı."
                val resp = AiResponse(
                    replyText = "🔓 **Patron İzni Onaylandı:** $msg",
                    actionSummary = "🔓 Uygulama Açıldı: $appLabel",
                    speechText = speech
                )
                UstaSessionState.lastAssistantResponse = resp
                AtillaMemoryManager.recordConversation(context, userMessage, resp.replyText)
                return@withContext resp
            } else if (isNegative) {
                UstaSessionState.isWaitingForAppLaunchConfirmation = false
                UstaSessionState.pendingAppToLaunchKeyword = null
                UstaSessionState.pendingAppToLaunchLabel = null
                val speech = "Emredersiniz $patronPrefix, erişim izni verilmediği için uygulama açma işlemi iptal edildi."
                val resp = AiResponse(
                    replyText = "🔒 **Uygulama Erişimi İptal Edildi:** Patron onay vermedi.",
                    actionSummary = "🔒 Erişim İptal Edildi",
                    speechText = speech
                )
                UstaSessionState.lastAssistantResponse = resp
                AtillaMemoryManager.recordConversation(context, userMessage, resp.replyText)
                return@withContext resp
            }
        }

        // =========================================================================
        // 0.0.0. TÜRKÇE İMLA, DEYİMLER, ATASÖZLERİ VE DOĞAL DİL ANALİZİ (LINGUISTICS)
        // =========================================================================
        val linguistic = AtillaTurkishLinguisticsHelper.analyze(context, cleanMsg, patronPrefix)
        if (linguistic.directReply != null) {
            val resp = AiResponse(
                replyText = linguistic.directReply.second,
                actionSummary = when (linguistic.intent) {
                    AtillaTurkishLinguisticsHelper.NormalizedIntent.PLAN_TOMORROW -> "📅 Yarının Planı"
                    AtillaTurkishLinguisticsHelper.NormalizedIntent.PLAN_TODAY -> "📋 Günlük Program"
                    AtillaTurkishLinguisticsHelper.NormalizedIntent.IDIOM_OR_PROVERB -> "📖 Türkçe Deyim & Atasözü"
                    else -> "⚡ Türkçe Analiz"
                },
                speechText = linguistic.directReply.first
            )
            UstaSessionState.lastAssistantResponse = resp
            AtillaMemoryManager.recordConversation(context, userMessage, resp.replyText)
            return@withContext resp
        }

        // =========================================================================
        // 0.0.0.0. ÖĞRENEN ZEKA: KULLANICI PROFİLİ VE KONUŞMA HAFIZASI
        // =========================================================================
        if (lowerMsg.contains("beni tanıyor musun") || lowerMsg.contains("ben kimim") || 
            lowerMsg.contains("kullanıcı profilim") || lowerMsg.contains("hafızan ne diyor") ||
            lowerMsg.contains("hakkımda ne biliyorsun")) {
            val (reply, speech) = AtillaMemoryManager.getProfileBriefing(context, patronPrefix)
            val resp = AiResponse(
                replyText = reply,
                actionSummary = "🧠 Öğrenen Hafıza Raporu",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            AtillaMemoryManager.recordConversation(context, userMessage, reply)
            return@withContext resp
        }

        if (lowerMsg.contains("daha önce ne konuştuk") || lowerMsg.contains("ne konuşmuştuk") || 
            lowerMsg.contains("konuşma geçmişi") || lowerMsg.contains("geçmiş konuşmalar") ||
            lowerMsg.contains("son konuştuklarımız")) {
            val (reply, speech) = AtillaMemoryManager.getRecentConversationsSummary(patronPrefix)
            val resp = AiResponse(
                replyText = reply,
                actionSummary = "💬 Konuşma Geçmişi",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            AtillaMemoryManager.recordConversation(context, userMessage, reply)
            return@withContext resp
        }

        // =========================================================================
        // 0.0.0.1. JARVIS PROTOKOLÜ: SİSTEM TELEMETRİSİ VE TEŞHİS RAPORU (STATUS REPORT)
        // =========================================================================
        if (lowerMsg.contains("durum raporu") || lowerMsg.contains("sistem durumu") || lowerMsg.contains("telemetri") ||
            lowerMsg.contains("cihaz sağlığı") || lowerMsg.contains("batarya durumu") || lowerMsg.contains("şarj durumu") ||
            lowerMsg.contains("teşhis") || lowerMsg.contains("sistem teşhisi") || lowerMsg.contains("rapor ver")) {
            val telemetry = AtillaJarvisCoreHelper.getDeviceTelemetry(context, patronPrefix)
            val resp = AiResponse(
                replyText = telemetry.screenReport,
                actionSummary = "⚡ ATİLLA Telemetri: %${telemetry.batteryPercent}",
                speechText = telemetry.voiceReport
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.0.0.2. JARVIS PROTOKOLÜ: DONANIM AYDINLATMA / FENER (TORCH) KONTROLÜ
        // =========================================================================
        if (lowerMsg.contains("feneri aç") || lowerMsg.contains("ışığı aç") || lowerMsg.contains("flaşı aç") || lowerMsg.contains("fener aç")) {
            val (success, msg) = AtillaJarvisCoreHelper.setFlashlight(context, true)
            val speech = if (success) "Fener açıldı $patronPrefix." else "Fener açılamadı."
            val resp = AiResponse(
                replyText = "💡 **Aydınlatma Protokolü:** $msg",
                actionSummary = "💡 Fener Açıldı",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("feneri kapat") || lowerMsg.contains("ışığı kapat") || lowerMsg.contains("flaşı kapat") || lowerMsg.contains("fener kapat")) {
            val (success, msg) = AtillaJarvisCoreHelper.setFlashlight(context, false)
            val speech = if (success) "Fener kapatıldı $patronPrefix." else "Fener kapatılamadı."
            val resp = AiResponse(
                replyText = "💡 **Aydınlatma Protokolü:** $msg",
                actionSummary = "💡 Fener Kapatıldı",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.0.0.3. JARVIS PROTOKOLÜ: SES VE SESSİZLİK PROFİLİ (AUDIO PROFILE)
        // =========================================================================
        if (lowerMsg.contains("sessize al") || lowerMsg.contains("sessiz mod") || lowerMsg.contains("toplantı modu") || lowerMsg.contains("ders modu")) {
            val (success, msg) = AtillaJarvisCoreHelper.setAudioProfile(context, "silent")
            val speech = "Cihaz sessiz moda alındı $patronPrefix. Bildirimler susturuldu."
            val resp = AiResponse(
                replyText = msg,
                actionSummary = "🔕 Sessiz Mod Devrede",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("sesi aç") || lowerMsg.contains("normal mod") || lowerMsg.contains("sesli mod")) {
            val (success, msg) = AtillaJarvisCoreHelper.setAudioProfile(context, "normal")
            val speech = "Normal sesli moda geçildi $patronPrefix."
            val resp = AiResponse(
                replyText = msg,
                actionSummary = "🔔 Sesli Mod Devrede",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.0.0.4. JARVIS KİMLİK & VAROLUŞ SORULARI (ATİLLA İMZASI)
        // =========================================================================
        if (lowerMsg.contains("sen kimsin") || lowerMsg.contains("adın ne") || lowerMsg.contains("kendini tanıt") ||
            lowerMsg.contains("kimsin sen") || lowerMsg.contains("jarvis kim") || lowerMsg.contains("atilla kim")) {
            val speech = AtillaJarvisCoreHelper.getIdentitySpeech(patronPrefix)
            val briefing = AtillaJarvisCoreHelper.getIdentityBriefing(patronPrefix)
            val resp = AiResponse(
                replyText = briefing,
                actionSummary = "🤖 ATİLLA Kimlik Brifingi",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.0.0.5. JARVIS CİHAZ VE EKRAN ERİŞİLEBİLİRLİK PROTOKOLÜ (GLOBAL ACTIONS)
        // =========================================================================
        if (lowerMsg.contains("geri git") || lowerMsg.contains("önceki ekrana dön")) {
            val success = AtillaAccessibilityService.performBack()
            val msg = if (success) "Geri gidildi efendim." else "Erişilebilirlik servisini ayarlardan etkinleştirmeniz gerekmektedir."
            val resp = AiResponse(replyText = msg, actionSummary = "◀️ Geri Git", speechText = msg)
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("ana ekrana dön") || lowerMsg.contains("ana sayfaya git") || lowerMsg.contains("masaüstüne dön")) {
            val success = AtillaAccessibilityService.performHome()
            val msg = if (success) "Ana ekrana dönüldü efendim." else "Erişilebilirlik servisini ayarlardan etkinleştirmeniz gerekmektedir."
            val resp = AiResponse(replyText = msg, actionSummary = "🏠 Ana Ekran", speechText = msg)
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("bildirim panelini aç") || lowerMsg.contains("bildirimleri aç") || lowerMsg.contains("bildirimleri göster")) {
            val success = AtillaAccessibilityService.performNotifications()
            val msg = if (success) "Bildirim paneli açıldı efendim." else "Erişilebilirlik servisini ayarlardan etkinleştirmeniz gerekmektedir."
            val resp = AiResponse(replyText = msg, actionSummary = "🔔 Bildirim Paneli", speechText = msg)
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("ekranda ne var") || lowerMsg.contains("ekranı oku") || lowerMsg.contains("ekrandakileri oku") ||
            lowerMsg.contains("ekrandaki soruyu çöz") || lowerMsg.contains("soruyu çöz") || lowerMsg.contains("ekranı analiz et") || lowerMsg.contains("ekran gözü")) {
            val insight = AtillaScreenVisionHelper.analyzeCurrentScreen(context, patronPrefix)
            val resp = AiResponse(
                replyText = insight.displayMarkdown,
                actionSummary = if (insight.isQuestion) "🎓 Maarif Soru Çözümü" else "👁️ Ekran Gözü Analizi",
                speechText = insight.spokenReply
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("sabah brifingi") || lowerMsg.contains("günün brifingi") || lowerMsg.contains("bugün tarihte ne oldu")) {
            val briefing = AtillaProactiveEngine.generateMorningBriefing(context, patronPrefix)
            val resp = AiResponse(
                replyText = briefing.second,
                actionSummary = "🏛️ Maarif Sabah Brifingi",
                speechText = briefing.first
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("akşam değerlendirmesi") || lowerMsg.contains("gün sonu raporu") || lowerMsg.contains("günü değerlendir")) {
            val debrief = AtillaProactiveEngine.generateEveningDebrief(context, patronPrefix)
            val resp = AiResponse(
                replyText = debrief.second,
                actionSummary = "🌙 Maarif Akşam Değerlendirmesi",
                speechText = debrief.first
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("yüzen küreyi aç") || lowerMsg.contains("yüzen balonu aç") || lowerMsg.contains("hologramı aç") || lowerMsg.contains("baloncuğu aç")) {
            AtillaFloatingBubbleService.startBubble(context)
            val speech = "Yüzen ATİLLA hologram küresi ekrana yerleştirildi $patronPrefix."
            val resp = AiResponse(
                replyText = "🔮 **ATİLLA Hologram Küresi:** Canlı ekran balonu aktif edildi. Her an dokunarak emir verebilirsiniz.",
                actionSummary = "🔮 Yüzen Hologram Küresi",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("yüzen küreyi kapat") || lowerMsg.contains("yüzen balonu kapat") || lowerMsg.contains("hologramı kapat") || lowerMsg.contains("baloncuğu kapat")) {
            AtillaFloatingBubbleService.stopBubble(context)
            val speech = "Yüzen hologram küresi kapatıldı $patronPrefix."
            val resp = AiResponse(
                replyText = "🔮 **ATİLLA Hologram Küresi:** Ekran balonu kapatıldı.",
                actionSummary = "🔮 Yüzen Küre Kapatıldı",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("saatime gönder") || lowerMsg.contains("akıllı saate gönder") || lowerMsg.contains("saatte göster") || lowerMsg.contains("bileğime gönder")) {
            AtillaWearSyncHelper.sendWearableHudNotification(context, "ATİLLA Brifingi", "Son asistan raporunuz akıllı saatinize aktarıldı Sayın Patronum.", patronPrefix)
            val speech = "Brifing akıllı saatinize aktarıldı $patronPrefix. Bileğinizden sesli cevap verebilirsiniz."
            val resp = AiResponse(
                replyText = "⌚ **Wear OS Akıllı Saat Entegrasyonu:** Bildirim ve mikrofonla hızlı yanıt düğmesi saatinize gönderildi.",
                actionSummary = "⌚ Saate İletildi",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("çevrimdışı düşün") || lowerMsg.contains("çevrimdışı muhakeme") || lowerMsg.contains("internetsiz analiz") || lowerMsg.contains("derin muhakeme")) {
            val result = AtillaOfflineReasoningEngine.reason(context, cleanMsg, patronPrefix)
            val resp = AiResponse(
                replyText = result.finalAnswer,
                actionSummary = "🧠 Çevrimdışı Derin Muhakeme",
                speechText = result.speechSummary
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (lowerMsg.contains("ses imzamı kaydet") || lowerMsg.contains("sesimi öğren") || lowerMsg.contains("sesimi tanı")) {
            AtillaVoiceprintVerifier.enrollPatronVoiceprint(context, patronPrefix)
            val speech = "Biyometrik ses imzanız başarıyla kaydedildi $patronPrefix. Artık 'Hey ATİLLA' uyandırmasına sadece siz yetkilisiniz."
            val resp = AiResponse(
                replyText = "🎙️ **Biyometrik Ses İmzası Kaydedildi:**\n\n• Profil: $patronPrefix\n• Frekans Koruması: Aktif (85 - 175 Hz Erkek Ses Tonu)\n• Yetkisiz Erişim Engeli: Devrede.",
                actionSummary = "🎙️ Biyometrik Ses İmzası",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.0.1. ÖĞRETMEN DERS PLANI SINIF SEÇİM DİYALOĞU
        // =========================================================================
        if (UstaSessionState.isWaitingForLessonPlanGrade) {
            val gradeLevel = when {
                lowerMsg.contains("9") -> "9. Sınıf"
                lowerMsg.contains("11") -> "11. Sınıf"
                lowerMsg.contains("12") -> "12. Sınıf"
                lowerMsg.contains("10") -> "10. Sınıf"
                else -> "10. Sınıf"
            }
            UstaSessionState.isWaitingForLessonPlanGrade = false
            val topic = UstaSessionState.pendingLessonPlanTopic.ifBlank { "Beylikten Devlete Osmanlı Siyaseti ve Teşkilatlanma" }

            val (pdfFile, pdfReport) = HistoryLessonPlanPdfHelper.createMaarifHistoryPlanPdf(
                context = context,
                gradeLevel = gradeLevel,
                topicTitle = topic,
                teacherName = if (currentNick.isNotBlank()) currentNick else "Tarih Öğretmeni"
            )
            UstaSessionState.lastGeneratedPdfFile = pdfFile
            val speech = "$patronPrefix, $gradeLevel Türkiye Yüzyılı Maarif Modeli Tarih ders planınızı resmi A4 PDF formatında hazırlayıp ekranınıza getirdim."

            val reply = buildString {
                append("📑 **$patronPrefix, $gradeLevel Türkiye Yüzyılı Maarif Modeli Tarih Ders Planınız Hazırlandı!**\n\n")
                append("📋 **Konu:** $topic\n")
                append("🏫 **Pedagojik Çerçeve:** MEB 2026/2027 Maarif Modeli (Beceri Temelli, Süreç Odaklı Değerlendirme)\n")
                append("📄 **Resmi Evrak:** A4 Formatında Tarih Dersi Günlük Planı\n")
                append("📁 **Kayıt Konumu:** `Documents/HatirlaGit_TarihPlanlari/${pdfFile?.name ?: "Tarih_Ders_Plani.pdf"}`\n\n")
                append("💡 Belge cihazınızda doğrudan açıldı. Dilerseniz aşağıdaki butondan tekrar açabilir veya yazdırabilirsiniz.")
            }

            val resp = AiResponse(
                replyText = reply,
                actionSummary = "📚 Maarif Planı Hazır: $gradeLevel",
                speechText = speech,
                generatedPdfFile = pdfFile
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.0.2. YEMEK TERCİHİ & DOLAP İSTİŞARE DİYALOĞU
        // =========================================================================
        if (UstaSessionState.isWaitingForFoodPreference) {
            UstaSessionState.isWaitingForFoodPreference = false
            val (recipeText, recipeSpeech) = RecipeHelper.analyzeFridgeAndSuggest(cleanMsg, patronPrefix)
            val resp = AiResponse(
                replyText = recipeText,
                actionSummary = "🍽️ Şef Menüsü & Tarif",
                speechText = recipeSpeech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.0.3. "PDF'İ AÇ" / "BELGEYİ EKRANA GETİR" / "PLANI GÖSTER"
        // =========================================================================
        if (lowerMsg.contains("pdf aç") || lowerMsg.contains("pdf'i aç") || lowerMsg.contains("belgeyi aç") ||
            lowerMsg.contains("ekrana getir") || lowerMsg.contains("dosyayı aç") || lowerMsg.contains("planı aç") ||
            lowerMsg.contains("hazırladığın planı göster") || lowerMsg.contains("pdf göster")) {
            val targetPdf = UstaSessionState.lastGeneratedPdfFile ?: ResearchPdfHelper.getLatestGeneratedPdf(context)
            if (targetPdf != null && targetPdf.exists()) {
                ResearchPdfHelper.openPdfFile(context, targetPdf)
                val speech = "$patronPrefix, hazırlanan PDF belgesini ekranınıza getirdim."
                val resp = AiResponse(
                    replyText = "📄 **Hazırlanan PDF Belgesi Ekranınıza Getirildi!**\n\n📁 **Dosya:** `${targetPdf.name}`\n\n💡 Belge açılmazsa aşağıdaki butona dokunarak doğrudan görüntüleyebilirsiniz.",
                    actionSummary = "📄 PDF Açıldı: ${targetPdf.name}",
                    speechText = speech,
                    generatedPdfFile = targetPdf
                )
                UstaSessionState.lastAssistantResponse = resp
                return@withContext resp
            }
        }

        // =========================================================================
        // 0.1. EKSİK MALZEMELERİ ALIŞVERİŞ LİSTESİNE EKLEME
        // =========================================================================
        if (lowerMsg.contains("alışveriş listeme ekle") || lowerMsg.contains("eksikleri ekle") || lowerMsg.contains("malzemeleri ekle") || lowerMsg.contains("listeme ekle")) {
            val itemsToAdd = UstaSessionState.pendingShoppingItems
            if (itemsToAdd.isNotEmpty()) {
                val count = RecipeHelper.addMissingToShoppingList(context, itemsToAdd)
                UstaSessionState.pendingShoppingItems = emptyList()
                val speech = "$patronPrefix, tarif için gereken $count adet eksik malzeme alışveriş listenize eklendi."
                val resp = AiResponse(
                    replyText = "🛒 **$count Adet Malzeme Alışveriş Listenize Eklendi:**\n\n" + itemsToAdd.joinToString("\n") { "• $it" },
                    actionSummary = "🛒 Alışverişe Eklendi ($count)",
                    speechText = speech
                )
                UstaSessionState.lastAssistantResponse = resp
                return@withContext resp
            }
        }

        // =========================================================================
        // 0.2. "BUNU PDF YAP" / "ARAŞTIRMAYI PDF YAP"
        // =========================================================================
        if (lowerMsg.contains("bunu pdf yap") || lowerMsg.contains("pdf olarak kaydet") || lowerMsg.contains("pdf'e dönüştür") || lowerMsg.contains("pdf yap") || lowerMsg.contains("pdf çıkar")) {
            val lastResp = UstaSessionState.lastAssistantResponse
            val contentToPdf = lastResp?.replyText ?: cleanMsg
            val titleToPdf = lastResp?.actionSummary?.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ _-]"), "")?.trim()?.ifBlank { "ATİLA Araştırma Raporu" } ?: "ATİLA Araştırma Raporu"
            val (pdfFile, status) = ResearchPdfHelper.createAndOpenPdf(context, titleToPdf, contentToPdf)
            UstaSessionState.lastGeneratedPdfFile = pdfFile
            val speech = "$patronPrefix, hazırladığım raporu PDF belgesi olarak oluşturup ekranınıza getirdim."
            val resp = AiResponse(
                replyText = status,
                actionSummary = "📄 PDF Oluşturuldu",
                speechText = speech,
                generatedPdfFile = pdfFile
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.3. TARİH ÖĞRETMENİ MAARİF GÜNLÜK & HAFTALIK DERS PLANI (İNTERAKTİF SINIF SORMA)
        // =========================================================================
        val isLessonPlanRequest = lowerMsg.contains("ders planı") ||
                lowerMsg.contains("maarif") ||
                lowerMsg.contains("tarih planı") ||
                lowerMsg.contains("haftalık plan") ||
                lowerMsg.contains("ders programı") ||
                (lowerMsg.contains("günlük plan") && !lowerMsg.contains("su iç") && !lowerMsg.contains("rutin")) ||
                (lowerMsg.contains("bu hafta için plan") || lowerMsg.contains("bu haftanın planı") || lowerMsg.contains("bu hafta için günlük plan")) ||
                (lowerMsg.contains("plan hazırla") && !lowerMsg.contains("rutin") && !lowerMsg.contains("kişisel"))

        if (isLessonPlanRequest) {
            val hasExplicitGrade = lowerMsg.contains("9") || lowerMsg.contains("10") || lowerMsg.contains("11") || lowerMsg.contains("12")

            if (!hasExplicitGrade) {
                // Öğretmen sınıf belirtmediyse ezbere rutin basmak YASAKTIR! Önce sınıf sorulur.
                UstaSessionState.isWaitingForLessonPlanGrade = true
                UstaSessionState.pendingLessonPlanTopic = "Beylikten Devlete Osmanlı Siyaseti ve Teşkilatlanma"
                val speech = "Sayın Hocam, bu haftanın Tarih ders planını hangi sınıf düzeyimiz için hazırlamamı istersiniz? 9, 10, 11 veya 12. Sınıf olarak belirtirseniz Maarif modeline uygun resmi planınızı hemen ekrana getireyim."
                val reply = buildString {
                    append("🎓 **$patronPrefix, Bu Haftanın Ders Planını Hangi Sınıf Düzeyimiz İçin Hazırlamamı İstersiniz?**\n\n")
                    append("• **9. Sınıf:** Tarih ve Zaman / İlk ve Orta Çağlarda Türk Dünyası\n")
                    append("• **10. Sınıf:** Beylikten Devlete Osmanlı Siyaseti ve Gaza Anlayışı\n")
                    append("• **11. Sınıf:** Değişen Dünya Dengeleri Karşısında Osmanlı Siyaseti\n")
                    append("• **12. Sınıf:** 20. Yüzyıl Başlarında Osmanlı ve Millî Mücadele\n\n")
                    append("✍️ Sınıf düzeyini (örn: _'10. Sınıf'_ veya _'10'_) söylemeniz yeterlidir; Maarif Modeline uygun resmi A4 PDF belgeniz anında hazırlanıp açılacaktır.")
                }
                val resp = AiResponse(
                    replyText = reply,
                    actionSummary = "🎓 Sınıf Düzeyi Bekleniyor (9-12)",
                    speechText = speech
                )
                UstaSessionState.lastAssistantResponse = resp
                return@withContext resp
            } else {
                val grade = if (lowerMsg.contains("9")) "9. Sınıf" else if (lowerMsg.contains("11")) "11. Sınıf" else if (lowerMsg.contains("12")) "12. Sınıf" else "10. Sınıf"
                val topic = when {
                    lowerMsg.contains("selçuklu") -> "Yerleşme ve Devletleşme Sürecinde Selçuklu Türkiyesi"
                    lowerMsg.contains("kurtuluş") || lowerMsg.contains("milli mücadele") -> "Millî Mücadele ve Atatürk İnkılapları"
                    lowerMsg.contains("osmanlı") -> "Beylikten Devlete Osmanlı Siyaseti ve Teşkilatlanma"
                    lowerMsg.contains("ilk çağ") || lowerMsg.contains("zaman") -> "Tarih ve Zaman - İnsanlığın İlk Dönemleri"
                    else -> "Beylikten Devlete Osmanlı Siyaseti ve Gaza Anlayışı"
                }
                val (pdfFile, pdfReport) = HistoryLessonPlanPdfHelper.createMaarifHistoryPlanPdf(
                    context = context,
                    gradeLevel = grade,
                    topicTitle = topic,
                    teacherName = if (currentNick.isNotBlank()) currentNick else "Tarih Öğretmeni"
                )
                UstaSessionState.lastGeneratedPdfFile = pdfFile
                val speech = "$patronPrefix, Türkiye Yüzyılı Maarif Modeline uygun $grade Tarih Dersi günlük planını resmi A4 PDF formatında hazırlayıp ekranınıza getirdim."
                val resp = AiResponse(
                    replyText = pdfReport,
                    actionSummary = "📚 Maarif Tarih Planı PDF ($grade)",
                    speechText = speech,
                    generatedPdfFile = pdfFile
                )
                UstaSessionState.lastAssistantResponse = resp
                return@withContext resp
            }
        }

        // =========================================================================
        // 0.4. LİSE ÖĞRENCİSİ İÇİN TEZ / PERFORMANS ÖDEVİ PDF
        // =========================================================================
        if (lowerMsg.contains("performans ödevi") || lowerMsg.contains("tarih tezi") || lowerMsg.contains("tez hazırla") || lowerMsg.contains("ödev hazırla") || lowerMsg.contains("performans görevi")) {
            val topic = cleanMsg.replace(Regex("(?i)performans ödevi|tarih tezi|tez hazırla|ödev hazırla|performans görevi|hazırla|hakkında|için|bana|lütfen"), "").trim().ifBlank { "Osmanlı Devleti Kuruluş Dönemi Dinamikleri" }
            val (thesisFile, thesisReport) = HistoryLessonPlanPdfHelper.createStudentHistoryThesisPdf(
                context = context,
                thesisTopic = topic,
                studentName = "Lise Öğrencisi",
                gradeLevel = "10. Sınıf"
            )
            UstaSessionState.lastGeneratedPdfFile = thesisFile
            val speech = "$patronPrefix, lise öğrencisine yönelik akademik performans tezi ve kaynakça raporunu A4 PDF formatında oluşturup ekranınıza getirdim."
            val resp = AiResponse(
                replyText = thesisReport,
                actionSummary = "🎓 Tarih Tezi PDF: $topic",
                speechText = speech,
                generatedPdfFile = thesisFile
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.5. YEMEK TARİFİ İSTİŞARESİ & BUZDOLABI MALZEMESİ MOTORU
        // =========================================================================
        val isFridgeRequest = lowerMsg.contains("dolapta") || lowerMsg.contains("buzdolab") || lowerMsg.contains("elimde") || lowerMsg.contains("neler yapabilirim") || (lowerMsg.contains("malzeme") && lowerMsg.contains("yemek"))
        val isFoodInquiryRequest = lowerMsg.contains("yemek tarifi") || lowerMsg.contains("akşam için yemek") || lowerMsg.contains("ne pişirsem") || lowerMsg.contains("akşam yemeği") || lowerMsg.contains("yemek öner")

        if (isFridgeRequest) {
            val (recipeText, recipeSpeech) = RecipeHelper.analyzeFridgeAndSuggest(cleanMsg, patronPrefix)
            val resp = AiResponse(
                replyText = recipeText,
                actionSummary = "🍽️ Dolap Analizi & Tarif",
                speechText = recipeSpeech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        } else if (isFoodInquiryRequest) {
            UstaSessionState.isWaitingForFoodPreference = true
            val (inquiryText, inquirySpeech) = RecipeHelper.getInteractiveFoodInquiry(patronPrefix)
            val resp = AiResponse(
                replyText = inquiryText,
                actionSummary = "🍽️ Akşam Yemeği İstişaresi",
                speechText = inquirySpeech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // =========================================================================
        // 0.6. TELEFON NUMARASI İSTEME & TEYİTLİ ARAMA MOTORU
        // =========================================================================
        if (lowerMsg.contains("telefon numarası") || lowerMsg.contains("telefon numarasını") || lowerMsg.contains("numarası kaç") || lowerMsg.contains("iletişim numarası") || lowerMsg.contains("telefonu ne")) {
            val targetTerm = cleanMsg.replace(Regex("(?i)telefon numarası|telefon numarasını|numarası kaç|iletişim numarası|telefonu ne|kaç|nedir|bana|ver|öğren"), "").trim()
            val (realLatForPhone, realLngForPhone) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
            val places = NearbyPlacesHelper.getRecommendedPlaces(context, realLatForPhone, realLngForPhone, targetTerm)
            val foundPlace = places.firstOrNull { !it.phone.isNullOrBlank() } ?: places.firstOrNull()
            val phone = foundPlace?.phone ?: "182"
            val placeName = foundPlace?.name ?: targetTerm.ifBlank { "İlgili Kuruluş" }

            UstaSessionState.isWaitingForCallConfirmation = true
            UstaSessionState.pendingCallPhoneNumber = phone
            UstaSessionState.pendingCallPlaceName = placeName

            val speech = "$patronPrefix, $placeName telefon numarası $phone. Şimdi aramamı ister misiniz?"
            val reply = buildString {
                append("📞 **$placeName İletişim Bilgisi**\n\n")
                append("• **Telefon:** `$phone`\n")
                if (foundPlace != null) append("• **Adres:** ${foundPlace.address}\n\n")
                append("💡 _Aramamı ister misiniz? **'Evet'** veya **'Ara'** demeniz yeterlidir._")
            }
            val resp = AiResponse(
                replyText = reply,
                actionSummary = "📞 $placeName: $phone",
                speechText = speech
            )
            UstaSessionState.lastAssistantResponse = resp
            return@withContext resp
        }

        // Sohbet Modu Açma / Kapatma Kontrolü
        if (lowerMsg.contains("sohbet modu") || lowerMsg.contains("arkadaş gibi konuş") || lowerMsg.contains("dost gibi konuş")) {
            UstaSessionState.isChatMode = true
            val speech = "Harika fikir dostum! Resmi protokolleri bir kenara bırakıyorum, arkadaş gibi sohbet ediyoruz. Neler yapıyorsun, günün nasıl geçiyor?"
            return@withContext AiResponse(
                replyText = "🤝 **Sohbet Modu Devrede!**\n\n$speech",
                actionSummary = "🤝 Sohbet Modu Aktif",
                speechText = speech
            )
        }
        if (lowerMsg.contains("patron modu") || lowerMsg.contains("normal mod") || lowerMsg.contains("resmi mod")) {
            UstaSessionState.isChatMode = false
            val speech = "Emredersiniz $patronPrefix. Tüm asistan protokolleri ve komut sistemleri hizmetinizdedir."
            return@withContext AiResponse(
                replyText = "👑 **Patron Modu Devrede!**\n\n$speech",
                actionSummary = "👑 Patron Modu Aktif",
                speechText = speech
            )
        }

        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // =========================================================================
        // 1. NÖBETÇİ ECZANE SORGULAMA (EN YAKIN 3 ECZANE KARTI & NAVİGASYON)
        // =========================================================================
        if (lowerMsg.contains("eczane") || lowerMsg.contains("nöbetçi") || lowerMsg.contains("nobetci") || lowerMsg.contains("ilaç nereden") || lowerMsg.contains("yol tarifi")) {
            val district = userDistrict.ifBlank { "Merkez" }
            val city = userCity.ifBlank { "Bulunduğunuz Şehir" }
            val top3 = NearbyPlacesHelper.getTop3NearbyPlaces(context, realLat, realLng, cleanMsg)
            val top1 = top3.firstOrNull()

            // Kullanıcı nöbetçi eczane veya yol tarifi istediğinde ilk sıradaki hedefe doğrudan navigasyonu başlat
            if (top1 != null) {
                NearbyPlacesHelper.openGoogleMapsNavigation(context, top1.name, top1.lat, top1.lng, top1.address)
            }

            val speech = "$patronPrefix, $district bölgesinde en yakın ${top1?.name ?: "nöbetçi eczane"} için Google Haritalar canlı yol tarifini başlattım. En yakın 3 nöbetçi eczane ekranınızda hazır."

            val reply = buildString {
                append("🏥 **$patronPrefix, $city $district Bölgesindeki En Yakın 3 Nöbetçi Eczane:**\n\n")
                top3.forEachIndexed { idx, p ->
                    append("${idx + 1}. **${p.name}**\n")
                    append("   • ${p.typeLabel} (${p.distanceMeters} metre mesafede)\n")
                    append("   • Adres: ${p.address}\n\n")
                }
                append("🗺️ **İlk sıradaki ${top1?.name ?: "nöbetçi eczane"} için Google Haritalar canlı yol tarifi başlatıldı.**\n")
                append("💡 Diğer eczanelere gitmek veya doğrudan aramak için aşağıdaki butonlara dokunabilirsiniz.")
            }

            return@withContext AiResponse(
                replyText = reply,
                recommendedPlaces = top3,
                actionSummary = "🏥 Nöbetçi Eczane Navigasyonu: ${top1?.name ?: "$city $district"}",
                speechText = speech
            )
        }

        // =========================================================================
        // 2. KİŞİSEL YAŞAM VE SAĞLIK RUTİNİ MOTORU (SADECE KİŞİSEL TALEP EDİLDİĞİNDE)
        // =========================================================================
        val isPersonalRoutineRequest = (lowerMsg.contains("benim günümü planla") || lowerMsg.contains("kişisel rutin") ||
                lowerMsg.contains("yaşam rutinim") || lowerMsg.contains("su içme rutini") || lowerMsg.contains("kişisel program") ||
                (lowerMsg.contains("günü planla") && !lowerMsg.contains("ders") && !lowerMsg.contains("okul") && !lowerMsg.contains("öğretmen") && !lowerMsg.contains("hafta")))

        if (isPersonalRoutineRequest) {
            if (lowerMsg.contains("işle") || lowerMsg.contains("kaydet") || lowerMsg.contains("kur")) {
                val scheduleSummary = DailyRoutinePlanner.scheduleFullRoutine(context)
                val speech = "Efendim, kişisel yaşam ve çalışma rutinlerinizin tamamını takviminize ve sesli alarmlarınıza başarıyla işledim."
                return@withContext AiResponse(
                    replyText = "🗓️ Efendim, günlük dengeli yaşam ve çalışma rutinleriniz alarmlarınıza ve yerel takviminize işlendi.\n\n$scheduleSummary",
                    actionSummary = scheduleSummary,
                    speechText = speech
                )
            } else {
                val fullPlan = DailyRoutinePlanner.getDailyPlanBriefing(currentNick)
                val voiceSummary = DailyRoutinePlanner.getVoiceRoutineSummary(currentNick)
                return@withContext AiResponse(
                    replyText = fullPlan,
                    actionSummary = "📋 Kişisel Yaşam Rutini",
                    speechText = voiceSummary
                )
            }
        }

        // =========================================================================
        // 3. DOĞRUDAN SESLİ / HIZLI NOT KAYDETME (GARANTİLİ ROOM + LOCALSTORAGE)
        // =========================================================================
        if (lowerMsg.startsWith("sesli not al") || lowerMsg.startsWith("not al") || lowerMsg.startsWith("not ekle") || lowerMsg.startsWith("hızlı not al")) {
            val noteContent = cleanMsg.replace(Regex("(?i)^(sesli not al|not al|not ekle|hızlı not al)[: ]*"), "").trim()
            val noteBody = noteContent.ifBlank { "Kayıtlı Not" }
            val noteTitle = noteBody.take(40).trim()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(now))

            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = "SESLİ NOT",
                    title = noteTitle,
                    customNote = noteBody,
                    dueDateMillis = now,
                    dueDatetime = dateStr,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "NOTE_SAVED"
                )
            )
            LocalStorageManager.saveLocalNote(context, noteTitle, noteBody, "SESLİ NOT")
            val speech = "Efendim, notunuz yerel belleğe başarıyla kaydedildi."
            return@withContext AiResponse(
                replyText = "Efendim, notunuz 'Sesli Notlar' listenize ve yerel belleğe başarıyla kaydedildi.",
                actionSummary = "📝 Not Kaydedildi: $noteTitle",
                speechText = speech
            )
        }

        // =========================================================================
        // 4. DOĞRUDAN KONUM KAYDETME (GARANTİLİ GPS + ROOM + LOCALSTORAGE)
        // =========================================================================
        if (lowerMsg.contains("konumumu kaydet") || lowerMsg.contains("burayı kaydet") || lowerMsg.contains("konum kaydet") || lowerMsg.contains("haritaya kaydet") || lowerMsg.contains("lokasyona kaydet")) {
            val locName = cleanMsg.replace(Regex("(?i)konumumu kaydet|burayı kaydet|konum kaydet|haritaya kaydet|lokasyona kaydet|olarak|adıyla|adı|bana"), "").trim()
                .ifBlank { "$userCity $userDistrict Konumu" }

            db.savedLocationDao().insertLocation(
                SavedLocationEntity(
                    name = locName,
                    lat = realLat,
                    lng = realLng,
                    timestamp = System.currentTimeMillis()
                )
            )
            LocalStorageManager.saveLocalLocation(context, locName, realLat, realLng)
            val speech = "Efendim, $locName konumunuz belleğe kaydedildi."
            return@withContext AiResponse(
                replyText = "Efendim, '$locName' konumunuz ($userCity $userDistrict) 'Kayıtlı Lokasyonlarım' listenize ve yerel belleğe başarıyla kaydedildi.",
                actionSummary = "📍 Konum Kaydedildi: $locName",
                speechText = speech
            )
        }

        // =========================================================================
        // 5. DOĞRUDAN İLAÇ HATIRLATICISI EKLEME
        // =========================================================================
        if (lowerMsg.startsWith("ilaç ekle") || lowerMsg.startsWith("ilaç hatırlat") || lowerMsg.contains("ilacımı ekle")) {
            val medName = cleanMsg.replace(Regex("(?i)^(ilaç ekle|ilaç hatırlat|ilacımı ekle)[: ]*"), "").trim()
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 9
            val min = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val drugTitle = medName.replace(Regex("""(?i)\d{1,2}[:.]\d{2}"""), "").trim().ifBlank { "İlaç Dozu" }

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(cal.time)
            val newRem = ReminderEntity(
                category = "MEDICINE",
                title = "💊 $drugTitle",
                customNote = "Saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} ilaç dozu.",
                dueDatetime = dateStr,
                dueDateMillis = cal.timeInMillis,
                isFavorite = true,
                encryptedMetadata = "{}",
                actionStep = "SOUND_CLASSIC_BELL"
            )
            val insertedId = db.reminderDao().insertReminder(newRem)
            AlarmHelper.scheduleAlarm(context, newRem.copy(id = insertedId.toInt()))
            LocalStorageManager.saveLocalReminder(context, "💊 $drugTitle", dateStr, cal.timeInMillis, "MEDICINE")

            val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", hour, min)
            val speech = "Efendim, $drugTitle ilacınız her gün saat $timeFormatted için alarmlarınıza eklendi."
            return@withContext AiResponse(
                replyText = "Efendim, '$drugTitle' ilacınız her gün saat $timeFormatted için ilaç listenize ve sesli alarmlarınıza eklendi.",
                actionSummary = "💊 İlaç Eklendi: $drugTitle",
                speechText = speech
            )
        }

        // =========================================================================
        // 6. CANLI HAVA DURUMU & GAZETE MANŞETLERİ
        // =========================================================================
        if (lowerMsg.contains("hava durumu") || lowerMsg.contains("hava nasıl") || lowerMsg.contains("hava kaç derece") || lowerMsg.contains("yağmur var mı") || lowerMsg.contains("hava raporu")) {
            val weatherText = WeatherHelper.getLiveWeather(context, realLat, realLng, userCity.ifBlank { "Bulunduğunuz Şehir" })
            val voiceWeather = WeatherHelper.getVoiceWeatherBriefing(context, realLat, realLng, userCity.ifBlank { "Bulunduğunuz Şehir" })
            return@withContext AiResponse(
                replyText = weatherText,
                actionSummary = "🌤️ Hava Durumu: $userCity",
                speechText = voiceWeather
            )
        }

        if (lowerMsg.contains("gazete manşet") || lowerMsg.contains("haberler") || lowerMsg.contains("günün haber") || lowerMsg.contains("gazeteleri özetle") || lowerMsg.contains("gündem") || lowerMsg.contains("son dakika")) {
            val headlines = DailyNewsHelper.getHeadlinesOnly()
            val voiceHeadlines = DailyNewsHelper.getVoiceHeadlinesSummary()
            return@withContext AiResponse(
                replyText = headlines,
                actionSummary = "📰 Günlük Gazete Manşetleri",
                speechText = voiceHeadlines
            )
        }

        // =========================================================================
        // 7. YOUTUBE EVRENSEL ARAMA VE OYNATMA (MÜZİK, VİDEO, YEMEK, DERS VB.)
        // =========================================================================
        if (lowerMsg.contains("youtube") || lowerMsg.contains("çal") || lowerMsg.contains("müzik") || lowerMsg.contains("şarkı") ||
            lowerMsg.contains("oyun havası") || lowerMsg.contains("oyun havasi") || lowerMsg.contains("türkü") ||
            lowerMsg.contains("videosu") || lowerMsg.contains("video aç") || lowerMsg.contains("video izle") || lowerMsg.contains("videolu tarif")) {
            val targetQuery = cleanMsg.replace(Regex("(?i)^(youtube'dan|youtube'da|youtube|youtubeden|youtubede|yt)[: ]*"), "")
                .replace(Regex("(?i)(şarkısını|şarkıyı|müziğini|müzik|videosunu|videoyu|video|aç|çal|oynat|bul|izle|bana)$"), "")
                .trim().ifBlank { if (lowerMsg.contains("oyun havası") || lowerMsg.contains("oyun havasi")) "Ankara Oyun Havaları" else "Türkçe Müzik" }
            val (_, msg) = AppLauncherHelper.searchAndPlayYouTube(context, targetQuery)
            val speech = "$patronPrefix, YouTube'da $targetQuery açılıyor."
            return@withContext AiResponse(
                replyText = msg,
                actionSummary = "▶️ YouTube: $targetQuery",
                speechText = speech
            )
        }

        // =========================================================================
        // 8. UYGULAMA AÇMA (PATRON GÜVENLİK VE ONAY KİLİDİ İLE BAŞLATMA)
        // =========================================================================
        if (lowerMsg.endsWith("aç") || lowerMsg.contains("uygulamayı aç") || lowerMsg.contains("uygulamasını aç") ||
            lowerMsg.contains("hesap makinesi") || lowerMsg.contains("galeri") || lowerMsg.contains("kamera") ||
            lowerMsg.contains("spotify") || lowerMsg.contains("instagram") || lowerMsg.contains("whatsapp") ||
            lowerMsg.contains("e-devlet") || lowerMsg.contains("mebbis")) {
            val target = cleanMsg.replace(Regex("(?i)lütfen|bana|hemen|aç|uygulamasını|uygulamayı|uygulama|giriş yap|çalıştır"), "").trim()
            if (target.isNotBlank()) {
                val hasDirectPermission = lowerMsg.contains("izin") || lowerMsg.contains("onay") || lowerMsg.contains("yetki") || lowerMsg.contains("izin veriyorum")
                val appLabel = target.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

                if (hasDirectPermission) {
                    // Kullanıcı komutta doğrudan izin vermişse hemen aç
                    val (success, msg) = AppLauncherHelper.openApplicationByVoice(context, target)
                    if (success) {
                        val resp = AiResponse(
                            replyText = "🔓 **Patron İzninizle Açıldı:** $msg",
                            actionSummary = msg,
                            speechText = "İzniniz doğrultusunda $appLabel açıldı $patronPrefix."
                        )
                        AtillaMemoryManager.recordConversation(context, userMessage, resp.replyText)
                        return@withContext resp
                    }
                } else {
                    // Güvenlik Kilidi: Önce Patron'dan yetkilendirme teyidi iste
                    UstaSessionState.isWaitingForAppLaunchConfirmation = true
                    UstaSessionState.pendingAppToLaunchKeyword = target
                    UstaSessionState.pendingAppToLaunchLabel = appLabel

                    val speech = "Patron teyidi gerekiyor: $appLabel uygulamasına erişmemi onaylıyor musunuz efendim?"
                    val reply = buildString {
                        append("🔒 **PATRON GÜVENLİK KİLİDİ: UYGULAMA ERİŞİM İZNİ TALEBİ**\n\n")
                        append("• **Hedef Uygulama:** $appLabel\n")
                        append("• **Erişim Türü:** Cihaz Uygulama Başlatma & Köprü\n")
                        append("• **Güvenlik Politikası:** Yetkisiz erişim yasaktır; Patron onayı zorunludur.\n\n")
                        append("💡 _Onaylamak için **'Evet'**, **'Aç'** veya **'İzin veriyorum'** demeniz yeterlidir Sayın Patronum._")
                    }
                    val resp = AiResponse(
                        replyText = reply,
                        actionSummary = "🔒 İzin Bekleniyor: $appLabel",
                        speechText = speech
                    )
                    UstaSessionState.lastAssistantResponse = resp
                    AtillaMemoryManager.recordConversation(context, userMessage, reply)
                    return@withContext resp
                }
            }
        }

        // =========================================================================
        // 9. DURUM MAKİNESİ (İNTERAKTİF ALARM & HATIRLATICI ADIMLARI)
        // =========================================================================
        if (UstaSessionState.isWaitingForAlarmTime) {
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val singleHourMatch = Regex("""(?i)\b(\d{1,2})\b""").find(cleanMsg)
            val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: singleHourMatch?.groupValues?.get(1)?.toIntOrNull()
            val min = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            if (hour != null && hour in 0..23 && min in 0..59) {
                UstaSessionState.pendingAlarmHour = hour
                UstaSessionState.pendingAlarmMinute = min
                UstaSessionState.isWaitingForAlarmTime = false
                UstaSessionState.isWaitingForAlarmLabel = true
                val formattedTime = String.format(Locale.ROOT, "%02d:%02d", hour, min)
                val speech = "Alarm saatini $formattedTime olarak belirledim efendim. Peki bu alarmın başlığı ne olsun?"
                return@withContext AiResponse(
                    replyText = "⏰ Alarm saatini $formattedTime olarak belirledim efendim. Peki bu alarmın başlığı ne olsun? (Örn: Uyanış, Toplantı, İlaç)",
                    speechText = speech
                )
            } else {
                val speech = "Saati tam anlayamadım efendim. Lütfen '07:30' veya '8:00' şeklinde söyler misiniz?"
                return@withContext AiResponse(replyText = speech, speechText = speech)
            }
        }

        if (UstaSessionState.isWaitingForAlarmLabel) {
            val label = cleanMsg.take(40).trim().ifBlank { "Alarm" }
            val hour = UstaSessionState.pendingAlarmHour ?: 8
            val min = UstaSessionState.pendingAlarmMinute ?: 0
            UstaSessionState.isWaitingForAlarmLabel = false
            UstaSessionState.pendingAlarmHour = null
            UstaSessionState.pendingAlarmMinute = null

            setSystemAlarm(context, hour, min, label)

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(cal.time)
            val rem = ReminderEntity(
                category = "ALARM",
                title = "⏰ Alarm: $label",
                customNote = "Saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} için kurulu alarm.",
                dueDatetime = dateStr,
                dueDateMillis = cal.timeInMillis,
                isFavorite = true,
                encryptedMetadata = "{}",
                actionStep = "ALARM_SET"
            )
            db.reminderDao().insertReminder(rem)
            LocalStorageManager.saveLocalReminder(context, "⏰ Alarm: $label", dateStr, cal.timeInMillis, "ALARM")

            val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", hour, min)
            val speech = "Efendim, saat $timeFormatted için $label alarmınız kuruldu."
            return@withContext AiResponse(
                replyText = "⏰ Efendim, saat $timeFormatted için '$label' alarmınız başarıyla kuruldu.",
                actionSummary = "⏰ Alarm Kuruldu: $label ($timeFormatted)",
                speechText = speech
            )
        }

        if (lowerMsg.contains("alarm kur") || lowerMsg.contains("alarm ekle") || lowerMsg.contains("alarmı kur")) {
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull()
            val min = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            if (hour != null && hour in 0..23) {
                val label = cleanMsg.replace(Regex("""(?i)\d{1,2}[:.]\d{2}|alarm kur|alarm ekle|alarmı kur|saat|için|bana"""), "").trim().ifBlank { "Alarm" }
                setSystemAlarm(context, hour, min, label)

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(cal.time)
                val rem = ReminderEntity(
                    category = "ALARM",
                    title = "⏰ Alarm: $label",
                    customNote = "Saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} için kurulu alarm.",
                    dueDatetime = dateStr,
                    dueDateMillis = cal.timeInMillis,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "ALARM_SET"
                )
                db.reminderDao().insertReminder(rem)
                LocalStorageManager.saveLocalReminder(context, "⏰ Alarm: $label", dateStr, cal.timeInMillis, "ALARM")

                val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", hour, min)
                val speech = "Efendim, saat $timeFormatted için $label alarmınız kuruldu."
                return@withContext AiResponse(
                    replyText = "⏰ Efendim, saat $timeFormatted için '$label' alarmınız kuruldu.",
                    actionSummary = "⏰ Alarm Kuruldu: $label",
                    speechText = speech
                )
            } else {
                UstaSessionState.isWaitingForAlarmTime = true
                val speech = "Alarm saat kaç için kurulsun efendim?"
                return@withContext AiResponse(
                    replyText = "Alarm saat kaç için kurulsun efendim? (Örn: 07:30 veya 8:00)",
                    speechText = speech
                )
            }
        }

        // =========================================================================
        // 10. ATİLA GARDIROP VE MODÜLER ÇEKMECELER MİMARİSİ (WARDROBE & DRAWERS)
        // =========================================================================
        val customApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey)?.trim() else null
        } catch (_: Exception) { null }

        val activeApiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getSecureDefaultKey()

        val sessionData = com.example.util.assistant.WardrobeSessionData(
            apiKey = activeApiKey,
            assistantName = resolvedAssistantName,
            userNick = currentNick,
            userCity = userCity,
            userDistrict = userDistrict,
            userLat = realLat,
            userLng = realLng,
            conversationHistory = conversationHistory,
            db = db,
            dataStoreManager = dataStoreManager
        )

        val drawerResult = com.example.util.assistant.AtillaWardrobeManager.dispatch(context, cleanMsg, sessionData)
        val emotionAnalysis = AtillaEmotionEngine.analyze(cleanMsg, patronPrefix)
        val rawSpeech = drawerResult.speechText ?: drawerResult.replyText
        val enrichedSpeech = if (emotionAnalysis.empathyPreamble.isNotBlank() && !rawSpeech.startsWith(emotionAnalysis.empathyPreamble)) {
            emotionAnalysis.empathyPreamble + rawSpeech
        } else {
            rawSpeech
        }

        val finalResp = AiResponse(
            replyText = drawerResult.replyText,
            recommendedPlaces = drawerResult.recommendedPlaces,
            actionSummary = drawerResult.actionSummary,
            speechText = enrichedSpeech,
            generatedPdfFile = drawerResult.generatedPdfFile
        )
        if (drawerResult.generatedPdfFile != null) {
            UstaSessionState.lastGeneratedPdfFile = drawerResult.generatedPdfFile
        }
        UstaSessionState.lastAssistantResponse = finalResp
        AtillaMemoryManager.recordConversation(context, userMessage, finalResp.replyText)
        return@withContext finalResp
    }

    private fun getDeviceLocation(context: Context): Pair<Double, Double> {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            if (best != null) Pair(best.latitude, best.longitude) else Pair(41.2867, 36.33)
        } catch (_: Exception) {
            Pair(41.2867, 36.33)
        }
    }
}
