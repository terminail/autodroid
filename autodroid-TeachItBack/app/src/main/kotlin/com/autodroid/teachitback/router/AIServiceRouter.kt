package com.autodroid.teachitback.router

import android.util.Log
import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.config.AIServiceCapability
import com.autodroid.teachitback.model.*
import com.autodroid.teachitback.registry.AIServiceRegistry

/**
 * AI服务路由异常
 */
class AIServiceException(message: String) : Exception(message)

/**
 * 余额不足异常
 */
class InsufficientBalanceException(message: String) : Exception(message)

/**
 * 频率限制异常
 */
class RateLimitException(message: String) : Exception(message)

/**
 * AI服务智能路由器
 * 根据能力和成本智能选择最合适的AI服务
 * 提供单例访问模式，便于ViewModel层使用
 */
class AIServiceRouter(
    private val serviceRegistry: AIServiceRegistry
) {
    
    companion object {
        /**
         * 单例实例 - 延迟初始化以避免循环依赖
         */
        @Volatile
        private var INSTANCE: AIServiceRouter? = null
        
        /**
         * 获取AIServiceRouter单例实例
         */
        val instance: AIServiceRouter
            get() = INSTANCE ?: throw IllegalStateException("AIServiceRouter未初始化，请先调用initialize()")
        
        /**
         * 初始化路由器
         */
        fun initialize(serviceRegistry: AIServiceRegistry) {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = AIServiceRouter(serviceRegistry)
                }
            }
        }
        
        /**
         * 配置默认AI服务（简化版，用于AppViewModel）
         */
        fun configureDefaultService(apiKey: String, model: String): Boolean {
            return try {
                val instance = INSTANCE
                if (instance != null) {
                    // 检查服务是否已注册
                    val service = instance.serviceRegistry.getService(model)
                    if (service != null) {
                        Log.d("AIServiceRouter", "默认服务配置成功: $model")
                        true
                    } else {
                        Log.w("AIServiceRouter", "服务未注册: $model")
                        false
                    }
                } else {
                    Log.w("AIServiceRouter", "路由器未初始化")
                    false
                }
            } catch (e: Exception) {
                Log.e("AIServiceRouter", "默认服务配置失败: ${e.message}")
                false
            }
        }
        
        /**
         * 获取当前AI服务状态（简化版，用于AppViewModel）
         */
        fun getCurrentServiceStatus(): String {
            return try {
                val instance = INSTANCE
                if (instance != null) {
                    val services = instance.serviceRegistry.getAllServices()
                    if (services.isNotEmpty()) {
                        val availableServices = services.filter { service -> service.isAvailable }
                        "可用服务: ${availableServices.size}/${services.size}"
                    } else {
                        "未配置服务"
                    }
                } else {
                    "路由器未初始化"
                }
            } catch (e: Exception) {
                "状态检查失败: ${e.message}"
            }
        }
    }
    
    /**
     * 根据能力检查路由到合适的服务
     * @param capabilityCheck 能力检查函数
     * @param operation 执行的操作
     * @return 操作结果
     */
    suspend fun <T> routeByCapability(
        capabilityCheck: (AIServiceCapability) -> Boolean,
        operation: suspend (AIService) -> T
    ): T {
        // 获取所有可用服务
        val allServices = serviceRegistry.getAllServices()
        Log.d("AIServiceRouter", "routeByCapability: 总服务数=${allServices.size}")
        
        // 过滤支持所需能力的服务
        val availableServices = allServices.filter { service ->
            capabilityCheck(service.config.capabilities)
        }
        Log.d("AIServiceRouter", "routeByCapability: 支持能力的服务数=${availableServices.size}, 服务列表=${availableServices.map { it.config.id }}")
        
        if (availableServices.isEmpty()) {
            throw AIServiceException("没有可用的AI服务支持该功能")
        }
        
        // 智能路由策略（能力导向设计）：
        // 1. 优先选择有免费额度的服务
        val servicesWithQuota = availableServices.filter { service -> service.remainingQuota > 0 }
        Log.d("AIServiceRouter", "routeByCapability: 有额度的服务数=${servicesWithQuota.size}")
        if (servicesWithQuota.isNotEmpty()) {
            return tryRoute(servicesWithQuota.sortedByDescending { service -> service.remainingQuota }, operation)
        }
        
        // 2. 按成本排序选择（价格从低到高）
        val servicesByCost = availableServices.sortedBy { service -> service.config.pricePerMillion }
        return tryRoute(servicesByCost, operation)
    }

    /**
     * 根据能力检查路由到合适的服务（带服务偏好）
     * @param capabilityCheck 能力检查函数
     * @param preferredServiceId 优先使用的服务ID
     * @param operation 执行的操作
     * @return 操作结果
     */
    suspend fun <T> routeWithPreference(
        capabilityCheck: (AIServiceCapability) -> Boolean,
        preferredServiceId: String,
        operation: suspend (AIService) -> T
    ): T {
        // 获取所有可用服务
        val allServices = serviceRegistry.getAllServices()
        
        // 过滤支持所需能力的服务
        val availableServices = allServices.filter { service ->
            capabilityCheck(service.config.capabilities)
        }
        
        if (availableServices.isEmpty()) {
            throw AIServiceException("没有可用的AI服务支持该功能")
        }
        
        // 1. 首先尝试使用推荐的服务
        val preferredService = availableServices.find { it.config.id == preferredServiceId }
        if (preferredService != null) {
            try {
                val status = preferredService.checkStatus()
                if (status.isOk) {
                    return operation(preferredService)
                } else {
                    Log.d("AIServiceRouter", "推荐服务 ${preferredService.config.displayName} 不可用，状态: $status")
                }
            } catch (e: Exception) {
                Log.e("AIServiceRouter", "推荐服务 ${preferredService.config.displayName} 调用失败: ${e.message}", e)
            }
        } else {
            Log.d("AIServiceRouter", "推荐服务 $preferredServiceId 不支持所需能力，回退到智能路由")
        }
        
        // 2. 如果推荐服务不可用或不支持，回退到普通的能力路由
        Log.d("AIServiceRouter", "回退到普通路由策略")
        return routeByCapability(capabilityCheck, operation)
    }
    
    /**
     * 尝试路由到服务列表中的服务，直到成功
     */
    private suspend fun <T> tryRoute(
        services: List<AIService>,
        operation: suspend (AIService) -> T
    ): T {
        val exceptions = mutableListOf<Exception>()
        
        for (service in services) {
            try {
                Log.d("AIServiceRouter", "tryRoute: 尝试服务 ${service.config.displayName}, isAvailable=${service.isAvailable}")
                // 检查服务状态
                val status = service.checkStatus()
                Log.d("AIServiceRouter", "tryRoute: 服务 ${service.config.displayName} 状态: $status")
                if (status.isOk) {
                    Log.d("AIServiceRouter", "tryRoute: 选择服务 ${service.config.displayName} 进行操作")
                    return operation(service)
                } else {
                    exceptions.add(Exception("${service.config.displayName} 不可用: ${status.description}"))
                }
            } catch (e: Exception) {
                Log.e("AIServiceRouter", "tryRoute: 服务 ${service.config.displayName} 调用失败", e)
                exceptions.add(e)
            }
        }
        
        throw AIServiceException("所有可用服务都失败：${exceptions.joinToString { exception -> exception.message ?: "未知错误" }}")
    }
    
    // ===== 便捷路由方法 =====
    
    /**
     * 路由基础对话请求
     */
    suspend fun routeBasicChat(operation: suspend (AIService) -> String): String {
        return routeByCapability(
            capabilityCheck = { capability -> capability.supportBasicChat },
            operation = operation
        )
    }
    
    /**
     * 路由思维导图生成请求
     */
    suspend fun routeMindMapGeneration(operation: suspend (AIService) -> MindMapEntity?): MindMapEntity? {
        return routeByCapability(
            capabilityCheck = { capability -> capability.supportMindMapGeneration },
            operation = operation
        )
    }
    
    /**
     * 路由文档解析请求
     */
    suspend fun routeDocumentParsing(operation: suspend (AIService) -> DocumentAnalysis): DocumentAnalysis {
        return routeByCapability(
            capabilityCheck = { capability -> capability.supportDocumentParsing },
            operation = operation
        )
    }
    
    /**
     * 路由学习分析请求
     */
    suspend fun routeLearningAnalysis(operation: suspend (AIService) -> ProgressAnalysis): ProgressAnalysis {
        return routeByCapability(
            capabilityCheck = { capability -> capability.supportLearningAnalysis },
            operation = operation
        )
    }
    
    /**
     * 路由苏格拉底提问请求
     */
    suspend fun routeSocraticQuestioning(operation: suspend (AIService) -> List<String>): List<String> {
        return routeByCapability(
            capabilityCheck = { capability -> capability.supportSocraticQuestioning },
            operation = operation
        )
    }
    
    /**
     * 路由代码生成请求
     */
    suspend fun routeCodeGeneration(operation: suspend (AIService) -> String): String {
        return routeByCapability(
            capabilityCheck = { capability -> capability.supportCodeGeneration },
            operation = operation
        )
    }
}
