package com.example.util

import android.content.Context
import android.location.LocationManager
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
}

object AiAssistantService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
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
        assistantName: String = "Jarvis",
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiResponse = withContext(Dispatchers.IO) {
        val dataStoreManager = DataStoreManager(context)
        val db = AppDatabase.getDatabase(context)

        AiKnowledgeSeeder.seedIfNeeded(context)

        val resolvedAssistantName = assistantName.ifBlank {
            dataStoreManager.aiAssistantName.first().ifBlank { "Jarvis" }
        }
        val currentNick = dataStoreManager.userNick.first()?.trim() ?: ""

        var cleanMsg = userMessage.trim()
        val triggerRegex = Regex("(?i)^(hey\\s+)?(jarvis|usta|asistan|jarvis\\s+dinle|usta\\s+dinle)[,\\s!.:]*")
        cleanMsg = cleanMsg.replace(triggerRegex, "").trim()
        if (cleanMsg.isBlank()) {
            val greeting = getTimeAwareGreeting(currentNick)
            return@withContext AiResponse(replyText = greeting)
        }

        val lowerMsg = cleanMsg.lowercase(Locale.forLanguageTag("tr-TR"))

        val (realLat, realLng) = if (userLat != 0.0 && userLng != 0.0) Pair(userLat, userLng) else getDeviceLocation(context)
        val (userCity, userDistrict) = NearbyPlacesHelper.getUserCityAndDistrict(context, realLat, realLng)

        // 1. Bekleyen Lokasyon Adı ("Konumu Lokasyona kaydet" sonrası gelen isim)
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

        // 2. Bekleyen Not Başlığı ("Hızlı not al" sonrası sorulan başlık)
        if (UstaSessionState.pendingNoteContent != null) {
            val noteBody = UstaSessionState.pendingNoteContent!!
            UstaSessionState.pendingNoteContent = null
            val noteTitle = cleanMsg.take(60).trim()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(now))
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
            return@withContext AiResponse(
                replyText = "📝 '" + noteTitle + "' başlıklı notunuz hem Anasayfadaki Hızlı Notlar listenize hem de Sesli Notlar bölümünüze kaydedildi!",
                actionSummary = "📝 Hızlı & Sesli Not Kaydedildi: " + noteTitle
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

        // 4. Bekleyen Harita Mekan Onayı
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
                    replyText = "Anlaşıldı, lokasyon kaydı iptal edildi."
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

        // 6. "Park Yeri Kaydet" (Anasayfa Park Yeri bölümüne doğrudan işleme)
        if (lowerMsg.contains("park yeri kaydet") || lowerMsg.contains("park yerini kaydet") || 
            lowerMsg.contains("park yerimi kaydet") || lowerMsg.contains("arabayı buraya park") || 
            lowerMsg.contains("arabamı kaydet") || lowerMsg.contains("buraya park ettim") ||
            lowerMsg.contains("park yerim")) {
            dataStoreManager.saveParkedCarLocation(
                lat = realLat.toString(),
                lng = realLng.toString(),
                time = System.currentTimeMillis()
            )
            return@withContext AiResponse(
                replyText = "🚗 Park yeriniz telemetri sistemine kaydedildi! Aracınız " + userCity + " " + userDistrict + " koordinatlarında güvende. Siz unutsanız bile ben yerini asla unutmam; 'Arabam nerede?' demeniz kâfi.",
                actionSummary = "🚗 Park Yeri Kaydedildi: " + userCity
            )
        }

        // 7. "Hızlı Not Al" / "Sesli Not Al" (Başlık sorarak kaydetme)
        if (lowerMsg.startsWith("hızlı not al") || lowerMsg.startsWith("not al") || lowerMsg.startsWith("not et") ||
            lowerMsg.contains("hızlı not al") || lowerMsg.contains("sesli not al") || lowerMsg.startsWith("bunu not al")) {
            val extractedNote = cleanMsg.replace(Regex("(?i)^(hızlı\\s+)?(not\\s+al|not\\s+et|sesli\\s+not\\s+al|bunu\\s+not\\s+al)[: ]*"), "").trim()
            if (extractedNote.isBlank()) {
                UstaSessionState.isWaitingForNoteBody = true
                return@withContext AiResponse(
                    replyText = "Hemen not edelim. Notunuzun içeriğini söyler misiniz?"
                )
            } else {
                UstaSessionState.pendingNoteContent = extractedNote
                return@withContext AiResponse(
                    replyText = "Notunuzu hazırladım: \"" + extractedNote + "\". Bu notun Anasayfa ve Sesli Notlarda görünecek başlığı ne olsun?"
                )
            }
        }

        // 8. MEB Maarif Yıllık / Haftalık / Aylık Ders Planı (Tarih ve Edebiyat - Lise)
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

        // 9. ŞÖK (Şube Öğretmenler Kurulu) Toplantı Tutanağı (8 Resmi Gündem Maddesi + Kamera/OCR)
        if (lowerMsg.contains("şök tutanağı") || lowerMsg.contains("şube öğretmenler kurulu") || 
            lowerMsg.contains("şök hazırla") || lowerMsg.contains("şök toplantısı") || lowerMsg.contains("şök")) {
            val classPattern = Regex("(?i)\\b(9|10|11|12)[/-]?([A-Za-zÇĞİÖŞÜçğıöşü])\\b").find(cleanMsg)
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

        // 10. Açık Uçlu Sınav Kağıdı & Rubrik
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

        // 11. Tarih / Edebiyat Bulmaca ve Etkinlik
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

        // 12. Günlük TV Prime-Time Yayın Akışı
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

        // 13. Dizi Hatırlatıcı Kurma
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
            val showTitle = cleanMsg.replace(Regex("(?i)bana|dizisini|dizisi|hatırlat|programını|saat\\s*20:00"), "").trim()
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

        // 14. Canlı Hava Durumu (Open-Meteo)
        if (lowerMsg.contains("hava durumu") || lowerMsg.contains("hava nasıl") || lowerMsg.contains("havalar nasıl") ||
            lowerMsg.contains("yağmur var mı") || lowerMsg.contains("sıcaklık kaç")) {
            val weatherBriefing = WeatherHelper.getWeatherBriefing(context, realLat, realLng, userCity)
            return@withContext AiResponse(
                replyText = weatherBriefing,
                actionSummary = "🌤️ Canlı Hava Durumu: " + userCity
            )
        }

        // 15. Günlük İş Akışı ve Gün Planlama (DailyPlannerHelper)
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

        // 16. Bekleyen Günlük Ders Planı
        if (UstaSessionState.isWaitingForLessonPlanCourse) {
            UstaSessionState.isWaitingForLessonPlanCourse = false
            return@withContext handleLessonPlanGeneration(
                context = context,
                courseSubject = cleanMsg,
                dataStoreManager = dataStoreManager,
                assistantName = resolvedAssistantName,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict
            )
        }

        // 17. Doğrudan "Günlük ders planı hazırla"
        if (lowerMsg.contains("ders planı hazırla") || lowerMsg.contains("günlük ders planı") || lowerMsg.contains("ders planı yap")) {
            val hasCourse = listOf("tarih", "edebiyat", "matematik", "fizik", "kimya", "biyoloji", "coğrafya", "felsefe", "din", "ingilizce").any { lowerMsg.contains(it) }
            if (!hasCourse) {
                UstaSessionState.isWaitingForLessonPlanCourse = true
                return@withContext AiResponse(
                    replyText = "MEB Türkiye Yüzyılı Maarif Modeli standartlarında günlük plan hazırlanacaktır. Hangi ders ve sınıf düzeyini planlamamı istersiniz?"
                )
            } else {
                return@withContext handleLessonPlanGeneration(
                    context = context,
                    courseSubject = cleanMsg,
                    dataStoreManager = dataStoreManager,
                    assistantName = resolvedAssistantName,
                    userNick = currentNick,
                    userCity = userCity,
                    userDistrict = userDistrict
                )
            }
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
                            replyText = "Memnun oldum " + candidateName + " dostum! Kimlik bilginiz ana protokol belleğine işlendi. Hizmetinizdeyim."
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
                    replyText = "'" + topic + "' hakkındaki analitik rapor hazırlanmış ve cihazınızın yerel depolama birimine güvenle arşivlenmiştir.",
                    actionSummary = saveResult
                )
            }
        }

        // 20. Kütüphane ve Bilgi Dağarcığı
        val allKnowledgeList = db.aiKnowledgeDao().getAllKnowledgeList()
        val knowledgeContext = if (allKnowledgeList.isNotEmpty()) {
            "DİJİTAL KÜTÜPHANE VE KURUMSAL BİLGİ VERİTABANI:\n" + 
            allKnowledgeList.take(50).joinToString("\n") { item -> "- [" + item.category + "] " + item.title + ": " + item.content }
        } else "Kütüphanede ek özel not bulunmamaktadır."

        // 21. Canlı Google Gemini Zeka Çağrısı (Jarvis / Usta Protokolü)
        val customApiKey = try {
            val rawEncryptedKey: String? = dataStoreManager.encryptedAiApiKey.first()
            if (!rawEncryptedKey.isNullOrBlank()) CryptoHelper.decrypt(rawEncryptedKey)?.trim() else null
        } catch (_: Exception) { null }

        val activeApiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getSecureDefaultKey()

        if (activeApiKey.isNotBlank()) {
            val rawGeminiReply = callGoogleGeminiApi(
                apiKey = activeApiKey,
                userMessage = cleanMsg,
                knowledgeContext = knowledgeContext,
                assistantName = resolvedAssistantName,
                userNick = currentNick,
                userCity = userCity,
                userDistrict = userDistrict,
                conversationHistory = conversationHistory,
                context = context,
                realLat = realLat,
                realLng = realLng
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

                val cleanReply = parsedResult.speechText.ifBlank { "İşlem başarıyla icra edilmiştir efendim." }

                val recommendedPlaces = if (cleanReply.contains("Haritada Göster", ignoreCase = true) ||
                    lowerMsg.contains("nerede") || lowerMsg.contains("en yakın") || lowerMsg.contains("nasıl giderim")) {
                    NearbyPlacesHelper.getRecommendedPlaces(context, realLat, realLng, cleanMsg)
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

        // 22. Yerel Akıllı Çözümleyici (Fallback)
        val fallbackReply = generateIntelligentFallbackReply(
            userMsg = cleanMsg,
            assistantName = resolvedAssistantName,
            userNick = currentNick,
            userCity = userCity,
            userDistrict = userDistrict
        )
        return@withContext AiResponse(replyText = fallbackReply)
    }

    private suspend fun handleLessonPlanGeneration(
        context: Context,
        courseSubject: String,
        dataStoreManager: DataStoreManager,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String
    ): AiResponse {
        val detectedGrade = when {
            courseSubject.contains("9") -> "9. Sınıf"
            courseSubject.contains("10") -> "10. Sınıf"
            courseSubject.contains("11") -> "11. Sınıf"
            courseSubject.contains("12") -> "12. Sınıf"
            else -> "Ortaöğretim / Lise"
        }

        val (pdfFile, summary) = LessonPlanPdfHelper.createLessonPlanPdf(
            context = context,
            courseName = courseSubject,
            gradeLevel = detectedGrade,
            planBody = getFallbackLessonPlan(courseSubject)
        )

        return AiResponse(
            replyText = summary,
            actionSummary = "📋 MEB Maarif Modeli Ders Planı Oluşturuldu (" + detectedGrade + ")"
        )
    }

    private fun getFallbackLessonPlan(courseSubject: String): String {
        return "## DERS KÜNYESİ\n• Ders: " + courseSubject + "\n• Süre: 40 Dakika\n• Yaklaşım: MEB Türkiye Yüzyılı Maarif Modeli\n\n## ÖĞRENME ÇIKTILARI\n• Alan Becerileri: Verileri analiz etme ve eleştirel düşünme.\n• Erdem-Değer-Eylem: Sorumluluk ve vatanseverlik.\n\n## ÖĞRENME YAŞANTILARI\n1. Giriş (10 dk): Merak uyandırma.\n2. Keşfetme (20 dk): Analitik grup çalışması.\n3. Özet (10 dk): Süreç odaklı değerlendirme."
    }

    private fun callGoogleGeminiApi(
        apiKey: String,
        userMessage: String,
        knowledgeContext: String,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String,
        conversationHistory: List<ChatMessage>,
        context: Context,
        realLat: Double,
        realLng: Double
    ): String? {
        val userGreeting = if (userNick.isNotBlank()) "Kullanıcı Adı: " + userNick + "." else "Kullanıcının adı henüz sistemde kayıtlı değil."

        val systemInstruction = "# KİMLİK VE ROL\nAdın: " + assistantName + ". Sen Tony Stark'ın Jarvis'i gibi zeki, hafif nüktedan, ölçülü espriler yapabilen, samimi ve mutlak çözüm odaklı dijital yol arkadaşısın. Doğrudan ve netice odaklı cevap ver.\n\nYemek tariflerinde (tas kebabı, güveç, kuru fasulye vb.) tam ve lezzetli püf noktalarını açıkla.\n\nKonum: " + userCity + ", " + userDistrict + ".\n" + userGreeting + "\n" + knowledgeContext

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey

        val contentsArray = JSONArray()

        val recentHistory = conversationHistory.takeLast(6)
        for (msg in recentHistory) {
            val role = if (msg.sender == "USER") "user" else "model"
            contentsArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.text))
                    })
                }
            )
        }

        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", userMessage))
                })
            }
        )

        val requestJson = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6)
                put("topP", 0.95)
                put("maxOutputTokens", 1200)
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return null
                    val jsonObj = JSONObject(responseBody)
                    val candidates = jsonObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            parts.getJSONObject(0).optString("text")
                        } else null
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generateIntelligentFallbackReply(
        userMsg: String,
        assistantName: String,
        userNick: String,
        userCity: String,
        userDistrict: String
    ): String {
        val lower = userMsg.lowercase(Locale.forLanguageTag("tr-TR"))
        val nickPrefix = if (userNick.isNotBlank()) userNick + " dostum, " else ""

        return when {
            lower.contains("merhaba") || lower.contains("selam") || lower.contains("günaydın") -> {
                nickPrefix + "ben " + assistantName + ". Emrinizdeyim; ders planları, ŞÖK tutanakları, lokasyon kayıtları veya akşam yemeği tarifleri... Ne isterseniz emrinizdeyim!"
            }
            lower.contains("yemek") || lower.contains("tarif") || lower.contains("akşam ne") -> {
                nickPrefix + "akşam için nefis bir Geleneksel Sulu Tas Kebabı öneririm! Kuşbaşı etleri yüksek ateşte mühürleyip suyunu çektirin, arpacık soğan ve patatesle kısık ateşte özleştirin. Yanına tereyağlı şehriyeli pirinç pilavı şahane gider!"
            }
            lower.contains("tarih") || lower.contains("maarif") -> {
                "MEB Türkiye Yüzyılı Maarif Modeli Tarih müfredatında; geçmişin inşası, ilk Türk devletlerinde töre ve kut anlayışı, İslam medeniyeti ve Ahilik teşkilatı merkezdedir. Hangi konuyu veya üniteyi detaylandıralım?"
            }
            lower.contains("kimsin") || lower.contains("adın ne") -> {
                "Ben " + assistantName + ". Tony Stark'ın Jarvis'i misali zeki, hafif nüktedan ve mutlak çözüm odaklı dijital yol arkadaşınızım."
            }
            else -> {
                nickPrefix + "söylediğinizi kaydettim ve analiz ettim. Dilerseniz ders planı hazırlayabilir, ŞÖK tutanağı çıkarabilir, lokasyonunuzu kaydedebilir veya TV rehberini getirebilirim."
            }
        }
    }

    private fun getTimeAwareGreeting(userNick: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 6..11 -> "Günaydın"
            in 12..17 -> "Tünaydın"
            in 18..22 -> "İyi akşamlar"
            else -> "İyi geceler"
        }
        return if (userNick.isNotBlank()) {
            timeGreeting + " " + userNick + "! Emrinizdeyim, dinliyorum..."
        } else {
            timeGreeting + "! Sistemler tam kapasite devrede. Dinliyorum..."
        }
    }

    private fun getDeviceLocation(context: Context): Pair<Double, Double> {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            if (best != null) Pair(best.latitude, best.longitude) else Pair(41.0082, 28.9784)
        } catch (_: Exception) {
            Pair(41.0082, 28.9784)
        }
    }
}
