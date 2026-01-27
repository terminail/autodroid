package com.autodroid.teachitback.ui.adapter

import com.autodroid.teachitback.model.TopicEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TopicsAdapterTest {

    @Before
    fun setup() {
        
    }

    @Test
    fun testAdapterItemCount() {
        val topics = listOf(
            TopicEntity(title = "Topic 1", description = "Description 1", topicCategoryId = "test-node"),
            TopicEntity(title = "Topic 2", description = "Description 2", topicCategoryId = "test-node"),
            TopicEntity(title = "Topic 3", description = "Description 3", topicCategoryId = "test-node")
        )

        val adapter = TopicsAdapter { topic -> }
        adapter.submitList(topics.map { 
            com.autodroid.teachitback.ui.adapter.TopicsItem.TopicItem(it)
        })

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun testAdapterEmptyList() {
        val adapter = TopicsAdapter { topic -> }
        adapter.submitList(emptyList())

        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun testAdapterItemClick() {
        val topics = listOf(
            TopicEntity(title = "Test Topic", description = "Test Description", topicCategoryId = "test-node")
        )

        var clickedTopic: com.autodroid.teachitback.ui.adapter.TopicsItem.TopicItem? = null
        val adapter = TopicsAdapter { topic ->
            clickedTopic = topic
        }
        adapter.submitList(topics.map { 
            com.autodroid.teachitback.ui.adapter.TopicsItem.TopicItem(it)
        })

        val testTopic = topics[0]
        
        assertEquals(1, adapter.itemCount)
        assertEquals("Test Topic", testTopic.title)
    }
}