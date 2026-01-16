package com.autodroid.guardiansdk.ui.contacts.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.guardiansdk.data.dao.MessageDao
import com.autodroid.guardiansdk.data.dao.ContactDao
import com.autodroid.guardiansdk.data.entity.ContactType
import com.autodroid.guardiansdk.data.entity.Message
import com.autodroid.guardiansdk.data.entity.MessageContent
import com.autodroid.guardiansdk.data.entity.MessageContentSerializer
import com.autodroid.guardiansdk.ui.contacts.detail.model.ContactDetailItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ContactDetailViewModel(
    private val contactDao: ContactDao,
    private val messageDao: MessageDao
) : ViewModel() {

    // 当前用户手机号（应该从配置或登录信息获取）
    private val currentUserPhoneNumber = "13800000000" // TODO: 从配置获取

    private val _detailItems = MutableStateFlow<List<ContactDetailItem>>(emptyList())
    val detailItems: StateFlow<List<ContactDetailItem>> = _detailItems.asStateFlow()

    private var wardPhoneNumber: String = ""

    /**
     * 加载被监护人详情和聊天记录
     */
    fun loadWardDetail(wardPhoneNumber: String) {
        this.wardPhoneNumber = wardPhoneNumber
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = mutableListOf<ContactDetailItem>()

                // 1. 加载被监护人基本信息（Header）
                val contact = contactDao.getContactByPhoneNumber(wardPhoneNumber)
                if (contact != null && contact.type == ContactType.WARD) {
                    items.add(
                        ContactDetailItem.Header(
                            phoneNumber = contact.phoneNumber,
                            name = contact.name,
                            relationship = contact.relationship,
                            alarmCount = contact.alarmCount,
                            lastAlarmTime = contact.lastMessageTime
                        )
                    )
                }

                // 2. 加载聊天记录
                val messages = messageDao.getMessagesByWard(wardPhoneNumber).first()

                // 3. 按日期分组并添加时间分割线
                val groupedMessages = groupMessagesByDate(messages)

                // 4. 添加消息项（使用统一的MessageItem）
                groupedMessages.forEach { (date, msgs) ->
                    // 添加时间分割线
                    items.add(ContactDetailItem.TimeDivider(date.time))

                    // 添加该日期的所有消息
                    msgs.forEach { message ->
                        items.add(ContactDetailItem.MessageItem(message))
                    }
                }

                _detailItems.value = items

            } catch (e: Exception) {
                // 错误处理
                _detailItems.value = emptyList()
            }
        }
    }

    /**
     * 按日期分组消息
     */
    private fun groupMessagesByDate(messages: List<Message>): Map<Date, List<Message>> {
        val calendar = Calendar.getInstance()
        return messages.groupBy { message ->
            calendar.time = Date(message.timestamp)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.time
        }
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = MessageContent.TextMessage(text)
                val message = Message(
                    fromPhoneNumber = currentUserPhoneNumber,
                    toPhoneNumber = wardPhoneNumber,
                    content = MessageContentSerializer.serialize(content)
                )
                messageDao.insert(message)

                // 重新加载数据
                loadWardDetail(wardPhoneNumber)

            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 发送查询消息
     */
    fun sendQueryMessage(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = MessageContent.QueryMessage(query)
                val message = Message(
                    fromPhoneNumber = currentUserPhoneNumber,
                    toPhoneNumber = wardPhoneNumber,
                    content = MessageContentSerializer.serialize(content)
                )
                messageDao.insert(message)

                // 重新加载数据
                loadWardDetail(wardPhoneNumber)

            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 获取当前用户手机号（提供给Adapter判断消息方向）
     */
    fun getCurrentUserPhoneNumber(): String {
        return currentUserPhoneNumber
    }
}