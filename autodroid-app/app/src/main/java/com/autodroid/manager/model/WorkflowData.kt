package com.autodroid.manager.model

/**
 * 工作流详情数据封装类
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
         * 创建空工作流数据
         */
        fun empty(): WorkflowData = WorkflowData()
    }
}

/**
 * 工作流步骤信息
 */
data class WorkflowStep(
    val stepNumber: Int,
    val action: String,
    val description: String? = null,
    val parameters: Map<String, Any>? = null
)