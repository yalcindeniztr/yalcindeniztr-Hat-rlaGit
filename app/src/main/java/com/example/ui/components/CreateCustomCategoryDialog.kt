package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import com.example.util.ElderlyVoiceActionButton
import com.example.util.VoiceInputTrailingIcon

val categoryColorOptions = listOf(
    "#FF8C00" to Color(0xFFFF8C00), // Orange
    "#40E0D0" to Color(0xFF40E0D0), // Turquoise
    "#3B82F6" to Color(0xFF3B82F6), // Blue
    "#10B981" to Color(0xFF10B981), // Emerald
    "#8B5CF6" to Color(0xFF8B5CF6), // Purple
    "#EC4899" to Color(0xFFEC4899), // Pink
    "#EF4444" to Color(0xFFEF4444), // Red
    "#F59E0B" to Color(0xFFF59E0B), // Amber
    "#6366F1" to Color(0xFF6366F1), // Indigo
    "#14B8A6" to Color(0xFF14B8A6)  // Teal
)

val categoryIconOptions = listOf(
    "Star" to Icons.Default.Star,
    "Favorite" to Icons.Default.Favorite,
    "Home" to Icons.Default.Home,
    "LocalHospital" to Icons.Default.LocalHospital,
    "Pets" to Icons.Default.Pets,
    "ShoppingCart" to Icons.Default.ShoppingCart,
    "School" to Icons.Default.School,
    "Build" to Icons.Default.Build,
    "CardGiftcard" to Icons.Default.CardGiftcard,
    "Receipt" to Icons.Default.Receipt,
    "Lightbulb" to Icons.Default.Lightbulb,
    "FitnessCenter" to Icons.Default.FitnessCenter,
    "LocationOn" to Icons.Default.LocationOn,
    "Work" to Icons.Default.Work,
    "Face" to Icons.Default.Face,
    "Savings" to Icons.Default.AccountBalanceWallet,
    "Warning" to Icons.Default.Warning,
    "Restaurant" to Icons.Default.Restaurant,
    "PhonelinkErase" to Icons.Default.Phone,
    "Bed" to Icons.Default.Bed,
    "CloudSync" to Icons.Default.Sync,
    "VpnKey" to Icons.Default.VpnKey,
    "Spa" to Icons.Default.Spa,
    "Subscriptions" to Icons.Default.Event,
    "Flight" to Icons.Default.Flight,
    "HomeRepairService" to Icons.Default.Build,
    "Notifications" to Icons.Default.Notifications
)

fun getIconVectorByName(iconName: String): ImageVector {
    return categoryIconOptions.find { it.first == iconName }?.second ?: Icons.Default.Notifications
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCustomCategoryDialog(
    onDismiss: () -> Unit,
    onCategoryCreated: (name: String, colorHex: String, iconName: String, fields: List<String>) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#FF8C00") }
    var selectedIconName by remember { mutableStateOf("Star") }
    var newFieldText by remember { mutableStateOf("") }
    val customFields = remember { mutableStateListOf("ozel_not", "oncelik") }

    val activeColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (e: Exception) {
            OrangePrimary
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFBF9F6),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(activeColor, activeColor.copy(alpha = 0.7f)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconVectorByName(selectedIconName),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Yeni Kategori Oluştur",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900
                                )
                                Text(
                                    text = "Özel hatırlatıcı alanınızı tanımlayın",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Slate900,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Typing button for elderly
                    ElderlyVoiceActionButton(
                        label = "🎙️ Kategori Adını Sesle Söyle",
                        prompt = "Lütfen oluşturmak istediğiniz kategori adını söyleyin...",
                        onSpeechResult = { categoryName = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Name Field
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Kategori Adı", fontWeight = FontWeight.Bold, color = Slate900) },
                        placeholder = { Text("Örn: İlaç Takibi, Bahçe Bakımı, Faturalar...", color = Slate700) },
                        trailingIcon = {
                            VoiceInputTrailingIcon(
                                prompt = "Kategori adını söyleyin...",
                                onSpeechResult = { categoryName = it }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Color Picker
                    Text(
                        text = "Renk Seçimi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        categoryColorOptions.take(5).forEach { (hex, color) ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                        color = if (selectedColorHex == hex) Slate900 else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        categoryColorOptions.drop(5).forEach { (hex, color) ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                        color = if (selectedColorHex == hex) Slate900 else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Icon Picker
                    Text(
                        text = "İkon Seçimi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryIconOptions.forEach { (name, iconVector) ->
                            val isSelected = selectedIconName == name
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) activeColor else Color(0xFFE2E8F0))
                                    .clickable { selectedIconName = name },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = name,
                                    tint = if (isSelected) Color.White else Slate800,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Custom Fields
                    Text(
                        text = "Özel Bilgi Alanları (İsteğe Bağlı)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customFields.forEach { field ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = field.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Sil",
                                    tint = Slate700,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { customFields.remove(field) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newFieldText,
                            onValueChange = { newFieldText = it },
                            placeholder = { Text("Alan adı ekle (Örn: Doz, Konum)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newFieldText.isNotBlank()) {
                                    val cleaned = newFieldText.trim().lowercase().replace(" ", "_")
                                    if (!customFields.contains(cleaned)) {
                                        customFields.add(cleaned)
                                    }
                                    newFieldText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeColor)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Create Category Button
                    Button(
                        onClick = {
                            onCategoryCreated(
                                categoryName.trim(),
                                selectedColorHex,
                                selectedIconName,
                                customFields.toList()
                            )
                            onDismiss()
                        },
                        enabled = categoryName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColor,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kategoriyi Kaydet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    )
}
