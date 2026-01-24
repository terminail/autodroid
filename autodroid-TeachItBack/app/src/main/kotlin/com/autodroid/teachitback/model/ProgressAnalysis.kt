package com.autodroid.teachitback.model

/**
 * 学习进度分析结果
 */
data class ProgressAnalysis(
    /**
     * 整体学习进度 (0-100)
     */
    val overallProgress: Int,
    /**
     * 各概念的掌握程度 (概念名称 -> 掌握度 0-100)
     */
    val conceptMastery: Map<String, Int>,
    /**
     * 学习速度指标
     */
    val learningVelocity: Double,
    /**
     * 识别出的知识缺口
     */
    val knowledgeGaps: List<String>,
    /**
     * 推荐的下一步学习行动
     */
    val recommendedNextSteps: List<String>
)

/**
 * 答案评估结果
 */
data class AnswerEvaluation(
    /**
     * 答案是否正确
     */
    val isCorrect: Boolean,
    /**
     * 置信度 (0.0-1.0)
     */
    val confidence: Double,
    /**
     * 反馈信息
     */
    val feedback: String,
    /**
     * 改进建议（可选）
     */
    val suggestedImprovement: String? = null
)

/**
 * 苏格拉底问题类型
 */
enum class QuestionType {
    /** 概念解释 */
    CONCEPT_EXPLANATION,
    /** 需要举例 */
    EXAMPLE_REQUIRED,
    /** 对比分析 */
    COMPARISON_ANALYSIS,
    /** 实际应用 */
    PRACTICAL_APPLICATION,
    /** 批判性思维 */
    CRITICAL_THINKING
}

/**
 * 苏格拉底问题
 */
data class SocraticQuestion(
    /**
     * 问题ID
     */
    val id: String,
    /**
     * 问题内容
     */
    val question: String,
    /**
     * 问题类型
     */
    val type: QuestionType,
    /**
     * 难度等级 (1-5)
     */
    val difficulty: Int,
    /**
     * 期望答案的提示
     */
    val expectedAnswerHints: List<String>
)

/**
 * 文档分析结果
 */
data class DocumentAnalysis(
    /**
     * 文件名
     */
    val fileName: String,
    /**
     * 文件类型
     */
    val fileType: String,
    /**
     * 文档摘要
     */
    val summary: String,
    /**
     * 关键要点
     */
    val keyPoints: List<String>,
    /**
     * 提取的文本内容
     */
    val extractedText: String
)

/**
 * 知识概念
 */
data class Concept(
    /**
     * 概念ID
     */
    val id: String,
    /**
     * 概念名称
     */
    val name: String,
    /**
     * 概念定义
     */
    val definition: String,
    /**
     * 相关概念列表
     */
    val relatedConcepts: List<String>
)

/**
 * 知识图谱
 */
data class KnowledgeGraph(
    /**
     * 概念节点列表
     */
    val nodes: List<Concept>,
    /**
     * 概念间的关系边
     */
    val edges: List<Edge>
) {
    /**
     * 知识图谱中的边
     */
    data class Edge(
        /**
         * 起始概念ID
         */
        val from: String,
        /**
         * 目标概念ID
         */
        val to: String,
        /**
         * 关系类型
         */
        val relationship: String
    )
}
