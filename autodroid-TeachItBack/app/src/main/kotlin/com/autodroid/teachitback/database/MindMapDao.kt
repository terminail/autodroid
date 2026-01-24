package com.autodroid.teachitback.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Transaction
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import kotlinx.coroutines.flow.Flow

/**
 * MindMap数据访问对象接口
 * 提供对MindMap实体和节点的CRUD操作
 */
@Dao
interface MindMapDao {

    // ===== MindMap Entity 操作 =====
    
    @Query("SELECT * FROM mindmaps WHERE topicId = :topicId")
    suspend fun getByTopicId(topicId: String): MindMapEntity?
    
    @Query("SELECT * FROM mindmaps WHERE id = :id")
    fun getById(id: String): Flow<MindMapEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mindMap: MindMapEntity)
    
    @Update
    suspend fun update(mindMap: MindMapEntity)
    
    @Delete
    suspend fun delete(mindMap: MindMapEntity)
    
    @Query("DELETE FROM mindmaps WHERE topicId = :topicId")
    suspend fun deleteByTopicId(topicId: String)
    
    @Query("DELETE FROM mindmaps")
    suspend fun deleteAllMindMaps()

    // ===== MindMapNode 操作 =====
    
    @Query("SELECT * FROM mindmap_nodes WHERE mindMapId = :mindMapId ORDER BY createdAt ASC")
    fun getNodesByMindMap(mindMapId: String): Flow<List<MindMapNode>>
    
    @Query("SELECT * FROM mindmap_nodes WHERE mindMapId = :mindMapId ORDER BY createdAt ASC")
    suspend fun getNodesByMindMapSync(mindMapId: String): List<MindMapNode>
    
    @Query("SELECT * FROM mindmap_nodes WHERE id = :id")
    suspend fun getNodeById(id: String): MindMapNode?
    
    @Query("SELECT * FROM mindmap_nodes WHERE parentId = :parentId ORDER BY createdAt ASC")
    fun getChildNodes(parentId: String): Flow<List<MindMapNode>>
    
    @Query("SELECT * FROM mindmap_nodes WHERE mindMapId = :mindMapId AND parentId IS NULL ORDER BY createdAt ASC")
    fun getRootNodes(mindMapId: String): Flow<List<MindMapNode>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: MindMapNode)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<MindMapNode>)
    
    @Update
    suspend fun updateNode(node: MindMapNode)
    
    @Delete
    suspend fun deleteNode(node: MindMapNode)
    
    @Query("DELETE FROM mindmap_nodes WHERE mindMapId = :mindMapId")
    suspend fun deleteNodesByMindMap(mindMapId: String)
    
    @Query("DELETE FROM mindmap_nodes")
    suspend fun deleteAllNodes()
    
    // ===== 事务操作 =====
    
    @Transaction
    suspend fun insertMindMapWithNodes(mindMap: MindMapEntity, nodes: List<MindMapNode>) {
        insert(mindMap)
        insertNodes(nodes)
    }
    
    @Transaction
    suspend fun deleteMindMapWithNodes(mindMapId: String) {
        deleteNodesByMindMap(mindMapId)
        deleteByTopicId(mindMapId)
    }
    
    @Query("SELECT COUNT(*) FROM mindmap_nodes WHERE mindMapId = :mindMapId")
    suspend fun getNodeCount(mindMapId: String): Int
    
    @Query("UPDATE mindmap_nodes SET progress = :progress, updatedAt = :timestamp WHERE id = :nodeId")
    suspend fun updateNodeProgress(nodeId: String, progress: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT AVG(progress) FROM mindmap_nodes WHERE mindMapId = :mindMapId")
    suspend fun getAverageProgress(mindMapId: String): Float?
    
    // ===== 验证工具需要的额外方法 =====
    
    @Query("SELECT * FROM mindmaps")
    suspend fun getAllMindMaps(): List<MindMapEntity>
    
    @Query("SELECT * FROM mindmaps")
    fun getAllMindMapsFlow(): Flow<List<MindMapEntity>>
    
    @Query("SELECT * FROM mindmap_nodes")
    suspend fun getAllNodes(): List<MindMapNode>
    
    @Query("SELECT * FROM mindmap_nodes")
    fun getAllNodesFlow(): Flow<List<MindMapNode>>
    
    @Query("SELECT * FROM mindmap_nodes WHERE parentId IS NULL")
    suspend fun getAllRootNodes(): List<MindMapNode>
    
    @Query("SELECT * FROM mindmap_nodes WHERE parentId IS NULL")
    fun getAllRootNodesFlow(): Flow<List<MindMapNode>>

    // AIProcessInfo相关查询
    @Query("SELECT * FROM mindmaps WHERE aiProcessInfoJson LIKE '%' || :serviceId || '%'")
    fun getMindMapsByService(serviceId: String): Flow<List<MindMapEntity>>

    @Query("SELECT * FROM mindmaps WHERE aiProcessInfoJson IS NOT NULL")
    fun getAIGeneratedMindMaps(): Flow<List<MindMapEntity>>

    @Query("SELECT COUNT(*) FROM mindmaps WHERE aiProcessInfoJson LIKE '%' || :serviceId || '%'")
    suspend fun getMindMapCountByService(serviceId: String): Int
}