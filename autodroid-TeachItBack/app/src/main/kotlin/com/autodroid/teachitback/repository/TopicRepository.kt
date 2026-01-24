package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.TopicDao
import com.autodroid.teachitback.model.TopicEntity
import kotlinx.coroutines.flow.Flow

class TopicRepository(private val topicDao: TopicDao) {
    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()

    fun getTopicById(id: String): Flow<TopicEntity?> = topicDao.getTopicById(id)

    suspend fun insertTopic(topic: TopicEntity) = topicDao.insertTopic(topic)

    suspend fun updateTopic(topic: TopicEntity) = topicDao.updateTopic(topic)

    suspend fun deleteTopic(topic: TopicEntity) = topicDao.deleteTopic(topic)

    suspend fun deleteAllTopics() = topicDao.deleteAllTopics()
}
