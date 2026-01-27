package com.autodroid.teachitback.model

import org.junit.Assert.*
import org.junit.Test

class TopicEntityTest {

    @Test
    fun `should have topicCategoryId field for hierarchical structure`() {
        val topic = TopicEntity(
            title = "测试主题",
            description = "测试描述",
            topicCategoryId = "test-node"
        )
        assertNotNull(topic.topicCategoryId)
        assertEquals("test-node", topic.topicCategoryId)
    }

    @Test
    fun `should have capabilities field for AI routing`() {
        val topic = TopicEntity(
            title = "数学主题",
            description = "数学主题描述",
            topicCategoryId = "test-node",
            capabilities = setOf(com.autodroid.teachitback.service.AIAbility.ANSWER_EVALUATION, com.autodroid.teachitback.service.AIAbility.CREATIVE_WRITING)
        )
        assertNotNull(topic.capabilities)
        assertEquals(2, topic.capabilities.size)
        assertTrue(topic.capabilities.contains(com.autodroid.teachitback.service.AIAbility.ANSWER_EVALUATION))
    }

    @Test
    fun `should have servicePreferences field for routing`() {
        val topic = TopicEntity(
            title = "测试主题",
            description = "测试描述",
            topicCategoryId = "test-node",
            servicePreferences = mapOf("doubao" to 0.9, "deepseek" to 0.8)
        )
        assertNotNull(topic.servicePreferences)
        assertEquals(2, topic.servicePreferences.size)
        assertEquals(0.9, topic.servicePreferences["doubao"]!!, 0.001)
    }

    @Test
    fun `should support empty capabilities by default`() {
        val topic = TopicEntity(
            title = "简单主题",
            description = "简单主题描述",
            topicCategoryId = "test-node"
        )
        assertNotNull(topic.capabilities)
        assertTrue(topic.capabilities.isEmpty())
    }

    @Test
    fun `should support empty servicePreferences by default`() {
        val topic = TopicEntity(
            title = "默认主题",
            description = "默认主题描述",
            topicCategoryId = "test-node"
        )
        assertNotNull(topic.servicePreferences)
        assertTrue(topic.servicePreferences.isEmpty())
    }
}
