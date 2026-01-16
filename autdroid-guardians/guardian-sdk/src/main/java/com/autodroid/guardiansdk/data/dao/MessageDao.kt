package com.autodroid.guardiansdk.data.dao

import androidx.room.*
import com.autodroid.guardiansdk.data.entity.Message
import kotlinx.coroutines.flow.Flow

/**
 * 消息DAO
 * 管理聊天记录的增删改查
 */
@Dao
interface MessageDao {

    /**
     * 插入单条消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    /**
     * 插入多条消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>): List<Long>

    /**
     * 更新消息
     */
    @Update
    suspend fun update(message: Message)

    /**
     * 删除消息
     */
    @Delete
    suspend fun delete(message: Message)

    /**
     * 根据ID获取消息
     */
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): Message?

    /**
     * 获取与被监护人的所有聊天记录，按时间升序
     * 匹配fromPhoneNumber或toPhoneNumber
     */
    @Query("SELECT * FROM messages WHERE fromPhoneNumber = :phoneNumber OR toPhoneNumber = :phoneNumber ORDER BY timestamp ASC")
    fun getMessagesByWard(phoneNumber: String): Flow<List<Message>>

    /**
     * 获取与被监护人的聊天记录（分页）
     */
    @Query("SELECT * FROM messages WHERE fromPhoneNumber = :phoneNumber OR toPhoneNumber = :phoneNumber ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesByWardPaged(phoneNumber: String, limit: Int, offset: Int): List<Message>

    /**
     * 删除与被监护人的所有聊天记录
     */
    @Query("DELETE FROM messages WHERE fromPhoneNumber = :phoneNumber OR toPhoneNumber = :phoneNumber")
    suspend fun deleteMessagesByWard(phoneNumber: String)

    /**
     * 搜索聊天记录
     */
    @Query("SELECT * FROM messages WHERE (fromPhoneNumber = :phoneNumber OR toPhoneNumber = :phoneNumber) AND content LIKE '%' || :keyword || '%' ORDER BY timestamp ASC")
    fun searchMessages(phoneNumber: String, keyword: String): Flow<List<Message>>
}
