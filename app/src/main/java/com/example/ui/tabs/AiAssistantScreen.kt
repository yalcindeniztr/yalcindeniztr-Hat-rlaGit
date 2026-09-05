package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiKnowledgeEntity
import com.example.ui.LifeAssistantViewModel
import com.example.util.AiAssistantService
import com.example.util.NearbyPlace
import com.example.util.NearbyPlacesHelper
import com.example.util.TtsHelper
import com.example.util.rememberVoiceRecognizer
import kotlinx.coroutines.launch
import kotlin.math.sin

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val recommendedPlaces: List<NearbyPlace> = emptyList(),
    val actionSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Sci-Fi Cyber Color Palette
private val CyberBgDark = Color(0xFF030712)
private val CyberBgMid = Color(0xFF090D1A)
private val NeonCyan = Color(0xFF00F2FE)
private val NeonBlue = Color(0xFF4FACFE)
private val NeonPurple = Color(0xFFA855F7)
private val NeonGreen = Color(0xFF10B981)
private val CyberCardBg = Color(0xD90B132B)
private val CyberCardUser = Color(0xDE1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: LifeAssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val assistantName by viewModel.aiAssistantName.collectAsStateWithLifecycle()
    val isVoiceResponsesEnabled by viewModel.isAiVoiceResponsesEnabled.collectAsStateWithLifecycle()
    val userNick by viewModel.userNick.collectAsStateWithLifecycle()
    val allKnowledge by viewModel.allAiKnowledge.collectAsStateWithLifecycle()

    val decryptedNick = remember(userNick) { viewModel.getDecryptedUserNick() }

    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var showKnowledgeDialog by remember { mutableStateOf(false) }

    val initialGreeting = remember(decryptedNick, assistantName) {
        if (decryptedNick.isNotBlank()) {
            "NÃ¶ral baÄŸlantÄ± kuruldu $decryptedNick dostum! Ben $assistantName. TÃ¼rk tarihi, Maarif mÃ¼fredatÄ±, yemek tarifleri, mekanlar veya alarmlar... Ne istersen emrindeyim."
        } else {
            "Sistem aktif! Ben HatÄ±rlaGit Yapay Zeka Ã‡ekirdeÄŸi $assistantName. Sana Ã¶zel hitap edebilmem iÃ§in adÄ±nÄ± Ã¶ÄŸrenebilir miyim?"
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "AI",
                text = initialGreeting
            )
        )
    }

    // Direct and Instant Speech execution helper
    fun executeUserPrompt(promptText: String) {
        if (promptText.isBlank()) return
        val userMsg = ChatMessage(sender = "USER", text = promptText)
        messages.add(userMsg)
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            isProcessing = true
            val response = AiAssistantService.processUserMessage(
                context = context,
                userMessage = promptText,
                assistantName = assistantName,
                conversationHistory = messages.toList()
            )
            val aiMsg = ChatMessage(
                sender = "AI",
                text = response.replyText,
                recommendedPlaces = response.recommendedPlaces,
                actionSummary = response.actionSummary
            )
            messages.add(aiMsg)
            isProcessing = false
            listState.animateScrollToItem(messages.size - 1)

            // Direct Instant Speech (No unnecessary delay, direct Turkish TTS)
            if (isVoiceResponsesEnabled && response.isSpeechReady) {
                isSpeaking = true
                TtsHelper.speak(context, response.replyText)
                // Speaking indicator resets after a natural duration
                launch {
                    val durationMs = (response.replyText.length * 60L).coerceIn(2000L, 12000L)
                    kotlinx.coroutines.delay(durationMs)
                    isSpeaking = false
                }
            }
        }
    }

    // Voice recognition launcher
    val startVoiceRecognition = rememberVoiceRecognizer { recognizedText ->
        isListening = false
        if (recognizedText.isNotBlank()) {
            executeUserPrompt(recognizedText)
        }
    }

    // Sci-Fi Hologram Transitions & Rotations
    val infiniteTransition = rememberInfiniteTransition(label = "scifi_hud")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRotation"
    )

    Scaffold(
        containerColor = CyberBgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 3D Hologram Avatar Core
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(8.dp, CircleShape, spotColor = NeonCyan)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(NeonCyan, NeonPurple, CyberBgDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().rotate(ringRotation)) {
                                drawCircle(
                                    color = NeonCyan,
                                    radius = size.minDimension / 2.2f,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = assistantName.uppercase(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan.copy(alpha = 0.2f))
                                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "v1.1.6",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonCyan
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isProcessing) NeonPurple else if (isSpeaking) NeonBlue else NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeaking) "USTA SESLÄ° CEVAP VERÄ°YOR..." else if (isProcessing) "VERÄ° Ä°ÅLENÄ°YOR..." else "QUANTUM Ã‡EKÄ°RDEK : Ã‡EVRÄ°MÄ°Ã‡Ä°",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpeaking) NeonBlue else if (isProcessing) NeonPurple else NeonGreen,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = NeonCyan)
                    }
                },
                actions = {
                    // Knowledge Vault Button
                    IconButton(
                        onClick = { showKnowledgeDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        BadgedBox(
                            badge = {
                                if (allKnowledge.isNotEmpty()) {
                                    Badge(containerColor = NeonPurple, contentColor = Color.White) {
                                        Text(allKnowledge.size.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Bellek", tint = NeonCyan)
                        }
                    }

                    // Mute/Unmute TTS
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.toggleAiVoiceResponses(!isVoiceResponsesEnabled)
                            }
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            if (isVoiceResponsesEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Ses AÃ§/Kapat",
                            tint = if (isVoiceResponsesEnabled) NeonCyan else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBgMid
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(CyberBgMid, CyberBgDark, CyberBgMid)
                    )
                )
        ) {
            // Ambient Sci-Fi Hologram Grid Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 40.dp.toPx()
                val linePaint = NeonCyan.copy(alpha = 0.03f)
                var x = 0f
                while (x < size.width) {
                    drawLine(linePaint, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(linePaint, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += gridSpacing
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Sinematik CanlÄ± Ses DalgasÄ± (Audio Spectrum Waveform) & Kuantum GÃ¶rselleÅŸtirici
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF070D1F).copy(alpha = 0.85f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "NÃ–RAL SPEKTRUM // CANLI SES DALGASI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonCyan,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (isSpeaking) "SES Ã‡IKIÅI: AKTÄ°F" else if (isListening) "DÄ°NLENÄ°YOR" else "BEKLEMEDE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpeaking) NeonBlue else if (isListening) NeonPurple else Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // CanlÄ± Ses DalgasÄ± Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            val barCount = 32
                            val barWidth = size.width / (barCount * 1.6f)
                            val isActive = isSpeaking || isListening || isProcessing

                            for (i in 0 until barCount) {
                                val x = i * (barWidth * 1.6f) + barWidth / 2
                                val multiplier = if (isActive) {
                                    (sin(wavePhase + i * 0.35f) * 0.5f + 0.5f).coerceIn(0.15f, 1f)
                                } else {
                                    (sin(i * 0.2f) * 0.15f + 0.2f)
                                }
                                val barHeight = size.height * multiplier
                                val topY = (size.height - barHeight) / 2
                                val barColor = if (i % 2 == 0) NeonCyan else NeonPurple

                                drawLine(
                                    color = barColor.copy(alpha = if (isActive) 0.9f else 0.4f),
                                    start = Offset(x, topY),
                                    end = Offset(x, topY + barHeight),
                                    strokeWidth = barWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                // Chat Messages Stream
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        SciFiChatMessageItem(
                            message = message,
                            assistantName = assistantName,
                            userNick = decryptedNick,
                            onPlaceClick = { place ->
                                NearbyPlacesHelper.openGoogleMapsNavigation(
                                    context = context,
                                    placeName = place.name,
                                    lat = place.lat,
                                    lng = place.lng,
                                    searchQuery = place.searchQuery
                                )
                            },
                            onCallClick = { phone ->
                                NearbyPlacesHelper.makePhoneCall(context, phone)
                            }
                        )
                    }

                    if (isProcessing) {
                        item {
                            SciFiProcessingIndicator(assistantName = assistantName)
                        }
                    }
                }

                // Bottom Cyber Deck & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(CyberBgDark.copy(alpha = 0.8f), CyberBgMid, CyberBgDark)
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(Color.Transparent, NeonCyan.copy(alpha = 0.4f), NeonPurple.copy(alpha = 0.4f), Color.Transparent)),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    // Quick Action Sci-Fi Chips (Tarih, Maarif, Yemek, Migros, vb.)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickPrompts = listOf(
                            "ğŸ“œ Maarif SÄ±nÄ±f GeÃ§me" to "MEB Maarif modeli ve lise sÄ±nÄ±f geÃ§me kurallarÄ± nelerdir?",
                            "ğŸ‡¹ğŸ‡· Lise Tarih KonularÄ±" to "Lise tarih dersi Ã¶nemli konularÄ±nÄ± ve KurtuluÅŸ SavaÅŸÄ±'nÄ± Ã¶zetle",
                            "ğŸ³ Samsun Pidesi Tarifi" to "Samsun pidesi nasÄ±l yapÄ±lÄ±r ayrÄ±ntÄ±lÄ± anlat",
                            "ğŸ›’ Migros Market" to "BugÃ¼n bana en yakÄ±n Migros marketi bul",
                            "ğŸ’Š NÃ¶betÃ§i Eczane" to "Konumuma gÃ¶re en yakÄ±n nÃ¶betÃ§i eczaneleri bul",
                            "ğŸš— Park Yerimi Kaydet" to "Park yerimi kaydet",
                            "â° Randevu & Alarm" to "YarÄ±n saat 09:00 iÃ§in randevu oluÅŸtur",
                            "ğŸ“š Bilgi Ekle" to "Åunu Ã¶ÄŸren: "
                        )
                        items(quickPrompts) { (chipLabel, promptAction) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (promptAction.endsWith(": ")) {
                                            inputText = promptAction
                                        } else {
                                            executeUserPrompt(promptAction)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = chipLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }
                    }

                    // Sci-Fi Text & Mic Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Tarih, Maarif, tarif, mekan veya alarm sor...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                            maxLines = 2,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val textToSend = inputText
                                    inputText = ""
                                    executeUserPrompt(textToSend)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonCyan, NeonBlue)))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "GÃ¶nder", tint = CyberBgDark, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3D Arc Reactor Central Voice Mic Orb
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                isListening = true
                                val greetingPrompt = if (decryptedNick.isNotBlank()) "Seni dinliyorum $decryptedNick..." else "Seni dinliyorum..."
                                if (isVoiceResponsesEnabled) {
                                    TtsHelper.speak(context, greetingPrompt)
                                }
                                startVoiceRecognition(greetingPrompt)
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(52.dp)
                                .scale(if (isListening) pulseScale else 1f)
                                .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = if (isListening) NeonPurple else NeonCyan),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) NeonPurple else NeonCyan
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = CyberBgDark,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isListening) "ğŸ™ï¸ SES ALINIYOR (DÄ°NLENÄ°YOR)..." else if (decryptedNick.isNotBlank()) "ğŸ™ï¸ SENÄ° DÄ°NLÄ°YORUM ${decryptedNick.uppercase()}..." else "ğŸ™ï¸ SENÄ° DÄ°NLÄ°YORUM...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyberBgDark,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showKnowledgeDialog) {
        SciFiKnowledgeDialog(
            viewModel = viewModel,
            knowledgeList = allKnowledge,
            onDismiss = { showKnowledgeDialog = false }
        )
    }
}

@Composable
fun SciFiChatMessageItem(
    message: ChatMessage,
    assistantName: String,
    userNick: String,
    onPlaceClick: (NearbyPlace) -> Unit,
    onCallClick: (String) -> Unit
) {
    val isUser = message.sender == "USER"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 3.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp)
        ) {
            Text(
                text = if (isUser) "[ ${if (userNick.isNotBlank()) userNick.uppercase() else "KULLANICI"} // SESLÄ° GÄ°RDÄ° ]" else "[ $assistantName // NÃ–RAL PROTOKOL ]",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) NeonGreen else NeonCyan,
                letterSpacing = 0.8.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .background(if (isUser) CyberCardUser else CyberCardBg)
                .border(
                    1.dp,
                    if (isUser) NeonGreen.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.4f),
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Normal
            )
        }

        if (!message.actionSummary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF064E3B))
                    .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "âš¡ ${message.actionSummary}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
        }

        if (message.recommendedPlaces.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.recommendedPlaces.forEach { place ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = place.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = place.typeLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonCyan
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ğŸ“ ${place.address}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onPlaceClick(place) },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = CyberBgDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("HARÄ°TA & YOL TARÄ°FÄ°", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberBgDark)
                                }

                                if (!place.phone.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { onCallClick(place.phone) },
                                        modifier = Modifier.weight(0.7f).height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ARA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SciFiProcessingIndicator(assistantName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1B4B))
                .border(1.dp, NeonPurple, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = NeonPurple,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$assistantName dÃ¼ÅŸÃ¼nÃ¼yor ve veri iÅŸliyor...",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
            }
        }
    }
}

@Composable
fun SciFiKnowledgeDialog(
    viewModel: LifeAssistantViewModel,
    knowledgeList: List<AiKnowledgeEntity>,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B132B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("USTA NÃ–RAL HAFIZA MERKEZÄ°", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Text(
                    text = "AsistanÄ±n Usta'ya Ã¶zel bilgiler, notlar ve hatÄ±rlanacak detaylar Ã¶ÄŸretin. Her konuÅŸmada bu hafÄ±za taranÄ±r.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Konu BaÅŸlÄ±ÄŸÄ± (Ã–rn: Aile Doktorum)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text("AÃ§Ä±klama / Bilgi Notu") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.addAiKnowledge(category = "KULLANICI_NOTU", title = newTitle, content = newContent)
                                newTitle = ""
                                newContent = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("HAFIZAYA KAYDET", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyberBgDark)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("KAYITLI HAFIZA LÄ°STESÄ° (${knowledgeList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(knowledgeList) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(item.content, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.deleteAiKnowledge(item.id)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("KAPAT", fontWeight = FontWeight.Bold, color = NeonCyan)
            }
        }
    )
}