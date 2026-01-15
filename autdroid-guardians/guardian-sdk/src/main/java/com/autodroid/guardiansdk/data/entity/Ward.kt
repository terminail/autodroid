package com.autodroid.guardiansdk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 被监护人实体
 * 社区警察管理的被监护人信息
 * 手机号作为唯一标识，纯短信方案中手机号是最可靠的身份标识
 */
@Entity(tableName = "wards")
data class Ward(
    @PrimaryKey
    val phoneNumber: String,           // 手机号（唯一标识）
    val name: String,                  // 姓名
    val relationship: String,          // 关系（如：社区居民、学生等）
    val passwordBook: String,          // 密码本（Base64编码）
    val lastAlarmTime: Long = 0,       // 最后报警时间
    val alarmCount: Int = 0,           // 报警次数
    val isActive: Boolean = true,      // 是否活跃
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)