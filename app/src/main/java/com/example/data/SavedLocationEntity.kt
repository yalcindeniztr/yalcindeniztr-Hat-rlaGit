package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)
