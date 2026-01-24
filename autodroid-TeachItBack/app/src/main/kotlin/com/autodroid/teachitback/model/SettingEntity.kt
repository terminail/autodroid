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

/**
 * 腾讯云配置相关常量
 */
object TencentCloudSettings {
    /** API密钥 */
    const val API_KEY = "tencentcloud_api_key"

    /** 密钥ID */
    const val SECRET_ID = "tencentcloud_secret_id"

    /** 是否启用 */
    const val ENABLED = "tencentcloud_enabled"

    /** 测试模式 */
    const val TEST_MODE = "tencentcloud_test_mode"

    /** 地区 */
    const val REGION = "tencentcloud_region"

    /** 默认地区 */
    const val DEFAULT_REGION = "ap-guangzhou"
}