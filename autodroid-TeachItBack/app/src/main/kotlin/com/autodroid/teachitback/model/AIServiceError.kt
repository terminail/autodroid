package com.autodroid.teachitback.model

/**
 * AI服务错误类型枚举
 * 用于标识不同类型的AI服务错误
 */
enum class AIServiceErrorType {
    // 网络相关错误
    NETWORK_ERROR,
    NETWORK_TIMEOUT,
    NETWORK_UNAVAILABLE,
    
    // API相关错误
    API_AUTHENTICATION_ERROR,
    API_RATE_LIMIT_EXCEEDED,
    API_QUOTA_EXCEEDED,
    API_INVALID_REQUEST,
    API_SERVER_ERROR,
    
    // 配置相关错误
    CONFIGURATION_ERROR,
    CONFIGURATION_MISSING,
    CONFIGURATION_INVALID,
    
    // 内容相关错误
    CONTENT_PROCESSING_ERROR,
    CONTENT_TOO_LARGE,
    CONTENT_INVALID_FORMAT,
    
    // 功能相关错误
    FEATURE_UNSUPPORTED,
    FEATURE_DISABLED,
    
    // 系统相关错误
    SYSTEM_ERROR,
    MEMORY_LIMIT_EXCEEDED,
    PROCESSING_TIMEOUT,
    
    // 未知错误
    UNKNOWN_ERROR
}

/**
 * AI服务错误信息
 * 包含错误类型、错误消息、可选的错误代码和原始异常
 */
data class AIServiceError(
    val type: AIServiceErrorType,
    val message: String,
    val errorCode: String? = null,
    val originalException: Exception? = null
) {
    companion object {
        /**
         * 创建网络错误
         */
        fun networkError(message: String, originalException: Exception? = null) =
            AIServiceError(AIServiceErrorType.NETWORK_ERROR, message, null, originalException)
        
        /**
         * 创建API限流错误
         */
        fun rateLimitError(message: String, errorCode: String? = null) =
            AIServiceError(AIServiceErrorType.API_RATE_LIMIT_EXCEEDED, message, errorCode)
        
        /**
         * 创建配置错误
         */
        fun configurationError(message: String, errorCode: String? = null) =
            AIServiceError(AIServiceErrorType.CONFIGURATION_ERROR, message, errorCode)
        
        /**
         * 创建功能不支持错误
         */
        fun unsupportedFeature(featureName: String, serviceName: String) =
            AIServiceError(
                AIServiceErrorType.FEATURE_UNSUPPORTED,
                "$serviceName 不支持功能: $featureName",
                "UNSUPPORTED_$featureName"
            )
        
        /**
         * 创建内容处理错误
         */
        fun contentProcessingError(message: String, originalException: Exception? = null) =
            AIServiceError(AIServiceErrorType.CONTENT_PROCESSING_ERROR, message, null, originalException)
        
        /**
         * 创建系统错误
         */
        fun systemError(message: String, originalException: Exception? = null) =
            AIServiceError(AIServiceErrorType.SYSTEM_ERROR, message, null, originalException)
    }
}

/**
 * AI服务异常基类
 * 所有AI服务相关的异常都继承此类
 */
open class AIServiceException(
    val error: AIServiceError,
    message: String = error.message
) : Exception(message) {
    
    constructor(errorType: AIServiceErrorType, message: String) : 
            this(AIServiceError(errorType, message))
}

/**
 * 网络错误异常
 */
class NetworkException(
    error: AIServiceError,
    message: String = error.message
) : AIServiceException(error, message)

/**
 * API错误异常
 */
class APIException(
    error: AIServiceError,
    message: String = error.message
) : AIServiceException(error, message)

/**
 * 配置错误异常
 */
class ConfigurationException(
    error: AIServiceError,
    message: String = error.message
) : AIServiceException(error, message)

/**
 * 内容处理错误异常
 */
class ContentProcessingException(
    error: AIServiceError,
    message: String = error.message
) : AIServiceException(error, message)

/**
 * 重试失败异常
 * 当重试次数耗尽时抛出
 */
class RetryFailedException(
    val retryCount: Int,
    val lastException: Exception,
    message: String = "重试 $retryCount 次后仍然失败: ${lastException.message}"
) : AIServiceException(AIServiceError.systemError(message), message)