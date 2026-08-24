package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReminderEntity
import com.example.ui.LifeAssistantViewModel
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.util.rememberVoiceRecognizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNotesScreen(
    viewModel: LifeAssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val voiceNotes = allReminders.filter { it.category == "SESLİ NOT" }.sortedByDescending { it.createdAt }

    var searchQuery by remember { mutableStateOf("") }
    var noteTitle by remember { mutableStateOf("") }
    
    val filteredNotes = voiceNotes.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.customNote.contains(searchQuery, ignoreCase = true)
    }

    val startVoice = rememberVoiceRecognizer { text ->
        if (text.isNotBlank()) {
            val titleToUse = if (noteTitle.isNotBlank()) noteTitle else "Sesli Not"
            viewModel.addQuickNote(
                title = titleToUse,
                note = text,
                categoryName = "SESLİ NOT"
            )
            noteTitle = "" // clear after saving
        }
    }

    var noteToEdit by remember { mutableStateOf<ReminderEntity?>(null) }
    var editNoteContent by remember { mutableStateOf("") }

    if (noteToEdit != null) {
        BasicAlertDialog(
            onDismissRequest = { noteToEdit = null }
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notu Düzenle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editNoteContent,
                        onValueChange = { editNoteContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { noteToEdit = null }) { Text("İptal") }
                        Button(onClick = {
                            val updated = noteToEdit!!.copy(customNote = editNoteContent)
                            viewModel.updateReminder(updated)
                            noteToEdit = null
                        }) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesli Notlar", fontWeight = FontWeight.Black, color = Slate900) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F2ED))
            )
        },
        containerColor = Color(0xFFF5F2ED)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header for saving new note
            Text("Yeni Not", fontWeight = FontWeight.Bold, color = Slate800, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = { Text("Not Başlığı (Tıklayınca ses dinler)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { startVoice("Sesli notunuzu söyleyin...") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Sesle Yaz", tint = Color(0xFF00E5FF))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedLabelColor = Color.Black, unfocusedLabelColor = Color.DarkGray)
            )
            Button(
                onClick = { startVoice("Sesli notunuzu söyleyin...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF004D40))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Başlığa göre Sesli Not Al", color = Color(0xFF004D40), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Kayıtlı notlarda ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredNotes.size) { index ->
                    val note = filteredNotes[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(note.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val prev = filteredNotes[index - 1]
                                                val t1 = note.createdAt
                                                val t2 = prev.createdAt
                                                viewModel.updateReminder(note.copy(createdAt = t2))
                                                viewModel.updateReminder(prev.copy(createdAt = t1))
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Yukarı") }
                                    IconButton(
                                        onClick = {
                                            if (index < filteredNotes.size - 1) {
                                                val next = filteredNotes[index + 1]
                                                val t1 = note.createdAt
                                                val t2 = next.createdAt
                                                viewModel.updateReminder(note.copy(createdAt = t2))
                                                viewModel.updateReminder(next.copy(createdAt = t1))
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Aşağı") }
                                    IconButton(onClick = {
                                        noteToEdit = note
                                        editNoteContent = note.customNote
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color.Blue)
                                    }
                                    IconButton(onClick = { viewModel.deleteReminder(note.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(note.customNote, fontSize = 14.sp, color = Slate800)
                        }
                    }
                }
            }
        }
    }
}
