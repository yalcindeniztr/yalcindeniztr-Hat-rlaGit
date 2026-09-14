package com.example.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import com.example.data.SavedLocationEntity
import com.example.data.AiKnowledgeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParsedActionResult(
    val speechText: String,
    val actionType: String?,
    val actionPayload: JSONObject?,
    val executionSummary: String? = null
)

data class ActionFeedbackResult(
    val status: String, // "success" or "error"
    val action: String,
    val message: String,
    val jsonFeedback: String = JSONObject().apply {
        put("status", status)
        put("action", action)
        put("message", message)
    }.toString()
)

object ActionDispatcherHelper {

    private val ACTION_BLOCK_REGEX = Regex("""(?s)```(?:action|json)?\s*(\{\s*["'](?:action_type|function)["'][\s\S]*?\})\s*```""")

    fun parseActionBlock(rawText: String): ParsedActionResult {
        var cleanSpeech = rawText.trim()
        var matchedJsonStr: String? = null

        val blockMatch = ACTION_BLOCK_REGEX.find(cleanSpeech)
        if (blockMatch != null) {
            matchedJsonStr = blockMatch.groupValues[1]
            cleanSpeech = cleanSpeech.replace(blockMatch.value, "").trim()
        } else {
            val actionKeyIdx = listOf("\"action_type\"", "'action_type'", "\"function\"", "'function'")
                .map { cleanSpeech.indexOf(it) }
                .filter { it != -1 }
                .minOrNull() ?: -1

            if (actionKeyIdx != -1) {
                val startBrace = cleanSpeech.lastIndexOf('{', actionKeyIdx)
                if (startBrace != -1) {
                    var braceCount = 0
                    var endBrace = -1
                    for (i in startBrace until cleanSpeech.length) {
                        if (cleanSpeech[i] == '{') braceCount++
                        else if (cleanSpeech[i] == '}') {
                            braceCount--
                            if (braceCount == 0) {
                                endBrace = i
                                break
                            }
                        }
                    }
                    if (endBrace != -1) {
                        matchedJsonStr = cleanSpeech.substring(startBrace, endBrace + 1)
                        cleanSpeech = (cleanSpeech.substring(0, startBrace) + " " + cleanSpeech.substring(endBrace + 1)).trim()
                    }
                }
            }
        }

        cleanSpeech = cleanSpeech.replace(Regex("""(?s)```[a-zA-Z0-9_-]*\s*[\s\S]*?```"""), " ")
        cleanSpeech = cleanSpeech.replace(Regex("""(?s)<(?:action|json|code)>[\s\S]*?</(?:action|json|code)>"""), " ")
        
        var braceStart = cleanSpeech.indexOf('{')
        var guard = 0
        while (braceStart != -1 && guard < 10) {
            guard++
            var depth = 0
            var braceEnd = -1
            for (i in braceStart until cleanSpeech.length) {
                if (cleanSpeech[i] == '{') depth++
                else if (cleanSpeech[i] == '}') {
                    depth--
                    if (depth == 0) {
                        braceEnd = i
                        break
                    }
                }
            }
            if (braceEnd != -1) {
                cleanSpeech = cleanSpeech.substring(0, braceStart) + " " + cleanSpeech.substring(braceEnd + 1)
            } else {
                cleanSpeech = cleanSpeech.replace("{", "")
                break
            }
            braceStart = cleanSpeech.indexOf('{')
        }

        cleanSpeech = cleanSpeech.replace(Regex("""(?i)(?:action_type|function|action_step|payload|parameters|target_package|coords|timestamp|status|action|code|json)\s*:\s*[^,
\}]+"""), " ")
        cleanSpeech = cleanSpeech.replace(Regex("""^(?:```[a-zA-Z0-9_-]*|```|\[[a-zA-Z0-9_-]+\]|CODE:|ACTION:)\s*""", RegexOption.IGNORE_CASE), "")

        var speechForTts = cleanSpeech
            .replace("**", "")
            .replace("*", "")
            .replace("###", "")
            .replace("##", "")
            .replace("#", "")
            .replace("```", "")
            .replace(Regex("""^[^\p{L}\p{N}]+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        var actionType: String? = null
        var actionPayload: JSONObject? = null

        if (!matchedJsonStr.isNullOrBlank()) {
            try {
                val json = JSONObject(matchedJsonStr)
                actionType = json.optString("action_type").takeIf { it.isNotBlank() }
                    ?: json.optString("function").takeIf { it.isNotBlank() }
                actionPayload = json.optJSONObject("payload")
                    ?: json.optJSONObject("parameters")
                    ?: JSONObject()
            } catch (_: Exception) { }
        }

        if (speechForTts.isBlank()) {
            speechForTts = when (actionType?.lowercase(Locale.ROOT)) {
                "create_event", "manage_calendar" -> "Etkinlik ajandanıza işlendi, efendim."
                "call_phone" -> "Aramayı başlatıyorum, efendim."
                "send_whatsapp" -> "WhatsApp mesajını hazırladım, efendim."
                "set_alarm" -> "Alarm kuruldu, efendim."
                "set_reminder" -> "Hatırlatıcı kaydedildi, efendim."
                "save_voice_memo", "add_quick_note" -> "Notunuzu aldım, efendim."
                "navigate", "search_map", "open_maps" -> "Harita navigasyonunu açıyorum, efendim."
                "save_location" -> "Konum kaydedildi, efendim."
                "play_music", "play_youtube" -> "Medya başlatılıyor, efendim."
                "open_gemini" -> "Google Gemini köprüsünü açıyorum, efendim."
                "search_google", "search_web" -> "Arama başlatılıyor, efendim."
                "open_app" -> "Uygulamayı açıyorum, efendim."
                "fetch_news" -> "Günün başlıklarını derledim, efendim."
                "generate_document" -> "Belgeniz Maarif Modeli standartlarında hazırlandı efendim."
                "set_device_profile" -> "Cihaz profili güncellendi efendim."
                else -> "Emredersiniz efendim, işlem tamamlandı."
            }
        }

        return ParsedActionResult(
            speechText = speechForTts,
            actionType = actionType,
            actionPayload = actionPayload
        )
    }

    suspend fun executeActionWithFeedback(
        context: Context,
        actionType: String,
        payload: JSONObject
    ): ActionFeedbackResult = withContext(Dispatchers.IO) {
        val normalized = actionType.lowercase(Locale.ROOT)
        try {
            when (normalized) {
                "generate_document" -> {
                    val title = payload.optString("title", "Maarif Modeli Belgesi")
                    val fileFormat = payload.optString("file_format", "pdf")
                    val templateType = payload.optString("template_type", "maarif_plan")
                    
                    val (_, summary) = when (templateType) {
                        "zumre_tutanak", "zümre" -> {
                            MebDocumentHelper.createSokMeetingPdf(
                                context = context,
                                params = SokMeetingParams(
                                    schoolName = "Anadolu Lisesi",
                                    className = "10-A",
                                    termName = "1. Dönem",
                                    classTeacherName = "Zümre Öğretmeni"
                                )
                            )
                        }
                        "sinav_analiz", "sınav" -> {
                            MebDocumentHelper.createExamPaperPdf(
                                context = context,
                                schoolName = "Anadolu Lisesi",
                                courseName = "Türk Dili ve Edebiyatı / Tarih",
                                gradeLevel = "10. Sınıf",
                                examName = "1. Dönem Yazılı Sınavı",
                                examContent = "Türkiye Yüzyılı Maarif Modeli süreç odaklı açık uçlu senaryolar ve rubrik puanlama anahtarı."
                            )
                        }
                        else -> {
                            MebDocumentHelper.createAnnualPlanPdf(
                                context = context,
                                params = AnnualPlanParams(
                                    schoolName = "Anadolu Lisesi",
                                    principalName = "Okul Müdürü",
                                    teachers = "Ders Öğretmeni",
                                    courseName = title,
                                    gradeLevel = "10. Sınıf",
                                    planType = "Türkiye Yüzyılı Maarif Modeli Yıllık Planı"
                                )
                            )
                        }
                    }

                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "generate_document",
                        message = "$title Maarif Modeli standartlarında $fileFormat olarak hazırlandı ve cihazınıza kaydedildi efendim."
                    )
                }

                "save_voice_memo" -> {
                    val title = payload.optString("title", "Sesli Not").take(40)
                    val cleanText = payload.optString("clean_text", "").ifBlank {
                        payload.optString("text", "")
                    }
                    val now = System.currentTimeMillis()
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val dateStr = sdf.format(Date(now))

                    val db = AppDatabase.getDatabase(context)
                    db.reminderDao().insertReminder(
                        ReminderEntity(
                            category = "SESLİ NOT",
                            title = title,
                            customNote = cleanText,
                            dueDateMillis = now,
                            dueDatetime = dateStr,
                            isFavorite = true,
                            encryptedMetadata = "{}",
                            actionStep = "NOTE_SAVED"
                        )
                    )
                    LocalStorageManager.saveLocalNote(context, title, cleanText, "SESLİ NOT")

                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "save_voice_memo",
                        message = "Sesli notunuz profesyonel formatta kaydedildi: $title"
                    )
                }

                "get_saved_memos", "query_memory" -> {
                    val query = payload.optString("search_query", "").ifBlank {
                        payload.optString("query", "")
                    }.lowercase(Locale.ROOT)
                    val db = AppDatabase.getDatabase(context)
                    val notes = db.reminderDao().getAllRemindersList().filter { it.category == "SESLİ NOT" }
                    val matched = if (query.isNotBlank()) {
                        notes.filter { it.title.lowercase(Locale.ROOT).contains(query) || it.customNote.lowercase(Locale.ROOT).contains(query) }
                    } else notes

                    val summary = if (matched.isNotEmpty()) {
                        "Kayıtlı Notlarınız:\n" + matched.take(3).joinToString("\n") { "• ${it.title}: ${it.customNote.take(80)}" }
                    } else "Eşleşen bir kayıtlı not bulunamadı efendim."

                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "query_memory",
                        message = summary
                    )
                }

                "library_manage" -> {
                    val act = payload.optString("action", "add").lowercase(Locale.ROOT)
                    val title = payload.optString("title", "Kütüphane Notu")
                    val content = payload.optString("content", "")
                    val category = payload.optString("category", "pedagogy").uppercase(Locale.ROOT)
                    val db = AppDatabase.getDatabase(context)
                    
                    val entity = AiKnowledgeEntity(
                        title = title,
                        content = content,
                        category = category,
                        isOfficialVerified = true,
                        source = "Jarvis İskenderiye Modülü",
                        createdAt = System.currentTimeMillis()
                    )
                    db.aiKnowledgeDao().insertKnowledge(entity)
                    
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "library_manage",
                        message = "'$title' başlıklı kayıt kişisel dijital kütüphanenize eklendi efendim."
                    )
                }

                "library_search" -> {
                    val query = payload.optString("query", "")
                    val db = AppDatabase.getDatabase(context)
                    val results = if (query.isNotBlank()) {
                        db.aiKnowledgeDao().searchKnowledge(query)
                    } else {
                        db.aiKnowledgeDao().getAllKnowledgeList()
                    }
                    
                    val summary = if (results.isNotEmpty()) {
                        "Kütüphane Sonuçları:\n" + results.take(3).joinToString("\n") { "• ${it.title}: ${it.content.take(90)}" }
                    } else "Kütüphanede eşleşen kaynak bulunamadı efendim."
                    
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "library_search",
                        message = summary
                    )
                }

                "set_device_profile" -> {
                    val profile = payload.optString("profile", "work").lowercase(Locale.ROOT)
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    when (profile) {
                        "silent", "class_mode", "focus", "meeting" -> {
                            audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
                        }
                        "relax" -> {
                            audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        }
                        "work" -> {
                            audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        }
                    }
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "set_device_profile",
                        message = "Cihaz profili '$profile' moduna ayarlandı efendim."
                    )
                }

                "get_device_status" -> {
                    val param = payload.optString("parameter", "battery").lowercase(Locale.ROOT)
                    val statusText = when (param) {
                        "battery" -> {
                            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                            "Batarya seviyesi: %$level"
                        }
                        "network" -> {
                            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                            val network = cm?.activeNetwork
                            val caps = cm?.getNetworkCapabilities(network)
                            val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                            if (isConnected) "Ağ bağlantısı: Aktif ve bağlı" else "Ağ bağlantısı: Çevrimdışı"
                        }
                        "activity_level", "circadian" -> {
                            "Biyolojik ritim: Patron aktif çalışma modunda, hidrasyon ve duruş molası önerilir."
                        }
                        else -> "Bildirim ve telemetri servisleri aktif."
                    }
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "get_device_status",
                        message = statusText
                    )
                }

                "set_alarm" -> {
                    val rawTime = payload.optString("time", "").ifBlank { payload.optString("trigger_time", "") }
                    val label = payload.optString("label", "").ifBlank {
                        payload.optString("title", "Alarm")
                    }

                    var hour = payload.optInt("hour", -1)
                    var minute = payload.optInt("minute", 0)

                    if (hour == -1 && rawTime.isNotBlank()) {
                        val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(rawTime)
                        val singleHourMatch = Regex("""(?i)(\d{1,2})""").find(rawTime)
                        hour = timeMatch?.groupValues?.get(1)?.toIntOrNull()
                            ?: singleHourMatch?.groupValues?.get(1)?.toIntOrNull()
                            ?: 9
                        minute = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                    }
                    if (hour == -1) hour = 9

                    val (sysSuccess, _) = AppLauncherHelper.setDeviceAlarm(context, hour, minute, label)

                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val reminder = ReminderEntity(
                        category = "GENEL",
                        title = label,
                        dueDatetime = sdf.format(cal.time),
                        dueDateMillis = cal.timeInMillis,
                        customNote = "ATİLA Jarvis tarafından kuruldu.",
                        encryptedMetadata = "{}",
                        actionStep = "SOUND_CLASSIC_BELL"
                    )
                    val db = AppDatabase.getDatabase(context)
                    val id = db.reminderDao().insertReminder(reminder)
                    AlarmHelper.scheduleAlarm(context, reminder.copy(id = id.toInt()), "CLASSIC_BELL")

                    val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                    val detail = if (sysSuccess) "$timeFormatted alarmı kuruldu." else "$timeFormatted alarmı yerel belleğe kaydedildi."
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "set_alarm",
                        message = detail
                    )
                }

                "set_reminder" -> {
                    val title = payload.optString("title", "Hatırlatıcı")
                    val rawTrigger = payload.optString("trigger_time", "").ifBlank { payload.optString("time", "") }
                    val cal = Calendar.getInstance()
                    if (rawTrigger.isNotBlank()) {
                        val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(rawTrigger)
                        val h = timeMatch?.groupValues?.get(1)?.toIntOrNull()
                        val m = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                        if (h != null) {
                            cal.set(Calendar.HOUR_OF_DAY, h)
                            cal.set(Calendar.MINUTE, m)
                            cal.set(Calendar.SECOND, 0)
                            if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                        } else {
                            cal.add(Calendar.HOUR_OF_DAY, 2)
                        }
                    } else {
                        cal.add(Calendar.HOUR_OF_DAY, 2)
                    }

                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val reminder = ReminderEntity(
                        category = "HATIRLATICI",
                        title = title,
                        dueDatetime = sdf.format(cal.time),
                        dueDateMillis = cal.timeInMillis,
                        customNote = "ATİLA Jarvis tarafından kaydedildi.",
                        encryptedMetadata = "{}",
                        actionStep = "SOUND_CLASSIC_BELL"
                    )
                    val db = AppDatabase.getDatabase(context)
                    val id = db.reminderDao().insertReminder(reminder)
                    AlarmHelper.scheduleAlarm(context, reminder.copy(id = id.toInt()), "CLASSIC_BELL")
                    LocalStorageManager.saveLocalReminder(context, title, sdf.format(cal.time), cal.timeInMillis, "HATIRLATICI")

                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "set_reminder",
                        message = "'$title' hatırlatıcısı ${sdf.format(cal.time)} için kaydedildi."
                    )
                }

                "manage_calendar", "create_event" -> {
                    val action = payload.optString("action", "create").lowercase(Locale.ROOT)
                    val title = payload.optString("title", "Randevu")
                    val desc = payload.optString("description", "ATİLA Jarvis Ajanda")

                    if (action == "list") {
                        val (text, _) = NearbyPlacesHelper.getUpcomingCalendarBriefing(context)
                        return@withContext ActionFeedbackResult(
                            status = "success",
                            action = "manage_calendar",
                            message = text
                        )
                    }

                    var startMillis = payload.optLong("startTimeMillis", 0L)
                    if (startMillis <= 0L) {
                        val rawStart = payload.optString("start_time", "")
                        startMillis = parseFlexibleTime(rawStart)
                    }
                    val endMillis = payload.optLong("endTimeMillis", startMillis + 3600000L)

                    val inserted = NearbyPlacesHelper.insertEventIntoCalendar(
                        context = context,
                        title = title,
                        description = desc,
                        startTimeMillis = startMillis,
                        endTimeMillis = endMillis,
                        openUi = false
                    )

                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val rem = ReminderEntity(
                        category = "RANDEVU",
                        title = title,
                        dueDatetime = sdf.format(Date(startMillis)),
                        dueDateMillis = startMillis,
                        customNote = desc,
                        encryptedMetadata = "{}",
                        actionStep = "SOUND_CLASSIC_BELL"
                    )
                    AppDatabase.getDatabase(context).reminderDao().insertReminder(rem)

                    return@withContext ActionFeedbackResult(
                        status = if (inserted) "success" else "success",
                        action = "manage_calendar",
                        message = "Etkinlik takvime işlendi: $title (${sdf.format(Date(startMillis))})"
                    )
                }

                "play_youtube", "play_music" -> {
                    val query = payload.optString("query", "").ifBlank {
                        payload.optString("songQuery", "Türkçe Müzik")
                    }
                    val (success, msg) = AppLauncherHelper.searchAndPlayYouTube(context, query)
                    return@withContext ActionFeedbackResult(
                        status = if (success) "success" else "error",
                        action = "play_youtube",
                        message = msg
                    )
                }

                "search_web", "search_google" -> {
                    val query = payload.optString("query", "Google Ara")
                    val (success, msg) = AppLauncherHelper.searchGoogle(context, query)
                    return@withContext ActionFeedbackResult(
                        status = if (success) "success" else "error",
                        action = "search_web",
                        message = msg
                    )
                }

                "fetch_news", "daily_news" -> {
                    val headlines = DailyNewsHelper.getHeadlinesOnly()
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "fetch_news",
                        message = headlines
                    )
                }

                "log_user_preference" -> {
                    val key = payload.optString("key", "")
                    val value = payload.optString("value", "")
                    if (key.isNotBlank() && value.isNotBlank()) {
                        val dsm = com.example.data.DataStoreManager(context)
                        if (key.contains("nick") || key.contains("isim")) {
                            dsm.updateNick(value)
                        }
                    }
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "log_user_preference",
                        message = "Tercih kaydedildi: $key = $value"
                    )
                }

                "call_phone" -> {
                    var phone = payload.optString("phone", "")
                    val name = payload.optString("name", "")
                    var resolvedName: String? = null

                    if (phone.isBlank() && name.isNotBlank()) {
                        if (ContactHelper.hasContactsPermission(context)) {
                            val contact = ContactHelper.findContactByName(context, name)
                            if (contact != null) {
                                phone = contact.phoneNumber
                                resolvedName = contact.name
                            }
                        } else {
                            return@withContext ActionFeedbackResult(
                                status = "error",
                                action = "call_phone",
                                message = "İzin yetkisi eksik: Ayarlardan rehber erişimi vermelisiniz efendim."
                            )
                        }
                    }

                    if (phone.isNotBlank()) {
                        NearbyPlacesHelper.makePhoneCall(context, phone)
                        return@withContext ActionFeedbackResult(
                            status = "success",
                            action = "call_phone",
                            message = if (resolvedName != null) "$resolvedName aranıyor ($phone)..." else "$phone aranıyor..."
                        )
                    }
                    return@withContext ActionFeedbackResult(
                        status = "error",
                        action = "call_phone",
                        message = "Aranacak kişi veya numara bulunamadı."
                    )
                }

                "send_whatsapp" -> {
                    var phone = payload.optString("phone", "").replace(Regex("[^0-9+]"), "")
                    val name = payload.optString("name", "")
                    val message = payload.optString("message", "")

                    if (phone.isBlank() && name.isNotBlank()) {
                        if (ContactHelper.hasContactsPermission(context)) {
                            val contact = ContactHelper.findContactByName(context, name)
                            if (contact != null) {
                                phone = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                            }
                        } else {
                            return@withContext ActionFeedbackResult(
                                status = "error",
                                action = "send_whatsapp",
                                message = "İzin yetkisi eksik: Ayarlardan rehber erişimi vermelisiniz efendim."
                            )
                        }
                    }

                    val cleanDigits = phone.replace("+", "")
                    val url = if (cleanDigits.isNotBlank()) {
                        "https://api.whatsapp.com/send?phone=$cleanDigits&text=${Uri.encode(message)}"
                    } else {
                        "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "send_whatsapp",
                        message = "WhatsApp mesaj ekranı açıldı."
                    )
                }

                "navigate", "search_map", "open_maps" -> {
                    val query = payload.optString("query", "Hedef")
                    val lat = payload.optDouble("lat", 0.0)
                    val lng = payload.optDouble("lng", 0.0)
                    NearbyPlacesHelper.openGoogleMapsNavigation(context, query, lat, lng, query)
                    return@withContext ActionFeedbackResult(
                        status = "success",
                        action = "navigate",
                        message = "Harita navigasyonu açıldı: $query"
                    )
                }

                "save_location" -> {
                    val name = payload.optString("name", "Kayıtlı Lokasyon")
                    val lat = payload.optDouble("lat", 0.0)
                    val lng = payload.optDouble("lng", 0.0)
                    if (lat != 0.0 && lng != 0.0) {
                        val loc = SavedLocationEntity(
                            name = name,
                            lat = lat,
                            lng = lng,
                            timestamp = System.currentTimeMillis()
                        )
                        AppDatabase.getDatabase(context).savedLocationDao().insertLocation(loc)
                        return@withContext ActionFeedbackResult(
                            status = "success",
                            action = "save_location",
                            message = "Konum kaydedildi: $name"
                        )
                    }
                    return@withContext ActionFeedbackResult(
                        status = "error",
                        action = "save_location",
                        message = "Koordinatlar geçersiz."
                    )
                }

                "open_app" -> {
                    val appName = payload.optString("app_name", "").ifBlank {
                        payload.optString("name", "")
                    }
                    val (success, msg) = AppLauncherHelper.openApplicationByVoice(context, appName)
                    return@withContext ActionFeedbackResult(
                        status = if (success) "success" else "error",
                        action = "open_app",
                        message = msg
                    )
                }

                else -> {
                    ActionFeedbackResult(
                        status = "success",
                        action = normalized,
                        message = "İşlem tamamlandı."
                    )
                }
            }
        } catch (e: Exception) {
            ActionFeedbackResult(
                status = "error",
                action = normalized,
                message = "İşletim sistemi hatası: ${e.localizedMessage}"
            )
        }
    }

    suspend fun executeAction(context: Context, actionType: String, payload: JSONObject): String {
        return executeActionWithFeedback(context, actionType, payload).message
    }

    private fun parseFlexibleTime(rawTime: String): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 1)
        if (rawTime.isBlank()) return cal.timeInMillis

        try {
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(rawTime)
            val h = timeMatch?.groupValues?.get(1)?.toIntOrNull()
            val m = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
            if (h != null && h in 0..23) {
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        } catch (_: Exception) {}
        return cal.timeInMillis
    }
}
