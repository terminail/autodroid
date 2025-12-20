package com.autodroid.trader.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 设备信息实体类
 * 用于Room数据库持久化存储设备信息
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    // 设备基本信息
    @PrimaryKey
    val id: String, // 设备唯一标识，可以使用Android ID或其他唯一标识
    val name: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    val androidVersion: String? = null,
    val platform: String? = null,
    val brand: String? = null,
    val device: String? = null,
    val product: String? = null,
    
    // 网络信息
    val ip: String? = null,
    
    // 状态信息
    val isConnected: Boolean = false,
    val lastSeen: Long? = null,
    
    // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 创建空设备信息
         */
        fun empty(): DeviceEntity = DeviceEntity(id = "")
        
        /**
         * 创建连接状态的设备信息
         */
        fun connected(id: String, ip: String, name: String? = null): DeviceEntity = DeviceEntity(
            id = id,
            ip = ip,
            name = name,
            isConnected = true,
            lastSeen = System.currentTimeMillis()
        )
        
        /**
         * 创建详细设备信息
         */
        fun detailed(
            id: String,
            ip: String,
            name: String,
            model: String,
            manufacturer: String,
            androidVersion: String,
            platform: String = "Android",
            brand: String = "",
            device: String = "",
            product: String = ""
        ): DeviceEntity = DeviceEntity(
            id = id,
            ip = ip,
            name = name,
            model = model,
            manufacturer = manufacturer,
            androidVersion = androidVersion,
            platform = platform,
            brand = brand,
            device = device,
            product = product,
            isConnected = true,
            lastSeen = System.currentTimeMillis()
        )
    }
    
    /**
     * 检查设备是否可用
     */
    fun isAvailable(): Boolean = ip != null && isConnected
    
    /**
     * 断开设备连接
     */
    fun disconnected(): DeviceEntity = this.copy(
        isConnected = false,
        lastSeen = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    /**
     * 更新设备信息
     */
    fun updateInfo(
        name: String? = this.name,
        model: String? = this.model,
        manufacturer: String? = this.manufacturer,
        androidVersion: String? = this.androidVersion,
        platform: String? = this.platform,
        brand: String? = this.brand,
        device: String? = this.device,
        product: String? = this.product
    ): DeviceEntity = this.copy(
        name = name,
        model = model,
        manufacturer = manufacturer,
        androidVersion = androidVersion,
        platform = platform,
        brand = brand,
        device = device,
        product = product,
        updatedAt = System.currentTimeMillis()
    )
}