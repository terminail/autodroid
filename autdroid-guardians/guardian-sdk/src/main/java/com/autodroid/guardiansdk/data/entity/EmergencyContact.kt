package com.autodroid.guardiansdk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 紧急联系人实体
 * 每个被监护人自己的紧急联系人（最多5个）
 * 手机号作为唯一标识，纯短信方案中手机号是最可靠的身份标识
 */
@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey
    val phoneNumber: String,           // 联系人手机号（唯一标识）
    val wardPhoneNumber: String,       // 所属被监护人手机号
    val name: String,                  // 联系人姓名
    val relationship: String,          // 关系（如：爸爸、妈妈、朋友等）
    val isPrimary: Boolean = false,    // 是否主要联系人
    val orderIndex: Int = 0,           // 显示顺序
    val createdAt: Long = System.currentTimeMillis()
)