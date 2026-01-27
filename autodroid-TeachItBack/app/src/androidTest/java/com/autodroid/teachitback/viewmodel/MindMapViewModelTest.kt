package com.autodroid.teachitback.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.repository.MindMapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MindMapViewModel单元测试
 * 测试ViewModel的业务逻辑和状态管理
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MindMapViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: AppDatabase
    private lateinit var mindMapRepository: MindMapRepository
    private lateinit var viewModel: MindMapViewModel
    
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = AppDatabase.getDatabase(context)
        mindMapRepository = MindMapRepository(database.mindMapDao())
        viewModel = MindMapViewModel(ApplicationProvider.getApplicationContext())
    }
    
    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }
    
    /**
     * 测试加载MindMap
     */
    @Test
    fun testLoadMindMapForTopic() = testScope.runTest {
        // 创建测试数据
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 验证状态
        val currentMindMap = viewModel.currentMindMap.value
        assertNotNull(currentMindMap)
        assertEquals("Test MindMap", currentMindMap?.title)
    }
    
    /**
     * 测试更新MindMap
     */
    @Test
    fun testUpdateMindMap() = testScope.runTest {
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Original Title")
        mindMapRepository.insertMindMap(mindMap)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 更新MindMap
        val updatedMindMap = viewModel.currentMindMap.value?.copy(title = "Updated Title")
        viewModel.updateMindMap(updatedMindMap)
        advanceUntilIdle()
        
        // 验证更新
        val retrieved = mindMapRepository.getMindMapByTopicId(topicId)
        assertEquals("Updated Title", retrieved?.title)
    }
    
    /**
     * 测试添加节点
     */
    @Test
    fun testAddNode() = testScope.runTest {
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 添加节点
        val node = MindMapNode(
            mindMapId = viewModel.currentMindMap.value?.id ?: "",
            title = "New Node",
            description = "New Description",
            progress = 0
        )
        viewModel.addNode(node)
        advanceUntilIdle()
        
        // 验证添加
        val nodes = mindMapRepository.getNodesByMindMap(mindMap.id)
        assertTrue(nodes.isNotEmpty())
        assertEquals("New Node", nodes[0].title)
    }
    
    /**
     * 测试更新节点
     */
    @Test
    fun testUpdateNode() = testScope.runTest {
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Original Node",
            progress = 50
        )
        mindMapRepository.insertNode(node)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 更新节点
        val updatedNode = node.copy(progress = 75)
        viewModel.updateNode(updatedNode)
        advanceUntilIdle()
        
        // 验证更新
        val retrieved = mindMapRepository.getNodeById(node.id)
        assertEquals(75, retrieved?.progress)
    }
    
    /**
     * 测试删除节点
     */
    @Test
    fun testDeleteNode() = testScope.runTest {
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node"
        )
        mindMapRepository.insertNode(node)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 删除节点
        viewModel.deleteNode(node)
        advanceUntilIdle()
        
        // 验证删除
        val nodes = mindMapRepository.getNodesByMindMap(mindMap.id)
        assertTrue(nodes.isEmpty())
    }
    
    /**
     * 测试清空MindMap
     */
    @Test
    fun testClearMindMap() = testScope.runTest {
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        val nodes = listOf(
            MindMapNode(mindMapId = mindMap.id, title = "Node 1"),
            MindMapNode(mindMapId = mindMap.id, title = "Node 2")
        )
        nodes.forEach { mindMapRepository.insertNode(it) }
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 清空MindMap
        viewModel.clearMindMap()
        advanceUntilIdle()
        
        // 验证清空
        val retrievedNodes = mindMapRepository.getNodesByMindMap(mindMap.id)
        assertTrue(retrievedNodes.isEmpty())
    }
    
    /**
     * 测试更新节点进度
     */
    @Test
    fun testUpdateNodeProgress() = testScope.runTest {
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node",
            progress = 0
        )
        mindMapRepository.insertNode(node)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 更新进度
        viewModel.updateNodeProgress(node.id, 50)
        advanceUntilIdle()
        
        // 验证更新
        val retrieved = mindMapRepository.getNodeById(node.id)
        assertEquals(50, retrieved?.progress)
    }
}
