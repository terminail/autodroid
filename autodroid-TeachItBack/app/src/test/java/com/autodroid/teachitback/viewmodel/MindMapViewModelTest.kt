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
     * 测试创建MindMap
     */
    @Test
    fun testCreateMindMapForTopic() = testScope.runTest {
        val topicId = "new_topic"
        val title = "New MindMap"
        
        // 创建MindMap
        viewModel.createMindMapForTopic(topicId, title)
        advanceUntilIdle()
        
        // 验证MindMap创建
        val currentMindMap = viewModel.currentMindMap.value
        assertNotNull(currentMindMap)
        assertEquals(title, currentMindMap?.title)
        assertEquals(topicId, currentMindMap?.topicId)
    }
    
    /**
     * 测试添加节点
     */
    @Test
    fun testAddNode() = testScope.runTest {
        // 先创建MindMap
        val topicId = "test_topic"
        viewModel.createMindMapForTopic(topicId, "Test MindMap")
        advanceUntilIdle()
        
        // 添加节点
        val nodeTitle = "Test Node"
        viewModel.addNode(nodeTitle)
        advanceUntilIdle()
        
        // 验证节点添加
        val nodes = viewModel.currentMindMapNodes.value
        assertTrue(nodes.isNotEmpty())
        assertEquals(nodeTitle, nodes.last().title)
    }
    
    /**
     * 测试更新节点进度
     */
    @Test
    fun testUpdateNodeProgress() = testScope.runTest {
        // 创建测试数据
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        val node = MindMapNode(mindMapId = mindMap.id, title = "Test Node", progress = 0)
        mindMapRepository.insertNode(node)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 更新进度
        viewModel.updateNodeProgress(node.id, 75)
        advanceUntilIdle()
        
        // 验证进度更新
        val updatedNode = mindMapRepository.getNodeById(node.id)
        assertEquals(75, updatedNode?.progress)
    }
    
    /**
     * 测试节点展开/收起状态
     */
    @Test
    fun testToggleNodeExpansion() = testScope.runTest {
        val nodeId = "test_node"
        
        // 初始状态应该为空
        assertTrue(viewModel.expandedNodeIds.value.isEmpty())
        
        // 展开节点
        viewModel.toggleNodeExpansion(nodeId)
        advanceUntilIdle()
        
        // 验证展开状态
        assertTrue(viewModel.expandedNodeIds.value.contains(nodeId))
        
        // 收起节点
        viewModel.toggleNodeExpansion(nodeId)
        advanceUntilIdle()
        
        // 验证收起状态
        assertTrue(viewModel.expandedNodeIds.value.isEmpty())
    }
    
    /**
     * 测试多个节点展开状态
     */
    @Test
    fun testMultipleNodeExpansions() = testScope.runTest {
        val nodeId1 = "node1"
        val nodeId2 = "node2"
        
        // 展开多个节点
        viewModel.toggleNodeExpansion(nodeId1)
        viewModel.toggleNodeExpansion(nodeId2)
        advanceUntilIdle()
        
        // 验证多个节点展开状态
        val expandedIds = viewModel.expandedNodeIds.value
        assertEquals(2, expandedIds.size)
        assertTrue(expandedIds.contains(nodeId1))
        assertTrue(expandedIds.contains(nodeId2))
        
        // 收起一个节点
        viewModel.toggleNodeExpansion(nodeId1)
        advanceUntilIdle()
        
        // 验证状态
        val updatedIds = viewModel.expandedNodeIds.value
        assertEquals(1, updatedIds.size)
        assertTrue(updatedIds.contains(nodeId2))
    }
    
    /**
     * 测试清除当前MindMap状态
     */
    @Test
    fun testClearCurrentMindMap() = testScope.runTest {
        // 创建测试数据
        val topicId = "test_topic"
        viewModel.createMindMapForTopic(topicId, "Test MindMap")
        viewModel.addNode("Test Node")
        viewModel.toggleNodeExpansion("test_node")
        advanceUntilIdle()
        
        // 验证初始状态
        assertNotNull(viewModel.currentMindMap.value)
        assertTrue(viewModel.currentMindMapNodes.value.isNotEmpty())
        assertTrue(viewModel.expandedNodeIds.value.isNotEmpty())
        
        // 清除状态
        viewModel.clearCurrentMindMap()
        advanceUntilIdle()
        
        // 验证清除状态
        assertNull(viewModel.currentMindMap.value)
        assertTrue(viewModel.currentMindMapNodes.value.isEmpty())
        assertTrue(viewModel.expandedNodeIds.value.isEmpty())
    }
    
    /**
     * 测试根节点获取
     */
    @Test
    fun testGetRootNodes() = testScope.runTest {
        // 创建测试数据
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        // 插入根节点和子节点
        val rootNode = MindMapNode(mindMapId = mindMap.id, title = "Root Node")
        val childNode = MindMapNode(mindMapId = mindMap.id, parentId = rootNode.id, title = "Child Node")
        mindMapRepository.insertNode(rootNode)
        mindMapRepository.insertNode(childNode)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 验证根节点获取
        val rootNodes = viewModel.getRootNodes().value
        assertNotNull(rootNodes)
        assertEquals(1, rootNodes.size)
        assertEquals("Root Node", rootNodes[0].title)
    }
    
    /**
     * 测试空MindMap处理
     */
    @Test
    fun testEmptyMindMapHandling() = testScope.runTest {
        // 加载不存在的MindMap
        viewModel.loadMindMapForTopic("non_existent_topic")
        advanceUntilIdle()
        
        // 验证空状态
        assertNull(viewModel.currentMindMap.value)
        assertTrue(viewModel.currentMindMapNodes.value.isEmpty())
    }
    
    /**
     * 测试节点操作在空MindMap时的安全处理
     */
    @Test
    fun testSafeOperationsOnEmptyMindMap() = testScope.runTest {
        // 在没有MindMap的情况下尝试添加节点
        viewModel.addNode("Test Node")
        advanceUntilIdle()
        
        // 验证没有错误发生，节点列表保持为空
        assertTrue(viewModel.currentMindMapNodes.value.isEmpty())
        
        // 在没有MindMap的情况下尝试更新进度
        viewModel.updateNodeProgress("non_existent_node", 50)
        advanceUntilIdle()
        
        // 验证没有错误发生
        assertTrue(viewModel.currentMindMapNodes.value.isEmpty())
    }
    
    /**
     * 测试MindMapItem的生成
     */
    @Test
    fun testMindMapItemGeneration() = testScope.runTest {
        // 创建测试数据
        val topicId = "test_topic"
        val mindMap = MindMapEntity(topicId = topicId, title = "Test MindMap")
        mindMapRepository.insertMindMap(mindMap)
        
        val node = MindMapNode(mindMapId = mindMap.id, title = "Test Node", progress = 50)
        mindMapRepository.insertNode(node)
        
        // 加载MindMap
        viewModel.loadMindMapForTopic(topicId)
        advanceUntilIdle()
        
        // 验证MindMapItem生成
        val mindMapItem = viewModel.currentMindMapItem.value
        assertNotNull(mindMapItem)
        assertEquals(mindMap.title, mindMapItem?.mindMap?.title)
        assertTrue(mindMapItem?.nodes?.isNotEmpty() ?: false)
    }
}