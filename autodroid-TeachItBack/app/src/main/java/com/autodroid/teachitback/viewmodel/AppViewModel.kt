package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.repository.MessageRepository
import com.autodroid.teachitback.repository.TopicRepository
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val topicRepository = TopicRepository(database.topicDao())
    private val messageRepository = MessageRepository(database.messageDao())

    // Topic LiveData
    val topics: LiveData<List<TopicEntity>> = topicRepository.getAllTopics().asLiveData()

    // Message LiveData
    private var _currentTopicMessages: LiveData<List<MessageEntity>>? = null
    val currentTopicMessages: LiveData<List<MessageEntity>>?
        get() = _currentTopicMessages

    fun loadMessagesForTopic(topicId: String) {
        _currentTopicMessages = messageRepository.getMessagesByTopic(topicId).asLiveData()
    }

    // Topic operations
    fun insertTopic(title: String, description: String) = viewModelScope.launch {
        val topic = TopicEntity(title = title, description = description)
        topicRepository.insertTopic(topic)
    }

    fun updateTopic(topic: TopicEntity) = viewModelScope.launch {
        topicRepository.updateTopic(topic)
    }

    fun deleteTopic(topic: TopicEntity) = viewModelScope.launch {
        topicRepository.deleteTopic(topic)
    }

    // Message operations
    fun insertMessage(topicId: String, content: String, senderType: String, messageType: String = "TEXT") =
        viewModelScope.launch {
            val message = MessageEntity(
                topicId = topicId,
                content = content,
                senderType = senderType,
                messageType = messageType
            )
            messageRepository.insertMessage(message)
        }

    fun updateMessage(message: MessageEntity) = viewModelScope.launch {
        messageRepository.updateMessage(message)
    }
}
