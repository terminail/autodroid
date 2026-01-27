package com.autodroid.teachitback.ui.adapter

import com.autodroid.teachitback.model.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MessagesAdapterTest {

    @Before
    fun setup() {
        
    }

    @Test
    fun testAdapterItemCount() {
        val messages = listOf(
            MessageEntity(
                topicId = "test-topic-id",
                content = "Message 1",
                senderType = "USER",
                messageType = "TEXT"
            ),
            MessageEntity(
                topicId = "test-topic-id",
                content = "Message 2",
                senderType = "AI",
                messageType = "TEXT"
            ),
            MessageEntity(
                topicId = "test-topic-id",
                content = "Message 3",
                senderType = "USER",
                messageType = "TEXT"
            )
        )

        val adapter = ChatAdapter()
        adapter.submitList(messages.map { 
            com.autodroid.teachitback.ui.adapter.ChatItem.UserMessageItem(it)
        })

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun testAdapterEmptyList() {
        val adapter = ChatAdapter()
        adapter.submitList(emptyList())

        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun testUserMessageViewType() {
        val messages = listOf(
            MessageEntity(
                topicId = "test-topic-id",
                content = "User message",
                senderType = "USER",
                messageType = "TEXT"
            )
        )

        val adapter = ChatAdapter()
        adapter.submitList(messages.map { 
            com.autodroid.teachitback.ui.adapter.ChatItem.UserMessageItem(it)
        })

        val viewType = adapter.getItemViewType(0)
        assertEquals(1, viewType)
    }

    @Test
    fun testAIMessageViewType() {
        val messages = listOf(
            MessageEntity(
                topicId = "test-topic-id",
                content = "AI message",
                senderType = "AI",
                messageType = "TEXT"
            )
        )

        val adapter = ChatAdapter()
        adapter.submitList(messages.map { 
            com.autodroid.teachitback.ui.adapter.ChatItem.AIMessageItem(it)
        })

        val viewType = adapter.getItemViewType(0)
        assertEquals(2, viewType)
    }
}