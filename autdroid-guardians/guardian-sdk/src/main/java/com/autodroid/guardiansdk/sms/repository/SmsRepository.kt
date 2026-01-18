package com.autodroid.guardiansdk.sms.repository

import android.content.Context
import com.autodroid.guardiansdk.sms.model.Conversation
import com.autodroid.guardiansdk.sms.model.SmsMessage
import com.autodroid.guardiansdk.sms.provider.SmsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SmsRepository(context: Context) {
    
    private val smsProvider = SmsProvider(context)
    
    fun getAllConversations(): Flow<List<Conversation>> = flow {
        val conversations = smsProvider.getAllConversations()
        emit(conversations)
    }
    
    fun getActiveConversations(): Flow<List<Conversation>> = flow {
        val conversations = smsProvider.getAllConversations()
        emit(conversations.filter { !it.archived })
    }
    
    fun getArchivedConversations(): Flow<List<Conversation>> = flow {
        val conversations = smsProvider.getAllConversations()
        emit(conversations.filter { it.archived })
    }
    
    suspend fun getUnreadConversationCount(): Int {
        val conversations = smsProvider.getAllConversations()
        return conversations.count { it.unreadCount > 0 }
    }
    
    fun getMessagesByThread(threadId: Long): Flow<List<SmsMessage>> = flow {
        val messages = smsProvider.getMessagesByThread(threadId)
        emit(messages)
    }
    
    fun getMessagesByAddress(address: String): Flow<List<SmsMessage>> = flow {
        val messages = smsProvider.getMessagesByAddress(address)
        emit(messages)
    }
    
    fun getAllMessages(): Flow<List<SmsMessage>> = flow {
        val conversations = smsProvider.getAllConversations()
        val allMessages = mutableListOf<SmsMessage>()
        
        conversations.forEach { conversation ->
            val messages = smsProvider.getMessagesByThread(conversation.threadId)
            allMessages.addAll(messages)
        }
        
        emit(allMessages.sortedByDescending { it.date })
    }
    
    suspend fun sendSms(address: String, body: String): Boolean {
        return smsProvider.sendSms(address, body)
    }
    
    suspend fun markMessageAsRead(messageId: Long): Boolean {
        return smsProvider.markMessageAsRead(messageId)
    }
    
    suspend fun markConversationAsRead(threadId: Long): Boolean {
        return smsProvider.markThreadAsRead(threadId)
    }
    
    suspend fun deleteMessage(messageId: Long): Boolean {
        return smsProvider.deleteMessage(messageId)
    }
    
    suspend fun deleteConversation(threadId: Long): Boolean {
        return smsProvider.deleteThread(threadId)
    }
}
