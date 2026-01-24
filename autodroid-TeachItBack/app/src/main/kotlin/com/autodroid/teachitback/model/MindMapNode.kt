package com.autodroid.teachitback.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * MindMap节点实体，表示树形结构中的单个节点
 * 每个节点属于一个MindMap，可以有父节点和子节点
 */
@Entity(
    tableName = "mindmap_nodes",
    foreignKeys = [
        ForeignKey(
            entity = MindMapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mindMapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mindMapId", "parentId"])]
)
data class MindMapNode(
    @PrimaryKey
    val id: String = "",
    
    val mindMapId: String = "",
    
    val parentId: String? = null,
    
    val title: String = "",
    
    val description: String? = null,
    
    val progress: Int = 0,
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * 检查是否为根节点（没有父节点）
     */
    fun isRoot(): Boolean = parentId == null
    
    /**
     * 根据进度返回颜色编码
     * 红色(0-30%): 未掌握
     * 黄色(31-70%): 部分掌握  
     * 绿色(71-100%): 基本掌握
     */
    fun getProgressColor(): Int {
        return when {
            progress <= 30 -> android.graphics.Color.RED
            progress <= 70 -> android.graphics.Color.YELLOW
            else -> android.graphics.Color.GREEN
        }
    }
    
    /**
     * 获取缩进层级（基于父节点关系计算）
     * 根节点层级为0，每增加一级深度加1
     */
    fun getIndentLevel(nodes: List<MindMapNode>): Int {
        var level = 0
        var currentParentId = parentId
        
        while (currentParentId != null) {
            level++
            val parent = nodes.find { it.id == currentParentId }
            currentParentId = parent?.parentId
        }
        
        return level
    }
}