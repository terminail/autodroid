package com.autodroid.teachitback.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 设置项实体类
 * 用于存储SettingsItem的序列化JSON字符串
 */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val created: Long = System.currentTimeMillis()
)