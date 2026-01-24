package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.database.MindMapDao
import com.autodroid.teachitback.database.TopicDao
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.model.TopicEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * MindMapRepository单元测试
 * 验证Repository与AIServiceRouter的集成及Local-First策略
 */
class MindMapRepositoryTest {

    private lateinit var repository: MindMapRepository
    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockMindMapDao: MindMapDao
    private lateinit var mockTopicDao: TopicDao

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockMindMapDao = mockk<MindMapDao>(relaxed = true)
        mockTopicDao = mockk<TopicDao>(relaxed = true)
        mockDatabase = mockk<AppDatabase>(relaxed = true)

        // Mock Database的DAO访问
        every { mockDatabase.mindMapDao() } returns mockMindMapDao
        every { mockDatabase.topicDao() } returns mockTopicDao

        // Repository只需要AppDatabase，不需要注入AIServiceRouter
        // AIServiceRouter是Repository内部的单例，对测试透明
        repository = MindMapRepository(mockDatabase)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetMindMapByTopicId_Existing() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"
        val existingMindMap = MindMapEntity(
            id = "mindmap-1",
            topicId = topicId,
            title = "Kotlin学习",
            structure = "{}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.getByTopicId(topicId) } returns existingMindMap

        // 执行测试
        val result = repository.getMindMapByTopicId(topicId)

        // 验证结果
        assertNotNull(result)
        assertEquals("mindmap-1", result.id)
        assertEquals("Kotlin学习", result.title)

        // 验证DAO调用
        coVerify { mockMindMapDao.getByTopicId(topicId) }
    }

    @Test
    fun testGetMindMapByTopicId_NotExisting() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"

        // Mock DAO行为
        coEvery { mockMindMapDao.getByTopicId(topicId) } returns null

        // 执行测试
        val result = repository.getMindMapByTopicId(topicId)

        // 验证结果
        assertNull(result)

        // 验证DAO调用
        coVerify { mockMindMapDao.getByTopicId(topicId) }
    }

    @Test
    fun testGenerateMindMap_LocalFirst_Existing() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"
        val learningGoal = "学习Kotlin编程"
        val existingMindMap = MindMapEntity(
            id = "mindmap-1",
            topicId = topicId,
            title = "Kotlin学习",
            structure = "{}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val topic = TopicEntity(
            id = topicId,
            title = "Kotlin学习",
            description = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.getByTopicId(topicId) } returns existingMindMap
        coEvery { mockTopicDao.getTopicByIdSync(topicId) } returns topic

        // 执行测试
        val result = repository.generateMindMap(topicId, learningGoal)

        // 验证结果：Local-First策略，直接返回本地数据
        assertNotNull(result)
        assertEquals("mindmap-1", result.id)
        assertEquals("Kotlin学习", result.title)

        // 验证DAO调用：只查询了本地，没有调用AI服务
        coVerify { mockMindMapDao.getByTopicId(topicId) }
        coVerify(exactly = 0) { mockTopicDao.getTopicByIdSync(any()) } // 因为已有本地数据，不会继续执行
    }

    @Test
    fun testGenerateMindMap_LocalFirst_NotExisting() = runBlocking {
        // 准备测试数据
        val topicId = "topic-123"
        val learningGoal = "学习Kotlin编程"
        val topic = TopicEntity(
            id = topicId,
            title = "Kotlin学习",
            description = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.getByTopicId(topicId) } returns null
        coEvery { mockTopicDao.getTopicByIdSync(topicId) } returns topic

        // 执行测试（AI服务返回null，模拟生成失败）
        val result = repository.generateMindMap(topicId, learningGoal)

        // 验证结果：本地没有，AI生成失败
        assertNull(result)

        // 验证DAO调用
        coVerify { mockMindMapDao.getByTopicId(topicId) }
        coVerify { mockTopicDao.getTopicByIdSync(topicId) }
    }

    @Test
    fun testUpdateNodeProgress() = runBlocking {
        // 准备测试数据
        val nodeId = "node-1"
        val progress = 75

        // Mock DAO行为
        coEvery { mockMindMapDao.updateNodeProgress(nodeId, progress) } returns Unit

        // 执行测试
        repository.updateNodeProgress(nodeId, progress)

        // 验证DAO调用
        coVerify { mockMindMapDao.updateNodeProgress(nodeId, progress) }
    }

    @Test
    fun testGetNodesByMindMapId() = runBlocking {
        // 准备测试数据
        val mindMapId = "mindmap-1"
        val nodes = listOf(
            MindMapNode(
                id = "node-1",
                mindMapId = mindMapId,
                parentId = null,
                title = "根节点",
                description = "",
                progress = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            MindMapNode(
                id = "node-2",
                mindMapId = mindMapId,
                parentId = "node-1",
                title = "子节点",
                description = "",
                progress = 50,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.getNodesByMindMapSync(mindMapId) } returns nodes

        // 执行测试
        val result = repository.getNodesByMindMapId(mindMapId)

        // 验证结果
        assertEquals(2, result.size)
        assertEquals("根节点", result[0].title)
        assertEquals("子节点", result[1].title)

        // 验证DAO调用
        coVerify { mockMindMapDao.getNodesByMindMapSync(mindMapId) }
    }

    @Test
    fun testInsertMindMap() = runBlocking {
        // 准备测试数据
        val mindMap = MindMapEntity(
            id = "mindmap-1",
            topicId = "topic-123",
            title = "Kotlin学习",
            structure = "{}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.insert(mindMap) } returns Unit

        // 执行测试
        repository.insertMindMap(mindMap)

        // 验证DAO调用
        coVerify { mockMindMapDao.insert(mindMap) }
    }

    @Test
    fun testInsertMindMapNode() = runBlocking {
        // 准备测试数据
        val node = MindMapNode(
            id = "node-1",
            mindMapId = "mindmap-1",
            parentId = null,
            title = "根节点",
            description = "",
            progress = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.insertNode(node) } returns Unit

        // 执行测试
        repository.insertMindMapNode(node)

        // 验证DAO调用
        coVerify { mockMindMapDao.insertNode(node) }
    }

    @Test
    fun testInsertMindMapNodes() = runBlocking {
        // 准备测试数据
        val nodes = listOf(
            MindMapNode(
                id = "node-1",
                mindMapId = "mindmap-1",
                parentId = null,
                title = "根节点",
                description = "",
                progress = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            MindMapNode(
                id = "node-2",
                mindMapId = "mindmap-1",
                parentId = "node-1",
                title = "子节点",
                description = "",
                progress = 50,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.insertNodes(nodes) } returns Unit

        // 执行测试
        repository.insertMindMapNodes(nodes)

        // 验证DAO调用
        coVerify { mockMindMapDao.insertNodes(nodes) }
    }

    @Test
    fun testGetAllMindMapsFlow() = runBlocking {
        // 准备测试数据
        val mindMaps = listOf(
            MindMapEntity(
                id = "mindmap-1",
                topicId = "topic-123",
                title = "Kotlin学习",
                structure = "{}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            MindMapEntity(
                id = "mindmap-2",
                topicId = "topic-456",
                title = "Java学习",
                structure = "{}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Mock DAO行为
        coEvery { mockMindMapDao.getAllMindMapsFlow() } returns flowOf(mindMaps)

        // 执行测试
        val result = repository.getAllMindMapsFlow().first()

        // 验证结果
        assertEquals(2, result.size)
        assertEquals("Kotlin学习", result[0].title)
        assertEquals("Java学习", result[1].title)
    }
}
