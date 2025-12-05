package com.autodroid.manager.model

/**
 * 报告信息封装类
 * 封装报告相关的属性和状态
 */
data class Report(
    val id: String? = null,
    val title: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val completedAt: String? = null,
    val duration: String? = null,
    val device: String? = null,
    val workflow: String? = null,
    val steps: List<ReportStep>? = null
) {
    companion object {
        /**
         * 创建空报告信息
         */
        fun empty(): Report = Report()
        
        /**
         * 创建模拟报告信息（用于测试）
         */
        fun mock(): Report = Report(
            id = "report-001",
            title = "Sample Workflow Execution",
            status = "COMPLETED",
            createdAt = "2024-01-15 10:30:00",
            completedAt = "2024-01-15 10:45:00",
            duration = "15 minutes",
            device = "Test Device (Android 12)",
            workflow = "Sample Workflow",
            steps = listOf(
                ReportStep(1, "Launch App", "PASSED", "2s"),
                ReportStep(2, "Login", "PASSED", "3s"),
                ReportStep(3, "Execute Test", "PASSED", "8s"),
                ReportStep(4, "Logout", "PASSED", "2s")
            )
        )
    }
}

/**
 * 报告步骤信息封装类
 */
data class ReportStep(
    val stepNumber: Int,
    val action: String,
    val status: String,
    val duration: String
)