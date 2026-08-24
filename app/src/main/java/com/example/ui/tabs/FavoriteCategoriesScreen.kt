package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.ui.tabs.Category
import com.example.ui.tabs.UnifiedCategory
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.components.getIconVectorByName
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteCategoriesScreen(
    viewModel: LifeAssistantViewModel,
    onCategorySelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val customSubCategories by viewModel.customSubCategories.collectAsStateWithLifecycle()
    val favoriteCategories by viewModel.favoriteCategories.collectAsStateWithLifecycle()

    val unifiedList = remember(customCategories, customSubCategories, favoriteCategories) {
        val list = mutableListOf<UnifiedCategory>()
        Category.values().forEach { cat ->
            if (favoriteCategories.contains(cat.name)) {
                list.add(UnifiedCategory(cat.name, cat.displayName, cat.color, cat.icon, false))
            }
        }
        customCategories.forEach { custom ->
            if (favoriteCategories.contains(custom.name)) {
                val color = try {
                    Color(android.graphics.Color.parseColor(custom.colorHex))
                } catch (e: Exception) {
                    OrangePrimary
                }
                list.add(UnifiedCategory(custom.name, custom.name, color, getIconVectorByName(custom.iconName), true))
            }
        }

        // Default SubCategories
        Category.values().forEach { cat ->
            com.example.data.DefaultSubCategories.getDefaultsForCategory(cat.name).forEach { sub ->
                if (favoriteCategories.contains(sub.id)) {
                    list.add(UnifiedCategory(sub.id, "${cat.displayName} > ${sub.name}", cat.color, getIconVectorByName(sub.iconName), false))
                }
            }
        }

        // Custom SubCategories
        customSubCategories.forEach { sub ->
            if (favoriteCategories.contains(sub.id)) {
                val parentCat = Category.values().find { it.name == sub.categoryKey }
                val parentCustom = customCategories.find { it.name == sub.categoryKey }
                val color = parentCat?.color ?: try {
                    Color(android.graphics.Color.parseColor(parentCustom?.colorHex ?: "#FF8C00"))
                } catch (e: Exception) {
                    OrangePrimary
                }
                val pName = parentCat?.displayName ?: parentCustom?.name ?: sub.categoryKey
                list.add(UnifiedCategory(sub.id, "$pName > ${sub.name}", color, getIconVectorByName(sub.iconName), true))
            }
        }
        list
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCategories,
                containerColor = Color(0xFFFF6D00),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Ekle")
            }
        },
        containerColor = Color(0xFFF5F2ED),
        topBar = {
            TopAppBar(
                title = { Text("Favori Kategoriler", fontWeight = FontWeight.Black, color = Slate900, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Slate900)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F2ED))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            if (unifiedList.isEmpty()) {
                Text(
                    text = "Henüz favori kategoriniz yok. 'Kategoriler' ekranından ekleyebilirsiniz.",
                    fontSize = 14.sp,
                    color = Slate700,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(unifiedList) { item ->
                        EmbossedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp),
                            cornerRadius = 14.dp,
                            elevation = 5.dp,
                            glowColor = item.color,
                            onClick = { onCategorySelected(item.key) },
                            contentPadding = 12.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(item.color, item.color.copy(alpha = 0.8f))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleFavoriteCategory(item.key) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = "Favoriden Çıkar",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = item.displayName,
                                    color = Slate900,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
