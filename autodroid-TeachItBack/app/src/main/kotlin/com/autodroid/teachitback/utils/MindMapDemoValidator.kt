package com.autodroid.teachitback.utils

import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.model.TopicEntity

/**
 * MindMap数据验证工具
 * 用于在数据插入数据库之前验证数据的正确性和完整性
 * 
 * 设计原则：
 * 1. 数据验证应该在插入数据库之前进行
 * 2. 数据库应该存储已验证的有效数据
 * 3. 验证工具应该是数据输入流程的一部分
 */
class MindMapDemoValidator {

    /**
     * 验证Topic数据的完整性
     */
    fun validateTopic(topic: TopicEntity): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证必填字段
        if (topic.title.isBlank()) {
            errors.add("话题标题不能为空")
        }
        
        if (topic.description.isBlank()) {
            warnings.add("话题描述为空，建议添加描述")
        }
        
        // 验证掌握程度范围
        if (topic.masteryLevel < 0 || topic.masteryLevel > 100) {
            errors.add("掌握程度必须在0-100之间，当前值: ${topic.masteryLevel}")
        }
        
        // 验证时间戳
        if (topic.createdAt <= 0) {
            errors.add("创建时间戳无效")
        }
        
        if (topic.lastAccessed <= 0) {
            warnings.add("最后访问时间戳无效")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * 验证MindMap数据的完整性
     */
    fun validateMindMap(mindMap: MindMapEntity): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证必填字段
        if (mindMap.title.isBlank()) {
            errors.add("MindMap标题不能为空")
        }
        
        if (mindMap.topicId.isBlank()) {
            errors.add("关联的话题ID不能为空")
        }
        
        // 验证时间戳
        if (mindMap.createdAt <= 0) {
            errors.add("创建时间戳无效")
        }
        
        if (mindMap.updatedAt <= 0) {
            warnings.add("更新时间戳无效")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * 验证MindMap节点数据的完整性
     */
    fun validateMindMapNode(node: MindMapNode, existingNodes: List<MindMapNode> = emptyList()): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证必填字段
        if (node.title.isBlank()) {
            errors.add("节点标题不能为空")
        }
        
        if (node.mindMapId.isBlank()) {
            errors.add("关联的MindMap ID不能为空")
        }
        
        // 验证进度值范围
        if (node.progress < 0 || node.progress > 100) {
            errors.add("进度值必须在0-100之间，当前值: ${node.progress}")
        }
        
        // 验证父子关系（如果提供了现有节点列表）
        if (node.parentId != null && existingNodes.isNotEmpty()) {
            val parentExists = existingNodes.any { it.id == node.parentId }
            if (!parentExists) {
                errors.add("父节点不存在: ${node.parentId}")
            }
        }
        
        // 验证循环引用（如果提供了现有节点列表）
        if (existingNodes.isNotEmpty() && node.parentId != null) {
            if (hasCyclicReference(node.id, node.parentId, existingNodes)) {
                errors.add("检测到循环引用: ${node.id} -> ${node.parentId}")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * 验证MindMap节点集合的完整性
     */
    fun validateMindMapNodes(nodes: List<MindMapNode>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证每个节点
        nodes.forEach { node ->
            val nodeResult = validateMindMapNode(node, nodes)
            errors.addAll(nodeResult.errors)
            warnings.addAll(nodeResult.warnings)
        }
        
        // 验证根节点数量
        val rootNodes = nodes.filter { it.parentId == null }
        if (rootNodes.size > 1) {
            warnings.add("检测到多个根节点（${rootNodes.size}个），建议只有一个根节点")
        }
        
        // 验证层级结构
        val maxDepth = calculateMaxDepth(nodes)
        if (maxDepth > 5) {
            warnings.add("层级过深（${maxDepth}层），建议简化结构")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * 检查循环引用
     */
    private fun hasCyclicReference(currentId: String, parentId: String, nodes: List<MindMapNode>): Boolean {
        val visited = mutableSetOf<String>()
        var currentParentId: String? = parentId
        
        while (currentParentId != null) {
            if (currentParentId == currentId) {
                return true // 检测到循环引用
            }
            
            if (visited.contains(currentParentId)) {
                break // 避免无限循环
            }
            
            visited.add(currentParentId)
            val parentNode = nodes.find { it.id == currentParentId }
            currentParentId = parentNode?.parentId
        }
        
        return false
    }

    /**
     * 计算最大深度
     */
    private fun calculateMaxDepth(nodes: List<MindMapNode>): Int {
        val nodeMap = nodes.associateBy { it.id }
        
        fun getDepth(nodeId: String): Int {
            val node = nodeMap[nodeId] ?: return 0
            val children = nodes.filter { it.parentId == nodeId }
            return if (children.isEmpty()) {
                1
            } else {
                1 + (children.maxOfOrNull { getDepth(it.id) } ?: 0)
            }
        }
        
        val rootNodes = nodes.filter { it.parentId == null }
        return if (rootNodes.isEmpty()) 0 else rootNodes.maxOf { getDepth(it.id) }
    }

    /**
     * 验证结果数据结构
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    )
}