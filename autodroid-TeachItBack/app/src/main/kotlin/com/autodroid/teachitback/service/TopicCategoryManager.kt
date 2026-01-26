package com.autodroid.teachitback.service

import com.autodroid.teachitback.config.PresetTopicCategories
import com.autodroid.teachitback.config.TopicCategoryNode
import com.autodroid.teachitback.database.TopicDao
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.config.AIServiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import android.util.Log

/**
 * 主题分类管理器
 * 负责构建和管理主题分类结构，实现智能路由功能
 * 分类节点代表目录，主题代表具体的内容实体
 *
 * 统一使用 TopicCategoryNode 作为分类节点数据结构
 */
class TopicCategoryManager(private val topicDao: TopicDao) {

    /**
     * 获取指定树节点下的所有主题
     */
    suspend fun getTopicsByTreeNode(treeNodeId: String): List<TopicEntity> {
        return withContext(Dispatchers.IO) {
            topicDao.getTopicsByTreeNode(treeNodeId).first()
        }
    }

    /**
     * 根据主题能力推荐AI服务
     */
    suspend fun recommendAIService(
        topicId: String, 
        availableServices: List<AIServiceConfig>
    ): AIServiceConfig? {
        return withContext(Dispatchers.IO) {
            val topic = topicDao.getTopicByIdSync(topicId) ?: return@withContext null
            
            // 优先使用主题的偏好服务（按权重排序）
            topic.servicePreferences.entries
                .sortedByDescending { it.value }
                .forEach { (serviceId, _) ->
                    availableServices.find { it.id == serviceId }?.let { 
                        return@withContext it
                    }
                }
            
            // 根据主题能力推荐服务
            val capabilities = topic.capabilities
            
            // 能力匹配逻辑
            availableServices.firstOrNull { service ->
                when {
                    capabilities.contains(AIAbility.CODE_GENERATION) && 
                    service.id in listOf("deepseek", "kimi", "openai") -> true
                    
                    capabilities.contains(AIAbility.BASIC_CHAT) && 
                    service.id in listOf("chatglm", "tinybert", "deepseek") -> true
                    
                    capabilities.contains(AIAbility.MATH) && 
                    service.id in listOf("deepseek", "kimi", "openai") -> true
                    
                    capabilities.contains(AIAbility.LONG_TEXT) && 
                    service.id in listOf("tinybert", "chatglm") -> true
                    
                    else -> false
                }
            } ?: availableServices.firstOrNull() // 如果没有匹配，返回第一个可用服务
        }
    }

    /**
     * 构建主题树
     * 树节点代表分类目录，主题代表具体的内容实体
     *
     * 使用PresetTopicCategories获取分类节点的名称和描述
     */
    suspend fun buildTopicTree(): List<TopicCategoryNode> {
        return withContext(Dispatchers.IO) {
            val allTopics = topicDao.getAllTopics().first()

            // 从主题中提取所有唯一的树节点ID
            val treeNodeIds = allTopics.map { it.topicTreeNodeId }.distinct()

            // 为每个树节点ID创建分类节点
            treeNodeIds.map { treeNodeId ->
                val topicsInNode = allTopics.filter { it.topicTreeNodeId == treeNodeId }

                // 从预设分类配置中获取节点信息
                val presetCategory = PresetTopicCategories.getCategoryById(treeNodeId)

                if (presetCategory != null) {
                    // 使用预设分类的信息
                    presetCategory
                } else {
                    // 如果找不到预设分类，创建一个临时节点
                    TopicCategoryNode(
                        id = treeNodeId,
                        name = "自定义分类",
                        parentId = null,
                        description = "包含 ${topicsInNode.size} 个主题",
                        source = "user"
                    )
                }
            }
        }
    }

    /**
     * 构建完整的主题分类树（包含多层级结构）
     *
     * 这个方法会构建一个包含所有预设分类的完整树结构
     * 包括有主题的节点和空的分类节点
     */
    suspend fun buildFullCategoryTree(): TopicCategoryNode {
        return withContext(Dispatchers.IO) {
            val allTopics = topicDao.getAllTopics().first()
            val allCategories = PresetTopicCategories.getAllCategoriesFlattened()

            Log.d("TopicTreeManager", "构建完整分类树: ${allCategories.size} 个分类节点")

            // 构建分类树结构
            buildCategoryTreeRecursive(allCategories, null, allTopics) ?: TopicCategoryNode(
                id = "root",
                name = "全部分类",
                parentId = null,
                description = "主题分类根节点",
                source = "system",
                orderIndex = 0
            )
        }
    }

    /**
     * 递归构建分类树
     */
    private fun buildCategoryTreeRecursive(
        categories: List<TopicCategoryNode>,
        parentId: String?,
        allTopics: List<TopicEntity>
    ): TopicCategoryNode? {
        // 获取当前层级的分类节点
        val currentCategories = categories.filter { it.parentId == parentId }

        if (currentCategories.isEmpty()) {
            return null
        }

        // 为每个分类节点构建子树
        val children = currentCategories.mapNotNull { category ->
            val topicsInNode = allTopics.filter { it.topicTreeNodeId == category.id }

            // 递归构建子节点
            val childTreeNodes = buildCategoryTreeRecursive(categories, category.id, allTopics)

            // 如果没有子节点且没有主题，则过滤掉这个节点
            if (childTreeNodes == null && topicsInNode.isEmpty() && category.source != "system") {
                return@mapNotNull null
            }

            TopicCategoryNode(
                id = category.id,
                name = category.name,
                parentId = category.parentId, // 保持原始的父节点ID
                children = childTreeNodes?.let { listOf(it) },
                description = if (topicsInNode.isNotEmpty()) {
                    "${category.description} (${topicsInNode.size} 个主题)"
                } else {
                    category.description
                },
                source = category.source,
                orderIndex = category.orderIndex
            )
        }

        // 如果只有一个根节点，直接返回；否则创建一个虚拟根节点
        return if (children.size == 1) {
            children[0]
        } else {
            TopicCategoryNode(
                id = "root",
                name = "全部分类",
                parentId = null,
                children = children,
                description = "主题分类根节点",
                source = "system",
                orderIndex = 0
            )
        }
    }

    /**
     * 搜索相关主题
     */
    suspend fun searchRelatedTopics(
        query: String, 
        maxResults: Int = 10
    ): List<TopicEntity> {
        return withContext(Dispatchers.IO) {
            val allTopics = topicDao.getAllTopics().first()
            
            // 简单的关键词匹配搜索
            allTopics.filter { topic ->
                topic.title.contains(query, ignoreCase = true) ||
                topic.description.contains(query, ignoreCase = true)
            }.take(maxResults)
        }
    }

    /**
     * 获取同一树节点下的其他主题（兄弟主题）
     */
    suspend fun getSiblingTopics(topicId: String): List<TopicEntity> {
        return withContext(Dispatchers.IO) {
            val topic = topicDao.getTopicByIdSync(topicId) ?: return@withContext emptyList()
            
            // 获取同一树节点下的所有主题，排除当前主题
            topicDao.getTopicsByTreeNode(topic.topicTreeNodeId).first()
                .filter { it.id != topicId }
        }
    }

    /**
     * 更新主题的服务偏好
     */
    suspend fun updateServicePreference(
        topicId: String, 
        serviceId: String, 
        preferred: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val topic = topicDao.getTopicByIdSync(topicId) ?: return@withContext
            
            val currentPreferences = topic.servicePreferences.toMutableMap()
            
            if (preferred) {
                currentPreferences[serviceId] = 1.0 // 简单权重设置
            } else {
                currentPreferences.remove(serviceId)
            }
            
            topicDao.updateTopic(topic.copy(servicePreferences = currentPreferences))
        }
    }
}
