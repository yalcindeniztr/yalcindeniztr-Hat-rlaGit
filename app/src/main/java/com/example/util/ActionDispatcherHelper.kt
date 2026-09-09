package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import com.example.data.SavedLocationEntity
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

object ActionDispatcherHelper {

    private val ACTION_BLOCK_REGEX = Regex("""(?s)```(?:action|json)?\s*(\{\s*["']action_type["'][\s\S]*?\})\s*```""")
    private val INLINE_ACTION_REGEX = Regex("""(?s)(\{\s*["']action_type["']\s*:\s*["'][A-Z_]+["'][\s\S]*?\})\s*$""")

    fun parseActionBlock(rawText: String): ParsedActionResult {
        var cleanSpeech = rawText.trim()
        var matchedJsonStr: String? = null

        // 1. Fenced kod bloklarını kontrol et (```action ... ``` veya ```json ... ```)
        val blockMatch = ACTION_BLOCK_REGEX.find(cleanSpeech)
        if (blockMatch != null) {
            matchedJsonStr = blockMatch.groupValues[1]
            cleanSpeech = cleanSpeech.replace(blockMatch.value, "").trim()
        } else {
            // 2. Metin içinde "action_type" içeren dengeli JSON bloğunu bul ve ayıkla
            val actionKeyIdx = cleanSpeech.indexOf("\"action_type\"").let { 
                if (it == -1) cleanSpeech.indexOf("'action_type'") else it 
            }
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

        // 3. Kalan tüm kod bloklarını ve teknik JSON/etiket kalıntılarını temizle
        cleanSpeech = cleanSpeech.replace(Regex("""(?s)```[a-zA-Z0-9_-]*\s*[\s\S]*?```"""), " ")
        cleanSpeech = cleanSpeech.replace(Regex("""(?s)<(?:action|json|code)>[\s\S]*?</(?:action|json|code)>"""), " ")
        
        // Kalan herhangi bir dengeli süslü parantez bloğunu kaldır
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

        cleanSpeech = cleanSpeech.replace(Regex("""(?i)\b(?:action_type|action_step|payload|target_package|coords|timestamp|status|action|code|json)\s*:\s*[^,\n\}]+"""), " ")
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
                actionPayload = json.optJSONObject("payload") ?: JSONObject()
            } catch (_: Exception) { }
        }

        // Eğer eylem bloğu çıkarıldıktan sonra konuşma metni boş kalmışsa, eyleme uygun kısa onay cümlesi üret
        if (speechForTts.isBlank()) {
            speechForTts = when (actionType?.uppercase(Locale.ROOT)) {
                "SET_ALARM" -> "Alarmı kurdum dostum."
                "SET_REMINDER" -> "Hatırlatıcıyı kaydettim dostum."
                "ADD_QUICK_NOTE" -> "Notu ekledim dostum."
                "NAVIGATE", "SEARCH_MAP" -> "Navigasyonu açıyorum dostum."
                "SAVE_LOCATION" -> "Konumu kaydettim dostum."
                "PLAY_MUSIC" -> "Müziği açıyorum dostum."
                "OPEN_GEMINI" -> "Google Gemini köprüsünü açıyorum dostum."
                "SEARCH_GOOGLE" -> "Google'da aratıyorum dostum."
                "OPEN_APP" -> "Uygulamayı açıyorum dostum."
                else -> "Buyrun dostum!"
            }
        }

        return ParsedActionResult(
            speechText = speechForTts,
            actionType = actionType,
            actionPayload = actionPayload
        )
    }

    suspend fun executeAction(context: Context, actionType: String, payload: JSONObject): String = withContext(Dispatchers.IO) {
        try {
            when (actionType.uppercase(Locale.ROOT)) {
                "SET_ALARM" -> {
                    val hour = payload.optInt("hour", 9)
                    val minute = payload.optInt("minute", 0)
                    val title = payload.optString("title", "HatırlaGit Alarm")
                    val message = payload.optString("message", title)

                    // 1. Android Native AlarmClock Intent
                    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, message)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (alarmIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(alarmIntent)
                    }

                    // 2. HatırlaGit Room DB Kaydı & Alarm Servisi
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
                        title = title,
                        dueDatetime = sdf.format(cal.time),
                        dueDateMillis = cal.timeInMillis,
                        customNote = "Usta tarafından sesle oluşturuldu.",
                        encryptedMetadata = "{}",
                        actionStep = "SOUND_CLASSIC_BELL"
                    )
                    val db = AppDatabase.getDatabase(context)
                    val id = db.reminderDao().insertReminder(reminder)
                    AlarmHelper.scheduleAlarm(context, reminder.copy(id = id.toInt()), "CLASSIC_BELL")

                    return@withContext "⏰ Alarm ${String.format(Locale.ROOT, "%02d:%02d", hour, minute)} için kuruldu."
                }

                "CREATE_EVENT" -> {
                    val title = payload.optString("title", "Randevu")
                    val description = payload.optString("description", "")
                    val startMillis = payload.optLong("startTimeMillis", System.currentTimeMillis() + 3600000L)
                    val endMillis = payload.optLong("endTimeMillis", startMillis + 3600000L)

                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, title)
                        putExtra(CalendarContract.Events.DESCRIPTION, description)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)

                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val reminder = ReminderEntity(
                        category = "RANDEVU",
                        title = title,
                        dueDatetime = sdf.format(Date(startMillis)),
                        dueDateMillis = startMillis,
                        customNote = description,
                        encryptedMetadata = "{}",
                        actionStep = "SOUND_CLASSIC_BELL"
                    )
                    AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

                    return@withContext "📅 Randevu telefon takviminize ve akıllı saat senkronizasyonuna işlendi: $title"
                }

                "SEND_WHATSAPP" -> {
                    var phone = payload.optString("phone", "").replace(Regex("[^0-9+]"), "")
                    val name = payload.optString("name", "")
                    val message = payload.optString("message", "")

                    var resolvedName: String? = null
                    if (phone.isBlank() && name.isNotBlank()) {
                        if (ContactHelper.hasContactsPermission(context)) {
                            val contact = ContactHelper.findContactByName(context, name)
                            if (contact != null) {
                                phone = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                                resolvedName = contact.name
                            }
                        } else {
                            return@withContext "🔒 Rehberinizdeki kişilere WhatsApp mesajı gönderebilmem için lütfen Rehber İznini etkinleştirin dostum."
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
                    return@withContext if (resolvedName != null) {
                        "💬 $resolvedName kişisine WhatsApp mesajı hazırlandı."
                    } else {
                        "💬 WhatsApp mesajı hazırlandı."
                    }
                }

                "SEND_SMS" -> {
                    val phone = payload.optString("phone", "").replace(Regex("[^0-9+]"), "")
                    val message = payload.optString("message", "")
                    val uri = if (phone.isNotBlank()) Uri.parse("smsto:$phone") else Uri.parse("smsto:")
                    val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (smsIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(smsIntent)
                        return@withContext "✉️ SMS mesaj ekranı açıldı."
                    } else {
                        return@withContext "SMS uygulaması bulunamadı."
                    }
                }

                "OPEN_MAPS" -> {
                    val query = payload.optString("query", "Nöbetçi Eczane")
                    val lat = payload.optDouble("lat", 0.0)
                    val lng = payload.optDouble("lng", 0.0)
                    NearbyPlacesHelper.openGoogleMapsNavigation(context, query, lat, lng, query)
                    return@withContext "🗺️ Google Haritalar açıldı: $query"
                }

                "POST_INSTAGRAM" -> {
                    val caption = payload.optString("caption", "")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        setPackage("com.instagram.android")
                        putExtra(Intent.EXTRA_TEXT, caption)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        return@withContext "📸 Instagram paylaşımı başlatıldı."
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                        return@withContext "📸 Instagram açıldı."
                    }
                }

                "START_VACUUM" -> {
                    val roborockPkg = "com.roborock.smart"
                    val miHomePkg = "com.xiaomi.smarthome"
                    val googleHomePkg = "com.google.android.apps.chromecast.app"

                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(roborockPkg)
                        ?: pm.getLaunchIntentForPackage(miHomePkg)
                        ?: pm.getLaunchIntentForPackage(googleHomePkg)

                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return@withContext "🧹 Akıllı robot süpürge uygulaması açıldı. Temizlik başlatılıyor..."
                    } else {
                        return@withContext "🧹 Cihazınızda Roborock veya Mi Home uygulaması bulunamadı. Lütfen önce süpürgenizin uygulamasını yükleyin."
                    }
                }

                "SAVE_RESEARCH" -> {
                    val topic = payload.optString("topic", "Genel Araştırma")
                    val content = payload.optString("content", "")
                    return@withContext ResearchFileManager.saveResearch(context, topic, content)
                }

                "MARKET_DEALS" -> {
                    val market = payload.optString("market", "")
                    return@withContext if (market.isNotBlank()) {
                        MarketDealsHelper.getDealsForMarket(market)
                    } else {
                        MarketDealsHelper.getMorningDealsSummary()
                    }
                }

                "DAILY_NEWS" -> {
                    return@withContext DailyNewsHelper.getHeadlinesBriefing()
                }

                "SAVE_LOCATION" -> {
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
                        return@withContext "📍 Lokasyon 'Kayıtlı Lokasyonlarım' arasına başarıyla eklendi: $name"
                    }
                    return@withContext "Lokasyon koordinatları bulunamadı."
                }

                "GENERATE_LESSON_PLAN_PDF" -> {
                    val course = payload.optString("course", "Tarih")
                    val grade = payload.optString("grade", "9. Sınıf")
                    val content = payload.optString("content", "")
                    val (_, summary) = LessonPlanPdfHelper.createLessonPlanPdf(context, course, grade, content)
                    return@withContext summary
                }

                "CHECK_NOTIFICATIONS" -> {
                    if (!AppNotificationListenerService.isPermissionGranted(context)) {
                        AppNotificationListenerService.openSettings(context)
                        return@withContext "🔔 Gardrops, WhatsApp ve alışveriş bildirimlerini takip edebilmem için lütfen açılan ekrandan 'HatırlaGit' için Bildirim Erişimi iznini etkinleştirin."
                    } else {
                        return@withContext CapturedNotificationCache.getSummaryText()
                    }
                }

                "CALL_PHONE" -> {
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
                            return@withContext "🔒 Rehberinizdeki kişileri arayabilmem için lütfen Rehber İznini etkinleştirin dostum."
                        }
                    }

                    if (phone.isNotBlank()) {
                        NearbyPlacesHelper.makePhoneCall(context, phone)
                        return@withContext if (resolvedName != null) {
                            "📞 $resolvedName aranıyor ($phone)..."
                        } else {
                            "📞 Arama başlatılıyor: $phone"
                        }
                    }
                    return@withContext "Aranacak kişi veya telefon numarası bulunamadı dostum."
                }

                "SAVE_PARK_LOCATION" -> {
                    val lat = payload.optDouble("lat", 0.0)
                    val lng = payload.optDouble("lng", 0.0)
                    val dataStoreManager = com.example.data.DataStoreManager(context)
                    if (lat != 0.0 && lng != 0.0) {
                        dataStoreManager.saveParkedCarLocation(
                            lat = lat.toString(),
                            lng = lng.toString(),
                            time = System.currentTimeMillis()
                        )
                    }
                    return@withContext "🚗 Park konumunuz başarıyla kaydedildi."
                }

                "OPEN_GEMINI" -> {
                    val prompt = payload.optString("prompt", "")
                    val (_, msg) = AppLauncherHelper.openGoogleGemini(context, prompt)
                    return@withContext msg
                }

                "SEARCH_GOOGLE" -> {
                    val query = payload.optString("query", "")
                    val (_, msg) = AppLauncherHelper.searchGoogle(context, query)
                    return@withContext msg
                }

                "OPEN_APP" -> {
                    val appName = payload.optString("app_name", "")
                    val (_, msg) = AppLauncherHelper.openApplicationByVoice(context, appName)
                    return@withContext msg
                }

                else -> "Eylem tamamlandı."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Eylem hatası: ${e.localizedMessage}"
        }
    }
}
