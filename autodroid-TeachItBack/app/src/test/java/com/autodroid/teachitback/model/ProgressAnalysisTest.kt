package com.autodroid.teachitback.model

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressAnalysisTest {

    @Test
    fun `ProgressAnalysis data class should be properly defined`() = runTest {
        val analysis = ProgressAnalysis(
            overallProgress = 50,
            conceptMastery = mapOf("概念1" to 80, "概念2" to 60),
            learningVelocity = 2.5,
            knowledgeGaps = listOf("缺口1"),
            recommendedNextSteps = listOf("步骤1")
        )

        assertTrue(analysis.overallProgress in 0..100)
        assertTrue(analysis.learningVelocity > 0)
        assertNotNull(analysis.conceptMastery)
        assertEquals(2, analysis.conceptMastery.size)
        assertEquals(80, analysis.conceptMastery["概念1"])
    }

    @Test
    fun `AnswerEvaluation data class should be properly defined`() = runTest {
        val evaluation = AnswerEvaluation(
            isCorrect = true,
            confidence = 0.9,
            feedback = "回答很好",
            suggestedImprovement = null
        )

        assertTrue(evaluation.isCorrect)
        assertTrue(evaluation.confidence in 0.0..1.0)
        assertEquals(0.9, evaluation.confidence, 0.01)
        assertNotNull(evaluation.feedback)
    }

    @Test
    fun `QuestionType enum should have correct values`() {
        val expectedTypes = listOf(
            QuestionType.CONCEPT_EXPLANATION,
            QuestionType.EXAMPLE_REQUIRED,
            QuestionType.COMPARISON_ANALYSIS,
            QuestionType.PRACTICAL_APPLICATION,
            QuestionType.CRITICAL_THINKING
        )

        assertEquals(5, expectedTypes.size)
    }

    @Test
    fun `SocraticQuestion data class should be properly defined`() = runTest {
        val question = SocraticQuestion(
            id = "q1",
            question = "什么是机器学习？",
            type = QuestionType.CONCEPT_EXPLANATION,
            difficulty = 3,
            expectedAnswerHints = listOf("提示1", "提示2")
        )

        assertEquals("q1", question.id)
        assertEquals("什么是机器学习？", question.question)
        assertEquals(QuestionType.CONCEPT_EXPLANATION, question.type)
        assertEquals(3, question.difficulty)
        assertEquals(2, question.expectedAnswerHints.size)
    }

    @Test
    fun `DocumentAnalysis data class should be properly defined`() = runTest {
        val analysis = DocumentAnalysis(
            fileName = "test.pdf",
            fileType = "PDF",
            summary = "文档摘要",
            keyPoints = listOf("要点1", "要点2"),
            extractedText = "提取的文本内容"
        )

        assertEquals("test.pdf", analysis.fileName)
        assertEquals("PDF", analysis.fileType)
        assertEquals("文档摘要", analysis.summary)
        assertEquals(2, analysis.keyPoints.size)
    }

    @Test
    fun `Concept data class should be properly defined`() = runTest {
        val concept = Concept(
            id = "c1",
            name = "机器学习",
            definition = "定义",
            relatedConcepts = listOf("AI", "数据科学")
        )

        assertEquals("c1", concept.id)
        assertEquals("机器学习", concept.name)
        assertEquals("定义", concept.definition)
        assertEquals(2, concept.relatedConcepts.size)
    }

    @Test
    fun `KnowledgeGraph data class should be properly defined`() = runTest {
        val graph = KnowledgeGraph(
            nodes = listOf(
                Concept("c1", "概念1", "定义1", emptyList()),
                Concept("c2", "概念2", "定义2", emptyList())
            ),
            edges = listOf(
                KnowledgeGraph.Edge("c1", "c2", "相关")
            )
        )

        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
        assertEquals("c1", graph.edges[0].from)
        assertEquals("c2", graph.edges[0].to)
        assertEquals("相关", graph.edges[0].relationship)
    }
}
