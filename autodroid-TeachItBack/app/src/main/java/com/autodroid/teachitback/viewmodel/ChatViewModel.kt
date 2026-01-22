package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.api.OpenAIService
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.repository.MessageRepository
import com.autodroid.teachitback.repository.MindMapRepository
import com.autodroid.teachitback.repository.TopicRepository
import com.autodroid.teachitback.ui.adapter.ChatItem
import com.autodroid.teachitback.utils.MindMapDemoValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Chat ViewModel
 * 统一管理ChatFragment所需的所有数据类型：消息、AI响应、MindMap等
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val messageRepository = MessageRepository(database.messageDao())
    private val topicRepository = TopicRepository(database.topicDao())
    val mindMapRepository = MindMapRepository(database)
    
    // AI Service
    private var aiService: AIService? = null
    
    fun initializeAI(apiKey: String, model: String = "gpt-3.5-turbo") {
        if (apiKey.isNotBlank()) {
            aiService = OpenAIService(apiKey, model)
        }
    }
    
    // UI状态
    private val _currentTopic = MutableStateFlow<TopicEntity?>(null)
    val currentTopic: StateFlow<TopicEntity?> = _currentTopic.asStateFlow()
    
    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()
    
    private val _chatItems = MutableStateFlow<List<ChatItem>>(emptyList())
    val chatItems: StateFlow<List<ChatItem>> = _chatItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _validationResult = MutableStateFlow<MindMapDemoValidator.ValidationResult?>(null)
    val validationResult: StateFlow<MindMapDemoValidator.ValidationResult?> = _validationResult.asStateFlow()
    
    /**
     * 加载话题和消息历史
     */
    fun loadTopicAndMessages(topicId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                // 加载话题信息
                val topic = topicRepository.getTopicById(topicId).first()
                _currentTopic.value = topic
                
                // 加载消息历史
                val messages = messageRepository.getMessagesByTopic(topicId).first()
                
                // 加载MindMap
                val mindMap = mindMapRepository.getMindMapByTopicId(topicId)
                val mindMapNodes = if (mindMap != null) {
                    mindMapRepository.getNodesByMindMapId(mindMap.id)
                } else {
                    emptyList()
                }
                
                android.util.Log.d("ChatViewModel", "TopicId: $topicId, MindMap: $mindMap, MindMapNodes count: ${mindMapNodes.size}")
                
                // 构建聊天项列表
                val chatItems = buildChatItems(messages, mindMapNodes)
                _chatItems.value = chatItems
                
            } catch (e: Exception) {
                _errorMessage.value = "加载聊天记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 发送用户消息
     */
    fun sendUserMessage(content: String, topicId: String) {
        viewModelScope.launch {
            try {
                val message = MessageEntity(
                    content = content,
                    senderType = "USER",
                    topicId = topicId,
                    messageType = "TEXT"
                )
                messageRepository.insertMessage(message)
                
                // 添加到显示列表
                val currentItems = _chatItems.value.toMutableList()
                currentItems.add(ChatItem.UserMessageItem(message))
                _chatItems.value = currentItems
                
                // 发送给AI并获取响应
                sendMessageToAI(topicId, _currentTopic.value?.title ?: "")
                
            } catch (e: Exception) {
                _errorMessage.value = "发送消息失败: ${e.message}"
            }
        }
    }
    
    /**
     * 发送消息给AI
     */
    fun sendMessageToAI(topicId: String, topicTitle: String) = viewModelScope.launch {
        val messages = messageRepository.getMessagesByTopic(topicId).first()
        val aiService = this@ChatViewModel.aiService

        if (aiService != null) {
            try {
                val context = "学习主题：$topicTitle"
                val response = aiService.sendMessage(messages, context)

                // Save AI response
                val aiMessage = MessageEntity(
                    topicId = topicId,
                    content = response,
                    senderType = "AI",
                    messageType = "TEXT"
                )
                messageRepository.insertMessage(aiMessage)
                
                // 添加到显示列表
                val currentItems = _chatItems.value.toMutableList()
                currentItems.add(ChatItem.AIMessageItem(aiMessage))
                _chatItems.value = currentItems
                
            } catch (e: Exception) {
                val errorMessage = MessageEntity(
                    topicId = topicId,
                    content = "错误：${e.message}",
                    senderType = "AI",
                    messageType = "TEXT"
                )
                messageRepository.insertMessage(errorMessage)
                
                val currentItems = _chatItems.value.toMutableList()
                currentItems.add(ChatItem.AIMessageItem(errorMessage))
                _chatItems.value = currentItems
            }
        } else {
            val noApiKeyMessage = MessageEntity(
                topicId = topicId,
                content = "请先在设置中配置 AI API Key",
                senderType = "AI",
                messageType = "TEXT"
            )
            messageRepository.insertMessage(noApiKeyMessage)
            
            val currentItems = _chatItems.value.toMutableList()
            currentItems.add(ChatItem.AIMessageItem(noApiKeyMessage))
            _chatItems.value = currentItems
        }
    }
    
    /**
     * 加载话题的思维导图
     */
    fun loadMindMap(topicId: String) {
        viewModelScope.launch {
            try {
                val mindMap = mindMapRepository.getMindMapByTopicId(topicId)
                if (mindMap != null) {
                    val nodes = mindMapRepository.getNodesByMindMapId(mindMap.id)
                    val currentItems = _chatItems.value.toMutableList()
                    currentItems.add(ChatItem.MindMapDisplayItem(
                        mindMapNodes = nodes,
                        title = mindMap.title
                    ))
                    _chatItems.value = currentItems
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载思维导图失败: ${e.message}"
            }
        }
    }
    
    /**
     * 创建MindMap
     */
    fun createMindMap(topicId: String, topicTitle: String) {
        viewModelScope.launch {
            try {
                val mindMap = com.autodroid.teachitback.model.MindMapEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    topicId = topicId,
                    title = topicTitle,
                    structure = "{}",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                mindMapRepository.insertMindMap(mindMap)
                
                kotlinx.coroutines.delay(100)
                
                val rootNode = com.autodroid.teachitback.model.MindMapNode(
                    id = java.util.UUID.randomUUID().toString(),
                    mindMapId = mindMap.id,
                    parentId = null,
                    title = "${topicTitle}学习路径",
                    description = "",
                    progress = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                mindMapRepository.insertMindMapNode(rootNode)
                
                val defaultNodes = listOf(
                    "基础概念",
                    "重点难点", 
                    "应用练习",
                    "知识扩展"
                )
                
                val childNodes = defaultNodes.map { nodeTitle ->
                    com.autodroid.teachitback.model.MindMapNode(
                        id = java.util.UUID.randomUUID().toString(),
                        mindMapId = mindMap.id,
                        parentId = rootNode.id,
                        title = nodeTitle,
                        description = "",
                        progress = 0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                mindMapRepository.insertMindMapNodes(childNodes)
                
                loadMindMap(topicId)
            } catch (e: Exception) {
                _errorMessage.value = "创建思维导图失败: ${e.message}"
            }
        }
    }
    
    /**
     * 验证Topic数据
     */
    fun validateTopic(topic: TopicEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val validator = MindMapDemoValidator()
                val result = validator.validateTopic(topic)
                _validationResult.value = result
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "验证失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 构建聊天项列表
     */
    private fun buildChatItems(messages: List<MessageEntity>, mindMapNodes: List<com.autodroid.teachitback.model.MindMapNode>): List<ChatItem> {
        val items = mutableListOf<ChatItem>()
        
        // 添加消息项
        messages.forEach { message ->
            if (message.senderType == "USER") {
                items.add(ChatItem.UserMessageItem(message))
            } else {
                items.add(ChatItem.AIMessageItem(message))
            }
        }
        
        // 添加MindMap项（如果有MindMap数据）
        if (mindMapNodes.isNotEmpty()) {
            val mindMapItem = ChatItem.MindMapDisplayItem(
                mindMapNodes = mindMapNodes,
                title = "思维导图"
            )
            
            // 根据消息数量决定MindMap位置
            if (messages.size < 10) {
                // 消息少于10个时，MindMap显示在第一个位置
                items.add(0, mindMapItem)
            } else {
                // 消息大于等于10个时，MindMap显示在倒数第10个位置
                val insertPosition = items.size - 10
                items.add(insertPosition, mindMapItem)
            }
        }
        
        return items
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 清除验证结果
     */
    fun clearValidationResult() {
        _validationResult.value = null
    }
}