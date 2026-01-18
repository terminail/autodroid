package com.autodroid.sms.data.local

import android.content.Context
import android.util.Log
import com.autodroid.sms.data.model.Conversation
import com.autodroid.sms.data.model.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * 本地短信存储
 * 由于 Android 4.4+ 中 WRITE_SMS 权限已被废弃，
 * 应用无法直接写入系统短信数据库，只能使用本地存储
 */
class LocalSmsStorage(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalSmsStorage"
        
        // 本地存储的短信
        private val localMessages = ConcurrentHashMap<Long, SmsMessage>()
        
        // 本地存储的会话
        private val localConversations = ConcurrentHashMap<Long, Conversation>()
        
        // 线程计数器
        private var threadCounter = 1000000L // 从大数字开始，避免与系统线程ID冲突
    }
    
    /**
     * 保存发送的短信到本地存储
     */
    suspend fun saveSentMessage(address: String, body: String): Long = withContext(Dispatchers.IO) {
        try {
            val messageId = System.currentTimeMillis()
            val threadId = getOrCreateThreadId(address)
            
            val message = SmsMessage(
                id = messageId,
                threadId = threadId,
                address = address,
                body = body,
                date = Date(),
                type = SmsMessage.TYPE_SENT,
                status = 0 // 0: 待发送
            )
            
            localMessages[messageId] = message
            
            // 更新会话
            updateConversation(threadId, address, body, message.date)
            
            Log.d(TAG, "Saved local sent message: messageId=$messageId, address=$address, body=$body")
            messageId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save local sent message", e)
            0L
        }
    }
    
    /**
     * 获取或创建线程ID
     */
    private fun getOrCreateThreadId(address: String): Long {
        // 查找现有的线程ID
        val existingConversation = localConversations.values.find { it.address == address }
        if (existingConversation != null) {
            return existingConversation.threadId
        }
        
        // 创建新的线程ID
        val threadId = threadCounter++
        
        val conversation = Conversation(
            threadId = threadId,
            address = address,
            contactName = null,
            snippet = "",
            date = Date(),
            messageCount = 0,
            unreadCount = 0,
            read = true,
            archived = false,
            blocked = false,
            mute = false
        )
        
        localConversations[threadId] = conversation
        return threadId
    }
    
    /**
     * 更新会话
     */
    private fun updateConversation(threadId: Long, address: String, snippet: String, date: Date) {
        val conversation = localConversations[threadId] ?: Conversation(
            threadId = threadId,
            address = address,
            contactName = null,
            snippet = snippet,
            date = date,
            messageCount = 1,
            unreadCount = 0,
            read = true,
            archived = false,
            blocked = false,
            mute = false
        )
        
        conversation.snippet = snippet
        conversation.date = date
        conversation.messageCount++
        
        localConversations[threadId] = conversation
    }
    
    /**
     * 获取所有本地会话（合并系统会话和本地会话）
     */
    suspend fun getAllConversations(systemConversations: List<Conversation>): List<Conversation> = withContext(Dispatchers.IO) {
        val mergedConversations = mutableListOf<Conversation>()
        
        // 添加系统会话
        mergedConversations.addAll(systemConversations)
        
        // 添加本地会话
        val localConversationList = localConversations.values.toList()
        
        // 合并重复的会话（基于地址）
        val addressMap = mutableMapOf<String, Conversation>()
        
        // 先添加系统会话
        systemConversations.forEach { conversation ->
            addressMap[conversation.address] = conversation
        }
        
        // 然后添加本地会话（如果地址不存在）
        localConversationList.forEach { conversation ->
            if (!addressMap.containsKey(conversation.address)) {
                addressMap[conversation.address] = conversation
            }
        }
        
        mergedConversations.clear()
        mergedConversations.addAll(addressMap.values.sortedByDescending { it.date })
        
        Log.d(TAG, "Merged conversations: system=${systemConversations.size}, local=${localConversationList.size}, merged=${mergedConversations.size}")
        mergedConversations
    }
    
    /**
     * 获取指定会话的消息（合并系统消息和本地消息）
     */
    suspend fun getMessagesByThread(threadId: Long, systemMessages: List<SmsMessage>): List<SmsMessage> = withContext(Dispatchers.IO) {
        val mergedMessages = mutableListOf<SmsMessage>()
        
        // 添加系统消息
        mergedMessages.addAll(systemMessages)
        
        // 添加本地消息
        val localMessageList = localMessages.values
            .filter { it.threadId == threadId }
            .sortedByDescending { it.date }
        
        mergedMessages.addAll(localMessageList)
        
        // 按日期排序
        mergedMessages.sortByDescending { it.date }
        
        Log.d(TAG, "Merged messages for thread $threadId: system=${systemMessages.size}, local=${localMessageList.size}, merged=${mergedMessages.size}")
        mergedMessages
    }
    
    /**
     * 更新短信状态
     */
    suspend fun updateMessageStatus(messageId: Long, status: Int) = withContext(Dispatchers.IO) {
        val message = localMessages[messageId]
        if (message != null) {
            message.status = status
            localMessages[messageId] = message
            Log.d(TAG, "Updated local message status: messageId=$messageId, status=$status")
        }
    }
    
    /**
     * 清除所有本地数据
     */
    fun clearAll() {
        localMessages.clear()
        localConversations.clear()
        Log.d(TAG, "Cleared all local SMS data")
    }
}