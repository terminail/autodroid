package com.autodroid.teachitback.config

import com.autodroid.teachitback.service.TopicTreeNode

/**
 * 预设主题分类配置
 * 
 * 设计思路：
 * 1. 当前阶段：硬编码预设分类，快速验证架构
 * 2. 未来扩展：可以从Git仓库动态下载分类结构
 * 
 * Git同步扩展性设计：
 * - 分类结构可以序列化为JSON格式
 * - 通过TopicCategorySyncManager从Git仓库拉取更新
 * - 支持用户自定义分类的持久化存储
 * 
 * Git仓库结构示例：
 * topic-categories/
 * ├── categories.json           # 主分类结构
 * ├── education/
 * │   ├── categories.json        # 教育子分类
 * │   └── topics.json            # 教育相关预设主题
 * ├── finance/
 * │   ├── categories.json
 * │   └── topics.json
 * └── ...
 */
object PresetTopicCategories {

    /**
     * 预设分类节点定义
     * 结构设计考虑了多层级支持和Git同步扩展性
     */
    val categories: List<TopicCategoryNode> = listOf(
        // ===== 根分类 =====
        TopicCategoryNode(
            id = "preset-topics",
            name = "预设主题",
            description = "应用预设的学习主题",
            orderIndex = 1,
            source = "preset"
        ),
        
        TopicCategoryNode(
            id = "user-topics",
            name = "用户主题",
            description = "用户自定义的学习主题",
            orderIndex = 2,
            source = "user"
        ),

        // ===== 教育学习 =====
        TopicCategoryNode(
            id = "education",
            name = "教育学习",
            description = "各阶段教育学科学习",
            orderIndex = 3,
            source = "preset",
            children = listOf(
                TopicCategoryNode(
                    id = "high-school",
                    name = "高中教育",
                    description = "高中各学科学习",
                    parentId = "education",
                    orderIndex = 1,
                    source = "preset"
                ),
                TopicCategoryNode(
                    id = "professional-exam",
                    name = "职业考试",
                    description = "各类职业资格考试",
                    parentId = "education",
                    orderIndex = 2,
                    source = "preset"
                )
            )
        ),

        // ===== 财务金融 =====
        TopicCategoryNode(
            id = "finance",
            name = "财务金融",
            description = "财务规划与投资管理",
            orderIndex = 4,
            source = "preset",
            children = listOf(
                TopicCategoryNode(
                    id = "cfp-exam",
                    name = "CFP考试",
                    description = "CFP财务规划师考试",
                    parentId = "finance",
                    orderIndex = 1,
                    source = "preset"
                ),
                TopicCategoryNode(
                    id = "investment",
                    name = "投资管理",
                    description = "资产配置与投资策略",
                    parentId = "finance",
                    orderIndex = 2,
                    source = "preset"
                )
            )
        ),

        // ===== 技术学习 =====
        TopicCategoryNode(
            id = "technology",
            name = "技术学习",
            description = "编程与技术技能学习",
            orderIndex = 5,
            source = "preset",
            children = listOf(
                TopicCategoryNode(
                    id = "programming",
                    name = "编程开发",
                    description = "各类编程语言与开发",
                    parentId = "technology",
                    orderIndex = 1,
                    source = "preset"
                ),
                TopicCategoryNode(
                    id = "data-science",
                    name = "数据科学",
                    description = "数据分析与机器学习",
                    parentId = "technology",
                    orderIndex = 2,
                    source = "preset"
                )
            )
        ),

        // ===== 语言学习 =====
        TopicCategoryNode(
            id = "language",
            name = "语言学习",
            description = "各类语言学习",
            orderIndex = 6,
            source = "preset",
            children = listOf(
                TopicCategoryNode(
                    id = "english",
                    name = "英语学习",
                    description = "英语听说读写",
                    parentId = "language",
                    orderIndex = 1,
                    source = "preset"
                ),
                TopicCategoryNode(
                    id = "other-languages",
                    name = "其他语言",
                    description = "其他外语学习",
                    parentId = "language",
                    orderIndex = 2,
                    source = "preset"
                )
            )
        )
    )

    /**
     * 主题到分类的映射
     * 用于快速查找主题应该关联到哪个分类节点
     */
    val topicCategoryMapping: Map<String, String> = mapOf(
        // ===== CFP财务规划相关 =====
        "CFP财务规划" to "cfp-exam",
        "投资组合管理" to "investment",
        "税务规划" to "cfp-exam",

        // ===== 高中教育相关 =====
        "高中数学" to "high-school",
        "高中物理" to "high-school",
        "高中化学" to "high-school",
        "高中生物" to "high-school",
        "高中英语" to "english",
        "高中历史" to "high-school",
        "高中地理" to "high-school",
        "高中政治" to "high-school"
    )

    /**
     * 获取主题所属的分类节点ID
     * @param topicTitle 主题标题
     * @return 分类节点ID，如果未找到则返回预设主题ID
     */
    fun getCategoryForTopic(topicTitle: String): String {
        return topicCategoryMapping[topicTitle] ?: "preset-topics"
    }

    /**
     * 根据ID获取分类节点
     * @param nodeId 分类节点ID
     * @return 分类节点，如果未找到则返回null
     */
    fun getCategoryById(nodeId: String): TopicCategoryNode? {
        return findNodeRecursive(categories, nodeId)
    }

    /**
     * 递归查找分类节点
     */
    private fun findNodeRecursive(
        nodes: List<TopicCategoryNode>,
        targetId: String
    ): TopicCategoryNode? {
        for (node in nodes) {
            if (node.id == targetId) {
                return node
            }
            node.children?.let { children ->
                findNodeRecursive(children, targetId)?.let { return it }
            }
        }
        return null
    }

    /**
     * 获取所有分类节点（扁平化）
     */
    fun getAllCategoriesFlattened(): List<TopicCategoryNode> {
        val result = mutableListOf<TopicCategoryNode>()
        flattenCategories(categories, result)
        return result
    }

    /**
     * 递归扁平化分类列表
     */
    private fun flattenCategories(
        nodes: List<TopicCategoryNode>,
        result: MutableList<TopicCategoryNode>
    ) {
        for (node in nodes) {
            result.add(node)
            node.children?.let { flattenCategories(it, result) }
        }
    }

    /**
     * 导出为JSON格式（为Git同步做准备）
     * 这个方法会在未来用于将分类结构导出为JSON，上传到Git仓库
     */
    fun exportToJson(): String {
        // TODO: 实现JSON导出逻辑
        return """
            {
                "version": "1.0.0",
                "lastUpdated": "${System.currentTimeMillis()}",
                "categories": [
                    // ... JSON结构
                ]
            }
        """.trimIndent()
    }

    /**
     * 构建从根节点到指定节点的分类路径
     * @param categoryId 分类节点ID
     * @return 分类路径列表（从根节点到目标节点）
     */
    fun buildCategoryPath(categoryId: String): List<TopicCategoryNode> {
        val path = mutableListOf<TopicCategoryNode>()
        buildPathRecursive(categories, categoryId, path)
        return path
    }

    /**
     * 递归构建路径
     */
    private fun buildPathRecursive(
        nodes: List<TopicCategoryNode>,
        targetId: String,
        path: MutableList<TopicCategoryNode>
    ): Boolean {
        for (node in nodes) {
            if (node.id == targetId) {
                path.add(0, node)
                return true
            }
            node.children?.let { children ->
                if (buildPathRecursive(children, targetId, path)) {
                    path.add(0, node)
                    return true
                }
            }
        }
        return false
    }

    /**
     * 获取分类路径字符串（用于显示）
     * @param categoryId 分类节点ID
     * @param separator 分隔符，默认为 " > "
     * @return 分类路径字符串，例如："教育学习 > 高中教育"
     */
    fun getCategoryPathString(categoryId: String, separator: String = " > "): String {
        val path = buildCategoryPath(categoryId)
        return path.joinToString(separator) { it.name }
    }
}

/**
 * 主题分类节点数据类
 * 
 * 设计考虑：
 * - 支持多层级结构（通过parentId和children）
 * - 支持来源标识（preset/git/user），便于区分分类来源
 * - 支持排序（orderIndex）
 * - 可序列化为JSON，便于Git同步
 */
data class TopicCategoryNode(
    val id: String,
    val name: String,
    val description: String = "",
    val parentId: String? = null,
    val orderIndex: Int = 0,
    val source: String = "preset", // preset, git, user
    val children: List<TopicCategoryNode>? = null
) {
    /**
     * 转换为TopicTreeNode（用于TopicTreeManager）
     */
    fun toTopicTreeNode(topicIds: List<String> = emptyList()): TopicTreeNode {
        return TopicTreeNode(
            id = id,
            name = name,
            parent = null, // 在这里不设置parent，由TopicTreeManager构建
            children = children?.map { it.toTopicTreeNode() } ?: emptyList(),
            description = description,
            topicIds = topicIds
        )
    }
}

/**
 * 主题分类同步管理器（预留接口）
 * 
 * 未来实现：
 * 1. 从Git仓库拉取分类结构
 * 2. 解析JSON并更新本地分类
 * 3. 支持增量更新
 * 4. 冲突解决（用户自定义 vs Git更新）
 */
class TopicCategorySyncManager {
    
    /**
     * 从Git仓库同步分类结构
     * @param repoUrl Git仓库URL
     * @param onSuccess 同步成功回调
     * @param onError 同步失败回调
     */
    suspend fun syncFromGit(
        repoUrl: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        // TODO: 实现Git同步逻辑
        // 1. 克隆/拉取Git仓库
        // 2. 解析categories.json
        // 3. 合并本地和远程分类
        // 4. 保存到数据库或SharedPreferences
    }
    
    /**
     * 检查是否有更新
     * @return 是否有新的分类结构可以更新
     */
    suspend fun checkForUpdates(): Boolean {
        // TODO: 实现更新检查逻辑
        return false
    }
}
