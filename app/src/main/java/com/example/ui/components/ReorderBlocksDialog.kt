package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderBlocksDialog(
    currentOrder: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var items by remember { mutableStateOf(currentOrder.filter { it != "ALL" && it != "REMINDERS" }) }
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ana Ekranı Düzenle", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                Text("Sırasını değiştirmek için basılı tutup sürükleyin.", fontSize = 12.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        offset.y.toInt() in it.offset..(it.offset + it.size)
                                    }
                                    if (item != null) {
                                        draggingIndex = item.index
                                        dragOffset = 0f
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    
                                    val dIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    val currentItem = listState.layoutInfo.visibleItemsInfo.find { it.index == dIndex }
                                    
                                    if (currentItem != null) {
                                        val currentCenter = currentItem.offset + dragOffset + (currentItem.size / 2)
                                        val targetItem = listState.layoutInfo.visibleItemsInfo.find {
                                            it.index != dIndex && currentCenter.toInt() in it.offset..(it.offset + it.size)
                                        }
                                        
                                        if (targetItem != null) {
                                            val tIndex = targetItem.index
                                            val newItems = items.toMutableList()
                                            val temp = newItems[dIndex]
                                            newItems[dIndex] = newItems[tIndex]
                                            newItems[tIndex] = temp
                                            items = newItems
                                            draggingIndex = tIndex
                                            dragOffset -= (targetItem.offset - currentItem.offset)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffset = 0f
                                }
                            )
                        }
                ) {
                    itemsIndexed(items) { index, blockKey ->
                        val isDragging = index == draggingIndex
                        val offsetModifier = if (isDragging) {
                            Modifier
                                .offset { IntOffset(0, dragOffset.roundToInt()) }
                                
                        } else {
                            Modifier
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth().then(offsetModifier)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDragging) Color(0xFFF1F5F9) else Color.White, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isDragging) Color(0xFF40E0D0) else Color.LightGray, RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(getBlockName(blockKey), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Icon(Icons.Default.DragHandle, contentDescription = "Sürükle", tint = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("İptal") }
                    Button(onClick = { onSave(items) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40E0D0))) {
                        Text("Kaydet", color = Color.Black)
                    }
                }
            }
        }
    }
}

fun getBlockName(key: String) = when(key) {
    "BILLS_CARDS" -> "Faturalar ve Kartlar"
    "MY_CAR" -> "Arabam"
    "QUICK_NOTE" -> "Hızlı Not"
    "VOICE_NOTE" -> "Sesli Not"
    "PARK" -> "Araba Park Yerim"
    "LOCATIONS" -> "Lokasyonlar"
    "REMINDERS" -> "Randevular"
    "ALL" -> "Tümü (Toplam Hatırlatıcı)"
    "FAVORITES" -> "Favoriler"
    "ADD_NEW" -> "Yeni Ekle"
    else -> key
}
