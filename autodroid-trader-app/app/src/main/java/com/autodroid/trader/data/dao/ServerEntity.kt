package com.autodroid.trader.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 服务器信息实体类
 * 用于Room数据库持久化存储服务器连接信息
 * 使用服务器名称作为主键，简化数据管理
 */
@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey
    val apiEndpoint: String, // API端点作为主键

    // 服务器基本信息
    val name: String?,

    // 连接状态
    val isConnected: Boolean = false,
    val lastConnectedTime: Long = 0,

    // 服务器详细信息（从ServerInfoResponse获取）
    val hostname: String = "",
    val platform: String = "",
    val version: String = "",
    val deviceCount: Int = 0,

    // 服务器能力信息
    val supportsDeviceRegistration: Boolean = false,
    val supportsApkManagement: Boolean = false,
    val supportsTradePlanExecution: Boolean = false,
    val supportsTradeScheduling: Boolean = false,
    val supportsEventTriggering: Boolean = false,

    // 发现方式
    val discoveryType: String = "", // "qrcode", "manual"

    // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)