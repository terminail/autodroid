package com.autodroid.teachitback.model

/**
 * AI服务使用统计数据类
 */
data class UsageStatistics(
    /**
     * 总调用次数
     */
    val totalCalls: Long = 0,
    
    /**
     * 成功调用次数
     */
    val successfulCalls: Long = 0,
    
    /**
     * 失败调用次数
     */
    val failedCalls: Long = 0,
    
    /**
     * 使用的总token数
     */
    val totalTokensUsed: Long = 0,
    
    /**
     * 总费用（单位：元）
     */
    val totalCost: Double = 0.0,
    
    /**
     * 平均响应时间（毫秒）
     */
    val averageResponseTime: Long = 0,
    
    /**
     * 可靠性评分（0.0-1.0）
     */
    val reliability: Double = 1.0,
    
    /**
     * 最后调用时间
     */
    val lastCallTime: Long = 0
)
