package com.autodroid.trader.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 设备信息实体类
 * 用于Room数据库持久化存储设备信息
 * 镜像服务器端Device模型 (core/database/models.py)
 * 使用设备序列号作为主键，与adb devices和Appium保持一致
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    // 设备基本信息 - 与服务器端保持一致
    @PrimaryKey
    val serialNo: String, // 设备序列号，与adb devices和Appium保持一致，作为主键
    val udid: String?=null, // 备用设备标识符，用于向后兼容
    val userId: String? = null, // 对应服务器端的user_id
    val name: String = "Unknown Device", // 对应服务器端的name
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
    val checkStatus: String = "UNKNOWN", // 对应服务器端的check_status: UNKNOWN, SUCCESS, FAILED
    val checkMessage: String? = null, // 对应服务器端的check_message
    val checkTime: Long? = null, // 对应服务器端的check_time
    
    // 已安装应用
    val apps: String? = null, // 对应服务器端的apps，JSON格式存储已安装的应用列表
    
    // 时间戳 - 使用Long类型存储时间戳，与服务器端DateTimeField对应
    val registeredAt: Long = System.currentTimeMillis(), // 对应服务器端的registered_at
    val createdAt: Long = System.currentTimeMillis(), // 对应服务器端的created_at
    val updatedAt: Long = System.currentTimeMillis() // 对应服务器端的updated_at
) {
    companion object {
        /**
         * 创建空设备信息
         */
        fun empty(): DeviceEntity = DeviceEntity(serialNo = "", udid = "")
        
        /**
         * 创建连接状态的设备信息
         */
        fun connected(serialNo: String, udid: String, ip: String, name: String? = null): DeviceEntity = DeviceEntity(
            serialNo = serialNo,
            udid = udid,
            ip = ip,
            name = name ?: "Unknown Device",
            isOnline = true,
            updatedAt = System.currentTimeMillis()
        )
        
        /**
         * 创建详细设备信息
         */
        fun detailed(
            serialNo: String,
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
            serialNo = serialNo,
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
     * 更新设备检查状态
     */
    fun updateCheckStatus(
        status: String,
        message: String? = null,
        usbDebugEnabled: Boolean? = null,
        wifiDebugEnabled: Boolean? = null
    ): DeviceEntity = this.copy(
        checkStatus = status,
        checkMessage = message,
        checkTime = System.currentTimeMillis(),
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
        name = name ?: this.name,
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