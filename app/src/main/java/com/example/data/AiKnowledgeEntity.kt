package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_knowledge")
data class AiKnowledgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String = "USER_NOTE", // USER_NOTE, OFFICIAL_LAW, SYSTEM_PREF, CHAT_MEMORY
    val isOfficialVerified: Boolean = false,
    val source: String = "Cihaz İçi Hafıza",
    val createdAt: Long = System.currentTimeMillis()
)
