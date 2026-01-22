package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.api.TencentCloudAIService
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
 * 集成腾讯云AI服务，实现Local-First策略
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)

    // AI Service（延迟初始化）
    private var aiService: TencentCloudAIService? = null

    // Repositories（在initializeAI之后初始化）
    private lateinit var messageRepository: MessageRepository
    val mindMapRepository: MindMapRepository by lazy {
        MindMapRepository(database, aiService!!)
    }
    private val topicRepository = TopicRepository(database.topicDao())

    /**
     * 初始化腾讯云AI服务
     */
    fun initializeAI(apiKey: String, secretId: String, testMode: Boolean = false) {
        if (apiKey.isNotBlank() && secretId.isNotBlank()) {
            aiService = com.autodroid.teachitback.impl.TencentCloudAIServiceImpl(
                context = getApplication(),
                apiKey = apiKey,
                secretId = secretId,
                testMode = testMode
            )
            // 初始化MessageRepository（需要aiService）
            messageRepository = MessageRepository(database.messageDao(), aiService!!)
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

    // 学习进度分析
    private val _progressAnalysis = MutableStateFlow<com.autodroid.teachitback.model.ProgressAnalysis?>(null)
    val progressAnalysis: StateFlow<com.autodroid.teachitback.model.ProgressAnalysis?> = _progressAnalysis.asStateFlow()
    
    /**
     * 加载话题和消息历史
     * 包含AI增强的进度分析
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

                // AI增强：分析学习进度（如果有足够消息）
                if (messages.size >= 3 && aiService != null) {
                    try {
                        val progressAnalysis = aiService!!.analyzeLearningProgress(messages)
                        _progressAnalysis.value = progressAnalysis
                    } catch (e: Exception) {
                        android.util.Log.w("ChatViewModel", "进度分析失败: ${e.message}")
                        _progressAnalysis.value = null
                    }
                } else {
                    _progressAnalysis.value = null
                }

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
     * 发送用户消息并获取AI回复
     * 使用MessageRepository的Local-First策略
     */
    fun sendUserMessage(content: String, topicId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // 使用MessageRepository的sendMessageAndGetReply方法
                // 该方法自动处理：保存用户消息、调用AI、保存AI回复
                val aiMessage = messageRepository.sendMessageAndGetReply(topicId, content)

                if (aiMessage != null) {
                    // 刷新消息列表
                    loadTopicAndMessages(topicId)
                } else {
                    _errorMessage.value = "获取AI回复失败"
                }

            } catch (e: Exception) {
                _errorMessage.value = "发送消息失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
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

    /**
     * 清除进度分析
     */
    fun clearProgressAnalysis() {
        _progressAnalysis.value = null
    }
}