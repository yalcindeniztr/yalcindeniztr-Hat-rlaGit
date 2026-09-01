package com.example.util

import android.content.Context
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

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 41.0082,
        userLng: Double = 28.9784,
        assistantName: String = "ASİSTAN"
    ): AiResponse = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()
        val lowerMsg = cleanMsg.lowercase(Locale("tr", "TR"))
        val db = AppDatabase.getDatabase(context)
        val dataStoreManager = DataStoreManager(context)
        val currentNickEncrypted = dataStoreManager.userNick.first()
        val currentNick = currentNickEncrypted?.let { CryptoHelper.decrypt(it) } ?: ""

        // 1. İsim Sorma ve Öğrenme Tespiti ("Benim adım Ahmet", "Bana Yalçın Bey de", "Adım Mehmet")
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
                    db.aiKnowledgeDao().insertKnowledge(
                        AiKnowledgeEntity(
                            title = "Kullanıcı Adı",
                            content = "Kullanıcının adı: $detectedName. Ona her zaman bu isimle hitap et.",
                            category = "SYSTEM_PREF"
                        )
                    )
                    return@withContext AiResponse(
                        replyText = "Tanıştığıma çok memnun oldum $detectedName! İsminizi hafızama kaydettim, bundan sonra size $detectedName olarak hitap edeceğim. Size bugün nasıl yardımcı olabilirim?"
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

            val places = NearbyPlacesHelper.getRecommendedPlaces(userLat, userLng, lowerMsg)
            val typeTitle = when {
                lowerMsg.contains("eczane") || lowerMsg.contains("nöbetçi") -> "Nöbetçi Eczaneler"
                lowerMsg.contains("hastane") || lowerMsg.contains("doktor") || lowerMsg.contains("sağlık") -> "Hastaneler & Sağlık Merkezleri"
                lowerMsg.contains("otopark") || lowerMsg.contains("park") -> "Otopark Alanları"
                else -> "Marketler & Alışveriş Yerleri"
            }
            val greeting = if (currentNick.isNotBlank()) "$currentNick, " else ""
            val voiceSummary = "${greeting}Konumunuza en yakın 3 $typeTitle bulundu. Haritadan yol tarifi almak veya doğrudan aramak için kartlardaki butonları kullanabilirsiniz."
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

                // Kütüphaneye de kaydet
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = finalReminder.title,
                        content = "Planlanan randevu/alarm: ${finalReminder.title}, Zaman: ${finalReminder.dueDatetime}",
                        category = "CHAT_MEMORY"
                    )
                )

                val greeting = if (currentNick.isNotBlank()) "$currentNick, " else ""
                val reply = "${greeting}'${finalReminder.title}' için ${finalReminder.dueDatetime} zamanına alarmınız kuruldu ve telefon takviminize kaydedildi."
                return@withContext AiResponse(
                    replyText = reply,
                    createdReminder = finalReminder
                )
            }
        }

        // 4. Kütüphaneye Manuel Bilgi Ekleme ("Şunu öğren:", "Kütüphaneye ekle:", "Not al:")
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
                    replyText = "${greeting}bu bilgiyi hafızama ve kütüphaneme kaydettim. İhtiyacınız olduğunda bana sorabilirsiniz.",
                    learnedKnowledge = contentToLearn
                )
            }
        }

        // 5. Kütüphanedeki TÜM Bilgilerin Analizi (RAG Context)
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "KULLANICININ ÖĞRETTİĞİ TÜM NOTLAR VE BİLGİLER:\n" + 
            allKnowledgeList.take(25).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir not eklemedi."

        // 6. Google Gemini API Çağrısı (Özel veya Dahili Şifrelenmiş Anahtar ile)
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
                userNick = currentNick
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
            userNick = currentNick
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

    private fun callGoogleGeminiApi(
        apiKey: String,
        userMessage: String,
        knowledgeContext: String,
        assistantName: String,
        userNick: String
    ): String? {
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcının Adı: $userNick. Ona her zaman bu isimle hitap et." else "Kullanıcının adını bilmiyorsan uygun bir zamanda adını sor."

        val systemInstruction = """
            Sen HatırlaGit kişisel asistan uygulamasının $assistantName isimli akıllı, yardımsever, güvenilir ve samimi yapay zeka asistanısın.
            Temel Kuralların:
            1. DİL: Sadece akıcı, doğal, samimi ve saygılı TÜRKÇE konuş.
            2. KİMLİK & HİTAP: $userGreeting
            3. KÜTÜPHANE VE HAFIZA BİLGİSİ:
            $knowledgeContext
            Kullanıcı kütüphanesindeki bir notu, faturayı, tarihi veya bilgiyi sorduğunda doğrudan yukarıdaki kütüphane kayıtlarını analiz ederek detaylı ve net cevap ver.
            4. RESMİ & YASAL KONULAR: Türkiye Cumhuriyeti yasal mevzuatına, kamu kurumlarına ve resmi devlet sitelerine (.gov.tr) uygun güvenilir bilgi ver.
            5. SESLİ ASİSTAN UYUMU: Cevaplarında gereksiz Markdown yıldızları (*) kullanma, doğrudan akıcı ve okunabilir cümleler kur.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nKullanıcı: $userMessage"))
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1000)
            })
        }

        // Endpoint listesi: Önce 1.5 Flash, olmazsa 2.0 Flash
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
        userNick: String
    ): String = withContext(Dispatchers.IO) {
        val lower = message.lowercase(Locale("tr", "TR"))
        val greeting = if (userNick.isNotBlank()) "$userNick, " else ""
        val db = AppDatabase.getDatabase(context)

        // 1. Kütüphanede arama & analiz
        val matchedKnowledge = knowledgeList.filter { 
            val titleLower = it.title.lowercase(Locale("tr", "TR"))
            val contentLower = it.content.lowercase(Locale("tr", "TR"))
            lower.split(" ").any { word -> word.length > 2 && (titleLower.contains(word) || contentLower.contains(word)) }
        }

        if (matchedKnowledge.isNotEmpty()) {
            val details = matchedKnowledge.take(4).joinToString("\n") { "📌 ${it.title}: ${it.content}" }
            return@withContext "${greeting}kütüphanenizdeki kayıtlara göre:\n$details\n\nBu bilgiyle ilgili başka bir işlem yapmamı ister misiniz?"
        }

        // 2. Randevularımı soruyorsa
        if (lower.contains("randevu") || lower.contains("hatırlatıcı") || lower.contains("plan") || lower.contains("ne var") || lower.contains("neler var")) {
            val upcoming = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            if (upcoming.isNotEmpty()) {
                val listStr = upcoming.take(3).joinToString("\n") { "• ${it.title} (${it.dueDatetime})" }
                return@withContext "${greeting}yaklaşan randevularınız şunlardır:\n$listStr\n\nYeni bir randevu veya alarm eklemek isterseniz 'Yarın saat 14:00 için randevu ekle' diyebilirsiniz."
            } else {
                return@withContext "${greeting}yakın zamanda planlanmış bir randevunuz bulunmuyor. İsterseniz hemen sesli bir hatırlatıcı kuralım!"
            }
        }

        // 3. Ezan Vakitleri soruyorsa
        if (lower.contains("ezan") || lower.contains("namaz") || lower.contains("vakit")) {
            return@withContext "${greeting}günün ezan vakitlerini ve sıradaki vakti Ana Sayfa'daki Ezan Vakti kartından veya Ezan Vakitleri sekmesinden takip edebilirsiniz."
        }

        // 4. Arabam nerede soruyorsa
        if (lower.contains("arabam") || lower.contains("araba") || lower.contains("park")) {
            val lat = DataStoreManager(context).parkedCarLat.first()
            if (!lat.isNullOrBlank()) {
                return@withContext "${greeting}aracınızın son park konumu başarıyla kayıtlıdır. Ana Sayfa'daki 'Arabam Nerede?' kartından tek tıkla yol tarifi alabilirsiniz."
            } else {
                return@withContext "${greeting}henüz bir araç park konumu kaydetmediniz. Ana sayfadan 'Park Konumumu Kaydet' butonuna basarak arabanızı kaydedebilirsiniz."
            }
        }

        // 5. Selamlaşma & Hal Hatır
        if (lower.contains("merhaba") || lower.contains("selam") || lower.contains("günaydın") || lower.contains("iyi günler")) {
            return@withContext "${greeting}Merhaba! Ben HatırlaGit Yapay Zeka Asistanınız $assistantName. Size nöbetçi eczane bulabilir, randevu/alarm kurabilir ve kütüphanenizdeki notları analiz edebilirim. Size bugün nasıl yardımcı olabilirim?"
        }

        if (lower.contains("nasılsın") || lower.contains("ne haber") || lower.contains("naber")) {
            return@withContext "Harikayım, teşekkür ederim! Sizin gününüzü kolaylaştırmak ve hatırlatmalarınızı organize etmek için hazırım. Ne sormak istersiniz?"
        }

        if (lower.contains("kimsin") || lower.contains("nesin") || lower.contains("ne yapabilirsin")) {
            return@withContext "Ben HatırlaGit'in akıllı kişisel asistanıyım. Telefonunuzda şifreli çalışır, nöbetçi eczane/hastane bulur, sesle alarm kurar ve kütüphanenizdeki tüm bilgileri analiz ederim."
        }

        // 6. Genel sorular için rehberlik
        return@withContext "${greeting}bu sorunuza doğrudan yapay zeka ile geniş kapsamlı yanıt verebilmem için Profil > Asistan & API Ayarları ekranından ücretsiz Google Gemini API anahtarınızı ekleyebilirsiniz. Şu anda cihazınızdaki tüm randevuları, kütüphane notlarını, nöbetçi eczaneleri ve sesli alarmları yönetebilirim. Ne yapmak istersiniz?"
    }
}
