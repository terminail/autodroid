package com.autodroid.teachitback.api

import com.autodroid.teachitback.model.*

interface TencentCloudAIService {
    suspend fun sendMessage(messages: List<MessageEntity>, context: String): String

    suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity?
    suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis
    suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String>
    suspend fun evaluateAnswer(userAnswer: String, correctAnswer: String): AnswerEvaluation

    suspend fun parseDocument(fileContent: String, fileType: String): DocumentAnalysis
    suspend fun extractKeyConcepts(content: String): List<Concept>
    suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph
}
