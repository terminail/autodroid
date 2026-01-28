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
 * 智谱AI服务实现
 * 支持基础对话、长文本、代码生成、数学计算、教育功能
 * 继承BaseAIServiceImpl，只重写支持的功能
 */
class AIServiceZhipu(
    private val context: android.content.Context
) : BaseAIServiceImpl() {

    override val config: AIServiceConfig = AIServiceConfig.ZhipuConfig()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override var isAvailable: Boolean = true
    override var remainingQuota: Long = 0L

    // ===== 基础对话功能 =====

    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse {
        val startTime = System.currentTimeMillis()

        try {
            val requestBody = buildChatRequest(message, context)
            val request = Request.Builder()
                .url("${currentConfig.baseUrl}/chat/completions")
                .addHeader("Authorization", "Bearer ${currentConfig.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("API请求失败: ${response.code}")
            }

            val chatResponse = gson.fromJson(responseBody, ZhipuChatResponse::class.java)
            val content = chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IOException("Empty response from Zhipu")

            val processingTime = System.currentTimeMillis() - startTime
            remainingQuota--

            return AIServiceResponse(
                content = content,
                processInfo = AIProcessInfo(
                    serviceId = currentConfig.id,
                    serviceName = currentConfig.displayName,
                    modelUsed = currentConfig.model,
                    processingTime = processingTime
                )
            )
        } catch (e: Exception) {
            throw IOException("智谱AI API调用失败: ${e.message}", e)
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

    // ===== 教育专用功能 =====

    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        if (!currentConfig.capabilities.supportMindMapGeneration) {
            throw UnsupportedFeatureException.mindMapGeneration(currentConfig.displayName)
        }

        val prompt = PromptTemplates.generateMindMap(topicId, learningGoal)
        val response = sendMessage(
            MessageEntity(
                topicId = topicId,
                content = prompt,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "mind-map-generation"
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
        if (!currentConfig.capabilities.supportLearningAnalysis) {
            throw UnsupportedFeatureException.learningAnalysis(currentConfig.displayName)
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

            val analysis = gson.fromJson(response.content, ZhipuAnalysisResponse::class.java)

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
        if (!currentConfig.capabilities.supportSocraticQuestioning) {
            throw UnsupportedFeatureException.socraticQuestioning(currentConfig.displayName)
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
            .filter { it.isNotBlank() }
            .take(5)
    }

    override suspend fun evaluateAnswer(userAnswer: MessageEntity, correctAnswer: String): AnswerEvaluation {
        if (!currentConfig.capabilities.supportAnswerEvaluation) {
            throw UnsupportedFeatureException.answerEvaluation(currentConfig.displayName)
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

            val evaluation = gson.fromJson(response.content, ZhipuEvaluationResponse::class.java)

            return AnswerEvaluation(
                isCorrect = evaluation.isCorrect,
                confidence = evaluation.confidence,
                feedback = evaluation.feedback,
                suggestedImprovement = evaluation.suggestedImprovement
            )
        } catch (e: Exception) {
            return AnswerEvaluation(
                isCorrect = false,
                confidence = 0.0,
                feedback = "无法评估答案",
                suggestedImprovement = "请提供更多信息"
            )
        }
    }

    // ===== 知识处理功能 =====

    override suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis {
        if (!currentConfig.capabilities.supportDocumentParsing) {
            throw UnsupportedFeatureException.documentParsing(currentConfig.displayName)
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

        try {
            val analysis = gson.fromJson(response.content, ZhipuDocumentAnalysisResponse::class.java)

            return DocumentAnalysis(
                fileName = file.fileName,
                fileType = fileType,
                summary = analysis.summary,
                keyPoints = analysis.keyPoints,
                extractedText = file.content
            )
        } catch (e: Exception) {
            return DocumentAnalysis(
                fileName = file.fileName,
                fileType = fileType,
                summary = response.content,
                keyPoints = emptyList(),
                extractedText = file.content
            )
        }
    }

    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        if (!currentConfig.capabilities.supportConceptExtraction) {
            throw UnsupportedFeatureException.conceptExtraction(currentConfig.displayName)
        }

        val prompt = PromptTemplates.extractKeyConcepts(content)
        val response = sendMessage(
            MessageEntity(
                topicId = "concept-extraction",
                content = prompt,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "concept-extraction"
        )

        try {
            val conceptsResponse = gson.fromJson(response.content, ZhipuConceptsResponse::class.java)
            return conceptsResponse.concepts.map { concept ->
                Concept(
                    id = concept.id,
                    name = concept.name,
                    definition = concept.definition,
                    relatedConcepts = concept.relatedConcepts
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        if (!currentConfig.capabilities.supportKnowledgeGraph) {
            throw UnsupportedFeatureException.knowledgeGraph(currentConfig.displayName)
        }

        val conceptsJson = gson.toJson(concepts)
        val prompt = "请基于以下概念构建知识图谱：\n$conceptsJson"
        
        val response = sendMessage(
            MessageEntity(
                topicId = "knowledge-graph",
                content = prompt,
                senderType = "USER",
                messageType = "TEXT"
            ),
            "knowledge-graph"
        )

        try {
            return gson.fromJson(response.content, KnowledgeGraph::class.java)
        } catch (e: Exception) {
            return KnowledgeGraph(
                nodes = concepts,
                edges = emptyList()
            )
        }
    }

    // ===== 状态检查 =====

    override suspend fun checkStatus(): AIServiceStatus {
        try {
            android.util.Log.d("AIServiceZhipu", "checkStatus: currentConfig.apiKey = ${currentConfig.apiKey.take(10)}...")
            android.util.Log.d("AIServiceZhipu", "checkStatus: currentConfig.baseUrl = ${currentConfig.baseUrl}")
            android.util.Log.d("AIServiceZhipu", "checkStatus: currentConfig.model = ${currentConfig.model}")
            
            val testMessage = "Hello"
            val requestBody = buildChatRequest(
                MessageEntity(
                    topicId = "status-check",
                    content = testMessage,
                    senderType = "USER",
                    messageType = "TEXT"
                ),
                "status-check"
            )

            val request = Request.Builder()
                .url("${currentConfig.baseUrl}/chat/completions")
                .addHeader("Authorization", "Bearer ${currentConfig.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            android.util.Log.d("AIServiceZhipu", "checkStatus: Request URL = ${request.url}")
            android.util.Log.d("AIServiceZhipu", "checkStatus: Request headers = ${request.headers}")
            
            val response = client.newCall(request).execute()
            
            android.util.Log.d("AIServiceZhipu", "checkStatus: Response code = ${response.code}")
            
            return if (response.isSuccessful) {
                AIServiceStatus.STATUS_OK
            } else {
                when (response.code) {
                    401 -> AIServiceStatus.fromCode(401, "API Key无效")
                    429 -> AIServiceStatus.fromCode(429, "请求频率超限")
                    500 -> AIServiceStatus.fromCode(500, "智谱AI服务器错误")
                    else -> AIServiceStatus.fromCode(response.code, "未知错误: ${response.code}")
                }
            }
        } catch (e: Exception) {
            return AIServiceStatus.fromCode(500, "连接失败: ${e.message}")
        }
    }

    // ===== 辅助方法 =====

    private fun buildChatRequest(message: MessageEntity, context: String): RequestBody {
        val messages = mutableListOf<ChatMessage>()
        
        if (context.isNotEmpty()) {
            messages.add(ChatMessage(role = "system", content = context))
        }
        
        messages.add(ChatMessage(role = "user", content = message.content))

        val chatRequest = ZhipuChatRequest(
            model = currentConfig.model,
            messages = messages,
            temperature = 0.7,
            max_tokens = 2000
        )

        val json = gson.toJson(chatRequest)
        return json.toRequestBody("application/json".toMediaType())
    }

    // ===== 数据类 =====

    data class ZhipuChatRequest(
        @SerializedName("model")
        val model: String,
        @SerializedName("messages")
        val messages: List<ChatMessage>,
        @SerializedName("temperature")
        val temperature: Double = 0.7,
        @SerializedName("max_tokens")
        val max_tokens: Int = 2000
    )

    data class ChatMessage(
        @SerializedName("role")
        val role: String,
        @SerializedName("content")
        val content: String
    )

    data class ZhipuChatResponse(
        @SerializedName("choices")
        val choices: List<ZhipuChoice>,
        @SerializedName("usage")
        val usage: Usage?
    )

    data class ZhipuChoice(
        @SerializedName("message")
        val message: ChatMessage
    )

    data class Usage(
        @SerializedName("prompt_tokens")
        val prompt_tokens: Int,
        @SerializedName("completion_tokens")
        val completion_tokens: Int,
        @SerializedName("total_tokens")
        val total_tokens: Int
    )

    data class ZhipuAnalysisResponse(
        @SerializedName("overallProgress")
        val overallProgress: Int,
        @SerializedName("conceptMastery")
        val conceptMastery: Map<String, Int>,
        @SerializedName("learningVelocity")
        val learningVelocity: Double,
        @SerializedName("knowledgeGaps")
        val knowledgeGaps: List<String>,
        @SerializedName("recommendedNextSteps")
        val recommendedNextSteps: List<String>
    )

    data class ZhipuEvaluationResponse(
        @SerializedName("isCorrect")
        val isCorrect: Boolean,
        @SerializedName("confidence")
        val confidence: Double,
        @SerializedName("feedback")
        val feedback: String,
        @SerializedName("suggestedImprovement")
        val suggestedImprovement: String
    )

    data class ZhipuDocumentAnalysisResponse(
        @SerializedName("summary")
        val summary: String,
        @SerializedName("keyPoints")
        val keyPoints: List<String>,
        @SerializedName("concepts")
        val concepts: List<String>
    )

    data class ZhipuConceptsResponse(
        @SerializedName("concepts")
        val concepts: List<ZhipuConcept>
    )

    data class ZhipuConcept(
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("definition")
        val definition: String,
        @SerializedName("relatedConcepts")
        val relatedConcepts: List<String>
    )
}
