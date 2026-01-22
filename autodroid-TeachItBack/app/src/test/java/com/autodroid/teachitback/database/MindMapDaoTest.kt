package com.autodroid.teachitback.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * MindMapDao单元测试
 * 测试MindMap和MindMapNode的数据库操作
 */
@RunWith(AndroidJUnit4::class)
class MindMapDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var mindMapDao: MindMapDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        mindMapDao = database.mindMapDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    /**
     * 测试插入和查询MindMap
     */
    @Test
    fun testInsertAndRetrieveMindMap() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 查询MindMap
        val retrieved = mindMapDao.getByTopicId("test_topic")
        assertNotNull(retrieved)
        assertEquals("Test MindMap", retrieved?.title)
        assertEquals("test_topic", retrieved?.topicId)
    }
    
    /**
     * 测试更新MindMap
     */
    @Test
    fun testUpdateMindMap() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Original Title"
        )
        mindMapDao.insert(mindMap)
        
        // 更新MindMap
        val updatedMindMap = mindMap.copy(title = "Updated Title")
        mindMapDao.update(updatedMindMap)
        
        // 验证更新
        val retrieved = mindMapDao.getByTopicId("test_topic")
        assertEquals("Updated Title", retrieved?.title)
    }
    
    /**
     * 测试删除MindMap
     */
    @Test
    fun testDeleteMindMap() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 删除MindMap
        mindMapDao.delete(mindMap)
        
        // 验证删除
        val retrieved = mindMapDao.getByTopicId("test_topic")
        assertNull(retrieved)
    }
    
    /**
     * 测试插入和查询MindMapNode
     */
    @Test
    fun testInsertAndRetrieveMindMapNode() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 插入节点
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node",
            description = "Test Description",
            progress = 50
        )
        mindMapDao.insertNode(node)
        
        // 查询节点
        val nodes = mindMapDao.getNodesByMindMap(mindMap.id).first()
        assertEquals(1, nodes.size)
        assertEquals("Test Node", nodes[0].title)
        assertEquals("Test Description", nodes[0].description)
        assertEquals(50, nodes[0].progress)
    }
    
    /**
     * 测试父子节点关系
     */
    @Test
    fun testParentChildNodeRelationship() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 插入父节点
        val parentNode = MindMapNode(
            mindMapId = mindMap.id,
            title = "Parent Node"
        )
        mindMapDao.insertNode(parentNode)
        
        // 插入子节点
        val childNode = MindMapNode(
            mindMapId = mindMap.id,
            parentId = parentNode.id,
            title = "Child Node"
        )
        mindMapDao.insertNode(childNode)
        
        // 查询所有节点
        val allNodes = mindMapDao.getNodesByMindMap(mindMap.id).first()
        assertEquals(2, allNodes.size)
        
        // 查询子节点
        val childNodes = mindMapDao.getChildNodes(parentNode.id).first()
        assertEquals(1, childNodes.size)
        assertEquals("Child Node", childNodes[0].title)
    }
    
    /**
     * 测试节点进度更新
     */
    @Test
    fun testUpdateNodeProgress() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 插入节点
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node",
            progress = 0
        )
        mindMapDao.insertNode(node)
        
        // 更新进度
        mindMapDao.updateNodeProgress(node.id, 75)
        
        // 验证更新
        val updatedNode = mindMapDao.getNodeById(node.id)
        assertEquals(75, updatedNode?.progress)
    }
    
    /**
     * 测试批量插入节点
     */
    @Test
    fun testBatchInsertNodes() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 批量插入节点
        val nodes = listOf(
            MindMapNode(mindMapId = mindMap.id, title = "Node 1"),
            MindMapNode(mindMapId = mindMap.id, title = "Node 2"),
            MindMapNode(mindMapId = mindMap.id, title = "Node 3")
        )
        mindMapDao.insertNodes(nodes)
        
        // 验证插入
        val retrievedNodes = mindMapDao.getNodesByMindMap(mindMap.id).first()
        assertEquals(3, retrievedNodes.size)
    }
    
    /**
     * 测试删除MindMap及其节点
     */
    @Test
    fun testDeleteMindMapWithNodes() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 插入节点
        val node = MindMapNode(
            mindMapId = mindMap.id,
            title = "Test Node"
        )
        mindMapDao.insertNode(node)
        
        // 删除MindMap及其节点
        mindMapDao.deleteMindMapWithNodes(mindMap.id)
        
        // 验证删除
        val retrievedMindMap = mindMapDao.getByTopicId("test_topic")
        assertNull(retrievedMindMap)
        
        val retrievedNodes = mindMapDao.getNodesByMindMap(mindMap.id).first()
        assertTrue(retrievedNodes.isEmpty())
    }
    
    /**
     * 测试节点数量统计
     */
    @Test
    fun testNodeCount() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 插入多个节点
        val nodes = listOf(
            MindMapNode(mindMapId = mindMap.id, title = "Node 1"),
            MindMapNode(mindMapId = mindMap.id, title = "Node 2"),
            MindMapNode(mindMapId = mindMap.id, title = "Node 3")
        )
        mindMapDao.insertNodes(nodes)
        
        // 验证节点数量
        val nodeCount = mindMapDao.getNodeCount(mindMap.id)
        assertEquals(3, nodeCount)
    }
    
    /**
     * 测试平均进度计算
     */
    @Test
    fun testAverageProgress() = runBlocking {
        // 插入MindMap
        val mindMap = MindMapEntity(
            topicId = "test_topic",
            title = "Test MindMap"
        )
        mindMapDao.insert(mindMap)
        
        // 插入带不同进度的节点
        val nodes = listOf(
            MindMapNode(mindMapId = mindMap.id, title = "Node 1", progress = 30),
            MindMapNode(mindMapId = mindMap.id, title = "Node 2", progress = 60),
            MindMapNode(mindMapId = mindMap.id, title = "Node 3", progress = 90)
        )
        mindMapDao.insertNodes(nodes)
        
        // 计算平均进度 (30+60+90)/3 = 60
        val averageProgress = mindMapDao.getAverageProgress(mindMap.id)
        assertEquals(60.0f, averageProgress)
    }
}