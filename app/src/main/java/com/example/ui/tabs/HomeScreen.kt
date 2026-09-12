package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReminderEntity
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.components.CalendarManagementDialog
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import com.example.util.AiAssistantService
import com.example.util.TtsHelper
import com.example.util.rememberVoiceRecognizer
import com.example.util.InAppSpeechRecognizerManager
import com.example.util.InAppListeningDialog
import com.example.util.UstaSessionState
import java.util.Locale
import kotlinx.coroutines.launch

// Canlı ve Enerjik Renk Paleti (Vibrant 3D Neumorphism)
private val LightBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val CoralGradient = Brush.horizontalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53)))
private val OceanGradient = Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8)))
private val EmeraldGradient = Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399)))
private val RoyalPurpleGradient = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFFA78BFA)))
private val GoldenAmberGradient = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFBBF24)))
private val NeonCyanGradient = Brush.horizontalGradient(listOf(Color(0xFF0891B2), Color(0xFF06B6D4), Color(0xFF22D3EE)))
private val RosePinkGradient = Brush.horizontalGradient(listOf(Color(0xFFDB2777), Color(0xFFEC4899), Color(0xFFF472B6)))
private val PurpleGradient = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7)))
private val MintGradient = Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF34D399)))
private val AmberGradient = Brush.horizontalGradient(listOf(Color(0xFFEA580C), Color(0xFFF59E0B)))
private val RoseGradient = Brush.horizontalGradient(listOf(Color(0xFFDB2777), Color(0xFFF472B6)))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LifeAssistantViewModel,
    onNavigateToAllReminders: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToParkScreen: () -> Unit,
    onNavigateToVoiceNotes: () -> Unit,
    onNavigateToFavoriteCategories: () -> Unit,
    onNavigateToCategory: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val prayerTimingData by viewModel.prayerTimingData.collectAsStateWithLifecycle()
    val directOpenAiAssistant by viewModel.directOpenAiAssistant.collectAsStateWithLifecycle()
    val autoStartListening by viewModel.autoStartListening.collectAsStateWithLifecycle()
    val initialAiPrompt by viewModel.initialAiPrompt.collectAsStateWithLifecycle()

    var showCalendarDialog by remember { mutableStateOf(false) }
    var showMebMenu by remember { mutableStateOf(false) }
    var showAgentsMenu by remember { mutableStateOf(false) }
    var showKnowledgeLibrary by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }

    // ATİLA Terminal Durumları
    var terminalInputText by remember { mutableStateOf("") }
    var lastUserPrompt by remember { mutableStateOf<String?>(null) }
    var lastAtilaStatus by remember { mutableStateOf("Sistemler hazır. Emrinizi bekliyorum efendim.") }
    var isAtilaProcessing by remember { mutableStateOf(false) }

    // 10 Saniyelik Kesintisiz Dinleyen Hands-Free Speech Recognizer
    var executeAtilaCommandRef by remember { mutableStateOf<(String) -> Unit>({}) }

    val inAppSpeechManager = remember {
        InAppSpeechRecognizerManager(context) { recognizedText ->
            if (recognizedText.isNotBlank()) {
                val lower = recognizedText.lowercase(Locale.forLanguageTag("tr-TR")).trim()
                if (lower == "kapat" || lower == "tamam" || lower == "teşekkürler" || lower == "sağol" || lower == "iptal" || lower == "dur") {
                    TtsHelper.speak(context, "Emredersiniz efendim, ben buradayım.")
                } else {
                    executeAtilaCommandRef(recognizedText)
                }
            }
        }
    }

    fun executeAtilaCommand(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        lastUserPrompt = clean
        terminalInputText = ""
        isAtilaProcessing = true
        lastAtilaStatus = "İşleniyor..."

        scope.launch {
            try {
                val response = AiAssistantService.processUserMessage(
                    context = context,
                    userMessage = clean,
                    assistantName = "ATİLA"
                )
                isAtilaProcessing = false
                lastAtilaStatus = response.actionSummary ?: if (response.replyText.length > 60) {
                    "İşleminiz tamamlandı efendim."
                } else {
                    response.replyText
                }

                if (response.isSpeechReady && response.replyText.isNotBlank()) {
                    val speechToSpeak = response.speechText ?: response.replyText
                    val trimmedSpeech = speechToSpeak.trim()

                    val needsFollowUp = !trimmedSpeech.endsWith("?") &&
                        !UstaSessionState.isWaitingForAlarmTime &&
                        !UstaSessionState.isWaitingForAlarmLabel

                    val fullSpeech = if (needsFollowUp) {
                        "$trimmedSpeech Başka bir emriniz var mı?"
                    } else {
                        trimmedSpeech
                    }

                    TtsHelper.speak(context, fullSpeech) {
                        // Seslendirme tamamlandıktan sonra kullanıcı butona basmadan otomatik 7 saniye dinlemeye geçer!
                        scope.launch {
                            kotlinx.coroutines.delay(500L)
                            inAppSpeechManager.startListening(scope, initialSeconds = 7)
                        }
                    }
                }
            } catch (_: Exception) {
                isAtilaProcessing = false
                lastAtilaStatus = "Hata oluştu efendim. Lütfen tekrar deneyin."
            }
        }
    }
    executeAtilaCommandRef = { prompt -> executeAtilaCommand(prompt) }

    val startVoice = rememberVoiceRecognizer { spoken ->
        if (spoken.isNotBlank()) {
            executeAtilaCommand(spoken)
        }
    }

    LaunchedEffect(directOpenAiAssistant) {
        if (directOpenAiAssistant) {
            showAiAssistant = true
        }
    }

    // 1. Özel Bilgi Kütüphanesi Tam Ekranı
    if (showKnowledgeLibrary) {
        androidx.activity.compose.BackHandler {
            showKnowledgeLibrary = false
        }
        KnowledgeLibraryScreen(
            viewModel = viewModel,
            onNavigateBack = { showKnowledgeLibrary = false }
        )
        return
    }

    // 2. Tam Ekran ATİLA Asistan Görünümü
    if (showAiAssistant) {
        androidx.activity.compose.BackHandler {
            showAiAssistant = false
            viewModel.consumeAiTrigger()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBg)
        ) {
            AiAssistantScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    showAiAssistant = false
                    viewModel.consumeAiTrigger()
                },
                autoStartListening = autoStartListening,
                initialPrompt = initialAiPrompt
            )
        }
        return
    }

    // 3. Takvim & Alarm Diyaloğu
    if (showCalendarDialog) {
        CalendarManagementDialog(
            allReminders = allReminders,
            onDismiss = { showCalendarDialog = false },
            onAddEvent = { title, dateMillis, note ->
                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                val newReminder = ReminderEntity(
                    category = "RANDEVU",
                    title = title,
                    dueDatetime = sdf.format(java.util.Date(dateMillis)),
                    dueDateMillis = dateMillis,
                    customNote = note,
                    encryptedMetadata = "{}",
                    actionStep = "SOUND_CLASSIC_BELL"
                )
                viewModel.addReminder(newReminder)
                com.example.util.NearbyPlacesHelper.insertEventIntoCalendar(
                    context = context,
                    title = title,
                    description = note,
                    startTimeMillis = dateMillis
                )
            },
            onDeleteEvent = { eventId ->
                viewModel.deleteReminder(eventId)
            }
        )
    }

    // 4. MEB & Planlama Açılır Menüsü
    if (showMebMenu) {
        AlertDialog(
            onDismissRequest = { showMebMenu = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📚", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MEB & Mevzuat Modülleri", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Slate900)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MebMenuItem(
                        icon = Icons.Default.MenuBook,
                        title = "Özel Bilgi Kütüphanesi",
                        subtitle = "Tüm kanun, yönetmelik ve müfredat kütüphanesi",
                        color = Color(0xFF2563EB)
                    ) {
                        showMebMenu = false
                        showKnowledgeLibrary = true
                    }
                    MebMenuItem(
                        icon = Icons.Default.Assignment,
                        title = "657 DMK & ÖMK Mevzuatı",
                        subtitle = "Öğretmen hakları, izinler ve kanunlar",
                        color = Color(0xFF7C3AED)
                    ) {
                        showMebMenu = false
                        executeAtilaCommand("657 Sayılı Kanun ve ÖMK öğretmen hakları nelerdir?")
                    }
                    MebMenuItem(
                        icon = Icons.Default.FactCheck,
                        title = "ŞÖK Toplantı Tutanağı",
                        subtitle = "Şube öğretmenler kurulu taslağı hazırla",
                        color = Color(0xFF059669)
                    ) {
                        showMebMenu = false
                        executeAtilaCommand("Şube Öğretmenler Kurulu ŞÖK toplantı tutanağı hazırla")
                    }
                    MebMenuItem(
                        icon = Icons.Default.CalendarToday,
                        title = "Sınav Kağıdı & Rubrik",
                        subtitle = "Ortak sınav sorusu ve değerlendirme tablosu",
                        color = Color(0xFFEA580C)
                    ) {
                        showMebMenu = false
                        executeAtilaCommand("Ortak sınav kağıdı ve değerlendirme rubriği taslağı hazırla")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMebMenu = false }) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = Slate700)
                }
            }
        )
    }

    // 5. Özel Yapay Zeka Ajanları Açılır Menüsü
    if (showAgentsMenu) {
        AlertDialog(
            onDismissRequest = { showAgentsMenu = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Özel Yapay Zeka Ajanları", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Slate900)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MebMenuItem(
                        icon = Icons.Default.School,
                        title = "Öğretmen Başdanışmanı Ajanı",
                        subtitle = "Mevzuat, plan ve sınav uzmanı",
                        color = Color(0xFF4F46E5)
                    ) {
                        showAgentsMenu = false
                        executeAtilaCommand("Öğretmen başdanışmanı ajanı devrede. Mevzuat veya sınav konusunda emrinizi dinliyorum.")
                    }
                    MebMenuItem(
                        icon = Icons.Default.Gavel,
                        title = "Mevzuat & Hukuk Ajanı",
                        subtitle = "657, sendika ve idari haklar",
                        color = Color(0xFF0284C7)
                    ) {
                        showAgentsMenu = false
                        executeAtilaCommand("Mevzuat ve hukuk ajanı devrede. İdari ve kanuni sorularınızı bekliyorum.")
                    }
                    MebMenuItem(
                        icon = Icons.Default.Explore,
                        title = "Seyahat & Rota Ajanı",
                        subtitle = "Tarihi eserler, kültür ve eczane",
                        color = Color(0xFF0D9488)
                    ) {
                        showAgentsMenu = false
                        executeAtilaCommand("Seyahat ve rota ajanı devrede. Gezilecek yerler ve konumlar için emrinizdeyim.")
                    }
                    MebMenuItem(
                        icon = Icons.Default.Security,
                        title = "Şifreli Kasa & Bellek Ajanı",
                        subtitle = "Uçtan uca şifreli notlar ve veriler",
                        color = Color(0xFFD97706)
                    ) {
                        showAgentsMenu = false
                        executeAtilaCommand("Şifreli kasa ajanı devrede. Tüm verileriniz güvenle saklanmaktadır.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAgentsMenu = false }) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = Slate700)
                }
            }
        )
    }

    InAppListeningDialog(manager = inAppSpeechManager, assistantName = "ATİLA")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // =========================================================================
        // 🕌 EZAN VAKTİ KARTI (AYNEN KORUNUYOR)
        // =========================================================================
        if (prayerTimingData != null) {
            item {
                val timing = prayerTimingData!!
                val prayers = listOf(
                    "İmsak" to timing.imsak,
                    "Güneş" to timing.gunes,
                    "Öğle" to timing.ogle,
                    "İkindi" to timing.ikindi,
                    "Akşam" to timing.aksam,
                    "Yatsı" to timing.yatsi
                )

                val now = java.util.Calendar.getInstance()
                val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = now.get(java.util.Calendar.MINUTE)
                val currentMinutesTotal = currentHour * 60 + currentMinute

                var nextName = "İmsak"
                var nextTime = timing.imsak
                var minutesUntilNext = 0

                for ((name, timeStr) in prayers) {
                    val parts = timeStr.split(":")
                    if (parts.size == 2) {
                        val h = parts[0].toIntOrNull() ?: 0
                        val m = parts[1].toIntOrNull() ?: 0
                        val targetTotal = h * 60 + m
                        if (targetTotal > currentMinutesTotal) {
                            nextName = name
                            nextTime = timeStr
                            minutesUntilNext = targetTotal - currentMinutesTotal
                            break
                        }
                    }
                }
                if (minutesUntilNext <= 0) {
                    val firstParts = timing.imsak.split(":")
                    val h = firstParts.getOrNull(0)?.toIntOrNull() ?: 5
                    val m = firstParts.getOrNull(1)?.toIntOrNull() ?: 0
                    val imsakTomorrowTotal = (24 * 60 - currentMinutesTotal) + (h * 60 + m)
                    nextName = "İmsak"
                    nextTime = timing.imsak
                    minutesUntilNext = imsakTomorrowTotal
                }

                val hoursLeft = minutesUntilNext / 60
                val minsLeft = minutesUntilNext % 60
                val remainingFormatted = if (hoursLeft > 0) "${hoursLeft}sa ${minsLeft}dk" else "${minsLeft}dk"

                EmbossedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    cornerRadius = 14.dp,
                    elevation = 3.dp,
                    contentPadding = 8.dp,
                    onClick = { onNavigateToCategory("PRAYER_TIMES") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕌", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sıradaki Ezan: $nextName ($nextTime)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0F2FE))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Kalan: $remainingFormatted",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 1. [SİSTEM & PLANLAMA] (KOMPAKT 3D PANELLER)
        // =========================================================================
        item {
            Text(
                text = "SİSTEM & PLANLAMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Slate800,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactPanelCard(
                    icon = Icons.Default.CalendarMonth,
                    title = "Takvim & Alarm",
                    gradient = AmberGradient,
                    modifier = Modifier.weight(1f)
                ) {
                    showCalendarDialog = true
                }
                CompactPanelCard(
                    icon = Icons.Default.Place,
                    title = "Harita & Konum",
                    gradient = OceanGradient,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigateToLocations()
                }
                CompactPanelCard(
                    icon = Icons.Default.MenuBook,
                    title = "Özel Kütüphane",
                    gradient = PurpleGradient,
                    modifier = Modifier.weight(1f)
                ) {
                    showKnowledgeLibrary = true
                }
            }
        }

        // =========================================================================
        // 2. [KİŞİSEL TAKİP & BELLEK] (DOĞRUDAN ODAKLI MİKRO ROZETLER)
        // =========================================================================
        item {
            Text(
                text = "KİŞİSEL TAKİP & BELLEK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Slate800,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MicroBadgeCard(
                        icon = Icons.Default.MedicalServices,
                        label = "İlaçlar",
                        badgeDesc = "Doz / Kullanım",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigateToCategory("MEDICINE")
                    }
                    MicroBadgeCard(
                        icon = Icons.Default.ReceiptLong,
                        label = "Faturalar",
                        badgeDesc = "Tutar / Ödeme",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigateToCategory("BILLS_CARDS")
                    }
                    MicroBadgeCard(
                        icon = Icons.Default.ShoppingCart,
                        label = "Alışveriş",
                        badgeDesc = "Kontrol Listesi",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigateToCategory("SHOPPING")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MicroBadgeCard(
                        icon = Icons.Default.DirectionsCar,
                        label = "Araç & Park",
                        badgeDesc = "GPS / Muayene",
                        color = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigateToParkScreen()
                    }
                    MicroBadgeCard(
                        icon = Icons.Default.Mic,
                        label = "Sesli Not",
                        badgeDesc = "Şifreli Notlar",
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigateToVoiceNotes()
                    }
                    MicroBadgeCard(
                        icon = Icons.Default.SmartToy,
                        label = "Agent'lar",
                        badgeDesc = "Özel AI Ajanı",
                        color = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f)
                    ) {
                        showAgentsMenu = true
                    }
                }
            }
        }

        // =========================================================================
        // 3. [ATİLA TERMİNALİ] (AÇIK RENKLİ, 3D KABARTMALI, FERAH SOHBET VE SES ALANI)
        // =========================================================================
        item {
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevation = 4.dp,
                contentPadding = 14.dp
            ) {
                Column {
                    // Terminal Üst Barı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ATİLA TERMİNALİ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAtilaProcessing) "İŞLENİYOR" else "ÇEVRİMİÇİ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAtilaProcessing) Color(0xFFD97706) else Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showAiAssistant = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = "Genişlet",
                                    tint = Slate700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hızlı Aksiyon Çipleri
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        val quickActions = listOf(
                            "🏥 Nöbetçi Eczane" to "En yakın nöbetçi eczane nerede?",
                            "📋 Günü Planla" to "Bugün için dengeli bir günlük rutin planla",
                            "🌤️ Canlı Hava" to "Bugün hava nasıl?",
                            "📰 Manşetler" to "Günün gazete manşetlerini özetle",
                            "🎵 Müzik Çal" to "YouTube'da Barış Manço çal",
                            "💡 Karar / Fikir Sor" to "Sence bir konuda ne yapmalıyım?",
                            "📍 Konum Kaydet" to "Konumumu burası olarak kaydet",
                            "📚 MEB Mevzuat" to "657 Sayılı Kanun ve ÖMK hakları"
                        )
                        items(quickActions) { (label, cmd) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { executeAtilaCommand(cmd) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            }
                        }
                    }

                    // Minimal Durum Göstergesi (Net ve okunaklı)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            if (lastUserPrompt != null) {
                                Text(
                                    text = "💬 Siz: $lastUserPrompt",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate700,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAtilaProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = OrangePrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Text("⚡", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "Atila: $lastAtilaStatus",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metin Yazma & Gönder Barı ("Ben yazarsam yeterli")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = terminalInputText,
                            onValueChange = { terminalInputText = it },
                            placeholder = {
                                Text("Emrinizi yazın efendim...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                executeAtilaCommand(terminalInputText)
                            })
                        )

                        // 3D Gönder Butonu
                        IconButton(
                            onClick = { executeAtilaCommand(terminalInputText) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OceanGradient)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Gönder", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // 3D Renk Geçişli Mikrofon Butonu (Hands-Free Akıcı Dinleme)
                        IconButton(
                            onClick = {
                                TtsHelper.stop()
                                inAppSpeechManager.startListening(scope, initialSeconds = 12)
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CoralGradient)
                                .shadow(4.dp, RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Sesle Konuş (Hands-Free)", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPanelCard(
    icon: ImageVector,
    title: String,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    EmbossedCard(
        modifier = modifier.height(76.dp),
        cornerRadius = 14.dp,
        elevation = 3.dp,
        contentPadding = 8.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Slate900,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MicroBadgeCard(
    icon: ImageVector,
    label: String,
    badgeDesc: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    EmbossedCard(
        modifier = modifier.height(68.dp),
        cornerRadius = 14.dp,
        elevation = 2.5.dp,
        contentPadding = 8.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = badgeDesc,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Slate700,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MebMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
            Text(subtitle, fontSize = 10.sp, color = Slate700)
        }
    }
}
