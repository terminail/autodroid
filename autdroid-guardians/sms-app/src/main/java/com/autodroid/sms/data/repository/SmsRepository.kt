package com.autodroid.sms.data.repository

import android.content.Context
import com.autodroid.sms.data.model.Conversation
import com.autodroid.sms.data.model.SmsMessage
import com.autodroid.sms.data.provider.SmsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 短信数据仓库
 * 统一管理短信数据的访问，直接使用SmsProvider
 */
class SmsRepository(context: Context) {
    
    private val smsProvider = SmsProvider(context)
    
    /**
     * 获取所有会话
     */
    fun getAllConversations(): Flow<List<Conversation>> = flow {
        val conversations = smsProvider.getAllConversations()
        emit(conversations)
    }
    
    /**
     * 获取活跃会话（非归档）
     */
    fun getActiveConversations(): Flow<List<Conversation>> = flow {
        val conversations = smsProvider.getAllConversations()
        emit(conversations.filter { !it.archived })
    }
    
    /**
     * 获取归档会话
     */
    fun getArchivedConversations(): Flow<List<Conversation>> = flow {
        val conversations = smsProvider.getAllConversations()
        emit(conversations.filter { it.archived })
    }
    
    /**
     * 获取未读会话数量
     */
    suspend fun getUnreadConversationCount(): Int {
        val conversations = smsProvider.getAllConversations()
        return conversations.count { it.unreadCount > 0 }
    }
    
    /**
     * 获取指定会话的消息
     */
    fun getMessagesByThread(threadId: Long): Flow<List<SmsMessage>> = flow {
        val messages = smsProvider.getMessagesByThread(threadId)
        emit(messages)
    }
    
    fun getMessagesByAddress(address: String): Flow<List<SmsMessage>> = flow {
        val messages = smsProvider.getMessagesByAddress(address)
        emit(messages)
    }
    
    /**
     * 获取所有消息
     */
    fun getAllMessages(): Flow<List<SmsMessage>> = flow {
        // 获取所有会话，然后获取每个会话的消息
        val conversations = smsProvider.getAllConversations()
        val allMessages = mutableListOf<SmsMessage>()
        
        conversations.forEach { conversation ->
            val messages = smsProvider.getMessagesByThread(conversation.threadId)
            allMessages.addAll(messages)
        }
        
        emit(allMessages.sortedByDescending { it.date })
    }
    
    /**
     * 发送短信
     */
    suspend fun sendSms(address: String, body: String): Boolean {
        return smsProvider.sendSms(address, body)
    }
    
    /**
     * 标记消息为已读
     */
    suspend fun markMessageAsRead(messageId: Long): Boolean {
        return smsProvider.markMessageAsRead(messageId)
    }
    
    /**
     * 标记会话为已读
     */
    suspend fun markConversationAsRead(threadId: Long): Boolean {
        return smsProvider.markThreadAsRead(threadId)
    }
    
    /**
     * 删除消息
     */
    suspend fun deleteMessage(messageId: Long): Boolean {
        return smsProvider.deleteMessage(messageId)
    }
    
    /**
     * 删除会话
     */
    suspend fun deleteConversation(threadId: Long): Boolean {
        return smsProvider.deleteThread(threadId)
    }
}