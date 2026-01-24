package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.MessageDao
import com.autodroid.teachitback.model.MessageEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * MessageRepository单元测试
 * 验证Repository与AIServiceRouter的集成
 */
class MessageRepositoryTest {

    private lateinit var repository: MessageRepository
    private lateinit var mockDao: MessageDao

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockDao = mockk<MessageDao>(relaxed = true)

        // Repository只需要MessageDao，不需要注入AIServiceRouter
        // AIServiceRouter是Repository内部的单例，对测试透明
        repository = MessageRepository(mockDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testSendMessageAndGetReply_Success() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"
        val userContent = "你好，我想学习Kotlin"
        val aiResponseContent = "你好！我很乐意帮助你学习Kotlin。"

        // Mock DAO行为
        coEvery { mockDao.insertMessage(any()) } returns Unit
        coEvery { mockDao.getMessagesByTopicSync(topicId) } returns listOf()

        // 执行测试
        val result = repository.sendMessageAndGetReply(topicId, userContent)

        // 验证结果
        assertNotNull(result)
        assertEquals("AI", result.senderType)
        assertEquals(aiResponseContent, result.content)

        // 验证DAO调用
        coVerify(exactly = 2) { mockDao.insertMessage(any()) } // 用户消息 + AI回复
        coVerify { mockDao.getMessagesByTopicSync(topicId) }
    }

    @Test
    fun testSendMessageAndGetReply_WithConversationHistory() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"
        val userContent = "请解释一下lambda表达式"
        val conversationHistory = listOf(
            MessageEntity(
                id = "msg-1",
                topicId = topicId,
                content = "你好",
                senderType = "USER",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            ),
            MessageEntity(
                id = "msg-2",
                topicId = topicId,
                content = "你好！有什么可以帮助你的吗？",
                senderType = "AI",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            )
        )

        // Mock DAO行为
        coEvery { mockDao.insertMessage(any()) } returns Unit
        coEvery { mockDao.getMessagesByTopicSync(topicId) } returns conversationHistory

        // 执行测试
        val result = repository.sendMessageAndGetReply(topicId, userContent)

        // 验证结果
        assertNotNull(result)
        assertEquals("AI", result.senderType)

        // 验证DAO调用
        coVerify(exactly = 2) { mockDao.insertMessage(any()) }
        coVerify { mockDao.getMessagesByTopicSync(topicId) }
    }

    @Test
    fun testGetMessagesByTopic() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"
        val messages = listOf(
            MessageEntity(
                id = "msg-1",
                topicId = topicId,
                content = "消息1",
                senderType = "USER",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            ),
            MessageEntity(
                id = "msg-2",
                topicId = topicId,
                content = "消息2",
                senderType = "AI",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            )
        )

        // Mock DAO行为
        coEvery { mockDao.getMessagesByTopic(topicId) } returns flowOf(messages)

        // 执行测试
        val result = repository.getMessagesByTopic(topicId).first()

        // 验证结果
        assertEquals(2, result.size)
        assertEquals("消息1", result[0].content)
        assertEquals("消息2", result[1].content)
    }

    @Test
    fun testInsertMessage() = runBlocking {
        // 准备测试数据
        val message = MessageEntity(
            id = "msg-1",
            topicId = "topic-123",
            content = "测试消息",
            senderType = "USER",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )

        // Mock DAO行为
        coEvery { mockDao.insertMessage(message) } returns Unit

        // 执行测试
        repository.insertMessage(message)

        // 验证DAO调用
        coVerify { mockDao.insertMessage(message) }
    }

    @Test
    fun testUpdateMessage() = runBlocking {
        // 准备测试数据
        val message = MessageEntity(
            id = "msg-1",
            topicId = "topic-123",
            content = "更新后的消息",
            senderType = "USER",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )

        // Mock DAO行为
        coEvery { mockDao.updateMessage(message) } returns Unit

        // 执行测试
        repository.updateMessage(message)

        // 验证DAO调用
        coVerify { mockDao.updateMessage(message) }
    }

    @Test
    fun testDeleteMessagesByTopic() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"

        // Mock DAO行为
        coEvery { mockDao.deleteMessagesByTopic(topicId) } returns Unit

        // 执行测试
        repository.deleteMessagesByTopic(topicId)

        // 验证DAO调用
        coVerify { mockDao.deleteMessagesByTopic(topicId) }
    }
}
