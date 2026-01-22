package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.database.WhyDao
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.model.WhyEntity
import com.autodroid.teachitback.repository.TopicRepository
import com.autodroid.teachitback.ui.adapter.WhyItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WhyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val topicRepository = TopicRepository(database.topicDao())
    private val whyDao: WhyDao = database.whyDao()
    
    private val _whyItems = MutableStateFlow<List<WhyItem>>(emptyList())
    val whyItems: StateFlow<List<WhyItem>> = _whyItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadWhyData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val whyEntities = whyDao.getAllWhyContent().first()
                val allTopics = topicRepository.getAllTopics().first()
                val presetTopics: List<TopicEntity> = allTopics.filter { topic -> topic.isPreset }
                
                val items = mutableListOf<WhyItem>()
                
                for (entity in whyEntities) {
                    items.add(WhyItem.TextCardItem(
                        title = entity.title,
                        content = entity.content
                    ))
                }
                
                for (topic in presetTopics) {
                    items.add(WhyItem.PresetTopicItem(topic))
                }
                
                _whyItems.value = items
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "加载数据失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun copyPresetTopic(presetTopic: TopicEntity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("WhyViewModel", "Copying preset topic: ${presetTopic.id}, title: ${presetTopic.title}")
                
                val personalTopic = presetTopic.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    isPreset = false,
                    presetTopicId = presetTopic.id,
                    createdAt = System.currentTimeMillis()
                )
                topicRepository.insertTopic(personalTopic)
                
                android.util.Log.d("WhyViewModel", "Created personal topic: ${personalTopic.id}")
                
                val presetMindMap = database.mindMapDao().getByTopicId(presetTopic.id)
                android.util.Log.d("WhyViewModel", "Found preset mindmap: $presetMindMap")
                
                presetMindMap?.let { mindMap ->
                    val newMindMap = mindMap.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        topicId = personalTopic.id,
                        createdAt = System.currentTimeMillis()
                    )
                    database.mindMapDao().insert(newMindMap)
                    
                    android.util.Log.d("WhyViewModel", "Created new mindmap: ${newMindMap.id} for topic: ${personalTopic.id}")
                    
                    val presetNodes = database.mindMapDao().getNodesByMindMapSync(mindMap.id)
                    android.util.Log.d("WhyViewModel", "Found ${presetNodes.size} preset nodes")
                    
                    val nodeIdMap = mutableMapOf<String, String>()
                    
                    // 先复制所有节点，建立ID映射
                    presetNodes.forEach { node ->
                        val newId = java.util.UUID.randomUUID().toString()
                        nodeIdMap[node.id] = newId
                    }
                    
                    // 然后创建新节点，使用映射后的parentId
                    presetNodes.forEach { node ->
                        val newId = nodeIdMap[node.id]!!
                        val newParentId = node.parentId?.let { nodeIdMap[it] }
                        
                        val newNode = node.copy(
                            id = newId,
                            mindMapId = newMindMap.id,
                            parentId = newParentId,
                            createdAt = System.currentTimeMillis()
                        )
                        database.mindMapDao().insertNode(newNode)
                    }
                    
                    android.util.Log.d("WhyViewModel", "Copied ${presetNodes.size} nodes to new mindmap")
                }
                
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("WhyViewModel", "Error copying preset topic", e)
                _errorMessage.value = "复制课程失败: ${e.message}"
                onComplete(false)
            }
        }
    }
    
    suspend fun checkPresetTopicCopied(presetTopicId: String): TopicEntity? {
        return database.topicDao().getPersonalCopyByPresetId(presetTopicId)
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
