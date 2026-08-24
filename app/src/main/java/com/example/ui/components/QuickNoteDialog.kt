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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.CustomCategory
import com.example.data.ReminderEntity
import com.example.ui.tabs.Category
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import com.example.util.ElderlyVoiceActionButton
import com.example.util.VoiceInputTrailingIcon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickNoteDialog(
    customCategories: List<CustomCategory> = emptyList(),
    onDismiss: () -> Unit,
    onSaveNote: (ReminderEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf("Genel Hatırlatıcı") }

    val defaultFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    
    // Quick time helper states
    var selectedTimeOffsetMinutes by remember { mutableStateOf(30) } // Default 30 min later
    var customDatetimeString by remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 30)
        mutableStateOf(defaultFormat.format(cal.time))
    }

    val updateTimeOffset: (Int) -> Unit = { minutes ->
        selectedTimeOffsetMinutes = minutes
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutes)
        customDatetimeString = defaultFormat.format(cal.time)
    }

    val categoryList = remember(customCategories) {
        val builtIns = Category.values().map { it.displayName }
        val customs = customCategories.map { it.name }
        builtIns + customs
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFBF9F6),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFF4500)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hızlı Not & Hatırlatıcı",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900
                                )
                                Text(
                                    text = "Yazın veya mikrofona basarak konuşun",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Slate900,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Voice Dictation Button for Seniors
                    ElderlyVoiceActionButton(
                        label = "🎙️ Sesle Not Yazdır (Konuşun)",
                        prompt = "Lütfen notunuzu söyleyin...",
                        onSpeechResult = { text ->
                            if (title.isBlank()) {
                                title = text
                            } else {
                                noteContent = if (noteContent.isBlank()) text else "$noteContent $text"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Not / Hatırlatıcı Başlığı", fontWeight = FontWeight.Bold, color = Slate900) },
                        placeholder = { Text("Örn: Tansiyon ilacı, Manav alışverişi...", color = Slate700) },
                        trailingIcon = {
                            VoiceInputTrailingIcon(
                                prompt = "Başlığı mikrofona söyleyin...",
                                onSpeechResult = { title = it }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Content / Note Input
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Açıklama / Detaylı Not", fontWeight = FontWeight.Bold, color = Slate900) },
                        placeholder = { Text("Unutmamanız gereken tüm detayları buraya yazın...", color = Slate700) },
                        minLines = 3,
                        maxLines = 5,
                        trailingIcon = {
                            VoiceInputTrailingIcon(
                                prompt = "Açıklamayı mikrofona söyleyin...",
                                onSpeechResult = { spoken ->
                                    noteContent = if (noteContent.isBlank()) spoken else "$noteContent $spoken"
                                }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selection
                    Text(
                        text = "Kategori Seçimi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryList.take(6).forEach { catName ->
                            val isSelected = selectedCategoryName == catName
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TurquoiseSecondary else Color(0xFFE2E8F0))
                                    .clickable { selectedCategoryName = catName }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = catName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color.White else Slate800
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reminder Time Presets
                    Text(
                        text = "Hatırlatma Zamanı",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimeChip(
                            label = "+30 Dk",
                            isSelected = selectedTimeOffsetMinutes == 30,
                            onClick = { updateTimeOffset(30) },
                            modifier = Modifier.weight(1f)
                        )
                        TimeChip(
                            label = "+1 Saat",
                            isSelected = selectedTimeOffsetMinutes == 60,
                            onClick = { updateTimeOffset(60) },
                            modifier = Modifier.weight(1f)
                        )
                        TimeChip(
                            label = "Yarın Sabah",
                            isSelected = selectedTimeOffsetMinutes == 1440,
                            onClick = { updateTimeOffset(1440) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Exact Date Time String Input
                    OutlinedTextField(
                        value = customDatetimeString,
                        onValueChange = { customDatetimeString = it },
                        label = { Text("Tarih & Saat (GG.AA.YYYY HH:mm)", fontWeight = FontWeight.Bold, color = Slate900) },
                        leadingIcon = {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = TurquoiseSecondary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoiseSecondary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Save Button
                    Button(
                        onClick = {
                            var parsedMillis = System.currentTimeMillis() + (selectedTimeOffsetMinutes * 60 * 1000L)
                            try {
                                val date = defaultFormat.parse(customDatetimeString.trim())
                                if (date != null) {
                                    parsedMillis = date.time
                                }
                            } catch (e: Exception) {}

                            val entity = ReminderEntity(
                                category = selectedCategoryName,
                                title = title.trim(),
                                dueDatetime = customDatetimeString.trim(),
                                dueDateMillis = parsedMillis,
                                customNote = noteContent.trim(),
                                isFavorite = false,
                                encryptedMetadata = "{}",
                                actionStep = "QuickNote"
                            )
                            onSaveNote(entity)
                            onDismiss()
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notu ve Alarmı Kaydet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun TimeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) OrangePrimary else Color(0xFFE2E8F0))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) Color.White else Slate800
        )
    }
}
