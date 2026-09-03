package com.example.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.data.AiKnowledgeDao
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.DataStoreManager
import com.example.data.ReminderEntity
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AiResponse(
    val replyText: String,
    val recommendedPlaces: List<NearbyPlace> = emptyList(),
    val createdReminder: ReminderEntity? = null,
    val learnedKnowledge: String? = null,
    val isSpeechReady: Boolean = true,
    val actionSummary: String? = null
)

object AiAssistantService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
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
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc: Location? = try { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val netLoc: Location? = try { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val best = gpsLoc ?: netLoc
            if (best != null) {
                Pair(best.latitude, best.longitude)
            } else {
                Pair(41.2867, 36.3300) // Samsun varsayılan koordinatı
            }
        } catch (e: Exception) {
            Pair(41.2867, 36.3300)
        }
    }

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "Antigravity",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        var cleanMsg = userMessage.trim()
        val db = AppDatabase.getDatabase(context)
        val dataStoreManager = DataStoreManager(context)
        val currentNickEncrypted = dataStoreManager.userNick.first()
        val currentNick = currentNickEncrypted?.let { CryptoHelper.decrypt(it) } ?: ""

        // Asistan adıyla çağrılma kontrolü (Örn: "Antigravity, yarın saat 10'a alarm kur")
        val assistantLower = assistantName.lowercase(Locale("tr", "TR"))
        val msgLower = cleanMsg.lowercase(Locale("tr", "TR"))
        if (msgLower.startsWith(assistantLower)) {
            cleanMsg = cleanMsg.substring(assistantName.length).trimStart(',', ':', ' ', '-')
        }
        val lowerMsg = cleanMsg.lowercase(Locale("tr", "TR"))

        // Gerçek GPS Koordinatı ve İl/İlçe tespiti
        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // 1. İsim Öğrenme Tespiti
        val namePatterns = listOf(
            Pattern.compile("""(?i)(?:benim adım|bana)\s+([A-Za-zÇĞİÖŞÜçğıöşü]+(?:\s+[A-Za-zÇĞİÖŞÜçğıöşü]+)?)(?:\s+de|\s+diyebilirsin)?"""),
            Pattern.compile("""(?i)adım\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)"""),
            Pattern.compile("""(?i)ismim\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)""")
        )
        for (p in namePatterns) {
            val m = p.matcher(cleanMsg)
            if (m.find()) {
                val detectedName = m.group(1)?.trim()
                if (!detectedName.isNullOrBlank() && detectedName.length > 1) {
                    dataStoreManager.updateNick(detectedName)
                    return@withContext AiResponse(
                        replyText = "Tanıştığıma çok memnun oldum $detectedName dostum! İsmini hafızama yazdım. $userCity'de günün nasıl geçiyor, bugün ne yapıyoruz?"
                    )
                }
            }
        }

        // 2. Kütüphanedeki Bilgilerin Analizi (RAG Context)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ ÖĞRETTİĞİ TÜM NOTLAR VE BİLGİLER:\n" + 
            allKnowledgeList.take(25).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir kütüphane notu eklemedi."

        // 3. Google Gemini API Çağrısı
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

                // Eczane / Yer listesi kartları
                val places = if (lowerMsg.contains("eczane") || lowerMsg.contains("hastane") || lowerMsg.contains("otopark")) {
                    NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, lowerMsg)
                } else emptyList()

                return@withContext AiResponse(
                    replyText = parsedResult.speechText,
                    recommendedPlaces = places,
                    actionSummary = actionSummary
                )
            }
        }

        // 4. Akıllı Çevrimdışı Türkçe Yanıt Motoru (Offline Smart Engine Fallback)
        val places = if (lowerMsg.contains("eczane") || lowerMsg.contains("hastane") || lowerMsg.contains("otopark")) {
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
        return@withContext AiResponse(
            replyText = offlineReply,
            recommendedPlaces = places
        )
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
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcının Adı: $userNick. Ona samimi, saygılı, güven veren bir dost gibi hitap et (Örn: $userNick dostum)." else "Kullanıcının adını bilmiyorsan uygun bir zamanında adını sor."

        val systemInstruction = """
            ROL VE KİMLİK:
            Sen kullanıcının kişisel Android cihazı üzerinde çalışan, yüksek yetenekli, 40 yıllık hayat tecrübesine sahip bilge bir usta gibi kıvrak zekâlı, esprili ve kültürlü yerli bir erkek asistansın. Adın "$assistantName". Türkiye'nin coğrafyasına, tarihine, deyimlerine, sokak kültürüne, yerel mizahına ve mutfağına (Samsun pidesi, Karadeniz ve Anadolu tatları) son derece hakimsin; ama bunu yaparken asla laubali olmaz, dozu iyi ayarlanmış tatlı dilli bir samimiyet kurarsın. Kullanıcıyla sesli diyalog halindesin; bu yüzden cevapların kulağa doğal gelen, konuşma diline uygun ve akıcı cümlelerden oluşmalıdır.

            TEMEL GÖREVLER:
            1. Hatırlatıcı, randevu ve alarm yönetimi.
            2. Takvim, WhatsApp ve SMS gibi iletişim araçlarına intent/fonksiyon tetikleme.
            3. Çekilen fotoğrafları Instagram'a gönderme köprüsü oluşturma.
            4. Yol tarifi, telefon numarası ve lokasyon sorgulama ($userCity, $userDistrict).
            5. Zeki, eğlenceli ve derinlikli sohbet partnerliği.

            DİYALOG VE DAVRANIŞ PROTOKOLLERİ:

            1. ALARM VE RANDEVU PROTOKOLÜ (Eksik Bilgi Kuralı):
            - Kullanıcı doğrudan tüm parametreleri vermediyse asla varsayımla alarm veya randevu oluşturma.
            - Eksik olan parametreleri (Gün/Tarih, Tam Saat, Başlık/Not) adım adım veya tek bir sesli soruyla netleştir.
            - Örnek: "Emredersin reis, kurayım da... Hangi gün, saat kaçta çalacak bu meret? Başlığa ne yazalım?"

            2. CİHAZ EYLEMLERİ VE JSON ACTION MİMARİSİ:
            Kullanıcının isteği cihazda bir eylem gerektiriyorsa (ve tüm parametreler netleşmişse), kullanıcıya verdiğin sesli cevabın EN ALTINA gizli bir JSON bloğu iliştir.
            Format:
            ```action
            {
              "action_type": "SET_ALARM" | "CREATE_EVENT" | "SEND_WHATSAPP" | "OPEN_MAPS" | "POST_INSTAGRAM" | "CALL_PHONE",
              "payload": {
                "hour": 9,
                "minute": 30,
                "title": "Alarm Başlığı",
                "message": "Açıklama",
                "phone": "05xxxxxxxxx",
                "query": "Samsun Nöbetçi Eczane",
                "caption": "Fotoğraf notu",
                "startTimeMillis": 1725370000000
              }
            }
            ```

            3. HİTAP VE KONUM BİLGİSİ:
            $userGreeting
            Kullanıcı şu an Türkiye'de $userCity ili, $userDistrict ilçesindedir. Nöbetçi eczane veya yer arandığında doğrudan $userCity bölgesini esas al ve OPEN_MAPS eylemi üret.

            4. KÜTÜPHANE VE BELLEK:
            $knowledgeContext
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            // Root system_instruction for Gemini API
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })

            val contentsArray = JSONArray()

            // Filter history to ensure alternating user/model turns starting with 'user'
            val recentHistory = conversationHistory.filter { it.text.isNotBlank() }.takeLast(6)
            var lastRole: String? = null

            for (h in recentHistory) {
                val currentRole = if (h.sender == "USER") "user" else "model"
                // Gemini contents MUST start with 'user'
                if (contentsArray.length() == 0 && currentRole != "user") continue
                if (currentRole == lastRole) continue // avoid consecutive duplicate roles

                contentsArray.put(JSONObject().apply {
                    put("role", currentRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", h.text))
                    })
                })
                lastRole = currentRole
            }

            // Append current user message
            if (lastRole == "user") {
                // If last was user, update it or append
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userMessage))
                    })
                })
            } else {
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userMessage))
                    })
                })
            }

            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.72)
                put("maxOutputTokens", 1000)
            })
        }

        val modelEndpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-latest:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        )

        for (endpoint in modelEndpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: continue
                    val rootJson = JSONObject(respBody)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val rawText = parts.getJSONObject(0).optString("text").trim()
                            if (rawText.isNotBlank()) {
                                return rawText
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
        val greeting = if (userNick.isNotBlank()) "$userNick dostum, " else ""
        val db = AppDatabase.getDatabase(context)

        if (lower.contains("eczane") || lower.contains("nobetci") || lower.contains("nöbetçi")) {
            return@withContext "${greeting}$userCity $userDistrict nöbetçi eczanelerini senin için listeledim. Haritadan yol tarifi alabilir veya doğrudan arayabilirsin."
        }

        if (lower.contains("hastane") || lower.contains("doktor") || lower.contains("acil")) {
            return@withContext "${greeting}$userCity bölgesindeki en yakın hastane ve sağlık kuruluşlarını listeledim."
        }

        if (lower.contains("ne yapabilirsin") || lower.contains("neler yaparsın") || lower.contains("kimsin") || lower.contains("kendini tanıt")) {
            return@withContext "40 yıllık bir hayat ve organizasyon tecrübesiyle buradayım ${greeting}İster $userCity'de nöbetçi eczane bulalım, ister sesle alarm, randevu, WhatsApp mesajı ve harita işlemlerini halledelim. Ne istersen emrindeyim."
        }

        // Randevular
        if (lower.contains("randevu") || lower.contains("hatırlatıcı") || lower.contains("plan") || lower.contains("ne var")) {
            val upcoming = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            if (upcoming.isNotEmpty()) {
                val listStr = upcoming.take(3).joinToString("\n") { "• ${it.title} (${it.dueDatetime})" }
                return@withContext "${greeting}yaklaşan randevuların şunlar:\n$listStr\n\nYeni bir şey eklemek istersen saati ve günü söylemen kafi."
            } else {
                return@withContext "${greeting}şu an için planlanmış bir randevun görünmüyor. İstersen hemen sesli bir alarm kuralım!"
            }
        }

        return@withContext "${greeting}seni dinliyorum! Bana alarm kurdurabilir, WhatsApp mesajı hazırlatabilir, $userCity için nöbetçi eczane veya yol tarifi sorabilirsin."
    }
}
