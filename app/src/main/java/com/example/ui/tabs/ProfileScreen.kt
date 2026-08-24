package com.example.ui.tabs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.CryptoHelper
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary

@Composable
fun ProfileScreen(viewModel: LifeAssistantViewModel) {
    val context = LocalContext.current
    val encryptedNick by viewModel.userNick.collectAsStateWithLifecycle()
    val userAvatarUri by viewModel.userAvatar.collectAsStateWithLifecycle()
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteReminders.collectAsStateWithLifecycle()
    val fontScaleLevel by viewModel.fontScaleLevel.collectAsStateWithLifecycle()
    
    val decryptedNick = encryptedNick?.let { CryptoHelper.decrypt(it) } ?: "Kullanıcı"
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val isVoiceSpeakingEnabled by viewModel.isVoiceSpeakingEnabled.collectAsStateWithLifecycle()
    val isLocationEnabled by viewModel.isLocationEnabled.collectAsStateWithLifecycle()

    var showEditNickDialog by remember { mutableStateOf(false) }
    var newNickInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Notification Permission Launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.toggleNotificationsPermission(isGranted)
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.toggleLocationPermission(fineGranted || coarseGranted)
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = com.example.util.FileHelper.copyUriToInternalStorage(context, it, "profile_avatar_${System.currentTimeMillis()}.jpg")
            if (localPath != null) {
                viewModel.updateAvatar(localPath)
            }
        }
    }

    if (showEditNickDialog) {
        AlertDialog(
            onDismissRequest = { showEditNickDialog = false },
            title = {
                Text(
                    "Kullanıcı Adını Değiştir",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column {
                    Text(
                        "Yeni takma adınızı girin:",
                        fontSize = 13.sp,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNickInput,
                        onValueChange = { newNickInput = it },
                        singleLine = true,
                        placeholder = { Text(decryptedNick) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNickInput.isNotBlank()) {
                            viewModel.updateNick(newNickInput.trim())
                            showEditNickDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseSecondary)
                ) {
                    Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNickDialog = false }) {
                    Text("İptal", color = Slate700)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("Tüm Verileri Sil?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            },
            text = {
                Text(
                    "Bu işlem tüm randevularınızı, şifrelenmiş notlarınızı ve alarmlarınızı kalıcı olarak silecektir. Bu işlem geri alınamaz.",
                    color = Slate800,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Evet, Her Şeyi Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Vazgeç", color = Slate800)
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
            Spacer(modifier = Modifier.height(16.dp))

            // Colorful 3D Profile Header Card
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                elevation = 6.dp,
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Box with 3D Border & Edit Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(
                                3.dp,
                                Brush.sweepGradient(listOf(OrangePrimary, TurquoiseSecondary, Color(0xFF8B5CF6), OrangePrimary)),
                                CircleShape
                            )
                            .background(Color.White)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUri != null) {
                            AsyncImage(
                                model = userAvatarUri,
                                contentDescription = "Profil Resmi",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profil",
                                tint = OrangePrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        
                        // Small camera badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Slate900)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Resim Değiştir",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = decryptedNick,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    newNickInput = decryptedNick
                                    showEditNickDialog = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Nick Değiştir",
                                    tint = TurquoiseSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Yerel AES-256 Şifreli",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Profil resmine tıklayarak fotoğraf seçebilirsiniz.",
                            fontSize = 10.sp,
                            color = Slate700,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Colorful 3D Metrics Grid
            Text(
                text = "KULLANIM İSTATİSTİKLERİ",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Reminders 3D Tile
                EmbossedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${allReminders.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = "Toplam Randevu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }
                }

                // Favorites 3D Tile
                EmbossedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${favorites.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "Favori Kayıt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }
                }

                // Categories Used Tile
                val distinctCategories = allReminders.map { it.category }.distinct().size
                EmbossedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$distinctCategories",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = "Aktif Kategori",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }
                }
            }

            // Accessibility & Font Size Setting Section (Elderly Mode)
            Text(
                text = "ERİŞİLEBİLİRLİK & YAZI BOYUTU (YAŞLI MODU)",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Metinleri büyüterek daha rahat ve zahmetsiz okuma sağlayın.",
                fontSize = 11.sp,
                color = Slate700
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 3 Font Size Options Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val fontOptions = listOf(
                    Triple("NORMAL", "Standart", "Normal Boyut"),
                    Triple("LARGE", "Büyük", "Kolay Okuma"),
                    Triple("EXTRA_LARGE", "Ekstra Büyük", "Yaşlı Modu")
                )

                fontOptions.forEach { (level, title, subtitle) ->
                    val isSelected = fontScaleLevel == level
                    EmbossedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(105.dp),
                        cornerRadius = 12.dp,
                        elevation = if (isSelected) 5.dp else 2.dp,
                        glowColor = if (isSelected) TurquoiseSecondary else Color.Transparent,
                        borderBrush = if (isSelected) Brush.linearGradient(listOf(TurquoiseSecondary, Color(0xFF0D9488))) else null,
                        onClick = { viewModel.setFontScaleLevel(level) },
                        contentPadding = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) TurquoiseSecondary else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Slate900 else Slate800
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) TurquoiseSecondary else Slate700
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Preview Card
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
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TurquoiseSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Örnek Görünüm: Doktor randevunuz saat 14:30'da.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Permissions & Notifications Management Header
            Text(
                text = "UYGULAMA İZİNLERİ VE BİLDİRİM YÖNETİMİ",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Notification Permission Card
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 3.dp,
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isNotificationsEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (isNotificationsEnabled) Color(0xFF16A34A) else Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hatırlatıcı & Alarm Bildirimleri",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = if (isNotificationsEnabled) "Bildirimler ve sesli alarmlar aktif" else "Bildirimler kapalı (hatırlatıcı sesi çalmaz)",
                                fontSize = 11.sp,
                                color = if (isNotificationsEnabled) Color(0xFF15803D) else Slate700
                            )
                        }
                    }

                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPerm) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.toggleNotificationsPermission(true)
                                    }
                                } else {
                                    viewModel.toggleNotificationsPermission(true)
                                }
                            } else {
                                viewModel.toggleNotificationsPermission(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TurquoiseSecondary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sesli Okuma / Söyleme Özelliği (Text-To-Speech Aç/Kapa)
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 3.dp,
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isVoiceSpeakingEnabled) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (isVoiceSpeakingEnabled) Color(0xFFD97706) else Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Zamanı Gelince Sesli Oku",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = if (isVoiceSpeakingEnabled) "Hatırlatıcı zamanında sesli olarak okunur" else "Sesli söyleme kapalı (sadece zil/alarm çalar)",
                                fontSize = 11.sp,
                                color = if (isVoiceSpeakingEnabled) Color(0xFFB45309) else Slate700
                            )
                        }
                    }

                    Switch(
                        checked = isVoiceSpeakingEnabled,
                        onCheckedChange = { checked ->
                            viewModel.toggleVoiceSpeaking(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OrangePrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location Permission Card
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 3.dp,
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLocationEnabled) Color(0xFFE0F2FE) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (isLocationEnabled) Color(0xFF0284C7) else Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Konum İzni (GPS & Vakitler)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = if (isLocationEnabled) "GPS otomatik konum tespiti açık" else "GPS kapalı (81 il manuel seçim kullanılır)",
                                fontSize = 11.sp,
                                color = if (isLocationEnabled) Color(0xFF0369A1) else Slate700
                            )
                        }
                    }

                    Switch(
                        checked = isLocationEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (!fine && !coarse) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    viewModel.toggleLocationPermission(true)
                                }
                            } else {
                                viewModel.toggleLocationPermission(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TurquoiseSecondary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Open System Settings Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = TurquoiseSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Cihaz Sistem İzin Ayarlarını Aç ⚙️",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Center Header
            Text(
                text = "VERİ VE GÜVENLİK YÖNETİMİ",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Export CSV 3D Button
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 4.dp,
                contentPadding = 14.dp,
                onClick = {
                    val csvContent = buildString {
                        append("ID,Kategori,Baslik,Tarih_Saat,Not,Favori\n")
                        allReminders.forEach {
                            append("${it.id},${it.category},\"${it.title}\",\"${it.dueDatetime}\",\"${it.customNote}\",${it.isFavorite}\n")
                        }
                    }
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                        type = "text/csv"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, "HatırlaGit Verilerini Dışa Aktar")
                    context.startActivity(shareIntent)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoiseSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tüm Verileri Dışa Aktar (CSV)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Randevularınızı Excel ve dosya formatında dışa aktarın",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Security PIN Status Card
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 4.dp,
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Korumalı PIN Şifreleme",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Verileriniz donanım seviyesinde şifrelidir",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Play & Application Center
            Text(
                text = "UYGULAMA & GOOGLE PLAY DESTEĞİ",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Rate on Google Play 5 Stars
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 4.dp,
                contentPadding = 14.dp,
                glowColor = Color(0xFFFBBF24),
                borderBrush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                onClick = {
                    com.example.ui.components.openPlayStore(context)
                    viewModel.onUserRatedApp()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Play'de 5 Yıldız Verin ⭐",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        Text(
                            text = "Uygulamayı değerlendirerek gelişimine destek olun",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Check for Updates
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 4.dp,
                contentPadding = 14.dp,
                onClick = {
                    viewModel.checkAppUpdate(manualCheck = true)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoiseSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Güncellemeleri Denetle (Google Play)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Mevcut Sürüm: ${viewModel.currentVersionName} • En son özellikleri kontrol edin",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete Everything Button (Distinct Red 3D Card)
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 4.dp,
                contentPadding = 14.dp,
                glowColor = Color(0xFFFCA5A5),
                onClick = { showDeleteConfirmDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDC2626)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tüm Verileri Güvenle Sıfırla",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "Tek tıkla tüm kayıtları ve alarmları sıfırlar",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account Deletion Request Button
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                elevation = 4.dp,
                contentPadding = 14.dp,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:admin@lifeassistant.com")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Hesap ve Tüm Verilerin Silinmesi Talebi")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Merhaba,\n\nUygulama üzerindeki tüm hesap verilerimin ve bilgilerimin kalıcı olarak sistemlerinizden silinmesini talep ediyorum.\n\nİyi çalışmalar.")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "E-posta uygulaması bulunamadı", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF475569)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hesabımı ve Bilgilerimi Sil",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Admine sistemden silme talebi gönder",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
