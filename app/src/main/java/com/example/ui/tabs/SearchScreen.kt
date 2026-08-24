package com.example.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.components.ReminderItem
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary

@Composable
fun SearchScreen(viewModel: LifeAssistantViewModel) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val all by viewModel.allReminders.collectAsStateWithLifecycle()
    
    val results = all.filter {
        it.title.contains(query, ignoreCase = true) ||
        it.category.contains(query, ignoreCase = true) ||
        it.customNote.contains(query, ignoreCase = true)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // High contrast Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Tüm dosya ve hatırlatmalarda ara...", color = Slate700) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Ara", tint = TurquoiseSecondary)
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = Slate900)
                        }
                    }
                    com.example.util.VoiceInputTrailingIcon(
                        prompt = "Aramak istediğiniz kelimeyi söyleyin...",
                        onSpeechResult = { viewModel.updateSearchQuery(it) }
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TurquoiseSecondary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedTextColor = Slate900,
                unfocusedTextColor = Slate900,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (query.isBlank()) "TÜM ARŞİV (${all.size})" else "ARAMA SONUÇLARI (${results.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        if (results.isEmpty()) {
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 3.dp,
                contentPadding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = TurquoiseSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = if (query.isBlank()) "Henüz kayıtlı bir dosya/hatırlatıcı bulunmuyor." else "Aramanızla eşleşen sonuç bulunamadı.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(results) { rem ->
                    ReminderItem(
                        reminder = rem,
                        onFavoriteClick = { viewModel.toggleFavorite(rem.id, rem.isFavorite) },
                        onClick = {},
                        onDeleteClick = { viewModel.deleteReminder(rem.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
