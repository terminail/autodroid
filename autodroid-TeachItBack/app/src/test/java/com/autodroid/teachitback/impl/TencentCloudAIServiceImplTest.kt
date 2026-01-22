package com.autodroid.teachitback.impl

import android.content.Context
import com.autodroid.teachitback.api.TencentCloudAIService
import com.autodroid.teachitback.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class TencentCloudAIServiceImplTest {

    @Mock
    private lateinit var context: Context

    private lateinit var service: TencentCloudAIService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = TencentCloudAIServiceImpl(
            context = context,
            apiKey = "test-api-key",
            secretId = "test-secret-id",
            testMode = true
        )
    }

    @Test
    fun `TencentCloudAIServiceImpl should implement TencentCloudAIService interface`() {
        assertTrue("service should implement TencentCloudAIService", service is TencentCloudAIService)
    }

    @Test
    fun `sendMessage in test mode should return mock response`() = runTest {
        val messages = emptyList<com.autodroid.teachitback.model.MessageEntity>()
        val context = "测试上下文"

        val response = service.sendMessage(messages, context)

        assertNotNull("Response should not be null", response)
        assertTrue("Response should contain mock text", response.contains("测试"))
    }

    @Test
    fun `generateMindMap should return mock MindMapEntity`() = runTest {
        val topicId = "test-topic"
        val learningGoal = "测试学习目标"

        val mindMap = service.generateMindMap(topicId, learningGoal)

        assertNotNull("MindMap should not be null", mindMap)
        assertTrue("MindMap ID should contain topicId", mindMap!!.id.contains(topicId))
        assertEquals("MindMap title should match learningGoal", learningGoal, mindMap.title)
    }

    @Test
    fun `analyzeLearningProgress should return mock ProgressAnalysis`() = runTest {
        val conversationHistory = emptyList<MessageEntity>()

        val progress = service.analyzeLearningProgress(conversationHistory)

        assertNotNull("ProgressAnalysis should not be null", progress)
        assertTrue("Overall progress should be between 0-100", progress.overallProgress in 0..100)
        assertNotNull("Concept mastery should not be null", progress.conceptMastery)
        assertTrue("Knowledge gaps should not be null", progress.knowledgeGaps.isNotEmpty())
        assertTrue("Recommended steps should not be null", progress.recommendedNextSteps.isNotEmpty())
    }

    @Test
    fun `generateSocraticQuestions should return mock questions`() = runTest {
        val topic = "测试话题"
        val currentLevel = 3

        val questions = service.generateSocraticQuestions(topic, currentLevel)

        assertNotNull("Questions should not be null", questions)
        assertTrue("Should return at least 3 questions", questions.size >= 3)
        questions.forEach { question ->
            assertTrue("Question should contain topic", question.contains(topic))
        }
    }

    @Test
    fun `evaluateAnswer should return mock AnswerEvaluation`() = runTest {
        val userAnswer = "用户答案"
        val correctAnswer = "正确答案"

        val evaluation = service.evaluateAnswer(userAnswer, correctAnswer)

        assertNotNull("AnswerEvaluation should not be null", evaluation)
        assertTrue("Confidence should be between 0.0-1.0", evaluation.confidence in 0.0..1.0)
        assertNotNull("Feedback should not be null", evaluation.feedback)
    }

    @Test
    fun `parseDocument should return mock DocumentAnalysis`() = runTest {
        val fileContent = "测试文件内容"
        val fileType = "pdf"

        val analysis = service.parseDocument(fileContent, fileType)

        assertNotNull("DocumentAnalysis should not be null", analysis)
        assertEquals("File type should match", fileType, analysis.fileType)
        assertNotNull("Summary should not be null", analysis.summary)
        assertTrue("Key points should not be empty", analysis.keyPoints.isNotEmpty())
        assertNotNull("Extracted text should not be null", analysis.extractedText)
    }

    @Test
    fun `extractKeyConcepts should return mock concepts`() = runTest {
        val content = "测试内容"

        val concepts = service.extractKeyConcepts(content)

        assertNotNull("Concepts should not be null", concepts)
        assertTrue("Should return at least 2 concepts", concepts.size >= 2)
        concepts.forEach { concept ->
            assertNotNull("Concept ID should not be null", concept.id)
            assertNotNull("Concept name should not be null", concept.name)
            assertNotNull("Concept definition should not be null", concept.definition)
        }
    }

    @Test
    fun `buildKnowledgeGraph should return mock KnowledgeGraph`() = runTest {
        val concepts = listOf(
            Concept("c1", "概念1", "定义1", listOf("c2")),
            Concept("c2", "概念2", "定义2", listOf("c1"))
        )

        val knowledgeGraph = service.buildKnowledgeGraph(concepts)

        assertNotNull("KnowledgeGraph should not be null", knowledgeGraph)
        assertEquals("Nodes should match input concepts", concepts.size, knowledgeGraph.nodes.size)
        assertTrue("Edges should not be null", knowledgeGraph.edges.isNotEmpty())
        knowledgeGraph.edges.forEach { edge ->
            assertNotNull("Edge 'from' should not be null", edge.from)
            assertNotNull("Edge 'to' should not be null", edge.to)
            assertNotNull("Edge relationship should not be null", edge.relationship)
        }
    }
}
