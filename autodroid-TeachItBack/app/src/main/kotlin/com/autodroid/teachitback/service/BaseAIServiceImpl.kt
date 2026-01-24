package com.autodroid.teachitback.service

import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.model.*
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * AI服务基础实现类
 * 提供完善的错误处理、重试机制和异常包装功能
 * 所有不支持的方法默认抛出适当的异常
 * 子类只需重写支持的功能，并可以调用安全执行方法
 */
abstract class BaseAIServiceImpl : AIService {
    
    // 错误处理相关属性
    protected val errorCounter = AtomicInteger(0)
    protected val lastErrorTime = AtomicInteger(0)
    protected val retryConfig = RetryConfig(
        maxRetries = 3,
        initialDelay = 1.seconds,
        maxDelay = 10.seconds,
        backoffFactor = 2.0
    )

    /**
     * 重试配置类
     */
    data class RetryConfig(
        val maxRetries: Int = 3,
        val initialDelay: Duration = 1.seconds,
        val maxDelay: Duration = 10.seconds,
        val backoffFactor: Double = 2.0,
        val retryableErrors: Set<AIServiceErrorType> = setOf(
            AIServiceErrorType.NETWORK_ERROR,
            AIServiceErrorType.NETWORK_TIMEOUT,
            AIServiceErrorType.API_RATE_LIMIT_EXCEEDED,
            AIServiceErrorType.API_SERVER_ERROR
        )
    )

    // ===== 安全执行方法 =====

    /**
     * 安全执行API调用，包含重试和错误处理
     */
    protected suspend fun <T> executeWithRetry(
        operation: String,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var currentDelay = retryConfig.initialDelay
        
        for (attempt in 0..retryConfig.maxRetries) {
            try {
                val result = block()
                // 成功时重置错误计数器
                errorCounter.set(0)
                return result
            } catch (e: Exception) {
                lastException = e
                
                // 检查是否需要重试
                val shouldRetry = when (e) {
                    is AIServiceException -> retryConfig.retryableErrors.contains(e.error.type)
                    is UnsupportedFeatureException -> false // 不支持的功能不重试
                    else -> true // 其他异常默认重试
                }
                
                if (attempt == retryConfig.maxRetries || !shouldRetry) {
                    break
                }
                
                // 记录错误
                errorCounter.incrementAndGet()
                lastErrorTime.set(System.currentTimeMillis().toInt())
                
                // 延迟重试
                delay(currentDelay)
                currentDelay = (currentDelay * retryConfig.backoffFactor).coerceAtMost(retryConfig.maxDelay)
            }
        }
        
        throw RetryFailedException(
            retryCount = retryConfig.maxRetries,
            lastException = lastException ?: Exception("未知错误")
        )
    }

    /**
     * 包装异常为AIServiceException
     */
    protected fun wrapException(operation: String, exception: Exception): AIServiceException {
        return when (exception) {
            is AIServiceException -> exception
            is java.net.UnknownHostException -> 
                AIServiceException(AIServiceError.networkError("网络不可用: ${exception.message}", exception))
            is java.net.SocketTimeoutException -> 
                AIServiceException(AIServiceError.networkError("请求超时: ${exception.message}", exception))
            is java.io.IOException -> 
                AIServiceException(AIServiceError.networkError("网络错误: ${exception.message}", exception))
            is UnsupportedFeatureException -> 
                AIServiceException(AIServiceError.unsupportedFeature(exception.message ?: operation, config.displayName))
            else -> 
                AIServiceException(AIServiceError.systemError("$operation 失败: ${exception.message}", exception))
        }
    }

    /**
     * 检查配置是否有效
     */
    protected fun validateConfig(): Boolean {
        return when (config) {
            is com.autodroid.teachitback.config.AIServiceConfig.OpenAIConfig -> {
                config.apiKey.isNotBlank() && config.model.isNotBlank()
            }
            is com.autodroid.teachitback.config.AIServiceConfig.DeepSeekConfig -> {
                config.apiKey.isNotBlank()
            }
            is com.autodroid.teachitback.config.AIServiceConfig.TencentHunyuanConfig -> {
                config.secretId.isNotBlank() && config.apiKey.isNotBlank()
            }
            is com.autodroid.teachitback.config.AIServiceConfig.MiniMaxConfig -> {
                config.apiKey.isNotBlank()
            }
            is com.autodroid.teachitback.config.AIServiceConfig.BaichuanConfig -> {
                config.apiKey.isNotBlank()
            }
            else -> true
        }
    }

    // ===== 基础对话功能（默认不支持）=====

    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse {
        throw UnsupportedFeatureException("基础对话", config.displayName)
    }

    override suspend fun processFileContent(file: FileEntity, context: String): AIServiceResponse {
        throw UnsupportedFeatureException("文件处理", config.displayName)
    }

    // ===== 教育专用功能（默认不支持）=====

    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        throw UnsupportedFeatureException("思维导图生成", config.displayName)
    }

    override suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis {
        throw UnsupportedFeatureException.learningAnalysis(config.displayName)
    }

    override suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String> {
        throw UnsupportedFeatureException.socraticQuestioning(config.displayName)
    }

    override suspend fun evaluateAnswer(userAnswer: MessageEntity, correctAnswer: String): AnswerEvaluation {
        throw UnsupportedFeatureException.answerEvaluation(config.displayName)
    }

    // ===== 知识处理功能（默认不支持）=====

    override suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis {
        throw UnsupportedFeatureException.documentParsing(config.displayName)
    }

    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        throw UnsupportedFeatureException.conceptExtraction(config.displayName)
    }

    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        throw UnsupportedFeatureException.knowledgeGraph(config.displayName)
    }

    // ===== 状态检测和管理（默认实现）=====

    override suspend fun checkStatus(): ServiceStatus {
        val errorCount = errorCounter.get()
        return when {
            errorCount > 10 -> ServiceStatus.ERROR
            errorCount > 5 -> ServiceStatus.NETWORK_ERROR
            !validateConfig() -> ServiceStatus.CONFIGURATION_ERROR
            else -> ServiceStatus.UNAVAILABLE
        }
    }

    override suspend fun getUsageStatistics(): UsageStatistics {
        return UsageStatistics(
            totalCalls = 0L,
            successfulCalls = 0L,
            failedCalls = errorCounter.get().toLong(),
            totalTokensUsed = 0L,
            totalCost = 0.0,
            averageResponseTime = 0L,
            lastCallTime = lastErrorTime.get().toLong()
        )
    }

    // ===== 配置管理（默认不支持）=====

    override suspend fun updateConfig(newConfig: com.autodroid.teachitback.config.AIServiceConfig) {
        throw UnsupportedFeatureException("配置更新", config.displayName)
    }

    override suspend fun testConnection(): Boolean {
        return validateConfig()
    }
}
