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
                Pair(41.2867, 36.3300) // Samsun koordinatlarÄ±
            }
        } catch (e: Exception) {
            Pair(41.2867, 36.3300)
        }
    }

    private val NAME_BLACKLIST = setOf(
        "bul", "sÃ¶yle", "gÃ¶ster", "kur", "hatÄ±rlat", "getir", "aÃ§", "yaz", "ekle", "kaydet",
        "migros", "eczane", "market", "hastane", "otopark", "fatura", "doktor", "hava",
        "nerede", "nasÄ±l", "nedir", "yardÄ±m", "usta", "antigravity", "alarm", "randevu",
        "tarih", "maarif", "mÃ¼fredat", "sÄ±nÄ±f", "yemek", "tarif"
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

        // Asistan adÄ±yla Ã§aÄŸrÄ±lma kontrolÃ¼
        val assistantLower = assistantName.lowercase(Locale("tr", "TR"))
        val msgLower = cleanMsg.lowercase(Locale("tr", "TR"))
        if (msgLower.startsWith(assistantLower)) {
            cleanMsg = cleanMsg.substring(assistantName.length).trimStart(',', ':', ' ', '-')
        }
        val lowerMsg = cleanMsg.lowercase(Locale("tr", "TR"))

        // GerÃ§ek GPS KoordinatÄ± ve Ä°l/Ä°lÃ§e tespiti
        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // 1. Kesin Ä°sim Ã–ÄŸrenme KuralÄ±
        val explicitNamePatterns = listOf(
            Pattern.compile("""(?i)^benim adÄ±m\s+([A-Za-zÃ‡ÄÄ°Ã–ÅÃœÃ§ÄŸÄ±Ã¶ÅŸÃ¼]+)$"""),
            Pattern.compile("""(?i)^adÄ±m\s+([A-Za-zÃ‡ÄÄ°Ã–ÅÃœÃ§ÄŸÄ±Ã¶ÅŸÃ¼]+)$"""),
            Pattern.compile("""(?i)^ismim\s+([A-Za-zÃ‡ÄÄ°Ã–ÅÃœÃ§ÄŸÄ±Ã¶ÅŸÃ¼]+)$"""),
            Pattern.compile("""(?i)bana\s+([A-Za-zÃ‡ÄÄ°Ã–ÅÃœÃ§ÄŸÄ±Ã¶ÅŸÃ¼]+)\s+diye\s+hitap\s+et"""),
            Pattern.compile("""(?i)bana\s+([A-Za-zÃ‡ÄÄ°Ã–ÅÃœÃ§ÄŸÄ±Ã¶ÅŸÃ¼]+)\s+diyebilirsin""")
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
                            replyText = "TanÄ±ÅŸtÄ±ÄŸÄ±ma Ã§ok memnun oldum $candidateName dostum! Ä°smini hafÄ±zama yazdÄ±m. $userCity'de gÃ¼nÃ¼n nasÄ±l geÃ§iyor, bugÃ¼n ne yapÄ±yoruz?"
                        )
                    }
                }
            }
        }

        // 2. Park Yeri Kaydetme
        if (lowerMsg.contains("park yerimi kaydet") || lowerMsg.contains("arabayÄ± buraya park") || 
            lowerMsg.contains("arabamÄ± kaydet") || lowerMsg.contains("park konumumu kaydet") || lowerMsg.contains("buraya park ettim")) {
            dataStoreManager.saveParkedCarLocation(
                lat = realLat.toString(),
                lng = realLng.toString(),
                time = System.currentTimeMillis()
            )
            val greeting = if (currentNick.isNotBlank()) "$currentNick dostum, " else "Reis, "
            return@withContext AiResponse(
                replyText = "${greeting}aracÄ±nÄ±n park konumunu $userCity $userDistrict olarak hafÄ±zama aldÄ±m. DilediÄŸinde 'Arabam nerede' de, tek tÄ±kla seni yanÄ±na gÃ¶tÃ¼reyim.",
                actionSummary = "ğŸš— Park konumu kaydedildi"
            )
        }

        // 3. KÃ¼tÃ¼phane Bilgileri (Yerel Room DB)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ Ã–ÄRETTÄ°ÄÄ° TÃœM NOTLAR VE BÄ°LGÄ°LER:\n" + 
            allKnowledgeList.take(25).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "KullanÄ±cÄ± henÃ¼z Ã¶zel bir kÃ¼tÃ¼phane notu eklemedi."

        // 4. Google Gemini API Ã‡aÄŸrÄ±sÄ± (UTF-8 Garantili & Zengin Bilgi DaÄŸarcÄ±klÄ±)
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

                // Migros / Eczane / Yer arama kartlarÄ±
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

        // 5. AkÄ±llÄ± Ã‡evrimdÄ±ÅŸÄ± TÃ¼rkÃ§e YanÄ±t Motoru
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
        val userGreeting = if (userNick.isNotBlank()) "KullanÄ±cÄ±nÄ±n AdÄ±: $userNick. Ona samimi, saygÄ±lÄ±, gÃ¼ven veren bir bilge gibi hitap et." else "KullanÄ±cÄ±nÄ±n adÄ±nÄ± bilmiyorsan uygun bir zamanda sor."

        val systemInstruction = """
            ROL VE KÄ°MLÄ°K:
            Sen kullanÄ±cÄ±nÄ±n kiÅŸisel Android cihazÄ± Ã¼zerinde Ã§alÄ±ÅŸan, Jarvis yeteneklerine ve 40 yÄ±llÄ±k bilge hayat tecrÃ¼besine sahip kÃ¼ltÃ¼rlÃ¼ yerli ERKEK asistansÄ±n. AdÄ±n "$assistantName".
            TÃ¼rkÃ§e dilbilgisi, TDK imla kurallarÄ± ve TÃ¼rkÃ§e karakterleri (Ã§, ÄŸ, Ä±, Ã¶, ÅŸ, Ã¼, Ä°) kusursuz kullanÄ±rsÄ±n. KonuÅŸman akÄ±cÄ±, tok sesli ve doÄŸrudan hedefe yÃ¶neliktir.

            GENÄ°ÅLETÄ°LMÄ°Å BÄ°LGÄ° ALANLARIN VE UZMANLIKLARIN:
            1. TÃœRK TARÄ°HÄ° VE KÃœLTÃœRÃœ:
               - Ä°slamiyet Ã–ncesi TÃ¼rk Tarihi (GÃ¶ktÃ¼rkler, Uygurlar, Hunlar), SelÃ§uklular, OsmanlÄ± Devleti (kuruluÅŸ, yÃ¼kselme, antlaÅŸmalar, savaÅŸlar).
               - KurtuluÅŸ SavaÅŸÄ±, Milli MÃ¼cadele, Mustafa Kemal AtatÃ¼rk, ilkeleri ve devrimleri, Nutuk.
               - Klasik TÃ¼rk Eserleri: Dede Korkut Hikayeleri, Kutadgu Bilig, Divan-Ä± Lugati't-TÃ¼rk, Mesnevi.

            2. MEB TÃœRKÄ°YE YÃœZYILI MAARÄ°F MODELÄ° VE ORTAÃ–ÄRETÄ°M MÃœFREDATI:
               - Lise 9, 10, 11 ve 12. sÄ±nÄ±f Tarih dersi konu ve kazanÄ±mlarÄ±.
               - OrtaÃ¶ÄŸretim SÄ±nÄ±f GeÃ§me ve SÄ±nav YÃ¶netmeliÄŸi:
                 * YÄ±l sonu baÅŸarÄ± puanÄ± en az 50 olan Ã¶ÄŸrenciler doÄŸrudan sÄ±nÄ±f geÃ§er.
                 * Baraj dersi (TÃ¼rk Dili ve EdebiyatÄ±) zayÄ±f olan Ã¶ÄŸrenci ortalamasÄ± 50'nin Ã¼stÃ¼nde olsa bile o dersten sorumlu geÃ§er.
                 * En fazla 3 dersten sorumlu olarak Ã¼st sÄ±nÄ±fa geÃ§ilebilir; toplamda en fazla 6 sorumlu ders birikebilir.
                 * DevamsÄ±zlÄ±k sÄ±nÄ±rÄ±: Ã–zÃ¼rsÃ¼z 10 gÃ¼n, toplam (Ã¶zÃ¼rlÃ¼+Ã¶zÃ¼rsÃ¼z) 30 gÃ¼ndÃ¼r. SÄ±nÄ±rÄ± aÅŸan Ã¶ÄŸrenci sÄ±nÄ±f tekrarÄ±na kalÄ±r.

            3. 81 Ä°L GEZÄ° REHBERÄ° VE YÃ–RESEL MUTFAK:
               - TÃ¼rkiye'nin tÃ¼m illerinin tarihi mekanlarÄ±, doÄŸal gÃ¼zellikleri, mÃ¼zeleri.
               - Geleneksel TÃ¼rk yemekleri ve ayrÄ±ntÄ±lÄ± tarifleri (Samsun pidesi, Karadeniz yemekleri, mantÄ±, kebaplar, tatlÄ±lar).

            4. GÃœNLÃœK ASÄ°STAN YETENEKLERÄ° (JARVIS PROTOKOLÃœ):
               - Alarm, randevu, takvim oluÅŸturma (AkÄ±llÄ± saat senkronizasyonu iÃ§in takvim onayÄ± sorulur).
               - Migros, sÃ¼permarket, nÃ¶betÃ§i eczane, hastane ve otopark canlÄ± navigasyonu (OPEN_MAPS).
               - Park yeri kaydetme (SAVE_PARK_LOCATION) ve hatÄ±rlatma.
               - WhatsApp mesajÄ± hazÄ±rlama ve telefon arama.

            5. EYLEM FORMATI (JSON ACTION):
            GerektiÄŸinde cevabÄ±n altÄ±na ÅŸu bloÄŸu ekle:
            ```action
            {
              "action_type": "SET_ALARM" | "CREATE_EVENT" | "SEND_WHATSAPP" | "OPEN_MAPS" | "CALL_PHONE" | "SAVE_PARK_LOCATION",
              "payload": {
                "hour": 9,
                "minute": 30,
                "title": "BaÅŸlÄ±k",
                "message": "AÃ§Ä±klama",
                "phone": "05xxxxxxxxx",
                "query": "$userCity Migros",
                "startTimeMillis": 1725370000000
              }
            }
            ```

            $userGreeting
            KullanÄ±cÄ± ÅŸu an TÃ¼rkiye'de $userCity ili, $userDistrict ilÃ§esindedir.
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

        if (lower.contains("tarih") || lower.contains("maarif") || lower.contains("sÄ±nÄ±f geÃ§me")) {
            return@withContext "${greeting}MEB OrtaÃ¶ÄŸretim YÃ¶netmeliÄŸine gÃ¶re yÄ±l sonu baÅŸarÄ± puanÄ± en az 50 olan Ã¶ÄŸrenci doÄŸrudan sÄ±nÄ±f geÃ§er. TÃ¼rk Dili ve EdebiyatÄ± baraj derstir; devamsÄ±zlÄ±k ise Ã¶zÃ¼rsÃ¼z en fazla 10, toplamda 30 gÃ¼ndÃ¼r."
        }

        if (lower.contains("yemek") || lower.contains("tarif") || lower.contains("pide")) {
            return@withContext "${greeting}Samsun kapalÄ± kÄ±ymalÄ± pidesi iÃ§in mayalÄ± hamur dinlendirilir; dana kÄ±yma, bol soÄŸan ve karabiberle harÃ§ kavrulur. FÄ±rÄ±ndan Ã§Ä±kÄ±nca Ã¼zerine hakiki tereyaÄŸÄ± sÃ¼rÃ¼lerek sÄ±cak servis edilir!"
        }

        if (lower.contains("migros") || lower.contains("market")) {
            return@withContext "${greeting}$userCity $userDistrict bÃ¶lgesindeki Migros ve sÃ¼permarketleri listeledim. Haritadan hemen yol tarifi alabilirsin."
        }

        if (lower.contains("eczane") || lower.contains("nÃ¶betÃ§i")) {
            return@withContext "${greeting}$userCity $userDistrict nÃ¶betÃ§i eczanelerini listeledim. Haritadan yol tarifi alabilir veya tek tÄ±kla arayabilirsin."
        }

        return@withContext "${greeting}seni dinliyorum! Tarih, Maarif mÃ¼fredatÄ±, yemek tarifleri, alarm veya konumla ilgili dilediÄŸini sorabilirsin."
    }
}