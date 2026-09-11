package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch

private val CyberDarkBg = Color(0xFF070D1E)
private val CyberCardBg = Color(0xFF0F1A36)
private val NeonCyan = Color(0xFF00F2FE)
private val NeonBlue = Color(0xFF4FACFE)
private val NeonPurple = Color(0xFFA855F7)

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
    var showAiAssistant by remember { mutableStateOf(false) }

    // Jarvis Terminal Durumları
    var terminalInputText by remember { mutableStateOf("") }
    var lastUserPrompt by remember { mutableStateOf<String?>(null) }
    var lastJarvisStatus by remember { mutableStateOf("Sistemler aktif. Emrinizi bekliyorum efendim.") }
    var isJarvisProcessing by remember { mutableStateOf(false) }

    fun executeJarvisCommand(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        lastUserPrompt = clean
        terminalInputText = ""
        isJarvisProcessing = true
        lastJarvisStatus = "İşleniyor..."

        scope.launch {
            try {
                val response = AiAssistantService.processUserMessage(
                    context = context,
                    userMessage = clean,
                    assistantName = "Jarvis"
                )
                isJarvisProcessing = false
                lastJarvisStatus = response.actionSummary ?: if (response.replyText.length > 50) {
                    "İşleminiz tamamlandı efendim."
                } else {
                    response.replyText
                }

                if (response.isSpeechReady && response.replyText.isNotBlank()) {
                    TtsHelper.speak(context, response.replyText)
                }
            } catch (e: Exception) {
                isJarvisProcessing = false
                lastJarvisStatus = "Hata oluştu efendim. Lütfen tekrar deneyin."
            }
        }
    }

    val startVoice = rememberVoiceRecognizer { spoken ->
        if (spoken.isNotBlank()) {
            executeJarvisCommand(spoken)
        }
    }

    LaunchedEffect(directOpenAiAssistant) {
        if (directOpenAiAssistant) {
            showAiAssistant = true
        }
    }

    // Tam Ekran Jarvis Asistan Görünümü (İstenirse)
    if (showAiAssistant) {
        androidx.activity.compose.BackHandler {
            showAiAssistant = false
            viewModel.consumeAiTrigger()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030712))
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

    // Takvim & Alarm Diyaloğu
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

    // MEB & Planlama Açılır Menüsü
    if (showMebMenu) {
        AlertDialog(
            onDismissRequest = { showMebMenu = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📚", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MEB & Planlama Modülleri", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Slate900)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MebMenuItem(
                        icon = Icons.Default.MenuBook,
                        title = "657 DMK & ÖMK Mevzuatı",
                        subtitle = "Öğretmen hakları, izinler ve kanunlar",
                        color = Color(0xFF2563EB)
                    ) {
                        showMebMenu = false
                        executeJarvisCommand("657 Sayılı Kanun ve ÖMK öğretmen hakları nelerdir?")
                    }
                    MebMenuItem(
                        icon = Icons.Default.Assignment,
                        title = "ŞÖK Toplantı Tutanağı",
                        subtitle = "Şube öğretmenler kurulu taslağı hazırla",
                        color = Color(0xFF7C3AED)
                    ) {
                        showMebMenu = false
                        executeJarvisCommand("Şube Öğretmenler Kurulu ŞÖK toplantı tutanağı hazırla")
                    }
                    MebMenuItem(
                        icon = Icons.Default.FactCheck,
                        title = "Sınav Kağıdı & Rubrik",
                        subtitle = "Ortak sınav sorusu ve değerlendirme tablosu",
                        color = Color(0xFF059669)
                    ) {
                        showMebMenu = false
                        executeJarvisCommand("Ortak sınav kağıdı ve değerlendirme rubriği taslağı hazırla")
                    }
                    MebMenuItem(
                        icon = Icons.Default.CalendarToday,
                        title = "MEB Yıllık Plan & Takvim",
                        subtitle = "Ara tatiller ve çalışma takvimi",
                        color = Color(0xFFEA580C)
                    ) {
                        showMebMenu = false
                        executeJarvisCommand("MEB çalışma takvimi ve ara tatil tarihleri nelerdir?")
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

    // Özel Yapay Zeka Ajanları Açılır Menüsü
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
                        executeJarvisCommand("Öğretmen başdanışmanı ajanı devrede. Mevzuat veya sınav konusunda emrinizi dinliyorum.")
                    }
                    MebMenuItem(
                        icon = Icons.Default.Gavel,
                        title = "Mevzuat & Hukuk Ajanı",
                        subtitle = "657, sendika ve idari haklar",
                        color = Color(0xFF0284C7)
                    ) {
                        showAgentsMenu = false
                        executeJarvisCommand("Mevzuat ve hukuk ajanı devrede. İdari ve kanuni sorularınızı bekliyorum.")
                    }
                    MebMenuItem(
                        icon = Icons.Default.Explore,
                        title = "Seyahat & Rota Ajanı",
                        subtitle = "Tarihi eserler, kültür ve eczane",
                        color = Color(0xFF0D9488)
                    ) {
                        showAgentsMenu = false
                        executeJarvisCommand("Seyahat ve rota ajanı devrede. Gezilecek yerler ve konumlar için emrinizdeyim.")
                    }
                    MebMenuItem(
                        icon = Icons.Default.Security,
                        title = "Şifreli Kasa & Bellek Ajanı",
                        subtitle = "Uçtan uca şifreli notlar ve veriler",
                        color = Color(0xFFD97706)
                    ) {
                        showAgentsMenu = false
                        executeJarvisCommand("Şifreli kasa ajanı devrede. Tüm verileriniz güvenle saklanmaktadır.")
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
                        .height(44.dp),
                    cornerRadius = 12.dp,
                    elevation = 2.dp,
                    contentPadding = 8.dp,
                    onClick = { onNavigateToCategory("PRAYER_TIMES") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕌", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sıradaki Ezan: $nextName ($nextTime)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
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
        // 1. [SİSTEM & PLANLAMA] (KOMPAKT MİNİ PANELLER)
        // =========================================================================
        item {
            Text(
                text = "SİSTEM & PLANLAMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Slate700,
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
                    accentColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                ) {
                    showCalendarDialog = true
                }
                CompactPanelCard(
                    icon = Icons.Default.Place,
                    title = "Harita & Konum",
                    accentColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigateToLocations()
                }
                CompactPanelCard(
                    icon = Icons.Default.MenuBook,
                    title = "MEB & Planlama",
                    accentColor = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                ) {
                    showMebMenu = true
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
                color = Slate700,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Satır 1: İlaçlar, Faturalar, Alışveriş
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
                // Satır 2: Araç & Park, Sesli Not, Agent'lar
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
        // 3. [JARVIS TERMİNALİ] (FERAH, GENİŞLETİLMİŞ SOHBET VE SES ALANI)
        // =========================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberDarkBg)
                    .border(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.6f))), RoundedCornerShape(20.dp))
                    .padding(14.dp)
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
                                    .background(NeonCyan)
                                    .shadow(6.dp, CircleShape, spotColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "JARVIS TERMINAL v2.0",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isJarvisProcessing) "İŞLENİYOR" else "ÇEVRİMİÇİ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isJarvisProcessing) Color(0xFFF59E0B) else Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showAiAssistant = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = "Genişlet",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Minimal Durum Göstergesi (Kullanıcı talebi: Uzun metin basılmaz, net durum gösterilir)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCardBg)
                            .padding(12.dp)
                    ) {
                        Column {
                            if (lastUserPrompt != null) {
                                Text(
                                    text = "💬 Siz: $lastUserPrompt",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isJarvisProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = NeonCyan
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Text("⚡", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "Jarvis: $lastJarvisStatus",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                Text("Emrinizi yazın efendim...", fontSize = 12.sp, color = Color(0xFF64748B))
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberCardBg,
                                unfocusedContainerColor = CyberCardBg,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                executeJarvisCommand(terminalInputText)
                            })
                        )

                        // Gönder Butonu
                        IconButton(
                            onClick = { executeJarvisCommand(terminalInputText) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(NeonCyan, NeonBlue)))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Gönder", tint = CyberDarkBg, modifier = Modifier.size(20.dp))
                        }

                        // 3D Neon Kuantum Mikrofon Butonu
                        IconButton(
                            onClick = {
                                TtsHelper.stop()
                                startVoice("Jarvis sizi dinliyor efendim...")
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(NeonPurple, Color(0xFFEC4899))))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Sesle Konuş", tint = Color.White, modifier = Modifier.size(22.dp))
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
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    EmbossedCard(
        modifier = modifier.height(72.dp),
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
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(16.dp))
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
