package com.autodroid.teachitback.repository

import android.util.Log
import com.autodroid.teachitback.database.MessageDao
import com.autodroid.teachitback.model.AIServiceResponse
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.router.AIServiceRouter
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Message数据仓库
 * 负责协调本地数据库和AI路由服务，实现Local-First策略
 *
 * 架构原则：
 * - ViewModel不感知AI服务，只与Repository交互
 * - Repository内部使用AIServiceRouter单例
 * - Local-First策略：先返回本地数据，异步调用AI服务
 */
class MessageRepository(
    private val messageDao: MessageDao
) {

    // Repository内部使用AIServiceRouter单例，对ViewModel完全透明
    private val aiRouter: AIServiceRouter
        get() = AIServiceRouter.instance

    // ===== Flow 数据流 =====

    fun getMessagesByTopic(topicId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesByTopic(topicId)

    // ===== Local-First: 保存消息并获取AI回复 =====

    /**
     * 发送消息并获取AI回复
     * Local-First策略：
     * 1. 保存用户消息到本地数据库
     * 2. 获取对话历史（用于AI分析）
     * 3. 使用AIServiceRouter智能路由调用AI服务分析进度
     * 4. 构建上下文并使用AIServiceRouter获取AI回复
     * 5. 保存AI回复到本地数据库（包含AIProcessInfo）
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
        Log.d("MessageRepository", "已保存用户消息: ${userMessage.id}")

        // 2. 获取对话历史（用于AI分析）
        val conversationHistory = messageDao.getMessagesByTopicSync(topicId)
        Log.d("MessageRepository", "获取对话历史: ${conversationHistory.size} 条消息")

        // 3. 使用AIServiceRouter智能路由调用AI服务分析进度
        val progressAnalysis = try {
            aiRouter.routeByCapability(
                capabilityCheck = { it.supportLearningAnalysis },
                operation = { service -> service.analyzeLearningProgress(conversationHistory) }
            )
        } catch (e: Exception) {
            Log.e("MessageRepository", "学习进度分析失败: ${e.message}", e)
            // 返回默认分析，不影响主流程
            com.autodroid.teachitback.model.ProgressAnalysis(
                overallProgress = 50,
                conceptMastery = emptyMap(),
                learningVelocity = 0.0,
                knowledgeGaps = emptyList(),
                recommendedNextSteps = emptyList()
            )
        }
        Log.d("MessageRepository", "学习进度分析完成: ${progressAnalysis.overallProgress}%")

        // 4. 构建上下文并使用AIServiceRouter获取AI回复
        val context = buildContext(conversationHistory, progressAnalysis)
        val aiResponse: AIServiceResponse = try {
            aiRouter.routeByCapability(
                capabilityCheck = { it.supportBasicChat },
                operation = { service -> service.sendMessage(userMessage, context) }
            )
        } catch (e: Exception) {
            Log.e("MessageRepository", "AI回复生成失败: ${e.message}", e)
            // 返回错误响应，不阻塞主流程
            AIServiceResponse(
                content = "抱歉，AI服务暂时不可用，请稍后重试。",
                processInfo = com.autodroid.teachitback.model.AIProcessInfo(
                    serviceId = "error",
                    serviceName = "错误服务",
                    modelUsed = "none",
                    processingTime = 0
                )
            )
        }
        Log.d("MessageRepository", "AI回复生成完成: ${aiResponse.processInfo.serviceName}")

        // 5. 保存AI回复到本地数据库（包含AIProcessInfo）
        val aiMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            content = aiResponse.content,
            senderType = "AI",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
            // TODO: 任务25完成后添加 aiProcessInfo = aiResponse.processInfo
        )
        messageDao.insertMessage(aiMessage)
        Log.d("MessageRepository", "已保存AI消息: ${aiMessage.id}")

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
            append("【学习进度分析】\n")
            append("当前进度: ${progressAnalysis.overallProgress}%\n")
            if (progressAnalysis.conceptMastery.isNotEmpty()) {
                append("概念掌握: ${progressAnalysis.conceptMastery}\n")
            }
            if (progressAnalysis.knowledgeGaps.isNotEmpty()) {
                append("知识缺口: ${progressAnalysis.knowledgeGaps.joinToString(", ")}\n")
            }
            if (progressAnalysis.recommendedNextSteps.isNotEmpty()) {
                append("推荐行动: ${progressAnalysis.recommendedNextSteps.joinToString(", ")}\n")
            }
            append("\n【对话历史】\n")
            append("最近${conversationHistory.size}条对话\n")
            append("请基于以上信息，给出个性化的学习指导和解答。")
        }
    }

    // ===== 数据库操作 =====

    suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)

    suspend fun updateMessage(message: MessageEntity) = messageDao.updateMessage(message)

    suspend fun deleteMessagesByTopic(topicId: String) =
        messageDao.deleteMessagesByTopic(topicId)
}
