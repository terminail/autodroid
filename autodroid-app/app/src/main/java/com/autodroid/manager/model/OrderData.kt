package com.autodroid.manager.model

/**
 * 订单详情数据封装类
 */
data class OrderData(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val scheduledAt: String? = null,
    val completedAt: String? = null,
    val workflowId: String? = null,
    val deviceId: String? = null,
    val priority: Int? = null
) {
    companion object {
        /**
         * 创建空订单数据
         */
        fun empty(): OrderData = OrderData()
    }
}