package com.example.util

import android.content.Context
import com.example.data.AiKnowledgeDao
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.DataStoreManager
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
            .connectTimeout(15, TimeUnit.SECONDS)
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
            val voiceSummary = "${greeting}Konumunuza en yakın 3 $typeTitle bulundu. Haritadan yol tarifi almak veya aramak için kartlardaki butonları kullanabilirsiniz."
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
            allKnowledgeList.take(20).joinToString("\n") { item -> "- [${item.category}] ${item.title}: ${item.content}" }
        } else "Kullanıcı henüz özel bir not eklemedi."

        // 6. Google Gemini API Çağrısı (Eğer kullanıcı API anahtarı girmişse)
        val plainApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey) else null
        } catch (_: Exception) { null }

        if (!plainApiKey.isNullOrBlank()) {
            val apiReply = callGoogleGeminiApi(
                apiKey = plainApiKey,
                userMessage = cleanMsg,
                knowledgeContext = knowledgeContext,
                assistantName = assistantName,
                userNick = currentNick
            )
            if (apiReply != null) {
                return@withContext AiResponse(replyText = apiReply)
            }
        }

        // 7. Akıllı Çevrimdışı Türkçe Yanıt Motoru (Offline Smart Engine)
        val offlineReply = generateOfflineSmartResponse(
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

    private fun callGoogleGeminiApi(
        apiKey: String,
        userMessage: String,
        knowledgeContext: String,
        assistantName: String,
        userNick: String
    ): String? {
        return try {
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
                5. KISA VE ÖZ: Sesli asistan olduğun için gereksiz uzatmadan, anlaşılır ve yapıcı cevaplar üret.
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
                    put("temperature", 0.7)
                    put("maxOutputTokens", 800)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: return null
                val rootJson = JSONObject(respBody)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text").trim()
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateOfflineSmartResponse(
        message: String,
        knowledgeList: List<AiKnowledgeEntity>,
        assistantName: String,
        userNick: String
    ): String {
        val lower = message.lowercase(Locale("tr", "TR"))
        val greeting = if (userNick.isNotBlank()) "$userNick, " else ""

        // Kütüphanede arama
        val matchedKnowledge = knowledgeList.filter { 
            val titleLower = it.title.lowercase(Locale("tr", "TR"))
            val contentLower = it.content.lowercase(Locale("tr", "TR"))
            lower.split(" ").any { word -> word.length > 2 && (titleLower.contains(word) || contentLower.contains(word)) }
        }

        if (matchedKnowledge.isNotEmpty()) {
            val details = matchedKnowledge.take(3).joinToString("\n") { "📌 ${it.title}: ${it.content}" }
            return "${greeting}hafızamdaki bilgilere göre:\n$details\n\nBaşka bir konuda yardımcı olmamı ister misiniz?"
        }

        return when {
            lower.contains("merhaba") || lower.contains("selam") || lower.contains("günaydın") || lower.contains("iyi günler") -> {
                "${greeting}Merhaba! Ben HatırlaGit akıllı asistanınız $assistantName. Size nöbetçi eczane, hastane, otopark bulabilir, sesle randevu/alarm kurabilir ve kütüphanenizdeki notları hatırlatabilirim. Size nasıl yardımcı olabilirim?"
            }
            lower.contains("nasılsın") || lower.contains("ne haber") || lower.contains("naber") -> {
                "Harikayım, teşekkür ederim! Sizin gününüzü kolaylaştırmak ve hatırlatmalarınızı organize etmek için hazırım. Bugün ne yapmak istersiniz?"
            }
            lower.contains("tavsiye") || lower.contains("öneri") || lower.contains("ne yapmalı") -> {
                "Gününüzü daha verimli geçirmek için yaklaşan randevularınızı kontrol edebilir, düzenli su içmeyi ve dinlenme molalarını ihmal etmemeyi hatırlatabilirim. İsterseniz hemen sesli bir hatırlatıcı kuralım!"
            }
            lower.contains("kimsin") || lower.contains("nesin") || lower.contains("ne yapabilirsin") -> {
                "Ben HatırlaGit'in kişisel yaşam asistanıyım. Bilgilerinizi sadece telefonunuzda güvenle saklar, konumunuza göre nöbetçi eczane/hastane bulur ve sesli alarmlar kurarım."
            }
            else -> {
                "${greeting}sizi dinliyorum! Bana sesli olarak alarm kurdurabilir, 'En yakın nöbetçi eczaneyi bul' diyebilir veya 'Şunu öğren:' diyerek hafızama yeni bilgiler kaydedebilirsiniz."
            }
        }
    }
}
