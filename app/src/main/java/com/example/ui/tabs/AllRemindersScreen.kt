package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomCategory
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.components.ReminderItem
import com.example.ui.components.getIconVectorByName
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary

data class CategoryFilterItem(
    val key: String,
    val displayName: String,
    val color: Color,
    val icon: ImageVector,
    val isCustom: Boolean = false
)

@Composable
fun AllRemindersScreen(
    viewModel: LifeAssistantViewModel,
    onNavigateToAdd: () -> Unit = {}
) {
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var selectedFilterTab by remember { mutableStateOf("TÜMÜ") }
    var selectedCategoryKey by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isCategoryPanelExpanded by remember { mutableStateOf(true) }

    val unifiedCategories = remember(customCategories) {
        val list = mutableListOf<CategoryFilterItem>()
        // Built-ins
        Category.values().forEach { cat ->
            list.add(
                CategoryFilterItem(
                    key = cat.name,
                    displayName = cat.displayName,
                    color = cat.color,
                    icon = cat.icon,
                    isCustom = false
                )
            )
        }
        // Customs
        customCategories.forEach { custom ->
            val color = try {
                Color(android.graphics.Color.parseColor(custom.colorHex))
            } catch (e: Exception) {
                OrangePrimary
            }
            list.add(
                CategoryFilterItem(
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

    val selectedCategoryItem = unifiedCategories.find { it.key == selectedCategoryKey }

    val filteredList = allReminders.filter { reminder ->
        val matchesCategory = selectedCategoryKey == null || reminder.category.equals(selectedCategoryKey, ignoreCase = true)
        val matchesTab = when (selectedFilterTab) {
            "FAVORİLER" -> reminder.isFavorite
            "YAKLAŞAN" -> reminder.dueDateMillis >= System.currentTimeMillis()
            else -> true
        }
        val matchesSearch = searchText.isBlank() ||
                reminder.title.contains(searchText, ignoreCase = true) ||
                reminder.customNote.contains(searchText, ignoreCase = true) ||
                reminder.category.contains(searchText, ignoreCase = true)
        
        matchesCategory && matchesTab && matchesSearch
    }.sortedBy { it.dueDateMillis }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar with High Contrast & 3D styling
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = {
                    Text(
                        "Randevu veya hatırlatıcı ara...",
                        color = Slate700,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Ara",
                        tint = Slate900
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = Slate900)
                        }
                    }
                },
                singleLine = true,
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

            Spacer(modifier = Modifier.height(12.dp))

            // Status Filter Tabs (TÜMÜ, YAKLAŞAN, FAVORİLER)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("TÜMÜ", "YAKLAŞAN", "FAVORİLER")
                tabs.forEach { tab ->
                    val isSelected = selectedFilterTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(TurquoiseSecondary, Color(0xFF0D9488)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                }
                            )
                            .clickable { selectedFilterTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.White else Slate800
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2-Column Category Grid Header (Accessible for Elderly - Visible 2 Columns, No Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable { isCategoryPanelExpanded = !isCategoryPanelExpanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        tint = TurquoiseSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KATEGORİ FİLTRESİ (2 SÜTUN KUTUCUKLAR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCategoryItem != null) {
                        Text(
                            text = selectedCategoryItem.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseSecondary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Icon(
                        if (isCategoryPanelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Genişlet/Daralt",
                        tint = Slate900,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2-Column Selectable Category Boxes (Arranged vertically in 2-column rows, no side scrolling!)
        if (isCategoryPanelExpanded) {
            item {
                // "Tüm Kategoriler (Filtreyi Temizle)" Full Width Card
                val isAllSelected = selectedCategoryKey == null
                EmbossedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    cornerRadius = 10.dp,
                    elevation = if (isAllSelected) 4.dp else 1.dp,
                    glowColor = if (isAllSelected) Slate900 else Color.Transparent,
                    borderBrush = if (isAllSelected) Brush.linearGradient(listOf(Slate900, Color(0xFF334155))) else null,
                    onClick = { selectedCategoryKey = null },
                    contentPadding = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAllSelected) Slate900 else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                tint = if (isAllSelected) Color.White else Slate700,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TÜM KATEGORİLER (HEPSİ)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isAllSelected) Slate900 else Slate800
                        )
                        if (isAllSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = TurquoiseSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Categories chunked in pairs for 2 distinct columns (Built-ins + User Custom Categories!)
            val chunkedCategories = unifiedCategories.chunked(2)

            items(chunkedCategories) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { category ->
                        val isSelected = selectedCategoryKey == category.key
                        EmbossedCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            cornerRadius = 10.dp,
                            elevation = if (isSelected) 5.dp else 2.dp,
                            glowColor = if (isSelected) category.color else Color.Transparent,
                            borderBrush = if (isSelected) Brush.linearGradient(listOf(category.color, category.color.copy(alpha = 0.8f))) else null,
                            onClick = {
                                selectedCategoryKey = if (isSelected) null else category.key
                            },
                            contentPadding = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) category.color else category.color.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else category.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        lineHeight = 14.sp,
                                        maxLines = 2
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = category.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    // If pair has only 1 element, add an empty spacer weight
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))

            // List Header Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kayıtlı Randevular (${filteredList.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                Text(
                    text = "+ Yeni Ekle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OrangePrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFEDD5))
                        .clickable { onNavigateToAdd() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Reminders List
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmbossedCard(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        cornerRadius = 14.dp,
                        elevation = 4.dp,
                        contentPadding = 20.dp,
                        onClick = onNavigateToAdd
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = TurquoiseSecondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Kayıt Bulunamadı",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Seçilen kategori veya filtreye uygun randevu yok. Yeni bir hatırlatıcı eklemek için dokunun.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredList) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    onFavoriteClick = { viewModel.toggleFavorite(reminder.id, reminder.isFavorite) },
                    onClick = {},
                    onDeleteClick = { viewModel.deleteReminder(reminder.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
