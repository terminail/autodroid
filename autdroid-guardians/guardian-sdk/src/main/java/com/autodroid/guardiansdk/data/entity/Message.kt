package com.autodroid.guardiansdk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 消息实体
 * 存储与被监护人之间的聊天记录
 * from: 发送方手机号
 * to: 接收方手机号
 * content: MessageContent密封类序列化后的JSON字符串
 * 通过比较from/to与当前用户手机号判断消息方向
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromPhoneNumber: String,       // 发送方手机号
    val toPhoneNumber: String,         // 接收方手机号
    val content: String,               // MessageContent密封类序列化后的JSON
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 消息内容密封类
 * 支持多种消息类型，序列化后存入Message.content字段
 * 反序列化时自动识别具体类型，无需content_type字段
 */
@Serializable
sealed class MessageContent {
    
    @Serializable
    data class TextMessage(val text: String) : MessageContent()
    
    @Serializable
    data class AlarmMessage(
        val location: String,
        val level: String,  // normal/emergency
        val message: String
    ) : MessageContent()
    
    @Serializable
    data class QueryMessage(val query: String) : MessageContent()
    
    @Serializable
    data class PingMessage(val status: String) : MessageContent()
}

/**
 * MessageContent序列化/反序列化工具
 */
object MessageContentSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    fun serialize(content: MessageContent): String {
        return json.encodeToString(MessageContent.serializer(), content)
    }
    
    fun deserialize(jsonString: String): MessageContent {
        return json.decodeFromString(MessageContent.serializer(), jsonString)
    }
}
