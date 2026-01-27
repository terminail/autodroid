package com.autodroid.teachitback.data

data class KnowledgeBaseEntry(
    val id: String,
    val question: String,
    val answer: String,
    val category: String = "",
    val tags: List<String> = emptyList()
)

data class SemanticMatchResult(
    val entry: KnowledgeBaseEntry,
    val similarity: Double,
    val rank: Int
)
