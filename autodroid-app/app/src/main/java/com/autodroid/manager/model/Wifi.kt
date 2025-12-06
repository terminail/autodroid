package com.autodroid.manager.model

/**
 * WiFi信息封装类
 * 封装WiFi网络相关的属性和状态
 */
data class Wifi(
    val ssid: String? = null,
    val bssid: String? = null,
    val signalStrength: Int? = null,
    val frequency: Int? = null,
    val ipAddress: String? = null,
    val isConnected: Boolean = false,
    val securityType: String? = null,
    val linkSpeed: Int? = null
) {
    companion object {
        /**
         * 创建空WiFi信息
         */
        fun empty(): Wifi = Wifi()
        
        /**
         * 创建连接状态的WiFi信息
         */
        fun connected(
            ssid: String,
            ipAddress: String,
            signalStrength: Int? = null
        ): Wifi = Wifi(
            ssid = ssid,
            ipAddress = ipAddress,
            signalStrength = signalStrength,
            isConnected = true
        )
        
        /**
         * 创建详细WiFi信息
         */
        fun detailed(
            ssid: String,
            bssid: String,
            signalStrength: Int,
            frequency: Int,
            ipAddress: String,
            securityType: String,
            linkSpeed: Int
        ): Wifi = Wifi(
            ssid = ssid,
            bssid = bssid,
            signalStrength = signalStrength,
            frequency = frequency,
            ipAddress = ipAddress,
            isConnected = true,
            securityType = securityType,
            linkSpeed = linkSpeed
        )
    }
    
    /**
     * 检查WiFi是否连接
     */
    fun isWifiConnected(): Boolean = isConnected && ssid != null
    
    /**
     * 获取信号强度描述
     */
    fun getSignalStrengthDescription(): String {
        return when (signalStrength) {
            null -> "未知"
            in -30..0 -> "极强"
            in -67..-31 -> "强"
            in -70..-68 -> "一般"
            in -80..-71 -> "弱"
            else -> "极弱"
        }
    }
    
    /**
     * 断开WiFi连接
     */
    fun disconnected(): Wifi = this.copy(
        isConnected = false
    )
    
    /**
     * 更新信号强度
     */
    fun updateSignalStrength(strength: Int): Wifi = this.copy(
        signalStrength = strength
    )
}