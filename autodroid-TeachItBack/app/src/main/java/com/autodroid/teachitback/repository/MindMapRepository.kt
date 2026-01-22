package com.autodroid.teachitback.repository

import com.autodroid.teachitback.api.TencentCloudAIService
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import kotlinx.coroutines.flow.Flow

/**
 * MindMap数据仓库
 * 负责协调本地数据库和AI服务，实现Local-First策略
 */
class MindMapRepository(
    private val database: AppDatabase,
    private val aiService: TencentCloudAIService
) {

    // ===== Flow 数据流 =====

    fun getAllMindMapsFlow() = database.mindMapDao().getAllMindMapsFlow()

    fun getAllNodesFlow() = database.mindMapDao().getAllNodesFlow()

    fun getNodesByMindMapFlow(mindMapId: String) = database.mindMapDao().getNodesByMindMap(mindMapId)

    fun getAllRootNodesFlow() = database.mindMapDao().getAllRootNodesFlow()

    // ===== Local-First: 优先返回本地数据 =====

    /**
     * 获取指定话题的MindMap
     * Local-First策略：优先返回本地数据
     */
    suspend fun getMindMapByTopicId(topicId: String): MindMapEntity? {
        return database.mindMapDao().getByTopicId(topicId)
    }

    /**
     * 生成MindMap（使用AI服务）
     * Local-First策略：
     * 1. 优先返回本地已有的MindMap
     * 2. 如果本地没有，异步调用AI服务生成
     * 3. 保存AI结果到本地数据库
     */
    suspend fun generateMindMap(topicId: String, learningGoal: String? = null): MindMapEntity? {
        // 1. 优先返回本地已有的MindMap
        val existingMindMap = database.mindMapDao().getByTopicId(topicId)
        if (existingMindMap != null) {
            return existingMindMap
        }

        // 2. 获取Topic信息
        val topic = database.topicDao().getTopicByIdSync(topicId)
        if (topic == null) {
            return null
        }

        // 3. 异步调用AI服务生成MindMap
        val mindMapEntity = aiService.generateMindMap(topicId, learningGoal ?: topic.title)

        // 4. 保存AI结果到本地数据库
        if (mindMapEntity != null) {
            database.mindMapDao().insert(mindMapEntity)

            // 保存MindMap节点
            val nodes = database.mindMapDao().getNodesByMindMapSync(mindMapEntity.id)
            if (nodes.isNotEmpty()) {
                database.mindMapDao().insertNodes(nodes)
            }
        }

        return mindMapEntity
    }

    /**
     * 更新节点的学习进度
     */
    suspend fun updateNodeProgress(nodeId: String, progress: Int) {
        database.mindMapDao().updateNodeProgress(nodeId, progress)
    }

    // ===== 数据库操作 =====

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
