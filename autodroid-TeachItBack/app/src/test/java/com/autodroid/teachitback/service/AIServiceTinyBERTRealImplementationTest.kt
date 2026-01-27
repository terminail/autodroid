package com.autodroid.teachitback.service

import android.content.Context
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.framework.MNNIntegration
import com.autodroid.teachitback.framework.MNNModel
import com.autodroid.teachitback.model.AIServiceResponse
import com.autodroid.teachitback.model.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class AIServiceTinyBERTRealImplementationTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockMNNIntegration: MNNIntegration

    @Mock
    private lateinit var mockModel: MNNModel

    private lateinit var config: AIServiceConfig.TinyBERTConfig
    private lateinit var tinyBERTService: AIServiceTinyBERT

    @Before
    fun setup() {
        config = AIServiceConfig.TinyBERTConfig(
            id = "tinybert-test",
            displayName = "TinyBERT Test",
            description = "Test TinyBERT service"
        )

        tinyBERTService = AIServiceTinyBERT(mockContext, config, mockMNNIntegration)
    }

    @Test
    fun testInitialize_UsesMNNIntegration() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)

        val result = tinyBERTService.initialize()

        assertTrue("初始化应该成功", result)
        verify(mockMNNIntegration).isModelDownloaded(eq("models/tinybert-int8.mnn"))
        verify(mockMNNIntegration).loadModel(eq("models/tinybert-int8.mnn"))
        verify(mockModel).load()
    }

    @Test
    fun testSendMessage_UsesRealMNNInference() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)
        `when`(mockModel.inference(any())).thenReturn("真实推理结果")

        tinyBERTService.initialize()

        val message = MessageEntity(
            topicId = "test_topic",
            content = "测试问题",
            senderType = "USER",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )

        val response = tinyBERTService.sendMessage(message, "测试上下文")

        assertNotNull("响应不应该为空", response)
        assertNotNull("响应内容不应该为空", response.content)
        assertNotEquals("响应内容不应该包含'模拟'字样", 
            response.content, "模拟推理结果: 测试问题")
        assertNotEquals("响应内容不应该包含'fake'字样", 
            response.content.lowercase(), "fake")
        
        verify(mockModel).inference(any())
    }

    @Test
    fun testCheckStatus_UsesRealInference() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)
        `when`(mockModel.inference(any())).thenReturn("测试响应")

        tinyBERTService.initialize()

        val status = tinyBERTService.checkStatus()

        assertNotNull("状态不应该为空", status)
        assertEquals("状态码应该是200", 200, status.code)
        assertTrue("状态应该是OK", status.isOk)
        
        verify(mockModel).inference(eq("Hello"))
    }

    @Test
    fun testInference_UsesRealMNNModel() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)
        `when`(mockModel.inference(any())).thenReturn("真实推理结果")

        tinyBERTService.initialize()

        val message = MessageEntity(
            topicId = "test_topic",
            content = "测试输入",
            senderType = "USER",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )

        val response = tinyBERTService.sendMessage(message, "")

        assertNotNull("响应不应该为空", response)
        verify(mockModel).inference(any())
    }

    @Test
    fun testNoFakeImplementation_NoDelaySimulation() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)
        `when`(mockModel.inference(any())).thenReturn("真实结果")

        tinyBERTService.initialize()

        val startTime = System.currentTimeMillis()
        val response = tinyBERTService.sendMessage(
            MessageEntity(
                topicId = "test_topic",
                content = "测试",
                senderType = "USER",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            ), ""
        )
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        assertNotNull("响应不应该为空", response)
        
        verify(mockModel).inference(any())
    }

    @Test
    fun testRelease_UsesMNNModelRelease() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)

        tinyBERTService.initialize()
        tinyBERTService.release()

        verify(mockModel).release()
    }

    @Test
    fun testIsAvailable_UsesMNNModelIsLoaded() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)

        tinyBERTService.initialize()

        assertTrue("服务应该是可用的", tinyBERTService.isAvailable)
        verify(mockModel).isLoaded()
    }

    @Test
    fun testModelNotDownloaded_ReturnsFalse() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(false)

        val result = tinyBERTService.initialize()

        assertFalse("模型未下载时初始化应该失败", result)
        verify(mockMNNIntegration, never()).loadModel(any())
    }

    @Test
    fun testModelLoadFailed_ReturnsFalse() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(null)

        val result = tinyBERTService.initialize()

        assertFalse("模型加载失败时初始化应该失败", result)
    }

    @Test
    fun testSendMessageWhenNotAvailable_ReturnsErrorResponse() = runTest {
        val message = MessageEntity(
            topicId = "test_topic",
            content = "测试问题",
            senderType = "USER",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )

        val response = tinyBERTService.sendMessage(message, "测试上下文")

        assertNotNull("响应不应该为空", response)
        assertTrue("响应内容应该包含'不可用'", 
            response.content.contains("不可用"))
    }

    @Test
    fun testCheckStatusWhenModelNotLoaded_ReturnsErrorStatus() = runTest {
        val status = tinyBERTService.checkStatus()

        assertNotNull("状态不应该为空", status)
        assertEquals("状态码应该是404", 404, status.code)
        assertFalse("状态不应该是OK", status.isOk)
    }

    @Test
    fun testCheckStatusWhenInferenceFails_ReturnsErrorStatus() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)
        `when`(mockModel.inference(any())).thenThrow(RuntimeException("推理失败"))

        tinyBERTService.initialize()

        val status = tinyBERTService.checkStatus()

        assertNotNull("状态不应该为空", status)
        assertEquals("状态码应该是500", 500, status.code)
        assertFalse("状态不应该是OK", status.isOk)
        assertTrue("状态描述应该包含错误信息", 
            status.description.contains("服务错误"))
    }

    @Test
    fun testCheckStatusWhenResponseIsEmpty_ReturnsErrorStatus() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)
        `when`(mockModel.inference(any())).thenReturn("")

        tinyBERTService.initialize()

        val status = tinyBERTService.checkStatus()

        assertNotNull("状态不应该为空", status)
        assertEquals("状态码应该是500", 500, status.code)
        assertFalse("状态不应该是OK", status.isOk)
    }

    @Test
    fun testRemainingQuota_WhenAvailable_ReturnsMaxValue() = runTest {
        `when`(mockMNNIntegration.isModelDownloaded(any())).thenReturn(true)
        `when`(mockMNNIntegration.loadModel(any())).thenReturn(mockModel)
        `when`(mockModel.load()).thenReturn(true)
        `when`(mockModel.isLoaded()).thenReturn(true)

        tinyBERTService.initialize()

        assertEquals("本地模型剩余配额应该是Long.MAX_VALUE", 
            Long.MAX_VALUE, tinyBERTService.remainingQuota)
    }

    @Test
    fun testRemainingQuota_WhenNotAvailable_ReturnsZero() = runTest {
        assertEquals("服务不可用时剩余配额应该是0", 
            0L, tinyBERTService.remainingQuota)
    }
}
