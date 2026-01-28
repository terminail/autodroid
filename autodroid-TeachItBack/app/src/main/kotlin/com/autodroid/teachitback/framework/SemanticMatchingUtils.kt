package com.autodroid.teachitback.framework

import java.util.StringJoiner

data class EncodeResult(
    val inputIds: List<Int>,
    val attentionMask: List<Int>,
    val tokenTypeIds: List<Int>
) {
    override fun toString(): String {
        return StringJoiner(", ", "EncodeResult(", ")")
            .add("inputIds=${inputIds.size}")
            .add("attentionMask=${attentionMask.size}")
            .add("tokenTypeIds=${tokenTypeIds.size}")
            .toString()
    }
}

fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Float {
    if (vec1.size != vec2.size) {
        throw IllegalArgumentException("Vectors must have the same size: ${vec1.size} vs ${vec2.size}")
    }

    if (vec1.isEmpty()) {
        return 0.0f
    }

    var dotProduct = 0.0f
    var norm1 = 0.0f
    var norm2 = 0.0f

    for (i in vec1.indices) {
        dotProduct += vec1[i] * vec2[i]
        norm1 += vec1[i] * vec1[i]
        norm2 += vec2[i] * vec2[i]
    }

    val denominator = kotlin.math.sqrt(norm1.toDouble()) * kotlin.math.sqrt(norm2.toDouble())

    return if (denominator > 0) {
        (dotProduct / denominator).toFloat()
    } else {
        0.0f
    }
}

fun calculateSimpleSimilarity(query: String, text: String): Float {
    val queryWords = tokenizeText(query.lowercase())
    val textWords = tokenizeText(text.lowercase())

    if (queryWords.isEmpty() || textWords.isEmpty()) {
        return 0.0f
    }

    var multiCharMatches = 0
    var queryMultiCharCount = queryWords.count { it.length > 1 }
    var keywordBonus = 0

    for (queryWord in queryWords) {
        if (queryWord.length > 1) {
            for (textWord in textWords) {
                if (textWord.contains(queryWord) || queryWord.contains(textWord)) {
                    multiCharMatches++
                    if (queryWord.length >= 2) {
                        keywordBonus++
                    }
                    break
                }
            }
        }
    }

    if (queryMultiCharCount > 0) {
        return multiCharMatches.toFloat() / queryMultiCharCount
    }

    var singleCharMatches = 0
    for (queryWord in queryWords) {
        if (queryWord.length == 1 && isChinese(queryWord[0])) {
            for (textWord in textWords) {
                if (textWord.contains(queryWord[0])) {
                    singleCharMatches++
                    break
                }
            }
        }
    }

    val baseSimilarity = if (queryWords.isNotEmpty()) singleCharMatches.toFloat() / queryWords.size else 0.0f
    val keywordScore = if (queryWords.isNotEmpty()) keywordBonus.toFloat() / queryWords.size else 0.0f

    return (baseSimilarity + keywordScore).coerceAtMost(1.0f)
}

fun tokenizeText(text: String): Set<String> {
    if (text.isBlank()) {
        return emptySet()
    }

    val spaceSplit = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (spaceSplit.isNotEmpty() && spaceSplit.size > 1) {
        return spaceSplit.toSet()
    }

    val result = mutableSetOf<String>()
    for (char in text) {
        if (!Character.isWhitespace(char)) {
            result.add(char.toString())
        }
    }
    return result
}

fun isChinese(char: Char): Boolean {
    val codePoint = char.code
    return codePoint in 0x4E00..0x9FFF ||
           codePoint in 0x3400..0x4DBF ||
           codePoint in 0x20000..0x2A6DF ||
           codePoint in 0x2A700..0x2B73F ||
           codePoint in 0x2B740..0x2B81F ||
           codePoint in 0x2B820..0x2CEAF ||
           codePoint in 0xF900..0xFAFF ||
           codePoint in 0x2F800..0x2FA1F
}
