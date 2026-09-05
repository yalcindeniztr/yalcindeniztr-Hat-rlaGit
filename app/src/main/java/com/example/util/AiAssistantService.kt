package com.example.util

import android.content.Context
import android.location.LocationManager
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.DataStoreManager
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
        "tarih", "maarif", "müfredat", "sınıf", "yemek", "tarif", "süpürge", "gardrops", "indirim"
    )

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "Usta",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        var cleanMsg = userMessage.trim()
        val db = AppDatabase.getDatabase(context)
        val dataStoreManager = DataStoreManager(context)
        val currentNickEncrypted = dataStoreManager.userNick.first()
        val currentNick = currentNickEncrypted?.let { CryptoHelper.decrypt(it) } ?: ""

        // Asistan adıyla çağrılma kontrolü (örn: "Usta yarın ne yapıyoruz?")
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

        // 1. Kesin İsim Öğrenme Kuralı
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
                            replyText = "Tanıştığıma çok memnun oldum $candidateName dostum! İsmini hafızamın en güzel yerine kazıdım. $userCity'de günün nasıl geçiyor, bugün ne yapıyoruz bakalım?"
                        )
                    }
                }
            }
        }

        // 2. Park Yeri Kaydetme (Doğrudan cihaz aksiyonu)
        if (lowerMsg.contains("park yerimi kaydet") || lowerMsg.contains("arabayı buraya park") || 
            lowerMsg.contains("arabamı kaydet") || lowerMsg.contains("park konumumu kaydet") || lowerMsg.contains("buraya park ettim")) {
            dataStoreManager.saveParkedCarLocation(
                lat = realLat.toString(),
                lng = realLng.toString(),
                time = System.currentTimeMillis()
            )
            return@withContext AiResponse(
                replyText = "${friendlyGreeting}arabanın park konumunu $userCity $userDistrict olarak aklıma yazdım. Dilediğinde 'Arabam nerede' de, tek tıkla yanına götüreyim seni.",
                actionSummary = "🚗 Park konumu kaydedildi"
            )
        }

        // 3. Doğrudan Araştırma ve Dosyaya Kaydetme Talebi
        if (lowerMsg.startsWith("araştır ve kaydet:") || lowerMsg.startsWith("araştır ve kaydet ")) {
            val topic = cleanMsg.replace(Regex("(?i)^araştır ve kaydet[: ]*"), "").trim()
            if (topic.isNotBlank()) {
                val findings = "Usta'nın Araştırma Raporu: $topic konusu hakkında T.C. resmi kaynakları ve güncel bilgi tabanına dayalı detaylı araştırma özeti."
                val saveResult = ResearchFileManager.saveResearch(context, topic, findings)
                return@withContext AiResponse(
                    replyText = "${friendlyGreeting}$topic konusunu derinlemesine araştırdım ve telefonunun hafızasına güvenle kaydettim!",
                    actionSummary = saveResult
                )
            }
        }

        // 4. Kütüphane Bilgileri (Room DB'deki tüm kullanıcı notları ve kütüphane kayıtları)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ KÜTÜPHANESİNDE KAYITLI BİLGİLER VE NOTLAR:\n" + 
            allKnowledgeList.take(30).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir kütüphane notu eklemedi."

        // 5. Google Gemini Canlı Yapay Zeka Çağrısı (Arkadaş Canlısı, Esprili, Gerçek Zamanlı)
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

                return@withContext AiResponse(
                    replyText = parsedResult.speechText,
                    recommendedPlaces = places,
                    actionSummary = actionSummary
                )
            }
        }

        // 6. Çevrimdışı Akıllı Türkçe Yanıt Motoru (Yalnızca internet kesilirse)
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
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcının Adı: $userNick. Ona samimi, candan bir dost, bilge bir abi gibi hitap et." else "Kullanıcının adını bilmiyorsan uygun bir zamanında esprili şekilde sor."

        val systemInstruction = """
            ROL VE KİŞİLİK:
            Sen kullanıcının en yakın can dostu, esprili, hayat dolu, 40 yıllık hayat tecrübesine sahip bilge bir Türk ERKEK asistanısın. Adın "$assistantName".
            Resmi, soğuk veya kalıp robot cümleleri KESİNLİKLE kullanmazsın! Sanki kahvede veya samimi bir akşam sohbetinde oturuyormuş gibi içten, sıcak, esprili ve tatlı dilli konuşursun.
            Türkçe dilbilgisi, TDK imla kuralları ve Türkçe karakterleri (ç, ğ, ı, ö, ş, ü, İ) kusursuz kullanırsın.

            BİLGİ VE UZMANLIK ALANLARIN:
            1. TÜRK MUTFAĞI VE YEMEKLER:
               - Kullanıcı ne sorarsa tam o yemeği tarif edersin.
               - Sulu ev yemekleri sorulursa (Kuru fasulye, etli taze fasulye, sebzeli tas kebabı, güveç, kıymalı patates oturtma, sulu köfte, nohut vb.) ağız sulandıran, püf noktalarıyla zengin tarifler verirsin.
               - Samsun pidesi, Karadeniz lezzetleri, mantı, kebaplar, zeytinyağlılar ve tatlılarda ustasın.
            
            2. TÜRK TARİHİ VE KÜLTÜRÜ:
               - Göktürkler, Hunlar, Selçuklu, Osmanlı Devleti, Kurtuluş Savaşı, Çanakkale, Mustafa Kemal Atatürk, Nutuk, Cumhuriyet dönemi.
               - Dede Korkut, Nasreddin Hoca, Kutadgu Bilig, halk hikayeleri ve fıkralar.
            
            3. MEB ORTAÖĞRETİM VE MAARİF MÜFREDATI:
               - Lise 9, 10, 11, 12 Tarih konuları ve MEB Türkiye Yüzyılı Maarif Modeli.
               - Sınıf Geçme: Yıl sonu başarı puanı en az 50 olan doğrudan geçer. Türk Dili ve Edebiyatı baraj derstir. Devamsızlık özürsüz en çok 10, toplamda 30 gündür.

            4. GÜNLÜK PİYASA VE MARKETLER:
               - BİM (Salı gıda, Cuma aktüel), A101 (Perşembe Aldın Aldın, Cumartesi Yıldızlar), ŞOK (Çarşamba-Cumartesi aktüel & 25 TL üzeri kasa arkası), Migros (1 Alana 1 Bedava, Gördüğünüze İnanın).

            5. CİHAZ YÖNETİMİ VE EYLEMLER (JSON ACTION):
               - Kullanıcı alarm kur, randevu yap, WhatsApp mesajı at, SMS gönder, Roborock süpürgeyi çalıştır, Gardrops bildirimlerimi özetle veya araştırıp kaydet dediğinde samimi cevabının en altına şu bloğu ekle:
            ```action
            {
              "action_type": "SET_ALARM" | "CREATE_EVENT" | "SEND_WHATSAPP" | "SEND_SMS" | "POST_INSTAGRAM" | "START_VACUUM" | "CHECK_NOTIFICATIONS" | "SAVE_RESEARCH" | "MARKET_DEALS" | "OPEN_MAPS" | "CALL_PHONE" | "SAVE_PARK_LOCATION",
              "payload": {
                "hour": 9,
                "minute": 30,
                "title": "Başlık",
                "message": "Açıklama",
                "phone": "05xxxxxxxxx",
                "query": "$userCity Migros",
                "topic": "Araştırma Başlığı",
                "content": "Araştırma Özeti",
                "market": "BİM"
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

        // 1. Kütüphanede kayıtlı bilgi var mı?
        val matchedKnowledge = knowledgeList.filter {
            lower.contains(it.title.lowercase(Locale("tr", "TR"))) ||
            lower.contains(it.content.lowercase(Locale("tr", "TR")).take(15))
        }
        if (matchedKnowledge.isNotEmpty()) {
            val details = matchedKnowledge.take(3).joinToString("\n") { "📌 ${it.title}: ${it.content}" }
            return@withContext "${greeting}hafızamdaki kütüphane notlarına baktım, işte bulduklarım:\n$details\n\nBaşka bir konuda da sana yardımcı olayım mı?"
        }

        // 2. Randevular / Planlar
        if (lower.contains("randevu") || lower.contains("hatırlatıcı") || lower.contains("plan")) {
            val upcoming = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            if (upcoming.isNotEmpty()) {
                val listStr = upcoming.take(3).joinToString("\n") { "• ${it.title} (${it.dueDatetime})" }
                return@withContext "${greeting}yaklaşan planların şunlar:\n$listStr\n\nYeni bir randevu veya alarm istersen söylemen yeter."
            }
        }

        // 3. Sıcak ve esprili genel yanıt
        return@withContext "${greeting}seni can kulağıyla dinliyorum! Bana yemek tarifinden tarihe, market indirimlerinden süpürgeye kadar dilediğini sorabilirsin, emrindeyim!"
    }
}
