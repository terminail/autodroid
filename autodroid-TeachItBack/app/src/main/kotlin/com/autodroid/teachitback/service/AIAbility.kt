package com.autodroid.teachitback.service

/**
 * AI能力枚举 - 消除隐藏的字符串代码
 */
enum class AIAbility {
    BASIC_CHAT,
    FILE_PROCESSING,
    MIND_MAP_GENERATION,
    LEARNING_ANALYSIS,
    SOCRATIC_QUESTIONING,
    ANSWER_EVALUATION,
    DOCUMENT_PARSING,
    CONCEPT_EXTRACTION,
    KNOWLEDGE_GRAPH,
    LONG_TEXT,
    MULTIMODAL,
    EDUCATION,
    CODE_GENERATION,
    MATH,
    CREATIVE_WRITING,
    IMAGE_ANALYSIS,
    IMAGE_GENERATION,
    AUDIO_PROCESSING,
    VIDEO_ANALYSIS,

    // RAG能力 - 基于腾讯云RAG链路解耦能力
    RAG_DOCUMENT_PARSING,        // 文档解析
    RAG_TEXT_SPLITTING,          // 文本拆分
    RAG_EMBEDDING,               // 向量嵌入
    RAG_MULTI_TURN_REWRITING,    // 多轮改写
    RAG_RE_RANKING,              // 重排序
    RAG_RETRIEVAL,               // 检索
    RAG_GENERATION               // 生成
}

