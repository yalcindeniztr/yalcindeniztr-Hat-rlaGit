package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY CASE WHEN dueDateMillis > 0 THEN dueDateMillis ELSE 9223372036854775807 END ASC, id ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE dueDateMillis > :now ORDER BY dueDateMillis ASC")
    suspend fun getActiveRemindersList(now: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isFavorite = 1 ORDER BY CASE WHEN dueDateMillis > 0 THEN dueDateMillis ELSE 9223372036854775807 END ASC, id ASC")
    fun getFavoriteReminders(): Flow<List<ReminderEntity>>
    
    @Query("SELECT * FROM reminders WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY CASE WHEN dueDateMillis > 0 THEN dueDateMillis ELSE 9223372036854775807 END ASC, id ASC")
    fun searchReminders(query: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE category = :category ORDER BY CASE WHEN dueDateMillis > 0 THEN dueDateMillis ELSE 9223372036854775807 END ASC, id ASC")
    fun getRemindersByCategory(category: String): Flow<List<ReminderEntity>>

    @androidx.room.Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Query("UPDATE reminders SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)
    
    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
