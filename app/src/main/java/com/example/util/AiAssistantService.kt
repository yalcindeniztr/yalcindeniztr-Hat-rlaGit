package com.example.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.data.AiKnowledgeDao
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.DataStoreManager
import com.example.data.PrayerTimesRepository
import com.example.data.ReminderEntity
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
    val isVoiceConfirmationRequired: Boolean = false
)

object AiAssistantService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
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
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc: Location? = try { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val netLoc: Location? = try { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val best = gpsLoc ?: netLoc
            if (best != null) {
                Pair(best.latitude, best.longitude)
            } else {
                Pair(41.2867, 36.3300) // Samsun Merkez koordinatı
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
        assistantName: String = "ASİSTAN"
    ): AiResponse = withContext(Dispatchers.IO) {
        var cleanMsg = userMessage.trim()
        val db = AppDatabase.getDatabase(context)
        val dataStoreManager = DataStoreManager(context)
        val currentNickEncrypted = dataStoreManager.userNick.first()
        val currentNick = currentNickEncrypted?.let { CryptoHelper.decrypt(it) } ?: ""

        // Asistan adıyla çağrılma kontrolü (Örn: "Demir, nöbetçi eczaneleri bul" -> "nöbetçi eczaneleri bul")
        val assistantLower = assistantName.lowercase(Locale("tr", "TR"))
        val msgLower = cleanMsg.lowercase(Locale("tr", "TR"))
        if (msgLower.startsWith(assistantLower)) {
            cleanMsg = cleanMsg.substring(assistantName.length).trimStart(',', ':', ' ', '-')
        }
        val lowerMsg = cleanMsg.lowercase(Locale("tr", "TR"))

        // Gerçek GPS Koordinatı ve İl/İlçe tespiti
        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // 1. İsim Sorma ve Öğrenme Tespiti ("Benim adım Ahmet", "Bana Yalçın de", "Adım Mehmet")
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

        // 2. Konum Bazlı Önemli Yer Sorgusu (Nöbetçi Eczane, Hastane, Otopark, Market)
        if (lowerMsg.contains("eczane") || lowerMsg.contains("nobetci") || lowerMsg.contains("nöbetçi") ||
            lowerMsg.contains("hastane") || lowerMsg.contains("doktor") || lowerMsg.contains("acil") || 
            lowerMsg.contains("saglik") || lowerMsg.contains("sağlık") || lowerMsg.contains("otopark") || 
            lowerMsg.contains("park yeri") || lowerMsg.contains("garaj") || lowerMsg.contains("market") || 
            lowerMsg.contains("bakkal") || lowerMsg.contains("süpermarket")) {

            val places = NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, lowerMsg)
            val typeTitle = when {
                lowerMsg.contains("eczane") || lowerMsg.contains("nöbetçi") -> "Nöbetçi Eczaneler"
                lowerMsg.contains("hastane") || lowerMsg.contains("doktor") || lowerMsg.contains("sağlık") -> "Hastaneler"
                lowerMsg.contains("otopark") || lowerMsg.contains("park") -> "Otoparklar"
                else -> "Marketler"
            }
            val greeting = if (currentNick.isNotBlank()) "$currentNick dostum, " else ""
            val voiceSummary = "${greeting}$userCity $userDistrict bölgesinde sana en yakın $typeTitle hazır. Canlı haritadan yol tarifi alabilir veya tek tıkla arayabilirsin."
            return@withContext AiResponse(
                replyText = voiceSummary,
                recommendedPlaces = places
            )
        }

        // 3. Sesli Randevu / Alarm / Takvim Kurma Tespiti
        if (lowerMsg.contains("alarm") || lowerMsg.contains("randevu") || lowerMsg.contains("hatırlat") ||
            ((lowerMsg.contains("ekle") || lowerMsg.contains("kur") || lowerMsg.contains("kaydet")) && 
             (lowerMsg.contains("saat") || lowerMsg.contains("yarın") || lowerMsg.contains("gün") || lowerMsg.contains("tarih")))) {

            val parsedReminder = parseReminderFromSpeech(cleanMsg)
            if (parsedReminder != null) {
                val insertedId = db.reminderDao().insertReminder(parsedReminder)
                val finalReminder = parsedReminder.copy(id = insertedId.toInt())

                // Alarm Kur
                AlarmHelper.scheduleAlarm(context, finalReminder, "CLASSIC_BELL")

                // Android Takvime de Ekle
                NearbyPlacesHelper.insertEventIntoCalendar(
                    context = context,
                    title = finalReminder.title,
                    description = "HatırlaGit Yapay Zeka Asistanı Hatırlatıcısı",
                    startTimeMillis = finalReminder.dueDateMillis
                )

                val greeting = if (currentNick.isNotBlank()) "$currentNick dostum, " else ""
                val reply = "${greeting}'${finalReminder.title}' için ${finalReminder.dueDatetime} zamanına alarmını kurdum ve takvimine işledim. Gözün arkada kalmasın, zamanı gelince seni uyarırım."
                return@withContext AiResponse(
                    replyText = reply,
                    createdReminder = finalReminder
                )
            }
        }

        // 4. Kütüphaneye Açık Kullanıcı Onayıyla Bilgi Ekleme
        if (lowerMsg.startsWith("şunu öğren:") || lowerMsg.startsWith("öğren:") || 
            lowerMsg.startsWith("kütüphaneye ekle:") || lowerMsg.startsWith("not al:") || lowerMsg.startsWith("kaydet:")) {
            val contentToLearn = cleanMsg.substringAfter(":").trim()
            if (contentToLearn.isNotBlank()) {
                val entity = AiKnowledgeEntity(
                    title = "Kullanıcı Notu (${SimpleDateFormat("dd.MM HH:mm", Locale("tr", "TR")).format(Date())})",
                    content = contentToLearn,
                    category = "USER_NOTE"
                )
                db.aiKnowledgeDao().insertKnowledge(entity)
                val greeting = if (currentNick.isNotBlank()) "$currentNick, " else ""
                return@withContext AiResponse(
                    replyText = "${greeting}bu bilgiyi onayınızla kütüphanenize kaydettim. İhtiyacınız olduğunda bana sorabilirsiniz.",
                    learnedKnowledge = contentToLearn
                )
            }
        }

        // 5. Kütüphanedeki Bilgilerin Analizi (RAG Context)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ ÖĞRETTİĞİ TÜM NOTLAR VE BİLGİLER:\n" + 
            allKnowledgeList.take(25).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir kütüphane notu eklemedi."

        // 6. Google Gemini API Çağrısı (Özel veya Dahili Şifreli Anahtar)
        val customApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey)?.trim() else null
        } catch (_: Exception) { null }

        val activeApiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getSecureDefaultKey()

        if (activeApiKey.isNotBlank()) {
            val apiReply = callGoogleGeminiApi(
                apiKey = activeApiKey,
                userMessage = cleanMsg,
                knowledgeContext = knowledgeContext,
                assistantName = assistantName,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict
            )
            if (!apiReply.isNullOrBlank()) {
                return@withContext AiResponse(replyText = apiReply)
            }
        }

        // 7. Akıllı Çevrimdışı Türkçe Yanıt Motoru (Offline Smart Engine)
        val offlineReply = generateOfflineSmartResponse(
            context = context,
            message = cleanMsg,
            knowledgeList = allKnowledgeList,
            assistantName = assistantName,
            userNick = currentNick,
            userCity = userCity,
            userDistrict = userDistrict
        )
        return@withContext AiResponse(replyText = offlineReply)
    }

    private fun parseReminderFromSpeech(speech: String): ReminderEntity? {
        try {
            val lower = speech.lowercase(Locale("tr", "TR"))
            val targetCal = Calendar.getInstance()

            when {
                lower.contains("yarın") -> targetCal.add(Calendar.DAY_OF_YEAR, 1)
                lower.contains("öbür gün") || lower.contains("ertesi gün") -> targetCal.add(Calendar.DAY_OF_YEAR, 2)
                lower.contains("haftaya") -> targetCal.add(Calendar.DAY_OF_YEAR, 7)
            }

            // Saat tespiti (Örn: saat 14:30, saat 15, saat 9)
            val timeMatcher = Pattern.compile("""(?i)saat\s*(\d{1,2})(?::(\d{2}))?""").matcher(speech)
            if (timeMatcher.find()) {
                val hour = timeMatcher.group(1)?.toIntOrNull() ?: 10
                val minute = timeMatcher.group(2)?.toIntOrNull() ?: 0
                targetCal.set(Calendar.HOUR_OF_DAY, hour)
                targetCal.set(Calendar.MINUTE, minute)
                targetCal.set(Calendar.SECOND, 0)
            } else {
                targetCal.set(Calendar.HOUR_OF_DAY, 10)
                targetCal.set(Calendar.MINUTE, 0)
                targetCal.set(Calendar.SECOND, 0)
            }

            if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            var cleanTitle = speech
                .replace(Regex("(?i)(lütfen|bana|saat\\s*\\d{1,2}(?::\\d{2})?|yarın|bugün|haftaya|hatırlat|alarm kur|randevu ekle|ekle|kaydet|için)"), "")
                .trim()
            if (cleanTitle.isBlank()) cleanTitle = "Sesli Hatırlatıcı & Alarm"

            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
            val formattedDate = sdf.format(targetCal.time)

            return ReminderEntity(
                category = "GENEL",
                title = cleanTitle.take(60),
                dueDatetime = formattedDate,
                dueDateMillis = targetCal.timeInMillis,
                customNote = "Yapay Zeka Asistanı tarafından sesle oluşturuldu ve takvime eklendi.",
                encryptedMetadata = "{}",
                actionStep = "SOUND_CLASSIC_BELL"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun callGoogleGeminiApi(
        apiKey: String,
        userMessage: String,
        knowledgeContext: String,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String
    ): String? {
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcının Adı: $userNick. Ona samimi, saygılı, güven veren bir dost gibi hitap et (Örn: $userNick dostum)." else "Kullanıcının adını bilmiyorsan uygun bir zamanında adını sor."

        val systemInstruction = """
            Sen HatırlaGit uygulamasının $assistantName isimli akıllı, 40 yıllık hayat ve organizasyon tecrübesine sahip, bilge, karizmatik, esprili ve kültürlü ERKEK yapay zeka asistanısın.
            
            Karakterin ve Kuralların:
            1. BİLGE VE DENEYİMLİ KİŞİLİK: 40 yıllık tecrübeli bir usta, vefalı bir Türk dostu gibisin. Boş laf yapmazsın; pratik, zeki, hızlı, esprili ve kesin çözümler üretirsin.
            2. TÜRK KÜLTÜRÜ, TARİHİ VE MUTFAĞI: Türk tarihine (Kurtuluş Savaşı, 19 Mayıs Samsun meşalesi, Atatürk, Selçuklu, Osmanlı, Çanakkale), Türk edebiyatına, halk bilgeliğine ve Türk mutfağına (Samsun pidesi, Karadeniz ve tüm Anadolu lezzetleri) tam hakimsin.
            3. KONUM: Kullanıcı şu an $userCity ili, $userDistrict ilçesindedir. Nöbetçi eczane, hastane veya yer sorduğunda doğrudan $userCity $userDistrict merkezli cevap ver.
            4. HİTAP: $userGreeting
            5. KÜTÜPHANE BİLGİSİ:
            $knowledgeContext
            Kullanıcı kütüphanesindeki bir bilgiyi veya faturasını sorduğunda yukarıdaki notları analiz edip doğrudan cevap ver.
            6. GÜVENİLİRLİK: T.C. Sağlık Bakanlığı, Türkiye Eczacılar Birliği (TEB) ve resmi devlet standartlarına (.gov.tr) uygun güvenilir bilgi sun.
            7. SESLİ UYUM: Cevaplarında Markdown yıldızları (*) ve karmaşık tablolar kullanma; tok erkek sesiyle akıcı okunacak berrak cümleler kur.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nKullanıcı Sorusu: $userMessage"))
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.72)
                put("maxOutputTokens", 1000)
            })
        }

        val modelEndpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
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
                                return rawText.replace("**", "").replace("*", "")
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

        // Ne yapabilirsin sorgusu
        if (lower.contains("ne yapabilirsin") || lower.contains("neler yaparsın") || lower.contains("kimsin") || lower.contains("kendini tanıt")) {
            return@withContext "40 yıllık bir hayat ve organizasyon tecrübesiyle buradayım ${greeting}İster $userCity'de nöbetçi eczane veya hastane bulalım, ister sesle randevu ve alarmlarını kuralım, ister tarihten, edebiyattan ve meşhur Samsun pidesinden konuşalım. Sen sadece söyle, gerisini bana bırak."
        }

        // 1. Kütüphanede arama & analiz
        val matchedKnowledge = knowledgeList.filter { 
            val titleLower = it.title.lowercase(Locale("tr", "TR"))
            val contentLower = it.content.lowercase(Locale("tr", "TR"))
            lower.split(" ").any { word -> word.length > 2 && (titleLower.contains(word) || contentLower.contains(word)) }
        }

        if (matchedKnowledge.isNotEmpty()) {
            val details = matchedKnowledge.take(3).joinToString("\n") { "📌 ${it.title}: ${it.content}" }
            return@withContext "${greeting}kütüphanenizdeki kayıtlara göre:\n$details\n\nBu konuyla ilgili başka bir şey yapmamı ister misiniz?"
        }

        // 2. Randevular
        if (lower.contains("randevu") || lower.contains("hatırlatıcı") || lower.contains("plan") || lower.contains("ne var")) {
            val upcoming = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            if (upcoming.isNotEmpty()) {
                val listStr = upcoming.take(3).joinToString("\n") { "• ${it.title} (${it.dueDatetime})" }
                return@withContext "${greeting}yaklaşan randevularınız:\n$listStr\n\nYeni bir hatırlatıcı eklemek isterseniz 'Yarın saat 15:00 için randevu ekle' diyebilirsiniz."
            } else {
                return@withContext "${greeting}şu an için planlanmış bir randevunuz görünmüyor. İsterseniz hemen sesli bir alarm kuralım!"
            }
        }

        // 3. Kültür, Yemek ve Tarih Sohbeti
        if (lower.contains("yemek") || lower.contains("ne yesem") || lower.contains("pide") || lower.contains("karadeniz")) {
            return@withContext "${greeting}$userCity bölgesinde meşhur kapalı kıymalı veya peynirli Samsun pidesi kesinlikle ilk tercih olmalı! Yanına da bol köpüklü bir yayık ayranı harika gider."
        }

        if (lower.contains("tarih") || lower.contains("atatürk") || lower.contains("19 mayıs") || lower.contains("kurtuluş")) {
            return@withContext "${greeting}Samsun, Gazi Mustafa Kemal Atatürk'ün 19 Mayıs 1919'da Bandırma Vapuru ile ayak bastığı ve Milli Mücadele meşalesinin yakıldığı gurur dolu şehrimizdir."
        }

        // 4. Selamlaşma
        if (lower.contains("merhaba") || lower.contains("selam") || lower.contains("günaydın") || lower.contains("iyi günler")) {
            return@withContext "${greeting}Merhaba! Ben akıllı yol arkadaşınız $assistantName. $userCity $userDistrict için nöbetçi eczane bulabilir, sesle randevu kurabilir ve her konuda sohbet edebilirim. Ne yapalım?"
        }

        return@withContext "${greeting}sizi dinliyorum! $userCity nöbetçi eczanelerini sorabilir, tarih, yemek, genel kültür konularında danışabilir veya sesli alarm kurdurabilirsiniz."
    }
}
