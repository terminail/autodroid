package com.autodroid.teachitback.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autodroid.teachitback.service.AIAbility
import java.util.UUID

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // 基本信息
    val title: String,
    val description: String,

    // 关联树节点（分类目录）
    val topicTreeNodeId: String,

    // 路由配置
    val capabilities: Set<AIAbility> = emptySet(),
    val servicePreferences: Map<String, Double> = emptyMap(),

    // 原有字段保持不变
    val masteryLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val nextLearningGoal: String? = null,
    val isPreset: Boolean = false,
    val presetTopicId: String? = null
)
