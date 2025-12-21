package com.autodroid.trader.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 设备信息实体类
 * 用于Room数据库持久化存储设备信息
 * 镜像服务器端Device模型 (core/database/models.py)
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    // 设备基本信息 - 与服务器端保持一致
    @PrimaryKey
    val udid: String, // 对应服务器端的udid，设备唯一标识符
    val userId: String? = null, // 对应服务器端的user_id
    val deviceName: String = "Unknown Device", // 对应服务器端的device_name
    val name: String? = null, // 对应服务器端的name
    val model: String? = null, // 对应服务器端的model
    val manufacturer: String? = null, // 对应服务器端的manufacturer
    val androidVersion: String = "Unknown", // 对应服务器端的android_version
    val apiLevel: Int? = null, // 对应服务器端的api_level
    val platform: String = "Android", // 对应服务器端的platform
    val brand: String? = null, // 对应服务器端的brand
    val device: String? = null, // 对应服务器端的device
    val product: String? = null, // 对应服务器端的product
    
    // 网络信息
    val ip: String? = null, // 对应服务器端的ip
    
    // 屏幕信息
    val screenWidth: Int? = null, // 对应服务器端的screen_width
    val screenHeight: Int? = null, // 对应服务器端的screen_height
    
    // 设备状态信息
    val batteryLevel: Int = 50, // 对应服务器端的battery_level
    val isOnline: Boolean = false, // 对应服务器端的is_online
    val connectionType: String = "network", // 对应服务器端的connection_type
    
    // 调试状态
    val usbDebugEnabled: Boolean = false, // 对应服务器端的usb_debug_enabled
    val wifiDebugEnabled: Boolean = false, // 对应服务器端的wifi_debug_enabled
    val debugCheckStatus: String = "UNKNOWN", // 对应服务器端的debug_check_status: UNKNOWN, SUCCESS, FAILED
    val debugCheckMessage: String? = null, // 对应服务器端的debug_check_message
    val debugCheckTime: Long? = null, // 对应服务器端的debug_check_time
    
    // 时间戳 - 使用Long类型存储时间戳，与服务器端DateTimeField对应
    val registeredAt: Long = System.currentTimeMillis(), // 对应服务器端的registered_at
    val createdAt: Long = System.currentTimeMillis(), // 对应服务器端的created_at
    val updatedAt: Long = System.currentTimeMillis() // 对应服务器端的updated_at
) {
    companion object {
        /**
         * 创建空设备信息
         */
        fun empty(): DeviceEntity = DeviceEntity(udid = "")
        
        /**
         * 创建连接状态的设备信息
         */
        fun connected(udid: String, ip: String, name: String? = null): DeviceEntity = DeviceEntity(
            udid = udid,
            ip = ip,
            name = name,
            isOnline = true,
            updatedAt = System.currentTimeMillis()
        )
        
        /**
         * 创建详细设备信息
         */
        fun detailed(
            udid: String,
            ip: String,
            name: String,
            model: String,
            manufacturer: String,
            androidVersion: String,
            apiLevel: Int? = null,
            platform: String = "Android",
            brand: String = "",
            device: String = "",
            product: String = "",
            screenWidth: Int? = null,
            screenHeight: Int? = null
        ): DeviceEntity = DeviceEntity(
            udid = udid,
            ip = ip,
            name = name,
            model = model,
            manufacturer = manufacturer,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            platform = platform,
            brand = brand,
            device = device,
            product = product,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            isOnline = true,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 检查设备是否可用
     */
    fun isAvailable(): Boolean = ip != null && isOnline
    
    /**
     * 更新设备调试权限检查状态
     */
    fun updateDebugCheckStatus(
        status: String,
        message: String? = null,
        usbDebugEnabled: Boolean? = null,
        wifiDebugEnabled: Boolean? = null
    ): DeviceEntity = this.copy(
        debugCheckStatus = status,
        debugCheckMessage = message,
        debugCheckTime = System.currentTimeMillis(),
        usbDebugEnabled = usbDebugEnabled ?: this.usbDebugEnabled,
        wifiDebugEnabled = wifiDebugEnabled ?: this.wifiDebugEnabled,
        updatedAt = System.currentTimeMillis()
    )
    
    /**
     * 断开设备连接
     */
    fun disconnected(): DeviceEntity = this.copy(
        isOnline = false,
        updatedAt = System.currentTimeMillis()
    )
    
    /**
     * 更新设备信息
     */
    fun updateInfo(
        name: String? = this.name,
        model: String? = this.model,
        manufacturer: String? = this.manufacturer,
        androidVersion: String = this.androidVersion,
        apiLevel: Int? = this.apiLevel,
        platform: String = this.platform,
        brand: String? = this.brand,
        device: String? = this.device,
        product: String? = this.product,
        screenWidth: Int? = this.screenWidth,
        screenHeight: Int? = this.screenHeight
    ): DeviceEntity = this.copy(
        name = name,
        model = model,
        manufacturer = manufacturer,
        androidVersion = androidVersion,
        apiLevel = apiLevel,
        platform = platform,
        brand = brand,
        device = device,
        product = product,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        updatedAt = System.currentTimeMillis()
    )
}