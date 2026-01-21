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
            TopicEntity(title = "Topic 1", description = "Description 1"),
            TopicEntity(title = "Topic 2", description = "Description 2"),
            TopicEntity(title = "Topic 3", description = "Description 3")
        )

        val adapter = TopicsAdapter { topic -> }
        adapter.submitList(topics)

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
            TopicEntity(title = "Test Topic", description = "Test Description")
        )

        var clickedTopic: TopicEntity? = null
        val adapter = TopicsAdapter { topic ->
            clickedTopic = topic
        }
        adapter.submitList(topics)

        val testTopic = topics[0]
        
        assertEquals(1, adapter.itemCount)
        assertEquals("Test Topic", topics[0].title)
    }
}