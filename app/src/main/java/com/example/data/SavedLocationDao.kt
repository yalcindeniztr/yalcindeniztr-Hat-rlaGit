package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<SavedLocationEntity>>

    @Insert
    suspend fun insertLocation(location: SavedLocationEntity)

    @Delete
    suspend fun deleteLocation(location: SavedLocationEntity)
    
    @Update
    suspend fun updateLocation(location: SavedLocationEntity)
    @Query("DELETE FROM saved_locations")
    suspend fun deleteAll()
}
