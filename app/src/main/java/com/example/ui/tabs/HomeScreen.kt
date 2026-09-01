package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReminderEntity
import com.example.ui.LifeAssistantViewModel
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import com.example.ui.components.CategoryUnlockDialog
import com.example.ui.components.EditReminderDialog
import com.example.ui.components.EmbossedCard
import com.example.ui.components.QuickNoteDialog
import com.example.ui.components.ReminderItem
import com.example.ui.components.ReorderBlocksDialog
import com.example.ui.components.getIconVectorByName
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import kotlinx.coroutines.launch

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
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val favoriteCategories by viewModel.favoriteCategories.collectAsStateWithLifecycle()
    val lockedCategories by viewModel.lockedCategories.collectAsStateWithLifecycle()
    val parkedCarLat by viewModel.parkedCarLat.collectAsStateWithLifecycle()
    val homeBlockOrder by viewModel.homeBlockOrder.collectAsStateWithLifecycle()
    val prayerTimingData by viewModel.prayerTimingData.collectAsStateWithLifecycle()

    var showReorderDialog by remember { mutableStateOf(false) }
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var showQuickNoteDialog by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderToDelete by remember { mutableStateOf<ReminderEntity?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var unlockTargetCategoryKey by remember { mutableStateOf<String?>(null) }
    var unlockTargetCategoryName by remember { mutableStateOf("") }
    var isUnlockError by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var lockConfigCategory by remember { mutableStateOf<UnifiedCategory?>(null) }

    if (showCreateDialog) {
        com.example.ui.components.CreateCustomCategoryDialog(
            onDismiss = { showCreateDialog = false },
            onCategoryCreated = { name, colorHex, iconName, fields ->
                viewModel.addCustomCategory(name, colorHex, iconName, fields)
            }
        )
    }

    if (lockConfigCategory != null) {
        val cat = lockConfigCategory!!
        val isCurrentlyLocked = lockedCategories.contains(cat.key)
        com.example.ui.components.CategoryLockDialog(
            categoryName = cat.displayName,
            isCurrentlyLocked = isCurrentlyLocked,
            onDismiss = { lockConfigCategory = null },
            onSaveLock = { pin, isLocked ->
                viewModel.setCategoryLock(cat.key, pin, isLocked)
                lockConfigCategory = null
            }
        )
    }

    if (unlockTargetCategoryKey != null) {
        CategoryUnlockDialog(
            categoryName = unlockTargetCategoryName,
            isError = isUnlockError,
            onDismiss = {
                unlockTargetCategoryKey = null
                isUnlockError = false
            },
            onVerifyPin = { enteredPin ->
                scope.launch {
                    val key = unlockTargetCategoryKey ?: return@launch
                    val isValid = viewModel.verifyCategoryPin(key, enteredPin)
                    if (isValid) {
                        isUnlockError = false
                        unlockTargetCategoryKey = null
                        onNavigateToCategory(key)
                    } else {
                        isUnlockError = true
                    }
                }
            }
        )
    }

    // Build unified category list for status badges
    val allCategories = remember(customCategories) {
        val list = mutableListOf<UnifiedCategory>()
        Category.values().forEach { cat ->
            list.add(
                UnifiedCategory(
                    key = cat.name,
                    displayName = cat.displayName,
                    color = cat.color,
                    icon = cat.icon
                )
            )
        }
        customCategories.forEach { custom ->
            val color = try {
                Color(android.graphics.Color.parseColor(custom.colorHex))
            } catch (e: Exception) {
                OrangePrimary
            }
            list.add(
                UnifiedCategory(
                    key = custom.name,
                    displayName = custom.name,
                    color = color,
                    icon = getIconVectorByName(custom.iconName),
                    isCustom = true
                )
            )
        }
        list
    }

    // Counts per category
    val categoryCounts = remember(allReminders) {
        allReminders.groupingBy { it.category }.eachCount()
    }

    // Filtered or all user reminders grouped by category
    val remindersGroupedByCategory = remember(allReminders, selectedCategoryFilter) {
        val filtered = if (selectedCategoryFilter != null) {
            allReminders.filter { it.category == selectedCategoryFilter }
        } else {
            allReminders
        }
        filtered.groupBy { it.category }
    }

    if (showQuickNoteDialog) {
        QuickNoteDialog(
            customCategories = customCategories,
            onDismiss = { showQuickNoteDialog = false },
            onSaveNote = { reminder ->
                viewModel.addReminder(reminder)
                showQuickNoteDialog = false
            }
        )
    }

    if (showAddBlockDialog) {
        val availableCategories = allCategories.filter { it.key !in homeBlockOrder }
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Kategori Ekle", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(availableCategories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    val newList = homeBlockOrder.toMutableList()
                                    val addIndex = newList.indexOf("ADD_NEW")
                                    if (addIndex != -1) {
                                        newList.add(addIndex, cat.key)
                                    } else {
                                        newList.add(cat.key)
                                    }
                                    viewModel.updateHomeBlockOrder(newList)
                                    showAddBlockDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(cat.displayName, fontSize = 16.sp)
                        }
                    }
                    if (availableCategories.isEmpty()) {
                        item {
                            Text("Eklenebilecek yeni kategori kalmadı.", color = Slate700)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddBlockDialog = false }) { Text("Kapat") }
            }
        )
    }

    if (showAiAssistant) {
        androidx.activity.compose.BackHandler {
            showAiAssistant = false
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .zIndex(99f)
        ) {
            AiAssistantScreen(
                viewModel = viewModel,
                onNavigateBack = { showAiAssistant = false }
            )
        }
    }

    if (showReorderDialog) {
        ReorderBlocksDialog(
            currentOrder = homeBlockOrder,
            onDismiss = { showReorderDialog = false },
            onSave = { newOrder ->
                viewModel.updateHomeBlockOrder(newOrder)
                showReorderDialog = false
            }
        )
    }

    if (editingReminder != null) {
        EditReminderDialog(
            reminder = editingReminder!!,
            customCategories = customCategories,
            onDismiss = { editingReminder = null },
            onSave = { updated ->
                viewModel.updateReminder(updated)
                editingReminder = null
            }
        )
    }

    if (reminderToDelete != null) {
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Hatırlatıcıyı Sil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = "\"${reminderToDelete?.title}\" adlı hatırlatıcıyı silmek istediğinize emin misiniz?",
                    fontSize = 14.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderToDelete?.let { viewModel.deleteReminder(it.id) }
                        reminderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text("Vazgeç", color = Slate700, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // 🕌 YAKLAŞAN EZAN VAKTİ KARTI (KOMPAKT)
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

                Spacer(modifier = Modifier.height(10.dp))
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

        // --- YAKLAŞAN RANDEVULARINIZ (KOMPAKT & ALAN KAZANDIRAN DİZAYN) ---
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFFF6D00), Color(0xFFFF9100)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yaklaşan Randevularınız",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                }
                TextButton(onClick = onNavigateToAllReminders, contentPadding = PaddingValues(0.dp)) {
                    Text("Tümünü Gör", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        val upcomingReminders = allReminders
            .filter { it.dueDateMillis > System.currentTimeMillis() }
            .sortedBy { it.dueDateMillis }
            .take(3)

        if (upcomingReminders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE2E8F0).copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "📌 Yaklaşan randevunuz bulunmuyor.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700
                    )
                }
            }
        } else {
            items(upcomingReminders, key = { it.id }) { reminder ->
                EmbossedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    cornerRadius = 10.dp,
                    elevation = 2.dp,
                    contentPadding = 8.dp,
                    onClick = { editingReminder = reminder }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🗓️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reminder.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reminder.dueDatetime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { reminderToDelete = reminder },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // 🌟 3D KABARTMALI PARLAK ASİSTAN HERO BUTONU
        item {
            Spacer(modifier = Modifier.height(10.dp))
            val currentAssistantName by viewModel.aiAssistantName.collectAsStateWithLifecycle()
            
            EmbossedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                cornerRadius = 20.dp,
                elevation = 6.dp,
                glowColor = Color(0xFF8B5CF6),
                borderBrush = Brush.horizontalGradient(
                    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6))
                ),
                onClick = { showAiAssistant = true },
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFF4C1D95))
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentAssistantName.uppercase(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Yapay Zeka", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                }
                            }
                            Text(
                                text = "Konuşun • Nöbetçi Eczane, Hastane, Sesli Alarm",
                                fontSize = 11.sp,
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))

            // KATEGORİLER & İŞLEMLER MENÜSÜ BAŞLIĞI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kategoriler & İşlemler",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                TextButton(onClick = { showReorderDialog = true }) {
                    Text("Düzenle", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // GÖSTERİŞLİ, 3 SÜTUNLU KABARTMALI İŞLEM KUTULARI
            val filteredBlocks = homeBlockOrder.filter { it != "ALL" && it != "REMINDERS" }
            val triples = filteredBlocks.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                triples.forEach { triple ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        triple.forEach { blockKey ->
                            RenderBlock(
                                blockKey = blockKey,
                                modifier = Modifier.weight(1f),
                                parkedCarLat = parkedCarLat,
                                allRemindersSize = allReminders.size,
                                favoriteCategoriesSize = favoriteCategories.size,
                                lockedCategories = lockedCategories,
                                allCategories = allCategories,
                                onAction = { action ->
                                    when (action) {
                                        "BILLS_CARDS" -> {
                                            if (lockedCategories.contains("BILLS_CARDS")) {
                                                unlockTargetCategoryKey = "BILLS_CARDS"
                                                unlockTargetCategoryName = "Faturalar & Kartlar"
                                                isUnlockError = false
                                            } else {
                                                onNavigateToCategory("BILLS_CARDS")
                                            }
                                        }
                                        "MY_CAR" -> {
                                            if (lockedCategories.contains("MY_CAR")) {
                                                unlockTargetCategoryKey = "MY_CAR"
                                                unlockTargetCategoryName = "Arabam"
                                                isUnlockError = false
                                            } else {
                                                onNavigateToCategory("MY_CAR")
                                            }
                                        }
                                        "QUICK_NOTE" -> showQuickNoteDialog = true
                                        "VOICE_NOTE" -> onNavigateToVoiceNotes()
                                        "PARK" -> onNavigateToParkScreen()
                                        "LOCATIONS" -> onNavigateToLocations()
                                        "FAVORITES" -> onNavigateToFavoriteCategories()
                                        "ADD_NEW" -> showAddBlockDialog = true
                                        else -> {
                                            val cat = allCategories.find { it.key == action }
                                            if (cat != null) {
                                                if (lockedCategories.contains(action)) {
                                                    unlockTargetCategoryKey = action
                                                    unlockTargetCategoryName = cat.displayName
                                                    isUnlockError = false
                                                } else {
                                                    onNavigateToCategory(action)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        // Fill empty slots in the row if less than 3 items
                        val remainder = 3 - triple.size
                        repeat(remainder) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }


            // --- KATEGORİLER VE YENİ ÖZEL KATEGORİ OLUŞTUR ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Randevu & Hatırlatıcı Kategorileri",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                EmbossedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(bottom = 12.dp),
                    cornerRadius = 14.dp,
                    elevation = 5.dp,
                    glowColor = OrangePrimary,
                    borderBrush = Brush.horizontalGradient(listOf(OrangePrimary, TurquoiseSecondary)),
                    onClick = { showCreateDialog = true },
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFF4500)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Yeni Özel Kategori Oluştur",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "İhtiyacınıza göre özelleştirin",
                                    fontSize = 12.sp,
                                    color = Slate700
                                )
                            }
                        }
                    }
                }
            }

            // Kategori gridi (3 sütunlu kompakt dizilim)
            val chunkedCategories = allCategories.chunked(3)
            chunkedCategories.forEach { triple ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        triple.forEach { cat ->
                            EmbossedCard(
                                modifier = Modifier.weight(1f).height(56.dp),
                                cornerRadius = 12.dp,
                                elevation = 3.dp,
                                glowColor = cat.color,
                                onClick = {
                                    if (lockedCategories.contains(cat.key)) {
                                        unlockTargetCategoryKey = cat.key
                                        unlockTargetCategoryName = cat.displayName
                                        isUnlockError = false
                                    } else {
                                        onNavigateToCategory(cat.key)
                                    }
                                },
                                contentPadding = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(cat.color, cat.color.copy(alpha = 0.8f))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (lockedCategories.contains(cat.key)) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Kilitli",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(13.dp).padding(end = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        val remainder = 3 - triple.size
                        repeat(remainder) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
    }
}

/**
 * Üstte yer alan kompakt, kabartmalı ve parlayan kategori durum yuvarlağı
 */

@Composable
fun CategoryEmbossedCircleBadge(
    category: UnifiedCategory,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = shape,
                spotColor = category.color.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(category.color, category.color.copy(alpha = 0.85f))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFF8F9FA))
                    )
                }
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.displayName,
                tint = if (isSelected) Color.White else category.color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.displayName,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) Color.White else Slate700
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else category.color.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.White else category.color
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBlock(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    gradientColors: List<Color> = listOf(Color(0xFF333333), Color(0xFF111111)),
    textColor: Color = Color.White,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val ovalShape = RoundedCornerShape(18.dp)
    
    Box(
        modifier = modifier
            .height(96.dp)
            .shadow(
                elevation = 5.dp,
                shape = ovalShape,
                spotColor = gradientColors.first().copy(alpha = 0.5f)
            )
            .clip(ovalShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            )
            .drawBehind {
                val highlightHeight = size.height * 0.4f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = highlightHeight
                    )
                )
            }
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Black.copy(alpha = 0.1f)
                    )
                ),
                shape = ovalShape
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLocked) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "Kilitli",
                        tint = textColor,
                        modifier = Modifier.size(12.dp).padding(end = 2.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor.copy(alpha = 0.98f)
                    )
                }
            }
        }
    }
}

@Composable
fun RenderBlock(
    lockedCategories: Set<String>,
    allCategories: List<UnifiedCategory>,
    blockKey: String,
    modifier: Modifier = Modifier,
    parkedCarLat: String?,
    allRemindersSize: Int,
    favoriteCategoriesSize: Int,
    onAction: (String) -> Unit
) {
    when (blockKey) {
        "BILLS_CARDS" -> CategoryBlock(
            title = "Faturalar & Kartlar",
            icon = Icons.Default.ReceiptLong,
            gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            textColor = Color.White,
            modifier = modifier,
            isLocked = lockedCategories.contains("BILLS_CARDS"),
            onClick = { onAction("BILLS_CARDS") }
        )
        "MY_CAR" -> CategoryBlock(
            title = "Arabam",
            icon = Icons.Default.DirectionsCar,
            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            textColor = Color.White,
            modifier = modifier,
            isLocked = lockedCategories.contains("MY_CAR"),
            onClick = { onAction("MY_CAR") }
        )
        "QUICK_NOTE" -> CategoryBlock(
            title = "Hızlı Not",
            icon = Icons.Default.Edit,
            gradientColors = listOf(Color(0xFFFF5252), Color(0xFFD50000)),
            modifier = modifier,
            onClick = { onAction("QUICK_NOTE") }
        )
        "VOICE_NOTE" -> CategoryBlock(
            title = "Sesli Not",
            icon = Icons.Default.Mic,
            gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF0091EA)),
            textColor = Color.White,
            modifier = modifier,
            onClick = { onAction("VOICE_NOTE") }
        )
        "PARK" -> CategoryBlock(
            title = if (parkedCarLat != null) "Park Halinde" else "Park Yeri Kaydet",
            icon = Icons.Default.DirectionsCar,
            gradientColors = listOf(Color(0xFFFFD600), Color(0xFFFF9100)),
            textColor = Color(0xFF3E2723),
            modifier = modifier,
            onClick = { onAction("PARK") }
        )
        "LOCATIONS" -> CategoryBlock(
            title = "Lokasyonlar",
            icon = Icons.Default.Place,
            gradientColors = listOf(Color(0xFF00E676), Color(0xFF00B248)),
            textColor = Color.White,
            modifier = modifier,
            onClick = { onAction("LOCATIONS") }
        )
        "REMINDERS" -> CategoryBlock(
            title = "Randevular",
            subtitle = "$allRemindersSize Adet",
            icon = Icons.Default.CalendarMonth,
            gradientColors = listOf(Color(0xFFFF6D00), Color(0xFFE65100)),
            modifier = modifier,
            onClick = { onAction("REMINDERS") }
        )
        "FAVORITES" -> CategoryBlock(
            title = "Favoriler",
            icon = Icons.Default.Favorite,
            gradientColors = listOf(Color(0xFFFF4081), Color(0xFFC2185B)),
            modifier = modifier,
            onClick = { onAction("FAVORITES") }
        )
        "ADD_NEW" -> CategoryBlock(
            title = "Yeni Ekle",
            icon = Icons.Default.Add,
            gradientColors = listOf(Color(0xFFE040FB), Color(0xFFAA00FF)),
            modifier = modifier,
            onClick = { onAction("ADD_NEW") }
        )
        else -> {
            val cat = allCategories.find { it.key == blockKey }
            if (cat != null) {
                CategoryBlock(
                    title = cat.displayName,
                    icon = cat.icon,
                    gradientColors = listOf(cat.color, cat.color.copy(alpha = 0.7f)),
                    textColor = Color.White,
                    modifier = modifier,
                    isLocked = lockedCategories.contains(blockKey),
                    onClick = { onAction(blockKey) }
                )
            }
        }
    }
}
