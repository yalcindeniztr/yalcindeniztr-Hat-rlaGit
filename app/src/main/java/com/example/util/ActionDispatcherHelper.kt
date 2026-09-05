package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
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

        val blockMatch = ACTION_BLOCK_REGEX.find(cleanSpeech)
        if (blockMatch != null) {
            matchedJsonStr = blockMatch.groupValues[1]
            cleanSpeech = cleanSpeech.replace(blockMatch.value, "").trim()
        } else {
            val inlineMatch = INLINE_ACTION_REGEX.find(cleanSpeech)
            if (inlineMatch != null) {
                matchedJsonStr = inlineMatch.groupValues[1]
                cleanSpeech = cleanSpeech.replace(inlineMatch.value, "").trim()
            }
        }

        // Clean speech text from Markdown characters for natural TTS
        val speechForTts = cleanSpeech
            .replace("**", "")
            .replace("*", "")
            .replace("###", "")
            .replace("##", "")
            .replace("#", "")
            .replace("```", "")
            .trim()

        if (!matchedJsonStr.isNullOrBlank()) {
            return try {
                val json = JSONObject(matchedJsonStr)
                val type = json.optString("action_type")
                val payload = json.optJSONObject("payload") ?: JSONObject()
                ParsedActionResult(
                    speechText = speechForTts,
                    actionType = type,
                    actionPayload = payload
                )
            } catch (e: Exception) {
                ParsedActionResult(
                    speechText = speechForTts,
                    actionType = null,
                    actionPayload = null
                )
            }
        }

        return ParsedActionResult(
            speechText = speechForTts,
            actionType = null,
            actionPayload = null
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
                    val phone = payload.optString("phone", "").replace(Regex("[^0-9]"), "")
                    val message = payload.optString("message", "")
                    val url = if (phone.isNotBlank()) {
                        "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
                    } else {
                        "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return@withContext "💬 WhatsApp mesajı hazırlandı."
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
                    // Roborock, Mi Home veya Google Home başlatma
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
                        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=roborock")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
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

                "CHECK_NOTIFICATIONS" -> {
                    if (!AppNotificationListenerService.isPermissionGranted(context)) {
                        AppNotificationListenerService.openSettings(context)
                        return@withContext "🔔 Gardrops, WhatsApp ve alışveriş bildirimlerini takip edebilmem için lütfen açılan ekrandan 'HatırlaGit' için Bildirim Erişimi iznini etkinleştirin."
                    } else {
                        return@withContext CapturedNotificationCache.getSummaryText()
                    }
                }

                "CALL_PHONE" -> {
                    val phone = payload.optString("phone", "")
                    if (phone.isNotBlank()) {
                        NearbyPlacesHelper.makePhoneCall(context, phone)
                        return@withContext "📞 Arama başlatılıyor: $phone"
                    }
                    return@withContext "Telefon numarası bulunamadı."
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

                else -> "Eylem tamamlandı."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Eylem hatası: ${e.localizedMessage}"
        }
    }
}
