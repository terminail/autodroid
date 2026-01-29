package com.autodroid.teachitback.initializer

import android.content.Context
import android.util.Log
import com.autodroid.teachitback.config.AIServiceCapability
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.framework.MNNIntegration
import com.autodroid.teachitback.registry.AIServiceRegistry
import com.autodroid.teachitback.service.AIServiceBaichuan
import com.autodroid.teachitback.service.AIServiceChatGLM
import com.autodroid.teachitback.service.AIServiceDeepSeek
import com.autodroid.teachitback.service.AIServiceKimi
import com.autodroid.teachitback.service.AIServiceMiniMax
import com.autodroid.teachitback.service.AIServiceTencentHunyuan
import com.autodroid.teachitback.service.AIServiceTinyBERT
import com.autodroid.teachitback.service.AIServiceZhipu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object AIServiceInitializer {
    private const val TAG = "AIServiceInitializer"
    private lateinit var registry: AIServiceRegistry

    fun registerAllServices(context: Context, registry: AIServiceRegistry) {
        this.registry = registry
        
        try {
            val tencentService = AIServiceTencentHunyuan(context)
            registry.registerAiService(tencentService)
            Log.i(TAG, "Tencent AI service registered: ${tencentService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "Tencent AI service registration failed: ${e.message}")
        }

        try {
            val deepseekService = AIServiceDeepSeek(context)
            registry.registerAiService(deepseekService)
            Log.i(TAG, "DeepSeek AI service registered: ${deepseekService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek AI service registration failed: ${e.message}")
        }

        try {
            val kimiService = AIServiceKimi(context)
            registry.registerAiService(kimiService)
            Log.i(TAG, "Kimi AI service registered: ${kimiService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "Kimi AI service registration failed: ${e.message}")
        }

        try {
            val minimaxService = AIServiceMiniMax(context)
            registry.registerAiService(minimaxService)
            Log.i(TAG, "MiniMax AI service registered: ${minimaxService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "MiniMax AI service registration failed: ${e.message}")
        }

        try {
            val baichuanService = AIServiceBaichuan(context)
            registry.registerAiService(baichuanService)
            Log.i(TAG, "Baichuan AI service registered: ${baichuanService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "Baichuan AI service registration failed: ${e.message}")
        }

        try {
            val zhipuService = AIServiceZhipu(context)
            registry.registerAiService(zhipuService)
            Log.i(TAG, "Zhipu AI service registered: ${zhipuService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "Zhipu AI service registration failed: ${e.message}")
        }

        try {
            val mnnIntegration = MNNIntegration(context)
            val mnnInitialized = runBlocking(Dispatchers.IO) {
                mnnIntegration.initialize()
            }
            if (!mnnInitialized) {
                Log.e(TAG, "MNN framework initialization failed")
                throw Exception("MNN framework initialization failed")
            }
            Log.i(TAG, "MNN framework initialized successfully")
            
            val tinybertConfig = AIServiceConfig.TinyBERTConfig(
                id = "tinybert_local",
                displayName = "TinyBERT (本地)",
                description = "轻量级BERT模型，快速答案匹配和评估",
                capabilities = AIServiceCapability.BASIC_CHAT.supportAnswerEvaluation(true)
            )
            val tinybertService = AIServiceTinyBERT(context, tinybertConfig, mnnIntegration)
            
            runBlocking(Dispatchers.IO) {
                val initSuccess = tinybertService.initialize()
                if (initSuccess) {
                    registry.registerAiService(tinybertService)
                    Log.i(TAG, "TinyBERT本地AI服务注册并初始化成功: ${tinybertService.config.displayName}")
                    
                    val knowledgeBasePaths = listOf(
                        "tibresource/knowledge/math_knowledge_base.txt",
                        "/sdcard/tibresource/knowledge/math_knowledge_base.txt",
                        "/sdcard/Android/data/com.autodroid.teachitback/files/tibresource/knowledge/math_knowledge_base.txt"
                    )
                    
                    var loadSuccess = false
                    for (path in knowledgeBasePaths) {
                        val result = tinybertService.loadKnowledgeBaseFromFile(path)
                        if (result) {
                            loadSuccess = true
                            Log.i(TAG, "TinyBERT knowledge base loaded successfully: $path")
                            break
                        }
                    }
                    
                    if (!loadSuccess) {
                        Log.w(TAG, "TinyBERT knowledge base loading failed, will use simplified similarity calculation")
                    } else {
                        // Knowledge base loaded successfully
                    }
                } else {
                    Log.e(TAG, "TinyBERT local AI service initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TinyBERT local AI service registration failed: ${e.message}")
        }

        try {
            val mnnIntegration = MNNIntegration(context)
            val mnnInitialized = runBlocking(Dispatchers.IO) {
                mnnIntegration.initialize()
            }
            if (!mnnInitialized) {
                Log.e(TAG, "MNN framework initialization failed")
                throw Exception("MNN framework initialization failed")
            }
            Log.i(TAG, "MNN framework initialized successfully")
            
            val chatglmConfig = AIServiceConfig.ChatGLMConfig(
                id = "chatglm",
                displayName = "ChatGLM (Local)",
                description = "More comprehensive local conversation model",
                capabilities = AIServiceCapability.EMPTY
                    .supportBasicChat(true)
                    .supportEducation(true)
                    .supportAnswerEvaluation(true)
                    .supportSocraticQuestioning(true)
                    .supportLearningAnalysis(true)
                    .supportConceptExtraction(true)
                    .supportMath(true)
                    .supportCreativeWriting(true)
            )
            val chatglmService = AIServiceChatGLM(context, chatglmConfig, mnnIntegration)
            
            runBlocking(Dispatchers.IO) {
                val initSuccess = chatglmService.initialize()
                if (initSuccess) {
                    registry.registerAiService(chatglmService)
                    Log.i(TAG, "ChatGLM local AI service registered and initialized: ${chatglmService.config.displayName}")
                } else {
                    Log.e(TAG, "ChatGLM local AI service initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ChatGLM local AI service registration failed: ${e.message}")
        }

        val totalServices = registry.getAllServices().size
        Log.i(TAG, "AI service initialization completed")
        Log.i(TAG, "Total registered services: $totalServices")
    }

    /**
     * 初始化所有服务的配置
     * 每个服务从数据库加载自己的配置
     * @param repository SettingsRepository 用于加载配置
     */
    suspend fun initializeAllServiceConfigs(repository: com.autodroid.teachitback.repository.SettingsRepository) {
        registry.getAllServices().forEach { service ->
            val serviceId = service.config.id
            // 从数据库加载配置
            val config = repository.loadAIServiceConfig(serviceId)
            if (config != null) {
                // 更新服务配置
                service.updateConfig(config)
                Log.d(TAG, "Service config loaded: $serviceId")
            }
            
            // 注册配置变更监听器 - 当配置在设置页面被修改时，自动更新服务
            repository.registerConfigChangeListener(serviceId) { newConfig ->
                service.updateConfig(newConfig)
                Log.d(TAG, "Service config updated via listener: $serviceId")
            }
            Log.d(TAG, "Config change listener registered: $serviceId")
        }
        Log.i(TAG, "All service configs initialized with change listeners")
    }
}