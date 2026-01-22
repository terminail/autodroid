package com.autodroid.teachitback.model

import org.junit.Test
import org.junit.Assert.*

class MindMapNodeTest {
    
    @Test
    fun `MindMapNode应该具有正确的属性`() {
        // RED: 这个测试会失败，因为MindMapNode类还不存在
        val node = MindMapNode(
            id = "node1",
            mindMapId = "map1",
            parentId = null,
            title = "根节点",
            progress = 50
        )
        
        assertEquals("node1", node.id)
        assertEquals("map1", node.mindMapId)
        assertNull(node.parentId)
        assertEquals("根节点", node.title)
        assertEquals(50, node.progress)
    }
    
    @Test
    fun `MindMapNode应该支持子节点`() {
        // RED: 这个测试会失败
        val parent = MindMapNode(
            id = "parent",
            mindMapId = "map1",
            parentId = null,
            title = "父节点",
            progress = 60
        )
        
        val child = MindMapNode(
            id = "child",
            mindMapId = "map1",
            parentId = "parent",
            title = "子节点",
            progress = 30
        )
        
        assertEquals("parent", child.parentId)
        assertTrue(parent.isRoot())
        assertFalse(child.isRoot())
    }
    
    @Test
    fun `MindMapNode应该根据进度返回颜色`() {
        // RED: 这个测试会失败
        val node0 = MindMapNode(id = "1", mindMapId = "map1", progress = 0)
        val node30 = MindMapNode(id = "2", mindMapId = "map1", progress = 30)
        val node70 = MindMapNode(id = "3", mindMapId = "map1", progress = 70)
        
        assertEquals(Color.RED, node0.getProgressColor())
        assertEquals(Color.YELLOW, node30.getProgressColor())
        assertEquals(Color.GREEN, node70.getProgressColor())
    }
}