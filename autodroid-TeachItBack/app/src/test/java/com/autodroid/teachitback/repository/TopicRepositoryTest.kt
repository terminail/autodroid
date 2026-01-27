package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.TopicDao
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.service.AIAbility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class TopicRepositoryTest {

    @Test
    fun `should get topics by tree node`() = runBlocking {
        val topicDao = mock<TopicDao>()
        val repository = TopicRepository(topicDao)
        
        val treeNodeId = "education-node"
        val expectedTopics = listOf(
            TopicEntity(
                title = "代数", 
                description = "代数主题", 
                topicCategoryId = treeNodeId
            ),
            TopicEntity(
                title = "几何", 
                description = "几何主题", 
                topicCategoryId = treeNodeId
            )
        )
        
        whenever(topicDao.getTopicsByCategoryId(treeNodeId)).thenReturn(flowOf(expectedTopics))
        
        val result = repository.getTopicsByTreeNode(treeNodeId).first()
        
        assertEquals(expectedTopics.size, result.size)
        assertEquals(treeNodeId, result[0].topicCategoryId)
        verify(topicDao).getTopicsByCategoryId(treeNodeId)
    }

    @Test
    fun `should get topics by capability`() = runBlocking {
        val topicDao = mock<TopicDao>()
        val repository = TopicRepository(topicDao)
        
        val capability = AIAbility.ANSWER_EVALUATION
        val expectedTopics = listOf(
            TopicEntity(
                title = "数学",
                description = "数学主题",
                topicCategoryId = "math-node",
                capabilities = setOf(capability, AIAbility.MATH)
            ),
            TopicEntity(
                title = "物理",
                description = "物理主题",
                topicCategoryId = "physics-node",
                capabilities = setOf(capability, AIAbility.CODE_GENERATION)
            )
        )
        
        whenever(topicDao.getTopicsByCapability(capability.name)).thenReturn(flowOf(expectedTopics))
        
        val result = repository.getTopicsByCapability(capability).first()
        
        assertEquals(expectedTopics.size, result.size)
        assertTrue(result[0].capabilities.contains(capability))
        verify(topicDao).getTopicsByCapability(capability.name)
    }

    @Test
    fun `should update topic mastery level`() = runBlocking {
        val topicDao = mock<TopicDao>()
        val repository = TopicRepository(topicDao)
        
        val topicId = "topic-123"
        val newMasteryLevel = 85
        val topic = TopicEntity(
            id = topicId,
            title = "测试主题",
            description = "测试描述",
            topicCategoryId = "test-node",
            masteryLevel = 50
        )
        
        val updatedTopic = topic.copy(masteryLevel = newMasteryLevel)
        
        // Mock the dao to return the topic and then update
        whenever(topicDao.getTopicById(topicId)).thenReturn(flowOf(topic))
        
        repository.updateTopicMasteryLevel(topicId, newMasteryLevel)
        
        // Verify that updateTopic was called with the correct mastery level
        verify(topicDao).updateTopic(argThat { 
            this.id == topicId && this.masteryLevel == newMasteryLevel 
        })
    }

    @Test
    fun `should search topics by title`() = runBlocking {
        val topicDao = mock<TopicDao>()
        val repository = TopicRepository(topicDao)
        
        val query = "数学"
        val expectedTopics = listOf(
            TopicEntity(title = "数学基础", description = "数学基础主题", topicCategoryId = "test-node"),
            TopicEntity(title = "高等数学", description = "高等数学主题", topicCategoryId = "test-node")
        )
        
        whenever(topicDao.searchTopicsByTitle(query)).thenReturn(flowOf(expectedTopics))
        
        val result = repository.searchTopicsByTitle(query).first()
        
        assertEquals(expectedTopics.size, result.size)
        assertTrue(result.all { it.title.contains(query) })
        verify(topicDao).searchTopicsByTitle(query)
    }

    @Test
    fun `should update topic service preferences`() = runBlocking {
        val topicDao = mock<TopicDao>()
        val repository = TopicRepository(topicDao)
        
        val topicId = "topic-123"
        val servicePreferences = mapOf("doubao" to 0.9, "deepseek" to 0.8)
        val topic = TopicEntity(
            id = topicId,
            title = "测试主题",
            description = "测试描述",
            topicCategoryId = "test-node",
            servicePreferences = emptyMap()
        )
        
        val updatedTopic = topic.copy(servicePreferences = servicePreferences)
        
        whenever(topicDao.getTopicById(topicId)).thenReturn(flowOf(topic))
        
        repository.updateTopicServicePreferences(topicId, servicePreferences)
        
        verify(topicDao).updateTopic(argThat {
            this.id == topicId && this.servicePreferences == servicePreferences
        })
    }
}
