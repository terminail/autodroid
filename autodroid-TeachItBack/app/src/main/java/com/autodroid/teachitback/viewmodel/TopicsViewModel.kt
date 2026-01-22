package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.repository.TopicRepository
import com.autodroid.teachitback.ui.adapter.TopicsItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * TopicsFragment的ViewModel
 * 负责管理话题列表界面的所有业务逻辑和数据
 */
class TopicsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = TopicRepository(database.topicDao())
    
    private val _topicsItems = MutableLiveData<List<TopicsItem>>()
    val topicsItems: LiveData<List<TopicsItem>> = _topicsItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        observeTopics()
    }
    
    /**
     * 观察话题列表变化
     */
    private fun observeTopics() {
        viewModelScope.launch {
            repository.getAllTopics().collect { topics ->
                val items = buildTopicsItems(topics)
                _topicsItems.value = items
            }
        }
    }
    
    /**
     * 创建新话题
     */
    fun insertTopic(title: String, description: String) {
        viewModelScope.launch {
            try {
                val topic = com.autodroid.teachitback.model.TopicEntity(
                    title = title,
                    description = description,
                    isPreset = false
                )
                repository.insertTopic(topic)
            } catch (e: Exception) {
                _errorMessage.value = "创建话题失败: ${e.message}"
            }
        }
    }
    
    /**
     * 更新话题
     */
    fun updateTopic(topic: com.autodroid.teachitback.model.TopicEntity) {
        viewModelScope.launch {
            try {
                repository.updateTopic(topic)
            } catch (e: Exception) {
                _errorMessage.value = "更新话题失败: ${e.message}"
            }
        }
    }
    
    /**
     * 删除话题
     */
    fun deleteTopic(topic: com.autodroid.teachitback.model.TopicEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTopic(topic)
            } catch (e: Exception) {
                _errorMessage.value = "删除话题失败: ${e.message}"
            }
        }
    }
    
    /**
     * 构建话题列表项
     */
    private fun buildTopicsItems(topics: List<TopicEntity>): List<TopicsItem> {
        val items = mutableListOf<TopicsItem>()
        
        // 添加预置话题分类
        val presetTopics = topics.filter { it.isPreset }
        if (presetTopics.isNotEmpty()) {
            items.add(TopicsItem.SectionHeaderItem("预置话题", "系统提供的学习话题"))
            presetTopics.forEach { topic ->
                items.add(TopicsItem.TopicItem(topic))
            }
        }
        
        // 添加用户话题分类
        val userTopics = topics.filter { !it.isPreset }
        if (userTopics.isNotEmpty()) {
            items.add(TopicsItem.SectionHeaderItem("我的话题", "您创建的学习话题"))
            userTopics.forEach { topic ->
                items.add(TopicsItem.TopicItem(topic))
            }
        }
        
        // 如果没有话题，显示空状态
        if (topics.isEmpty()) {
            items.add(TopicsItem.EmptyStateItem())
        }
        
        // 添加添加按钮
        items.add(TopicsItem.AddButtonItem())
        
        return items
    }
    
    /**
     * 创建新话题
     */
    fun createTopic(title: String, description: String) {
        viewModelScope.launch {
            try {
                val newTopic = TopicEntity(
                    title = title,
                    description = description,
                    masteryLevel = 0,
                    isPreset = false
                )
                repository.insertTopic(newTopic)
            } catch (e: Exception) {
                _errorMessage.value = "创建话题失败: ${e.message}"
            }
        }
    }
    
    /**
     * 删除话题
     */
    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            try {
                // 需要先获取话题实体，然后删除
                val topics = repository.getAllTopics().first()
                val topicToDelete = topics.find { it.id == topicId }
                topicToDelete?.let { topic ->
                    repository.deleteTopic(topic)
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除话题失败: ${e.message}"
            }
        }
    }
    
    /**
     * 更新话题掌握程度
     */
    fun updateTopicMastery(topicId: String, masteryLevel: Int) {
        viewModelScope.launch {
            try {
                // 需要先获取话题实体，然后更新
                val topics = repository.getAllTopics().first()
                val topicToUpdate = topics.find { it.id == topicId }
                topicToUpdate?.let { topic ->
                    val updatedTopic = topic.copy(masteryLevel = masteryLevel)
                    repository.updateTopic(updatedTopic)
                }
            } catch (e: Exception) {
                _errorMessage.value = "更新掌握程度失败: ${e.message}"
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}