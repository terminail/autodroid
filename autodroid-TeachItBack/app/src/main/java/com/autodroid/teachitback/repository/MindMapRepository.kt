package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import kotlinx.coroutines.flow.Flow

class MindMapRepository(private val database: AppDatabase) {
    
    fun getAllMindMapsFlow() = database.mindMapDao().getAllMindMapsFlow()
    
    fun getAllNodesFlow() = database.mindMapDao().getAllNodesFlow()
    
    fun getNodesByMindMapFlow(mindMapId: String) = database.mindMapDao().getNodesByMindMap(mindMapId)
    
    fun getAllRootNodesFlow() = database.mindMapDao().getAllRootNodesFlow()
    
    suspend fun getMindMapByTopicId(topicId: String): MindMapEntity? {
        return database.mindMapDao().getByTopicId(topicId)
    }
    
    suspend fun getNodesByMindMapId(mindMapId: String): List<MindMapNode> {
        return database.mindMapDao().getNodesByMindMapSync(mindMapId)
    }
    
    suspend fun insertMindMap(mindMap: MindMapEntity) {
        database.mindMapDao().insert(mindMap)
    }
    
    suspend fun insertMindMapNode(node: MindMapNode) {
        database.mindMapDao().insertNode(node)
    }
    
    suspend fun insertMindMapNodes(nodes: List<MindMapNode>) {
        database.mindMapDao().insertNodes(nodes)
    }
}
