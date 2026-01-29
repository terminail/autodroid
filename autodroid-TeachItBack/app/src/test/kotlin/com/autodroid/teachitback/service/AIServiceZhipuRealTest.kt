package com.autodroid.teachitback.service

import android.content.Context
import com.autodroid.teachitback.config.AIServiceConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 智谱AI服务真实API测试
 * 使用真实的API Key测试连接
 * 
 * 注意：运行此测试需要有效的API Key
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AIServiceZhipuRealTest {

    private lateinit var service: AIServiceZhipu
    private lateinit var context: Context
    
    // 请在这里填入你的真实API Key进行测试
    private val REAL_API_KEY = "c83048896c..." // 你的API Key
    
    @Before
    fun setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication()
        service = AIServiceZhipu(context)
    }
    
    @Test
    fun `test checkStatus with real API`() = runBlocking {
        // 使用真实配置更新服务
        val realConfig = AIServiceConfig.ZhipuConfig(
            apiKey = REAL_API_KEY,
            isEnabled = true
        )
        service.updateConfig(realConfig)
        
        // 检查服务状态
        val status = service.checkStatus()
        
        println("Status code: ${status.code}")
        println("Status description: ${status.description}")
        println("Status isOk: ${status.isOk}")
        
        // 验证状态
        assertTrue("API连接应该成功，但返回: ${status.description}", status.isOk)
    }
    
    @Test
    fun `test checkStatus with invalid API key`() = runBlocking {
        // 使用无效配置
        val invalidConfig = AIServiceConfig.ZhipuConfig(
            apiKey = "invalid-api-key",
            isEnabled = true
        )
        service.updateConfig(invalidConfig)
        
        val status = service.checkStatus()
        
        println("Invalid key status code: ${status.code}")
        println("Invalid key status description: ${status.description}")
        
        // 应该返回错误状态
        assertFalse("无效API Key应该返回错误", status.isOk)
    }
}
