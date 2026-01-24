package com.autodroid.teachitback.service

import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.PromptTemplates
import com.autodroid.teachitback.model.FileEntity
import com.autodroid.teachitback.model.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 百川AI服务实现
 * 支持基础对话功能
 * 继承BaseAIServiceImpl，只重写基础对话
 */
class AIServiceBaichuan(
    private val context: android.content.Context,
    override val config: AIServiceConfig = AIServiceConfig.BaichuanConfig()
) : BaseAIServiceImpl() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override var isAvailable: Boolean = true
    override var remainingQuota: Long = config.freeQuota

    // ===== 基础对话功能 =====

    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse {
        val startTime = System.currentTimeMillis()

        try {
            val requestBody = buildChatRequest(message, context)
            val request = Request.Builder()
                .url("${config.baseUrl}/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("API请求失败: ${response.code}")
            }

            val chatResponse = gson.fromJson(responseBody, BaichuanChatResponse::class.java)
            val content = chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IOException("Empty response from Baichuan")

            val processingTime = System.currentTimeMillis() - startTime
            remainingQuota--

            return AIServiceResponse(
                content = content,
                processInfo = AIProcessInfo(
                    serviceId = config.id,
                    serviceName = config.displayName,
                    modelUsed = config.model,
                    processingTime = processingTime
                )
            )
        } catch (e: Exception) {
            throw IOException("百川API调用失败: ${e.message}", e)
        }
    }

    override suspend fun processFileContent(file: FileEntity, context: String): AIServiceResponse {
        val prompt = PromptTemplates.processFileContent(file.content, context)

        return sendMessage(
            MessageEntity(
                topicId = file.topicId,
                content = prompt,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "file-processing"
        )
    }

    // ===== 状态检测和管理 =====

    override suspend fun checkStatus(): ServiceStatus {
        return try {
            val testRequest = """
                {"model": "${config.model}", "messages": [{"role": "user", "content": "test"}], "max_tokens": 5}
            """.trimIndent()

            val request = Request.Builder()
                .url("${config.baseUrl}/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(testRequest.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                ServiceStatus.AVAILABLE
            } else when (response.code) {
                401 -> ServiceStatus.UNAUTHORIZED
                429 -> ServiceStatus.RATE_LIMITED
                else -> ServiceStatus.UNAVAILABLE
            }
        } catch (e: IOException) {
            ServiceStatus.UNAVAILABLE
        }
    }

    override suspend fun getUsageStatistics(): UsageStatistics {
        // 百川API不提供详细的使用统计接口
        return UsageStatistics(
            totalCalls = config.freeQuota - remainingQuota,
            successfulCalls = config.freeQuota - remainingQuota,
            failedCalls = 0,
            totalTokensUsed = 0,
            totalCost = 0.0,
            averageResponseTime = 0,
            lastCallTime = System.currentTimeMillis()
        )
    }

    // ===== 配置管理 =====

    override suspend fun updateConfig(newConfig: AIServiceConfig) {
        // 百川不支持运行时更新配置
    }

    override suspend fun testConnection(): Boolean {
        return try {
            checkStatus() == ServiceStatus.AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    // ===== 教育专用功能（百川不支持）=====

    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        throw UnsupportedFeatureException.mindMapGeneration(config.displayName)
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

    // ===== 知识处理功能（百川不支持）=====

    override suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis {
        throw UnsupportedFeatureException.documentParsing(config.displayName)
    }

    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        throw UnsupportedFeatureException.conceptExtraction(config.displayName)
    }

    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        throw UnsupportedFeatureException.knowledgeGraph(config.displayName)
    }

    // ===== 私有辅助方法 =====

    private fun buildChatRequest(message: MessageEntity, context: String): RequestBody {
        val systemMessage = if (context.isNotBlank()) {
            listOf(
                Message(role = "system", content = context)
            )
        } else {
            emptyList()
        }

        val chatMessages = systemMessage + listOf(
            Message(
                role = if (message.senderType == "USER") "user" else "assistant",
                content = message.content
            )
        )

        val request = BaichuanChatRequest(
            model = config.model,
            messages = chatMessages,
            maxTokens = 2000,
            temperature = 0.7
        )

        val json = gson.toJson(request)
        return json.toRequestBody("application/json".toMediaType())
    }

    // ===== 数据类 =====

    data class Message(
        val role: String,
        val content: String
    )

    data class BaichuanChatRequest(
        val model: String,
        val messages: List<Message>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double
    )

    data class BaichuanChatResponse(
        val choices: List<Choice>,
        val usage: Usage?
    )

    data class Choice(
        val message: Message,
        val index: Int,
        @SerializedName("finish_reason") val finishReason: String?
    )

    data class Usage(
        @SerializedName("prompt_tokens") val promptTokens: Int,
        @SerializedName("completion_tokens") val completionTokens: Int,
        @SerializedName("total_tokens") val totalTokens: Int
    )
}
