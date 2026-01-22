package com.autodroid.teachitback.adapter

import org.junit.Test
import org.junit.Assert.*
import com.autodroid.teachitback.model.MindMapNode

class MindMapItemTest {
    
    @Test
    fun `MindMapItem应该支持不同类型的消息项`() {
        // RED: 这个测试会失败，因为MindMapItem类还不存在
        val textItem = MindMapItem.TextItem("Hello World", "user")
        val mindMapItem = MindMapItem.MindMapDisplayItem("map1")
        
        assertEquals(MindMapItem.TYPE_TEXT, textItem.getType())
        assertEquals(MindMapItem.TYPE_MINDMAP, mindMapItem.getType())
    }
    
    @Test
    fun `MindMapDisplayItem应该包含正确的节点数据`() {
        // RED: 这个测试会失败
        val nodes = listOf(
            MindMapNode(id = "1", mindMapId = "map1", title = "根节点"),
            MindMapNode(id = "2", mindMapId = "map1", parentId = "1", title = "子节点")
        )
        
        val mindMapItem = MindMapItem.MindMapDisplayItem(
            mindMapId = "map1",
            nodes = nodes
        )
        
        assertEquals("map1", mindMapItem.mindMapId)
        assertEquals(2, mindMapItem.nodes.size)
    }
    
    @Test
    fun `MindMapItem应该支持树形结构构建`() {
        // RED: 这个测试会失败
        val nodes = listOf(
            MindMapNode(id = "1", mindMapId = "map1", title = "A"),
            MindMapNode(id = "2", mindMapId = "map1", parentId = "1", title = "A1"),
            MindMapNode(id = "3", mindMapId = "map1", parentId = "1", title = "A2")
        )
        
        val mindMapItem = MindMapItem.MindMapDisplayItem("map1", nodes)
        val treeStructure = mindMapItem.buildTreeStructure()
        
        assertNotNull(treeStructure)
        assertEquals(1, treeStructure.size)
        assertEquals(2, treeStructure[0].children.size)
    }
}