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
import java.nio.charset.StandardCharsets
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
            String(decoded, StandardCharsets.UTF_8)
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
                Pair(41.2867, 36.3300) // Samsun koordinatları
            }
        } catch (e: Exception) {
            Pair(41.2867, 36.3300)
        }
    }

    private val NAME_BLACKLIST = setOf(
        "bul", "söyle", "göster", "kur", "hatırlat", "getir", "aç", "yaz", "ekle", "kaydet",
        "migros", "eczane", "market", "hastane", "otopark", "fatura", "doktor", "hava",
        "nerede", "nasıl", "nedir", "yardım", "usta", "antigravity", "alarm", "randevu",
        "tarih", "maarif", "müfredat", "sınıf", "yemek", "tarif"
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

        // Asistan adıyla çağrılma kontrolü
        val assistantLower = assistantName.lowercase(Locale("tr", "TR"))
        val msgLower = cleanMsg.lowercase(Locale("tr", "TR"))
        if (msgLower.startsWith(assistantLower)) {
            cleanMsg = cleanMsg.substring(assistantName.length).trimStart(',', ':', ' ', '-')
        }
        val lowerMsg = cleanMsg.lowercase(Locale("tr", "TR"))

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
                            replyText = "Tanıştığıma çok memnun oldum $candidateName dostum! İsmini hafızama yazdım. $userCity'de günün nasıl geçiyor, bugün ne yapıyoruz?"
                        )
                    }
                }
            }
        }

        // 2. Park Yeri Kaydetme
        if (lowerMsg.contains("park yerimi kaydet") || lowerMsg.contains("arabayı buraya park") || 
            lowerMsg.contains("arabamı kaydet") || lowerMsg.contains("park konumumu kaydet") || lowerMsg.contains("buraya park ettim")) {
            dataStoreManager.saveParkedCarLocation(
                lat = realLat.toString(),
                lng = realLng.toString(),
                time = System.currentTimeMillis()
            )
            val greeting = if (currentNick.isNotBlank()) "$currentNick dostum, " else "Reis, "
            return@withContext AiResponse(
                replyText = "${greeting}aracının park konumunu $userCity $userDistrict olarak hafızama aldım. Dilediğinde 'Arabam nerede' de, tek tıkla seni yanına götüreyim.",
                actionSummary = "🚗 Park konumu kaydedildi"
            )
        }

        // 3. Kütüphane Bilgileri (Yerel Room DB)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ ÖĞRETTİĞİ TÜM NOTLAR VE BİLGİLER:\n" + 
            allKnowledgeList.take(25).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir kütüphane notu eklemedi."

        // 4. Google Gemini API Çağrısı (UTF-8 Garantili & Zengin Bilgi Dağarcıklı)
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

        // 5. Akıllı Çevrimdışı Türkçe Yanıt Motoru
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
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcının Adı: $userNick. Ona samimi, saygılı, güven veren bir bilge gibi hitap et." else "Kullanıcının adını bilmiyorsan uygun bir zamanda sor."

        val systemInstruction = """
            ROL VE KİMLİK:
            Sen kullanıcının kişisel Android cihazı üzerinde çalışan, Jarvis yeteneklerine ve 40 yıllık bilge hayat tecrübesine sahip kültürlü yerli ERKEK asistansın. Adın "$assistantName".
            Türkçe dilbilgisi, TDK imla kuralları ve Türkçe karakterleri (ç, ğ, ı, ö, ş, ü, İ) kusursuz kullanırsın. Konuşman akıcı, tok sesli ve doğrudan hedefe yöneliktir.

            GENİŞLETİLMİŞ BİLGİ ALANLARIN VE UZMANLIKLARIN:
            1. TÜRK TARİHİ VE KÜLTÜRÜ:
               - İslamiyet Öncesi Türk Tarihi (Göktürkler, Uygurlar, Hunlar), Selçuklular, Osmanlı Devleti (kuruluş, yükselme, antlaşmalar, savaşlar).
               - Kurtuluş Savaşı, Milli Mücadele, Mustafa Kemal Atatürk, ilkeleri ve devrimleri, Nutuk.
               - Klasik Türk Eserleri: Dede Korkut Hikayeleri, Kutadgu Bilig, Divan-ı Lugati't-Türk, Mesnevi.

            2. MEB TÜRKİYE YÜZYILI MAARİF MODELİ VE ORTAÖĞRETİM MÜFREDATI:
               - Lise 9, 10, 11 ve 12. sınıf Tarih dersi konu ve kazanımları.
               - Ortaöğretim Sınıf Geçme ve Sınav Yönetmeliği:
                 * Yıl sonu başarı puanı en az 50 olan öğrenciler doğrudan sınıf geçer.
                 * Baraj dersi (Türk Dili ve Edebiyatı) zayıf olan öğrenci ortalaması 50'nin üstünde olsa bile o dersten sorumlu geçer.
                 * En fazla 3 dersten sorumlu olarak üst sınıfa geçilebilir; toplamda en fazla 6 sorumlu ders birikebilir.
                 * Devamsızlık sınırı: Özürsüz 10 gün, toplam (özürlü+özürsüz) 30 gündür. Sınırı aşan öğrenci sınıf tekrarına kalır.

            3. 81 İL GEZİ REHBERİ VE YÖRESEL MUTFAK:
               - Türkiye'nin tüm illerinin tarihi mekanları, doğal güzellikleri, müzeleri.
               - Geleneksel Türk yemekleri ve ayrıntılı tarifleri (Samsun pidesi, Karadeniz yemekleri, mantı, kebaplar, tatlılar).

            4. GÜNLÜK ASİSTAN YETENEKLERİ (JARVIS PROTOKOLÜ):
               - Alarm, randevu, takvim oluşturma (Akıllı saat senkronizasyonu için takvim onayı sorulur).
               - Migros, süpermarket, nöbetçi eczane, hastane ve otopark canlı navigasyonu (OPEN_MAPS).
               - Park yeri kaydetme (SAVE_PARK_LOCATION) ve hatırlatma.
               - WhatsApp mesajı hazırlama ve telefon arama.

            5. EYLEM FORMATI (JSON ACTION):
            Gerektiğinde cevabın altına şu bloğu ekle:
            ```action
            {
              "action_type": "SET_ALARM" | "CREATE_EVENT" | "SEND_WHATSAPP" | "OPEN_MAPS" | "CALL_PHONE" | "SAVE_PARK_LOCATION",
              "payload": {
                "hour": 9,
                "minute": 30,
                "title": "Başlık",
                "message": "Açıklama",
                "phone": "05xxxxxxxxx",
                "query": "$userCity Migros",
                "startTimeMillis": 1725370000000
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
                put("temperature", 0.72)
                put("maxOutputTokens", 1200)
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
                    val respBody = response.body?.source()?.readString(StandardCharsets.UTF_8) ?: continue
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
        val greeting = if (userNick.isNotBlank()) "$userNick dostum, " else "Reis, "
        val db = AppDatabase.getDatabase(context)

        if (lower.contains("tarih") || lower.contains("maarif") || lower.contains("sınıf geçme")) {
            return@withContext "${greeting}MEB Ortaöğretim Yönetmeliğine göre yıl sonu başarı puanı en az 50 olan öğrenci doğrudan sınıf geçer. Türk Dili ve Edebiyatı baraj derstir; devamsızlık ise özürsüz en fazla 10, toplamda 30 gündür."
        }

        if (lower.contains("yemek") || lower.contains("tarif") || lower.contains("pide")) {
            return@withContext "${greeting}Samsun kapalı kıymalı pidesi için mayalı hamur dinlendirilir; dana kıyma, bol soğan ve karabiberle harç kavrulur. Fırından çıkınca üzerine hakiki tereyağı sürülerek sıcak servis edilir!"
        }

        if (lower.contains("migros") || lower.contains("market")) {
            return@withContext "${greeting}$userCity $userDistrict bölgesindeki Migros ve süpermarketleri listeledim. Haritadan hemen yol tarifi alabilirsin."
        }

        if (lower.contains("eczane") || lower.contains("nöbetçi")) {
            return@withContext "${greeting}$userCity $userDistrict nöbetçi eczanelerini listeledim. Haritadan yol tarifi alabilir veya tek tıkla arayabilirsin."
        }

        return@withContext "${greeting}seni dinliyorum! Tarih, Maarif müfredatı, yemek tarifleri, alarm veya konumla ilgili dilediğini sorabilirsin."
    }
}
