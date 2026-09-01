package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiKnowledgeDao {
    @Query("SELECT * FROM ai_knowledge ORDER BY id DESC")
    fun getAllKnowledge(): Flow<List<AiKnowledgeEntity>>

    @Query("SELECT * FROM ai_knowledge")
    suspend fun getAllKnowledgeList(): List<AiKnowledgeEntity>

    @Query("SELECT * FROM ai_knowledge WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY id DESC")
    suspend fun searchKnowledge(query: String): List<AiKnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(entity: AiKnowledgeEntity): Long

    @Query("DELETE FROM ai_knowledge WHERE id = :id")
    suspend fun deleteKnowledgeById(id: Int)

    @Query("DELETE FROM ai_knowledge")
    suspend fun deleteAll()
}
