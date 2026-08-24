package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // e.g. [SAĞLIK_KLİNİK]
    val title: String,
    val dueDatetime: String,
    val dueDateMillis: Long = 0L,
    val customNote: String = "",
    val isFavorite: Boolean = false,
    val encryptedMetadata: String, // Encrypted JSON payload
    val actionStep: String,
    val createdAt: Long = System.currentTimeMillis()
)
