package com.autodroid.sms.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.autodroid.sms.data.model.Conversation
import com.autodroid.sms.data.model.SmsMessage
import com.autodroid.sms.data.repository.SmsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ConversationViewModel(context: Context) : ViewModel() {
    
    private val repository = SmsRepository(context)
    
    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations
    
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _messages = MutableLiveData<List<SmsMessage>>()
    val messages: LiveData<List<SmsMessage>> = _messages
    
    init {
        refreshConversations()
    }
    
    fun refreshConversations() {
        _isLoading.postValue(true)
        
        viewModelScope.launch {
            try {
                repository.getActiveConversations().collect { conversations ->
                    _conversations.postValue(conversations)
                }
                
                val count = repository.getUnreadConversationCount()
                _unreadCount.postValue(count)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    
    fun getMessagesByThread(threadId: Long): LiveData<List<SmsMessage>> {
        viewModelScope.launch {
            repository.getMessagesByThread(threadId).collect { messages ->
                _messages.postValue(messages)
            }
        }
        return _messages
    }
    
    fun getMessagesByAddress(address: String): LiveData<List<SmsMessage>> {
        viewModelScope.launch {
            repository.getMessagesByAddress(address).collect { messages ->
                _messages.postValue(messages)
            }
        }
        return _messages
    }
    
    fun markConversationAsRead(threadId: Long) {
        viewModelScope.launch {
            repository.markConversationAsRead(threadId)
            refreshConversations()
        }
    }
    
    fun deleteConversation(threadId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(threadId)
            refreshConversations()
        }
    }
}