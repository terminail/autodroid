package com.autodroid.guardiansdk.sms.model

import java.util.Date

data class Conversation(
    var threadId: Long,
    var address: String,
    var contactName: String? = null,
    var snippet: String,
    var date: Date,
    var messageCount: Int,
    var unreadCount: Int = 0,
    var read: Boolean = true,
    var archived: Boolean = false,
    var blocked: Boolean = false,
    var mute: Boolean = false
)
