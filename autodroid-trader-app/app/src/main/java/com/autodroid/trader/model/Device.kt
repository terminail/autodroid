package com.autodroid.trader.model

/**
 * 设备信息封装类
 * 封装设备相关的属性和状态
 */
data class Device(
    val ip: String? = null,
    val name: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    val androidVersion: String? = null,
    val platform: String? = null,
    val brand: String? = null,
    val device: String? = null,
    val product: String? = null,
    val isConnected: Boolean = false,
    val lastSeen: Long? = null
) {
    companion object {
        /**
         * 创建空设备信息
         */
        fun empty(): Device = Device()
        
        /**
         * 创建连接状态的设备信息
         */
        fun connected(ip: String, name: String? = null): Device = Device(
            ip = ip,
            name = name,
            isConnected = true,
            lastSeen = System.currentTimeMillis()
        )
        
        /**
         * 创建详细设备信息
         */
        fun detailed(
            ip: String,
            name: String,
            model: String,
            manufacturer: String,
            androidVersion: String,
            platform: String = "Android",
            brand: String = "",
            device: String = "",
            product: String = ""
        ): Device = Device(
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
    fun disconnected(): Device = this.copy(
        isConnected = false,
        lastSeen = System.currentTimeMillis()
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
    ): Device = this.copy(
        name = name,
        model = model,
        manufacturer = manufacturer,
        androidVersion = androidVersion,
        platform = platform,
        brand = brand,
        device = device,
        product = product
    )
}