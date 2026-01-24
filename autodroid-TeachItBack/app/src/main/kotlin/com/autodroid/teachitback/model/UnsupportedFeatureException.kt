package com.autodroid.teachitback.model

/**
 * 不支持的功能异常
 * 当AI服务不支持某个特定功能时抛出
 */
class UnsupportedFeatureException(
    feature: String,
    serviceName: String
) : Exception("AI服务 $serviceName 不支持功能: $feature") {

    companion object {
        fun mindMapGeneration(serviceName: String) =
            UnsupportedFeatureException("思维导图生成", serviceName)

        fun learningAnalysis(serviceName: String) =
            UnsupportedFeatureException("学习进度分析", serviceName)

        fun socraticQuestioning(serviceName: String) =
            UnsupportedFeatureException("苏格拉底式提问", serviceName)

        fun answerEvaluation(serviceName: String) =
            UnsupportedFeatureException("答案评估", serviceName)

        fun documentParsing(serviceName: String) =
            UnsupportedFeatureException("文档解析", serviceName)

        fun conceptExtraction(serviceName: String) =
            UnsupportedFeatureException("概念提取", serviceName)

        fun knowledgeGraph(serviceName: String) =
            UnsupportedFeatureException("知识图谱", serviceName)

        fun configurationUpdate(serviceName: String) =
            UnsupportedFeatureException("配置更新", serviceName)
    }
}
