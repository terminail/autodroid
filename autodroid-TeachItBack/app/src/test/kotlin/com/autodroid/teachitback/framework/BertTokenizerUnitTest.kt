

package com.autodroid.teachitback.framework

import com.autodroid.teachitback.data.KnowledgeBaseEntry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BertTokenizerTest {

    @Before
    fun setUp() {
        BertTokenizer.setLoggingEnabled(false)
    }

    @Test
    fun `test vocabulary loading from string`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
[MASK]
a
b
c
你
好
抛
物
线
解
释
一
下
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)

        assertEquals(17, tokenizer.vocabSize)
        assertEquals(0, BertTokenizer.PAD_TOKEN_ID)
        assertEquals(100, BertTokenizer.UNK_TOKEN_ID)
        assertEquals(101, BertTokenizer.CLS_TOKEN_ID)
        assertEquals(102, BertTokenizer.SEP_TOKEN_ID)
    }

    @Test
    fun `test tokenization of Chinese text`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
[MASK]
a
b
你
好
抛
物
线
解
释
一
下
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)
        val tokens = tokenizer.tokenize("你好抛物线")

        assertTrue("Should contain CLS token", tokens.contains(BertTokenizer.CLS_TOKEN_ID))
        assertTrue("Should contain SEP token", tokens.contains(BertTokenizer.SEP_TOKEN_ID))
        assertTrue("Should contain '你' token at index 7", tokens.contains(7))
        assertTrue("Should contain '好' token at index 8", tokens.contains(8))
    }

    @Test
    fun `test encode function`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
[MASK]
a
b
你
好
抛
物
线
解
释
一
下
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)
        val result = tokenizer.encode("你好", "抛物线")

        assertNotNull("Encode result should not be null", result)
        assertTrue("Input IDs should not be empty", result.inputIds.isNotEmpty())
        assertTrue("Attention mask should not be empty", result.attentionMask.isNotEmpty())
        assertEquals("Input IDs and attention mask should have same size",
            result.inputIds.size, result.attentionMask.size)
    }

    @Test
    fun `test unknown word handling`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
[MASK]
a
b
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)
        val tokens = tokenizer.tokenize("xyzunknown")

        assertTrue("Should contain UNK token for unknown words",
            tokens.contains(BertTokenizer.UNK_TOKEN_ID))
    }

    @Test
    fun `test max length truncation`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
[MASK]
a
b
c
d
e
f
g
h
i
j
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)
        val longText = "a b c d e f g h i j k l m n o p q r s t u v w x y z"
        val result = tokenizer.encode(longText, null, maxLength = 10)

        assertNotNull("Encode result should not be null", result)
        assertTrue("Result should be truncated to max length", result.inputIds.size <= 10)
    }

    @Test
    fun `test vocabulary map access`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
hello
world
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)
        val vocab = tokenizer.vocabulary

        assertEquals(6, vocab.size)
        assertEquals(4, vocab["hello"])
        assertEquals(5, vocab["world"])
    }
}

class CosineSimilarityTest {

    @Test
    fun `test identical vectors have similarity of 1`() {
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(1.0f, 2.0f, 3.0f)

        val similarity = cosineSimilarity(vec1, vec2)

        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test
    fun `test opposite vectors have similarity of -1`() {
        val vec1 = listOf(1.0f, 0.0f, 0.0f)
        val vec2 = listOf(-1.0f, 0.0f, 0.0f)

        val similarity = cosineSimilarity(vec1, vec2)

        assertEquals(-1.0f, similarity, 0.0001f)
    }

    @Test
    fun `test orthogonal vectors have similarity of 0`() {
        val vec1 = listOf(1.0f, 0.0f)
        val vec2 = listOf(0.0f, 1.0f)

        val similarity = cosineSimilarity(vec1, vec2)

        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun `test similarity calculation with actual values`() {
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(2.0f, 4.0f, 6.0f)

        val similarity = cosineSimilarity(vec1, vec2)

        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test different sized vectors throw exception`() {
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(1.0f, 2.0f)

        cosineSimilarity(vec1, vec2)
    }

    @Test
    fun `test empty vectors return 0`() {
        val vec1 = emptyList<Float>()
        val vec2 = emptyList<Float>()

        val similarity = cosineSimilarity(vec1, vec2)

        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun `test single element vectors`() {
        val vec1 = listOf(5.0f)
        val vec2 = listOf(3.0f)

        val similarity = cosineSimilarity(vec1, vec2)

        assertEquals(1.0f, similarity, 0.0001f)
    }
}

class SemanticMatchingTest {

    @Test
    fun `test calculateSimpleSimilarity with identical strings`() {
        val similarity = calculateSimpleSimilarity("你好", "你好")

        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test
    fun `test calculateSimpleSimilarity with no overlap`() {
        val similarity = calculateSimpleSimilarity("你好", "再见")

        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun `test tokenizeText for Chinese`() {
        val result = tokenizeText("你好世界")
        val result2 = tokenizeText("你好")

        assertEquals(4, result.size)
        assertEquals(2, result2.size)
    }

    @Test
    fun `test calculateSimpleSimilarity with partial overlap`() {
        val similarity = calculateSimpleSimilarity("你好世界", "你好")

        assertTrue("Similarity should be greater than 0", similarity > 0.0f)
    }

    @Test
    fun `test calculateSimpleSimilarity with empty query`() {
        val similarity = calculateSimpleSimilarity("", "你好世界")

        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun `test calculateSimpleSimilarity with empty text`() {
        val similarity = calculateSimpleSimilarity("你好世界", "")

        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun `test tokenizeText with English words`() {
        val result = tokenizeText("hello world")

        assertEquals(2, result.size)
        assertTrue(result.contains("hello"))
        assertTrue(result.contains("world"))
    }
}

class KnowledgeBaseEntryTest {

    @Test
    fun `test knowledge base entry creation`() {
        val entry = KnowledgeBaseEntry(
            id = "1",
            question = "什么是抛物线",
            answer = "抛物线是...",
            category = "高中数学",
            tags = listOf("抛物线", "二次函数")
        )

        assertEquals("1", entry.id)
        assertEquals("什么是抛物线", entry.question)
        assertEquals("抛物线是...", entry.answer)
        assertEquals("高中数学", entry.category)
    }

    @Test
    fun `test knowledge base entry with empty tags`() {
        val entry = KnowledgeBaseEntry(
            id = "2",
            question = "问题",
            answer = "答案",
            category = "数学",
            tags = emptyList()
        )

        assertTrue("Tags should be empty", entry.tags.isEmpty())
    }

    @Test
    fun `test knowledge base entry equality`() {
        val entry1 = KnowledgeBaseEntry(
            id = "1",
            question = "问题",
            answer = "答案",
            category = "数学",
            tags = emptyList()
        )
        val entry2 = KnowledgeBaseEntry(
            id = "1",
            question = "问题",
            answer = "答案",
            category = "数学",
            tags = emptyList()
        )

        assertTrue("Entries should be equal", entry1 == entry2)
    }
}

class TextProcessingTest {

    @Test
    fun `test Chinese character detection`() {
        val chineseChars = listOf('你', '好', '抛', '物', '线', '中', '文')
        val nonChineseChars = listOf('a', 'b', '1', ' ', '.', ',', '!')

        chineseChars.forEach { char ->
            assertTrue("$char should be detected as Chinese", isChinese(char))
        }

        nonChineseChars.forEach { char ->
            assertFalse("$char should not be detected as Chinese", isChinese(char))
        }
    }

    @Test
    fun `test Chinese character range detection`() {
        assertTrue("U+4E00 should be Chinese", isChinese('\u4E00'))
        assertTrue("U+9FFF should be Chinese", isChinese('\u9FFF'))
        assertTrue("U+3400 should be Chinese", isChinese('\u3400'))
        assertTrue("U+4DBF should be Chinese", isChinese('\u4DBF'))
    }

    @Test
    fun `test BertTokenizer isChinese method`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)

        assertTrue("Tokenizer should detect '你' as Chinese", tokenizer.isChinese('你'))
        assertFalse("Tokenizer should not detect 'a' as Chinese", tokenizer.isChinese('a'))
    }

    @Test
    fun `test punctuation detection`() {
        val vocabContent = """
[PAD]
[UNK]
[CLS]
[SEP]
""".trimIndent()

        val tokenizer = BertTokenizer.fromVocabulary(vocabContent)

        assertTrue("Tokenizer should detect ',' as punctuation", tokenizer.isPunctuation(','))
        assertTrue("Tokenizer should detect '!' as punctuation", tokenizer.isPunctuation('!'))
        assertFalse("Tokenizer should not detect 'a' as punctuation", tokenizer.isPunctuation('a'))
    }
}

class EncodeResultTest {

    @Test
    fun `test encode result creation`() {
        val result = EncodeResult(
            inputIds = listOf(101, 7599, 2023, 102),
            attentionMask = listOf(1, 1, 1, 1),
            tokenTypeIds = listOf(0, 0, 0, 0)
        )

        assertEquals(4, result.inputIds.size)
        assertEquals(4, result.attentionMask.size)
        assertEquals(4, result.tokenTypeIds.size)
    }

    @Test
    fun `test encode result toString`() {
        val result = EncodeResult(
            inputIds = listOf(101, 7599, 2023, 102),
            attentionMask = listOf(1, 1, 1, 1),
            tokenTypeIds = listOf(0, 0, 0, 0)
        )

        val str = result.toString()

        assertTrue("toString should contain inputIds size", str.contains("inputIds=4"))
        assertTrue("toString should contain attentionMask size", str.contains("attentionMask=4"))
    }
}
