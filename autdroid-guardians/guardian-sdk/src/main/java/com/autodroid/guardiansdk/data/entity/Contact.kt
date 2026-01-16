package com.autodroid.guardiansdk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 联系人实体
 * 统一被监护人(Ward)和报警联系人(Guardian)的数据结构
 * 通过type字段区分联系人类型
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val phoneNumber: String,           // 手机号（唯一标识）
    val name: String,                  // 姓名
    val type: ContactType,             // 联系人类型：WARD-被监护人，GUARDIAN-报警联系人
    val relationship: String,          // 关系（如：社区居民、学生、朋友、家人等）
    val passwordBook: String? = null,  // 密码本（仅WARD类型需要，Base64编码）
    val isPrimary: Boolean = false,    // 是否主要联系人（仅GUARDIAN类型有效）
    val orderIndex: Int = 0,           // 排序索引（仅GUARDIAN类型有效，1-5）
    val lastMessageTime: Long = 0,     // 最后消息时间
    val messageCount: Int = 0,         // 消息总数
    val alarmCount: Int = 0,           // 报警次数（仅WARD类型有效）
    val isActive: Boolean = true,      // 是否活跃
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 联系人类型枚举
 */
enum class ContactType {
    WARD,      // 被监护人（被保护对象）
    GUARDIAN   // 报警联系人（紧急联系人）
}