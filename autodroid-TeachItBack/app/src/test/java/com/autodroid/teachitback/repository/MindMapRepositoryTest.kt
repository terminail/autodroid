package com.autodroid.teachitback.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.database.MindMapDao
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MindMapRepository单元测试
 * 测试Repository层的业务逻辑
 */
@RunWith(AndroidJUnit4::class)
class MindMapRepositoryTest {
    
    private lateinit var database: AppDatabase
    private lateinit var mindMapDao: MindMapDao
    private lateinit var mindMapRepository: MindMapRepository
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = AppDatabase.getDatabase(context)
        mindMapDao = database.mindMapDao()
        mindMapRepository = MindMapRepository(mindMapDao)
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    /**
     * 测试插入和获取MindMap
     */
    @Test
    fun testInsertAndGetMindMap() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 获取MindMap
        val retrieved = mindMapRepository.getMindMapByTopicId("test_topic").first()
        assertNotNull(retrieved)
        assertEquals("Test MindMap", retrieved?.title)
    }
    
    /**
     * 测试节点操作
     */
    @Test
    fun testNodeOperations() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 插入节点
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node",
            progress = 0
        )
        mindMapRepository.insertNode(node)
        
        // 获取节点
        val nodes = mindMapRepository.getNodesByMindMap(mindMap.id).first()
        assertEquals(1, nodes.size)
        assertEquals("Test Node", nodes[0].title)
    }
    
    /**
     * 测试进度更新
     */
    @Test
    fun testProgressUpdate() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 插入节点
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node",
            progress = 0
        )
        mindMapRepository.insertNode(node)
        
        // 更新进度
        val updatedNode = node.copy(progress = 75)
        mindMapRepository.updateNodeProgress(updatedNode)
        
        // 验证进度更新
        val retrievedNode = mindMapRepository.getNodeById(node.id)
        assertEquals(75, retrievedNode?.progress)
    }
    
    /**
     * 测试批量操作
     */
    @Test
    fun testBatchOperations() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 批量插入节点
        val nodes = listOf(
            MindMapNode(mindMapId = mindMap.id, title = "Node 1", progress = 30),
            MindMapNode(mindMapId = mindMap.id, title = "Node 2", progress = 60),
            MindMapNode(mindMapId = mindMap.id, title = "Node 3", progress = 90)
        )
        mindMapRepository.insertNodes(nodes)
        
        // 验证节点数量
        val nodeCount = mindMapRepository.getNodeCount(mindMap.id)
        assertEquals(3, nodeCount)
        
        // 验证平均进度
        val averageProgress = mindMapRepository.calculateOverallProgress(mindMap.id)
        assertEquals(60, averageProgress) // (30+60+90)/3 = 60
    }
    
    /**
     * 测试父子节点关系
     */
    @Test
    fun testParentChildRelationship() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 插入父节点
        val parentNode = MindMapNode(
            mindMapId = mindMap.id,
            title = "Parent Node"
        )
        mindMapRepository.insertNode(parentNode)
        
        // 插入子节点
        val childNode = MindMapNode(
            mindMapId = mindMap.id,
            parentId = parentNode.id,
            title = "Child Node"
        )
        mindMapRepository.insertNode(childNode)
        
        // 验证子节点
        val childNodes = mindMapRepository.getChildNodes(parentNode.id).first()
        assertEquals(1, childNodes.size)
        assertEquals("Child Node", childNodes[0].title)
    }
    
    /**
     * 测试根节点获取
     */
    @Test
    fun testRootNodes() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 插入根节点和子节点
        val rootNode = MindMapNode(
            mindMapId = mindMap.id,
            title = "Root Node"
        )
        val childNode = MindMapNode(
            mindMapId = mindMap.id,
            parentId = rootNode.id,
            title = "Child Node"
        )
        mindMapRepository.insertNode(rootNode)
        mindMapRepository.insertNode(childNode)
        
        // 获取根节点
        val rootNodes = mindMapRepository.getRootNodes(mindMap.id).first()
        assertEquals(1, rootNodes.size)
        assertEquals("Root Node", rootNodes[0].title)
    }
    
    /**
     * 测试删除操作
     */
    @Test
    fun testDeleteOperations() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 插入节点
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node"
        )
        mindMapRepository.insertNode(node)
        
        // 删除节点
        mindMapRepository.deleteNode(node)
        
        // 验证删除
        val retrievedNode = mindMapRepository.getNodeById(node.id)
        assertNull(retrievedNode)
        
        // 删除MindMap
        mindMapRepository.deleteMindMap(mindMap)
        
        // 验证删除
        val retrievedMindMap = mindMapRepository.getMindMapByTopicId("test_topic").first()
        assertNull(retrievedMindMap)
    }
    
    /**
     * 测试空MindMap处理
     */
    @Test
    fun testEmptyMindMap() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Empty MindMap"
        )
        mindMapRepository.insertMindMap(mindMap)
        
        // 验证空MindMap的进度计算
        val progress = mindMapRepository.calculateOverallProgress(mindMap.id)
        assertEquals(0, progress)
        
        // 验证空节点列表
        val nodes = mindMapRepository.getNodesByMindMap(mindMap.id).first()
        assertTrue(nodes.isEmpty())
        
        // 验证空根节点列表
        val rootNodes = mindMapRepository.getRootNodes(mindMap.id).first()
        assertTrue(rootNodes.isEmpty())
    }
}