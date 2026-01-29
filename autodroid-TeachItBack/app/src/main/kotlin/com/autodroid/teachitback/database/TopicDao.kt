package com.autodroid.teachitback.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.autodroid.teachitback.model.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY lastAccessed DESC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY lastAccessed DESC")
    suspend fun getAllTopicsSync(): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE id = :id")
    fun getTopicById(id: String): Flow<TopicEntity?>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getTopicByIdSync(id: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()
    
    @Query("SELECT * FROM topics WHERE presetTopicId = :presetTopicId AND isPreset = 0 LIMIT 1")
    suspend fun getPersonalCopyByPresetId(presetTopicId: String): TopicEntity?
    
    // ===== 基于新字段的查询方法 =====
    
    @Query("SELECT * FROM topics WHERE topicCategoryId = :topicCategoryId ORDER BY lastAccessed DESC")
    fun getTopicsByCategoryId(topicCategoryId: String): Flow<List<TopicEntity>>
    
    @Query("SELECT * FROM topics WHERE capabilities LIKE '%' || :capability || '%' ORDER BY masteryLevel DESC")
    fun getTopicsByCapability(capability: String): Flow<List<TopicEntity>>
    
    @Query("SELECT * FROM topics WHERE title LIKE '%' || :query || '%' ORDER BY lastAccessed DESC")
    fun searchTopicsByTitle(query: String): Flow<List<TopicEntity>>
}
