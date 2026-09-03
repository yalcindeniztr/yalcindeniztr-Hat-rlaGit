package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiKnowledgeEntity
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import com.example.util.AiAssistantService
import com.example.util.NearbyPlace
import com.example.util.NearbyPlacesHelper
import com.example.util.TtsHelper
import com.example.util.rememberVoiceRecognizer
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val recommendedPlaces: List<NearbyPlace> = emptyList(),
    val actionSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

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
    var showKnowledgeDialog by remember { mutableStateOf(false) }

    val initialGreeting = remember(decryptedNick, assistantName) {
        if (decryptedNick.isNotBlank()) {
            "Merhaba $decryptedNick! Ben akıllı asistanınız $assistantName. Size nöbetçi eczane, hastane veya otopark bulabilir, sesle randevu ve alarm kurabilirim. Size nasıl yardımcı olabilirim?"
        } else {
            "Merhaba! Ben HatırlaGit Yapay Zeka Asistanınız $assistantName. Size daha iyi yardımcı olabilmem için adınızı öğrenebilir miyim?"
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

    // Voice recognition launcher
    val startVoiceRecognition = rememberVoiceRecognizer { recognizedText ->
        isListening = false
        if (recognizedText.isNotBlank()) {
            val userMsg = ChatMessage(sender = "USER", text = recognizedText)
            messages.add(userMsg)
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                isProcessing = true
                val response = AiAssistantService.processUserMessage(
                    context = context,
                    userMessage = recognizedText,
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

                if (isVoiceResponsesEnabled && response.isSpeechReady) {
                    TtsHelper.speak(context, response.replyText)
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = assistantName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "v1.1.4",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C3AED)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (decryptedNick.isNotBlank()) "Aktif • $decryptedNick" else "Google Gemini Destekli",
                                    fontSize = 11.sp,
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Slate900)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAiVoiceResponses(!isVoiceResponsesEnabled) }) {
                        Icon(
                            imageVector = if (isVoiceResponsesEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Sesli Yanıt",
                            tint = if (isVoiceResponsesEnabled) Color(0xFF8B5CF6) else Slate700
                        )
                    }
                    IconButton(onClick = { showKnowledgeDialog = true }) {
                        Icon(Icons.Default.LocalLibrary, contentDescription = "Kütüphane", tint = Color(0xFF8B5CF6))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF1F5F9))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Chat Messages List (Expanded weight)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(message = msg)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isProcessing) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF8B5CF6), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Düşünüyor ve kütüphaneyi araştırıyor...", fontSize = 12.sp, color = Slate700, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
            }

            // Bottom Action Area (Pinned, 100% visible, safe from system navigation pill)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = Color(0xFFF1F5F9),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 20.dp)
                ) {
                    // Quick Action Prompt Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickPrompts = listOf(
                            "💊 Nöbetçi Eczane" to "Konumuma göre en yakın nöbetçi eczaneleri bul",
                            "🏥 En Yakın Hastane" to "En yakın hastaneyi göster ve yol tarifi ver",
                            "🅿️ Otopark Bul" to "Yakındaki otoparkları listele",
                            "⏰ Sesli Alarm Kur" to "Yarın saat 09:00 için alarm kur",
                            "📚 Bilgi Ekle" to "Şunu öğren: "
                        )
                        items(quickPrompts) { (chipLabel, promptAction) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (promptAction.endsWith(": ")) {
                                            inputText = promptAction
                                        } else {
                                            val userMsg = ChatMessage(sender = "USER", text = promptAction)
                                            messages.add(userMsg)
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(messages.size - 1)
                                                isProcessing = true
                                                val response = AiAssistantService.processUserMessage(
                                                    context = context,
                                                    userMessage = promptAction,
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
                                                if (isVoiceResponsesEnabled) {
                                                    TtsHelper.speak(context, response.replyText)
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(text = chipLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            }
                        }
                    }

                    // Input Text Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Asistanınıza sorun veya öğretin...", fontSize = 12.sp, color = Slate700) },
                            maxLines = 2,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF8B5CF6),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val textToSend = inputText
                                    inputText = ""
                                    val userMsg = ChatMessage(sender = "USER", text = textToSend)
                                    messages.add(userMsg)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                        isProcessing = true
                                        val response = AiAssistantService.processUserMessage(
                                            context = context,
                                            userMessage = textToSend,
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
                                        if (isVoiceResponsesEnabled) {
                                            TtsHelper.speak(context, response.replyText)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 🌟 BÜYÜK YUVARLAK "SENİ DİNLİYORUM" BUTONU (TEK TIKLA DOĞRUDAN DİNLEME)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                val promptText = if (decryptedNick.isNotBlank()) "Seni dinliyorum $decryptedNick..." else "Seni dinliyorum..."
                                startVoiceRecognition(promptText)
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (decryptedNick.isNotBlank()) "🎙️ Seni Dinliyorum $decryptedNick..." else "🎙️ Seni Dinliyorum...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    // Knowledge Base Management Modal
    if (showKnowledgeDialog) {
        var newFactInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showKnowledgeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalLibrary, contentDescription = null, tint = Color(0xFF8B5CF6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🧠 Asistan Kütüphanesi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Asistanınızın analiz ettiği ve öğrendiği tüm notlar burada şifreli olarak saklanır. Asistanınıza yeni bilgiler öğretebilirsiniz.",
                        fontSize = 12.sp,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newFactInput,
                        onValueChange = { newFactInput = it },
                        label = { Text("Yeni Bilgi Öğret", fontSize = 12.sp) },
                        placeholder = { Text("Örn: Su faturası abone no: 994812", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (newFactInput.isNotBlank()) {
                                viewModel.addAiKnowledge(
                                    title = "Kullanıcı Notu",
                                    content = newFactInput.trim()
                                )
                                newFactInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Kütüphaneye Ekle", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Kayıtlı Bilgiler (${allKnowledge.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(allKnowledge) { k ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(k.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                    Text(k.content, fontSize = 10.sp, color = Slate700)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteAiKnowledge(k.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKnowledgeDialog = false }) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.sender == "USER"
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ))
                    .background(
                        if (isUser) Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                        else Brush.linearGradient(listOf(Color.White, Color(0xFFF8FAFC)))
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) Color.Transparent else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (isUser) Color.White else Slate900,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action Executed Summary Badge (if any)
        if (!message.actionSummary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .padding(start = 34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFDCFCE7))
                    .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = message.actionSummary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534)
                )
            }
        }

        // Recommended Places Cards (if any)
        if (message.recommendedPlaces.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 34.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.recommendedPlaces.forEach { place ->
                    EmbossedCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        elevation = 3.dp,
                        contentPadding = 10.dp
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
                                    color = Slate900,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${place.distanceMeters} m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${place.typeLabel} • ${place.address}",
                                fontSize = 11.sp,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Google Maps Navigation Button
                                Button(
                                    onClick = {
                                        NearbyPlacesHelper.openGoogleMapsNavigation(context, place.name, place.lat, place.lng, place.searchQuery)
                                    },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Yol Tarifi", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                // Phone Call Button (if phone exists)
                                if (!place.phone.isNullOrBlank()) {
                                    Button(
                                        onClick = {
                                            NearbyPlacesHelper.makePhoneCall(context, place.phone)
                                        },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ara", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
