package com.autodroid.teachitback.framework

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class BertEmbeddingTest {

    private val testVocab = """
[PAD]
[UNK]
[CLS]
[SEP]
[MASK]
你
好
世
界
抛
物
线
解
释
一
下
什
么
是
圆
形
""".trimIndent()

    @Test
    fun `test embedding generation for identical phrases`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)

        val embedding1 = createMockEmbedding(tokenizer, "你好")
        val embedding2 = createMockEmbedding(tokenizer, "你好")

        val similarity = cosineSimilarity(embedding1, embedding2)

        assertEquals("Identical phrases should have similarity 1.0", 1.0f, similarity, 0.01f)
    }

    @Test
    fun `test embedding generation for different phrases`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)

        val embedding1 = createMockEmbedding(tokenizer, "你好")
        val embedding2 = createMockEmbedding(tokenizer, "圆形")

        val similarity = cosineSimilarity(embedding1, embedding2)

        assertTrue("Different phrases should have similarity < 1.0", similarity < 1.0f)
    }

    @Test
    fun `test embedding contains special tokens`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)
        val tokens = tokenizer.tokenize("你好")

        assertTrue("Should contain CLS token", tokens.contains(BertTokenizer.CLS_TOKEN_ID))
        assertTrue("Should contain SEP token", tokens.contains(BertTokenizer.SEP_TOKEN_ID))
    }

    @Test
    fun `test cosine similarity calculation`() {
        val vec1 = listOf(1.0f, 0.0f, 0.0f)
        val vec2 = listOf(1.0f, 0.0f, 0.0f)
        val vec3 = listOf(0.0f, 1.0f, 0.0f)

        val sim1 = cosineSimilarity(vec1, vec2)
        val sim2 = cosineSimilarity(vec1, vec3)

        assertEquals("Identical vectors should have similarity 1.0", 1.0f, sim1, 0.01f)
        assertEquals("Orthogonal vectors should have similarity 0.0", 0.0f, sim2, 0.01f)
    }

    @Test
    fun `test cosine similarity with empty vectors`() {
        val vec1 = emptyList<Float>()
        val vec2 = emptyList<Float>()

        val similarity = cosineSimilarity(vec1, vec2)

        assertEquals("Empty vectors should have similarity 0.0", 0.0f, similarity, 0.01f)
    }

    @Test
    fun `test encode function produces valid structure`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)
        val result = tokenizer.encode("你好", "世界")

        assertNotNull("Encode result should not be null", result)
        assertTrue("Input IDs should not be empty", result.inputIds.isNotEmpty())
        assertTrue("Attention mask should not be empty", result.attentionMask.isNotEmpty())
        assertEquals("Input IDs and attention mask should have same size",
            result.inputIds.size, result.attentionMask.size)
        assertEquals("Token type IDs should have same size",
            result.inputIds.size, result.tokenTypeIds.size)
    }

    @Test
    fun `test encode with single text`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)
        val result = tokenizer.encode("你好", null)

        assertNotNull("Encode result should not be null", result)
        assertTrue("Should contain CLS", result.inputIds.contains(BertTokenizer.CLS_TOKEN_ID))
        assertTrue("Should contain SEP", result.inputIds.contains(BertTokenizer.SEP_TOKEN_ID))
    }

    @Test
    fun `test vocabulary loading and size`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)

        assertEquals("Vocabulary should have correct size", 21, tokenizer.vocabSize)
    }

    @Test
    fun `test Chinese character tokenization`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)
        val tokens = tokenizer.tokenize("你好世界")

        assertTrue("Should contain '你' token", tokens.any { it == 5 })
        assertTrue("Should contain '好' token", tokens.any { it == 6 })
        assertTrue("Should contain '世' token", tokens.any { it == 7 })
        assertTrue("Should contain '界' token", tokens.any { it == 8 })
    }

    @Test
    fun `test unknown word handling`() {
        val smallVocab = """
[PAD]
[UNK]
[CLS]
[SEP]
[MASK]
a
b
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(smallVocab)
        val tokens = tokenizer.tokenize("xyzunknown")

        assertTrue("Should contain UNK token for unknown words",
            tokens.contains(BertTokenizer.UNK_TOKEN_ID))
    }

    @Test
    fun `test sentence pair encoding`() {
        val tokenizer = BertTokenizer.fromVocabulary(testVocab)
        val result = tokenizer.encode("你好", "世界")

        val sepCount = result.inputIds.count { it == BertTokenizer.SEP_TOKEN_ID }
        assertEquals("Should have exactly 2 SEP tokens for sentence pair", 2, sepCount)
    }

    private fun createMockEmbedding(tokenizer: BertTokenizer, text: String): List<Float> {
        val tokens = tokenizer.tokenize(text)
        val embedding = mutableListOf<Float>()

        for (tokenId in tokens) {
            val normalizedId = if (tokenId < 100) tokenId.toFloat() / 100f else 0.5f
            embedding.add(normalizedId)
            embedding.add(1.0f - normalizedId)
        }

        while (embedding.size < 10) {
            embedding.add(0.0f)
        }

        return embedding.take(10)
    }
}
