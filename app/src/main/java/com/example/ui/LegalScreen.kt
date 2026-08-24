package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary

@Composable
fun LegalScreen(onAccept: () -> Unit) {
    Scaffold(
        containerColor = Color(0xFFF5F2ED)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(OrangePrimary, TurquoiseSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "HatırlaGit Kullanım Sözleşmesi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "Kullanıcı Gizliliği ve Yasal Koşullar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Content with Sharp 3D Embossed Sections
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Yasal Uyarı & Sorumluluk Reddi (Red 3D Box)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    glowColor = Color(0xFFFCA5A5),
                    contentPadding = 14.dp
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFDC2626)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1. Tıbbi & Sağlık Sorumluluk Reddi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "HatırlaGit kişisel bir zaman ve randevu asistanıdır. Asla tıbbi teşhis, tedavi veya klinik tavsiye amacı taşımaz. Sağlık kararlarınızı mutlaka yetkili hekiminize danışarak alınız.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Yerel Şifreleme & Gizlilik (Turquoise 3D Box)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    glowColor = TurquoiseSecondary,
                    contentPadding = 14.dp
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0D9488)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. %100 Çevrimdışı & Yerel Şifreleme",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F766E)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Eklediğiniz tüm randevu, not ve kişisel veriler cihazınızda AES-256 şifreleme algoritması ile yalnızca yerel veritabanında saklanır. Hiçbir veriniz üçüncü taraf sunuculara iletilmez.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Bildirim ve Hatırlatma İzinleri (Orange 3D Box)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    glowColor = OrangePrimary,
                    contentPadding = 14.dp
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OrangePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Hassas Alarm & Erken Uyarı Bildirimleri",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC2410C)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Faturalarınız, muayene/kasko süreleriniz ve randevularınız için seçtiğiniz gün öncesinden (1-30 gün) ve tam işlem saatinde sesli bildirim uyarısı verilir.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 4: Faturalar, Kartlar ve Araç Verileri Güvenliği (Blue 3D Box)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    glowColor = Color(0xFF38BDF8),
                    contentPadding = 14.dp
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0284C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "4. Fatura, Kart ve Araç Veri Gizliliği",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0369A1)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Su, elektrik, doğalgaz, kredi kartı borçları, araç muayene ve sigorta kayıtlarınız doğrudan telefonunuzun korumalı hafızasında şifrelenir. Asla harici sunucuya veya üçüncü taraflara aktarılmaz.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 5: Kategori Kilidi ve Özel PIN Koruması (Purple 3D Box)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    glowColor = Color(0xFFC084FC),
                    contentPadding = 14.dp
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF7E22CE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "5. Kategori Şifreleme ve Yerel PIN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6B21A8)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dilediğiniz kategoriye özel PIN şifresi koyabilirsiniz. Şifreler cihazınızın Android KeyStore donanım modülünde şifreli tutulur ve yalnızca cihazınızda doğrulanır.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 19.sp
                        )
                    }
                }

                
                // Section 6: Reklam Spacer(modifier = Modifier.height(16.dp)) AdMob Politikası (Green 3D Box)
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                    glowColor = Color(0xFF34D399),
                    contentPadding = 14.dp
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF059669)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "6. Reklam ve Google AdMob Politikası",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF047857)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Uygulamamızın ücretsiz kalabilmesi için Google AdMob (Google Play Hizmetleri) entegrasyonuyla 15 saniyeyi geçmeyen reklam gösterimleri yapılabilir. Bu reklam servisleri, çerezleri kullanabilir ve anonim kullanım verilerini reklam deneyimini iyileştirmek için analiz edebilir.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 19.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3D Accept Button with High Contrast & Deep Elevation
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TurquoiseSecondary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Okudum, Anladım ve Kabul Ediyorum",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
