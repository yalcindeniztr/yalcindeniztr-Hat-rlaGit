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

    suspend fun processUserMessage(
        context: Context,
        userMessage: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        assistantName: String = "ATİLA",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val dataStoreManager = DataStoreManager(context)
        val db = AppDatabase.getDatabase(context)

        AiKnowledgeSeeder.seedIfNeeded(context)

        val resolvedAssistantName = "ATİLA"
        val currentNick = dataStoreManager.userNick.first()?.trim() ?: ""

        var cleanMsg = userMessage.trim()
        val triggerRegex = Regex("""(?i)^(hey\s+)?(atilla|atila|jarvis|usta|asistan|atilla\s+dinle|atila\s+dinle|usta\s+dinle)[,\s!.:]*""")
        val hadTriggerWord = triggerRegex.find(cleanMsg) != null || cleanMsg.equals("atilla", ignoreCase = true) || cleanMsg.equals("atila", ignoreCase = true)
        cleanMsg = cleanMsg.replace(triggerRegex, "").trim()
        if (cleanMsg.isBlank() || (hadTriggerWord && cleanMsg.isBlank())) {
            return@withContext AiResponse(replyText = "Buyrun efendim, sizi dinliyorum.")
        }

        val lowerMsg = cleanMsg.lowercase(Locale.forLanguageTag("tr-TR"))

        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // =========================================================================
        // 1. DOĞRUDAN SESLİ / HIZLI NOT KAYDETME (GARANTİLİ ROOM + LOCALSTORAGE)
        // =========================================================================
        if (lowerMsg.startsWith("sesli not al") || lowerMsg.startsWith("not al") || lowerMsg.startsWith("not ekle") || lowerMsg.startsWith("hızlı not al")) {
            val noteContent = cleanMsg.replace(Regex("(?i)^(sesli not al|not al|not ekle|hızlı not al)[: ]*"), "").trim()
            val noteBody = noteContent.ifBlank { "Kayıtlı Not" }
            val noteTitle = noteBody.take(40).trim()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(now))

            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = "SESLİ NOT",
                    title = noteTitle,
                    customNote = noteBody,
                    dueDateMillis = now,
                    dueDatetime = dateStr,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "NOTE_SAVED"
                )
            )
            LocalStorageManager.saveLocalNote(context, noteTitle, noteBody, "SESLİ NOT")
            return@withContext AiResponse(
                replyText = "Efendim, notunuz 'Sesli Notlar' listenize ve yerel belleğe başarıyla kaydedildi.",
                actionSummary = "📝 Not Kaydedildi: $noteTitle"
            )
        }

        // =========================================================================
        // 2. DOĞRUDAN KONUM KAYDETME (GARANTİLİ GPS + ROOM + LOCALSTORAGE)
        // =========================================================================
        if (lowerMsg.contains("konumumu kaydet") || lowerMsg.contains("burayı kaydet") || lowerMsg.contains("konum kaydet") || lowerMsg.contains("haritaya kaydet") || lowerMsg.contains("lokasyona kaydet")) {
            val locName = cleanMsg.replace(Regex("(?i)konumumu kaydet|burayı kaydet|konum kaydet|haritaya kaydet|lokasyona kaydet|olarak|adıyla|adı|bana"), "").trim()
                .ifBlank { "$userCity $userDistrict Konumu" }

            db.savedLocationDao().insertLocation(
                SavedLocationEntity(
                    name = locName,
                    lat = realLat,
                    lng = realLng,
                    timestamp = System.currentTimeMillis()
                )
            )
            LocalStorageManager.saveLocalLocation(context, locName, realLat, realLng)
            return@withContext AiResponse(
                replyText = "Efendim, '$locName' konumunuz ($userCity $userDistrict) 'Kayıtlı Lokasyonlarım' listenize ve yerel belleğe başarıyla kaydedildi.",
                actionSummary = "📍 Konum Kaydedildi: $locName"
            )
        }

        // =========================================================================
        // 3. DOĞRUDAN İLAÇ HATIRLATICISI EKLEME
        // =========================================================================
        if (lowerMsg.startsWith("ilaç ekle") || lowerMsg.startsWith("ilaç hatırlat") || lowerMsg.contains("ilacımı ekle")) {
            val medName = cleanMsg.replace(Regex("(?i)^(ilaç ekle|ilaç hatırlat|ilacımı ekle)[: ]*"), "").trim()
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 9
            val min = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val drugTitle = medName.replace(Regex("""(?i)\d{1,2}[:.]\d{2}"""), "").trim().ifBlank { "İlaç Dozu" }

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(cal.time)
            val newRem = ReminderEntity(
                category = "MEDICINE",
                title = "💊 $drugTitle",
                customNote = "Saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} ilaç dozu.",
                dueDatetime = dateStr,
                dueDateMillis = cal.timeInMillis,
                isFavorite = true,
                encryptedMetadata = "{}",
                actionStep = "SOUND_CLASSIC_BELL"
            )
            val insertedId = db.reminderDao().insertReminder(newRem)
            AlarmHelper.scheduleAlarm(context, newRem.copy(id = insertedId.toInt()))
            LocalStorageManager.saveLocalReminder(context, "💊 $drugTitle", dateStr, cal.timeInMillis, "MEDICINE")

            return@withContext AiResponse(
                replyText = "Efendim, '$drugTitle' ilacınız her gün saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} için ilaç listenize ve sesli alarmlarınıza eklendi.",
                actionSummary = "💊 İlaç Eklendi: $drugTitle"
            )
        }

        // =========================================================================
        // 4. CANLI HAVA DURUMU & GAZETE MANŞETLERİ
        // =========================================================================
        if (lowerMsg.contains("hava durumu") || lowerMsg.contains("hava nasıl")) {
            val weatherText = WeatherHelper.getLiveWeather(context, realLat, realLng, userCity.ifBlank { "Bulunduğunuz Şehir" })
            return@withContext AiResponse(
                replyText = weatherText,
                actionSummary = "🌤️ Hava Durumu: $userCity"
            )
        }

        if (lowerMsg.contains("gazete manşet") || lowerMsg.contains("haberler") || lowerMsg.contains("günün haber")) {
            val headlines = DailyNewsHelper.getHeadlinesOnly()
            return@withContext AiResponse(
                replyText = headlines,
                actionSummary = "📰 Günlük Gazete Manşetleri"
            )
        }

        // =========================================================================
        // 5. YOUTUBE MÜZİK ÇALMA
        // =========================================================================
        if (lowerMsg.contains("çal") || lowerMsg.contains("müzik") || lowerMsg.contains("şarkı") || lowerMsg.contains("youtube")) {
            val songQuery = cleanMsg.replace(Regex("(?i)youtube'dan|youtube'da|youtube|şarkısını|şarkıyı|müziğini|müzik|çal|aç|oynat|bul|bana"), "").trim().ifBlank { "Müzik" }
            val (_, msg) = AppLauncherHelper.playYouTubeSong(context, songQuery)
            return@withContext AiResponse(
                replyText = msg,
                actionSummary = "▶️ YouTube: $songQuery"
            )
        }

        // =========================================================================
        // 6. DURUM MAKİNESİ (İNTERAKTİF ALARM & HATIRLATICI ADIMLARI)
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
                    replyText = "⏰ Alarm saatini $formattedTime olarak belirledim efendim. Peki bu alarmın başlığı ne olsun? (Örn: Uyanış, Toplantı, İlaç)"
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
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(cal.time)
            val rem = ReminderEntity(
                category = "ALARM",
                title = "⏰ Alarm: $label",
                customNote = "Saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} için kurulu alarm.",
                dueDatetime = dateStr,
                dueDateMillis = cal.timeInMillis,
                isFavorite = true,
                encryptedMetadata = "{}",
                actionStep = "ALARM_SET"
            )
            db.reminderDao().insertReminder(rem)
            LocalStorageManager.saveLocalReminder(context, "⏰ Alarm: $label", dateStr, cal.timeInMillis, "ALARM")

            return@withContext AiResponse(
                replyText = "⏰ Efendim, saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} için '$label' alarmınız başarıyla kuruldu.",
                actionSummary = "⏰ Alarm Kuruldu: $label (${String.format(Locale.ROOT, "%02d:%02d", hour, min)})"
            )
        }

        // C. Doğrudan "Saat 07:30'a alarm kur" gibi tek seferlik alarm komutu
        if (lowerMsg.contains("alarm kur") || lowerMsg.contains("alarm ekle") || lowerMsg.contains("alarmı kur")) {
            val timeMatch = Regex("""(?i)(\d{1,2})[:.](\d{2})""").find(cleanMsg)
            val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull()
            val min = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            if (hour != null && hour in 0..23) {
                val label = cleanMsg.replace(Regex("""(?i)\d{1,2}[:.]\d{2}|alarm kur|alarm ekle|alarmı kur|saat|için|bana"""), "").trim().ifBlank { "Alarm" }
                setSystemAlarm(context, hour, min, label)

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(cal.time)
                val rem = ReminderEntity(
                    category = "ALARM",
                    title = "⏰ Alarm: $label",
                    customNote = "Saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} için kurulu alarm.",
                    dueDatetime = dateStr,
                    dueDateMillis = cal.timeInMillis,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "ALARM_SET"
                )
                db.reminderDao().insertReminder(rem)
                LocalStorageManager.saveLocalReminder(context, "⏰ Alarm: $label", dateStr, cal.timeInMillis, "ALARM")

                return@withContext AiResponse(
                    replyText = "⏰ Efendim, saat ${String.format(Locale.ROOT, "%02d:%02d", hour, min)} için '$label' alarmınız kuruldu.",
                    actionSummary = "⏰ Alarm Kuruldu: $label"
                )
            } else {
                UstaSessionState.isWaitingForAlarmTime = true
                return@withContext AiResponse(
                    replyText = "Alarm saat kaç için kurulsun efendim? (Örn: 07:30 veya 8:00)"
                )
            }
        }

        // =========================================================================
        // ATİLA GARDIROP VE MODÜLER ÇEKMECELER MİMARİSİ (WARDROBE & DRAWERS)
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
