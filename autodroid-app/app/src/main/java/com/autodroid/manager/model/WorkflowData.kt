package com.autodroid.manager.model

/**
 * 工作脚本详情数据封装类
 */
data class WorkflowData(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val status: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val steps: List<WorkflowStep>? = null
) {
    companion object {
        /**
         * 创建空工作脚本数据
         */
        fun empty(): WorkflowData = WorkflowData()
    }
}

/**
 * 工作脚本步骤信息
 */
data class WorkflowStep(
    val stepNumber: Int,
    val action: String,
    val description: String? = null,
    val parameters: Map<String, Any>? = null
)

/**
 * 工作脚本列表项数据类
 */
data class Workflow(
    val id: String? = null,
    val name: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val status: String? = null
)