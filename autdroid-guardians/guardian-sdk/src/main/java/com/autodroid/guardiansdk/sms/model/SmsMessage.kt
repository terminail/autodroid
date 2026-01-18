package com.autodroid.guardiansdk.sms.model

import java.util.Date

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Date,
    val type: Int,
    val read: Boolean = false,
    val subject: String? = null,
    val serviceCenter: String? = null,
    var status: Int = -1,
    val errorCode: Int = 0,
    val isMms: Boolean = false
) {
    companion object {
        const val TYPE_RECEIVED = 1
        const val TYPE_SENT = 2
        const val TYPE_DRAFT = 3
    }
    
    val isReceived: Boolean
        get() = type == TYPE_RECEIVED
    
    val isSent: Boolean
        get() = type == TYPE_SENT
    
    val isDraft: Boolean
        get() = type == TYPE_DRAFT
}
