package com.autodroid.teachitback.impl

import android.content.Context
import com.autodroid.teachitback.api.TencentCloudAIService
import com.autodroid.teachitback.model.*

/**
 * 腾讯云AI服务实现
 * 用于测试目的的模拟实现
 */
class TencentCloudAIServiceImpl(
    private val context: Context,
    private val apiKey: String,
    private val secretId: String,
    private val testMode: Boolean = false
) : TencentCloudAIService {

    override suspend fun sendMessage(messages: List<MessageEntity>, context: String): String {
        if (testMode) {
            return "测试回复：${context}"
        }
        return "模拟回复"
    }

    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        return MindMapEntity(
            id = "mindmap-$topicId",
            topicId = topicId,
            title = learningGoal,
            structure = "{}"
        )
    }

    override suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis {
        return ProgressAnalysis(
            overallProgress = 50,
            conceptMastery = mapOf("概念1" to 80, "概念2" to 60),
            learningVelocity = 0.5,
            knowledgeGaps = listOf("知识点1", "知识点2"),
            recommendedNextSteps = listOf("步骤1", "步骤2")
        )
    }

    override suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String> {
        return listOf(
            "关于${topic}的问题1",
            "关于${topic}的问题2",
            "关于${topic}的问题3"
        )
    }

    override suspend fun evaluateAnswer(userAnswer: String, correctAnswer: String): AnswerEvaluation {
        return AnswerEvaluation(
            isCorrect = true,
            confidence = 0.8,
            feedback = "回答正确"
        )
    }

    override suspend fun parseDocument(fileContent: String, fileType: String): DocumentAnalysis {
        return DocumentAnalysis(
            fileName = "test-file.$fileType",
            fileType = fileType,
            summary = "文档摘要",
            keyPoints = listOf("要点1", "要点2"),
            extractedText = fileContent
        )
    }

    override suspend fun extractKeyConcepts(content: String): List<Concept> {
        return listOf(
            Concept("c1", "概念1", "定义1", listOf("c2")),
            Concept("c2", "概念2", "定义2", listOf("c1"))
        )
    }

    override suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph {
        return KnowledgeGraph(
            nodes = concepts,
            edges = concepts.flatMap { c ->
                c.relatedConcepts.map { related ->
                    KnowledgeGraph.Edge(c.id, related, "related")
                }
            }
        )
    }
}
