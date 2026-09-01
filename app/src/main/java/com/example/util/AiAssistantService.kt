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
    val isSpeechReady: Boolean = true
)

object AiAssistantService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 41.0082, // Default / current user location
        userLng: Double = 28.9784,
        assistantName: String = "ASİSTAN"
    ): AiResponse = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()
        val lowerMsg = cleanMsg.lowercase(Locale("tr"))
        val db = AppDatabase.getDatabase(context)
        val dataStoreManager = DataStoreManager(context)

        // 1. İsim Öğrenme Kontrolü ("Benim adım Ahmet", "Bana Yalçın Bey de")
        val namePatterns = listOf(
            Pattern.compile("""(?i)(?:benim adım|bana)\s+([A-Za-zÇĞİÖŞÜçğıöşü]+(?:\s+[A-Za-zÇĞİÖŞÜçğıöşü]+)?)(?:\s+de|\s+diyebilirsin)?"""),
            Pattern.compile("""(?i)adım\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)""")
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
                        replyText = "Memnun oldum $detectedName! İsminizi hafızama kaydettim, bundan sonra size $detectedName olarak hitap edeceğim. Size bugün nasıl yardımcı olabilirim?"
                    )
                }
            }
        }

        // 2. Önemli Yer / Konum Sorgusu (Nöbetçi Eczane, Hastane, Otopark, Market)
        if (lowerMsg.contains("eczane") || lowerMsg.contains("nöbetçi") || lowerMsg.contains("hastane") || 
            lowerMsg.contains("doktor") || lowerMsg.contains("otopark") || lowerMsg.contains("park yeri") || 
            lowerMsg.contains("market") || lowerMsg.contains("bakkal")) {
            
            val places = NearbyPlacesHelper.getRecommendedPlaces(userLat, userLng, lowerMsg)
            val typeTitle = when {
                lowerMsg.contains("eczane") || lowerMsg.contains("nöbetçi") -> "Nöbetçi Eczaneler"
                lowerMsg.contains("hastane") || lowerMsg.contains("doktor") -> "Hastaneler & Sağlık Merkezleri"
                lowerMsg.contains("otopark") || lowerMsg.contains("park") -> "Otoparklar"
                else -> "Marketler"
            }
            val voiceSummary = "Konumunuza en yakın 3 $typeTitle bulundu. Haritadan yol tarifi almak veya aramak için aşağıdaki butonları kullanabilirsiniz."
            return@withContext AiResponse(
                replyText = voiceSummary,
                recommendedPlaces = places
            )
        }

        // 3. Sesli Randevu / Alarm Kurma Tespiti ("Yarın saat 14'te toplantı", "5 Eylül'de kasko yenile")
        if (lowerMsg.contains("hatırlat") || lowerMsg.contains("alarm kur") || lowerMsg.contains("randevu") || 
            lowerMsg.contains("ekle") || lowerMsg.contains("kaydet") && (lowerMsg.contains("saat") || lowerMsg.contains("yarın") || lowerMsg.contains("gün"))) {
            
            val parsedReminder = parseReminderFromSpeech(cleanMsg)
            if (parsedReminder != null) {
                val insertedId = db.reminderDao().insertReminder(parsedReminder)
                val finalReminder = parsedReminder.copy(id = insertedId.toInt())
                
                // Alarm Planla
                AlarmHelper.scheduleAlarm(context, finalReminder, "CLASSIC_BELL")
                
                // Kütüphaneye de kaydet
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = finalReminder.title,
                        content = "Planlanan randevu: ${finalReminder.title} Tarih: ${finalReminder.dueDatetime}",
                        category = "CHAT_MEMORY"
                    )
                )

                val reply = "Anlaşıldı! '${finalReminder.title}' randevunuz ${finalReminder.dueDatetime} için kaydedildi ve tam zamanlı alarmı kuruldu."
                return@withContext AiResponse(
                    replyText = reply,
                    createdReminder = finalReminder
                )
            }
        }

        // 4. Kütüphaneye Manuel Bilgi Ekleme ("Şunu öğren:", "Kütüphaneye ekle:", "Not al:")
        if (lowerMsg.startsWith("şunu öğren:") || lowerMsg.startsWith("öğren:") || lowerMsg.startsWith("kütüphaneye ekle:") || lowerMsg.startsWith("not al:")) {
            val contentToLearn = cleanMsg.substringAfter(":").trim()
            if (contentToLearn.isNotBlank()) {
                val entity = AiKnowledgeEntity(
                    title = "Kullanıcı Notu (${SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())})",
                    content = contentToLearn,
                    category = "USER_NOTE"
                )
                db.aiKnowledgeDao().insertKnowledge(entity)
                return@withContext AiResponse(
                    replyText = "Harika! Bu bilgiyi kütüphaneme ekledim ve hafızama kaydettim. İhtiyacınız olduğunda bana sorabilirsiniz.",
                    learnedKnowledge = contentToLearn
                )
            }
        }

        // 5. Yerel Kütüphane (RAG) Taraması
        val matchingKnowledge = db.aiKnowledgeDao().searchKnowledge(cleanMsg)
        val knowledgeContext = if (matchingKnowledge.isNotEmpty()) {
            "Hafızamdaki ilgili kayıtlar:\n" + matchingKnowledge.take(3).joinToString("\n") { "- ${it.title}: ${it.content}" }
        } else ""

        // 6. Harici AI API Çağrısı (Eğer kullanıcı API anahtarı girmişse)
        val plainApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey) else null
        } catch (_: Exception) { null }

        if (!plainApiKey.isNullOrBlank()) {
            val apiReply = callExternalAiApi(plainApiKey, cleanMsg, knowledgeContext, assistantName)
            if (apiReply != null) {
                return@withContext AiResponse(replyText = apiReply)
            }
        }

        // 7. Akıllı Çevrimdışı / Yerel Yanıt Motoru (Offline Knowledge & Friendly Fallback)
        val offlineReply = generateOfflineSmartResponse(cleanMsg, knowledgeContext, assistantName)
        return@withContext AiResponse(replyText = offlineReply)
    }

    private fun parseReminderFromSpeech(speech: String): ReminderEntity? {
        try {
            val lower = speech.lowercase(Locale("tr"))
            var targetCal = Calendar.getInstance()
            
            // Tarih Ayrıştırma
            when {
                lower.contains("yarın") -> targetCal.add(Calendar.DAY_OF_YEAR, 1)
                lower.contains("öbür gün") || lower.contains("ertesi gün") -> targetCal.add(Calendar.DAY_OF_YEAR, 2)
                lower.contains("haftaya") -> targetCal.add(Calendar.DAY_OF_YEAR, 7)
            }

            // Saat Ayrıştırma (Örn: saat 14:30, saat 15, saat 9)
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
            }

            // Eğer geçmiş zamana denk geliyorsa 1 gün sonraya al
            if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Başlık Temizleme
            var cleanTitle = speech
                .replace(Regex("(?i)(lütfen|bana|saat\\s*\\d{1,2}(?::\\d{2})?|yarın|bugün|haftaya|hatırlat|alarm kur|ekle|kaydet)"), "")
                .trim()
            if (cleanTitle.isBlank()) cleanTitle = "Sesli Hatırlatıcı"

            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = sdf.format(targetCal.time)

            return ReminderEntity(
                category = "GENEL",
                title = cleanTitle.take(60),
                dueDatetime = formattedDate,
                dueDateMillis = targetCal.timeInMillis,
                customNote = "Yapay Zeka Asistanı tarafından sesle oluşturuldu.",
                encryptedMetadata = "{}",
                actionStep = "SOUND_CLASSIC_BELL"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun callExternalAiApi(apiKey: String, message: String, knowledgeContext: String, assistantName: String): String? {
        return try {
            val systemPrompt = """
                Sen HatırlaGit uygulamasının $assistantName isimli akıllı, yardımsever, güvenilir ve samimi kişisel yaşam asistanısın.
                Temel Kuralların:
                1. Türkçe, akıcı, nazik ve samimi konuş.
                2. Resmi işlemler, kamu destekleri, randevular ve yasal konularda Türkiye Cumhuriyeti mevzuatına ve resmi devlet sitelerine (.gov.tr) uygun doğru bilgi ver.
                3. Kullanıcının hafızasındaki bilgileri dikkate al: $knowledgeContext
                4. Cevapların net, anlaşılır ve yapıcı olsun.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", "gemini-1.5-flash")
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemPrompt\n\nKullanıcı: $message"))
                        })
                    })
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
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
                        return parts.getJSONObject(0).optString("text")
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateOfflineSmartResponse(message: String, knowledgeContext: String, assistantName: String): String {
        val lower = message.lowercase(Locale("tr"))

        if (knowledgeContext.isNotBlank()) {
            return "Hafızamdaki bilgilere göre:\n$knowledgeContext\n\nBaşka bir konuda yardımcı olmamı ister misiniz?"
        }

        return when {
            lower.contains("merhaba") || lower.contains("selam") -> {
                "Merhaba! Ben $assistantName. Size randevularınızı hatırlatabilir, nöbetçi eczane, hastane veya otopark bulabilir, sesli alarmlar kurabilirim. Size nasıl yardımcı olabilirim?"
            }
            lower.contains("nasılsın") || lower.contains("ne haber") -> {
                "Harikayım, teşekkür ederim! Sizin gününüzü kolaylaştırmak için hazırım. Bugün planladığınız önemli bir randevunuz var mı?"
            }
            lower.contains("tavsiye") || lower.contains("öneri") || lower.contains("ne yapmalı") -> {
                "Gününüzü daha verimli geçirmek için yaklaşan randevularınızı kontrol edebilir, düzenli su içmeyi ve dinlenme molalarını ihmal etmemeyi hatırlatabilirim. İsterseniz hemen sesli bir hatırlatıcı kuralım!"
            }
            lower.contains("kimsin") || lower.contains("nesin") -> {
                "Ben HatırlaGit akıllı yaşam asistanınızım. Bilgilerinizi sadece telefonunuzda güvenle saklar, randevularınızı organize eder ve size en yakın önemli yerleri bulurum."
            }
            else -> {
                "Sizi dinliyorum! Bana sesli olarak randevu kurdurabilir, 'En yakın nöbetçi eczane nerede?' diye sorabilir veya 'Şunu öğren:' diyerek hafızama yeni bilgiler kaydedebilirsiniz."
            }
        }
    }
}
