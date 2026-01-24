package com.autodroid.teachitback.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autodroid.teachitback.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE topicId = :topicId ORDER BY timestamp ASC")
    fun getMessagesByTopic(topicId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE topicId = :topicId ORDER BY timestamp ASC")
    suspend fun getMessagesByTopicSync(topicId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE topicId = :topicId")
    suspend fun deleteMessagesByTopic(topicId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    // AIProcessInfo相关查询
    @Query("SELECT * FROM messages WHERE aiProcessInfoJson LIKE '%' || :serviceId || '%'")
    fun getMessagesByService(serviceId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE aiProcessInfoJson IS NOT NULL")
    fun getAIGeneratedMessages(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE aiProcessInfoJson LIKE '%' || :serviceId || '%'")
    suspend fun getMessageCountByService(serviceId: String): Int
}
