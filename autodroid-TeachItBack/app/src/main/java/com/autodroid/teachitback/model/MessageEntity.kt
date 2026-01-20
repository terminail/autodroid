package com.autodroid.teachitback.model

import androidx.room.Entity
import androidx.room.ForeignKey
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
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val content: String,
    val senderType: String, // USER or AI
    val messageType: String, // TEXT, AUDIO, FILE_CONTENT
    val timestamp: Long = System.currentTimeMillis()
)
