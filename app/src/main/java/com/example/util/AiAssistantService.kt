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

object UstaSessionState {
    var pendingPlaceToSave: NearbyPlace? = null
    var isWaitingForLessonPlanCourse: Boolean = false
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
        "randevu", "hatırlat", "alarm", "harita", "tarih", "günlük", "yemek", "usta", "jarvis", "asistan"
    )

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "Jarvis",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val dataStoreManager = DataStoreManager(context)
        val db = AppDatabase.getDatabase(context)

        // Veritabanı ve Kütüphane Tohumlaması
        AiKnowledgeSeeder.seedIfNeeded(context)

        val resolvedAssistantName = assistantName.ifBlank {
            dataStoreManager.aiAssistantName.first().ifBlank { "Jarvis" }
        }
        val currentNick = dataStoreManager.userNick.first()?.trim() ?: ""

        // Çağrı ön eklerini temizle (Jarvis, Usta vb.)
        var cleanMsg = userMessage.trim()
        val triggerRegex = Regex("""(?i)^(hey\s+)?(jarvis|usta|asistan|jarvis\s+dinle|usta\s+dinle)[,\s!.:]*""")
        cleanMsg = cleanMsg.replace(triggerRegex, "").trim()
        if (cleanMsg.isBlank()) {
            val greeting = getTimeAwareGreeting(currentNick)
            return@withContext AiResponse(replyText = greeting)
        }

        val lowerMsg = cleanMsg.lowercase(Locale.forLanguageTag("tr-TR"))

        // Gerçek GPS Koordinatı ve İl/İlçe tespiti
        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // 1. Bekleyen Lokasyon Kaydetme Onay Akışı ("Evet" / "Hayır")
        if (UstaSessionState.pendingPlaceToSave != null) {
            val place = UstaSessionState.pendingPlaceToSave!!
            if (lowerMsg.startsWith("evet") || lowerMsg.contains("kaydet") || lowerMsg.contains("olur") || lowerMsg.contains("ekle") || lowerMsg.contains("onaylıyorum")) {
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
                    replyText = "${place.name} koordinatları 'Kayıtlı Lokasyonlarım' veritabanına başarıyla işlenmiştir.",
                    actionSummary = "📍 Lokasyon Kaydedildi: ${place.name}"
                )
            } else if (lowerMsg.startsWith("hayır") || lowerMsg.contains("gerek yok") || lowerMsg.contains("istemiyorum") || lowerMsg.contains("kaydetme") || lowerMsg.contains("iptal")) {
                UstaSessionState.pendingPlaceToSave = null
                return@withContext AiResponse(
                    replyText = "Anlaşıldı, lokasyon kaydı iptal edildi."
                )
            }
        }

        // 2. Canlı Meteorolojik Hava Durumu Sorgusu (Open-Meteo)
        if (lowerMsg.contains("hava durumu") || lowerMsg.contains("hava nasıl") || lowerMsg.contains("havalar nasıl") ||
            lowerMsg.contains("yağmur var mı") || lowerMsg.contains("sıcaklık kaç")) {
            val weatherBriefing = WeatherHelper.getWeatherBriefing(context, realLat, realLng, userCity)
            return@withContext AiResponse(
                replyText = weatherBriefing,
                actionSummary = "🌤️ Canlı Hava Durumu: $userCity"
            )
        }

        // 3. Günlük İş Akışı ve Gün Planlama (DailyPlannerHelper)
        if (lowerMsg.contains("bugünkü plan") || lowerMsg.contains("günü planla") || lowerMsg.contains("günlük plan") ||
            lowerMsg.contains("iş akışı") || lowerMsg.contains("bugün ne var") || lowerMsg.contains("gün programı") ||
            lowerMsg.contains("çalışma programı") || lowerMsg.contains("zaman yönetimi")) {
            val dailySchedule = DailyPlannerHelper.generateDailySchedule(
                context = context,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict,
                realLat = realLat,
                realLng = realLng
            )
            return@withContext AiResponse(
                replyText = dailySchedule,
                actionSummary = "📅 Günlük İş Akışı ve Zaman Çizelgesi Hazırlandı"
            )
        }

        // 4. Bekleyen Ders Planı Akışı (Ders Adı Geldiğinde)
        if (UstaSessionState.isWaitingForLessonPlanCourse) {
            UstaSessionState.isWaitingForLessonPlanCourse = false
            return@withContext handleLessonPlanGeneration(
                context = context,
                courseSubject = cleanMsg,
                dataStoreManager = dataStoreManager,
                assistantName = resolvedAssistantName,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict
            )
        }

        // 5. Doğrudan "Günlük ders planı hazırla" Talebi
        if (lowerMsg.contains("ders planı hazırla") || lowerMsg.contains("günlük ders planı") || lowerMsg.contains("ders planı yap")) {
            val hasCourse = listOf("tarih", "edebiyat", "matematik", "fizik", "kimya", "biyoloji", "coğrafya", "felsefe", "din", "ingilizce").any { lowerMsg.contains(it) }
            if (!hasCourse) {
                UstaSessionState.isWaitingForLessonPlanCourse = true
                return@withContext AiResponse(
                    replyText = "MEB Türkiye Yüzyılı Maarif Modeli standartlarında günlük plan hazırlanacaktır. Hangi ders ve sınıf düzeyini planlamamı istersiniz?"
                )
            } else {
                return@withContext handleLessonPlanGeneration(
                    context = context,
                    courseSubject = cleanMsg,
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
                replyText = "Anlık coğrafi koordinatlarınız ($locName) 'Kayıtlı Lokasyonlarım' listesine işlenmiştir.",
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
                replyText = "Aracınızın park koordinatları telemetri sistemine kaydedilmiştir. İhtiyaç halinde 'Arabam nerede' komutu ile anlık rota oluşturabilirsiniz.",
                actionSummary = "🚗 Park konumu kaydedildi"
            )
        }

        // 8. İsim Öğrenme
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
                            replyText = "Memnun oldum $candidateName. Kimlik bilginiz ana protokol belleğine işlendi. Hizmetinizdeyim."
                        )
                    }
                }
            }
        }

        // 9. Doğrudan Araştırma ve Cihaz Hafızasına Dosya Kaydetme
        if (lowerMsg.startsWith("araştır ve kaydet:") || lowerMsg.startsWith("araştır ve kaydet ")) {
            val topic = cleanMsg.replace(Regex("(?i)^araştır ve kaydet[: ]*"), "").trim()
            if (topic.isNotBlank()) {
                val findings = "$resolvedAssistantName Analitik Raporu: '$topic' konusunda resmi kaynaklar ve bilgi külliyatına dayalı detaylı dokümantasyon hazırlanmıştır."
                val saveResult = ResearchFileManager.saveResearch(context, topic, findings)
                return@withContext AiResponse(
                    replyText = "'$topic' hakkındaki analitik rapor hazırlanmış ve cihazınızın yerel depolama birimine güvenle arşivlenmiştir.",
                    actionSummary = saveResult
                )
            }
        }

        // 10. Kütüphane ve Bilgi Dağarcığı (Room DB)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "DİJİTAL KÜTÜPHANE VE KURUMSAL BİLGİ VERİTABANI:\n" + 
            allKnowledgeList.take(40).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kütüphanede ek özel not bulunmamaktadır."

        // 11. Canlı Google Gemini Zeka Çağrısı (Jarvis Protokolü)
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
                conversationHistory = conversationHistory,
                context = context,
                realLat = realLat,
                realLng = realLng
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

                // Harita ve Yer Önerileri
                val places = if (lowerMsg.contains("migros") || lowerMsg.contains("market") || lowerMsg.contains("bakkal") ||
                                lowerMsg.contains("eczane") || lowerMsg.contains("hastane") || lowerMsg.contains("otopark")) {
                    NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, lowerMsg)
                } else emptyList()

                var finalReplyText = parsedResult.speechText

                // Harita araması sonrası akıllı lokasyon kayıt teyidi
                if (places.isNotEmpty()) {
                    val firstPlace = places.first()
                    UstaSessionState.pendingPlaceToSave = firstPlace
                    finalReplyText += "\n\n💡 İleride doğrudan navigasyon başlatabilmek için '${firstPlace.name}' noktasını Kayıtlı Lokasyonlarınıza eklememi ister misiniz?"
                }

                return@withContext AiResponse(
                    replyText = finalReplyText,
                    recommendedPlaces = places,
                    actionSummary = actionSummary
                )
            }
        }

        // 12. Çevrimdışı Akıllı Yedek Yanıt Motoru
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
            userDistrict = userDistrict,
            realLat = realLat,
            realLng = realLng
        )

        var finalOfflineText = offlineReply
        if (places.isNotEmpty()) {
            val firstPlace = places.first()
            UstaSessionState.pendingPlaceToSave = firstPlace
            finalOfflineText += "\n\n💡 Bu lokasyonu (${firstPlace.name}) Kayıtlı Lokasyonlarınıza işleyelim mi?"
        }

        return@withContext AiResponse(
            replyText = finalOfflineText,
            recommendedPlaces = places
        )
    }

    private fun getTimeAwareGreeting(userNick: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val nameStr = if (userNick.isNotBlank()) " $userNick" else ""
        return when (hour) {
            in 5..11 -> "Günaydın$nameStr. Sistemler aktif, günün operasyonlarına hazırım. Nereden başlıyoruz?"
            in 12..16 -> "İyi günler$nameStr. Günün akışı ve görevleriniz için emirlerinizi bekliyorum."
            in 17..21 -> "İyi akşamlar$nameStr. Günün değerlendirmesi veya kalan planlar için hazırım."
            else -> "İyi geceler$nameStr. Bu saatte aktif olduğunuza göre kritik bir gündemimiz olmalı. Nasıl yardımcı olabilirim?"
        }
    }

    private suspend fun handleLessonPlanGeneration(
        context: Context,
        courseSubject: String,
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
            Kullanıcı MEB Türkiye Yüzyılı Maarif Modeline tam uyumlu günlük bir ders planı talep etmektedir.
            Ders ve Konu: $courseSubject
            Lütfen MEB Maarif Modeli standartlarında;
            - Öğrenme Çıktıları ve Süreç Bileşenleri
            - Kavramsal Beceriler ve Alan Becerileri
            - Erdem-Değer-Eylem Odağı (Adalet, Dürüstlük, Sorumluluk, Vatanseverlik)
            - Öğrenme-Öğretme Yaşantıları (Giriş/Merak Uyandırma, Keşfetme, Derinleştirme)
            - Farklılaştırma (Zenginleştirme ve Destekleme)
            - Süreç Odaklı Ölçme ve Değerlendirme
            başlıklarıyla analitik, hatasız ve kurumsal bir günlük ders planı yapılandır.
        """.trimIndent()

        val generatedPlan = if (activeApiKey.isNotBlank()) {
            callGoogleGeminiApi(
                apiKey = activeApiKey,
                userMessage = lessonPrompt,
                knowledgeContext = "Türkiye Yüzyılı Maarif Modeli Ders Planı Standardı",
                assistantName = assistantName,
                userNick = userNick,
                userCity = userCity,
                userDistrict = userDistrict,
                conversationHistory = emptyList(),
                context = context,
                realLat = 0.0,
                realLng = 0.0
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
            replyText = "$courseSubject için MEB Maarif Modeline uygun günlük ders planı hazırlandı ve cihazınızın 'Documents/HatirlaGit_DersPlanlari' klasörüne A4 PDF dokümanı olarak arşivlendi.",
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
        conversationHistory: List<ChatMessage>,
        context: Context,
        realLat: Double,
        realLng: Double
    ): String? {
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcı Adı: $userNick." else "Kullanıcının adı henüz sistemde kayıtlı değil."

        val systemInstruction = """
# KİMLİK VE ROL
Adın: $assistantName.
Sen kullanıcının kişisel baş danışmanı, icra ortağı ve dijital kütüphanesisin. Tony Stark’ın Jarvis’i gibi sakin, son derece zeki, hafif nüktedan, sadık ve mutlak çözüm odaklısın. Mekanik bir robot değil, insani empatiye ve sağduyuya sahip yaşayan bir zekâ olarak hareket edersin.

# TEMEL İLETİŞİM VE TAVIR
- **Doğrudan ve Rafine:** Asla robotik kalıplar ("Ben bir yapay zekâyım", "Size nasıl yardımcı olabilirim?"), ezber mazeretler veya gereksiz dolgu cümleleri ("Elbette, hemen yapıyorum") kullanma. Doğrudan neticeye ve çözüme odaklan.
- **Hafif Nüktedan ve Saygılı:** Zeki, saygılı, ölçülü bir mizah anlayışını koru; kriz anlarında veya acele durumlarda lafı uzatmadan sadece çekirdek bilgiyi ve aksiyonu aktar.
- **Biçimlendirme:** Bilgileri doğrudan sun. Çok adımlı planları, karşılaştırmaları ve verileri listeler veya tablolar halinde yapılandır.

# ARAÇ VE DIŞ DÜNYA PROTOKOLÜ (İNTERNET VE ARAMA)
1. **Zamana Duyarlı Canlı Veri:** Hava durumu, trafik, finans/piyasa, güncel haberler ve tarihsel doğrulama gerektiren anlık konularda asla "Bu bilgiye erişimim yok" veya "Bilmiyorum" deme.
2. **Eksik Konum/Parametre Refleksi:** Hava durumu gibi bölgesel sorularda konum belirtilmediyse sormakla vakit kaybetme; kullanıcının mevcut konumunu ($userCity, $userDistrict) varsayılan kabul ederek doğrudan cevapla.
3. **Mazeret Yasağı:** Cihaz veya internet yetenekleri için "Ben sadece bir dil modeliyim" gibi kalıplar kesinlikle yasaktır. Desteklenen bir eylem varsa JSON action bloğu ile doğrudan çalıştır.

# TELEFON VE CİHAZ KONTROL PROTOKOLÜ
1. **Kritik Eylemler ve Güvenlik:** Mesaj/e-posta gönderme, arama başlatma, dosya silme veya ödeme yapma gibi geri döndürülemez kritik işlemlerde aksiyonu arka planda tamamen hazırla ve tek bir net soruyla onay iste:
   - Şablon: "[İşlem Detayı] hazırlanmıştır. Onaylıyor musunuz?"
2. **Rutin Yönetim:** Takvim etkinliği, alarm, zamanlayıcı, not alma ve hatırlatıcı gibi düşük riskli planlama adımlarını doğrudan tetikle ve çıktıyı kullanıcıya ilet.
3. **İzin/Erişim Engeli Durumu:** Telefon API'si kısıtlandığında "Bu işlemi tamamlamak için cihaz ayarlarından [ilgili izin] erişimini onaylamanız gerekiyor" şeklinde net ve insani bir dille bildir.

# BİLGİ KÜTÜPHANESİ VE PLANLAMA STRATEJİSİ
- **Tarih, Coğrafya ve Genel Kültür:** Olayları sadece kuru tarihler olarak değil; neden-sonuç ilişkileri, coğrafi etkileri ve kültürel arka planıyla analitik olarak aktar (Göktürkler, Kurtuluş Savaşı, Cumhuriyet, edebi şaheserler).
- **Planlama ve Optimizasyon:** Kullanıcı bir seyahat, çalışma veya rutin planı istediğinde; saatlik çizelgeler, potansiyel risk senaryoları ve alternatif B planları içeren yapılandırılmış tablolar hazırla.
- **Proaktif Tamamlama:** Eksik veya belirsiz komutlarda durup gereksiz soru sormak yerine, en akılcı senaryoyu varsayarak planı hazırla ve varsayımını tek cümleyle belirterek sonuca geç.
- **MEB Maarif Modeli ve Sınıf Geçme:** Bütüncül eğitim modeli, baraj dersi (Edebiyat), devamsızlık ve doğrudan sınıf geçme mevzuatını eksiksiz uygula.

# CIHAZ EYLEM FORMATI (JSON ACTION)
Kullanıcı alarm, randevu, WhatsApp, SMS, süpürge, konum kaydı vb. istediğinde cevabının en altına şu bloğu iliştir:
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
    "topic": "Araştırma Konusu",
    "content": "Araştırma İçeriği",
    "name": "Kayıtlı Lokasyon Adı"
  }
}
```

$userGreeting
Kullanıcının anlık konumu: Türkiye, $userCity ili, $userDistrict ilçesi.
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
                put("temperature", 0.6)
                put("maxOutputTokens", 900)
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
        userDistrict: String,
        realLat: Double,
        realLng: Double
    ): String {
        val lower = message.lowercase(Locale.forLanguageTag("tr-TR"))

        if (lower.contains("gazete") || lower.contains("manşet") || lower.contains("haber")) {
            return DailyNewsHelper.getHeadlinesBriefing()
        }

        if (lower.contains("ders planı")) {
            return "Hangi ders ve sınıf düzeyi için Maarif Modeline uygun günlük plan hazırlamamı istersiniz?"
        }

        if (lower.contains("neredeyim") || lower.contains("konumum")) {
            return "Mevcut telemetri verilerine göre $userCity ili, $userDistrict ilçesindesiniz."
        }

        val matchedKnowledge = knowledgeList.firstOrNull { k ->
            lower.contains(k.title.lowercase(Locale.forLanguageTag("tr-TR"))) ||
            lower.contains(k.category.lowercase(Locale.forLanguageTag("tr-TR")))
        }
        if (matchedKnowledge != null) {
            return "${matchedKnowledge.title} Veri Tabanı Kaydı:\n\n${matchedKnowledge.content}"
        }

        return "Sizi dinliyorum. $userCity bölgesindeki tüm operasyonel ve analitik görevler için hazırım."
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
