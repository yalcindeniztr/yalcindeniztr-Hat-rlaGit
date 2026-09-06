package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatHistoryDao {
    @Query("SELECT * FROM ai_chat_history ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 30): List<AiChatHistoryEntity>

    @Query("SELECT * FROM ai_chat_history ORDER BY id ASC")
    fun getAllMessagesFlow(): Flow<List<AiChatHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatHistoryEntity): Long

    @Query("DELETE FROM ai_chat_history")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM ai_chat_history")
    suspend fun getCount(): Int
}
