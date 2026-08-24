package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.*

@Composable
fun UsageDashboardScreen(viewModel: LifeAssistantViewModel) {
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()

    val totalReminders = allReminders.size
    val activeReminders = allReminders.filter { it.dueDateMillis > System.currentTimeMillis() }.size
    val pastReminders = totalReminders - activeReminders

    // Simple expenses mock/calc based on FINANCE/FAMILY_BUDGET
    val financeReminders = allReminders.filter { it.category == "FINANCE" || it.category == "FAMILY_BUDGET" || it.category == "BILLS_CARDS" }
    
    val categoryCounts = allReminders.groupBy { it.category }.mapValues { it.value.size }
    
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), 
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F2ED))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "Kullanım & İstatistikler",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Slate900,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Nasıl Kullanılır?
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 4.dp,
            contentPadding = 16.dp
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = OrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uygulama Nasıl Kullanılır?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Ana sayfadaki (+) veya 'Yeni Randevu Ekle' butonları ile hatırlatıcılarınızı oluşturabilirsiniz.\n" +
                    "• Özel kategoriler ekleyerek hayatınızı kişiselleştirebilirsiniz.\n" +
                    "• Randevularınız aktif olduğunda size bildirim veya sesli alarm ile hatırlatılır.\n" +
                    "• Verileriniz tamamen telefonunuzda şifreli olarak tutulur.",
                    fontSize = 13.sp,
                    color = Slate700,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Aylık Randevu Dökümü (Grafik)
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 4.dp,
            contentPadding = 16.dp
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = TurquoiseSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Randevu Hatırlatıcı Dökümü", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (totalReminders == 0) {
                    Text("Henüz randevu bulunmuyor.", fontSize = 13.sp, color = Slate700)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Simple Pie Chart Placeholder
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                var startAngle = -90f
                                categoryCounts.entries.forEachIndexed { index, entry ->
                                    val sweepAngle = (entry.value.toFloat() / totalReminders) * 360f
                                    drawArc(
                                        color = colors[index % colors.size],
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 40f, cap = StrokeCap.Butt),
                                        size = Size(size.width, size.height)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                            Text(
                                text = "$totalReminders",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            categoryCounts.entries.take(5).forEachIndexed { index, entry ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colors[index % colors.size]))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${entry.key}: ${entry.value}", fontSize = 11.sp, color = Slate800, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Aylık Giderler & Finans Dökümü (Grafiksel Bar)
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 4.dp,
            contentPadding = 16.dp
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aylık Giderler & Faturalar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                if (financeReminders.isEmpty()) {
                    Text("Finans kategorisinde henüz kayıt bulunmuyor.", fontSize = 13.sp, color = Slate700)
                } else {
                    val count = financeReminders.size
                    Text("Toplam $count adet finansal hatırlatıcınız mevcut.", fontSize = 13.sp, color = Slate700)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple Horizontal Bar Mock
                    financeReminders.take(4).forEach { reminder ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(reminder.title, fontSize = 12.sp, color = Slate900, maxLines = 1, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0))) {
                                Box(modifier = Modifier.fillMaxWidth(0.5f + (Math.random().toFloat() * 0.4f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF10B981)))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Hakkımızda
        EmbossedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 4.dp,
            contentPadding = 16.dp
        ) {
            Column {
                Text("Hakkımızda", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "LifeAssistant, gündelik yaşamınızda, sağlık randevularınızda, araç ve ev fatura takiplerinizde size eşlik eden profesyonel bir yaşam asistanıdır. Gizliliğinize en üst düzeyde önem vererek verilerinizi sadece cihazınızda şifreli tutar.",
                    fontSize = 13.sp,
                    color = Slate700,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
