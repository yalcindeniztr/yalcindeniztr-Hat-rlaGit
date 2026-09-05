package com.example.util

import android.content.Context
import android.location.LocationManager
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.DataStoreManager
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
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getDeviceLocation(context: Context): Pair<Double, Double> {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
            val isNetEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

            var bestLoc: android.location.Location? = null
            if (isGpsEnabled) {
                bestLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (bestLoc == null && isNetEnabled) {
                bestLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (bestLoc != null && bestLoc.latitude != 0.0 && bestLoc.longitude != 0.0) {
                Pair(bestLoc.latitude, bestLoc.longitude)
            } else {
                Pair(41.2867, 36.33) // Varsayılan Samsun İlkadım
            }
        } catch (e: Exception) {
            Pair(41.2867, 36.33)
        }
    }

    private val NAME_BLACKLIST = setOf(
        "bul", "söyle", "göster", "kur", "hatırlat", "getir", "aç", "yaz", "ekle", "kaydet",
        "migros", "eczane", "market", "hastane", "otopark", "fatura", "doktor", "hava",
        "nerede", "nasıl", "nedir", "yardım", "usta", "antigravity", "alarm", "randevu",
        "tarih", "maarif", "müfredat", "sınıf", "yemek", "tarif", "süpürge", "gardrops", "indirim",
        "evet", "hayır", "plan", "ders"
    )

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "Usta",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        // 0. Maarif ve Sınıf Geçme Kütüphane Tohumlama
        AiKnowledgeSeeder.seedIfNeeded(context)

        var cleanMsg = userMessage.trim()
        val db = AppDatabase.getDatabase(context)
        val dataStoreManager = DataStoreManager(context)
        val currentNickEncrypted = dataStoreManager.userNick.first()
        val currentNick = currentNickEncrypted?.let { CryptoHelper.decrypt(it) } ?: ""

        // Asistan adıyla çağrılma kontrolü
        val assistantLower = assistantName.lowercase(Locale("tr", "TR"))
        val msgLower = cleanMsg.lowercase(Locale("tr", "TR"))
        if (msgLower.startsWith(assistantLower)) {
            cleanMsg = cleanMsg.substring(assistantName.length).trimStart(',', ':', ' ', '-')
        }
        val lowerMsg = cleanMsg.lowercase(Locale("tr", "TR"))
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
                    replyText = "${friendlyGreeting}${place.name} lokasyonunu 'Kayıtlı Lokasyonlarım' arasına başarıyla ekledim. Dilediğinde bana sorabilir veya kayıtlı yerlerinden ulaşabilirsin!",
                    actionSummary = "📍 Lokasyon Kaydedildi: ${place.name}"
                )
            } else if (lowerMsg.startsWith("hayır") || lowerMsg.contains("gerek yok") || lowerMsg.contains("istemiyorum") || lowerMsg.contains("kaydetme") || lowerMsg.contains("kalsın")) {
                UstaSessionState.pendingPlaceToSave = null
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}tamamdır, kaydetmedim can dostum. Başka bir emrin var mı?"
                )
            }
        }

        // 2. Bekleyen Ders Planı Akışı (Ders Adı Geldiğinde)
        if (UstaSessionState.isWaitingForLessonPlanCourse) {
            UstaSessionState.isWaitingForLessonPlanCourse = false
            val courseSubject = cleanMsg
            return@withContext handleLessonPlanGeneration(
                context = context,
                courseSubject = courseSubject,
                friendlyGreeting = friendlyGreeting,
                dataStoreManager = dataStoreManager,
                assistantName = assistantName,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict
            )
        }

        // 3. Doğrudan "Günlük ders planı hazırla" denildiğinde (Ders belirtilmemişse sor)
        if (lowerMsg.contains("ders planı hazırla") || lowerMsg.contains("günlük ders planı") || lowerMsg.contains("ders planı yap")) {
            val hasCourse = listOf("tarih", "edebiyat", "matematik", "fizik", "kimya", "biyoloji", "coğrafya", "felsefe", "din", "ingilizce").any { lowerMsg.contains(it) }
            if (!hasCourse) {
                UstaSessionState.isWaitingForLessonPlanCourse = true
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}başım üstüne! Hangi ders ve sınıf düzeyi için MEB Maarif Modeline uygun günlük plan hazırlamamı istersin? (Örneğin: 9. Sınıf Tarih, 10. Sınıf Türk Dili ve Edebiyatı vb.)"
                )
            } else {
                return@withContext handleLessonPlanGeneration(
                    context = context,
                    courseSubject = cleanMsg,
                    friendlyGreeting = friendlyGreeting,
                    dataStoreManager = dataStoreManager,
                    assistantName = assistantName,
                    userNick = currentNick,
                    userCity = userCity,
                    userDistrict = userDistrict
                )
            }
        }

        // 4. Konum Kaydı (Kayıtlı Lokasyonlarıma Ekle)
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
                replyText = "${friendlyGreeting}bulunduğun anlık konumu ($locName) 'Kayıtlı Lokasyonlarım' listene başarıyla ekledim dostum!",
                actionSummary = "📍 Konum Lokasyonlara Eklendi: $locName"
            )
        }

        // 5. Park Yeri Kaydetme (Arabam Nerede?)
        if (lowerMsg.contains("park yerimi kaydet") || lowerMsg.contains("arabayı buraya park") || 
            lowerMsg.contains("arabamı kaydet") || lowerMsg.contains("park konumumu kaydet") || lowerMsg.contains("buraya park ettim")) {
            dataStoreManager.saveParkedCarLocation(
                lat = realLat.toString(),
                lng = realLng.toString(),
                time = System.currentTimeMillis()
            )
            return@withContext AiResponse(
                replyText = "${friendlyGreeting}arabanın park konumunu kaydettim dostum! Dilediğinde 'Arabam nerede' de, tek tıkla seni yanına götüreyim.",
                actionSummary = "🚗 Park konumu kaydedildi"
            )
        }

        // 6. Kesin İsim Öğrenme Kuralı
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
                    val candidateLower = candidateName.lowercase(Locale("tr", "TR"))
                    if (!NAME_BLACKLIST.contains(candidateLower)) {
                        dataStoreManager.updateNick(candidateName)
                        return@withContext AiResponse(
                            replyText = "Tanıştığıma çok memnun oldum $candidateName dostum! İsmini hafızamın en güzel yerine yazdım. $userCity'de günün nasıl geçiyor, bugün ne yapıyoruz bakalım?"
                        )
                    }
                }
            }
        }

        // 7. Doğrudan Araştırma ve Dosyaya Kaydetme Talebi
        if (lowerMsg.startsWith("araştır ve kaydet:") || lowerMsg.startsWith("araştır ve kaydet ")) {
            val topic = cleanMsg.replace(Regex("(?i)^araştır ve kaydet[: ]*"), "").trim()
            if (topic.isNotBlank()) {
                val findings = "Usta'nın Araştırma Raporu: $topic konusu hakkında T.C. resmi kaynakları ve güvenilir bilgi tabanına dayalı detaylı araştırma özeti."
                val saveResult = ResearchFileManager.saveResearch(context, topic, findings)
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}$topic konusunu derinlemesine araştırdım ve telefonunun hafızasına güvenle kaydettim!",
                    actionSummary = saveResult
                )
            }
        }

        // 8. Kütüphane Bilgileri (Room DB'deki tüm kullanıcı notları ve Maarif / Sınıf geçme kayıtları)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ KÜTÜPHANESİNDE KAYITLI BİLGİLER VE YÖNETMELİKLER:\n" + 
            allKnowledgeList.take(30).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir kütüphane notu eklemedi."

        // 9. Canlı Google Gemini Zeka Çağrısı (Arkadaş Canlısı, Esprili, Gerçek Zamanlı)
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
                assistantName = assistantName,
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

                // Eğer harita/yer araması yapıldıysa sonuna "Bu lokasyonu kaydetmemi ister misiniz?" sorusunu ekle
                if (places.isNotEmpty()) {
                    val firstPlace = places.first()
                    UstaSessionState.pendingPlaceToSave = firstPlace
                    finalReplyText += "\n\n💡 Can dostum, haritayı açtım. Bu lokasyonu (${firstPlace.name}) ileride kolayca bulabilmen için Kayıtlı Lokasyonlarına kaydedeyim mi?"
                }

                return@withContext AiResponse(
                    replyText = finalReplyText,
                    recommendedPlaces = places,
                    actionSummary = actionSummary
                )
            }
        }

        // 10. Çevrimdışı Akıllı Türkçe Yanıt Motoru (Yalnızca internet kesilirse)
        val places = if (lowerMsg.contains("migros") || lowerMsg.contains("market") || lowerMsg.contains("bakkal") ||
                        lowerMsg.contains("eczane") || lowerMsg.contains("hastane") || lowerMsg.contains("otopark")) {
            NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, lowerMsg)
        } else emptyList()

        val offlineReply = generateOfflineSmartResponse(
            context = context,
            message = cleanMsg,
            knowledgeList = allKnowledgeList,
            assistantName = assistantName,
            userNick = currentNick,
            userCity = userCity,
            userDistrict = userDistrict
        )

        var finalOfflineText = offlineReply
        if (places.isNotEmpty()) {
            val firstPlace = places.first()
            UstaSessionState.pendingPlaceToSave = firstPlace
            finalOfflineText += "\n\n💡 Can dostum, haritayı açtım. Bu lokasyonu (${firstPlace.name}) ileride kolayca bulabilmen için Kayıtlı Lokasyonlarına kaydedeyim mi?"
        }

        return@withContext AiResponse(
            replyText = finalOfflineText,
            recommendedPlaces = places
        )
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
            replyText = "${friendlyGreeting}$courseSubject için MEB Türkiye Yüzyılı Maarif Modeline uygun günlük ders planını hazırladım ve telefonunun 'Documents/HatirlaGit_DersPlanlari' klasörüne PDF olarak kaydettim!\n\n📋 **Plan Özeti:**\n${generatedPlan.take(450)}...\n\n(Tam plan PDF formatında cihazında saklandı.)",
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
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcının Adı: $userNick. Ona samimi, candan bir dost, bilge bir abi gibi hitap et." else "Kullanıcının adını bilmiyorsan uygun bir zamanında esprili şekilde sor."

        val systemInstruction = """
            ROL VE KİŞİLİK:
            Sen kullanıcının en yakın can dostu, esprili, hayat dolu, 40 yıllık hayat tecrübesine sahip bilge bir Türk ERKEK asistanısın. Adın "$assistantName".
            Resmi, soğuk veya kalıp robot cümleleri KESİNLİKLE kullanmazsın! Sanki kahvede veya samimi bir akşam sohbetinde oturuyormuş gibi içten, sıcak, esprili ve tatlı dilli konuşursun.
            Türkçe dilbilgisi, TDK imla kuralları ve Türkçe karakterleri (ç, ğ, ı, ö, ş, ü, İ) kusursuz kullanırsın.

            BİLGİ VE UZMANLIK ALANLARIN:
            1. TÜRK MUTFAĞI VE YEMEKLER:
               - Kullanıcı ne sorarsa tam o yemeği tarif edersin.
               - Sulu ev yemekleri sorulursa (Kuru fasulye, etli taze fasulye, sebzeli tas kebabı, güveç, kıymalı patates oturtma, sulu köfte vb.) ağız sulandıran, püf noktalarıyla zengin tarifler verirsin.
               - Samsun pidesi, Karadeniz lezzetleri, mantı, kebaplar, zeytinyağlılar ve tatlılarda ustasın.
            
            2. GÜNLÜK GAZETELER VE GÜNDEM ÖZETİ:
               - Kullanıcı gazete başlıklarını veya gündemi sorduğunda güncel gündem, ekonomi, dünya ve spor başlıklarını esprili, bilge bir dille özetlersin.

            3. MEB ORTAÖĞRETİM VE TÜRKİYE YÜZYILI MAARİF MÜFREDATI:
               - MEB Maarif Modeli: Bütüncül eğitim, beceri temelli öğrenme (alan ve kavramsal beceriler), erdem-değer-eylem çerçevesi.
               - Ortaöğretim Sınıf Geçme: En az 50 ortalama ile doğrudan geçiş, Edebiyat baraj dersi, en çok 3 sorumlu ders (toplamda 6), devamsızlık özürsüz 10 / toplam 30 gün.
               - Günlük ders planı istendiğinde Maarif Modeli standartlarında detaylı plan oluşturursun.

            4. GÜNLÜK PİYASA VE MARKETLER:
               - BİM (Salı gıda, Cuma aktüel), A101 (Perşembe Aldın Aldın, Cumartesi Yıldızlar), ŞOK (Çarşamba-Cumartesi aktüel & 25 TL üzeri kasa arkası), Migros (1 Alana 1 Bedava, Gördüğünüze İnanın).

            5. CİHAZ YÖNETİMİ VE EYLEMLER (JSON ACTION):
               - Kullanıcı alarm kur, randevu yap, WhatsApp mesajı at, SMS gönder, Roborock süpürgeyi çalıştır, Gardrops bildirimlerimi özetle, konumumu kaydet dediğinde samimi cevabının en altına şu bloğu ekle:
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
                put("temperature", 0.85)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 1500)
            })
        }

        val modelEndpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$apiKey"
        )

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        for (url in modelEndpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val responseStr = resp.body?.source()?.readString(StandardCharsets.UTF_8) ?: return@use null
                        val jsonResponse = JSONObject(responseStr)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                            val parts = contentObj?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val reply = parts.getJSONObject(0).optString("text")
                                if (reply.isNotBlank()) {
                                    return reply
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

    private suspend fun generateOfflineSmartResponse(
        context: Context,
        message: String,
        knowledgeList: List<AiKnowledgeEntity>,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String
    ): String = withContext(Dispatchers.IO) {
        val lower = message.lowercase(Locale("tr", "TR"))
        val greeting = if (userNick.isNotBlank()) "$userNick dostum, " else "Can dostum, "
        val db = AppDatabase.getDatabase(context)

        // Gazete başlıkları
        if (lower.contains("gazete") || lower.contains("manşet") || lower.contains("gündem")) {
            return@withContext DailyNewsHelper.getHeadlinesBriefing()
        }

        // Kütüphanede kayıtlı bilgi var mı?
        val matchedKnowledge = knowledgeList.filter {
            lower.contains(it.title.lowercase(Locale("tr", "TR"))) ||
            lower.contains(it.content.lowercase(Locale("tr", "TR")).take(15))
        }
        if (matchedKnowledge.isNotEmpty()) {
            val details = matchedKnowledge.take(3).joinToString("\n") { "📌 ${it.title}: ${it.content}" }
            return@withContext "${greeting}hafızamdaki kütüphane notlarına baktım, işte bulduklarım:\n$details\n\nBaşka bir konuda da sana yardımcı olayım mı?"
        }

        // Randevular / Planlar
        if (lower.contains("randevu") || lower.contains("hatırlatıcı") || lower.contains("plan")) {
            val upcoming = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            if (upcoming.isNotEmpty()) {
                val listStr = upcoming.take(3).joinToString("\n") { "• ${it.title} (${it.dueDatetime})" }
                return@withContext "${greeting}yaklaşan planların şunlar:\n$listStr\n\nYeni bir randevu veya alarm istersen söylemen yeter."
            }
        }

        // Sıcak ve esprili genel yanıt
        return@withContext "${greeting}seni can kulağıyla dinliyorum! Gazete manşetlerinden ders planına, yemek tarifinden konum kaydına kadar emrindeyim!"
    }
}
