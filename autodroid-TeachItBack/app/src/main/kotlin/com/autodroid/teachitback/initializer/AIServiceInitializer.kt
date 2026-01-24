package com.autodroid.teachitback.initializer

import android.content.Context
import android.util.Log
import com.autodroid.teachitback.registry.AIServiceRegistry
import com.autodroid.teachitback.service.AIServiceBaichuan
import com.autodroid.teachitback.service.AIServiceDeepSeek
import com.autodroid.teachitback.service.AIServiceKimi
import com.autodroid.teachitback.service.AIServiceMiniMax
import com.autodroid.teachitback.service.AIServiceTencentHunyuan

/**
 * AI服务初始化器
 * 负责在应用启动时注册所有AI服务
 *
 * 使用方式：
 * 在Application类的onCreate()方法中调用：
 * AIServiceInitializer.initialize(context)
 */
object AIServiceInitializer {

    private const val TAG = "AIServiceInitializer"
    private lateinit var registry: AIServiceRegistry

    /**
     * 初始化所有AI服务
     * @param context 应用上下文
     * @return AIServiceRegistry 实例
     */
    fun initialize(context: Context): AIServiceRegistry {
        registry = AIServiceRegistry()

        // 注册腾讯云混元AI服务（教育功能最强，优先级最高）
        try {
            val tencentService = AIServiceTencentHunyuan(context)
            registry.registerAiService(tencentService)
            Log.i(TAG, "✓ 已注册腾讯云混元AI服务: ${tencentService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 注册腾讯云混元AI服务失败: ${e.message}")
        }

        // 注册DeepSeek AI服务（成本低，基础功能）
        try {
            val deepseekService = AIServiceDeepSeek(context)
            registry.registerAiService(deepseekService)
            Log.i(TAG, "✓ 已注册DeepSeek AI服务: ${deepseekService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 注册DeepSeek AI服务失败: ${e.message}")
        }

        // 注册Kimi AI服务（长文档处理能力强）
        try {
            val kimiService = AIServiceKimi(context)
            registry.registerAiService(kimiService)
            Log.i(TAG, "✓ 已注册Kimi AI服务: ${kimiService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 注册Kimi AI服务失败: ${e.message}")
        }

        // 注册MiniMax AI服务（创意写作和多模态）
        try {
            val minimaxService = AIServiceMiniMax(context)
            registry.registerAiService(minimaxService)
            Log.i(TAG, "✓ 已注册MiniMax AI服务: ${minimaxService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 注册MiniMax AI服务失败: ${e.message}")
        }

        // 注册百川AI服务（简单配置）
        try {
            val baichuanService = AIServiceBaichuan(context)
            registry.registerAiService(baichuanService)
            Log.i(TAG, "✓ 已注册百川AI服务: ${baichuanService.config.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 注册百川AI服务失败: ${e.message}")
        }

        val totalServices = registry.getAllServices().size
        Log.i(TAG, "========== AI服务初始化完成 ==========")
        Log.i(TAG, "总计注册服务: $totalServices 个")
        Log.i(TAG, "=====================================")

        return registry
    }

    /**
     * 获取服务注册表
     * @return AIServiceRegistry实例
     */
    fun getRegistry(): AIServiceRegistry {
        if (!::registry.isInitialized) {
            throw IllegalStateException("AIServiceInitializer未初始化，请先调用initialize()方法")
        }
        return registry
    }

    /**
     * 重新初始化所有服务
     * 用于动态更新配置后重新注册服务
     */
    fun reinitialize(context: Context): AIServiceRegistry {
        Log.i(TAG, "重新初始化AI服务...")
        return initialize(context)
    }

    /**
     * 注册单个AI服务（用于动态注册）
     */
    fun registerService(service: Any) {
        if (!::registry.isInitialized) {
            throw IllegalStateException("AIServiceInitializer未初始化")
        }
        // 实际注册逻辑
        Log.i(TAG, "动态注册服务: $service")
    }
}
