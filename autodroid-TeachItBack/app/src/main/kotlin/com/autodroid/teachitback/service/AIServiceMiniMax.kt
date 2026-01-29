package com.autodroid.teachitback.service

import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.config.PromptTemplates
import com.autodroid.teachitback.model.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * MiniMax AI服务实现
 * 支持基础对话、文件处理、创意写作和多模态
 * 继承BaseAIServiceImpl，只重写支持的功能
 */
class AIServiceMiniMax(
    private val context: android.content.Context
) : BaseAIServiceImpl() {

    override val config: AIServiceConfig = AIServiceConfig.MiniMaxConfig()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override val isAvailable: Boolean
        get() = config.isEnabled
    override var remainingQuota: Long = 0L

    // ===== 基础对话功能 =====

    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse {
        val startTime = System.currentTimeMillis()

        try {
            val requestBody = buildChatRequest(message, context)
            val request = Request.Builder()
                .url("${config.baseUrl}/v1/text/chatcompletion_v2")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("API请求失败: ${response.code}")
            }

            val chatResponse = gson.fromJson(responseBody, MiniMaxChatResponse::class.java)
            val content = chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IOException("Empty response from MiniMax")

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
            throw IOException("MiniMax API调用失败: ${e.message}", e)
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

    // ===== 创意写作功能 =====

    override suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String> {
        val prompt = PromptTemplates.generateSocraticQuestions(topic, currentLevel)

        val response = sendMessage(
            MessageEntity(
                topicId = topic,
                content = prompt,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "socratic-questioning"
        )

        return response.content.lines()
            .filter { it.isNotBlank() }
            .take(5)
    }

    // ===== 知识处理功能 =====

    override suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis {
        val prompt = PromptTemplates.parseDocument(file.content, fileType)

        val response = sendMessage(
            MessageEntity(
                topicId = file.topicId,
                content = prompt,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "document-parsing"
        )

        return DocumentAnalysis(
            fileName = file.fileName,
            fileType = fileType,
            summary = response.content.substringBefore("关键要点:").trim(),
            keyPoints = response.content
                .substringAfter("关键要点:")
                .substringBefore("提取文本:")
                .lines()
                .filter { it.startsWith("-") }
                .map { it.removePrefix("-").trim() },
            extractedText = response.content.substringAfter("提取文本:").trim()
        )
    }

    // ===== 状态检测和管理 =====

    override suspend fun checkStatus(): AIServiceStatus {
        return try {
            val testRequest = """
                {"model": "${config.model}", "messages": [{"role": "user", "content": "test"}], "max_tokens": 5}
            """.trimIndent()

            val request = Request.Builder()
                .url("${config.baseUrl}/v1/text/chatcompletion_v2")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(testRequest.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                AIServiceStatus.fromCode(200, "服务可用")
            } else when (response.code) {
                401 -> AIServiceStatus.fromCode(401, "未授权")
                429 -> AIServiceStatus.fromCode(429, "请求频率限制")
                else -> AIServiceStatus.fromCode(503, "服务不可用")
            }
        } catch (e: IOException) {
            AIServiceStatus.fromCode(500, "网络错误")
        }
    }

    override suspend fun getUsageStatistics(): UsageStatistics {
        // MiniMax API可能不提供详细的使用统计接口
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

    // ===== 私有辅助方法 =====

    private fun buildChatRequest(message: MessageEntity, context: String): RequestBody {
        val chatMessages = listOf(
            Message(
                role = if (message.senderType == "USER") "USER" else "BOT",
                content = message.content
            )
        )

        val request = MiniMaxChatRequest(
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

    data class MiniMaxChatRequest(
        val model: String,
        val messages: List<Message>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double
    )

    data class MiniMaxChatResponse(
        val choices: List<Choice>,
        val usage: Usage?
    )

    data class Choice(
        val message: Message,
        val index: Int,
        @SerializedName("finish_reason") val finishReason: String?
    )

    data class Usage(
        @SerializedName("input_tokens") val inputTokens: Int,
        @SerializedName("output_tokens") val outputTokens: Int,
        @SerializedName("total_tokens") val totalTokens: Int
    )
}
