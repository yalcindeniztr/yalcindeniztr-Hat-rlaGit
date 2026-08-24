package com.example.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.components.AppUpToDateDialog
import com.example.ui.components.HeaderBannerAdView
import com.example.ui.components.InterstitialAdDialog
import com.example.ui.components.PlayStoreRatingDialog
import com.example.ui.components.PlayStoreUpdateDialog
import com.example.ui.tabs.AllRemindersScreen
import com.example.ui.tabs.CategoriesScreen
import com.example.ui.tabs.HomeScreen
import com.example.ui.tabs.UsageDashboardScreen
import androidx.compose.material.icons.filled.PieChart
import com.example.ui.tabs.PrayerTimesScreen
import com.example.ui.tabs.ProfileScreen
import com.example.ui.tabs.SearchScreen
import com.example.ui.tabs.SavedLocationsScreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LifeAssistantViewModel, rootNavController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val userAvatarUri by viewModel.userAvatar.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val showUpToDateDialog by viewModel.showUpToDateDialog.collectAsStateWithLifecycle()
    val showRatingDialog by viewModel.showRatingDialog.collectAsStateWithLifecycle()
    val showInterstitialAd by viewModel.showInterstitialAd.collectAsStateWithLifecycle()

    // Google Play In-App Update Dialog (Yalnızca gerçekten yeni sürüm çıktığında görünür)
    if (showUpdateDialog) {
        PlayStoreUpdateDialog(
            newVersionName = viewModel.latestAvailableVersionName,
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    // Uygulama Güncel Dialogu (Kullanıcı profilden güncelleme denetlediğinde ve güncelse görünür)
    if (showUpToDateDialog) {
        AppUpToDateDialog(
            currentVersionName = viewModel.currentVersionName,
            onDismiss = { viewModel.dismissUpToDateDialog() }
        )
    }

    // Google Play Yıldız Verin Uyarısı (Haftada 1 kez, yıldız verirse bir daha gelmez)
    if (showRatingDialog) {
        PlayStoreRatingDialog(
            onRated = { viewModel.onUserRatedApp() },
            onDismissLater = { viewModel.onDismissRatingPrompt() }
        )
    }

    // Geçici Geçiş Reklamı (Interstitial)
    if (showInterstitialAd) {
        InterstitialAdDialog(
            onDismiss = { viewModel.dismissInterstitial() }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF5F2ED),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val dateFormat = SimpleDateFormat("dd MMMM yyyy • HH:mm", Locale("tr"))
                        Text(
                            text = dateFormat.format(Date()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate800,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Hatırla",
                                fontWeight = FontWeight.Black,
                                color = OrangePrimary,
                                fontSize = 22.sp
                            )
                            Text(
                                "Git",
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                fontSize = 22.sp
                            )
                        }
                    }
                },
                actions = {
                    // Clickable Top Profile Avatar (Sol menü tamamen kaldırıldı, temiz ve net üst alan)
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                Brush.linearGradient(listOf(OrangePrimary, TurquoiseSecondary)),
                                CircleShape
                            )
                            .background(Color.White)
                            .clickable { selectedTab = 3 },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUri != null) {
                            AsyncImage(
                                model = userAvatarUri,
                                contentDescription = "Profil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profil",
                                tint = TurquoiseSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F2ED)
                )
            )
        },
        bottomBar = {
            // 3D Custom Bottom Navigation Dock with Center Raised Embossed Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                ) {
                    // Bottom Navigation Bar Background with 3D Elevation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFFFFFFF), Color(0xFFFAF8F5))
                                ),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFD6D3CD),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Ana Sayfa
                            BottomNavButton(
                                icon = Icons.Default.Home,
                                label = "Ana Sayfa",
                                isSelected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                modifier = Modifier.weight(1f)
                            )

                            // 2. Kullanım & İstatistikler
                            BottomNavButton(
                                icon = Icons.Default.PieChart,
                                label = "Kullanım",
                                isSelected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                modifier = Modifier.weight(1f)
                            )

                            // Center Space for 3D Floating Action Button
                            Spacer(modifier = Modifier.width(68.dp))

                            // 3. Ezan Vakitleri & Hatırlatıcı (Diyanet Uyumlu)
                            BottomNavButton(
                                icon = Icons.Default.AccessTime,
                                label = "Ezan Vakti",
                                isSelected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                modifier = Modifier.weight(1f)
                            )

                            // 4. Profil & Ayarlar
                            BottomNavButton(
                                icon = Icons.Default.Person,
                                label = "Profil",
                                isSelected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Center 3D Raised Embossed Circular Button (Hatırlatma Ekle)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 0.dp)
                            .size(64.dp)
                            .drawBehind {
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        color = android.graphics.Color.TRANSPARENT
                                        setShadowLayer(
                                            12.dp.toPx(),
                                            0f,
                                            6.dp.toPx(),
                                            android.graphics.Color.argb(120, 255, 140, 0)
                                        )
                                    }
                                    canvas.nativeCanvas.drawCircle(
                                        size.width / 2,
                                        size.height / 2,
                                        size.width / 2,
                                        paint
                                    )
                                }
                            }
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(OrangePrimary, Color(0xFFFF4500))
                                )
                            )
                            .border(
                                width = 3.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFFFF3E0), Color(0xFFFF8C00))
                                ),
                                shape = CircleShape
                            )
                            .clickable {
                                rootNavController.navigate("categories")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Hatırlatıcı Ekle",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header Banner Reklam Alanı (Geçici Header Reklam)
            HeaderBannerAdView(
                title = "Önemli Sağlık ve Randevu İpucu",
                description = "Randevu alarmlarınızı zamanında kurarak hayatınızı kolaylaştırın.",
                onAdClick = {
                    viewModel.showInterstitial()
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAllReminders = { selectedTab = 9 },
                        onNavigateToAdd = { selectedTab = 1 },
                        onNavigateToLocations = { selectedTab = 5 },
                        onNavigateToParkScreen = { selectedTab = 6 },
                        onNavigateToVoiceNotes = { selectedTab = 7 },
                        onNavigateToFavoriteCategories = { selectedTab = 8 },
                        onNavigateToCategory = { categoryKey -> rootNavController.navigate("subcategories/$categoryKey") }
                    )
                    1 -> UsageDashboardScreen(viewModel)
                    2 -> PrayerTimesScreen(
                        viewModel = viewModel
                    )
                    3 -> ProfileScreen(viewModel = viewModel)
                    4 -> SearchScreen(viewModel = viewModel)
                    5 -> SavedLocationsScreen(viewModel = viewModel)
                    6 -> com.example.ui.tabs.ParkScreen(viewModel = viewModel, onNavigateBack = { selectedTab = 0 })
                    7 -> com.example.ui.tabs.VoiceNotesScreen(viewModel = viewModel, onNavigateBack = { selectedTab = 0 })
                    8 -> com.example.ui.tabs.FavoriteCategoriesScreen(
                        viewModel = viewModel,
                        onCategorySelected = { categoryKey -> rootNavController.navigate("add_reminder/$categoryKey") },
                        onNavigateBack = { selectedTab = 0 },
                        onNavigateToCategories = { selectedTab = 1 }
                    )
                    9 -> AllRemindersScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { selectedTab = 1 }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = TurquoiseSecondary
    val inactiveColor = Slate700

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) activeColor else inactiveColor,
            maxLines = 1
        )
    }
}
