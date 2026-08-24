package com.example.ui.tabs

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.components.CategoryLockDialog
import com.example.ui.components.CategoryUnlockDialog
import com.example.ui.components.CreateCustomCategoryDialog
import com.example.ui.components.EmbossedCard
import com.example.ui.components.getIconVectorByName
import com.example.ui.theme.CatCareer
import com.example.ui.theme.CatDaily
import com.example.ui.theme.CatFinance
import com.example.ui.theme.CatGeneral
import com.example.ui.theme.CatHealth
import com.example.ui.theme.CatLegal
import com.example.ui.theme.CatLocation
import com.example.ui.theme.CatPersonal
import com.example.ui.theme.CatSocial
import com.example.ui.theme.CatVehicle
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import kotlinx.coroutines.launch

enum class Category(val displayName: String, val color: Color, val icon: ImageVector) {
    BILLS_CARDS("Faturalar ve Kartlar", Color(0xFF0284C7), Icons.Default.ReceiptLong),
    MY_CAR("Arabam", Color(0xFFD97706), Icons.Default.DirectionsCar),
    HEALTH("Sağlık & Klinik", CatHealth, Icons.Default.LocalHospital),
    LEGAL("Hukuk & Resmi", CatLegal, Icons.Default.Gavel),
    DAILY("Günlük Yaşam", CatDaily, Icons.Default.FitnessCenter),
    SOCIAL("Sosyal & İlişkiler", CatSocial, Icons.Default.People),
    FINANCE("Finans & Alışveriş", CatFinance, Icons.Default.ShoppingBag),
    LOCATION("Konum & Navigasyon", CatLocation, Icons.Default.LocationOn),
    CAREER("Kariyer & Projeler", CatCareer, Icons.Default.Work),
    PERSONAL("Kişisel Gelişim", CatPersonal, Icons.Default.AccountBalance),
    VEHICLE("Araç & Teknoloji", CatVehicle, Icons.Default.DirectionsCar),
    FAMILY_BUDGET("Aile Bütçesi Harcamalar", Color(0xFF10B981), Icons.Default.AccountBalanceWallet),
    GENERAL("Genel Hatırlatıcı", CatGeneral, Icons.Default.Notifications)
}

data class UnifiedCategory(
    val key: String,
    val displayName: String,
    val color: Color,
    val icon: ImageVector,
    val isCustom: Boolean = false,
    val customFields: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: LifeAssistantViewModel,
    onCategorySelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val favoriteCategories by viewModel.favoriteCategories.collectAsStateWithLifecycle()
    val lockedCategories by viewModel.lockedCategories.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var lockConfigCategory by remember { mutableStateOf<UnifiedCategory?>(null) }
    var unlockTargetCategory by remember { mutableStateOf<UnifiedCategory?>(null) }
    var isUnlockError by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateCustomCategoryDialog(
            onDismiss = { showCreateDialog = false },
            onCategoryCreated = { name, colorHex, iconName, fields ->
                viewModel.addCustomCategory(name, colorHex, iconName, fields)
            }
        )
    }

    if (lockConfigCategory != null) {
        val cat = lockConfigCategory!!
        val isCurrentlyLocked = lockedCategories.contains(cat.key)
        CategoryLockDialog(
            categoryName = cat.displayName,
            isCurrentlyLocked = isCurrentlyLocked,
            onDismiss = { lockConfigCategory = null },
            onSaveLock = { pin, isLocked ->
                viewModel.setCategoryLock(cat.key, pin, isLocked)
                lockConfigCategory = null
            }
        )
    }

    if (unlockTargetCategory != null) {
        val cat = unlockTargetCategory!!
        CategoryUnlockDialog(
            categoryName = cat.displayName,
            isError = isUnlockError,
            onDismiss = {
                unlockTargetCategory = null
                isUnlockError = false
            },
            onVerifyPin = { enteredPin ->
                scope.launch {
                    val isValid = viewModel.verifyCategoryPin(cat.key, enteredPin)
                    if (isValid) {
                        isUnlockError = false
                        val targetKey = cat.key
                        unlockTargetCategory = null
                        onCategorySelected(targetKey)
                    } else {
                        isUnlockError = true
                    }
                }
            }
        )
    }

    val unifiedList = remember(customCategories) {
        val list = mutableListOf<UnifiedCategory>()
        // Built-ins
        Category.values().forEach { cat ->
            list.add(
                UnifiedCategory(
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
                UnifiedCategory(
                    key = custom.name,
                    displayName = custom.name,
                    color = color,
                    icon = getIconVectorByName(custom.iconName),
                    isCustom = true,
                    customFields = custom.customFields
                )
            )
        }
        list
    }

    Scaffold(
        containerColor = Color(0xFFF5F2ED),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Randevu Al",
                        fontWeight = FontWeight.Black,
                        color = Slate900,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = Slate900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F2ED)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = "Hangi alanda randevu veya hatırlatıcı eklemek istersiniz?",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate800,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Item 0: Create New Custom Category Card
                item(span = { GridItemSpan(2) }) {
                    EmbossedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
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
                                        text = "+ Yeni Özel Kategori Oluştur",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Kendi renk ve alanlarınızla tanımlayın",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    }
                }

                items(unifiedList) { item ->
                    val isLocked = lockedCategories.contains(item.key)
                    EmbossedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        cornerRadius = 14.dp,
                        elevation = 5.dp,
                        glowColor = if (isLocked) Color(0xFF7E22CE) else item.color,
                        onClick = {
                            if (isLocked) {
                                isUnlockError = false
                                unlockTargetCategory = item
                            } else {
                                onCategorySelected(item.key)
                            }
                        },
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Lock / Shield Button
                                    IconButton(
                                        onClick = { lockConfigCategory = item },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Şifre Kilidi",
                                            tint = if (isLocked) Color(0xFF7E22CE) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(2.dp))

                                    val isFav = favoriteCategories.contains(item.key)
                                    IconButton(
                                        onClick = { viewModel.toggleFavoriteCategory(item.key) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favori",
                                            tint = if (isFav) Color(0xFFEF4444) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (item.isCustom) {
                                        IconButton(
                                            onClick = {
                                                val customObj = customCategories.find { it.name == item.key }
                                                if (customObj != null) {
                                                    viewModel.deleteCustomCategory(customObj.id)
                                                }
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Sil",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.displayName,
                                    color = Slate900,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isLocked) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Kilitli",
                                        tint = Color(0xFF7E22CE),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
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
