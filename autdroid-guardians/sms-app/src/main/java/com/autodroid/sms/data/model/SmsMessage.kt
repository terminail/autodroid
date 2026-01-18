package com.autodroid.sms.data.model

import java.util.Date

/**
 * 短信消息数据模型
 * 对应系统短信数据库中的消息
 */
data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Date,
    val type: Int, // 1: 接收, 2: 发送, 3: 草稿
    val read: Boolean = false,
    val subject: String? = null,
    val serviceCenter: String? = null,
    var status: Int = -1, // -1: 失败, 0: 待发送, 1: 发送中, 2: 已发送, 3: 已送达
    val errorCode: Int = 0,
    val isMms: Boolean = false
) {
    companion object {
        const val TYPE_RECEIVED = 1
        const val TYPE_SENT = 2
        const val TYPE_DRAFT = 3
    }
    
    /**
     * 是否是接收的消息
     */
    val isReceived: Boolean
        get() = type == TYPE_RECEIVED
    
    /**
     * 是否是发送的消息
     */
    val isSent: Boolean
        get() = type == TYPE_SENT
    
    /**
     * 是否是草稿
     */
    val isDraft: Boolean
        get() = type == TYPE_DRAFT
}