package com.autodroid.teachitback.service

import android.content.Context
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.model.AIServiceResponse
import com.autodroid.teachitback.model.AIProcessInfo
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

/**
 * GLM-4.7-Flash AI服务单元测试
 * 测试API连接、聊天功能和错误处理
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AIServiceGLM47FlashTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var service: AIServiceGLM47Flash
    private lateinit var config: AIServiceConfig.GLM47FlashConfig

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockServer = MockWebServer()
        mockServer.start()
        
        // 创建测试配置，使用mock server的URL
        config = AIServiceConfig.GLM47FlashConfig(
            baseUrl = mockServer.url("/").toString(),
            apiKey = "test-api-key-123456",
            isEnabled = true
        )
        
        // 创建模拟Context
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        
        service = AIServiceGLM47Flash(context, config)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `test service initialization`() {
        assertEquals("glm-4.7-flash", service.config.id)
        assertEquals("GLM-4.7-Flash", service.config.name)
        assertEquals("智谱AI最新模型，支持端云协同，响应速度快，免费额度充足", service.config.description)
        assertTrue(service.config.isEnabled)
    }

    @Test
    fun `test successful chat response`() = runBlocking {
        // 模拟成功的API响应
        val mockResponse = """
            {
                "id": "chat-123",
                "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "你好！我是GLM-4.7-Flash，很高兴为你服务。"
                            },
                            "index": 0,
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 15,
                        "total_tokens": 25
                    },
                    "created": 1234567890
            }
        """.trimIndent()

        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = "你好",
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertFalse(response.content.contains("失败"))
        assertEquals("你好！我是GLM-4.7-Flash，很高兴为你服务。", response.content)
        assertEquals(25, response.processInfo.tokensUsed)
    }

    @Test
    fun `test chat with conversation history`() = runBlocking {
        val mockResponse = """
            {
                "id": "chat-456",
                "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "抛物线是一种二次函数图像，形状像开口向上或向下的U形曲线。"
                            },
                            "index": 0,
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 20,
                        "completion_tokens": 25,
                        "total_tokens": 45
                    },
                    "created": 1234567891
            }
        """.trimIndent()

        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = "请详细解释一下",
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertFalse(response.content.contains("失败"))
        assertTrue(response.content.contains("抛物线"))
        assertEquals(45, response.processInfo.tokensUsed)
    }

    @Test
    fun `test API authentication error`() = runBlocking {
        val mockResponse = """
            {
                "code": 401,
                "msg": "API Key无效",
                "data": null
            }
        """.trimIndent()

        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody(mockResponse)
                .setResponseCode(401)
        )

        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = "测试消息",
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertTrue(response.content.contains("失败"))
    }

    @Test
    fun `test rate limit error`() = runBlocking {
        val mockResponse = """
            {
                "code": 429,
                "msg": "请求频率超限",
                "data": null
            }
        """.trimIndent()

        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody(mockResponse)
                .setResponseCode(429)
        )

        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = "测试消息",
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertTrue(response.content.contains("失败"))
    }

    @Test
    fun `test network timeout`() = runBlocking {
        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBodyDelay(10, TimeUnit.SECONDS) // 10秒延迟模拟超时
                .setResponseCode(200)
        )

        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = "测试消息",
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertTrue(response.content.contains("失败"))
    }

    @Test
    fun `test service status check - healthy`() = runBlocking {
        val mockResponse = """
            {
                "id": "chat-status",
                "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "服务正常"
                            }
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 1,
                        "completion_tokens": 1,
                        "total_tokens": 2
                    },
                    "created": 1234567892
            }
        """.trimIndent()

        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val status = service.checkStatus()
        
        assertEquals(200, status.code)
        assertTrue(status.description.contains("服务可用") || status.description.contains("正常") || status.description.contains("OK"))
    }

    @Test
    fun `test service status check - unhealthy`() = runBlocking {
        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setResponseCode(500)
                .setBody("内部服务器错误")
        )

        val status = service.checkStatus()
        
        assertEquals(500, status.code)
        assertTrue(status.description.contains("错误") || status.description.contains("unavailable"))
    }

    @Test
    fun `test empty message handling`() = runBlocking {
        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = "",
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertTrue(response.content.contains("失败"))
    }

    @Test
    fun `test long message handling`() = runBlocking {
        val longMessage = "这是一个很长的消息。".repeat(1000) // 约2000字符
        
        val mockResponse = """
            {
                "id": "chat-long",
                "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "收到长消息"
                            }
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 1000,
                        "completion_tokens": 5,
                        "total_tokens": 1005
                    },
                    "created": 1234567893
            }
        """.trimIndent()

        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = longMessage,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertFalse(response.content.contains("失败"))
        assertEquals("收到长消息", response.content)
    }

    @Test
    fun `test JSON parsing error`() = runBlocking {
        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody("无效的JSON响应")
                .setResponseCode(200)
        )

        val response = service.sendMessage(
            com.autodroid.teachitback.model.MessageEntity(
                topicId = "test",
                content = "测试消息",
                senderType = "USER",
                messageType = "TEXT"
            ),
            "test-context"
        )
        
        assertTrue(response.content.contains("失败"))
    }
}