package com.autodroid.teachitback.repository

import android.util.Log
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.router.AIServiceRouter
import kotlinx.coroutines.flow.Flow

/**
 * MindMap数据仓库
 * 负责协调本地数据库和AI路由服务，实现Local-First策略
 *
 * 架构原则：
 * - ViewModel不感知AI服务，只与Repository交互
 * - Repository内部使用AIServiceRouter单例
 * - Local-First策略：先返回本地数据，异步调用AI服务
 */
class MindMapRepository(
    private val database: AppDatabase
) {

    // Repository内部使用AIServiceRouter单例，对ViewModel完全透明
    private val aiRouter: AIServiceRouter
        get() = AIServiceRouter.instance

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
     * 2. 如果本地没有，使用AIServiceRouter智能路由到合适的AI服务生成
     * 3. 保存AI结果到本地数据库（包含AIProcessInfo）
     */
    suspend fun generateMindMap(topicId: String, learningGoal: String? = null): MindMapEntity? {
        Log.d("MindMapRepository", "开始生成MindMap: topicId=$topicId")

        // 1. 优先返回本地已有的MindMap
        val existingMindMap = database.mindMapDao().getByTopicId(topicId)
        if (existingMindMap != null) {
            Log.d("MindMapRepository", "返回本地已有MindMap: ${existingMindMap.id}")
            return existingMindMap
        }
        Log.d("MindMapRepository", "本地无MindMap，准备调用AI服务生成")

        // 2. 获取Topic信息
        val topic = database.topicDao().getTopicByIdSync(topicId)
        if (topic == null) {
            Log.e("MindMapRepository", "Topic不存在: $topicId")
            return null
        }
        Log.d("MindMapRepository", "获取Topic信息: ${topic.title}")

        // 3. 使用AIServiceRouter智能路由到合适的AI服务生成MindMap
        val mindMapEntity = try {
            aiRouter.routeByCapability(
                capabilityCheck = { it.supportMindMapGeneration },
                operation = { service ->
                    val startTime = System.currentTimeMillis()
                    val result = service.generateMindMap(topicId, learningGoal ?: topic.title)
                    val processingTime = System.currentTimeMillis() - startTime
                    Log.d("MindMapRepository", "AI生成MindMap完成: ${service.config.displayName}, 耗时: ${processingTime}ms")

                    // 添加AIProcessInfo到结果中
                    result?.copy(
                        // TODO: 任务26完成后添加 aiProcessInfo = AIProcessInfo(...)
                    )
                }
            )
        } catch (e: Exception) {
            Log.e("MindMapRepository", "MindMap生成失败: ${e.message}", e)
            null
        }

        // 4. 保存AI结果到本地数据库
        if (mindMapEntity != null) {
            database.mindMapDao().insert(mindMapEntity)
            Log.d("MindMapRepository", "保存MindMap到数据库: ${mindMapEntity.id}")

            // 保存MindMap节点
            val nodes = database.mindMapDao().getNodesByMindMapSync(mindMapEntity.id)
            if (nodes.isNotEmpty()) {
                database.mindMapDao().insertNodes(nodes)
                Log.d("MindMapRepository", "保存MindMap节点: ${nodes.size} 个")
            }
        } else {
            Log.w("MindMapRepository", "AI未生成有效的MindMap")
        }

        return mindMapEntity
    }

    /**
     * 更新节点的学习进度
     */
    suspend fun updateNodeProgress(nodeId: String, progress: Int) {
        Log.d("MindMapRepository", "更新节点进度: nodeId=$nodeId, progress=$progress")
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
