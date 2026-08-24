package com.example.ui.tabs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PrayerTimingData
import com.example.data.TurkeyCities
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PrayerItemInfo(
    val name: String,
    val time: String,
    val icon: ImageVector,
    val iconColor: Color,
    val description: String
)

@Composable
fun PrayerTimesScreen(viewModel: LifeAssistantViewModel) {
    val context = LocalContext.current
    val prayerData by viewModel.prayerTimingData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isPrayerLoading.collectAsStateWithLifecycle()
    val locationMode by viewModel.prayerLocationMode.collectAsStateWithLifecycle()
    val selectedCity by viewModel.prayerSelectedCity.collectAsStateWithLifecycle()
    val prayerNotifications by viewModel.prayerNotifications.collectAsStateWithLifecycle()
    val prayerReminderMinutesBefore by viewModel.prayerReminderMinutesBefore.collectAsStateWithLifecycle()
    val isLocationPermissionEnabled by viewModel.isLocationEnabled.collectAsStateWithLifecycle()

    var showCityPickerModal by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.setPrayerLocationMode("AUTO")
            viewModel.toggleLocationPermission(true)
            fetchGpsLocationAndPrayers(context, viewModel)
        } else {
            viewModel.setPrayerLocationMode("MANUAL")
        }
    }

    // Initial load
    LaunchedEffect(selectedCity, locationMode) {
        if (locationMode == "AUTO" && isLocationPermissionEnabled) {
            fetchGpsLocationAndPrayers(context, viewModel)
        } else {
            if (prayerData == null || prayerData?.city != selectedCity) {
                viewModel.loadPrayerTimes(city = selectedCity)
            }
        }
    }

    // City Selection Dialog for 81 Turkish Provinces
    if (showCityPickerModal) {
        AlertDialog(
            onDismissRequest = { showCityPickerModal = false },
            title = {
                Text(
                    text = "81 İl Seçimi (Diyanet Vakitleri)",
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.height(350.dp)) {
                    OutlinedTextField(
                        value = citySearchQuery,
                        onValueChange = { citySearchQuery = it },
                        placeholder = { Text("İl ara (Örn: Ankara, İzmir...)", fontSize = 12.sp, color = Slate700) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate700) },
                        trailingIcon = {
                            if (citySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { citySearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = Slate700)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredCities = TurkeyCities.list.filter {
                        it.name.contains(citySearchQuery, ignoreCase = true)
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredCities) { city ->
                            val isSelected = selectedCity.equals(city.name, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFDCFCE7) else Color.Transparent)
                                    .clickable {
                                        viewModel.setPrayerLocationMode("MANUAL")
                                        viewModel.setPrayerSelectedCity(city.name)
                                        showCityPickerModal = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = city.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF15803D) else Slate900
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityPickerModal = false }) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = TurquoiseSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Location Mode Selector Card (Otomatik GPS vs 81 İl Manuel)
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                elevation = 4.dp,
                contentPadding = 12.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(TurquoiseSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Konum: ${prayerData?.city ?: selectedCity}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900
                                )
                                Text(
                                    text = if (locationMode == "AUTO") "🛰️ Otomatik GPS Konumu" else "📍 81 İl Manuel Seçim",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                        }

                        // Refresh / Reload Button
                        IconButton(
                            onClick = {
                                if (locationMode == "AUTO") {
                                    fetchGpsLocationAndPrayers(context, viewModel)
                                } else {
                                    viewModel.loadPrayerTimes(selectedCity)
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TurquoiseSecondary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = Slate900, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Toggle Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // GPS Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (locationMode == "AUTO") {
                                        Brush.horizontalGradient(listOf(TurquoiseSecondary, Color(0xFF0D9488)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                    }
                                )
                                .clickable {
                                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                        viewModel.setPrayerLocationMode("AUTO")
                                        fetchGpsLocationAndPrayers(context, viewModel)
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = if (locationMode == "AUTO") Color.White else Slate800,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Otomatik (GPS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (locationMode == "AUTO") Color.White else Slate800
                                )
                            }
                        }

                        // Manual City Picker Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (locationMode == "MANUAL") {
                                        Brush.horizontalGradient(listOf(OrangePrimary, Color(0xFFFF8C00)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                    }
                                )
                                .clickable {
                                    citySearchQuery = ""
                                    showCityPickerModal = true
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (locationMode == "MANUAL") Color.White else Slate800,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "İl Değiştir (81 İl)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (locationMode == "MANUAL") Color.White else Slate800
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minutes Before Reminder Setting
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                elevation = 2.dp,
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Önceden Hatırlat",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                            Text(
                                text = "Ezan vaktine kalan süre",
                                fontSize = 11.sp,
                                color = Slate700
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            if (prayerReminderMinutesBefore > 5) viewModel.setPrayerReminderMinutesBefore(prayerReminderMinutesBefore - 5)
                        }) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }
                        Text(
                            text = "$prayerReminderMinutesBefore dk",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        IconButton(onClick = { 
                            if (prayerReminderMinutesBefore < 60) viewModel.setPrayerReminderMinutesBefore(prayerReminderMinutesBefore + 5)
                        }) {
                            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prayer Timing Calculation & Next Prayer Countdown
            val timing = prayerData
            val prayerItems = if (timing != null) {
                listOf(
                    PrayerItemInfo("İmsak", timing.imsak, Icons.Default.DarkMode, Color(0xFF6366F1), "Sahur vakti sona erişi"),
                    PrayerItemInfo("Güneş", timing.gunes, Icons.Default.WbTwilight, Color(0xFFF59E0B), "Güneş doğuş vakti"),
                    PrayerItemInfo("Öğle", timing.ogle, Icons.Default.WbSunny, Color(0xFFEAB308), "Öğle ezanı vakti"),
                    PrayerItemInfo("İkindi", timing.ikindi, Icons.Default.Brightness5, Color(0xFFFF8C00), "İkindi ezanı vakti"),
                    PrayerItemInfo("Akşam", timing.aksam, Icons.Default.Brightness6, Color(0xFFEF4444), "İftar & Akşam ezanı"),
                    PrayerItemInfo("Yatsı", timing.yatsi, Icons.Default.DarkMode, Color(0xFF4338CA), "Yatsı ezanı vakti")
                )
            } else {
                emptyList()
            }

            val nextPrayerInfo = computeNextPrayerCountdown(prayerItems)

            // Prominent Hero Card: Next Prayer & Countdown Timer
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                elevation = 6.dp,
                glowColor = Color(0xFF0D9488),
                borderBrush = Brush.linearGradient(listOf(Color(0xFF0D9488), TurquoiseSecondary, Color(0xFF14B8A6))),
                contentPadding = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Diyanet Takvimi Esaslı",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }

                        Text(
                            text = timing?.dateGregorian ?: SimpleDateFormat("dd MMMM yyyy", Locale("tr")).format(Date()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SONRAKİ VAKİT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate700,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${nextPrayerInfo.nextPrayerName} (${nextPrayerInfo.nextPrayerTime})",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Countdown Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0F766E), Color(0xFF0D9488))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kalan Süre: ${nextPrayerInfo.countdownString}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Prayer Times Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GÜNLÜK EZAN VE NAMAZ VAKİTLERİ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "🔔 Hatırlatıcı",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 6 Prayer Times Items
        val timing = prayerData
        if (timing != null) {
            val prayerItems = listOf(
                PrayerItemInfo("İmsak", timing.imsak, Icons.Default.DarkMode, Color(0xFF6366F1), "Sahur vakti sona erişi"),
                PrayerItemInfo("Güneş", timing.gunes, Icons.Default.WbTwilight, Color(0xFFF59E0B), "Güneş doğuş vakti"),
                PrayerItemInfo("Öğle", timing.ogle, Icons.Default.WbSunny, Color(0xFFEAB308), "Öğle ezanı vakti"),
                PrayerItemInfo("İkindi", timing.ikindi, Icons.Default.Brightness5, Color(0xFFFF8C00), "İkindi ezanı vakti"),
                PrayerItemInfo("Akşam", timing.aksam, Icons.Default.Brightness6, Color(0xFFEF4444), "İftar & Akşam ezanı"),
                PrayerItemInfo("Yatsı", timing.yatsi, Icons.Default.DarkMode, Color(0xFF4338CA), "Yatsı ezanı vakti")
            )

            val nextPrayerInfo = computeNextPrayerCountdown(prayerItems)

            items(prayerItems) { prayer ->
                val isNextPrayer = prayer.name == nextPrayerInfo.nextPrayerName
                val isNotificationEnabled = prayerNotifications[prayer.name] ?: true

                EmbossedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    cornerRadius = 12.dp,
                    elevation = if (isNextPrayer) 4.dp else 2.dp,
                    glowColor = if (isNextPrayer) prayer.iconColor else Color.Transparent,
                    borderBrush = if (isNextPrayer) Brush.linearGradient(listOf(prayer.iconColor, TurquoiseSecondary)) else null,
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(prayer.iconColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = prayer.icon,
                                    contentDescription = prayer.name,
                                    tint = prayer.iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = prayer.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Slate900
                                    )
                                    if (isNextPrayer) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFEF3C7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Sıradaki",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFD97706)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = prayer.description,
                                    fontSize = 10.sp,
                                    color = Slate700
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = prayer.time,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isNextPrayer) TurquoiseSecondary else Slate900
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Bell notification toggle button
                            IconButton(
                                onClick = { viewModel.togglePrayerNotification(prayer.name) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isNotificationEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    imageVector = if (isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = "Hatırlatma",
                                    tint = if (isNotificationEnabled) Color(0xFF16A34A) else Slate700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Diyanet Legal Assurance Notice Card
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 10.dp,
                elevation = 2.dp,
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = TurquoiseSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ezan vakti bilgileri T.C. Diyanet İşleri Başkanlığı resmi hesaplama takvimine uygundur. İnternetsiz çevrimdışı modda dahi 81 il için anında hassas hesaplanır.",
                        fontSize = 11.sp,
                        color = Slate800,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

data class NextPrayerResult(
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val countdownString: String
)

private fun computeNextPrayerCountdown(prayers: List<PrayerItemInfo>): NextPrayerResult {
    if (prayers.isEmpty()) {
        return NextPrayerResult("Öğle", "13:15", "00:00:00")
    }

    val cal = Calendar.getInstance()
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val currentSeconds = cal.get(Calendar.SECOND)

    for (p in prayers) {
        val parts = p.time.split(":")
        if (parts.size >= 2) {
            val prayerMinutes = (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            if (prayerMinutes > currentMinutes) {
                val diffMinutes = prayerMinutes - currentMinutes - 1
                val diffSeconds = 60 - currentSeconds
                val totalSecs = diffMinutes * 60 + diffSeconds
                val hours = totalSecs / 3600
                val mins = (totalSecs % 3600) / 60
                val secs = totalSecs % 60
                val countdown = String.format(Locale.US, "%02d saat %02d dk %02d sn", hours, mins, secs)
                return NextPrayerResult(p.name, p.time, countdown)
            }
        }
    }

    // If passed Yatsı, next is tomorrow's İmsak
    val firstPrayer = prayers.first()
    val parts = firstPrayer.time.split(":")
    val imsakMinutes = (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    val diffMinutes = (24 * 60 - currentMinutes) + imsakMinutes - 1
    val diffSeconds = 60 - currentSeconds
    val totalSecs = diffMinutes * 60 + diffSeconds
    val hours = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    val countdown = String.format(Locale.US, "%02d saat %02d dk %02d sn", hours, mins, secs)
    return NextPrayerResult(firstPrayer.name, firstPrayer.time, countdown)
}

private fun fetchGpsLocationAndPrayers(context: Context, viewModel: LifeAssistantViewModel) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null) {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var lastLocation: Location? = null
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (isGpsEnabled) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
                if (lastLocation == null && isNetEnabled) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
            }

            if (lastLocation != null) {
                val closest = TurkeyCities.findClosestCity(lastLocation.latitude, lastLocation.longitude)
                viewModel.loadPrayerTimes(city = closest.name, lat = lastLocation.latitude, lng = lastLocation.longitude)
            } else {
                viewModel.loadPrayerTimes(city = "İstanbul")
            }
        } else {
            viewModel.loadPrayerTimes(city = "İstanbul")
        }
    } catch (e: Exception) {
        viewModel.loadPrayerTimes(city = "İstanbul")
    }
}
