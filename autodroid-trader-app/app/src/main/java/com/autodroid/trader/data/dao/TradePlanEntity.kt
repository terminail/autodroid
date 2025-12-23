package com.autodroid.trader.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 交易计划信息实体类
 * 用于Room数据库持久化存储交易计划信息
 */
@Entity(tableName = "trade_plans")
data class TradePlanEntity(
    // 交易计划基本信息
    @PrimaryKey
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val status: String? = null,
    
    // 交易计划配置信息
    val config: Map<String, String> = emptyMap(),
    val parameters: Map<String, Any> = emptyMap(),
    
    // 手机端管理用信息
    // 是否激活
    val isActive: Boolean = false,
    val lastExecutedTime: Long = 0,
    val executionCount: Int = 0,
    val executionStatus: String? = null, // 执行状态：SUCCESS, FAILED, RUNNING
    val lastExecutionResult: String? = null, // 最后执行结果
    
    // 来源信息
    val sourceServerIp: String? = null,
    val sourceServerPort: Int? = null,
    
    // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取显示名称（优先使用title，其次使用name）
     */
    fun getDisplayName(): String = title ?: name ?: id
    
    /**
     * 获取显示描述（优先使用subtitle，其次使用description）
     */
    fun getDisplayDescription(): String = subtitle ?: description ?: ""
    
    companion object {
        /**
         * 从TradePlan模型创建TradePlanEntity
         */
        fun fromTradePlan(tradePlan: com.autodroid.trader.model.TradePlan, 
                          sourceServerIp: String? = null, 
                          sourceServerPort: Int? = null): TradePlanEntity {
            return TradePlanEntity(
                id = tradePlan.id ?: java.util.UUID.randomUUID().toString(),
                name = tradePlan.name,
                title = tradePlan.title,
                subtitle = tradePlan.subtitle,
                description = tradePlan.description,
                status = tradePlan.status,
                sourceServerIp = sourceServerIp,
                sourceServerPort = sourceServerPort
            )
        }
        
        /**
         * 创建一个详细的交易计划实体
         */
        fun detailed(
            id: String,
            name: String? = null,
            title: String? = null,
            subtitle: String? = null,
            description: String? = null,
            status: String? = null,
            config: Map<String, String> = emptyMap(),
            parameters: Map<String, Any> = emptyMap(),
            isActive: Boolean = false,
            lastExecutedTime: Long = 0,
            executionCount: Int = 0,
            executionStatus: String? = null,
            lastExecutionResult: String? = null,
            sourceServerIp: String? = null,
            sourceServerPort: Int? = null
        ): TradePlanEntity {
            return TradePlanEntity(
                id = id,
                name = name,
                title = title,
                subtitle = subtitle,
                description = description,
                status = status,
                config = config,
                parameters = parameters,
                isActive = isActive,
                lastExecutedTime = lastExecutedTime,
                executionCount = executionCount,
                executionStatus = executionStatus,
                lastExecutionResult = lastExecutionResult,
                sourceServerIp = sourceServerIp,
                sourceServerPort = sourceServerPort
            )
        }
    }
}