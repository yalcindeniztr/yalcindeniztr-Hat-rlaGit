package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LightBg = Color(0xFFFBF9F5)
private val CardBg = Color(0xFFFFFFFF)
private val CoralAccent = Color(0xFFFF6B6B)
private val TurquoiseAccent = Color(0xFF0284C7)
private val PurpleAccent = Color(0xFF7C3AED)
private val GreenAccent = Color(0xFF059669)
private val AmberAccent = Color(0xFFD97706)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeLibraryScreen(
    viewModel: LifeAssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    val allKnowledgeFlow = remember { db.aiKnowledgeDao().getAllKnowledge() }
    val knowledgeList by allKnowledgeFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntity by remember { mutableStateOf<AiKnowledgeEntity?>(null) }
    var viewingEntity by remember { mutableStateOf<AiKnowledgeEntity?>(null) }
    var entityToDelete by remember { mutableStateOf<AiKnowledgeEntity?>(null) }

    // Kategori Filtreleme
    val categories = listOf(
        "ALL" to "🌐 Tümü",
        "OFFICIAL_LAW" to "📜 Memur Kanunları",
        "SENDIKA" to "⚖️ Sendika",
        "MEB" to "🏫 MEB Yönetmelikleri",
        "MAARIF_CURRICULUM" to "📚 Maarif Müfredatı",
        "CULTURE" to "🏛️ Türk Tarihi & Eserleri",
        "USER_NOTE" to "📌 Özel Notlar"
    )

    val filteredList = remember(knowledgeList, searchQuery, selectedCategoryFilter) {
        knowledgeList.filter { entity ->
            val matchesCategory = when (selectedCategoryFilter) {
                "ALL" -> true
                "OFFICIAL_LAW" -> entity.category.contains("LAW", ignoreCase = true) || entity.title.contains("657") || entity.title.contains("ÖMK")
                "SENDIKA" -> entity.category.contains("SENDIKA", ignoreCase = true) || entity.title.contains("Sendika", ignoreCase = true)
                "MEB" -> entity.category.contains("MEB", ignoreCase = true) || entity.title.contains("Yönetmelik", ignoreCase = true) || entity.title.contains("Sınıf Geçme", ignoreCase = true)
                "MAARIF_CURRICULUM" -> entity.category.contains("MAARIF", ignoreCase = true) || entity.title.contains("Maarif", ignoreCase = true) || entity.title.contains("Müfredat", ignoreCase = true)
                "CULTURE" -> entity.category.contains("CULTURE", ignoreCase = true) || entity.title.contains("Tarih", ignoreCase = true) || entity.title.contains("UNESCO", ignoreCase = true)
                "USER_NOTE" -> entity.category == "USER_NOTE"
                else -> entity.category == selectedCategoryFilter
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                entity.title.contains(searchQuery, ignoreCase = true) || entity.content.contains(searchQuery, ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        containerColor = LightBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PurpleAccent, TurquoiseAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Özel Bilgi Kütüphanesi", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Slate900)
                            Text("Maarif, Kanun, Yönetmelik & Notlar", fontSize = 10.sp, color = Slate700)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Slate900)
                    }
                },
                actions = {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Arama Çubuğu (Açık 3D)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Kanun, yönetmelik veya konu ara...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OrangePrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Temizle", tint = Slate700)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true
            )

            // Kategori Çipleri (Yatay Kaydırmalı)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(categories) { (key, label) ->
                    val isSelected = selectedCategoryFilter == key
                    val chipBg = if (isSelected) OrangePrimary else Color(0xFFF1F5F9)
                    val textColor = if (isSelected) Color.White else Slate800

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .border(1.dp, if (isSelected) OrangePrimary else Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                            .clickable { selectedCategoryFilter = key }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }

            // İçerik Listesi
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kayıtlı bilgi bulunamadı.", fontWeight = FontWeight.Bold, color = Slate800)
                        Text("Sağ üstteki 'Ekle' butonuyla yeni bilgi ekleyebilirsiniz.", fontSize = 12.sp, color = Slate700)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id }) { entity ->
                        KnowledgeCardItem(
                            entity = entity,
                            onClick = { viewingEntity = entity },
                            onEdit = { editingEntity = entity },
                            onDelete = { entityToDelete = entity }
                        )
                    }
                }
            }
        }
    }

    // 1. Detay Okuma Penceresi
    if (viewingEntity != null) {
        val item = viewingEntity!!
        AlertDialog(
            onDismissRequest = { viewingEntity = null },
            title = {
                Text(item.title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Slate900)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE0F2FE))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(item.source, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.content,
                        fontSize = 13.sp,
                        color = Slate800,
                        lineHeight = 19.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingEntity = null }) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = OrangePrimary)
                }
            }
        )
    }

    // 2. Yeni Bilgi Ekleme Diyaloğu
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("USER_NOTE") }
        var newSource by remember { mutableStateOf("Özel Kullanıcı Notu") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Bilgi Ekle", fontWeight = FontWeight.Black, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Başlık (Örn: 657 İzin Hakları)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Kategori (OFFICIAL_LAW, MEB, SENDIKA, vb.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("İçerik / Kanun / Yönetmelik Metni") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    OutlinedTextField(
                        value = newSource,
                        onValueChange = { newSource = it },
                        label = { Text("Kaynak (Örn: Resmî Gazete / MEB)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    db.aiKnowledgeDao().insertKnowledge(
                                        AiKnowledgeEntity(
                                            title = newTitle.trim(),
                                            content = newContent.trim(),
                                            category = newCategory.trim().ifBlank { "USER_NOTE" },
                                            source = newSource.trim().ifBlank { "Kullanıcı Kaydı" }
                                        )
                                    )
                                }
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal", color = Slate700)
                }
            }
        )
    }

    // 3. Bilgi Düzenleme Diyaloğu
    if (editingEntity != null) {
        val current = editingEntity!!
        var editTitle by remember { mutableStateOf(current.title) }
        var editContent by remember { mutableStateOf(current.content) }
        var editCategory by remember { mutableStateOf(current.category) }

        AlertDialog(
            onDismissRequest = { editingEntity = null },
            title = { Text("Bilgiyi Düzenle", fontWeight = FontWeight.Black, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Başlık") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Kategori") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("İçerik") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.aiKnowledgeDao().updateKnowledge(
                                    current.copy(
                                        title = editTitle.trim(),
                                        content = editContent.trim(),
                                        category = editCategory.trim()
                                    )
                                )
                            }
                            editingEntity = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseAccent)
                ) {
                    Text("Güncelle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntity = null }) {
                    Text("İptal", color = Slate700)
                }
            }
        )
    }

    // 4. Silme Onay Diyaloğu
    if (entityToDelete != null) {
        val item = entityToDelete!!
        AlertDialog(
            onDismissRequest = { entityToDelete = null },
            title = { Text("Silme Onayı", fontWeight = FontWeight.Black, color = Slate900) },
            text = { Text("\"${item.title}\" başlıklı kaydı silmek istediğinize emin misiniz?", color = Slate800) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.aiKnowledgeDao().deleteKnowledgeById(item.id)
                            }
                            entityToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entityToDelete = null }) {
                    Text("Vazgeç", color = Slate700)
                }
            }
        )
    }
}

@Composable
private fun KnowledgeCardItem(
    entity: AiKnowledgeEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    EmbossedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
        elevation = 3.dp,
        contentPadding = 12.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entity.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = TurquoiseAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entity.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entity.content.take(120).replace("\n", " ") + "...",
                fontSize = 11.sp,
                color = Slate700,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "📌 ${entity.source}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}
