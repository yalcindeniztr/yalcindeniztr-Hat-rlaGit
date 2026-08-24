package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CustomCategory
import com.example.data.ReminderEntity
import com.example.ui.tabs.Category
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditReminderDialog(
    reminder: ReminderEntity,
    customCategories: List<CustomCategory> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    var title by remember { mutableStateOf(reminder.title) }
    var customNote by remember { mutableStateOf(reminder.customNote) }
    var dueDatetime by remember { mutableStateOf(reminder.dueDatetime) }
    var selectedCategoryKey by remember { mutableStateOf(reminder.category) }
    var selectedSound by remember {
        mutableStateOf(
            if (reminder.actionStep.startsWith("SOUND_")) reminder.actionStep else "SOUND_CLASSIC_BELL"
        )
    }

    val soundOptions = listOf(
        Triple("SOUND_CLASSIC_BELL", "🔔 Klasik Çan", "Dengeli ve standart zil"),
        Triple("SOUND_CALM_MELODY", "🎵 Sakin Huzur", "Yumuşak nazik bildirim"),
        Triple("SOUND_DIGITAL_SIREN", "🚨 Dijital Siren", "Acil ve kaçırılmaz alarm")
    )

    // Build all selectable categories
    val allCategories = remember(customCategories) {
        val list = mutableListOf<Pair<String, String>>()
        Category.values().forEach { cat ->
            list.add(Pair(cat.name, cat.displayName))
        }
        customCategories.forEach { custom ->
            list.add(Pair(custom.name, "⭐ ${custom.name}"))
        }
        list
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFFAF9F6),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Randevuyu Düzenle",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                            Text(
                                text = "Bilgileri güncelleyin ve kaydedin",
                                fontSize = 12.sp,
                                color = Slate700
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title Input
                Text(
                    text = "Randevu / Hatırlatıcı Başlığı",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Örn: Doktor Kontrolü, Fatura vb.") },
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null, tint = OrangePrimary)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Date & Time
                Text(
                    text = "Tarih ve Saat",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = dueDatetime,
                    onValueChange = { dueDatetime = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Örn: 22.08.2026 14:30") },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF2563EB))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Note
                Text(
                    text = "Özel Not & Açıklama",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Eklemek istediğiniz notlar veya detaylar...") },
                    leadingIcon = {
                        Icon(Icons.Default.Note, contentDescription = null, tint = Color(0xFFB45309))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB45309),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "Kategori",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allCategories.forEach { (catKey, catName) ->
                        val isSelected = selectedCategoryKey == catKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) OrangePrimary else Color(0xFFE2E8F0)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) OrangePrimary else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCategoryKey = catKey }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = catName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Slate800
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alarm Sound
                Text(
                    text = "Alarm & Zil Sesi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    soundOptions.forEach { (soundKey, soundName, soundDesc) ->
                        val isSelected = selectedSound == soundKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFFFEF3C7) else Color.White
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) OrangePrimary else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedSound = soundKey }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = soundName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFB45309) else Slate900
                                )
                                Text(
                                    text = soundDesc,
                                    fontSize = 10.sp,
                                    color = Slate700
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(OrangePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Vazgeç", color = Slate700, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val updated = reminder.copy(
                                    title = title.trim(),
                                    customNote = customNote.trim(),
                                    dueDatetime = dueDatetime.trim(),
                                    category = selectedCategoryKey,
                                    actionStep = selectedSound
                                )
                                onSave(updated)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
