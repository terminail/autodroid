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

class AIServiceKimi(
    private val context: android.content.Context,
    override val config: AIServiceConfig = AIServiceConfig.KimiConfig()
) : BaseAIServiceImpl() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override var isAvailable: Boolean = true
    override var remainingQuota: Long = config.freeQuota

    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse {
        val startTime = System.currentTimeMillis()

        try {
            val requestBody = buildChatRequest(message, context)
            val request = Request.Builder()
                .url("${config.baseUrl}/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("API请求失败: ${response.code}")
            }

            val chatResponse = gson.fromJson(responseBody, KimiChatResponse::class.java)
            val content = chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IOException("Empty response from Kimi")

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
            throw IOException("Kimi API调用失败: ${e.message}", e)
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

        val lines = response.content.lines()
        if (lines.isEmpty()) return null

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

    override suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis {
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

            val analysis = gson.fromJson(response.content, KimiAnalysisResponse::class.java)

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

    override suspend fun evaluateAnswer(userAnswer: MessageEntity, correctAnswer: String): AnswerEvaluation {
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

            val evaluation = gson.fromJson(response.content, KimiEvaluationResponse::class.java)

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

    override suspend fun extractKeyConcepts(content: String): List<Concept> {
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

            val concepts = gson.fromJson(response.content, Array<KimiConcept>::class.java)
            return concepts.map { concept ->
                Concept(
                    id = concept.id,
                    name = concept.name,
                    definition = concept.definition,
                    relatedConcepts = concept.relatedConcepts
                )
            }.toList()
        } catch (e: Exception) {
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

    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        return KnowledgeGraph(
            nodes = concepts,
            edges = concepts.flatMapIndexed { index, concept ->
                concept.relatedConcepts.map { relatedId ->
                    KnowledgeGraph.Edge(
                        from = concept.id,
                        to = relatedId,
                        relationship = "相关"
                    )
                }
            }
        )
    }

    override suspend fun checkStatus(): AIServiceStatus {
        return try {
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

    override suspend fun updateConfig(newConfig: AIServiceConfig) {
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

        val request = ChatRequest(
            model = config.model,
            messages = chatMessages,
            maxTokens = 2000,
            temperature = 0.7
        )

        val json = gson.toJson(request)
        return json.toRequestBody("application/json".toMediaType())
    }

    private fun calculateTextSimilarity(text1: String, text2: String): Double {
        val words1 = text1.toLowerCase().split(Regex("\\W+")).toSet()
        val words2 = text2.toLowerCase().split(Regex("\\W+")).toSet()
        val common = words1.intersect(words2).size
        val total = words1.union(words2).size
        return if (total > 0) common.toDouble() / total else 0.0
    }

    data class Message(
        val role: String,
        val content: String
    )

    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double
    )

    data class KimiChatResponse(
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

    data class KimiAnalysisResponse(
        val overallProgress: Int,
        val conceptMastery: Map<String, Int>,
        val learningVelocity: Double,
        val knowledgeGaps: List<String>,
        val recommendedNextSteps: List<String>
    )

    data class KimiEvaluationResponse(
        val isCorrect: Boolean,
        val confidence: Double,
        val feedback: String,
        val suggestedImprovement: String?
    )

    data class KimiConcept(
        val id: String,
        val name: String,
        val definition: String,
        val relatedConcepts: List<String>
    )
}
