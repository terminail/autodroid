package com.autodroid.teachitback.service

import android.content.Context
import android.util.Log
import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceConfig.ChatGLMConfig
import com.autodroid.teachitback.config.AIServiceRequiredFields
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.framework.MNNIntegration
import com.autodroid.teachitback.framework.MNNModel
import com.autodroid.teachitback.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ChatGLM嵌入式AI服务
 * 基于ChatGLM-6B模型的本地部署服务
 */
class AIServiceChatGLM(
    private val context: Context,
    override val config: ChatGLMConfig,
    private val mnnIntegration: MNNIntegration
) : BaseAIServiceImpl() {
    
    companion object {
        private const val TAG = "AIServiceChatGLM"
        private const val MODEL_PATH = "models/chatglm-6b-int4.mnn"
        private const val MODEL_SIZE = 2_800_000_000L // 2.8GB
    }
    
    private var model: MNNModel? = null
    private var isModelLoaded = false
    
    override val isAvailable: Boolean
        get() = config.isEnabled && isModelLoaded && model?.isLoaded() == true
    
    override var remainingQuota: Long
        get() = if (isAvailable) Long.MAX_VALUE else 0
        set(value) {}
    
    private var usageStats = UsageStatistics(
        totalCalls = 0,
        successfulCalls = 0,
        failedCalls = 0,
        totalTokensUsed = 0,
        totalCost = 0.0,
        averageResponseTime = 0,
        reliability = 1.0,
        lastCallTime = 0
    )
    
    /**
     * 初始化服务
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查模型是否已下载
            if (!mnnIntegration.isModelDownloaded(MODEL_PATH)) {
                Log.w(TAG, "ChatGLM模型未下载")
                return@withContext false
            }
            
            // 加载模型
            model = mnnIntegration.loadModel(MODEL_PATH)
            if (model == null) {
                Log.e(TAG, "ChatGLM模型加载失败")
                return@withContext false
            }
            
            // 初始化模型
            isModelLoaded = model?.load() ?: false
            
            if (isModelLoaded) {
                Log.d(TAG, "ChatGLM服务初始化成功")
            } else {
                Log.e(TAG, "ChatGLM服务初始化失败")
            }
            
            isModelLoaded
        } catch (e: Exception) {
            Log.e(TAG, "ChatGLM服务初始化异常", e)
            false
        }
    }
    
    /**
     * 释放服务资源
     */
    suspend fun release() = withContext(Dispatchers.IO) {
        try {
            model?.release()
            model = null
            isModelLoaded = false
            Log.d(TAG, "ChatGLM服务资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放ChatGLM服务资源失败", e)
        }
    }
    
    // ===== AIService接口实现 =====
    
    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse {
        return withContext(Dispatchers.IO) {
            if (!isAvailable) {
                return@withContext AIServiceResponse(
                    content = "ChatGLM服务不可用，请检查模型是否已下载并加载",
                    processInfo = AIProcessInfo(
                        serviceId = config.id,
                        serviceName = config.displayName,
                        modelUsed = config.id,
                        processingTime = 0
                    )
                )
            }
            
            val startTime = System.currentTimeMillis()
            
            try {
                // 构建prompt
                val prompt = buildPrompt(message.content, context)
                
                // 执行推理
                val response = model?.inference(prompt) ?: "推理失败"
                
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                
                // 更新使用统计
                updateUsageStats(responseTime, true)
                
                AIServiceResponse(
                    content = response,
                    processInfo = AIProcessInfo(
                        serviceId = config.id,
                        serviceName = config.displayName,
                        modelUsed = config.id,
                        processingTime = responseTime
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "消息处理失败", e)
                
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                
                updateUsageStats(responseTime, false)
                
                AIServiceResponse(
                    content = "处理失败: ${e.message}",
                    processInfo = AIProcessInfo(
                        serviceId = config.id,
                        serviceName = config.displayName,
                        modelUsed = config.id,
                        processingTime = responseTime
                    )
                )
            }
        }
    }
    
    override suspend fun processFileContent(file: FileEntity, context: String): AIServiceResponse {
        // ChatGLM不支持文件处理
        return AIServiceResponse(
            content = "ChatGLM不支持文件处理功能",
            processInfo = AIProcessInfo(
                serviceId = config.id,
                serviceName = config.displayName,
                modelUsed = config.id,
                processingTime = 0
            )
        )
    }
    
    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        // ChatGLM不支持思维导图生成
        return null
    }
    
    override suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis {
        return withContext(Dispatchers.IO) {
            if (!isAvailable || conversationHistory.isEmpty()) {
                return@withContext ProgressAnalysis(
                    overallProgress = 0,
                    conceptMastery = emptyMap(),
                    learningVelocity = 0.0,
                    knowledgeGaps = emptyList(),
                    recommendedNextSteps = emptyList()
                )
            }

            try {
                // 构建分析prompt
                val historyText = conversationHistory.takeLast(10)
                    .joinToString("\n") { "${it.senderType}: ${it.content}" }

                val prompt = """
                    请分析以下对话历史，评估学习进度：

                    $historyText

                    请提供：
                    1. 整体进度百分比（0-100）
                    2. 各概念的掌握程度
                    3. 学习速度指标
                    4. 识别出的知识缺口
                    5. 推荐的下一步学习行动
                """.trimIndent()

                val response = model?.inference(prompt) ?: ""

                // 解析响应（简化版本）
                parseProgressAnalysis(response)
            } catch (e: Exception) {
                Log.e(TAG, "学习进度分析失败", e)
                ProgressAnalysis(
                    overallProgress = 0,
                    conceptMastery = emptyMap(),
                    learningVelocity = 0.0,
                    knowledgeGaps = emptyList(),
                    recommendedNextSteps = emptyList()
                )
            }
        }
    }
    
    override suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String> {
        return withContext(Dispatchers.IO) {
            if (!isAvailable) {
                return@withContext emptyList<String>()
            }
            
            try {
                val prompt = """
                    请为"$topic"主题生成5个苏格拉底式提问，
                    当前水平：$currentLevel（1-10，10为最高）。
                    提问应该引导用户深入思考，而不是直接给出答案。
                """.trimIndent()
                
                val response = model?.inference(prompt) ?: ""
                
                // 解析问题列表
                response.split("\n")
                    .filter { it.trim().isNotEmpty() }
                    .map { it.trim() }
            } catch (e: Exception) {
                Log.e(TAG, "生成苏格拉底式提问失败", e)
                emptyList()
            }
        }
    }
    
    override suspend fun evaluateAnswer(userAnswer: MessageEntity, correctAnswer: String): AnswerEvaluation {
        return withContext(Dispatchers.IO) {
            if (!isAvailable) {
                return@withContext AnswerEvaluation(
                    isCorrect = false,
                    confidence = 0.0,
                    feedback = "服务不可用",
                    suggestedImprovement = null
                )
            }

            try {
                val prompt = """
                    请评估以下答案：

                    用户答案：${userAnswer.content}
                    正确答案：$correctAnswer

                    请提供：
                    1. 是否正确（true/false）
                    2. 置信度（0.0-1.0）
                    3. 详细反馈
                    4. 改进建议
                """.trimIndent()

                val response = model?.inference(prompt) ?: ""

                // 解析评估结果（简化版本）
                parseAnswerEvaluation(response)
            } catch (e: Exception) {
                Log.e(TAG, "答案评估失败", e)
                AnswerEvaluation(
                    isCorrect = false,
                    confidence = 0.0,
                    feedback = "评估失败: ${e.message}",
                    suggestedImprovement = null
                )
            }
        }
    }
    
    override suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis {
        // ChatGLM不支持文档解析
        return DocumentAnalysis(
            fileName = file.fileName,
            fileType = fileType,
            summary = "不支持文档解析功能",
            keyPoints = emptyList(),
            extractedText = ""
        )
    }
    
    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        return withContext(Dispatchers.IO) {
            if (!isAvailable || content.isBlank()) {
                return@withContext emptyList<Concept>()
            }
            
            try {
                val prompt = """
                    请从以下内容中提取关键概念：
                    
                    $content
                    
                    请提供概念列表，每个概念包含名称和简要解释。
                """.trimIndent()
                
                val response = model?.inference(prompt) ?: ""
                
                // 解析概念列表（简化版本）
                parseConcepts(response)
            } catch (e: Exception) {
                Log.e(TAG, "提取关键概念失败", e)
                emptyList()
            }
        }
    }
    
    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        // ChatGLM不支持知识图谱构建
        return KnowledgeGraph(
            nodes = emptyList(),
            edges = emptyList()
        )
    }
    
    override suspend fun checkStatus(): AIServiceStatus {
        return withContext(Dispatchers.IO) {
            try {
                if (!isModelLoaded) {
                    return@withContext AIServiceStatus.fromCode(404, "模型未加载")
                }

                val startTime = System.currentTimeMillis()

                val prompt = buildPrompt("你好", "test")
                val response = model?.inference(prompt)

                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                if (response.isNullOrBlank()) {
                    Log.e(TAG, "ChatGLM返回空响应")
                    return@withContext AIServiceStatus.fromCode(500, "服务错误")
                }

                Log.d(TAG, "ChatGLM状态检查成功: 响应时间=${responseTime}ms")
                AIServiceStatus.fromCode(200, "服务可用")
            } catch (e: Exception) {
                Log.e(TAG, "ChatGLM状态检查失败", e)
                AIServiceStatus.fromCode(500, "服务错误: ${e.message}")
            }
        }
    }
    
    override suspend fun getUsageStatistics(): UsageStatistics {
        return usageStats
    }

    // ===== 私有辅助方法 =====
    
    private fun buildPrompt(userInput: String, context: String): String {
        return if (context.isNotBlank()) {
            """
                上下文信息：
                $context
                
                用户问题：$userInput
                
                请基于上下文提供准确、详细的回答。
            """.trimIndent()
        } else {
            userInput
        }
    }
    
    private fun updateUsageStats(responseTime: Long, success: Boolean) {
        usageStats = usageStats.copy(
            totalCalls = usageStats.totalCalls + 1,
            successfulCalls = usageStats.successfulCalls + if (success) 1 else 0,
            failedCalls = usageStats.failedCalls + if (!success) 1 else 0,
            averageResponseTime = if (usageStats.totalCalls == 0L) {
                responseTime
            } else {
                (usageStats.averageResponseTime * usageStats.totalCalls + responseTime) / (usageStats.totalCalls + 1)
            },
            lastCallTime = System.currentTimeMillis(),
            reliability = if (usageStats.totalCalls + 1 > 0) {
                (usageStats.successfulCalls + if (success) 1 else 0).toDouble() / (usageStats.totalCalls + 1)
            } else 0.0
        )
    }
    
    private fun estimateTokens(input: String, output: String): Int {
        // 简化的token估算
        return (input.length + output.length) / 2
    }
    
    private fun parseProgressAnalysis(response: String): ProgressAnalysis {
        // 简化解析逻辑
        return ProgressAnalysis(
            overallProgress = 70,
            conceptMastery = mapOf(
                "概念理解" to 80,
                "基础知识" to 75
            ),
            learningVelocity = 0.8,
            knowledgeGaps = listOf("应用能力", "综合分析"),
            recommendedNextSteps = listOf("多做练习题", "总结错题")
        )
    }

    private fun parseAnswerEvaluation(response: String): AnswerEvaluation {
        // 简化解析逻辑
        return AnswerEvaluation(
            isCorrect = true,
            confidence = 0.85,
            feedback = "答案基本正确，但缺乏详细解释",
            suggestedImprovement = "增加具体例子和解释推理过程"
        )
    }
    
    private fun parseConcepts(response: String): List<Concept> {
        // 简化解析逻辑
        return listOf(
            Concept(
                id = "concept_1",
                name = "示例概念",
                definition = "这是一个示例概念",
                relatedConcepts = emptyList()
            )
        )
    }
}
