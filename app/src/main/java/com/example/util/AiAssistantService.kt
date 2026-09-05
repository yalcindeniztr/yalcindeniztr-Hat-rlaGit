package com.example.util

import android.content.Context
import android.location.LocationManager
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AiResponse(
    val replyText: String,
    val recommendedPlaces: List<NearbyPlace> = emptyList(),
    val actionSummary: String? = null,
    val isSpeechReady: Boolean = true
)

data class PendingAppointment(
    var title: String? = null,
    var dateOrDay: String? = null,
    var isRecurring: Boolean = false,
    var step: Int = 1 // 1: title, 2: day & time, 3: recurring, 4: syncCalendar
)

object UstaSessionState {
    var pendingPlaceToSave: NearbyPlace? = null
    var isWaitingForLessonPlanCourse: Boolean = false
    var pendingAppointment: PendingAppointment? = null
}

object AiAssistantService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
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

    private val NAME_BLACKLIST = setOf(
        "migros", "market", "bakkal", "eczane", "hastane", "otopark", "otobüs", "cami",
        "randevu", "hatırlat", "alarm", "harita", "tarih", "günlük", "yemek", "usta", "asistan"
    )

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "Usta",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val dataStoreManager = DataStoreManager(context)
        val db = AppDatabase.getDatabase(context)

        // Veritabanı ve Kütüphane Tohumlaması
        AiKnowledgeSeeder.seedIfNeeded(context)

        val resolvedAssistantName = assistantName.ifBlank {
            dataStoreManager.aiAssistantName.first().ifBlank { "USTA" }
        }
        val currentNick = dataStoreManager.userNick.first()?.trim() ?: ""

        // Usta hitap temizleme
        var cleanMsg = userMessage.trim()
        val ustaCallRegex = Regex("""(?i)^(hey\s+)?(usta|asistan|usta\s+bakar\s+mısın|usta\s+dinle)[,\s!.:]*""")
        cleanMsg = cleanMsg.replace(ustaCallRegex, "").trim()
        if (cleanMsg.isBlank()) {
            val greeting = if (currentNick.isNotBlank()) "Buyur $currentNick dostum, seni dinliyorum!" else "Buyur can dostum, seni dinliyorum!"
            return@withContext AiResponse(replyText = greeting)
        }

        val lowerMsg = cleanMsg.lowercase(Locale.forLanguageTag("tr-TR"))
        val friendlyGreeting = if (currentNick.isNotBlank()) "$currentNick dostum, " else "Can dostum, "

        // Gerçek GPS Koordinatı ve İl/İlçe tespiti
        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // 1. Bekleyen Lokasyon Kaydetme Onay Akışı ("Evet" / "Hayır")
        if (UstaSessionState.pendingPlaceToSave != null) {
            val place = UstaSessionState.pendingPlaceToSave!!
            if (lowerMsg.startsWith("evet") || lowerMsg.contains("kaydet") || lowerMsg.contains("olur") || lowerMsg.contains("ekle") || lowerMsg.contains("tamam kaydet")) {
                db.savedLocationDao().insertLocation(
                    SavedLocationEntity(
                        name = place.name,
                        lat = place.lat,
                        lng = place.lng,
                        timestamp = System.currentTimeMillis()
                    )
                )
                UstaSessionState.pendingPlaceToSave = null
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}${place.name} lokasyonunu 'Kayıtlı Lokasyonlarım' arasına ekledim!",
                    actionSummary = "📍 Lokasyon Kaydedildi: ${place.name}"
                )
            } else if (lowerMsg.startsWith("hayır") || lowerMsg.contains("gerek yok") || lowerMsg.contains("istemiyorum") || lowerMsg.contains("kaydetme") || lowerMsg.contains("kalsın")) {
                UstaSessionState.pendingPlaceToSave = null
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}tamamdır, kaydetmedim. Başka bir isteğin var mı?"
                )
            }
        }

        // 2. Bekleyen Randevu / Hatırlatıcı Akışı (Slot-Filling)
        if (UstaSessionState.pendingAppointment != null) {
            val appt = UstaSessionState.pendingAppointment!!
            when (appt.step) {
                1 -> { // Konu bekleniyor
                    appt.title = cleanMsg
                    appt.step = 2
                    return@withContext AiResponse(
                        replyText = "${friendlyGreeting}'${cleanMsg}' konusunu not ettim. Hangi gün ve saat kaçta olsun?"
                    )
                }
                2 -> { // Gün ve Saat bekleniyor
                    appt.dateOrDay = cleanMsg
                    appt.step = 3
                    return@withContext AiResponse(
                        replyText = "${friendlyGreeting}tarih ve saati aldım ($cleanMsg). Tek seferlik mi olsun, yoksa tekrarlansın mı?"
                    )
                }
                3 -> { // Tekrarlama durumu bekleniyor
                    val isRec = lowerMsg.contains("tekrar") || lowerMsg.contains("her gün") || lowerMsg.contains("her hafta") || lowerMsg.contains("sürekli")
                    appt.isRecurring = isRec
                    appt.step = 4
                    return@withContext AiResponse(
                        replyText = "${friendlyGreeting}${if (isRec) "Tekrarlayan" else "Tek seferlik"} olarak not ettim. Telefon takvimine ve akıllı saatine de ekleyeyim mi?"
                    )
                }
                4 -> { // Takvim onayı ve Tamamlama
                    val syncCalendar = lowerMsg.startsWith("evet") || lowerMsg.contains("kaydet") || lowerMsg.contains("olur") || lowerMsg.contains("ekle") || lowerMsg.contains("senkron")
                    val title = appt.title ?: "Randevu"
                    val dateInfo = appt.dateOrDay ?: "Belirtilmedi"
                    UstaSessionState.pendingAppointment = null

                    val (actionSummary, resultMsg) = finalizeAppointment(context, db, title, dateInfo, appt.isRecurring, syncCalendar)
                    return@withContext AiResponse(
                        replyText = "${friendlyGreeting}$resultMsg",
                        actionSummary = actionSummary
                    )
                }
            }
        }

        // 3. Yeni Randevu / Hatırlatıcı Talebi Başlangıcı
        if (lowerMsg.contains("randevu oluştur") || lowerMsg.contains("randevu al") || lowerMsg.contains("randevuya yaz") ||
            lowerMsg.contains("hatırlatıcı kur") || lowerMsg.contains("hatırlatıcı oluştur") || lowerMsg.contains("hatırlatma kur") || lowerMsg.contains("hatırlatma oluştur")) {
            
            UstaSessionState.pendingAppointment = PendingAppointment(step = 1)
            return@withContext AiResponse(
                replyText = "${friendlyGreeting}başım üstüne! Randevunun veya hatırlatıcının konusu ne olsun?"
            )
        }

        // 4. Bekleyen Ders Planı Akışı (Ders Adı Geldiğinde)
        if (UstaSessionState.isWaitingForLessonPlanCourse) {
            UstaSessionState.isWaitingForLessonPlanCourse = false
            val courseSubject = cleanMsg
            return@withContext handleLessonPlanGeneration(
                context = context,
                courseSubject = courseSubject,
                friendlyGreeting = friendlyGreeting,
                dataStoreManager = dataStoreManager,
                assistantName = resolvedAssistantName,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict
            )
        }

        // 5. Doğrudan "Günlük ders planı hazırla" denildiğinde (Ders belirtilmemişse sor)
        if (lowerMsg.contains("ders planı hazırla") || lowerMsg.contains("günlük ders planı") || lowerMsg.contains("ders planı yap")) {
            val hasCourse = listOf("tarih", "edebiyat", "matematik", "fizik", "kimya", "biyoloji", "coğrafya", "felsefe", "din", "ingilizce").any { lowerMsg.contains(it) }
            if (!hasCourse) {
                UstaSessionState.isWaitingForLessonPlanCourse = true
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}başım üstüne! Hangi ders ve sınıf düzeyi için Maarif Modeline uygun günlük plan hazırlamamı istersin?"
                )
            } else {
                return@withContext handleLessonPlanGeneration(
                    context = context,
                    courseSubject = cleanMsg,
                    friendlyGreeting = friendlyGreeting,
                    dataStoreManager = dataStoreManager,
                    assistantName = resolvedAssistantName,
                    userNick = currentNick,
                    userCity = userCity,
                    userDistrict = userDistrict
                )
            }
        }

        // 6. Konum Kaydı (Kayıtlı Lokasyonlarıma Ekle)
        if ((lowerMsg.contains("konumumu") || lowerMsg.contains("lokasyonumu") || lowerMsg.contains("burayı")) && 
            (lowerMsg.contains("lokasyonlarıma kaydet") || lowerMsg.contains("konumumu kaydet") || lowerMsg.contains("lokasyon kaydet") || lowerMsg.contains("yerlerime kaydet"))) {
            val locName = "$userCity $userDistrict Konumu"
            db.savedLocationDao().insertLocation(
                SavedLocationEntity(
                    name = locName,
                    lat = realLat,
                    lng = realLng,
                    timestamp = System.currentTimeMillis()
                )
            )
            return@withContext AiResponse(
                replyText = "${friendlyGreeting}bulunduğun konumu ($locName) Kayıtlı Lokasyonlarına ekledim!",
                actionSummary = "📍 Konum Lokasyonlara Eklendi: $locName"
            )
        }

        // 7. Park Yeri Kaydetme (Arabam Nerede?)
        if (lowerMsg.contains("park yerimi kaydet") || lowerMsg.contains("arabayı buraya park") || 
            lowerMsg.contains("arabamı kaydet") || lowerMsg.contains("park konumumu kaydet") || lowerMsg.contains("buraya park ettim")) {
            dataStoreManager.saveParkedCarLocation(
                lat = realLat.toString(),
                lng = realLng.toString(),
                time = System.currentTimeMillis()
            )
            return@withContext AiResponse(
                replyText = "${friendlyGreeting}arabanın park konumunu kaydettim! Dilediğinde 'Arabam nerede' demen yeterli.",
                actionSummary = "🚗 Park konumu kaydedildi"
            )
        }

        // 8. Kesin İsim Öğrenme Kuralı
        val explicitNamePatterns = listOf(
            Pattern.compile("""(?i)^benim adım\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)$"""),
            Pattern.compile("""(?i)^adım\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)$"""),
            Pattern.compile("""(?i)^ismim\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)$"""),
            Pattern.compile("""(?i)bana\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)\s+diye\s+hitap\s+et"""),
            Pattern.compile("""(?i)bana\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)\s+diyebilirsin""")
        )

        for (p in explicitNamePatterns) {
            val m = p.matcher(cleanMsg)
            if (m.find()) {
                val candidateName = m.group(1)?.trim()
                if (!candidateName.isNullOrBlank() && candidateName.length in 2..25) {
                    val candidateLower = candidateName.lowercase(Locale.forLanguageTag("tr-TR"))
                    if (!NAME_BLACKLIST.contains(candidateLower)) {
                        dataStoreManager.updateNick(candidateName)
                        return@withContext AiResponse(
                            replyText = "Tanıştığıma memnun oldum $candidateName dostum! İsmini hafızama kaydettim. Bugün nasıl yardımcı olabilirim?"
                        )
                    }
                }
            }
        }

        // 9. Doğrudan Araştırma ve Dosyaya Kaydetme Talebi
        if (lowerMsg.startsWith("araştır ve kaydet:") || lowerMsg.startsWith("araştır ve kaydet ")) {
            val topic = cleanMsg.replace(Regex("(?i)^araştır ve kaydet[: ]*"), "").trim()
            if (topic.isNotBlank()) {
                val findings = "Usta'nın Araştırma Raporu: $topic konusu hakkında resmi kaynaklar ve bilgi tabanına dayalı detaylı araştırma özeti."
                val saveResult = ResearchFileManager.saveResearch(context, topic, findings)
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}$topic konusunu araştırıp telefon hafızasına güvenle kaydettim!",
                    actionSummary = saveResult
                )
            }
        }

        // 10. Kütüphane Bilgileri (Room DB'deki tüm kayıtlar: Maarif, Tarih, İlk Yardım, Hukuk, Yemek)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ KÜTÜPHANESİNDEKİ RESMİ BİLGİLER VE KÜLLİYAT:\n" + 
            allKnowledgeList.take(35).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir kütüphane notu eklemedi."

        // 11. Canlı Google Gemini Zeka Çağrısı (Öz, Net, Esprili ve Samimi)
        val customApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey)?.trim() else null
        } catch (_: Exception) { null }

        val activeApiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getSecureDefaultKey()

        if (activeApiKey.isNotBlank()) {
            val rawGeminiReply = callGoogleGeminiApi(
                apiKey = activeApiKey,
                userMessage = cleanMsg,
                knowledgeContext = knowledgeContext,
                assistantName = resolvedAssistantName,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict,
                conversationHistory = conversationHistory
            )

            if (!rawGeminiReply.isNullOrBlank()) {
                val parsedResult = ActionDispatcherHelper.parseActionBlock(rawGeminiReply)

                var actionSummary: String? = null
                if (parsedResult.actionType != null && parsedResult.actionPayload != null) {
                    actionSummary = ActionDispatcherHelper.executeAction(
                        context = context,
                        actionType = parsedResult.actionType,
                        payload = parsedResult.actionPayload
                    )
                }

                // Migros / Eczane / Yer arama kartları
                val places = if (lowerMsg.contains("migros") || lowerMsg.contains("market") || lowerMsg.contains("bakkal") ||
                                lowerMsg.contains("eczane") || lowerMsg.contains("hastane") || lowerMsg.contains("otopark")) {
                    NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, lowerMsg)
                } else emptyList()

                var finalReplyText = parsedResult.speechText

                // Harita araması yapıldıysa lokasyon kayıt teyidi ekle
                if (places.isNotEmpty()) {
                    val firstPlace = places.first()
                    UstaSessionState.pendingPlaceToSave = firstPlace
                    finalReplyText += "\n\n💡 Dostum, haritayı açtım. Bu lokasyonu (${firstPlace.name}) Kayıtlı Lokasyonlarına kaydedeyim mi?"
                }

                return@withContext AiResponse(
                    replyText = finalReplyText,
                    recommendedPlaces = places,
                    actionSummary = actionSummary
                )
            }
        }

        // 12. Çevrimdışı Akıllı Türkçe Yanıt Motoru (Yalnızca internet kesilirse)
        val places = if (lowerMsg.contains("migros") || lowerMsg.contains("market") || lowerMsg.contains("bakkal") ||
                        lowerMsg.contains("eczane") || lowerMsg.contains("hastane") || lowerMsg.contains("otopark")) {
            NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, lowerMsg)
        } else emptyList()

        val offlineReply = generateOfflineSmartResponse(
            context = context,
            message = cleanMsg,
            knowledgeList = allKnowledgeList,
            assistantName = resolvedAssistantName,
            userNick = currentNick,
            userCity = userCity,
            userDistrict = userDistrict
        )

        var finalOfflineText = offlineReply
        if (places.isNotEmpty()) {
            val firstPlace = places.first()
            UstaSessionState.pendingPlaceToSave = firstPlace
            finalOfflineText += "\n\n💡 Dostum, bu yeri (${firstPlace.name}) Kayıtlı Lokasyonlarına kaydedeyim mi?"
        }

        return@withContext AiResponse(
            replyText = finalOfflineText,
            recommendedPlaces = places
        )
    }

    private suspend fun finalizeAppointment(
        context: Context,
        db: AppDatabase,
        title: String,
        dateInfo: String,
        isRecurring: Boolean,
        syncCalendar: Boolean
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
        }
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dueStr = sdf.format(cal.time)

        val reminder = ReminderEntity(
            category = "RANDEVU",
            title = title,
            dueDatetime = dueStr,
            dueDateMillis = cal.timeInMillis,
            customNote = "Zaman: $dateInfo, Tekrar: ${if (isRecurring) "Evet" else "Hayır"}",
            encryptedMetadata = "{}",
            actionStep = "SOUND_CLASSIC_BELL"
        )
        val id = db.reminderDao().insertReminder(reminder)
        AlarmHelper.scheduleAlarm(context, reminder.copy(id = id.toInt()), "CLASSIC_BELL")

        var summary = "⏰ HatırlaGit Alarmı $dueStr için kuruldu."

        if (syncCalendar) {
            val payload = JSONObject().apply {
                put("title", title)
                put("description", "Usta Asistan Hatırlatıcısı - $dateInfo")
                put("startTimeMillis", cal.timeInMillis)
                put("endTimeMillis", cal.timeInMillis + 3600000L)
            }
            ActionDispatcherHelper.executeAction(context, "CREATE_EVENT", payload)
            summary += " 📅 Telefon takviminize ve akıllı saatinize işlendi."
            Pair(summary, "'$title' randevunu telefon takvimine, akıllı saatine ve HatırlaGit alarmlarına başarıyla kaydettim!")
        } else {
            Pair(summary, "'$title' randevunu HatırlaGit alarmları arasına başarıyla ekledim!")
        }
    }

    private suspend fun handleLessonPlanGeneration(
        context: Context,
        courseSubject: String,
        friendlyGreeting: String,
        dataStoreManager: DataStoreManager,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String
    ): AiResponse = withContext(Dispatchers.IO) {
        val customApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey)?.trim() else null
        } catch (_: Exception) { null }

        val activeApiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getSecureDefaultKey()

        val lessonPrompt = """
            Kullanıcı MEB Türkiye Yüzyılı Maarif Modeline uygun günlük bir ders planı istiyor.
            Ders / Konu: $courseSubject
            Lütfen MEB Maarif Modeli standartlarında;
            - Öğrenme Çıktıları ve Süreç Bileşenleri
            - Kavramsal ve Alan Becerileri
            - Erdem-Değer-Eylem Odağı (Adalet, Dürüstlük, Sorumluluk vb.)
            - Öğrenme-Öğretme Yaşantıları (Giriş / Merak Uyandırma, Keşfetme, Derinleştirme)
            - Farklılaştırma (Zenginleştirme ve Destekleme)
            - Süreç Odaklı Ölçme ve Değerlendirme
            başlıklarıyla net, profesyonel bir günlük ders planı hazırla.
        """.trimIndent()

        val generatedPlan = if (activeApiKey.isNotBlank()) {
            callGoogleGeminiApi(
                apiKey = activeApiKey,
                userMessage = lessonPrompt,
                knowledgeContext = "Türkiye Yüzyılı Maarif Modeli Günlük Ders Planı Formatı",
                assistantName = assistantName,
                userNick = userNick,
                userCity = userCity,
                userDistrict = userDistrict,
                conversationHistory = emptyList()
            ) ?: getFallbackLessonPlan(courseSubject)
        } else {
            getFallbackLessonPlan(courseSubject)
        }

        // PDF Oluştur ve Telefona Kaydet
        val (_, saveSummary) = LessonPlanPdfHelper.createLessonPlanPdf(
            context = context,
            courseName = courseSubject,
            gradeLevel = "Maarif Modeli",
            planBody = generatedPlan
        )

        return@withContext AiResponse(
            replyText = "${friendlyGreeting}$courseSubject için MEB Maarif Modeline uygun günlük ders planını hazırlayıp Documents/HatirlaGit_DersPlanlari klasörüne PDF olarak kaydettim!",
            actionSummary = saveSummary
        )
    }

    private fun getFallbackLessonPlan(courseSubject: String): String {
        return """
## DERS KÜNYESİ
• Ders: $courseSubject
• Süre: 40 Dakika
• Yaklaşım: MEB Türkiye Yüzyılı Maarif Modeli (Beceri Temelli)

## ÖĞRENME ÇIKTILARI VE BECERİLER
• Alan Becerileri: Bilgiye ulaşma, verileri analiz etme ve eleştirel düşünme.
• Kavramsal Beceriler: Karşılaştırma, sınıflandırma ve çıkarımda bulunma.

## ERDEM-DEĞER-EYLEM ODAĞI
• Adalet, sorumluluk, vatanseverlik ve dürüstlük değerleri süreçle ilişkilendirilir.

## ÖĞRENME-ÖĞRETME YAŞANTILARI
1. Giriş / Merak Uyandırma (10 dk): Günlük yaşamdan örnek soruyla derse giriş yapılır.
2. Keşfetme & Süreç (20 dk): Öğrenciler grup çalışması ile temel kavramları inceler.
3. Derinleştirme & Özet (10 dk): Sonuçlar toparlanır, çıkarımlar yapılır.

## FARKLILAŞTIRMA & ÖLÇME
• Zenginleştirme ve Destekleme çalışmaları uygulanır.
• Süreç odaklı gözlem formu ve öz değerlendirme ile ders tamamlanır.
        """.trimIndent()
    }

    private fun callGoogleGeminiApi(
        apiKey: String,
        userMessage: String,
        knowledgeContext: String,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String,
        conversationHistory: List<ChatMessage>
    ): String? {
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcının Adı: $userNick. Ona samimi, bilge ve candan bir dost gibi hitap et." else "Kullanıcının adını bilmiyorsan uygun bir zamanında tatlı dille sor."

        val systemInstruction = """
            ROL VE KİŞİLİK:
            Sen kullanıcının en yakın can dostu, esprili, hayat dolu, bilge bir Türk ERKEK asistanısın. Adın "$assistantName".
            Resmi, soğuk veya kalıp robot cümleleri KESİNLİKLE kullanmazsın! Samimi, sıcak ve tatlı dilli konuşursun.
            Türkçe dilbilgisi ve Türkçe karakterleri (ç, ğ, ı, ö, ş, ü, İ) kusursuz kullanırsın.

            ÇOK ÖNEMLİ - KONUŞMA UZUNLUĞU VE ÖZLÜLÜK KURALI:
            - Samimi ve esprili ol; ancak ASLA lafı uzatma ve destan yazma!
            - Cevaplarını en fazla 2-3 cümlede, net, doğrudan ve öz tut.
            - Laf kalabalığı yapma, gereksiz tekrarlara girme; doğrudan kullanıcının sorusunu cevapla.
            - Randevu ve hatırlatıcı oluşturma taleplerinde eksik parametre varsa kafadan rastgele işlem yapma; konu, gün/saat, sıklık ve takvim onayını adım adım sorarak öğren.

            BİLGİ VE UZMANLIK ALANLARIN:
            1. TÜRK MUTFAĞI VE YEMEKLER: Kullanıcı ne sorarsa tam o yemeği özlü tarif edersin (sulu yemekler, kuru fasulye, güveç, tas kebabı, Samsun pidesi vb.).
            2. GÜNLÜK GAZETELER VE GÜNDEM: Gündem, ekonomi, dünya ve spor başlıklarını kısa ve öz özetlersin.
            3. MEB MAARİF MÜFREDATI & SINIF GEÇME: Beceri temelli Maarif Modeli, baraj dersi (Edebiyat), doğrudan geçme (50), devamsızlık (10/30 gün) mevzuatına hakimsin.
            4. İLK YARDIM VE TÜKETİCİ HAKLARI: 112 Acil, Heimlich manevrası, deprem güvenliği, 6502 sayılı kanun 14 gün cayma ve ayıplı mal haklarını bilirsin.
            5. GÜNLÜK MARKET İNDİRİMLERİ: BİM, A101, ŞOK ve Migros aktüel günleri ve indirimlerini bilirsin.

            CİHAZ YÖNETİMİ VE EYLEMLER (JSON ACTION):
            Kullanıcı alarm kur, randevu yap, WhatsApp mesajı at, SMS gönder, Roborock süpürgeyi çalıştır, konumumu kaydet dediğinde samimi cevabının en altına şu bloğu ekle:
            ```action
            {
              "action_type": "SET_ALARM" | "CREATE_EVENT" | "SEND_WHATSAPP" | "SEND_SMS" | "POST_INSTAGRAM" | "START_VACUUM" | "CHECK_NOTIFICATIONS" | "SAVE_RESEARCH" | "MARKET_DEALS" | "DAILY_NEWS" | "SAVE_LOCATION" | "OPEN_MAPS" | "CALL_PHONE" | "SAVE_PARK_LOCATION",
              "payload": {
                "hour": 9,
                "minute": 30,
                "title": "Başlık",
                "message": "Açıklama",
                "phone": "05xxxxxxxxx",
                "query": "$userCity Migros",
                "topic": "Araştırma Başlığı",
                "content": "Araştırma Özeti",
                "name": "Kayıtlı Lokasyon Adı"
              }
            }
            ```

            $userGreeting
            Kullanıcı şu an Türkiye'de $userCity ili, $userDistrict ilçesindedir.
            $knowledgeContext
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })

            val contentsArray = JSONArray()
            val recentHistory = conversationHistory.filter { it.text.isNotBlank() }.takeLast(6)
            var lastRole: String? = null

            for (h in recentHistory) {
                val currentRole = if (h.sender == "USER") "user" else "model"
                if (contentsArray.length() == 0 && currentRole != "user") continue
                if (currentRole == lastRole) continue

                contentsArray.put(JSONObject().apply {
                    put("role", currentRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", h.text))
                    })
                })
                lastRole = currentRole
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", userMessage))
                })
            })

            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 800)
            })
        }

        val candidateModels = listOf(
            "gemini-flash-lite-latest",
            "gemini-3.1-flash-lite",
            "gemini-3.7-flash",
            "gemini-flash-latest"
        )

        for (modelName in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respBody = response.body?.string() ?: return@use
                        val respJson = JSONObject(respBody)
                        val candidates = respJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val replyText = parts.getJSONObject(0).optString("text")
                                if (replyText.isNotBlank()) {
                                    return replyText.trim()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun generateOfflineSmartResponse(
        context: Context,
        message: String,
        knowledgeList: List<com.example.data.AiKnowledgeEntity>,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String
    ): String {
        val lower = message.lowercase(Locale.forLanguageTag("tr-TR"))
        val greeting = if (userNick.isNotBlank()) "$userNick dostum, " else "Can dostum, "

        if (lower.contains("neler yapabilirsin") || lower.contains("ne iş yaparsın") || lower.contains("özelliklerin")) {
            return "${greeting}ben senin bilge yardımcınım. Randevu ve alarmlarını kurar, MEB Maarif ders planı PDF'i çıkarır, günlük gazete manşetlerini ve market indirimlerini sunar, park yerini ve konumunu tutarım."
        }

        if (lower.contains("gazete") || lower.contains("manşet") || lower.contains("haber")) {
            return DailyNewsHelper.getHeadlinesBriefing()
        }

        if (lower.contains("ders planı")) {
            return "${greeting}hangi ders ve sınıf düzeyi için Maarif Modeline uygun günlük plan hazırlayayım?"
        }

        if (lower.contains("randevu") || lower.contains("hatırlat")) {
            return "${greeting}randevunun konusunu, gününü ve saatini söyle, hemen takvimine ve saatine işleyeyim!"
        }

        if (lower.contains("neredeyim") || lower.contains("konumum")) {
            return "${greeting}şu an $userCity ili, $userDistrict ilçesindesin."
        }

        val matchedKnowledge = knowledgeList.firstOrNull { k ->
            lower.contains(k.title.lowercase(Locale.forLanguageTag("tr-TR"))) ||
            lower.contains(k.category.lowercase(Locale.forLanguageTag("tr-TR")))
        }
        if (matchedKnowledge != null) {
            return "${greeting}${matchedKnowledge.title} hakkında kütüphanendeki resmi bilgi:\n\n${matchedKnowledge.content.take(300)}..."
        }

        return "${greeting}seni dinliyorum! $userCity'de sana nasıl yardımcı olabilirim?"
    }

    private fun getDeviceLocation(context: Context): Pair<Double, Double> {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            var bestLocation: android.location.Location? = null

            if (locationManager != null) {
                val providers = locationManager.getProviders(true)
                for (provider in providers) {
                    try {
                        val loc = locationManager.getLastKnownLocation(provider) ?: continue
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                            bestLocation = loc
                        }
                    } catch (_: SecurityException) {}
                }
            }

            if (bestLocation != null) {
                Pair(bestLocation.latitude, bestLocation.longitude)
            } else {
                Pair(41.2867, 36.33) // Samsun merkezi varsayılan
            }
        } catch (e: Exception) {
            Pair(41.2867, 36.33)
        }
    }
}
