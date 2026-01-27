package com.autodroid.teachitback.api

import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.model.*

/**
 * 统一AI服务接口
 * 合并UnifiedAIService和原AIService的功能，支持所有大模型的无缝切换
 */
interface AIService {
    val config: AIServiceConfig
    val isAvailable: Boolean
    val remainingQuota: Long
    
    // ===== 基础对话功能 =====
    
    /**
     * 发送消息并获取回复
     * @param message 单个消息实体（已存储在本地数据库）
     * @param context 上下文信息
     * @return AI服务响应
     */
    suspend fun sendMessage(message: MessageEntity, context: String): AIServiceResponse
    
    /**
     * 处理文件内容
     * @param file 文件实体（已存储在本地数据库，包含topicId）
     * @param context 上下文信息
     * @return AI服务响应
     */
    suspend fun processFileContent(file: FileEntity, context: String): AIServiceResponse
    
    // ===== 教育专用功能 =====
    
    /**
     * 生成思维导图
     * @param topicId 主题ID
     * @param learningGoal 学习目标
     * @return 思维导图实体
     */
    suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity?
    
    /**
     * 分析学习进度
     * @param conversationHistory 对话历史
     * @return 进度分析结果
     */
    suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis
    
    /**
     * 生成苏格拉底式问题
     * @param topic 主题
     * @param currentLevel 当前水平
     * @return 问题列表
     */
    suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String>
    
    /**
     * 评估答案
     * @param userAnswer 用户答案（已存储在本地数据库）
     * @param correctAnswer 正确答案
     * @return 评估结果
     */
    suspend fun evaluateAnswer(userAnswer: MessageEntity, correctAnswer: String): AnswerEvaluation
    
    // ===== 知识处理功能 =====
    
    /**
     * 解析文档
     * @param file 文件实体（已存储在本地数据库，包含topicId）
     * @param fileType 文件类型
     * @return 文档分析结果
     */
    suspend fun parseDocument(file: FileEntity, fileType: String): DocumentAnalysis
    
    /**
     * 提取关键概念
     * @param content 内容
     * @return 概念列表
     */
    suspend fun extractKeyConcepts(content: String): List<Concept>
    
    /**
     * 构建知识图谱
     * @param concepts 概念列表
     * @return 知识图谱
     */
    suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph
    
    // ===== 状态检测和管理 =====
    
    /**
     * 检查服务状态
     * @return 服务状态
     */
    suspend fun checkStatus(): AIServiceStatus
    
    /**
     * 获取使用统计
     * @return 使用统计
     */
    suspend fun getUsageStatistics(): UsageStatistics
    
    // ===== 配置管理 =====
    
    /**
     * 更新配置
     * @param newConfig 新配置
     */
    suspend fun updateConfig(newConfig: AIServiceConfig)
}
