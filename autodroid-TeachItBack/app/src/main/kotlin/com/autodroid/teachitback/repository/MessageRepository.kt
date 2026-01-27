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
        return sendMessageAndGetReplyWithPreference(topicId, userContent, null)
    }

    /**
     * 发送消息并获取AI回复（带服务偏好）
     * Local-First策略：
     * 1. 保存用户消息到本地数据库
     * 2. 获取对话历史（用于AI分析）
     * 3. 使用AIServiceRouter智能路由调用AI服务分析进度
     * 4. 构建上下文并使用AIServiceRouter获取AI回复（优先使用suggestedService）
     * 5. 保存AI回复到本地数据库（包含AIProcessInfo）
     */
    suspend fun sendMessageAndGetReplyWithPreference(
        topicId: String,
        userContent: String,
        suggestedService: String?
    ): MessageEntity? {
        Log.d("MessageRepository", "开始处理消息: topicId=$topicId, userContent=$userContent, suggestedService=$suggestedService")
        
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
        Log.d("MessageRepository", "已保存用户消息: ${userMessage.id}, 内容: $userContent")

        // 2. 获取对话历史（用于AI分析）
        val conversationHistory = messageDao.getMessagesByTopicSync(topicId)
        Log.d("MessageRepository", "获取对话历史: ${conversationHistory.size} 条消息")
        if (conversationHistory.isNotEmpty()) {
            Log.d("MessageRepository", "最近对话摘要: ${conversationHistory.take(3).joinToString(", ") { it.content.take(20) + "..." }}")
        }

        // 3. 使用AIServiceRouter智能路由调用AI服务分析进度
        Log.d("MessageRepository", "开始学习进度分析...")
        val progressAnalysis = try {
            val analysis = aiRouter.routeByCapability(
                capabilityCheck = { it.supportLearningAnalysis },
                operation = { service -> 
                    Log.d("MessageRepository", "使用服务进行进度分析: ${service.config.displayName}")
                    service.analyzeLearningProgress(conversationHistory) 
                }
            )
            Log.d("MessageRepository", "学习进度分析成功: 总体进度=${analysis.overallProgress}%, 概念掌握=${analysis.conceptMastery.size}个")
            analysis
        } catch (e: Exception) {
            Log.e("MessageRepository", "学习进度分析失败: ${e.message}", e)
            // 返回默认分析，不影响主流程
            val defaultAnalysis = com.autodroid.teachitback.model.ProgressAnalysis(
                overallProgress = 50,
                conceptMastery = emptyMap(),
                learningVelocity = 0.0,
                knowledgeGaps = emptyList(),
                recommendedNextSteps = emptyList()
            )
            Log.w("MessageRepository", "使用默认进度分析: ${defaultAnalysis.overallProgress}%")
            defaultAnalysis
        }

        // 4. 构建上下文并使用AIServiceRouter获取AI回复
        Log.d("MessageRepository", "开始生成AI回复...")
        val context = buildContext(conversationHistory, progressAnalysis)
        Log.d("MessageRepository", "构建上下文完成，上下文长度: ${context.length}")
        
        val aiResponse: com.autodroid.teachitback.model.AIServiceResponse = try {
            Log.d("MessageRepository", "检查服务推荐: suggestedService=$suggestedService")
            if (suggestedService != null) {
                // 如果有推荐的服务，优先使用该服务
                Log.d("MessageRepository", "使用推荐服务: $suggestedService")
                aiRouter.routeWithPreference(
                    capabilityCheck = { it.supportBasicChat },
                    preferredServiceId = suggestedService,
                    operation = { service -> 
                        Log.d("MessageRepository", "调用服务生成回复: ${service.config.displayName}")
                        service.sendMessage(userMessage, context) 
                    }
                )
            } else {
                // 否则使用智能路由
                Log.d("MessageRepository", "使用智能路由选择服务")
                aiRouter.routeByCapability(
                    capabilityCheck = { it.supportBasicChat },
                    operation = { service -> 
                        Log.d("MessageRepository", "调用服务生成回复: ${service.config.displayName}")
                        service.sendMessage(userMessage, context) 
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "AI回复生成失败: ${e.message}", e)
            // 返回错误响应，不阻塞主流程
            val errorResponse = com.autodroid.teachitback.model.AIServiceResponse(
                content = "抱歉，AI服务暂时不可用，请稍后重试。",
                processInfo = com.autodroid.teachitback.model.AIProcessInfo(
                    serviceId = "error",
                    serviceName = "错误服务",
                    modelUsed = "none",
                    processingTime = 0
                )
            )
            Log.w("MessageRepository", "返回错误响应: ${errorResponse.content}")
            errorResponse
        }
        
        Log.d("MessageRepository", "AI回复生成完成: 服务=${aiResponse.processInfo.serviceName}, 模型=${aiResponse.processInfo.modelUsed}, 处理时间=${aiResponse.processInfo.processingTime}ms")
        Log.d("MessageRepository", "AI回复内容: ${aiResponse.content.take(100)}...")

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

        Log.d("MessageRepository", "消息处理流程完成")
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
