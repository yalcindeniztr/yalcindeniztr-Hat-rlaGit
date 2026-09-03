package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
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

    private val ACTION_REGEX = Regex("""(?s)```action\s*(\{.*?\})\s*```""")

    fun parseActionBlock(rawText: String): ParsedActionResult {
        val match = ACTION_REGEX.find(rawText)
        if (match != null) {
            val jsonStr = match.groupValues[1]
            val speechOnly = rawText.replace(match.value, "").trim()
            return try {
                val json = JSONObject(jsonStr)
                val type = json.optString("action_type")
                val payload = json.optJSONObject("payload")
                ParsedActionResult(
                    speechText = speechOnly,
                    actionType = type,
                    actionPayload = payload
                )
            } catch (e: Exception) {
                ParsedActionResult(
                    speechText = speechOnly,
                    actionType = null,
                    actionPayload = null
                )
            }
        }
        return ParsedActionResult(
            speechText = rawText.trim(),
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

                    // 1. Android Alarm Clock Intent
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

                    // 2. HatırlaGit Room DB Kaydı
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
                    val reminder = ReminderEntity(
                        category = "GENEL",
                        title = title,
                        dueDatetime = sdf.format(cal.time),
                        dueDateMillis = cal.timeInMillis,
                        customNote = "Yapay Zeka Antigravity tarafından sesle oluşturuldu.",
                        encryptedMetadata = "{}",
                        actionStep = "SOUND_CLASSIC_BELL"
                    )
                    val db = AppDatabase.getDatabase(context)
                    val id = db.reminderDao().insertReminder(reminder)
                    AlarmHelper.scheduleAlarm(context, reminder.copy(id = id.toInt()), "CLASSIC_BELL")

                    return@withContext "⏰ Alarm ${String.format(Locale.ROOT, "%02d:%02d", hour, minute)} için kuruldu."
                }

                "CREATE_EVENT" -> {
                    val title = payload.optString("title", "Randevu / Etkinlik")
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

                    // HatırlaGit Veritabanına da Ekle
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
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

                    return@withContext "📅 Etkinlik takviminize kaydedildi: $title"
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

                "OPEN_MAPS" -> {
                    val query = payload.optString("query", "Nöbetçi Eczane")
                    val lat = payload.optDouble("lat", 0.0)
                    val lng = payload.optDouble("lng", 0.0)
                    NearbyPlacesHelper.openGoogleMapsNavigation(context, query, lat, lng, query)
                    return@withContext "🗺️ Harita açıldı: $query"
                }

                "POST_INSTAGRAM" -> {
                    val caption = payload.optString("caption", "")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        setPackage("com.instagram.android")
                        putExtra(Intent.EXTRA_TEXT, caption)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                    }
                    return@withContext "📸 Instagram paylaşımı başlatıldı."
                }

                "CALL_PHONE" -> {
                    val phone = payload.optString("phone", "")
                    if (phone.isNotBlank()) {
                        NearbyPlacesHelper.makePhoneCall(context, phone)
                        return@withContext "📞 Arama başlatılıyor: $phone"
                    }
                    return@withContext "Telefon numarası bulunamadı."
                }

                else -> "Bilinmeyen eylem türü."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Eylem gerçekleştirilemedi: ${e.localizedMessage}"
        }
    }
}
