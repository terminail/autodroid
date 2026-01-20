package com.autodroid.teachitback.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.api.OpenAIService
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.repository.MessageRepository
import com.autodroid.teachitback.repository.TopicRepository
import kotlinx.coroutines.flow.firstOrNull
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

    // AI Service
    private var aiService: AIService? = null

    fun initializeAI(apiKey: String, model: String = "gpt-3.5-turbo") {
        if (apiKey.isNotBlank()) {
            aiService = OpenAIService(apiKey, model)
        }
    }

    fun sendMessageToAI(topicId: String, topicTitle: String) = viewModelScope.launch {
        val messages = messageRepository.getMessagesByTopic(topicId).firstOrNull() ?: emptyList()
        val aiService = this@AppViewModel.aiService

        if (aiService != null) {
            try {
                val context = "学习主题：$topicTitle"
                val response = aiService.sendMessage(messages, context)

                // Save AI response
                insertMessage(
                    topicId = topicId,
                    content = response,
                    senderType = "AI",
                    messageType = "TEXT"
                )
            } catch (e: Exception) {
                insertMessage(
                    topicId = topicId,
                    content = "错误：${e.message}",
                    senderType = "AI",
                    messageType = "TEXT"
                )
            }
        } else {
            insertMessage(
                topicId = topicId,
                content = "请先在设置中配置 AI API Key",
                senderType = "AI",
                messageType = "TEXT"
            )
        }
    }

    fun processFileWithAI(topicId: String, fileContent: String, topicTitle: String) = viewModelScope.launch {
        val aiService = this@AppViewModel.aiService

        if (aiService != null) {
            try {
                val context = "学习主题：$topicTitle"
                val response = aiService.processFileContent(fileContent, context)

                insertMessage(
                    topicId = topicId,
                    content = response,
                    senderType = "AI",
                    messageType = "FILE_CONTENT"
                )
            } catch (e: Exception) {
                insertMessage(
                    topicId = topicId,
                    content = "文件处理错误：${e.message}",
                    senderType = "AI",
                    messageType = "TEXT"
                )
            }
        }
    }
}
