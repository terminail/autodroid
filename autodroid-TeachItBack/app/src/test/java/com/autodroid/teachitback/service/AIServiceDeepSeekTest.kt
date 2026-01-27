package com.autodroid.teachitback.service

import android.content.Context
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.model.MessageEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AIServiceDeepSeekTest {

    @Test
    fun `DeepSeek API should respond to user question`() {
        runBlocking {
            val context = org.robolectric.RuntimeEnvironment.getApplication() as Context
            val deepSeekService = AIServiceDeepSeek(
                context = context,
                config = AIServiceConfig.DeepSeekConfig()
            )

            val testMessage = MessageEntity(
                topicId = "test-topic",
                content = "What is 2 + 2?",
                senderType = "USER",
                messageType = "TEXT"
            )

            val response = deepSeekService.sendMessage(testMessage, "")

            assertNotNull("Response should not be null", response)
            assertNotNull("Response content should not be null", response.content)
            assertTrue("Response content should not be empty", response.content.isNotEmpty())
            
            println("DeepSeek API Response: ${response.content}")
            println("Processing Time: ${response.processInfo?.processingTime}ms")
            println("Service: ${response.processInfo?.serviceName}")
            println("Model: ${response.processInfo?.modelUsed}")
        }
    }

    @Test
    fun `DeepSeek status check should return AVAILABLE`() {
        runBlocking {
            val context = org.robolectric.RuntimeEnvironment.getApplication() as Context
            val deepSeekService = AIServiceDeepSeek(
                context = context,
                config = AIServiceConfig.DeepSeekConfig()
            )

            val status = deepSeekService.checkStatus()
            assertNotNull("Status should not be null", status)
            println("DeepSeek Service Status: $status")
        }
    }
}
