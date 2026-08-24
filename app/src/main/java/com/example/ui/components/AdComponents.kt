package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

/**
 * Native Sponsorlu Hatırlatıcı / Banner Reklam Alanı (Google AdMob Uyumlu)
 * Play Store kurallarına tam uygun, belirgin 'Sponsorlu / Reklam' etiketi ve net eylem düğmesi içerir.
 */
@Composable
fun HeaderBannerAdView(
    modifier: Modifier = Modifier,
    title: String = "Günün Önemli Hatırlatma İpucu",
    description: String = "Faturalarınızı ve araç muayenenizi gecikmeden planlayın.",
    onAdClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 12.dp,
            elevation = 2.dp,
            glowColor = Color(0xFFFDE68A),
            borderBrush = Brush.horizontalGradient(
                listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
            ),
            contentPadding = 8.dp,
            onClick = onAdClick
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = "Sponsorlu Reklam",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .border(0.8.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SPONSORLU",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = description,
                            fontSize = 10.sp,
                            color = Slate700,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "İncele",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen / Transition Interstitial Ad Component (Geçiş Reklamı)
 * Includes a countdown timer and dismiss button after 3 seconds.
 */
@Composable
fun InterstitialAdDialog(
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(15) }
    var canClose by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
        canClose = true
    }

    Dialog(
        onDismissRequest = {
            if (canClose) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = canClose,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            EmbossedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                cornerRadius = 18.dp,
                elevation = 8.dp,
                contentPadding = 20.dp,
                borderBrush = Brush.verticalGradient(
                    listOf(OrangePrimary, TurquoiseSecondary)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar with Ad Tag & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SPONSORLU DUYURU",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309)
                            )
                        }

                        if (canClose) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Slate900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${countdown}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                        }
                    }

                    // Ad Creative Body
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(OrangePrimary, TurquoiseSecondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "HatırlaGit Premium Partner",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sağlıklı yaşam, düzenli su ve ilaç hatırlatıcılarıyla hayatınızı kolaylaştırın. Reklam alanı Google AdMob entegrasyonuna hazırdır.",
                            fontSize = 12.sp,
                            color = Slate700,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }

                    // Action Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canClose) TurquoiseSecondary else Color(0xFF94A3B8)
                        )
                    ) {
                        Text(
                            text = if (canClose) "Reklamı Geç / Devam Et" else "Lütfen Bekleyin ($countdown)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
