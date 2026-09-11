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
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
            return@withContext AiResponse(replyText = "Buyrun efendim, sizi dinliyorum.")
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
                    replyText = "Saati tam anlayamadım efendim. Lütfen alarm saatini '07:30' veya '8:00' şeklinde söyler misiniz?"
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
                    replyText = "Emredersiniz efendim, lokasyon kaydı iptal edildi."
                )
            }
        }

        // =========================================================================
        // ATİLLA GARDIROP VE MODÜLER ÇEKMECELER MİMARİSİ (WARDROBE & DRAWERS)
        // =========================================================================
        val customApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey)?.trim() else null
        } catch (_: Exception) { null }

        val activeApiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getSecureDefaultKey()

        val sessionData = com.example.util.assistant.WardrobeSessionData(
            apiKey = activeApiKey,
            assistantName = resolvedAssistantName,
            userNick = currentNick,
            userCity = userCity,
            userDistrict = userDistrict,
            userLat = realLat,
            userLng = realLng,
            conversationHistory = conversationHistory,
            db = db,
            dataStoreManager = dataStoreManager
        )

        val drawerResult = com.example.util.assistant.AtillaWardrobeManager.dispatch(context, cleanMsg, sessionData)
        return@withContext AiResponse(
            replyText = drawerResult.replyText,
            recommendedPlaces = drawerResult.recommendedPlaces,
            actionSummary = drawerResult.actionSummary
        )
    }

    private fun getTimeAwareGreeting(userNick: String): String {
        return "Emredersiniz efendim."
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
