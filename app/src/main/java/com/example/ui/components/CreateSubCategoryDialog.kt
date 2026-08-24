package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSubCategoryDialog(
    categoryDisplayName: String,
    categoryKey: String,
    onDismiss: () -> Unit,
    onSubCategoryCreated: (name: String, iconName: String, description: String, defaultTimes: List<String>, interval: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Alarm") }
    var interval by remember { mutableStateOf("DAILY") }
    val timesList = remember { mutableStateListOf("09:00") }
    var newTimeInput by remember { mutableStateOf("14:00") }

    val iconOptions = listOf(
        "Alarm" to Icons.Default.Alarm,
        "Notifications" to Icons.Default.Notifications,
        "WaterDrop" to Icons.Default.WaterDrop,
        "Medication" to Icons.Default.Medication,
        "MonitorHeart" to Icons.Default.MonitorHeart,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "CleanHands" to Icons.Default.CleanHands,
        "Gavel" to Icons.Default.Gavel,
        "CreditCard" to Icons.Default.CreditCard,
        "ReceiptLong" to Icons.Default.ReceiptLong,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "Yard" to Icons.Default.Yard,
        "Pets" to Icons.Default.Pets,
        "Restaurant" to Icons.Default.Restaurant,
        "Cake" to Icons.Default.Cake,
        "MenuBook" to Icons.Default.MenuBook,
        "Work" to Icons.Default.Work,
        "Checklist" to Icons.Default.Checklist
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Yeni Alt Kategori Ekle",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Slate900
                )
                Text(
                    text = "$categoryDisplayName altına özel hatırlatıcı şablonu oluşturun.",
                    fontSize = 12.sp,
                    color = Slate700,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Alt Kategori İsmi", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Örn: Vitamin Saati, Boyun Egzersizi vb.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Kısa Açıklama / İpucu", fontWeight = FontWeight.Medium) },
                    placeholder = { Text("Örn: Yemekten sonra 1 adet alınacak.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("İkon Seçin:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(iconOptions) { (iconKey, vector) ->
                        val isSelected = selectedIconName == iconKey
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) OrangePrimary else Color(0xFFF1F5F9))
                                .clickable { selectedIconName = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Slate700,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Varsayılan Saatler (Birden Fazla Eklenebilir):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    timesList.forEach { t ->
                        InputChip(
                            selected = true,
                            onClick = {
                                if (timesList.size > 1) timesList.remove(t)
                            },
                            label = { Text(t, fontWeight = FontWeight.Bold) },
                            trailingIcon = {
                                if (timesList.size > 1) {
                                    Icon(Icons.Default.Close, contentDescription = "Kaldır", modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTimeInput,
                        onValueChange = { newTimeInput = it },
                        label = { Text("Saat (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            if (newTimeInput.isNotBlank() && !timesList.contains(newTimeInput.trim())) {
                                timesList.add(newTimeInput.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Saat Ekle")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = Slate700)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSubCategoryCreated(
                                    name.trim(),
                                    selectedIconName,
                                    description.trim(),
                                    timesList.toList(),
                                    interval
                                )
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Alt Kategoriyi Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
