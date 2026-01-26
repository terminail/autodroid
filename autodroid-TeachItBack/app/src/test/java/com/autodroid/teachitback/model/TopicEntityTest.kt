package com.autodroid.teachitback.model

import org.junit.Assert.*
import org.junit.Test

class TopicEntityTest {

    @Test
    fun `should have path field for hierarchical structure`() {
        val topic = TopicEntity(
            title = "测试主题",
            description = "测试描述",
            path = listOf("教育", "数学")
        )
        assertNotNull(topic.path)
        assertEquals(2, topic.path.size)
        assertEquals("教育", topic.path[0])
        assertEquals("数学", topic.path[1])
    }

    @Test
    fun `should have parentId field for hierarchy`() {
        val topic = TopicEntity(
            title = "子主题",
            description = "子主题描述",
            parentId = "parent-id"
        )
        assertNotNull(topic.parentId)
        assertEquals("parent-id", topic.parentId)
    }

    @Test
    fun `should have capabilities field for AI routing`() {
        val topic = TopicEntity(
            title = "数学主题",
            description = "数学主题描述",
            capabilities = setOf(AIAbility.ANSWER_EVALUATION, AIAbility.CREATIVE_WRITING)
        )
        assertNotNull(topic.capabilities)
        assertEquals(2, topic.capabilities.size)
        assertTrue(topic.capabilities.contains(AIAbility.ANSWER_EVALUATION))
    }

    @Test
    fun `should have servicePreferences field for routing`() {
        val topic = TopicEntity(
            title = "测试主题",
            description = "测试描述",
            servicePreferences = mapOf("doubao" to 0.9, "deepseek" to 0.8)
        )
        assertNotNull(topic.servicePreferences)
        assertEquals(2, topic.servicePreferences.size)
        assertEquals(0.9, topic.servicePreferences["doubao"], 0.001)
    }

    @Test
    fun `should support empty path by default`() {
        val topic = TopicEntity(
            title = "根主题",
            description = "根主题描述"
        )
        assertNotNull(topic.path)
        assertTrue(topic.path.isEmpty())
    }

    @Test
    fun `should support null parentId by default`() {
        val topic = TopicEntity(
            title = "根主题",
            description = "根主题描述"
        )
        assertNull(topic.parentId)
    }

    @Test
    fun `should support empty capabilities by default`() {
        val topic = TopicEntity(
            title = "简单主题",
            description = "简单主题描述"
        )
        assertNotNull(topic.capabilities)
        assertTrue(topic.capabilities.isEmpty())
    }

    @Test
    fun `should support empty servicePreferences by default`() {
        val topic = TopicEntity(
            title = "默认主题",
            description = "默认主题描述"
        )
        assertNotNull(topic.servicePreferences)
        assertTrue(topic.servicePreferences.isEmpty())
    }
}
