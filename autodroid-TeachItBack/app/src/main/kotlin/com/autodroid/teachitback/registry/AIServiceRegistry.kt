package com.autodroid.teachitback.registry

import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.config.AIServiceCapability

/**
 * AI服务注册中心
 * 管理所有AI服务的注册、查询和发现
 */
class AIServiceRegistry {
    
    private val services = mutableMapOf<String, AIService>()
    
    /**
     * 注册AI服务
     * @param service AI服务实例
     */
    fun registerAiService(service: AIService) {
        services[service.config.id] = service
    }
    
    /**
     * 根据能力检查获取可用服务
     * @param capabilityCheck 能力检查函数
     * @return 可用服务列表
     */
    fun getAvailableServices(capabilityCheck: (AIServiceCapability) -> Boolean): List<AIService> {
        return services.values.filter { service ->
            service.isAvailable && capabilityCheck(service.config.capabilities)
        }
    }
    
    /**
     * 根据模型ID获取服务
     * @param modelId 模型ID
     * @return AI服务实例或null
     */
    fun getService(modelId: String): AIService? {
        return services[modelId]
    }
    
    /**
     * 获取所有服务
     * @return 所有AI服务列表
     */
    fun getAllServices(): List<AIService> {
        return services.values.toList()
    }
    
    /**
     * 注销服务
     * @param modelId 模型ID
     */
    fun unregisterService(modelId: String) {
        services.remove(modelId)
    }
    
    /**
     * 获取服务数量
     * @return 注册的服务数量
     */
    fun getServiceCount(): Int = services.size
    
    /**
     * 清空所有服务
     */
    fun clearAllServices() {
        services.clear()
    }
}
