package com.example.ui.tabs

import android.media.RingtoneManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CryptoHelper
import com.example.data.ReminderEntity
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.components.ReminderItem
import com.example.ui.components.getIconVectorByName
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import com.example.util.AlarmHelper
import com.example.util.ElderlyVoiceActionButton
import com.example.util.VoiceInputTrailingIcon
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.OcrScannerHelper
import kotlinx.coroutines.launch

fun getFieldsForCategory(category: Category): List<String> {
    return when (category) {
        Category.BILLS_CARDS -> listOf("fatura_veya_kart_turu", "kurum_veya_banka_adi", "abone_veya_kart_no", "son_odeme_tarihi", "tutar_tl")
        Category.MY_CAR -> listOf("plaka_no", "arac_marka_model", "islem_turu_muayene_kasko_sigorta", "son_gecerlilik_tarihi", "servis_istasyonu")
        Category.HEALTH -> listOf("doktor_adi", "hastane_bolum", "dozaj_talimati", "ilac_adi")
        Category.LEGAL -> listOf("dava_dosya_no", "avukat_kurum", "mahkeme_yeri", "belge_listesi")
        Category.DAILY -> listOf("aktivite_yeri", "oncelik_seviyesi", "hedef_sure")
        Category.SOCIAL -> listOf("kisi_veya_grup", "bulusma_yeri", "ozel_not")
        Category.FINANCE -> listOf("odeme_tutari", "hesap_iban", "son_odeme_gunu")
        Category.LOCATION -> listOf("adres_tarifi", "harita_notu", "varis_saati")
        Category.CAREER -> listOf("proje_gorev_adi", "sorumlu_kisi", "teslim_tarihi")
        Category.PERSONAL -> listOf("hedef_konu", "kitap_kurs_adi", "calisma_suresi")
        Category.VEHICLE -> listOf("plaka_veya_arac", "servis_istasyonu", "km_veya_masraf")
        Category.FAMILY_BUDGET -> listOf("harcama_turu", "tutar_tl", "odeme_yontemi", "aciklama")
        Category.GENERAL -> listOf("oncelik_durumu", "ozel_aciklama")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddReminderScreen(
    categoryKey: String,
    initialSubCategoryName: String = "",
    initialDescription: String = "",
    initialTimes: List<String> = emptyList(),
    initialInterval: String = "DAILY",
    viewModel: LifeAssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    // Resolve Category Info (Built-in or Custom)
    val builtInEnum = remember(categoryKey) {
        try {
            Category.valueOf(categoryKey)
        } catch (e: Exception) {
            null
        }
    }

    val customCategoryObj = remember(categoryKey, customCategories) {
        customCategories.find { it.name.equals(categoryKey, ignoreCase = true) || it.id == categoryKey }
    }

    val categoryDisplayName = remember(builtInEnum, customCategoryObj, categoryKey) {
        builtInEnum?.displayName ?: customCategoryObj?.name ?: categoryKey
    }

    val categoryColor = remember(builtInEnum, customCategoryObj) {
        if (builtInEnum != null) {
            builtInEnum.color
        } else if (customCategoryObj != null) {
            try {
                Color(android.graphics.Color.parseColor(customCategoryObj.colorHex))
            } catch (e: Exception) {
                OrangePrimary
            }
        } else {
            OrangePrimary
        }
    }

    val categoryIcon = remember(builtInEnum, customCategoryObj) {
        if (builtInEnum != null) {
            builtInEnum.icon
        } else if (customCategoryObj != null) {
            getIconVectorByName(customCategoryObj.iconName)
        } else {
            Icons.Default.Star
        }
    }

    val fields = remember(builtInEnum, customCategoryObj) {
        if (builtInEnum != null) {
            getFieldsForCategory(builtInEnum)
        } else if (customCategoryObj != null && customCategoryObj.customFields.isNotEmpty()) {
            customCategoryObj.customFields
        } else {
            listOf("ozel_not", "oncelik_durumu")
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    var title by remember { mutableStateOf(initialSubCategoryName) }
    var customNote by remember { mutableStateOf(initialDescription) }
    var isFavorite by remember { mutableStateOf(false) }

    // Repetition & Date interval
    // Options: "ONCE", "DAILY", "PRAYER_BASED", "RANGE_7", "RANGE_14", "RANGE_30"
    var selectedRecurrence by remember { mutableStateOf(if (initialInterval.isNotBlank()) initialInterval else "DAILY") }
    var selectedDateStr by remember { mutableStateOf(dateFormat.format(Date())) }

    // Multiple Times Management (At least 5 times can be added / removed dynamically)
    val selectedTimes = remember {
        val list = mutableStateListOf<String>()
        if (initialTimes.isNotEmpty()) {
            list.addAll(initialTimes)
        } else {
            list.addAll(listOf("08:30", "13:00", "18:00"))
        }
        list
    }
    var newTimeInput by remember { mutableStateOf("") }
    var showTimePickerAdd by remember { mutableStateOf(false) }

    // Alarm Sound Options: CLASSIC_BELL, CALM_MELODY, DIGITAL_SIREN
    var selectedAlarmSound by remember { mutableStateOf("CLASSIC_BELL") }

    // Early reminder / advance notification days (e.g. 0, 1, 3, 7, 15, 30 or custom manual input)
    var earlyReminderDays by remember { mutableStateOf("3") }

    val fieldValues = remember { mutableStateMapOf<String, String>() }

    val categoryReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val filteredReminders = categoryReminders.filter { 
        it.category.equals(categoryKey, ignoreCase = true) || 
        it.category.equals(categoryDisplayName, ignoreCase = true) ||
        (builtInEnum != null && it.category == builtInEnum.name)
    }.sortedBy { it.dueDateMillis }

    val coroutineScope = rememberCoroutineScope()
    var isOcrScanning by remember { mutableStateOf(false) }

    val ocrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isOcrScanning = true
                val result = OcrScannerHelper.scanImage(context, uri)
                if (result != null) {
                    if (result.title.isNotBlank()) title = result.title
                    if (result.amount != null) {
                        customNote = if (customNote.isBlank()) "Tutar: ${result.amount}" else "$customNote - Tutar: ${result.amount}"
                    }
                    if (result.dateMillis != null && result.dateMillis > System.currentTimeMillis()) {
                        selectedDateStr = dateFormat.format(Date(result.dateMillis))
                    }
                }
                isOcrScanning = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F2ED),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(categoryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Yeni Hatırlatıcı Ekle",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                            Text(
                                text = categoryDisplayName,
                                fontSize = 12.sp,
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Slate900)
                    }
                },
                actions = {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) Color(0xFFEF4444) else Slate700
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F2ED))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // Elder-friendly prominent voice dictation and OCR Scanner action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ElderlyVoiceActionButton(
                        label = "🎙️ Sesle Söyle",
                        prompt = "Lütfen hatırlatma başlığı ve notunuzu mikrofona söyleyin...",
                        onSpeechResult = { text ->
                            if (title.isBlank()) {
                                title = text
                            } else {
                                customNote = if (customNote.isBlank()) text else "$customNote $text"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { ocrImagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        if (isOcrScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📸 Fatura Tara", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Temel Bilgiler (İsim & Not)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    elevation = 4.dp,
                    glowColor = categoryColor,
                    contentPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "1. Randevu / Hatırlatıcı Başlığı & Not",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Title Input with Voice Trailing Icon
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Hatırlatıcı İsmi", fontWeight = FontWeight.Bold, color = Slate900) },
                            placeholder = { Text("Örn: Tansiyon Ölçümü, Su İçme, vb.", color = Slate700) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = categoryColor)
                            },
                            trailingIcon = {
                                VoiceInputTrailingIcon(
                                    prompt = "Başlığı söyleyin...",
                                    onSpeechResult = { title = it }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = categoryColor,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Note Input with Voice Trailing Icon
                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("Açıklama & Özel Notlar", fontWeight = FontWeight.Bold, color = Slate900) },
                            placeholder = { Text("Unutulmaması gereken ayrıntıları buraya yazın...", color = Slate700) },
                            minLines = 2,
                            maxLines = 4,
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null, tint = categoryColor)
                            },
                            trailingIcon = {
                                VoiceInputTrailingIcon(
                                    prompt = "Açıklamayı söyleyin...",
                                    onSpeechResult = { spoken ->
                                        customNote = if (customNote.isBlank()) spoken else "$customNote $spoken"
                                    }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = categoryColor,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Tarih & Tekrar Periyodu Seçimi
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    elevation = 4.dp,
                    glowColor = categoryColor,
                    contentPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "2. Tarih & Tekrar Planlaması",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        Text(
                            text = "Hatırlatıcının hangi periyotta veya vakitlerde çalacağını belirleyin.",
                            fontSize = 11.sp,
                            color = Slate700,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        // Chips for recurrence
                        val recurrenceOptions = listOf(
                            "DAILY" to "🔁 Her Gün Düzenli",
                            "ONCE" to "📅 Tek Seferlik Tarih",
                            "PRAYER_BASED" to "🕌 Vakit Bazlı (Sabah/Öğle/Akşam/Yatsı)",
                            "RANGE_7" to "🗓️ 7 Gün Boyunca",
                            "RANGE_14" to "🗓️ 14 Gün Boyunca",
                            "RANGE_30" to "🗓️ 1 Ay Boyunca"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recurrenceOptions) { (key, label) ->
                                val isSelected = selectedRecurrence == key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) categoryColor else Color(0xFFE2E8F0))
                                        .clickable { selectedRecurrence = key }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Slate800
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // If single date, show date selector field
                        if (selectedRecurrence == "ONCE") {
                            OutlinedTextField(
                                value = selectedDateStr,
                                onValueChange = { selectedDateStr = it },
                                label = { Text("Tarih (GG.AA.YYYY)", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = categoryColor)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = categoryColor,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        } else if (selectedRecurrence == "PRAYER_BASED") {
                            Text(
                                text = "🕌 Sabah, Öğle, İkindi, Akşam ve Yatsı ezan vakitlerine otomatik senkronize edilir.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE6FFFA))
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Çoklu Saat Yönetimi (En az 5 saat veya daha fazlası eklenebilir)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    elevation = 4.dp,
                    glowColor = categoryColor,
                    contentPadding = 16.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "3. Hatırlatma Saatleri (${selectedTimes.size} Saat)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900
                                )
                                Text(
                                    text = "Günde 5 veya daha fazla farklı alarm saati ekleyebilirsiniz.",
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Time Chips with Delete option
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            selectedTimes.forEach { timeStr ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(categoryColor.copy(alpha = 0.15f))
                                        .border(1.dp, categoryColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Alarm, contentDescription = null, tint = categoryColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(timeStr, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Slate900)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Kaldır",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    if (selectedTimes.size > 1) {
                                                        selectedTimes.remove(timeStr)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fast Presets and Manual Add
                        Text("Hızlı Saat Ekle:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            val presets = listOf("07:30", "08:30", "11:30", "13:00", "15:30", "18:00", "20:30", "22:30")
                            items(presets) { p ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .clickable {
                                            if (!selectedTimes.contains(p)) {
                                                selectedTimes.add(p)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("+$p", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TurquoiseSecondary.copy(alpha = 0.2f))
                                        .clickable {
                                            // Quick Add 5 standard water/medicine times
                                            val fiveTimes = listOf("08:00", "11:30", "15:00", "18:30", "21:30")
                                            fiveTimes.forEach { t ->
                                                if (!selectedTimes.contains(t)) selectedTimes.add(t)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("⚡ 5 Doz/Saat Doldur", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom time input field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTimeInput,
                                onValueChange = { newTimeInput = it },
                                label = { Text("Özel Saat (HH:mm)") },
                                placeholder = { Text("16:45") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (newTimeInput.isNotBlank() && !selectedTimes.contains(newTimeInput.trim())) {
                                        selectedTimes.add(newTimeInput.trim())
                                        newTimeInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = categoryColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Saat Ekle")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Alarm Sesi Seçimi (3 Farklı Alarm Sesi)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    elevation = 4.dp,
                    glowColor = categoryColor,
                    contentPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "4. Alarm Sesi ve Tonu Seçimi (3 Farklı Ton)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        Text(
                            text = "Alarm çaldığında cihazınızın vereceği ses ve titreşim tarzını seçin.",
                            fontSize = 11.sp,
                            color = Slate700,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        val sounds = listOf(
                            Triple("CLASSIC_BELL", "🔔 1. Klasik Çan & Standart Zil", "Dengeli melodik bildirim çanı ve çift titreşim"),
                            Triple("CALM_MELODY", "🎵 2. Sakin Huzur Melodisi", "Yumuşak tınılı, rahatsız etmeyen nazik hatırlatma"),
                            Triple("DIGITAL_SIREN", "🚨 3. Güçlü Siren & Dijital Alarm", "Yüksek sesli, acil durum ve doz kaçırmayan alarm")
                        )

                        sounds.forEach { (soundKey, titleText, descText) ->
                            val isSelected = selectedAlarmSound == soundKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) categoryColor.copy(alpha = 0.12f) else Color(0xFFF8FAFC))
                                    .border(1.dp, if (isSelected) categoryColor else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedAlarmSound = soundKey
                                        // Play preview tone
                                        try {
                                            val uri = when (soundKey) {
                                                "DIGITAL_SIREN" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                            }
                                            val r = RingtoneManager.getRingtone(context, uri)
                                            r?.play()
                                        } catch (e: Exception) {}
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = titleText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Slate900 else Slate800
                                    )
                                    Text(
                                        text = descText,
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedAlarmSound = soundKey },
                                    colors = RadioButtonDefaults.colors(selectedColor = categoryColor)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Erken Uyarı & Önceden Bildirim (Kaç Gün Önce Uyaralım?)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    elevation = 4.dp,
                    glowColor = Color(0xFFF59E0B),
                    contentPadding = 16.dp
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "5. Erken Hatırlatma (Kaç Gün Önce?)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900
                                )
                            }
                        }

                        Text(
                            text = "Fatura son ödeme, araç muayene/kasko ve randevulardan kaç gün önce bildirim verilsin?",
                            fontSize = 11.sp,
                            color = Slate700,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )

                        // Quick selection chips
                        val dayOptions = listOf(
                            "0" to "Aynı Gün",
                            "1" to "1 Gün Önce",
                            "2" to "2 Gün Önce",
                            "3" to "3 Gün Önce",
                            "7" to "7 Gün Önce",
                            "15" to "15 Gün Önce",
                            "30" to "30 Gün (1 Ay)"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dayOptions) { (daysStr, label) ->
                                val isSelected = earlyReminderDays == daysStr
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFD97706) else Color(0xFFE2E8F0))
                                        .clickable { earlyReminderDays = daysStr }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Slate800
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manual days input with voice input
                        OutlinedTextField(
                            value = earlyReminderDays,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    earlyReminderDays = it
                                }
                            },
                            label = { Text("Manuel Kaç Gün Önce Uyaralım? (Örn: 3)") },
                            placeholder = { Text("Gün sayısı girin (Örn: 5)") },
                            trailingIcon = {
                                VoiceInputTrailingIcon(
                                    prompt = "Kaç gün önce olduğunu söyleyin...",
                                    onSpeechResult = { spoken ->
                                        val digits = spoken.filter { it.isDigit() }
                                        if (digits.isNotEmpty()) {
                                            earlyReminderDays = digits
                                        }
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Kategoriye Özel Detay Alanları (Varsa)
                if (fields.isNotEmpty()) {
                    EmbossedCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        elevation = 4.dp,
                        contentPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                text = "6. $categoryDisplayName Ek Detay Alanları",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            fields.forEach { fieldName ->
                                val labelText = fieldName.replace("_", " ").replaceFirstChar { it.uppercase() }

                                OutlinedTextField(
                                    value = fieldValues[fieldName] ?: "",
                                    onValueChange = { fieldValues[fieldName] = it },
                                    label = { Text(labelText, fontWeight = FontWeight.SemiBold, color = Slate800) },
                                    trailingIcon = {
                                        VoiceInputTrailingIcon(
                                            prompt = "$labelText alanını söyleyin...",
                                            onSpeechResult = { fieldValues[fieldName] = it }
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = categoryColor,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedTextColor = Slate900,
                                        unfocusedTextColor = Slate900,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 7. Kaydet ve Alarmları Kur Butonu
                Button(
                    onClick = {
                        val mergedMetadataMap = mutableMapOf<String, String>()
                        mergedMetadataMap.putAll(fieldValues)
                        mergedMetadataMap["alarm_sound"] = selectedAlarmSound
                        mergedMetadataMap["recurrence"] = selectedRecurrence
                        mergedMetadataMap["times_list"] = selectedTimes.joinToString(",")
                        mergedMetadataMap["early_reminder_days"] = earlyReminderDays

                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                        val jsonAdapter = moshi.adapter(Map::class.java)
                        val jsonMetadata = jsonAdapter.toJson(mergedMetadataMap.toMap())
                        val encryptedMetadata = CryptoHelper.encrypt(jsonMetadata)

                        val firstTimeStr = selectedTimes.firstOrNull() ?: "09:00"
                        val dueDatetimeStr = "$selectedDateStr $firstTimeStr"

                        var primaryMillis = System.currentTimeMillis() + 60000
                        try {
                            val parsedDate = fullDateFormat.parse(dueDatetimeStr)
                            if (parsedDate != null && parsedDate.time > System.currentTimeMillis()) {
                                primaryMillis = parsedDate.time
                            } else {
                                val cal = Calendar.getInstance()
                                val timeParts = firstTimeStr.split(":")
                                cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 9)
                                cal.set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                                cal.set(Calendar.SECOND, 0)
                                if (cal.timeInMillis <= System.currentTimeMillis()) {
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                primaryMillis = cal.timeInMillis
                            }
                        } catch (e: Exception) {}

                        val entity = ReminderEntity(
                            category = builtInEnum?.name ?: categoryDisplayName,
                            title = title.trim(),
                            dueDatetime = dueDatetimeStr,
                            dueDateMillis = primaryMillis,
                            customNote = customNote.trim(),
                            isFavorite = isFavorite,
                            encryptedMetadata = encryptedMetadata,
                            actionStep = "Execute"
                        )
                        viewModel.addReminderWithSubAlarms(
                            reminder = entity,
                            earlyDays = earlyReminderDays.toIntOrNull() ?: 0,
                            times = selectedTimes,
                            sound = selectedAlarmSound
                        )

                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    enabled = title.isNotBlank() && selectedTimes.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = categoryColor,
                        disabledContainerColor = Color(0xFFCBD5E1)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hatırlatıcıyı & ${selectedTimes.size} Alarmı Kaydet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Existing Reminders Header
                Text(
                    text = "Bu Kategorideki Kayıtlar (${filteredReminders.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (filteredReminders.isEmpty()) {
                item {
                    EmbossedCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 10.dp,
                        elevation = 2.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Bu kategoride henüz kayıtlı hatırlatıcı yok.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                items(filteredReminders) { rem ->
                    ReminderItem(
                        reminder = rem,
                        onFavoriteClick = { viewModel.toggleFavorite(rem.id, rem.isFavorite) },
                        onClick = {},
                        onDeleteClick = { viewModel.deleteReminder(rem.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
