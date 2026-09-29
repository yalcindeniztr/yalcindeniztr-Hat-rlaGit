package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.AiResponse
import com.example.util.AppLauncherHelper
import com.example.util.NearbyPlace
import com.example.util.NearbyPlacesHelper
import com.example.util.ResearchPdfHelper
import com.example.util.TtsHelper
import kotlinx.coroutines.launch

/**
 * ATİLA Asistan & Araştırma Pop-up HUD Ekranı
 * Kullanıcı sesle veya yazıyla soru sorduğunda, ekrana tam teşekküllü
 * bilgi kartı, araştırma metni, nöbetçi eczane/mekan butonları,
 * tek dokunuşla A4 PDF üretimi ve Gemini köprüsünü getirir.
 */
@Composable
fun AtilaResponsePopupDialog(
    response: AiResponse,
    userQuery: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var isSpeaking by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80030712))
                .padding(horizontal = 16.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFF0F172A))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                color = Color(0xFF0F172A) // Koyu Lacivert / Slate 900
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // =========================================================
                    // 1. ÜST BAŞLIK VE KONTROL ŞERİDİ
                    // =========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0284C7), Color(0xFF4F46E5))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = response.actionSummary?.take(35) ?: "ATİLA Asistan Raporu",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Canlı Araştırma & Asistan Paneli",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                TtsHelper.stop()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color(0xFFE2E8F0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // =========================================================
                    // 2. KAYDIRILABİLİR GÖVDE: METİN & MEKAN KARTLARI
                    // =========================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 420.dp)
                            .verticalScroll(scrollState)
                    ) {
                        // Yanıt Metni
                        FormattedResponseText(text = response.replyText)

                        // Varsa Önerilen Yerler (Eczane, Market vb.)
                        if (response.recommendedPlaces.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "📍 Tespit Edilen Canlı Konumlar:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            response.recommendedPlaces.forEach { place ->
                                PlaceCardItem(context = context, place = place)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // =========================================================
                    // 3. AKSİYON BUTONLARI (PDF, GEMINI, SES, KAPAT)
                    // =========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PDF OLUŞTUR BUTONU
                        Button(
                            onClick = {
                                scope.launch {
                                    val title = response.actionSummary?.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ _-]"), "")?.trim()?.ifBlank { "ATILA Raporu" } ?: "ATILA Raporu"
                                    ResearchPdfHelper.createAndOpenPdf(context, title, response.replyText)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0369A1)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("📄", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Yap", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // GEMINI KÖPRÜSÜ
                        Button(
                            onClick = {
                                val query = userQuery.ifBlank { response.actionSummary ?: response.replyText.take(60) }
                                AppLauncherHelper.openGoogleGemini(context, query)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4338CA)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("✨", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // SESLENDİR / DURDUR
                        Button(
                            onClick = {
                                if (isSpeaking) {
                                    TtsHelper.stop()
                                    isSpeaking = false
                                } else {
                                    val toSpeak = response.speechText ?: response.replyText
                                    TtsHelper.speak(context, toSpeak)
                                    isSpeaking = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpeaking) Color(0xFFDC2626) else Color(0xFF059669)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Seslendir",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeaking) "Durdur" else "Dinle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedResponseText(text: String) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("##") || line.startsWith("###") -> {
                    Text(
                        text = line.replace("#", "").trim(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("**") && line.endsWith("**") -> {
                    Text(
                        text = line.replace("**", "").trim(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("•") || line.startsWith("-") || line.startsWith("*") -> {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", fontSize = 14.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = line.replace(Regex("^[•\\-*]\\s*"), "").replace("**", ""),
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 18.sp
                        )
                    }
                }
                line.isNotBlank() -> {
                    Text(
                        text = line.replace("**", ""),
                        fontSize = 13.5.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 19.sp
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaceCardItem(context: Context, place: NearbyPlace) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = place.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${place.distanceMeters}m",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.address,
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Yol Tarifi Al (GPS Kontrollü)
                OutlinedButton(
                    onClick = {
                        NearbyPlacesHelper.openGoogleMapsNavigation(
                            context = context,
                            placeName = place.name,
                            lat = place.lat,
                            lng = place.lng,
                            searchQuery = place.searchQuery.ifBlank { place.name }
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF38BDF8)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("🗺️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yol Tarifi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Telefon Et
                if (!place.phone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            NearbyPlacesHelper.makePhoneCall(context, place.phone)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4ADE80)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("📞", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ara", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
