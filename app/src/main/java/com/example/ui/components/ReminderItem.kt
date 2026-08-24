package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReminderEntity
import com.example.ui.tabs.Category
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun ReminderItem(
    reminder: ReminderEntity,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    isGlass: Boolean = false
) {
    val categoryColor = try {
        Category.valueOf(reminder.category).color
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val categoryDisplayName = try {
        Category.valueOf(reminder.category).displayName
    } catch (e: Exception) {
        reminder.category
    }
    
    EmbossedCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = categoryColor,
        onClick = onClick,
        isGlass = isGlass,
        cornerRadius = 14.dp,
        elevation = 5.dp,
        contentPadding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(categoryColor, categoryColor.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryDisplayName,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Due Date Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = "Saat",
                        tint = Slate900,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reminder.dueDatetime,
                        fontSize = 11.sp,
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Title in rich contrast
            Text(
                text = reminder.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                lineHeight = 22.sp
            )
            
            // Custom note if present
            if (reminder.customNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Default.Note,
                        contentDescription = "Not",
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reminder.customNote,
                        fontSize = 12.sp,
                        color = Color(0xFF78350F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))
            
            // Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Favorite button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (reminder.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (reminder.isFavorite) Color(0xFFEF4444) else Slate700,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onEditClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    
                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
                
                // Status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)) // Light green background
                        .border(1.dp, Color(0xFF22C55E), CircleShape) // Green border
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AKTİF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF16A34A) // Green text
                    )
                }
            }
        }
    }
}
