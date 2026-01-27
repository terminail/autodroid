package com.autodroid.teachitback.adapter

import org.junit.Test
import org.junit.Assert.*
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.ui.adapter.ChatItem

class MindMapItemTest {
    
    @Test
    fun `ChatItem应该支持不同类型的消息项`() {
        val userMessageItem = ChatItem.UserMessageItem(
            com.autodroid.teachitback.model.MessageEntity(
                id = "msg-1",
                topicId = "topic-1",
                content = "Hello World",
                senderType = "USER",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            )
        )
        val mindMapItem = ChatItem.MindMapDisplayItem(
            mindMapNodes = emptyList(),
            title = "map1"
        )
        
        assertEquals(ChatItem.TYPE_USER_MESSAGE, userMessageItem.getType())
        assertEquals(ChatItem.TYPE_MINDMAP, mindMapItem.getType())
    }
    
    @Test
    fun `MindMapDisplayItem应该包含正确的节点数据`() {
        val nodes = listOf(
            MindMapNode(id = "1", mindMapId = "map1", title = "根节点"),
            MindMapNode(id = "2", mindMapId = "map1", parentId = "1", title = "子节点")
        )
        
        val mindMapItem = ChatItem.MindMapDisplayItem(
            mindMapNodes = nodes,
            title = "map1"
        )
        
        assertEquals("map1", mindMapItem.title)
        assertEquals(2, mindMapItem.mindMapNodes.size)
    }
    
    @Test
    fun `ChatItem应该支持树形结构构建`() {
        val nodes = listOf(
            MindMapNode(id = "1", mindMapId = "map1", title = "A"),
            MindMapNode(id = "2", mindMapId = "map1", parentId = "1", title = "A1"),
            MindMapNode(id = "3", mindMapId = "map1", parentId = "1", title = "A2")
        )
        
        val mindMapItem = ChatItem.MindMapDisplayItem(nodes, "map1")
        assertNotNull(mindMapItem.mindMapNodes)
        assertEquals(3, mindMapItem.mindMapNodes.size)
    }
}