package com.autodroid.teachitback.model

/**
 * AI服务状态枚举
 */
enum class ServiceStatus {
    /** 服务可用 */
    AVAILABLE,
    
    /** 余额不足 */
    INSUFFICIENT_BALANCE,
    
    /** 频率限制 */
    RATE_LIMITED,
    
    /** 未授权 */
    UNAUTHORIZED,
    
    /** 服务不可用 */
    UNAVAILABLE,
    
    /** 模型不支持 */
    MODEL_NOT_SUPPORTED,
    
    /** 配额耗尽 */
    QUOTA_EXHAUSTED,
    
    /** 配置错误 */
    CONFIGURATION_ERROR,
    
    /** 网络错误 */
    NETWORK_ERROR,
    
    /** 一般错误 */
    ERROR
}
