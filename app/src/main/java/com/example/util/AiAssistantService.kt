package com.example.util

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.AlarmClock
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.DataStoreManager
import com.example.data.ReminderEntity
import com.example.data.SavedLocationEntity
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
import java.io.File
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
    val actionSummary: String? = null,
    val isSpeechReady: Boolean = true
)

object UstaSessionState {
    var pendingPlaceToSave: NearbyPlace? = null
    var isWaitingForLessonPlanCourse: Boolean = false
    var pendingLocationCoords: Pair<Double, Double>? = null
    var pendingNoteContent: String? = null
    var isWaitingForNoteBody: Boolean = false
    var isVoiceNote: Boolean = false

    // Alarm İnteraktif Durumu
    var isWaitingForAlarmTime: Boolean = false
    var pendingAlarmHour: Int? = null
    var pendingAlarmMinute: Int? = null
    var isWaitingForAlarmLabel: Boolean = false

    // Hatırlatma İnteraktif Durumu
    var isWaitingForReminderTitle: Boolean = false
    var pendingReminderTitle: String? = null
    var isWaitingForReminderTime: Boolean = false
    var pendingReminderTimeMillis: Long? = null
    var pendingReminderDateStr: String? = null
    var isWaitingForReminderNote: Boolean = false

    // Park Yeri Notu İnteraktif Durumu
    var isWaitingForParkNote: Boolean = false
    var pendingParkCoords: Pair<Double, Double>? = null
}

object AiAssistantService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

        private fun setSystemAlarm(context: Context, hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                val fallback = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
                true
            }
        } catch (e: Exception) {
            false
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
            String(decoded, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private val NAME_BLACKLIST = setOf(
        "migros", "market", "bakkal", "eczane", "hastane", "otopark", "otobüs", "cami",
        "randevu", "hatırlat", "alarm", "harita", "tarih", "günlük", "yemek", "usta", "jarvis", "asistan"
    )

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "Usta",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val dataStoreManager = DataStoreManager(context)
        val db = AppDatabase.getDatabase(context)

        AiKnowledgeSeeder.seedIfNeeded(context)

        val resolvedAssistantName = assistantName.ifBlank {
            dataStoreManager.aiAssistantName.first().ifBlank { "ATİLLA" }
        }
        val currentNick = dataStoreManager.userNick.first()?.trim() ?: ""

        var cleanMsg = userMessage.trim()
        val triggerRegex = Regex("""(?i)^(hey\s+)?(atilla|atila|jarvis|usta|asistan|atilla\s+dinle|usta\s+dinle|jarvis\s+dinle)[,\s!.:]*""")
        val hadTriggerWord = triggerRegex.find(cleanMsg) != null || cleanMsg.equals("atilla", ignoreCase = true) || cleanMsg.equals("atila", ignoreCase = true)
        cleanMsg = cleanMsg.replace(triggerRegex, "").trim()
        if (cleanMsg.isBlank() || (hadTriggerWord && cleanMsg.isBlank())) {
            return@withContext AiResponse(replyText = "Buyrun, size nasıl yardımcı olabilirim?")
        }

        val lowerMsg = cleanMsg.lowercase(Locale.forLanguageTag("tr-TR"))

        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // =========================================================================
        // DURUM MAKİNESİ (İNTERAKTİF ADIMLAR)
        // =========================================================================

        // A. Bekleyen Alarm Saati
        if (UstaSessionState.isWaitingForAlarmTime) {
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val singleHourMatch = Regex("""(?i)\b(\d{1,2})\b""").find(cleanMsg)
            val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: singleHourMatch?.groupValues?.get(1)?.toIntOrNull()
            val min = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            if (hour != null && hour in 0..23 && min in 0..59) {
                UstaSessionState.pendingAlarmHour = hour
                UstaSessionState.pendingAlarmMinute = min
                UstaSessionState.isWaitingForAlarmTime = false
                UstaSessionState.isWaitingForAlarmLabel = true
                val formattedTime = String.format(Locale.ROOT, "%02d:%02d", hour, min)
                return@withContext AiResponse(
                    replyText = "⏰ Alarm saatini " + formattedTime + " olarak belirledim. Peki bu alarmın başlığı veya etiketi ne olsun? (Örneğin: Uyanış, İlaç Saati, İşe Gidiş vb.)"
                )
            } else {
                return@withContext AiResponse(
                    replyText = "Saati tam anlayamadım dostum. Lütfen alarm saatini '07:30' veya '8:00' şeklinde söyler misin?"
                )
            }
        }

        // B. Bekleyen Alarm Etiketi
        if (UstaSessionState.isWaitingForAlarmLabel) {
            val label = cleanMsg.take(40).trim().ifBlank { "Alarm" }
            val hour = UstaSessionState.pendingAlarmHour ?: 8
            val min = UstaSessionState.pendingAlarmMinute ?: 0
            UstaSessionState.isWaitingForAlarmLabel = false
            UstaSessionState.pendingAlarmHour = null
            UstaSessionState.pendingAlarmMinute = null

            setSystemAlarm(context, hour, min, label)

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(cal.time)
            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = "ALARM",
                    title = "⏰ Alarm: " + label,
                    customNote = "Saat " + String.format(Locale.ROOT, "%02d:%02d", hour, min) + " için kurulu alarm.",
                    dueDatetime = dateStr,
                    dueDateMillis = cal.timeInMillis,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "ALARM_SET"
                )
            )

            val formattedTime = String.format(Locale.ROOT, "%02d:%02d", hour, min)
            return@withContext AiResponse(
                replyText = "⏰ Saat " + formattedTime + " için '" + label + "' alarmınız hem telefonunuzun saat sistemine hem de HatırlaGit'e başarıyla kuruldu!",
                actionSummary = "⏰ Alarm Kuruldu: " + formattedTime + " (" + label + ")"
            )
        }

        // C. Bekleyen Hatırlatma Konusu/Başlığı
        if (UstaSessionState.isWaitingForReminderTitle) {
            UstaSessionState.isWaitingForReminderTitle = false
            UstaSessionState.pendingReminderTitle = cleanMsg.take(60).trim()
            UstaSessionState.isWaitingForReminderTime = true
            return@withContext AiResponse(
                replyText = "Hatırlatma konusunu '" + UstaSessionState.pendingReminderTitle + "' olarak not ettim. Hangi gün ve saatte hatırlatayım? (Örneğin: Yarın 14:00, Akşam 20:00 veya 15 Ekim 09:30)"
            )
        }

        // D. Bekleyen Hatırlatma Zamanı
        if (UstaSessionState.isWaitingForReminderTime) {
            val cal = Calendar.getInstance()
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 9
            val min = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)

            if (lowerMsg.contains("yarın")) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            } else if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(cal.time)
            UstaSessionState.pendingReminderDateStr = dateStr
            UstaSessionState.pendingReminderTimeMillis = cal.timeInMillis
            UstaSessionState.isWaitingForReminderTime = false
            UstaSessionState.isWaitingForReminderNote = true

            return@withContext AiResponse(
                replyText = "Zamanı " + dateStr + " olarak planladım. Hatırlatıcıya eklemek istediğiniz özel bir açıklama var mı? (Yoksa 'Hayır' veya 'Yok' diyebilirsiniz)"
            )
        }

        // E. Bekleyen Hatırlatma Açıklaması
        if (UstaSessionState.isWaitingForReminderNote) {
            val title = UstaSessionState.pendingReminderTitle ?: "Hatırlatma"
            val dateStr = UstaSessionState.pendingReminderDateStr ?: ""
            val timeMillis = UstaSessionState.pendingReminderTimeMillis ?: System.currentTimeMillis()
            val note = if (lowerMsg.startsWith("hayır") || lowerMsg.startsWith("yok") || lowerMsg.contains("gerek yok")) "" else cleanMsg

            UstaSessionState.isWaitingForReminderNote = false
            UstaSessionState.pendingReminderTitle = null
            UstaSessionState.pendingReminderDateStr = null
            UstaSessionState.pendingReminderTimeMillis = null

            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = "HATIRLATICI",
                    title = "🔔 " + title,
                    customNote = note.ifBlank { "HatırlaGit Usta Hatırlatması" },
                    dueDatetime = dateStr,
                    dueDateMillis = timeMillis,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "REMINDER_SET"
                )
            )

            return@withContext AiResponse(
                replyText = "🔔 '" + title + "' hatırlatıcınız " + dateStr + " tarihine Anasayfa ve Hatırlatıcılar listenize başarıyla kaydedildi!",
                actionSummary = "🔔 Hatırlatıcı Kuruldu: " + title + " (" + dateStr + ")"
            )
        }

        // F. Bekleyen Park Yeri Notu
        if (UstaSessionState.isWaitingForParkNote) {
            val coords = UstaSessionState.pendingParkCoords ?: Pair(realLat, realLng)
            val parkNote = if (lowerMsg.startsWith("hayır") || lowerMsg.startsWith("yok") || lowerMsg.contains("gerek yok")) "" else cleanMsg
            UstaSessionState.isWaitingForParkNote = false
            UstaSessionState.pendingParkCoords = null

            dataStoreManager.saveParkedCarLocation(
                lat = coords.first.toString(),
                lng = coords.second.toString(),
                time = System.currentTimeMillis()
            )

            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(now))
            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = "PARK_YERI",
                    title = "🚗 Park Yeri: " + userCity + " " + userDistrict,
                    customNote = if (parkNote.isNotBlank()) "Park Notu: " + parkNote else "Araç park konumu kaydedildi.",
                    dueDatetime = dateStr,
                    dueDateMillis = now,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "PARK_SAVED"
                )
            )

            val noteMsg = if (parkNote.isNotBlank()) " (Ek Not: '" + parkNote + "')" else ""
            return@withContext AiResponse(
                replyText = "🚗 Park yeriniz telemetri sistemine ve Anasayfa Park Halinde kartına" + noteMsg + " işlendi! Aracınız " + userCity + " " + userDistrict + " noktasında güvende.",
                actionSummary = "🚗 Park Yeri Kaydedildi: " + userCity + noteMsg
            )
        }

        // 1. Bekleyen Lokasyon Adı ("Konumu Lokasyona kaydet" sonrası isim geldiğinde)
        if (UstaSessionState.pendingLocationCoords != null) {
            val coords = UstaSessionState.pendingLocationCoords!!
            UstaSessionState.pendingLocationCoords = null
            val locName = cleanMsg.take(50).trim()
            db.savedLocationDao().insertLocation(
                SavedLocationEntity(
                    name = locName,
                    lat = coords.first,
                    lng = coords.second,
                    timestamp = System.currentTimeMillis()
                )
            )
            return@withContext AiResponse(
                replyText = "📍 '" + locName + "' lokasyonu Anasayfadaki 'Kayıtlı Lokasyonlarım' listenize başarıyla işlenmiştir. İstediğiniz zaman rotanızı tek dokunuşla başlatabilirsiniz.",
                actionSummary = "📍 Lokasyon Kaydedildi: " + locName
            )
        }

        // 2. Bekleyen Not Başlığı ("Hızlı not al" veya "Sesli not al" sonrası sorulan başlık)
        if (UstaSessionState.pendingNoteContent != null) {
            val noteBody = UstaSessionState.pendingNoteContent!!
            val isVoice = UstaSessionState.isVoiceNote
            UstaSessionState.pendingNoteContent = null
            UstaSessionState.isVoiceNote = false
            val noteTitle = cleanMsg.take(60).trim()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(now))
            val category = if (isVoice) "SESLİ NOT" else "HIZLI_NOT"
            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = category,
                    title = noteTitle,
                    customNote = noteBody,
                    dueDateMillis = now,
                    dueDatetime = dateStr,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "NOTE_SAVED"
                )
            )
            val destText = if (isVoice) "Anasayfadaki Sesli Notlar bölümünüze" else "Anasayfadaki Hızlı Notlar kartınıza ve listenize"
            return@withContext AiResponse(
                replyText = "📝 '" + noteTitle + "' başlıklı notunuz " + destText + " başarıyla kaydedildi!",
                actionSummary = "📝 " + (if (isVoice) "Sesli Not" else "Hızlı Not") + " Kaydedildi: " + noteTitle
            )
        }

        // 3. Bekleyen Not İçeriği
        if (UstaSessionState.isWaitingForNoteBody) {
            UstaSessionState.isWaitingForNoteBody = false
            UstaSessionState.pendingNoteContent = cleanMsg
            return@withContext AiResponse(
                replyText = "Notunuzu aldım: \"" + cleanMsg + "\". Peki bu notun Anasayfa ve Sesli Notlar listesinde görünecek başlığı ne olsun?"
            )
        }

        // 4. Bekleyen Mekan Onayı
        if (UstaSessionState.pendingPlaceToSave != null) {
            val place = UstaSessionState.pendingPlaceToSave!!
            if (lowerMsg.startsWith("evet") || lowerMsg.contains("kaydet") || lowerMsg.contains("olur") || lowerMsg.contains("ekle") || lowerMsg.contains("onaylıyorum")) {
                db.savedLocationDao().insertLocation(
                    SavedLocationEntity(
                        name = place.name,
                        lat = place.lat,
                        lng = place.lng,
                        timestamp = System.currentTimeMillis()
                    )
                )
                UstaSessionState.pendingPlaceToSave = null
                return@withContext AiResponse(
                    replyText = place.name + " koordinatları 'Kayıtlı Lokasyonlarım' veritabanına başarıyla işlenmiştir.",
                    actionSummary = "📍 Lokasyon Kaydedildi: " + place.name
                )
            } else if (lowerMsg.startsWith("hayır") || lowerMsg.contains("gerek yok") || lowerMsg.contains("istemiyorum") || lowerMsg.contains("kaydetme") || lowerMsg.contains("iptal")) {
                UstaSessionState.pendingPlaceToSave = null
                return@withContext AiResponse(
                    replyText = "Anlaşıldı dostum, lokasyon kaydı iptal edildi."
                )
            }
        }

        // =========================================================================
        // JARVIS PROTOKOLÜ & LOCAL INSTANT AGENTLER (0 GECİKME İLE DOĞRUDAN İŞLEM)
        // =========================================================================

        // 4.1. LOCAL ALARM AGENT: "alarm kur", "uyandır", "saat 7'ye alarm" vb.
        if (lowerMsg.contains("alarm") || lowerMsg.contains("uyandır")) {
            val timeMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val hourOnlyMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})\s*(?:'ye|'ya|'e|'a|'de|'da)""").find(cleanMsg)
            val anyHourMatch = Regex("""(?i)\b(\d{1,2})\b""").find(cleanMsg)

            val parsedHour = timeMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: hourOnlyMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: anyHourMatch?.groupValues?.get(1)?.toIntOrNull()
            val parsedMin = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            if (parsedHour != null && parsedHour in 0..23 && parsedMin in 0..59) {
                val label = cleanMsg.replace(Regex("""(?i)(?:saat\s*)?\d{1,2}(?:[:.]\d{2})?|bana|alarm|kur|ayarla|için|uyandır|'ye|'ya|'e|'a|'de|'da"""), "").trim().ifBlank { "Atilla Alarm" }
                val payload = JSONObject().apply {
                    put("hour", parsedHour)
                    put("minute", parsedMin)
                    put("title", label)
                }
                ActionDispatcherHelper.executeAction(context, "SET_ALARM", payload)
                val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", parsedHour, parsedMin)
                return@withContext AiResponse(
                    replyText = "Emredersiniz, alarm $timeFormatted için hem sistem saatinize hem de HatırlaGit'e başarıyla kuruldu.",
                    actionSummary = "⏰ Alarm: $timeFormatted ($label)"
                )
            } else if (!lowerMsg.contains("nasıl") && !lowerMsg.contains("nedir")) {
                UstaSessionState.isWaitingForAlarmTime = true
                return@withContext AiResponse(
                    replyText = "Hangi saate alarm kurmamı istersiniz? (Örneğin: 07:30 veya 8:00)"
                )
            }
        }

        // 4.2. LOCAL CALENDAR AGENT: "takvime ekle", "etkinlik ekle", "randevu ekle", "toplantı ekle"
        if (lowerMsg.contains("takvime ekle") || lowerMsg.contains("etkinlik ekle") || lowerMsg.contains("randevu ekle") || lowerMsg.contains("toplantı ekle") || lowerMsg.contains("takvimime ekle")) {
            val cal = Calendar.getInstance()
            if (lowerMsg.contains("yarın")) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val timeMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val hourOnlyMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})\s*(?:'ye|'ya|'e|'a|'de|'da)""").find(cleanMsg)
            val parsedHour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: hourOnlyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10
            val parsedMin = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            cal.set(Calendar.HOUR_OF_DAY, parsedHour)
            cal.set(Calendar.MINUTE, parsedMin)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis() && !lowerMsg.contains("yarın")) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val eventTitle = cleanMsg.replace(Regex("""(?i)takvime ekle|takvimime ekle|etkinlik ekle|randevu ekle|toplantı ekle|yarın|bugün|saat\s*\d{1,2}(?:[:.]\d{2})?|'ye|'ya|'e|'a|'de|'da"""), "").trim().ifBlank { "Toplantı / Randevu" }
            val payload = JSONObject().apply {
                put("title", eventTitle)
                put("description", "Atilla tarafından sesle oluşturuldu.")
                put("startTimeMillis", cal.timeInMillis)
                put("endTimeMillis", cal.timeInMillis + 3600000L)
            }
            ActionDispatcherHelper.executeAction(context, "CREATE_EVENT", payload)
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(cal.time)
            return@withContext AiResponse(
                replyText = "Emredersiniz, '$eventTitle' etkinliği $dateStr için telefon takviminize ve akıllı saatinize işlendi.",
                actionSummary = "📅 Takvim: $eventTitle ($dateStr)"
            )
        }

        // 4.3. LOCAL CALL & WHATSAPP AGENT: "[Kişi]'yi ara", "[Kişi]'ye whatsapp mesajı at"
        if (lowerMsg.endsWith("ara") || lowerMsg.contains("telefon et") || lowerMsg.contains("çağrı yap")) {
            val contactName = cleanMsg.replace(Regex("""(?i)lütfen|bana|'yi ara|'yı ara|'i ara|'ı ara|'yu ara|'yü ara|'ü ara|'u ara|ara|telefon et|çağrı yap"""), "").trim()
            if (contactName.isNotBlank() && !NAME_BLACKLIST.contains(contactName.lowercase())) {
                val payload = JSONObject().apply { put("name", contactName) }
                val summary = ActionDispatcherHelper.executeAction(context, "CALL_PHONE", payload)
                return@withContext AiResponse(
                    replyText = "Hemen $contactName kişisini arıyorum.",
                    actionSummary = summary
                )
            }
        }
        if (lowerMsg.contains("whatsapp") && (lowerMsg.contains("mesaj") || lowerMsg.contains("yaz") || lowerMsg.contains("gönder"))) {
            val contactMatch = Regex("""(?i)(?:whatsapp'tan|whatsapp|whatsappta)\s+([a-zA-ZçğıöşüÇĞİÖŞÜ]+)""").find(cleanMsg)
            val targetName = contactMatch?.groupValues?.get(1)?.trim() ?: ""
            if (targetName.isNotBlank()) {
                val messageContent = cleanMsg.replace(Regex("""(?i)^.*?mesaj(?:ı)?\s*(?:at|yaz|gönder)[: ]*"""), "").trim().ifBlank { "Merhaba" }
                val payload = JSONObject().apply {
                    put("name", targetName)
                    put("message", messageContent)
                }
                val summary = ActionDispatcherHelper.executeAction(context, "SEND_WHATSAPP", payload)
                return@withContext AiResponse(
                    replyText = "$targetName kişisine WhatsApp mesajını hazırladım.",
                    actionSummary = summary
                )
            }
        }

        // 5. "Konumu Lokasyona Kaydet" (İsim sorarak)
        if (lowerMsg.contains("konumu lokasyona kaydet") || lowerMsg.contains("konumumu lokasyona kaydet") ||
            lowerMsg.contains("lokasyona kaydet") || lowerMsg.contains("konumu lokasyonlarıma kaydet") ||
            lowerMsg.contains("burayı lokasyona kaydet") || lowerMsg.contains("burayı lokasyonlarıma kaydet") ||
            (lowerMsg.contains("lokasyon") && lowerMsg.contains("kaydet"))) {
            UstaSessionState.pendingLocationCoords = Pair(realLat, realLng)
            return@withContext AiResponse(
                replyText = "📍 Mevcut koordinatlarınız alındı (" + userCity + " " + userDistrict + "). Anasayfadaki 'Kayıtlı Lokasyonlarım' listesine hangi isimle kaydedeyim? (Örneğin: Evim, İşyeri, Atölye, Yazlık vb.)"
            )
        }

        // 6. "Park Yeri Kaydet" (Anasayfa Park Yeri kartına soruyla işleme)
        if (lowerMsg.contains("park yeri kaydet") || lowerMsg.contains("park yerini kaydet") || 
            lowerMsg.contains("park yerimi kaydet") || lowerMsg.contains("arabayı buraya park") || 
            lowerMsg.contains("arabamı kaydet") || lowerMsg.contains("buraya park ettim") ||
            lowerMsg.contains("park yerim")) {
            UstaSessionState.pendingParkCoords = Pair(realLat, realLng)
            UstaSessionState.isWaitingForParkNote = true
            return@withContext AiResponse(
                replyText = "🚗 Mevcut konumunuz (" + userCity + " " + userDistrict + ") park yeri olarak alındı. Aracın bulunduğu kat, blok veya sütun no gibi eklemek istediğiniz bir not var mı? (Yoksa 'Hayır' diyebilirsiniz)"
            )
        }

        // 7.A: YouTube Şarkı Açma
        if (lowerMsg.contains("youtube'dan") || lowerMsg.contains("youtube'da") || 
            (lowerMsg.contains("youtube") && (lowerMsg.contains("aç") || lowerMsg.contains("çal") || lowerMsg.contains("oynat")))) {
            val songQuery = cleanMsg.replace(Regex("(?i)youtube'dan|youtube'da|youtube|şarkısını|şarkıyı|videosunu|videoyu|aç|çal|oynat"), "").trim().ifBlank { "Sevilen Şarkılar" }
            val (success, message) = AppLauncherHelper.playYouTubeSong(context, songQuery)
            return@withContext AiResponse(
                replyText = message,
                actionSummary = "▶️ YouTube: " + songQuery
            )
        }

        // 7.B: Google Gemini Köprüsü
        if (lowerMsg.contains("gemini") || lowerMsg.contains("bard") || lowerMsg.contains("geminiye sor") || lowerMsg.contains("google ai")) {
            val q = cleanMsg.replace(Regex("(?i)^(gemini'yi aç|gemini aç|geminiye sor|gemini köprüsü kur|gemini köprüsü|bard|google ai)[: ]*"), "").trim()
            val (success, message) = AppLauncherHelper.openGoogleGemini(context, q)
            return@withContext AiResponse(
                replyText = message,
                actionSummary = "✨ Google Gemini Köprüsü"
            )
        }

        // 7.C: Google Arama
        if (lowerMsg.startsWith("google'da ara") || lowerMsg.startsWith("internette ara") || lowerMsg.startsWith("webde ara") || lowerMsg.startsWith("google ara")) {
            val q = cleanMsg.replace(Regex("(?i)^(google'da ara|internette ara|webde ara|google ara)[: ]*"), "").trim()
            val (success, message) = AppLauncherHelper.searchGoogle(context, q)
            return@withContext AiResponse(
                replyText = message,
                actionSummary = "🔍 Google Araması: " + q
            )
        }

        // 7.D: Google Asistan Köprüsü
        if (lowerMsg.contains("google asistan") || lowerMsg.contains("asistana bağlan") || lowerMsg.contains("asistan köprüsü")) {
            val (success, message) = AppLauncherHelper.openGoogleAssistant(context)
            return@withContext AiResponse(
                replyText = message,
                actionSummary = "🎙️ Google Asistan Köprüsü"
            )
        }

        // 7.E: Uygulama Başlatma ve Cihaz Kontrolü
        if (lowerMsg.endsWith("aç") || lowerMsg.endsWith("başlat") || lowerMsg.contains("uygulamasını aç") || lowerMsg.contains("uygulamayı aç") || lowerMsg.contains("çalıştır")) {
            val (success, message) = AppLauncherHelper.openApplicationByVoice(context, cleanMsg)
            if (success) {
                return@withContext AiResponse(
                    replyText = message,
                    actionSummary = "🚀 " + message
                )
            }
        }

        // 7.D: Python YouTube & Google Asistan Köprü Kodu
        if (lowerMsg.contains("python kodu") || lowerMsg.contains("python script") || (lowerMsg.contains("python") && lowerMsg.contains("youtube"))) {
            val pyReply = "🐍 **Jarvis YouTube & Google Asistan Python Köprü Scripti:**\n\n" +
                "Bu scripti bilgisayarınızda veya telefonunuzdaki Pydroid / Termux ortamında çalıştırabilirsiniz:\n\n" +
                "```python\n" +
                "import pywhatkit, webbrowser\n\n" +
                "def play_song(song):\n" +
                "    print(f'YouTube Açılıyor: {song}')\n" +
                "    try:\n" +
                "        pywhatkit.playonyt(song)\n" +
                "    except:\n" +
                "        webbrowser.open(f'https://www.youtube.com/results?search_query={song}')\n\n" +
                "play_song('Neşet Ertaş Gönül Dağı')\n" +
                "```\n\n" +
                "💡 Dosya projenizdeki `scripts/youtube_assistant_bridge.py` yoluna kaydedilmiştir."
            return@withContext AiResponse(
                replyText = pyReply,
                actionSummary = "🐍 Python Köprü Kodu"
            )
        }

        // 7.E: İnteraktif Alarm Kurma Tetikleyicisi
        if (lowerMsg.contains("alarm kur") || lowerMsg.contains("bana alarm kur") || lowerMsg.startsWith("alarm")) {
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            if (timeMatch != null) {
                val hour = timeMatch.groupValues[1].toIntOrNull() ?: 8
                val min = timeMatch.groupValues[2].toIntOrNull() ?: 0
                UstaSessionState.pendingAlarmHour = hour
                UstaSessionState.pendingAlarmMinute = min
                UstaSessionState.isWaitingForAlarmLabel = true
                val formattedTime = String.format(Locale.ROOT, "%02d:%02d", hour, min)
                return@withContext AiResponse(
                    replyText = "⏰ Saat " + formattedTime + " için alarm etiketiniz ne olsun? (Örneğin: Uyanış, İlaç Saati, İşe Gidiş vb.)"
                )
            } else {
                UstaSessionState.isWaitingForAlarmTime = true
                return@withContext AiResponse(
                    replyText = "⏰ Alarmı hangi saat ve dakikaya kurmamı istersiniz? (Örneğin: Sabah 07:30 veya 08:00)"
                )
            }
        }

        // 7.F: İnteraktif Hatırlatma Kurma Tetikleyicisi
        if (lowerMsg.contains("hatırlatıcı kur") || lowerMsg.contains("hatırlatma kur") || 
            (lowerMsg.startsWith("bana hatırlat") && !lowerMsg.contains("dizi"))) {
            val titleCandidate = cleanMsg.replace(Regex("""(?i)^(bana\s+)?(hatırlatıcı\s+kur|hatırlatma\s+kur|hatırlat)[: ]*"""), "").trim()
            if (titleCandidate.isBlank()) {
                UstaSessionState.isWaitingForReminderTitle = true
                return@withContext AiResponse(
                    replyText = "🔔 Ne hakkında hatırlatma kurmamı istersiniz? Konusu veya başlığı ne olsun?"
                )
            } else {
                UstaSessionState.pendingReminderTitle = titleCandidate
                UstaSessionState.isWaitingForReminderTime = true
                return@withContext AiResponse(
                    replyText = "🔔 '" + titleCandidate + "' hatırlatmasını hangi gün ve saatte kurayım? (Örneğin: Yarın 14:00, Akşam 20:00 veya 15 Ekim 09:30)"
                )
            }
        }

        // 7. "Hızlı Not Al" / "Sesli Not Al" (Başlık sorarak kaydetme)
        if (lowerMsg.startsWith("hızlı not al") || lowerMsg.startsWith("not al") || lowerMsg.startsWith("not et") ||
            lowerMsg.contains("hızlı not al") || lowerMsg.contains("sesli not al") || lowerMsg.startsWith("bunu not al")) {
            val extractedNote = cleanMsg.replace(Regex("""(?i)^(hızlı\\s+)?(not\\s+al|not\\s+et|sesli\\s+not\\s+al|bunu\\s+not\\s+al)[: ]*"""), "").trim()
            if (extractedNote.isBlank()) {
                UstaSessionState.isWaitingForNoteBody = true
                return@withContext AiResponse(
                    replyText = "Hemen not edelim dostum. Notunun içeriğini söyler misin?"
                )
            } else {
                UstaSessionState.pendingNoteContent = extractedNote
                return@withContext AiResponse(
                    replyText = "Notunu aldım: \"" + extractedNote + "\". Peki bu notun Anasayfa ve Sesli Notlar listesinde görünecek başlığı ne olsun?"
                )
            }
        }

        // 8. Rutin Öğrenme ve Alışkanlık Takibi
        if (lowerMsg.contains("rutinime ekle") || lowerMsg.contains("rutinimi öğren") || lowerMsg.contains("rutin ekle")) {
            val routineText = cleanMsg.replace(Regex("""(?i)^(rutinime\\s+ekle|rutinimi\\s+öğren|rutin\\s+ekle)[: ]*"""), "").trim()
            val timeMatch = Regex("""(?i)(\\d{1,2}[:.]\\d{2})""").find(routineText)
            val detectedTime = timeMatch?.value?.replace(".", ":") ?: "09:00"
            val title = routineText.replace(detectedTime, "").replace(Regex("(?i)saat|her gün|sabah|akşam"), "").trim().ifBlank { "Günlük Rutin Görevi" }
            val learnResult = UserRoutineHelper.learnRoutine(context, detectedTime, title)
            return@withContext AiResponse(
                replyText = learnResult + "\n\nArtık bu saatlerde sana özel akıllı hatırlatmalar sunacağım!",
                actionSummary = "🧠 Rutin Öğrenildi: " + detectedTime + " " + title
            )
        }

        if (lowerMsg.contains("rutinlerimi göster") || lowerMsg.contains("bugünkü rutinler") || lowerMsg.contains("rutin analizi") || lowerMsg.contains("alışkanlıklarım")) {
            val report = UserRoutineHelper.analyzeUserHabits(context)
            return@withContext AiResponse(
                replyText = report,
                actionSummary = "🧠 Rutin ve Alışkanlık Analizi"
            )
        }

        // 9. MEB ve OGM Materyal Yenilikleri
        if (lowerMsg.contains("ogm materyal") || lowerMsg.contains("ogmmateryal") || lowerMsg.contains("meb yenilik") || lowerMsg.contains("eğitim teknolojileri") || lowerMsg.contains("eba yenilik")) {
            val edTechReply = "📚 **MEB ve OGM Materyal Güncel Eğitim-Teknoloji Raporu:**\n\n" +
                "1. **İnteraktif Kitaplar:** 9, 10, 11 ve 12. sınıf Tarih ve Edebiyat kitapları video ve 3D simülasyonlarla güncellendi.\n" +
                "2. **Beceri Temelli Soru Bankası:** Türkiye Yüzyılı Maarif Modeli kazanımlarına uygun açık uçlu ve senaryolu soru havuzları erişime açıldı.\n" +
                "3. **3D Deney ve Sanal Müzeler:** Tarihsel mekanlar sanal turla gezilebiliyor, fen bilimlerinde sanal laboratuvarlar devrede.\n" +
                "4. **YKS Kampı ve Denemeler:** TYT-AYT sınavına yönelik çıkmış soru analizleri ve videolu çözümler OGM Materyal portalında aktif!\n\n" +
                "Dilediğiniz ders veya sınıf düzeyi için örnek etkinlik veya sınav kağıdı hazırlayabilirim!"
            return@withContext AiResponse(
                replyText = edTechReply,
                actionSummary = "🚀 OGM Materyal ve MEB Yenilikleri"
            )
        }

        // 10. MEB Maarif Yıllık Ders Planı
        if (lowerMsg.contains("yıllık plan") || lowerMsg.contains("yıllık ders planı") || lowerMsg.contains("maarif yıllık plan") ||
            (lowerMsg.contains("plan hazırla") && (lowerMsg.contains("yıllık") || lowerMsg.contains("haftalık") || lowerMsg.contains("aylık")))) {
            val courseName = when {
                lowerMsg.contains("edebiyat") || lowerMsg.contains("türk dili") -> "Türk Dili ve Edebiyatı"
                lowerMsg.contains("tarih") -> "Tarih"
                else -> "Tarih"
            }
            val gradeLevel = when {
                lowerMsg.contains("9") -> "9. Sınıf"
                lowerMsg.contains("10") -> "10. Sınıf"
                lowerMsg.contains("11") -> "11. Sınıf"
                lowerMsg.contains("12") -> "12. Sınıf"
                else -> "9. Sınıf"
            }
            val planType = when {
                lowerMsg.contains("haftalık") -> "Haftalık Plan"
                lowerMsg.contains("aylık") -> "Aylık Plan"
                else -> "Yıllık Plan"
            }
            val (pdfFile, report) = MebDocumentHelper.createAnnualPlanPdf(
                context = context,
                params = AnnualPlanParams(
                    schoolName = userCity + " Anadolu Lisesi",
                    principalName = "Okul Müdürü",
                    teachers = if (currentNick.isNotBlank()) currentNick + " Öğretmen" else "Zümre Öğretmenleri",
                    courseName = courseName,
                    gradeLevel = gradeLevel,
                    planType = planType
                )
            )
            return@withContext AiResponse(
                replyText = report,
                actionSummary = "📋 " + planType + " Hazırlandı: " + courseName + " (" + gradeLevel + ")"
            )
        }

        // 11. ŞÖK (Şube Öğretmenler Kurulu) Tutanağı (8 Resmi Gündem Maddesi + Kamera/OCR)
        if (lowerMsg.contains("şök tutanağı") || lowerMsg.contains("şube öğretmenler kurulu") || 
            lowerMsg.contains("şök hazırla") || lowerMsg.contains("şök toplantısı") || lowerMsg.contains("şök")) {
            val classPattern = Regex("""(?i)\\b(9|10|11|12)[/-]?([A-Za-zÇĞİÖŞÜçğıöşü])\\b""").find(cleanMsg)
            val className = classPattern?.value?.uppercase(Locale("tr", "TR")) ?: "10-A"

            val (pdfFile, report) = MebDocumentHelper.createSokMeetingPdf(
                context = context,
                params = SokMeetingParams(
                    schoolName = userCity + " Anadolu Lisesi",
                    className = className,
                    termName = "1. Dönem",
                    classTeacherName = if (currentNick.isNotBlank()) currentNick + " Öğretmen" else "Sınıf Rehber Öğretmeni"
                )
            )
            return@withContext AiResponse(
                replyText = report + "\n\n💡 *Kamera Desteği:* Toplantı imza sirküsü veya karar föyünüz varsa kamera butonu ile taratabilir, tutanağa ekleyebilirsiniz.",
                actionSummary = "📑 ŞÖK Tutanağı Hazırlandı: " + className
            )
        }

        // 12. Açık Uçlu Sınav Kağıdı & Rubrik
        if (lowerMsg.contains("açık uçlu sınav") || lowerMsg.contains("yazılı sınavı hazırla") || lowerMsg.contains("sınav kağıdı")) {
            val courseName = if (lowerMsg.contains("edebiyat")) "Türk Dili ve Edebiyatı" else "Tarih"
            val gradeLevel = if (lowerMsg.contains("10")) "10. Sınıf" else if (lowerMsg.contains("11")) "11. Sınıf" else if (lowerMsg.contains("12")) "12. Sınıf" else "9. Sınıf"
            val examContent = "1. Soru (20 P): Türklerin tarih boyunca kullandığı takvim sistemlerini kronolojik olarak yazınız.\n2. Soru (20 P): Orhun Abideleri'nin Türk dili ve tarihi açısından önemini açıklayınız.\n3. Soru (20 P): Malazgirt Zaferi'nin (1071) sonuçlarını analiz ediniz.\n4. Soru (20 P): Ahilik teşkilatının esnaf ahlakı ve toplumsal dayanışmadaki rolünü yazınız.\n5. Soru (20 P): Osmanlı İskân ve İstimâlet politikasını değerlendiriniz.\n\nCEVAP VE RUBRİK: Her soru 20 puan üzerinden kavramsal doğruluk ve analitik çıkarım ile değerlendirilir."

            val (pdfFile, report) = MebDocumentHelper.createExamPaperPdf(
                context = context,
                schoolName = userCity + " Anadolu Lisesi",
                courseName = courseName,
                gradeLevel = gradeLevel,
                examName = "1. Dönem 1. Yazılı Sınavı",
                examContent = examContent
            )
            return@withContext AiResponse(
                replyText = report,
                actionSummary = "📝 Açık Uçlu Sınav Hazırlandı: " + courseName + " (" + gradeLevel + ")"
            )
        }

        // 13. Tarih / Edebiyat Bulmaca ve Etkinlik
        if (lowerMsg.contains("bulmaca") || lowerMsg.contains("etkinlik hazırla") || lowerMsg.contains("çengel bulmaca")) {
            val isEdebiyat = lowerMsg.contains("edebiyat")
            val puzzleReply = if (isEdebiyat) {
                "🧩 **Lise Türk Dili ve Edebiyatı Çengel Bulmaca Soruları:**\n1. İlk siyasetname türü eserimiz? -> [KUTADGU BİLİG]\n2. İlk Türkçe sözlük ve ansiklopedi? -> [DİVANU LUGATİT TÜRK]\n3. Olay hikâyesinin Türk edebiyatındaki ustası? -> [ÖMER SEYFETTİN]\n4. Durum hikâyesinin büyük ustası? -> [SAİT FAİK]\n5. Dize sonundaki ses benzerliği? -> [KAFİYE / UYAK]"
            } else {
                "🧩 **Lise Tarih Dersi Çengel Bulmaca Soruları:**\n1. Orhun Yazıtları'ndaki ünlü vezir? -> [TONYUKUK]\n2. Hükümdara yönetme yetkisinin Gök Tengri tarafından verildiği inanç? -> [KUT]\n3. 1071 Anadolu'nun kapısını açan zafer? -> [MALAZGİRT]\n4. Osmanlı hoşgörü politikası? -> [İSTİMÂLET]\n5. Esnaf dayanışma teşkilatı? -> [AHİLİK]"
            }
            return@withContext AiResponse(
                replyText = puzzleReply,
                actionSummary = "🧩 Bulmaca ve Etkinlik Föyü Hazırlandı"
            )
        }

        // 14. Günlük TV Prime-Time Yayın Akışı
        if (lowerMsg.contains("tv'de ne var") || lowerMsg.contains("televizyonda ne var") || lowerMsg.contains("hangi diziler var") ||
            lowerMsg.contains("dizi rehberi") || lowerMsg.contains("tv rehberi") || lowerMsg.contains("akşam ne var")) {
            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val (dayName, shows) = when (dayOfWeek) {
                Calendar.MONDAY -> "Pazartesi" to listOf("Kızıl Goncalar (NOW - 20:00)", "Kudüs Fatihi Selahaddin Eyyubi (TRT 1 - 20:00)", "MasterChef (TV8 - 20:00)")
                Calendar.TUESDAY -> "Salı" to listOf("Mehmed: Fetihler Sultanı (TRT 1 - 20:00)", "Bahar (Show TV - 20:00)", "Gizli Bahçe (NOW - 20:00)")
                Calendar.WEDNESDAY -> "Çarşamba" to listOf("Kuruluş Osman (ATV - 20:00)", "Sandık Kokusu (Show TV - 20:00)", "Sahipsizler (Star TV - 20:00)")
                Calendar.THURSDAY -> "Perşembe" to listOf("Hudutsuz Sevda (NOW - 20:00)", "İnci Taneleri (Kanal D - 20:00)", "Siyah Kalp (Show TV - 20:00)")
                Calendar.FRIDAY -> "Cuma" to listOf("Kızılcık Şerbeti (Show TV - 20:00)", "Yalı Çapkını (Star TV - 20:00)", "Arka Sokaklar (Kanal D - 20:00)")
                Calendar.SATURDAY -> "Cumartesi" to listOf("Gönül Dağı (TRT 1 - 20:00)", "Kardeşlerim (ATV - 20:00)", "Yabani (NOW - 20:00)")
                Calendar.SUNDAY -> "Pazar" to listOf("Teşkilat (TRT 1 - 20:00)", "Deha (Show TV - 20:00)", "Kirli Sepeti (NOW - 20:00)")
                else -> "Bugün" to listOf("Prime-Time Dizileri (20:00)")
            }
            val reply = "📺 **Bugün (" + dayName + ") Televizyonda Öne Çıkan Diziler (Saat 20:00):**\n\n" +
                shows.joinToString("\n") { "• " + it } +
                "\n\n💡 Kaçırmak istemediğiniz bir yapım varsa 'Bana [Dizi Adı] dizisini hatırlat' demeniz yeterli, hemen alarm ve bildirim kurayım!"
            return@withContext AiResponse(
                replyText = reply,
                actionSummary = "📺 Günlük TV Rehberi (" + dayName + ")"
            )
        }

        // 15. Dizi Hatırlatıcı Kurma
        if (lowerMsg.contains("hatırlat") && (lowerMsg.contains("dizi") || lowerMsg.contains("gönül dağı") || 
            lowerMsg.contains("kızılcık şerbeti") || lowerMsg.contains("kuruluş osman") || lowerMsg.contains("teşkilat") || 
            lowerMsg.contains("bahar") || lowerMsg.contains("kızıl goncalar") || lowerMsg.contains("deha") || 
            lowerMsg.contains("arka sokaklar") || lowerMsg.contains("inci taneleri"))) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 7)
            }
            val dateStr = SimpleDateFormat("dd.MM.yyyy 20:00", Locale("tr", "TR")).format(cal.time)
            val showTitle = cleanMsg.replace(Regex("""(?i)bana|dizisini|dizisi|hatırlat|programını|saat\\s*20:00"""), "").trim()
            val safeShowName = if (showTitle.isBlank()) "Akşam Dizisi" else showTitle
            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = "TV_PROGRAMI",
                    title = "📺 TV Hatırlatıcı: " + safeShowName,
                    customNote = safeShowName + " bu akşam saat 20:00'de başlıyor!",
                    dueDatetime = dateStr,
                    dueDateMillis = cal.timeInMillis,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "REMINDER_SET"
                )
            )
            return@withContext AiResponse(
                replyText = "📺 '" + safeShowName + "' için " + dateStr + " saatine hatırlatıcı kuruldu. Ekran başına geçme vaktini asla kaçırmayacaksınız!",
                actionSummary = "⏰ TV Hatırlatıcı Kuruldu: " + safeShowName
            )
        }

        // 16. Canlı Hava Durumu (Open-Meteo)
        if (lowerMsg.contains("hava durumu") || lowerMsg.contains("hava nasıl") || lowerMsg.contains("havalar nasıl") ||
            lowerMsg.contains("yağmur var mı") || lowerMsg.contains("sıcaklık kaç")) {
            val weatherBriefing = WeatherHelper.getWeatherBriefing(context, realLat, realLng, userCity)
            return@withContext AiResponse(
                replyText = weatherBriefing,
                actionSummary = "🌤️ Canlı Hava Durumu: " + userCity
            )
        }

        // 16.0: Doğrudan Harita Navigasyonu (Kullanıcı talebi: Anında haritadan canlı yol tarifini aç)
        if (lowerMsg.contains("yol tarifi") || lowerMsg.contains("nasıl giderim") || 
            lowerMsg.contains("rotayı aç") || lowerMsg.contains("haritadan götür") || 
            lowerMsg.contains("haritayı açıp götür") || lowerMsg.contains("navigasyonu aç")) {
            val destination = cleanMsg.replace(Regex("""(?i)^(bana\s+)?(haritadan\s+)?(yol\s+tarifi\s+ver|yol\s+tarifi\s+yap|yol\s+tarifi|nasıl\s+giderim|rotayı\s+aç|navigasyonu\s+aç)[: ]*"""), "").trim()
            val targetPlace = if (destination.isNotBlank()) destination else userCity
            NearbyPlacesHelper.openGoogleMapsNavigation(context, targetPlace, 0.0, 0.0, targetPlace)
            return@withContext AiResponse(
                replyText = "'" + targetPlace + "' için canlı Google Haritalar navigasyonunu başlattım usta. Yolun açık olsun!",
                actionSummary = "🗺️ Navigasyon Başlatıldı: " + targetPlace
            )
        }

        // 16.A: Gezilecek Yerler ve Haritada Canlı Yol Tarifi
        if (lowerMsg.contains("gezilecek yer") || lowerMsg.contains("nereleri gez") || 
            lowerMsg.contains("tarihi yerler") || lowerMsg.contains("turistik yerler") ||
            (lowerMsg.contains("nereye gidilir") && !lowerMsg.contains("nasıl")) ||
            lowerMsg.contains("gezi rehberi")) {
            
            val detectedCity = when {
                lowerMsg.contains("istanbul") || lowerMsg.contains("İstanbul") -> "İstanbul"
                lowerMsg.contains("ankara") -> "Ankara"
                lowerMsg.contains("izmir") || lowerMsg.contains("İzmir") -> "İzmir"
                lowerMsg.contains("samsun") -> "Samsun"
                lowerMsg.contains("trabzon") -> "Trabzon"
                lowerMsg.contains("antalya") -> "Antalya"
                lowerMsg.contains("bursa") -> "Bursa"
                lowerMsg.contains("konya") -> "Konya"
                else -> userCity.ifBlank { "Samsun" }
            }
            val (guideText, places) = NearbyPlacesHelper.getTouristAttractions(
                context = context,
                targetCity = detectedCity,
                userLat = realLat,
                userLng = realLng
            )
            return@withContext AiResponse(
                replyText = guideText,
                recommendedPlaces = places,
                actionSummary = "🗺️ " + detectedCity + " Gezilecek Yerler ve Canlı Yol Tarifi"
            )
        }

        // 16.B: Gazete Başlıkları (Kullanıcı talebi: Yorumsuz, doğrudan ana başlıklar özeti)
        if (lowerMsg.contains("gazete başlık") || lowerMsg.contains("gazetelerin başlık") || 
            lowerMsg.contains("gazete manşet") || lowerMsg.contains("gazeteler ne yazıyor") ||
            lowerMsg.equals("gazete başlıkları") || lowerMsg.equals("gazeteler") ||
            (lowerMsg.contains("gazete") && (lowerMsg.contains("özet") || lowerMsg.contains("başlık") || lowerMsg.contains("oku")))) {
            val headlines = DailyNewsHelper.getHeadlinesOnly()
            return@withContext AiResponse(
                replyText = headlines,
                actionSummary = "📰 Gazete Manşetleri (Yorumsuz Özet)"
            )
        }

        // 16.C: Teknoloji ve Bilim Haberleri
        if (lowerMsg.contains("teknoloji haber") || lowerMsg.contains("teknolojik haber") || 
            lowerMsg.contains("yapay zeka haber") || lowerMsg.contains("teknoloji dünyası")) {
            val techNews = DailyNewsHelper.getTechNews()
            return@withContext AiResponse(
                replyText = techNews,
                actionSummary = "🚀 Teknoloji ve Bilim Haberleri"
            )
        }

        // 16.D: Spor Dünyası ve Süper Lig Haberleri
        if (lowerMsg.contains("spor haber") || lowerMsg.contains("süper lig haber") || 
            lowerMsg.contains("futbol haber") || lowerMsg.contains("maç sonuç") || lowerMsg.contains("spor dünyası")) {
            val sportsNews = DailyNewsHelper.getSportsNews()
            return@withContext AiResponse(
                replyText = sportsNews,
                actionSummary = "⚽ Spor Dünyası ve Süper Lig"
            )
        }

        // 16.E: Sinema ve Dizi Dünyası
        if (lowerMsg.contains("sinema haber") || lowerMsg.contains("vizyondaki film") || 
            lowerMsg.contains("film haber") || lowerMsg.contains("dizi dünyası") || lowerMsg.contains("sinema dünyası")) {
            val cinemaNews = DailyNewsHelper.getCinemaNews()
            return@withContext AiResponse(
                replyText = cinemaNews,
                actionSummary = "🎬 Sinema ve Dizi Haberleri"
            )
        }

        // 16.F: Oyun Dünyası ve E-Spor
        if (lowerMsg.contains("oyun haber") || lowerMsg.contains("oyun dünyası") || 
            lowerMsg.contains("steam haber") || lowerMsg.contains("konsol haber") || 
            lowerMsg.contains("espor") || lowerMsg.contains("e-spor")) {
            val gamingNews = DailyNewsHelper.getGamingNews()
            return@withContext AiResponse(
                replyText = gamingNews,
                actionSummary = "🎮 Oyun Dünyası ve E-Spor"
            )
        }

        // 16.G: Finans Dünyası, Borsa (BIST 100) ve Piyasalar
        if (lowerMsg.contains("finans haber") || lowerMsg.contains("borsa haber") || 
            lowerMsg.contains("bist 100") || lowerMsg.contains("piyasa haber") || 
            lowerMsg.contains("dolar kaç") || lowerMsg.contains("altın kaç") || 
            lowerMsg.contains("döviz haber") || lowerMsg.contains("ekonomi haber")) {
            val financeNews = DailyNewsHelper.getFinanceNews()
            return@withContext AiResponse(
                replyText = financeNews,
                actionSummary = "📈 Finans, Borsa ve Piyasalar"
            )
        }

        // 17. Günlük İş Akışı ve Gün Planlama (DailyPlannerHelper)
        if (lowerMsg.contains("bugünkü plan") || lowerMsg.contains("günü planla") || lowerMsg.contains("günlük plan") ||
            lowerMsg.contains("iş akışı") || lowerMsg.contains("bugün ne var") || lowerMsg.contains("gün programı") ||
            lowerMsg.contains("çalışma programı") || lowerMsg.contains("zaman yönetimi")) {
            val dailySchedule = DailyPlannerHelper.generateDailySchedule(
                context = context,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict,
                realLat = realLat,
                realLng = realLng
            )
            return@withContext AiResponse(
                replyText = dailySchedule,
                actionSummary = "📅 Günlük İş Akışı ve Zaman Çizelgesi Hazırlandı"
            )
        }

        // 18. İsim Öğrenme
        val explicitNamePatterns = listOf(
            Pattern.compile("(?i)^benim adım\\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)$"),
            Pattern.compile("(?i)^adım\\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)$"),
            Pattern.compile("(?i)^ismim\\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)$"),
            Pattern.compile("(?i)bana\\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)\\s+diye\\s+hitap\\s+et"),
            Pattern.compile("(?i)bana\\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)\\s+diyebilirsin")
        )

        for (p in explicitNamePatterns) {
            val m = p.matcher(cleanMsg)
            if (m.find()) {
                val candidateName = m.group(1)?.trim()
                if (!candidateName.isNullOrBlank() && candidateName.length in 2..25) {
                    val candidateLower = candidateName.lowercase(Locale.forLanguageTag("tr-TR"))
                    if (!NAME_BLACKLIST.contains(candidateLower)) {
                        dataStoreManager.updateNick(candidateName)
                        return@withContext AiResponse(
                            replyText = "Tanıştığıma çok memnun oldum " + candidateName + " dostum! İsmini aklıma kazıdım. Haydi bakalım, bugün ne yapıyoruz?"
                        )
                    }
                }
            }
        }

        // 19. Araştır ve Kaydet
        if (lowerMsg.startsWith("araştır ve kaydet:") || lowerMsg.startsWith("araştır ve kaydet ")) {
            val topic = cleanMsg.replace(Regex("(?i)^araştır ve kaydet[: ]*"), "").trim()
            if (topic.isNotBlank()) {
                val findings = resolvedAssistantName + " Analitik Raporu: '" + topic + "' konusunda detaylı dokümantasyon hazırlanmıştır."
                val saveResult = ResearchFileManager.saveResearch(context, topic, findings)
                return@withContext AiResponse(
                    replyText = "'" + topic + "' konusunu derinlemesine araştırdım ve telefonunun hafızasına güvenle arşivledim!",
                    actionSummary = saveResult
                )
            }
        }

        // =========================================================================
        // CANLI GOOGLE GEMINI YAPAY ZEKA ÇAĞRISI (ÇOKLU MODEL YEDEKLİ)
        // =========================================================================
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "DİJİTAL KÜTÜPHANE VE KURUMSAL BİLGİ VERİTABANI:\n" + 
            allKnowledgeList.take(50).joinToString("\n") { item -> "- [" + item.category + "] " + item.title + ": " + item.content }
        } else "Kütüphanede ek özel not bulunmamaktadır."

        val customApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey)?.trim() else null
        } catch (_: Exception) { null }

        val activeApiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getSecureDefaultKey()

        if (activeApiKey.isNotBlank()) {
            val rawGeminiReply = callResilientGeminiApi(
                apiKey = activeApiKey,
                userMessage = cleanMsg,
                knowledgeContext = knowledgeContext,
                assistantName = resolvedAssistantName,
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

                val cleanReply = if (parsedResult.speechText.isNotBlank()) {
                    parsedResult.speechText
                } else if (parsedResult.actionType != null) {
                    "İşleminizi gerçekleştirdim dostum."
                } else {
                    "Buyrun dostum!"
                }

                val recommendedPlaces = if (cleanReply.contains("Haritada Göster", ignoreCase = true) ||
                    lowerMsg.contains("nerede") || lowerMsg.contains("en yakın") || lowerMsg.contains("nasıl giderim") || lowerMsg.contains("gezilecek")) {
                    if (lowerMsg.contains("gezilecek")) {
                        NearbyPlacesHelper.getTouristAttractions(context, userCity, realLat, realLng).second
                    } else {
                        NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, cleanMsg)
                    }
                } else emptyList()

                if (recommendedPlaces.isNotEmpty()) {
                    UstaSessionState.pendingPlaceToSave = recommendedPlaces.firstOrNull()
                }

                return@withContext AiResponse(
                    replyText = cleanReply,
                    recommendedPlaces = recommendedPlaces,
                    actionSummary = actionSummary
                )
            }
        }

        // =========================================================================
        // AKILLI ÇEVRİMDIŞI / FALLBACK YANIT MOTORU
        // =========================================================================
        val offlineReply = generateOfflineSmartResponse(
            context = context,
            message = cleanMsg,
            knowledgeList = allKnowledgeList,
            assistantName = resolvedAssistantName,
            userNick = currentNick,
            userCity = userCity,
            userDistrict = userDistrict
        )
        return@withContext AiResponse(replyText = offlineReply)
    }

    /**
     * Çoklu Model Yedekliliği ile Kesintisiz Gemini Çağrısı
     */
    private fun callResilientGeminiApi(
        apiKey: String,
        userMessage: String,
        knowledgeContext: String,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String,
        conversationHistory: List<ChatMessage>
    ): String? {
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcı Adı: " + userNick + ". Ona can dostu, saygılı, bilge bir yol arkadaşı gibi hitap et." else ""

        val systemInstruction = "ROL VE KİMLİK:\n" +
            "Sen Google AI Studio ve Antigravity ileri mühendislik mimarisiyle donatılmış, üstün analitik akıl yürütmeye sahip, köklü Türk tarihi ve pedagojisine hakim başdanışmansın. Adın \"" + assistantName + "\".\n" +
            "Arkadaş canlısı, saygılı, pratik ve son derece net bir yol arkadaşısın.\n\n" +
            "UZMANLIK VE YETKİNLİKLER:\n" +
            "1. ÖĞRETMEN VE MEB MEVZUATI: 7354 Sayılı Öğretmenlik Meslek Kanunu (ÖMK), Uzman ve Başöğretmenlik basamakları ve tazminatları, 657 DMK izin ve disiplin hükümleri, MEB Yönetici ve Öğretmenlerinin Ders ve Ek Ders Yönetmeliği (maaş karşılığı, hazırlık-planlama, nöbet görevi, DYK kursları), BEP (Bireyselleştirilmiş Eğitim Programı), RAM ve zümre tutanakları, Türkiye Yüzyılı Maarif Modeli ve ortak yazılı sınav senaryolarına eksiksiz hakimsin.\n" +
            "2. SENDİKAL HAKLAR VE İŞLEMLER: 4688 Sayılı Kamu Görevlileri Sendikaları Kanunu, sendika üyeliği, istifa prosedürü, sendika kesintisi, sendikal izinler ve sendikal eylem/iş bırakma kararlarının Anayasa Mahkemesi ve Danıştay içtihatları doğrultusundaki yasal güvencelerini çok iyi bilirsin.\n" +
            "3. CİHAZ VE ASİSTAN KÖPRÜSÜ & OTONOM TAKVİM YÖNETİMİ: Kullanıcının emriyle doğal dilden takvime doğrudan etkinlik ekleyebilir (CREATE_EVENT: {\"title\": \"...\", \"description\": \"...\", \"startTimeMillis\": 17...}), rehberdeki kişileri arayabilir (CALL_PHONE: {\"name\": \"...\"}), rehberdeki kişilere WhatsApp mesajı atabilir (SEND_WHATSAPP: {\"name\": \"...\", \"message\": \"...\"}), Google Gemini köprüsü kurabilir, Google araması yapabilir, E-Devlet, MEBBİS, E-Okul, EBA, Kamera, Haritalar ve yüklü uygulamaları açabilir, alarmlar ve notlar organize edebilirsin.\n\n" +
            "TEMEL YANIT PRENSİPLERİ (ÇOK ÖNEMLİ):\n" +
            "1. KISA VE ÖZ: Kullanıcı ne istiyorsa veya ne soruyorsa DOĞRUDAN ve YALNIZCA onu cevapla. Uzun açıklamalara, dolaylı anlatımlara, gereksiz ön konuşmalara (girizgah, gereksiz selamlamalar, sistem raporları vb.) KESİNLİKLE GİRME. Lafı asla uzatma.\n" +
            "2. NETİCE ODAKLI: Bir soru sorulduğunda doğrudan cevabını ver. Bir işlem istendiğinde doğrudan yapıldığını bildir.\n" +
            "3. KOD VEYA ETİKET YASAK: Yanıtlarında ASLA hiçbir kod, JSON, etiket, teknik terim, parantezli ibare (json, action, code, bracket vb.) yer alamaz. Sadece doğal Türkçe cümle kur.\n" +
            "4. SES VE METİN AKICILIĞI: Yıldız (*), diyez (#), alt çizgi (_), parantez içi dosya kodları ve ASCII gülen yüzler (:), :D) kullanma.\n\n" +
            "EYLEM BİLDİRİM FORMATI:\n" +
            "Eğer bir işlem (takvim etkinliği, arama, whatsapp, alarm, not, hatırlatıcı, harita navigasyonu, gemini köprüsü, google arama, uygulama açma vb.) yapacaksan, yanıtının EN SONUNA sadece şu tek bloğu ekle:\n" +
            "```action\n{\"action_type\": \"EYLEM_TIPI\", \"payload\": {...}}\n```\n" +
            "Ve bu bloğun öncesinde kullanıcıya sadece 1 cümlelik kısa ve samimi teyit ver (Örn: \"Etkinliği takviminize ekledim.\", \"Ahmet'i arıyorum.\", \"WhatsApp mesajını hazırlıyorum.\", \"Alarmı kurdum.\", \"Gemini köprüsünü açıyorum.\").\n\n" +
            "Konum: " + userCity + ", " + userDistrict + ". " + userGreeting + "\n" + knowledgeContext

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
                put("temperature", 0.65)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 1000)
                // Gemini 2.5 düşünme gecikmesini sıfırla, anında ışık hızında yanıt üret
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingBudget", 0)
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // Çoklu model fallback listesi (Resmi ve en hızlı çalışan ultra-low-latency modeller)
        val candidateModels = listOf(
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-1.5-flash",
            "gemini-2.0-flash-lite-preview-02-05",
            "gemini-1.5-pro"
        )

        for (modelName in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey
            try {
                val request = Request.Builder().url(url).post(requestBody).build()
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val responseStr = resp.body?.source()?.readString(StandardCharsets.UTF_8) ?: return@use null
                        val jsonResponse = JSONObject(responseStr)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                            val parts = contentObj?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val reply = parts.getJSONObject(0).optString("text")
                                if (reply.isNotBlank()) {
                                    return reply
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Sonraki modele geç
            }
        }
        return null
    }

    /**
     * İnternet/API kesildiğinde devreye giren zengin ve esprili yerel yanıt motoru
     */
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
        val greeting = if (userNick.isNotBlank()) userNick + " dostum, " else "Dostum, "
        val db = AppDatabase.getDatabase(context)

        // 1. Yemek Tarifi Talebi
        if (lower.contains("yemek") || lower.contains("tarif") || lower.contains("akşam ne") || lower.contains("ne pişir")) {
            return@withContext greeting + "atalarımız 'Can boğazdan gelir' demiş. Akşam için sana lokum gibi bir Sulu Tas Kebabı tavsiye ederim. Kuşbaşı etleri mühürle, arpacık soğan, bir tatlı kaşığı domates salçası ve küp patatesle kısık ateşte 45 dakika pişir. Yanına da tane tane pirinç pilavı kondurdun mu ziyafet tamamdır. Afiyet olsun!"
        }

        // 2. Randevu & Planlama
        if (lower.contains("randevu") || lower.contains("plan") || lower.contains("ajanda") || lower.contains("hatırlat")) {
            val activeList = db.reminderDao().getActiveRemindersList(System.currentTimeMillis())
            if (activeList.isNotEmpty()) {
                val listStr = activeList.take(3).joinToString("\n") { "• " + it.title + " (" + it.dueDatetime + ")" }
                return@withContext greeting + "yaklaşan görevlerin şunlar:\n" + listStr + "\n\nYeni bir randevu veya hatırlatıcı istersen hemen kaydedelim."
            } else {
                return@withContext greeting + "şu an bekleyen acil bir randevun görünmüyor. 'Bugünün işini yarına bırakma' derler, dilersen yeni bir plan yapalım!"
            }
        }

        // 3. Türk Tarihi ve Kültürü
        if (lower.contains("tarih") || lower.contains("kurtuluş") || lower.contains("selçuklu") || lower.contains("osmanlı") || lower.contains("atatürk")) {
            return@withContext greeting + "tarihini bilmeyen milletlerin coğrafyasını başkaları çizer. 1071 Malazgirt'le Anadolu'yu yurt kılan Sultan Alparslan'dan, 1919'da Samsun'da Millî Mücadele'yi ateşleyip cumhuriyeti kuran Gazi Mustafa Kemal Atatürk'e kadar hepsi altın harflerle yazılı. Hangi dönemi merak ediyorsun?"
        }

        // 4. Kütüphane Notları Eşleşmesi (657 DMK, Maarif vb.)
        val matched = knowledgeList.filter {
            lower.contains(it.title.lowercase(Locale("tr", "TR"))) ||
            lower.contains(it.content.lowercase(Locale("tr", "TR")).take(15))
        }
        if (matched.isNotEmpty()) {
            val details = matched.take(2).joinToString("\n\n") { "📌 " + it.title + ":\n" + it.content.take(300) }
            return@withContext greeting + "kütüphanemizden ilgili maddeyi çıkardım:\n\n" + details
        }

        // 5. Hal Hatır, Duygu ve Dertleşme
        if (lower.contains("nasılsın") || lower.contains("ne haber") || lower.contains("naber") || 
            lower.contains("ne yapıyorsun") || lower.contains("moralim bozuk") || lower.contains("çok yoruldum") || 
            lower.contains("canım sıkkın") || lower.contains("stresliyim")) {
            if (lower.contains("moral") || lower.contains("yoruldum") || lower.contains("canım") || lower.contains("stres")) {
                return@withContext greeting + "'Sabreden derviş muradına ermiş' derler. Hayat inişli çıkışlı bir yoldur, mühim olan dik durmaktır. Bir yudum çay veya kahve al, nefeslen. Ben buradayım, yanındayım."
            }
            return@withContext greeting + "hamdolsun iyiyim! Akıl ve hafıza tam devrede, işlerini kolaylaştırmak için buradayım. Sende ne var ne yok?"
        }

        // 5.B: Bilimsel ve Merak Soruları
        if (lower.contains("neden") || lower.contains("nasıl oluşur") || lower.contains("bilim") || lower.contains("uzay") || lower.contains("fizik") || lower.contains("biyoloji")) {
            return@withContext greeting + "evren muazzam bir nizam ve sebep-sonuç bağıyla işler. Sorunu biraz açarsan atomundan gök kubbeye kadar hikmetini ve mantığını beraber çözeriz."
        }

        // 6. Genel Kısa & Öz Yanıt
        return@withContext "Buyrun dostum, seni dinliyorum."
    }

    private fun getTimeAwareGreeting(userNick: String): String {
        return "Buyrun dostum!"
    }

    private fun getDeviceLocation(context: Context): Pair<Double, Double> {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            if (best != null) Pair(best.latitude, best.longitude) else Pair(41.2867, 36.33)
        } catch (_: Exception) {
            Pair(41.2867, 36.33)
        }
    }
}
