package com.autodroid.teachitback.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topicId"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val content: String,
    val senderType: String, // USER or AI
    val messageType: String, // TEXT, AUDIO, FILE_CONTENT
    val timestamp: Long = System.currentTimeMillis(),
    
    // AI服务处理信息（JSON序列化存储）
    val aiProcessInfoJson: String? = null
) {
    // 辅助方法：获取AIProcessInfo对象
    fun getAIProcessInfo(): AIProcessInfo? {
        return if (aiProcessInfoJson != null) {
            try {
                // 实际实现需要使用Gson或Kotlinx Serialization反序列化
                // 这里简化处理
                null
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
