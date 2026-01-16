package com.autodroid.guardiansdk.ui.contacts.detail.model

import com.autodroid.guardiansdk.data.entity.Message

/**
 * 被监护人详情页面数据项
 * 使用密封类支持异构item
 */
sealed class ContactDetailItem {

    /**
     * 基本信息头部
     */
    data class Header(
        val phoneNumber: String,
        val name: String,
        val relationship: String,
        val alarmCount: Int,
        val lastAlarmTime: Long
    ) : ContactDetailItem()

    /**
     * 消息项（统一类型，发送/接收方向在Adapter中根据from/to判断）
     */
    data class MessageItem(
        val message: Message
    ) : ContactDetailItem()

    /**
     * 时间分割线
     */
    data class TimeDivider(
        val timestamp: Long
    ) : ContactDetailItem()
}
