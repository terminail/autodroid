package com.autodroid.teachitback.service

import android.content.Context
import android.util.Log
import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.config.AIServiceConfig.TinyBERTConfig
import com.autodroid.teachitback.config.AIServiceRequiredFields
import com.autodroid.teachitback.framework.MNNIntegration
import com.autodroid.teachitback.framework.MNNModel
import com.autodroid.teachitback.model.*
import com.autodroid.teachitback.data.KnowledgeBaseManager
import com.autodroid.teachitback.data.KnowledgeBaseEntry
import com.autodroid.teachitback.data.SemanticMatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sqrt
import java.io.BufferedReader

/**
 * TinyBERT嵌入式AI服务
 * 轻量级BERT模型，专门用于快速答案判断和相似度计算
 */
class AIServiceTinyBERT(
    private val context: Context,
    override val config: TinyBERTConfig,
    private val mnnIntegration: MNNIntegration
) : AIService {
    
    companion object {
        private const val TAG = "AIServiceTinyBERT"
        private const val MODEL_PATH = "models/tinybert-int8.mnn"
        private const val MODEL_SIZE = 100_000_000L // 100MB
        private const val MAX_RESPONSE_TIME = 200L // 最大响应时间200ms
    }
    
    private var model: MNNModel? = null
    private var isModelLoaded = false
    
    private val knowledgeBaseManager = KnowledgeBaseManager()
    private var isKnowledgeBaseLoaded = false
    
    override val isAvailable: Boolean
        get() = isModelLoaded && model?.isLoaded() == true
    
    override val remainingQuota: Long
        get() = if (isAvailable) Long.MAX_VALUE else 0
    
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
            Log.d(TAG, "========== 开始初始化TinyBERT服务 ==========")
            
            // 检查模型是否已下载
            val isDownloaded = mnnIntegration.isModelDownloaded(MODEL_PATH)
            Log.d(TAG, "模型下载状态: $isDownloaded (路径: $MODEL_PATH)")
            
            if (!isDownloaded) {
                Log.w(TAG, "TinyBERT模型未下载")
                return@withContext false
            }
            
            val modelSize = mnnIntegration.getModelFileSize(MODEL_PATH)
            Log.d(TAG, "模型文件大小: $modelSize bytes")
            
            // 加载模型
            Log.d(TAG, "开始加载模型...")
            model = mnnIntegration.loadModel(MODEL_PATH)
            if (model == null) {
                Log.e(TAG, "TinyBERT模型加载失败: loadModel() 返回 null")
                return@withContext false
            }
            Log.d(TAG, "模型加载成功")
            
            // 初始化模型
            Log.d(TAG, "开始初始化模型...")
            isModelLoaded = model?.load() ?: false
            Log.d(TAG, "模型初始化结果: $isModelLoaded")
            
            if (isModelLoaded) {
                // 检查词汇表是否加载成功
                val tokenizerLoaded = model?.getTokenizer() != null
                if (!tokenizerLoaded) {
                    Log.e(TAG, "✗ TinyBERT服务初始化失败: 词汇表加载失败")
                    isModelLoaded = false
                } else {
                    Log.i(TAG, "✓ TinyBERT服务初始化成功")
                }
            } else {
                Log.e(TAG, "✗ TinyBERT服务初始化失败: model?.load() 返回 false")
            }
            
            isModelLoaded
        } catch (e: Exception) {
            Log.e(TAG, "✗ TinyBERT服务初始化异常", e)
            false
        }
    }
    
    /**
     * 加载知识库
     */
    suspend fun loadKnowledgeBase(entries: List<KnowledgeBaseEntry>): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始加载知识库，条目数: ${entries.size}")
            knowledgeBaseManager.clear()
            knowledgeBaseManager.addEntries(entries)
            isKnowledgeBaseLoaded = true
            Log.i(TAG, "✓ 知识库加载成功，共 ${entries.size} 条")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ 知识库加载失败", e)
            false
        }
    }
    
    /**
     * 从文件加载知识库
     */
    suspend fun loadKnowledgeBaseFromFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "从文件加载知识库: $filePath")
            
            // 尝试从多个路径加载文件
            val possiblePaths = listOf(
                filePath,  // 原始路径
                "/sdcard/$filePath",  // SD卡根目录
                "/storage/emulated/0/$filePath",  // 内部存储根目录
                "/sdcard/Android/data/com.autodroid.teachitback/files/$filePath",  // 应用私有目录
                context.filesDir.absolutePath + "/$filePath"  // 应用files目录
            )
            
            var inputStream: java.io.InputStream? = null
            var loadedPath = ""
            
            for (path in possiblePaths) {
                try {
                    val file = java.io.File(path)
                    if (file.exists() && file.canRead()) {
                        inputStream = java.io.FileInputStream(file)
                        loadedPath = path
                        Log.d(TAG, "找到知识库文件: $path")
                        break
                    }
                } catch (e: Exception) {
                    // 继续尝试下一个路径
                }
            }
            
            // 如果文件系统找不到，尝试从 assets 加载
            if (inputStream == null) {
                try {
                    inputStream = context.assets.open(filePath)
                    loadedPath = "assets:$filePath"
                    Log.d(TAG, "从 assets 加载知识库文件: $filePath")
                } catch (e: Exception) {
                    Log.w(TAG, "无法从 assets 加载: ${e.message}")
                }
            }
            
            if (inputStream == null) {
                Log.e(TAG, "✗ 无法找到知识库文件，已尝试路径: ${possiblePaths.joinToString(", ")}")
                return@withContext false
            }
            
            val reader = BufferedReader(java.io.InputStreamReader(inputStream))
            val entries = mutableListOf<KnowledgeBaseEntry>()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split("|")
                        if (parts.size >= 3) {
                            val entry = KnowledgeBaseEntry(
                                id = parts[0],
                                question = parts[1],
                                answer = parts[2],
                                category = if (parts.size > 3) parts[3] else "",
                                tags = if (parts.size > 4) parts[4].split(",") else emptyList()
                            )
                            entries.add(entry)
                        }
                    }
                }
            }
            
            reader.close()
            inputStream.close()
            
            if (entries.isNotEmpty()) {
                knowledgeBaseManager.clear()
                knowledgeBaseManager.addEntries(entries)
                isKnowledgeBaseLoaded = true
                Log.i(TAG, "✓ 知识库加载成功，共 ${entries.size} 条 (来源: $loadedPath)")
                true
            } else {
                Log.w(TAG, "✗ 知识库文件为空或格式错误")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ 知识库加载失败", e)
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
            Log.d(TAG, "TinyBERT服务资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放TinyBERT服务资源失败", e)
        }
    }
    
    // ===== AIService接口实现 =====
    
    override suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse {
        Log.d(TAG, "=== TinyBERT sendMessage 开始 ===")
        Log.d(TAG, "消息ID: ${message.id}")
        Log.d(TAG, "消息内容: '${message.content}'")
        Log.d(TAG, "上下文: ${context.take(100)}...")
        Log.d(TAG, "服务可用状态: isAvailable=$isAvailable, isModelLoaded=$isModelLoaded")
        
        return withContext(Dispatchers.IO) {
            if (!isAvailable) {
                Log.w(TAG, "TinyBERT服务不可用，模型状态: isAvailable=$isAvailable, isModelLoaded=$isModelLoaded")
                return@withContext AIServiceResponse(
                    content = "TinyBERT服务不可用，请检查模型是否已下载并加载",
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
                val query = message.content
                Log.d(TAG, "收到查询: query=${query.take(50)}...")
                
                // 使用语义匹配找到最相关的问题
                val matchResult = semanticMatch(query)
                
                val response = if (matchResult != null) {
                    val entry = matchResult.entry
                    val similarity = matchResult.similarity
                    
                    Log.d(TAG, "找到匹配: question=${entry.question}, similarity=$similarity, rank=${matchResult.rank}")
                    
                    if (similarity >= 0.4) {
                        // 高相似度，直接返回答案
                        entry.answer
                    } else if (similarity >= 0.3) {
                        // 中等相似度，返回答案并提示
                        "${entry.answer}\n\n(相似度: ${(similarity * 100).toInt()}%)"
                    } else {
                        // 低相似度，返回提示
                        "抱歉，我没有找到与您的问题高度匹配的答案。\n\n最接近的问题是：${entry.question}\n\n相似度较低 (${(similarity * 100).toInt()}%)，建议您重新表述问题或使用其他AI服务。"
                    }
                } else {
                    // 没有找到匹配
                    if (!isKnowledgeBaseLoaded) {
                        "知识库未加载，无法进行语义匹配。\n\n请先加载知识库数据。"
                    } else {
                        "抱歉，知识库中没有找到相关答案。\n\n建议您：\n1. 重新表述问题\n2. 使用其他AI服务（如DeepSeek或腾讯云混元）"
                    }
                }
                
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                
                Log.d(TAG, "响应生成完成: 耗时=${responseTime}ms")
                
                // 更新使用统计
                updateUsageStats(responseTime, true)
                
                val aiResponse = AIServiceResponse(
                    content = response,
                    processInfo = AIProcessInfo(
                        serviceId = config.id,
                        serviceName = config.displayName,
                        modelUsed = config.id,
                        processingTime = responseTime
                    )
                )
                
                Log.d(TAG, "消息处理成功: 内容长度=${response.length}, 处理时间=${responseTime}ms")
                aiResponse
            } catch (e: Exception) {
                Log.e(TAG, "消息处理失败", e)
                
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                
                Log.e(TAG, "处理异常，耗时: ${responseTime}ms", e)
                
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
        // TinyBERT不支持文件处理
        return AIServiceResponse(
            content = "TinyBERT不支持文件处理功能",
            processInfo = AIProcessInfo(
                serviceId = config.id,
                serviceName = config.displayName,
                modelUsed = config.id,
                processingTime = 0
            )
        )
    }
    
    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        // TinyBERT不支持思维导图生成
        return null
    }
    
    override suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis {
        // TinyBERT不支持学习进度分析
        return ProgressAnalysis(
            overallProgress = 0,
            conceptMastery = emptyMap(),
            learningVelocity = 0.0,
            knowledgeGaps = emptyList(),
            recommendedNextSteps = emptyList()
        )
    }
    
    override suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String> {
        // TinyBERT不支持生成苏格拉底式提问
        return emptyList()
    }
    
    override suspend fun evaluateAnswer(userAnswer: MessageEntity, correctAnswer: String): AnswerEvaluation {
        return withContext(Dispatchers.IO) {
            if (!isAvailable) {
                return@withContext AnswerEvaluation(
                    isCorrect = false,
                    confidence = 0.0,
                    feedback = "TinyBERT服务不可用",
                    suggestedImprovement = null
                )
            }

            val startTime = System.currentTimeMillis()

            try {
                // 使用TinyBERT计算答案相似度
                val similarity = calculateSimilarity(userAnswer.content, correctAnswer)

                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                // 根据相似度判断正确性和置信度
                val isCorrect = similarity >= 0.85
                val confidence = similarity

                val feedback = when {
                    isCorrect -> "答案完全正确！"
                    similarity >= 0.7 -> "答案基本正确，但还有一些细节需要完善。"
                    similarity >= 0.5 -> "答案部分正确，需要进一步改进。"
                    else -> "答案与正确答案差距较大，建议重新学习相关知识点。"
                }

                val suggestion = when {
                    isCorrect -> null
                    similarity >= 0.7 -> "可以添加更多细节，解释可以更清晰"
                    similarity >= 0.5 -> "需要补充关键知识点，理解还不够深入"
                    else -> "建议重新学习相关概念，多做相关练习题"
                }

                // 更新使用统计
                updateUsageStats(responseTime, true)

                AnswerEvaluation(
                    isCorrect = isCorrect,
                    confidence = confidence,
                    feedback = feedback,
                    suggestedImprovement = suggestion
                )
            } catch (e: Exception) {
                Log.e(TAG, "答案评估失败", e)

                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                updateUsageStats(responseTime, false)

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
        // TinyBERT不支持文档解析
        return DocumentAnalysis(
            fileName = file.fileName,
            fileType = fileType,
            summary = "不支持文档解析功能",
            keyPoints = emptyList(),
            extractedText = ""
        )
    }
    
    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        // TinyBERT不支持提取关键概念
        return emptyList()
    }
    
    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        // TinyBERT不支持知识图谱构建
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

                // 简单验证：检查模型是否可用，不进行实际推理
                val isModelReady = model?.isLoaded() == true
                
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                if (responseTime > MAX_RESPONSE_TIME) {
                    Log.w(TAG, "TinyBERT响应时间过长: ${responseTime}ms")
                    return@withContext AIServiceStatus.fromCode(500, "响应超时")
                }

                if (!isModelReady) {
                    Log.e(TAG, "TinyBERT模型未就绪")
                    return@withContext AIServiceStatus.fromCode(500, "模型错误")
                }

                Log.d(TAG, "TinyBERT状态检查成功: 响应时间=${responseTime}ms")
                return@withContext AIServiceStatus.fromCode(200, "正常")
            } catch (e: Exception) {
                Log.e(TAG, "TinyBERT状态检查失败: ${e.message}")
                AIServiceStatus.fromCode(500, "检查失败: ${e.message}")
            }
        }
    }
    
    override suspend fun getUsageStatistics(): UsageStatistics {
        return usageStats
    }
    
    override suspend fun updateConfig(newConfig: AIServiceConfig) {
        // 配置更新逻辑
        Log.d(TAG, "配置已更新: ${newConfig.id}")
    }

    // ===== 私有辅助方法 =====
    
    /**
     * 计算文本相似度（使用 BERT embedding）
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度（0-1）
     */
    private suspend fun calculateSimilarity(text1: String, text2: String): Double {
        return withContext(Dispatchers.Default) {
            try {
                if (model == null || !isAvailable) {
                    Log.w(TAG, "模型不可用，使用简化相似度计算")
                    return@withContext calculateSimpleSimilarity(text1, text2)
                }
                
                Log.d(TAG, "开始计算 BERT embedding 相似度")
                Log.d(TAG, "文本1: $text1")
                Log.d(TAG, "文本2: $text2")
                
                // 提取文本1的 embedding
                val embedding1 = model!!.extractEmbedding(text1)
                if (embedding1 == null) {
                    Log.w(TAG, "文本1 embedding 提取失败，使用简化相似度")
                    return@withContext calculateSimpleSimilarity(text1, text2)
                }
                Log.d(TAG, "Embedding1 提取完成: size=${embedding1.size}")
                
                // 提取文本2的 embedding
                val embedding2 = model!!.extractEmbedding(text2)
                if (embedding2 == null) {
                    Log.w(TAG, "文本2 embedding 提取失败，使用简化相似度")
                    return@withContext calculateSimpleSimilarity(text1, text2)
                }
                Log.d(TAG, "Embedding2 提取完成: size=${embedding2.size}")
                
                // 计算余弦相似度
                val similarity = cosineSimilarity(embedding1, embedding2)
                
                Log.d(TAG, "BERT embedding 相似度: $similarity")
                similarity
            } catch (e: Exception) {
                Log.e(TAG, "BERT embedding 相似度计算失败，使用简化方法", e)
                calculateSimpleSimilarity(text1, text2)
            }
        }
    }
    
    /**
     * 计算余弦相似度（基于 embedding 向量）
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度（0-1）
     */
    private fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Double {
        if (vector1.size != vector2.size) {
            Log.w(TAG, "向量维度不匹配: ${vector1.size} vs ${vector2.size}")
            return 0.0
        }
        
        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0
        
        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            magnitude1 += vector1[i] * vector1[i]
            magnitude2 += vector2[i] * vector2[i]
        }
        
        magnitude1 = sqrt(magnitude1)
        magnitude2 = sqrt(magnitude2)
        
        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0.0
        }
    }
    
    /**
     * 简化的相似度计算（基于字符）
     * 当 BERT 模型不可用时使用
     */
    private fun calculateSimpleSimilarity(text1: String, text2: String): Double {
        // 文本预处理
        val processed1 = preprocessText(text1)
        val processed2 = preprocessText(text2)
        
        if (processed1.isEmpty() || processed2.isEmpty()) {
            return 0.0
        }
        
        // 计算余弦相似度（简化版本）
        val vector1 = textToVector(processed1)
        val vector2 = textToVector(processed2)
        
        return cosineSimilaritySimple(vector1, vector2)
    }
    
    /**
     * 文本预处理
     */
    private fun preprocessText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^\\w\\s]"), "") // 移除标点符号
            .trim()
    }
    
    /**
     * 将文本转换为向量（简化版本）
     */
    private fun textToVector(text: String): Map<Char, Int> {
        val vector = mutableMapOf<Char, Int>()
        for (char in text) {
            if (!char.isWhitespace()) {
                vector[char] = (vector[char] ?: 0) + 1
            }
        }
        return vector
    }
    
    /**
     * 计算余弦相似度（简化版本，基于字符向量）
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度（0-1）
     */
    private fun cosineSimilaritySimple(vector1: Map<Char, Int>, vector2: Map<Char, Int>): Double {
        val allChars = (vector1.keys + vector2.keys).toSet()
        
        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0
        
        for (char in allChars) {
            val val1 = vector1[char] ?: 0
            val val2 = vector2[char] ?: 0
            
            dotProduct += val1 * val2
            magnitude1 += val1 * val1
            magnitude2 += val2 * val2
        }
        
        magnitude1 = sqrt(magnitude1)
        magnitude2 = sqrt(magnitude2)
        
        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0.0
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
        return (input.length + output.length) / 3
    }
    
    // ===== 语义匹配功能 =====
    
    /**
     * 语义匹配：根据用户查询找到最相关的问题
     * @param query 用户查询
     * @return 匹配结果（包含最相关的问题和相似度）
     */
    private suspend fun semanticMatch(query: String): SemanticMatchResult? = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "semanticMatch: 步骤1 - 检查知识库状态")
            
            if (!isKnowledgeBaseLoaded) {
                Log.w(TAG, "知识库未加载，无法进行语义匹配")
                return@withContext null
            }
            
            val entries = knowledgeBaseManager.getAllEntries()
            if (entries.isEmpty()) {
                Log.w(TAG, "知识库为空")
                return@withContext null
            }
            
            Log.d(TAG, "开始语义匹配: query=$query, 知识库条目数=${entries.size}")
            
            // 使用BERT embedding进行语义匹配
            Log.d(TAG, "使用BERT embedding进行语义匹配")
            
            Log.d(TAG, "semanticMatch: 步骤2 - 开始循环匹配")
            
            val matchResults = mutableListOf<SemanticMatchResult>()
            
            for (entry in entries) {
                // 使用BERT embedding计算相似度
                val similarity = calculateSimilarity(query, entry.question)
                matchResults.add(SemanticMatchResult(entry, similarity, 0))
            }
            
            Log.d(TAG, "semanticMatch: 步骤3 - 排序结果")
            
            // 按相似度排序
            matchResults.sortByDescending { it.similarity }
            
            // 更新排名
            matchResults.forEachIndexed { index, result ->
                matchResults[index] = result.copy(rank = index + 1)
            }
            
            // 返回最匹配的结果
            val bestMatch = matchResults.firstOrNull()
            
            if (bestMatch != null) {
                Log.d(TAG, "语义匹配完成: 最佳匹配=${bestMatch.entry.question}, 相似度=${bestMatch.similarity}, 排名=${bestMatch.rank}")
                Log.d(TAG, "Top 3 匹配:")
                matchResults.take(3).forEach { result ->
                    Log.d(TAG, "  ${result.rank}. ${result.entry.question} (相似度: ${result.similarity})")
                }
            }
            
            bestMatch
        } catch (e: Exception) {
            Log.e(TAG, "语义匹配失败", e)
            null
        }
    }
    
    /**
     * 批量语义匹配：返回所有匹配结果
     * @param query 用户查询
     * @param topK 返回前K个结果
     * @return 匹配结果列表
     */
    suspend fun semanticMatchBatch(query: String, topK: Int = 5): List<SemanticMatchResult> = withContext(Dispatchers.Default) {
        try {
            if (!isKnowledgeBaseLoaded) {
                Log.w(TAG, "知识库未加载，无法进行语义匹配")
                return@withContext emptyList()
            }
            
            val entries = knowledgeBaseManager.getAllEntries()
            if (entries.isEmpty()) {
                Log.w(TAG, "知识库为空")
                return@withContext emptyList()
            }
            
            Log.d(TAG, "开始批量语义匹配: query=$query, topK=$topK, 知识库条目数=${entries.size}")
            
            // 提取查询的 embedding
            val queryEmbedding = if (model != null && isAvailable) {
                try {
                    model!!.extractEmbedding(query)
                } catch (e: Exception) {
                    Log.e(TAG, "提取查询 embedding 失败", e)
                    null
                }
            } else {
                null
            }
            
            // 计算每个条目的相似度
            val matchResults = mutableListOf<SemanticMatchResult>()
            
            for (entry in entries) {
                val similarity = if (queryEmbedding != null) {
                    // 使用 BERT embedding 计算相似度
                    try {
                        val entryEmbedding = model!!.extractEmbedding(entry.question)
                        if (entryEmbedding != null) {
                            cosineSimilarity(queryEmbedding, entryEmbedding)
                        } else {
                            Log.w(TAG, "条目 embedding 提取失败: ${entry.question}")
                            calculateSimpleSimilarity(query, entry.question)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "提取条目 embedding 失败: ${entry.question}", e)
                        // 降级到简化相似度计算
                        calculateSimpleSimilarity(query, entry.question)
                    }
                } else {
                    // 使用简化相似度计算
                    calculateSimpleSimilarity(query, entry.question)
                }
                
                matchResults.add(SemanticMatchResult(entry, similarity, 0))
            }
            
            // 按相似度排序
            matchResults.sortByDescending { it.similarity }
            
            // 更新排名
            matchResults.forEachIndexed { index, result ->
                matchResults[index] = result.copy(rank = index + 1)
            }
            
            // 返回前K个结果
            val topResults = matchResults.take(topK)
            
            Log.d(TAG, "批量语义匹配完成: 返回 ${topResults.size} 个结果")
            topResults.forEach { result ->
                Log.d(TAG, "  ${result.rank}. ${result.entry.question} (相似度: ${result.similarity})")
            }
            
            topResults
        } catch (e: Exception) {
            Log.e(TAG, "批量语义匹配失败", e)
            emptyList()
        }
    }
}
