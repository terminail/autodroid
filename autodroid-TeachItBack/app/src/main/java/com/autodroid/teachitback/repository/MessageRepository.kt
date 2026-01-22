package com.autodroid.teachitback.repository

import com.autodroid.teachitback.api.TencentCloudAIService
import com.autodroid.teachitback.database.MessageDao
import com.autodroid.teachitback.model.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Message数据仓库
 * 负责协调本地数据库和AI服务，实现Local-First策略
 */
class MessageRepository(
    private val messageDao: MessageDao,
    private val aiService: TencentCloudAIService
) {

    // ===== Flow 数据流 =====

    fun getMessagesByTopic(topicId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesByTopic(topicId)

    // ===== Local-First: 保存消息并获取AI回复 =====

    /**
     * 发送消息并获取AI回复
     * Local-First策略：
     * 1. 保存用户消息到本地数据库
     * 2. 获取对话历史（用于AI分析）
     * 3. 异步调用AI服务分析进度
     * 4. 构建上下文并获取AI回复
     * 5. 保存AI回复到本地数据库
     */
    suspend fun sendMessageAndGetReply(
        topicId: String,
        userContent: String
    ): MessageEntity? {
        // 1. 保存用户消息到本地数据库
        val userMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            content = userContent,
            senderType = "USER",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMessage)

        // 2. 获取对话历史（用于AI分析）
        val conversationHistory = messageDao.getMessagesByTopicSync(topicId)

        // 3. 异步调用AI服务分析进度
        val progressAnalysis = aiService.analyzeLearningProgress(conversationHistory)

        // 4. 构建上下文并获取AI回复
        val context = buildContext(conversationHistory, progressAnalysis)
        val aiResponse = aiService.sendMessage(conversationHistory, context)

        // 5. 保存AI回复到本地数据库
        val aiMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            content = aiResponse,
            senderType = "AI",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(aiMessage)

        return aiMessage
    }

    /**
     * 构建AI对话上下文
     */
    private fun buildContext(
        conversationHistory: List<MessageEntity>,
        progressAnalysis: com.autodroid.teachitback.model.ProgressAnalysis
    ): String {
        return buildString {
            append("当前学习进度: ${progressAnalysis.overallProgress}%\n")
            append("掌握度: ${progressAnalysis.conceptMastery}\n")
            if (progressAnalysis.knowledgeGaps.isNotEmpty()) {
                append("知识缺口: ${progressAnalysis.knowledgeGaps.joinToString(", ")}\n")
            }
            if (progressAnalysis.recommendedNextSteps.isNotEmpty()) {
                append("推荐下一步: ${progressAnalysis.recommendedNextSteps.joinToString(", ")}\n")
            }
        }
    }

    // ===== 数据库操作 =====

    suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)

    suspend fun updateMessage(message: MessageEntity) = messageDao.updateMessage(message)

    suspend fun deleteMessagesByTopic(topicId: String) =
        messageDao.deleteMessagesByTopic(topicId)
}
