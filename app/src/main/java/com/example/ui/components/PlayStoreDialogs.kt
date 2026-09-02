package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary

/**
 * Utility to open Google Play Store listing or browser
 */
fun openPlayStore(context: Context) {
    val packageName = context.packageName
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}

/**
 * Google Play In-App Update Prompt Dialog
 * Lets user decide whether to update now or later.
 */
@Composable
fun PlayStoreUpdateDialog(
    newVersionName: String = "v1.1.1",
    updateHighlights: List<String> = listOf(
        "🤖 Sesli ve Konuşan Yapay Zeka Asistanı",
        "📍 Konuma göre Nöbetçi Eczane, Hastane ve Otopark bulma",
        "⏰ Sesli komutla otomatik randevu ve alarm kurma",
        "📸 OCR ile Akıllı Fatura ve Belge Tarayıcı",
        "📊 PDF ve Excel Raporlama & Güvenli QR Aktarımı",
        "🔒 Donanım Korumalı AES-256 Şifreli Güvenlik"
    ),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevation = 6.dp,
            glowColor = OrangePrimary,
            borderBrush = Brush.verticalGradient(listOf(OrangePrimary, TurquoiseSecondary)),
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFF5722)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.RocketLaunch,
                        contentDescription = "Güncelleme",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Yeni Güncelleme Mevcut! 🚀",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "HatırlaGit $newVersionName Google Play'de yayınlandı",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Highlights box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Yenilikler & İyileştirmeler:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        updateHighlights.forEach { item ->
                            Text(
                                text = "• $item",
                                fontSize = 11.sp,
                                color = Slate800,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Decision Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Daha Sonra",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }

                    Button(
                        onClick = {
                            openPlayStore(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text(
                            text = "Şimdi Güncelle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog shown when user checks for updates and application is already at the latest version
 */
@Composable
fun AppUpToDateDialog(
    currentVersionName: String = "v1.4.0",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevation = 6.dp,
            glowColor = TurquoiseSecondary,
            borderBrush = Brush.verticalGradient(listOf(TurquoiseSecondary, Color(0xFF10B981))),
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Verified/Check Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(TurquoiseSecondary, Color(0xFF10B981)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Güncel",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Uygulamanız Güncel! ✨",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Harika! Şu an en son sürüm olan $currentVersionName kullanıyorsunuz. Yeni bir güncelleme bulunmuyor.",
                    fontSize = 12.sp,
                    color = Slate700,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseSecondary)
                ) {
                    Text(
                        text = "Anladım",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Google Play Rating & Review Prompt Dialog (Haftada 1 kez, verirse bir daha gelmez)
 */
@Composable
fun PlayStoreRatingDialog(
    onRated: () -> Unit,
    onDismissLater: () -> Unit
) {
    val context = LocalContext.current
    var selectedStars by remember { mutableIntStateOf(5) }

    Dialog(
        onDismissRequest = onDismissLater,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevation = 6.dp,
            glowColor = Color(0xFFFBBF24),
            borderBrush = Brush.verticalGradient(listOf(Color(0xFFF59E0B), TurquoiseSecondary)),
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Star Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Yıldız",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "HatırlaGit'i Sevdiniz mi? ⭐",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Görüşleriniz bizim için çok değerli. Google Play'de 5 yıldız vererek geliştirmemize destek olun!",
                    fontSize = 12.sp,
                    color = Slate700,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive 5 Star Row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= selectedStars) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = "$i Yıldız",
                            tint = if (i <= selectedStars) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedStars = i }
                                .padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Rate Button
                Button(
                    onClick = {
                        openPlayStore(context)
                        onRated()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseSecondary)
                ) {
                    Text(
                        text = "Google Play'de $selectedStars Yıldız Ver",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remind Later Button (Will not ask for 1 week)
                OutlinedButton(
                    onClick = onDismissLater,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Daha Sonra Hatırlat (1 Hafta)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                }
            }
        }
    }
}
