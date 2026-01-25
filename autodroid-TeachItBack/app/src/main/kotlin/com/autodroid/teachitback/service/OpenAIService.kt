package com.autodroid.teachitback.service

import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.PromptTemplates
import com.autodroid.teachitback.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

/**
 * OpenAI AI服务实现
 * 支持基础对话和文件处理功能
 * 继承BaseAIServiceImpl，使用统一的PromptTemplates
 */
class OpenAIService(
    private val openAIConfig: AIServiceConfig.OpenAIConfig
) : com.autodroid.teachitback.api.AIService {
    override val config: com.autodroid.teachitback.config.AIServiceConfig = openAIConfig
    override var isAvailable: Boolean = true
    override var remainingQuota: Long = (config as com.autodroid.teachitback.config.AIServiceConfig.OpenAIConfig).freeQuota

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse =
        executeWithRetry("sendMessage") {
            withContext(Dispatchers.IO) {
                if (!validateConfig()) {
                    throw ConfigurationException(AIServiceError.configurationError("OpenAI配置无效"))
                }
                
                val requestBody = createChatRequestBody(message, context)
                val request = Request.Builder()
                    .url("${config.baseUrl}/chat/completions")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMessage = "OpenAI API错误: ${response.code} ${response.message}"
                    val errorType = when (response.code) {
                        401 -> AIServiceErrorType.API_AUTHENTICATION_ERROR
                        429 -> AIServiceErrorType.API_RATE_LIMIT_EXCEEDED
                        400 -> AIServiceErrorType.API_INVALID_REQUEST
                        500 -> AIServiceErrorType.API_SERVER_ERROR
                        else -> AIServiceErrorType.API_SERVER_ERROR
                    }
                    throw APIException(AIServiceError(errorType, errorMessage, "OPENAI_${response.code}"))
                }

                val content = parseResponse(response)
                remainingQuota--
                AIServiceResponse(content, AIProcessInfo("openai", "OpenAI", config.model, System.currentTimeMillis()))
            }
        }

    override suspend fun processFileContent(file: FileEntity, context: String): AIServiceResponse =
        executeWithRetry("processFileContent") {
            withContext(Dispatchers.IO) {
                if (!validateConfig()) {
                    throw ConfigurationException(AIServiceError.configurationError("OpenAI配置无效"))
                }
                
                if (file.content.length > 10000) {
                    throw ContentProcessingException(AIServiceError.contentProcessingError("文件内容过长，请精简内容"))
                }
                
                val requestBody = createProcessFileRequestBody(file.content, context)
                val request = Request.Builder()
                    .url("${config.baseUrl}/chat/completions")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMessage = "OpenAI API错误: ${response.code} ${response.message}"
                    val errorType = when (response.code) {
                        401 -> AIServiceErrorType.API_AUTHENTICATION_ERROR
                        429 -> AIServiceErrorType.API_RATE_LIMIT_EXCEEDED
                        400 -> AIServiceErrorType.API_INVALID_REQUEST
                        500 -> AIServiceErrorType.API_SERVER_ERROR
                        else -> AIServiceErrorType.API_SERVER_ERROR
                    }
                    throw APIException(AIServiceError(errorType, errorMessage, "OPENAI_${response.code}"))
                }

                val parsedContent = parseResponse(response)
                remainingQuota--
                AIServiceResponse(parsedContent, AIProcessInfo("openai", "OpenAI", config.model, System.currentTimeMillis()))
            }
        }

    private fun createChatRequestBody(message: MessageEntity, context: String): okhttp3.RequestBody {
        val json = JSONObject().apply {
            put("model", config.model)

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", PromptTemplates.basicChatSystemPrompt(context))
            })

            messagesArray.put(JSONObject().apply {
                put("role", if (message.senderType == "USER") "user" else "assistant")
                put("content", message.content)
            })

            put("messages", messagesArray)
            put("max_tokens", 500)
            put("temperature", 0.7)
        }

        return json.toString().toRequestBody(jsonMediaType)
    }

    private fun createProcessFileRequestBody(content: String, context: String): okhttp3.RequestBody {
        val json = JSONObject().apply {
            put("model", config.model)

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", PromptTemplates.fileProcessingSystemPrompt(context))
            })
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", "请分析以下文档内容：\n\n$content")
            })

            put("messages", messagesArray)
            put("max_tokens", 1000)
            put("temperature", 0.5)
        }

        return json.toString().toRequestBody(jsonMediaType)
    }

    private fun parseResponse(response: okhttp3.Response): String {
        val responseBody = response.body?.string() ?: return "Empty response"

        return try {
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                choice.getJSONObject("message").getString("content")
            } else {
                "No response generated"
            }
        } catch (e: Exception) {
            "Error parsing response: ${e.message}"
        }
    }

    // ===== 状态检测和管理 =====

    override suspend fun checkStatus(): ServiceStatus {
        return try {
            executeWithRetry("checkStatus") {
                withContext(Dispatchers.IO) {
                    // 验证配置
                    if (!validateConfig()) {
                        return@withContext ServiceStatus.CONFIGURATION_ERROR
                    }
                    
                    val testRequest = """
                        {"model": "${config.model}", "messages": [{"role": "user", "content": "test"}], "max_tokens": 5}
                    """.trimIndent()

                    val request = Request.Builder()
                        .url("${config.baseUrl}/chat/completions")
                        .addHeader("Authorization", "Bearer ${config.apiKey}")
                        .addHeader("Content-Type", "application/json")
                        .post(testRequest.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        ServiceStatus.AVAILABLE
                    } else {
                        when (response.code) {
                            401 -> ServiceStatus.UNAUTHORIZED
                            429 -> ServiceStatus.RATE_LIMITED
                            400 -> ServiceStatus.CONFIGURATION_ERROR
                            else -> ServiceStatus.UNAVAILABLE
                        }
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is ConfigurationException -> ServiceStatus.CONFIGURATION_ERROR
                is NetworkException -> ServiceStatus.NETWORK_ERROR
                is APIException -> {
                    when (e.error.type) {
                        AIServiceErrorType.API_RATE_LIMIT_EXCEEDED -> ServiceStatus.RATE_LIMITED
                        AIServiceErrorType.API_AUTHENTICATION_ERROR -> ServiceStatus.UNAUTHORIZED
                        else -> ServiceStatus.UNAVAILABLE
                    }
                }
                else -> ServiceStatus.ERROR
            }
        }
    }

    override suspend fun getUsageStatistics(): UsageStatistics {
        val totalCalls = config.freeQuota - remainingQuota
        val reliability = if (totalCalls > 0) {
            successfulCalls.toDouble() / totalCalls.toDouble()
        } else {
            1.0
        }
        
        return UsageStatistics(
            totalCalls = totalCalls,
            successfulCalls = totalCalls,
            failedCalls = 0,
            totalTokensUsed = 0,
            totalCost = 0.0,
            averageResponseTime = 0,
            reliability = reliability,
            lastCallTime = System.currentTimeMillis()
        )
    }

    override suspend fun testConnection(): Boolean {
        return try {
            checkStatus() == ServiceStatus.AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        return executeWithRetry("generateMindMap") {
            withContext(Dispatchers.IO) {
                // 验证配置
                if (!validateConfig()) {
                    throw ConfigurationException(AIServiceError.configurationError("OpenAI配置无效"))
                }
                
                val prompt = PromptTemplates.generateMindMap(topicId, learningGoal)
                
                val requestBody = JSONObject().apply {
                    put("model", config.model)
                    val messagesArray = JSONArray()
                    messagesArray.put(JSONObject().apply {
                        put("role", "system")
                        put("content", "你是一个专业的教育助手，擅长创建思维导图。")
                    })
                    messagesArray.put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                    put("messages", messagesArray)
                    put("max_tokens", 1000)
                    put("temperature", 0.5)
                }.toString().toRequestBody(jsonMediaType)
                
                val request = Request.Builder()
                    .url("${config.baseUrl}/chat/completions")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMessage = "OpenAI API错误: ${response.code} ${response.message}"
                    val errorType = when (response.code) {
                        401 -> AIServiceErrorType.API_AUTHENTICATION_ERROR
                        429 -> AIServiceErrorType.API_RATE_LIMIT_EXCEEDED
                        400 -> AIServiceErrorType.API_INVALID_REQUEST
                        500 -> AIServiceErrorType.API_SERVER_ERROR
                        else -> AIServiceErrorType.API_SERVER_ERROR
                    }
                    throw APIException(AIServiceError(errorType, errorMessage, "OPENAI_${response.code}"))
                }

                val content = parseResponse(response)
                remainingQuota-- // 减少配额
                
                MindMapEntity(
                    id = topicId,
                    topicId = topicId,
                    title = learningGoal,
                    structure = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    aiProcessInfoJson = AIProcessInfo("openai", "OpenAI", config.model, System.currentTimeMillis()).let { 
                        com.google.gson.Gson().toJson(it) 
                    }
                )
            }
        }
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

    override suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis {
        throw UnsupportedFeatureException.documentParsing(config.displayName)
    }

    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        throw UnsupportedFeatureException.conceptExtraction(config.displayName)
    }

    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        throw UnsupportedFeatureException.knowledgeGraph(config.displayName)
    }

    override suspend fun updateConfig(newConfig: AIServiceConfig) {
        throw UnsupportedFeatureException.configurationUpdate(config.displayName)
    }

    private fun validateConfig(): Boolean {
        return config.apiKey.isNotBlank() && config.baseUrl.isNotBlank()
    }

    private suspend fun <T> executeWithRetry(
        operation: String,
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay *= 2 // 指数退避
                }
            }
        }

        throw lastException ?: RuntimeException("Operation $operation failed after $maxRetries retries")
    }
}

// ===== 缺失的异常类定义 =====

class ConfigurationException(val error: AIServiceError) : Exception(error.message)

class APIException(val error: AIServiceError) : Exception(error.message)

class NetworkException(val error: AIServiceError) : Exception(error.message)

class ContentProcessingException(val error: AIServiceError) : Exception(error.message)