package com.autodroid.teachitback.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Why页面内容实体类
 * 用于存储Why页面的各种内容项
 */
@Entity(tableName = "why_content")
data class WhyEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val orderIndex: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val created: Long = System.currentTimeMillis()
)
