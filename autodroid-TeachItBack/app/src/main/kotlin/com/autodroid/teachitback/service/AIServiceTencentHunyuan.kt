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
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * 腾讯云混元AI服务实现
 * 支持全功能，特别是教育专用能力（思维导图、学习分析、苏格拉底提问等）
 * 继承BaseAIServiceImpl，重写所有教育相关功能
 */
class AIServiceTencentHunyuan(
    private val context: android.content.Context
) : BaseAIServiceImpl() {

    override val config: AIServiceConfig = AIServiceConfig.TencentHunyuanConfig()

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
            val request = createSignedRequest(
                url = "${config.baseUrl}/v1/chat/completions",
                body = requestBody
            )

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("API请求失败: ${response.code}")
            }

            val chatResponse = gson.fromJson(responseBody, HunyuanChatResponse::class.java)
            val content = chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IOException("Empty response from Hunyuan")

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
            throw IOException("腾讯云混元API调用失败: ${e.message}", e)
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
            context
        )
    }

    // ===== 教育专用功能（全功能支持） =====

    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        if (!config.capabilities.supportMindMapGeneration) {
            throw UnsupportedFeatureException.mindMapGeneration(config.displayName)
        }

        val prompt = PromptTemplates.generateMindMap(topicId, learningGoal)

        val response = sendMessage(
            MessageEntity(
                topicId = topicId,
                content = prompt,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "mindmap-generation"
        )

        try {
            val mindMapResponse = gson.fromJson(response.content, HunyuanMindMapResponse::class.java)

            return MindMapEntity(
                id = topicId,
                topicId = topicId,
                title = learningGoal,
                structure = mindMapResponse.structure,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                aiProcessInfoJson = response.processInfo?.let { gson.toJson(it) }
            )
        } catch (e: Exception) {
            return MindMapEntity(
                id = topicId,
                topicId = topicId,
                title = learningGoal,
                structure = response.content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                aiProcessInfoJson = response.processInfo?.let { gson.toJson(it) }
            )
        }
    }

    override suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis {
        if (!config.capabilities.supportLearningAnalysis) {
            throw UnsupportedFeatureException.learningAnalysis(config.displayName)
        }

        val conversationText = PromptTemplates.buildConversationText(conversationHistory)
        val prompt = PromptTemplates.analyzeLearningProgress(conversationText)

        try {
            val response = sendMessage(
                MessageEntity(
                    topicId = conversationHistory.firstOrNull()?.topicId ?: "",
                    content = prompt,
                    senderType = "USER",
                    messageType = "TEXT"
                ),
                "learning-analysis"
            )

            val analysis = gson.fromJson(response.content, HunyuanAnalysisResponse::class.java)

            return ProgressAnalysis(
                overallProgress = analysis.overallProgress,
                conceptMastery = analysis.conceptMastery,
                learningVelocity = analysis.learningVelocity,
                knowledgeGaps = analysis.knowledgeGaps,
                recommendedNextSteps = analysis.recommendedNextSteps
            )
        } catch (e: Exception) {
            return ProgressAnalysis(
                overallProgress = 50,
                conceptMastery = emptyMap(),
                learningVelocity = 0.0,
                knowledgeGaps = listOf("需要更多对话数据"),
                recommendedNextSteps = listOf("继续学习")
            )
        }
    }

    override suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String> {
        if (!config.capabilities.supportSocraticQuestioning) {
            throw UnsupportedFeatureException.socraticQuestioning(config.displayName)
        }

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
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.replace(Regex("^\\d+\\.?\\s*"), "") }
            .filter { it.isNotBlank() }
            .take(5)
    }

    override suspend fun evaluateAnswer(userAnswer: MessageEntity, correctAnswer: String): AnswerEvaluation {
        if (!config.capabilities.supportAnswerEvaluation) {
            throw UnsupportedFeatureException.answerEvaluation(config.displayName)
        }

        val prompt = PromptTemplates.evaluateAnswer(userAnswer.content, correctAnswer)

        try {
            val response = sendMessage(
                MessageEntity(
                    topicId = userAnswer.topicId,
                    content = prompt,
                    senderType = "USER",
                    messageType = "TEXT"
                ),
                "answer-evaluation"
            )

            val evaluation = gson.fromJson(response.content, HunyuanEvaluationResponse::class.java)

            return AnswerEvaluation(
                isCorrect = evaluation.isCorrect,
                confidence = evaluation.confidence,
                feedback = evaluation.feedback,
                suggestedImprovement = evaluation.suggestedImprovement
            )
        } catch (e: Exception) {
            val similarity = calculateTextSimilarity(userAnswer.content, correctAnswer)
            return AnswerEvaluation(
                isCorrect = similarity > 0.8,
                confidence = similarity,
                feedback = if (similarity > 0.8) "答案正确" else "答案需要改进",
                suggestedImprovement = "请参考正确答案的表述方式"
            )
        }
    }

    // ===== 知识处理功能 =====

    override suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis {
        if (!config.capabilities.supportDocumentParsing) {
            throw UnsupportedFeatureException.documentParsing(config.displayName)
        }

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

    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        if (!config.capabilities.supportConceptExtraction) {
            throw UnsupportedFeatureException.conceptExtraction(config.displayName)
        }

        val prompt = PromptTemplates.extractKeyConcepts(content)

        try {
            val response = sendMessage(
                MessageEntity(
                    topicId = "temp-topic",
                    content = prompt,
                    senderType = "USER",
                    messageType = "TEXT"
                ),
                "concept-extraction"
            )

            val concepts = gson.fromJson(response.content, Array<HunyuanConcept>::class.java)
            return concepts.map { concept ->
                Concept(
                    id = concept.id,
                    name = concept.name,
                    definition = concept.definition,
                    relatedConcepts = concept.relatedConcepts
                )
            }.toList()
        } catch (e: Exception) {
            // 简单的关键词提取作为fallback
            val keywords = content.split(" ", "\n", ",", ".", ";", ":")
                .filter { it.length > 3 && it.length < 20 }
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(10)
                .mapIndexed { index, pair ->
                    Concept(
                        id = "concept_$index",
                        name = pair.first,
                        definition = "从文本中提取的关键概念",
                        relatedConcepts = emptyList()
                    )
                }
            return keywords
        }
    }

    // ===== 状态检测和管理 =====

    override suspend fun checkStatus(): AIServiceStatus {
        return try {
            val testRequest = """
                {"model": "${config.model}", "messages": [{"role": "user", "content": "test"}], "max_tokens": 5}
            """.trimIndent()

            val request = createSignedRequest(
                url = "${config.baseUrl}/v1/chat/completions",
                body = testRequest.toRequestBody("application/json".toMediaType())
            )

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
        // 腾讯云可能提供使用统计接口
        return UsageStatistics(
            totalCalls = config.freeQuota - remainingQuota,
            successfulCalls = config.freeQuota - remainingQuota,
            failedCalls = 0,
            totalTokensUsed = 0,
            totalCost = (config.freeQuota - remainingQuota) * config.pricePerMillion / 1000000.0,
            averageResponseTime = 0,
            lastCallTime = System.currentTimeMillis()
        )
    }

    // ===== 私有辅助方法 =====

    private fun buildChatRequest(message: MessageEntity, context: String): RequestBody {
        val chatMessages = if (context.isNotBlank()) {
            listOf(
                Message(role = "system", content = context),
                Message(
                    role = if (message.senderType == "USER") "user" else "assistant",
                    content = message.content
                )
            )
        } else {
            listOf(
                Message(
                    role = if (message.senderType == "USER") "user" else "assistant",
                    content = message.content
                )
            )
        }

        val request = HunyuanChatRequest(
            model = config.model,
            messages = chatMessages,
            maxTokens = 2000,
            temperature = 0.7
        )

        val json = gson.toJson(request)
        return json.toRequestBody("application/json".toMediaType())
    }

    private fun createSignedRequest(url: String, body: RequestBody): Request {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signature = generateSignature(
            secretKey = config.apiKey,
            timestamp = timestamp
        )

        return Request.Builder()
            .url(url)
            .addHeader("Authorization", signature)
            .addHeader("X-TC-Action", "ChatCompletions")
            .addHeader("X-TC-Version", "2023-09-01")
            .addHeader("X-TC-Timestamp", timestamp)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
    }

    private fun generateSignature(secretKey: String, timestamp: String): String {
        val stringToSign = "POST\nv1/chat/completions\n$timestamp"
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val signature = mac.doFinal(stringToSign.toByteArray())
        return Base64.encodeToString(signature, Base64.DEFAULT)
    }

    private fun calculateTextSimilarity(text1: String, text2: String): Double {
        val words1 = text1.toLowerCase().split(Regex("\\W+")).toSet()
        val words2 = text2.toLowerCase().split(Regex("\\W+")).toSet()
        val common = words1.intersect(words2).size
        val total = words1.union(words2).size
        return if (total > 0) common.toDouble() / total else 0.0
    }

    // ===== 数据类 =====

    data class Message(
        val role: String,
        val content: String
    )

    data class HunyuanChatRequest(
        val model: String,
        val messages: List<Message>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double
    )

    data class HunyuanChatResponse(
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

    data class HunyuanMindMapResponse(
        val structure: String,
        val nodes: List<MindMapNode>?
    )

    data class HunyuanAnalysisResponse(
        val overallProgress: Int,
        val conceptMastery: Map<String, Int>,
        val learningVelocity: Double,
        val knowledgeGaps: List<String>,
        val recommendedNextSteps: List<String>
    )

    data class HunyuanEvaluationResponse(
        val isCorrect: Boolean,
        val confidence: Double,
        val feedback: String,
        val suggestedImprovement: String?
    )

    data class HunyuanConcept(
        val id: String,
        val name: String,
        val definition: String,
        val relatedConcepts: List<String>
    )
}
