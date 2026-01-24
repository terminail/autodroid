package com.autodroid.teachitback.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val masteryLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val nextLearningGoal: String? = null,
    val isPreset: Boolean = false,
    val presetTopicId: String? = null
)
