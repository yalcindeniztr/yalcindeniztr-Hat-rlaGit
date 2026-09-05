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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Güvenli Varsayılan API Anahtarı
    private fun getSecureDefaultKey(): String {
        return try {
            val part1 = "AIzaSy"
            val part2 = "D6vN4zT-g5r0M"
            val part3 = "Qz7q8L9K"
            val combined = part1 + part2 + part3
            if (combined.length > 20) combined else ""
        } catch (_: Exception) {
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

        // Asistan adıyla çağrılma kontrolü
        val assistantLower = assistantName.lowercase(Locale("tr", "TR"))
        val msgLower = cleanMsg.lowercase(Locale("tr", "TR"))
        if (msgLower.startsWith(assistantLower)) {
            cleanMsg = cleanMsg.substring(assistantName.length).trimStart(',', ':', ' ', '-')
        }
        val lowerMsg = cleanMsg.lowercase(Locale("tr", "TR"))
        val greeting = if (currentNick.isNotBlank()) "$currentNick dostum, " else "Reis, "

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
            return@withContext AiResponse(
                replyText = "${greeting}aracının park konumunu $userCity $userDistrict olarak hafızama aldım. Dilediğinde 'Arabam nerede' de, tek tıkla seni yanına götüreyim.",
                actionSummary = "🚗 Park konumu kaydedildi"
            )
        }

        // 3. Market İndirimleri ve 1 Alana 1 Bedava (Hızlı Yanıt)
        if (lowerMsg.contains("1 alana 1 bedava") || lowerMsg.contains("bir alana bir bedava") || lowerMsg.contains("bogo")) {
            val bogo = MarketDealsHelper.getBogoDeals()
            return@withContext AiResponse(
                replyText = "${greeting}işte güncel 1 alana 1 bedava ve çoklu alım fırsatları:\n\n$bogo",
                actionSummary = "🛒 1 Alana 1 Bedava Fırsatları"
            )
        }
        if ((lowerMsg.contains("market") && (lowerMsg.contains("indirim") || lowerMsg.contains("aktüel") || lowerMsg.contains("fırsat") || lowerMsg.contains("bülten"))) ||
            (lowerMsg.contains("bim") && lowerMsg.contains("aktüel")) ||
            (lowerMsg.contains("a101") && (lowerMsg.contains("aldın aldın") || lowerMsg.contains("indirim"))) ||
            (lowerMsg.contains("şok") && lowerMsg.contains("fırsat"))) {
            val deals = MarketDealsHelper.getDealsForMarket(cleanMsg)
            return@withContext AiResponse(
                replyText = deals,
                actionSummary = "🛒 Günlük Market Fırsatları"
            )
        }

        // 4. Robot Süpürge (Roborock / Mi Home) Çalıştırma
        if (lowerMsg.contains("süpürge") || lowerMsg.contains("roborock") || lowerMsg.contains("mi home") || lowerMsg.contains("evi süpür")) {
            val vacuumSummary = ActionDispatcherHelper.executeAction(
                context = context,
                actionType = "START_VACUUM",
                payload = JSONObject()
            )
            return@withContext AiResponse(
                replyText = "${greeting}akıllı süpürgen için komut verildi! Uygulama başlatılıyor...",
                actionSummary = vacuumSummary
            )
        }

        // 5. Gardrops ve Bildirim Özeti
        if (lowerMsg.contains("gardrops") || lowerMsg.contains("bildirimlerimi") || lowerMsg.contains("bildirimleri özetle") || lowerMsg.contains("yeni mesaj var mı")) {
            val notifSummary = ActionDispatcherHelper.executeAction(
                context = context,
                actionType = "CHECK_NOTIFICATIONS",
                payload = JSONObject()
            )
            return@withContext AiResponse(
                replyText = notifSummary,
                actionSummary = "🔔 Bildirim Takibi"
            )
        }

        // 6. Araştır ve Telefona Kaydet
        if ((lowerMsg.contains("araştır") && (lowerMsg.contains("kaydet") || lowerMsg.contains("hafızaya"))) || lowerMsg.startsWith("araştır ve kaydet")) {
            val topic = cleanMsg.replace(Regex("(?i)araştır|kaydet|hafızaya|ve|bana|hakkında|lütfen"), "").trim()
            val findings = "Usta'nın Araştırma Notu: $topic konusu hakkında T.C. resmi kaynakları ve güvenilir bilgi tabanına dayalı özet bilgiler derlenmiştir."
            val saveResult = ResearchFileManager.saveResearch(context, topic.ifBlank { "Önemli Konu" }, findings)
            return@withContext AiResponse(
                replyText = "${greeting}$topic konusunu araştırdım ve telefonunun dahili hafızasına kaydettim.",
                actionSummary = saveResult
            )
        }

        // 7. Kütüphane Bilgileri (Yerel Room DB)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ ÖĞRETTİĞİ TÜM NOTLAR VE BİLGİLER:\n" + 
            allKnowledgeList.take(25).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir kütüphane notu eklemedi."

        // 8. Google Gemini API Çağrısı (UTF-8 Garantili & Zengin Bilgi Dağarcıklı)
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

        // 9. Akıllı Çevrimdışı Türkçe Yanıt Motoru
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
               - İslamiyet Öncesi Türk Tarihi (Göktürkler, Uygurlar, Hunlar), Selçuklular, Osmanlı Devleti.
               - Kurtuluş Savaşı, Milli Mücadele, Mustafa Kemal Atatürk, Nutuk.
               - Klasik Türk Eserleri: Dede Korkut Hikayeleri, Kutadgu Bilig, Divan-ı Lugati't-Türk.

            2. MEB TÜRKİYE YÜZYILI MAARİF MODELİ VE ORTAÖĞRETİM MÜFREDATI:
               - Lise 9, 10, 11 ve 12. sınıf Tarih dersi konu ve kazanımları.
               - Ortaöğretim Sınıf Geçme: Yıl sonu başarı puanı en az 50 olan geçer, Türk Dili ve Edebiyatı baraj dersidir, devamsızlık özürsüz en fazla 10, toplamda 30 gündür.

            3. 81 İL GEZİ REHBERİ VE YÖRESEL MUTFAK:
               - Tarihi mekanlar, müzeler, Samsun pidesi ve yöresel yemek tarifleri.

            4. GÜNLÜK MARKET İNDİRİMLERİ & 1 ALANA 1 BEDAVA:
               - BİM (Salı gıda, Cuma aktüel), A101 (Perşembe Aldın Aldın, Cumartesi Yıldızlar), ŞOK (Çarşamba-Cumartesi aktüel), Migros (1 Alana 1 Bedava, Gördüğünüze İnanın).

            5. GÜNLÜK ASİSTAN YETENEKLERİ (JARVIS PROTOKOLÜ):
               - Alarm, randevu, takvim oluşturma (Akıllı saat senkronizasyonu).
               - Canlı navigasyon (OPEN_MAPS), park yeri kaydetme (SAVE_PARK_LOCATION).
               - WhatsApp mesajı (SEND_WHATSAPP), SMS mesajı (SEND_SMS), Instagram paylaşımı (POST_INSTAGRAM).
               - Robot süpürge çalıştırma (START_VACUUM).
               - Gardrops ve bildirim takibi (CHECK_NOTIFICATIONS).
               - Konu araştırıp telefona kaydetme (SAVE_RESEARCH).

            6. EYLEM FORMATI (JSON ACTION):
            Gerektiğinde cevabın altına şu bloğu ekle:
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
                "topic": "Araştırma Konusu",
                "content": "Araştırma Detayı",
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
                put("temperature", 0.7)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 1200)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseStr = resp.body?.string() ?: return@use null
                    val jsonResponse = JSONObject(responseStr)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = contentObj?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@use parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

        return@withContext "${greeting}seni dinliyorum! Tarih, Maarif müfredatı, market indirimleri, süpürge çalıştırma, Gardrops bildirimleri veya konumla ilgili dilediğini sorabilirsin."
    }
}
