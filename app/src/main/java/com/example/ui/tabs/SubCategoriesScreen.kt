package com.example.ui.tabs

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DefaultSubCategories
import com.example.data.SubCategory
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.CreateSubCategoryDialog
import com.example.ui.components.EmbossedCard
import com.example.ui.components.getIconVectorByName
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import com.example.util.ElderlyVoiceActionButton
import com.example.util.VoiceInputTrailingIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoriesScreen(
    categoryKey: String,
    viewModel: LifeAssistantViewModel,
    onSubCategorySelected: (categoryKey: String, subCategoryName: String, description: String, times: List<String>, interval: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val favoriteCategories by viewModel.favoriteCategories.collectAsStateWithLifecycle()
    val customSubCategories by viewModel.customSubCategories.collectAsStateWithLifecycle()

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

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateSubCategoryDialog(
            categoryDisplayName = categoryDisplayName,
            categoryKey = categoryKey,
            onDismiss = { showCreateDialog = false },
            onSubCategoryCreated = { name, iconName, desc, times, interval ->
                viewModel.addCustomSubCategory(categoryKey, name, iconName, desc, times, interval)
            }
        )
    }

    // Combine default subcategories and user custom subcategories for this category
    val allSubCategories = remember(categoryKey, customSubCategories) {
        val defaults = DefaultSubCategories.getDefaultsForCategory(builtInEnum?.name ?: categoryKey)
        val userSubs = customSubCategories.filter {
            it.categoryKey.equals(categoryKey, ignoreCase = true) ||
            it.categoryKey.equals(categoryDisplayName, ignoreCase = true) ||
            (builtInEnum != null && it.categoryKey == builtInEnum.name)
        }
        userSubs + defaults
    }

    val filteredList = remember(allSubCategories, searchQuery) {
        if (searchQuery.isBlank()) {
            allSubCategories
        } else {
            allSubCategories.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.defaultDescription.contains(searchQuery, ignoreCase = true)
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(categoryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(categoryIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = categoryDisplayName,
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Alt Kategoriler & Hatırlatıcılar",
                                fontSize = 11.sp,
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
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Alt Kategori Ekle", tint = categoryColor, modifier = Modifier.size(28.dp))
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
                Spacer(modifier = Modifier.height(6.dp))

                // Search Bar with voice input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Alt kategorilerde ara...", color = Slate700, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = categoryColor)
                    },
                    trailingIcon = {
                        VoiceInputTrailingIcon(
                            prompt = "Aramak istediğiniz alt kategoriyi söyleyin...",
                            onSpeechResult = { searchQuery = it }
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Add Custom SubCategory Action Banner
                EmbossedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true },
                    cornerRadius = 14.dp,
                    elevation = 3.dp,
                    glowColor = categoryColor,
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = categoryColor)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "+ Kendinize Özel Alt Kategori Oluşturun",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "İstediğiniz isim ve saatlerle yeni şablon ekleyin",
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = categoryColor)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Mevcut Alt Kategoriler (${filteredList.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (filteredList.isEmpty()) {
                item {
                    EmbossedCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        elevation = 2.dp,
                        contentPadding = 16.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Aradığınız alt kategori bulunamadı.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
                            ) {
                                Text("Bu İsimle Alt Kategori Oluştur")
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { sub ->
                    SubCategoryCard(
                        favoriteCategories = favoriteCategories,
                        onToggleFavorite = { viewModel.toggleFavoriteCategory(sub.id) },
                        subCategory = sub,
                        categoryColor = categoryColor,
                        onSelect = {
                            onSubCategorySelected(
                                categoryKey,
                                sub.name,
                                sub.defaultDescription,
                                sub.defaultTimes,
                                sub.suggestedInterval
                            )
                        },
                        onDeleteCustom = if (sub.isCustom) {
                            { viewModel.deleteCustomSubCategory(sub.id) }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SubCategoryCard(
    favoriteCategories: Set<String>,
    onToggleFavorite: () -> Unit,
    subCategory: SubCategory,
    categoryColor: Color,
    onSelect: () -> Unit,
    onDeleteCustom: (() -> Unit)? = null
) {
    val subIcon = remember(subCategory.iconName) {
        getIconVectorByName(subCategory.iconName)
    }

    EmbossedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        cornerRadius = 14.dp,
        elevation = 3.dp,
        glowColor = categoryColor,
        contentPadding = 14.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(subIcon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = subCategory.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                        if (subCategory.isCustom) {
                            Text(
                                text = "⭐ Özel Oluşturduğunuz Alt Kategori",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }
                    }
                }

                val isFav = favoriteCategories.contains(subCategory.id)
                IconButton(
                    onClick = { onToggleFavorite() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (isFav) Color(0xFFEF4444) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (onDeleteCustom != null) {
                    IconButton(onClick = onDeleteCustom) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (subCategory.defaultDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subCategory.defaultDescription,
                    fontSize = 12.sp,
                    color = Slate700,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time chips & Suggested interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(subCategory.defaultTimes) { timeStr ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE2E8F0))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⏰ $timeStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }
                    }
                    if (subCategory.suggestedInterval == "DAILY") {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TurquoiseSecondary.copy(alpha = 0.2f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🔁 Her Gün",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = categoryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Oluştur ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
