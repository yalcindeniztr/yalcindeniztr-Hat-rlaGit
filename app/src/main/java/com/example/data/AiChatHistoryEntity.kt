package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_history")
data class AiChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // USER or AI
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSummary: String? = null,
    val recommendedPlacesJson: String? = null
)
