package com.autodroid.trader.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 服务器信息实体类
 * 用于Room数据库持久化存储服务器连接信息
 * 使用ip+port作为复合主键，确保每个服务器地址唯一
 */
@Entity(tableName = "servers", primaryKeys = ["ip", "port"])
data class ServerEntity(
    // 服务器基本信息
    val ip: String,
    val port: Int,
    val name: String? = null,
    val platform: String? = null,
    val services: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap(),

    // 手机端管理用信息
    // 连接状态
    val isConnected: Boolean = false,
    val lastConnectedTime: Long = 0,

    // 发现方式
    val discoveryType: String = "", // "qrcode", "manual", "autoscan"

    // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取API端点URL
     */
    fun apiEndpoint(): String = "http://${ip}:${port}/api"
}