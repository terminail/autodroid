package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.TopicDao
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.service.AIAbility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TopicRepository(private val topicDao: TopicDao) {
    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()

    fun getTopicById(id: String): Flow<TopicEntity?> = topicDao.getTopicById(id)

    suspend fun insertTopic(topic: TopicEntity) = topicDao.insertTopic(topic)

    suspend fun updateTopic(topic: TopicEntity) = topicDao.updateTopic(topic)

    suspend fun deleteTopic(topic: TopicEntity) = topicDao.deleteTopic(topic)

    suspend fun deleteAllTopics() = topicDao.deleteAllTopics()
    
    // ===== 基于新字段的管理功能 =====
    
    fun getTopicsByTreeNode(topicTreeNodeId: String): Flow<List<TopicEntity>> = 
        topicDao.getTopicsByTreeNode(topicTreeNodeId)
    
    fun getTopicsByCapability(capability: AIAbility): Flow<List<TopicEntity>> = 
        topicDao.getTopicsByCapability(capability.name)
    
    fun searchTopicsByTitle(query: String): Flow<List<TopicEntity>> = 
        topicDao.searchTopicsByTitle(query)
    
    suspend fun updateTopicMasteryLevel(topicId: String, masteryLevel: Int) {
        val topic = topicDao.getTopicByIdSync(topicId)
        topic?.let {
            val updatedTopic = it.copy(masteryLevel = masteryLevel)
            topicDao.updateTopic(updatedTopic)
        }
    }
    
    suspend fun updateTopicServicePreferences(topicId: String, servicePreferences: Map<String, Double>) {
        val topic = topicDao.getTopicByIdSync(topicId)
        topic?.let {
            val updatedTopic = it.copy(servicePreferences = servicePreferences)
            topicDao.updateTopic(updatedTopic)
        }
    }
}
