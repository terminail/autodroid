package com.autodroid.manager.model

/**
 * 网络信息封装类
 * 封装网络连接相关的属性和状态
 */
data class Network(
    val isConnected: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val ipAddress: String? = null,
    val networkName: String? = null,
    val isMetered: Boolean = false,
    val isRoaming: Boolean = false,
    val isFailover: Boolean = false,
    val isAvailable: Boolean = false
) {
    enum class ConnectionType {
        NONE, WIFI, MOBILE, ETHERNET, VPN, BLUETOOTH, OTHER
    }
    
    companion object {
        /**
         * 创建空网络信息
         */
        fun empty(): Network = Network()
        
        /**
         * 创建WiFi连接的网络信息
         */
        fun wifiConnected(ipAddress: String, networkName: String? = null): Network = Network(
            isConnected = true,
            connectionType = ConnectionType.WIFI,
            ipAddress = ipAddress,
            networkName = networkName,
            isAvailable = true
        )
        
        /**
         * 创建移动数据连接的网络信息
         */
        fun mobileConnected(ipAddress: String, isRoaming: Boolean = false): Network = Network(
            isConnected = true,
            connectionType = ConnectionType.MOBILE,
            ipAddress = ipAddress,
            isRoaming = isRoaming,
            isAvailable = true
        )
        
        /**
         * 创建详细网络信息
         */
        fun detailed(
            isConnected: Boolean,
            connectionType: ConnectionType,
            ipAddress: String?,
            networkName: String?,
            isMetered: Boolean,
            isRoaming: Boolean,
            isFailover: Boolean
        ): Network = Network(
            isConnected = isConnected,
            connectionType = connectionType,
            ipAddress = ipAddress,
            networkName = networkName,
            isMetered = isMetered,
            isRoaming = isRoaming,
            isFailover = isFailover,
            isAvailable = isConnected
        )
    }
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean = isConnected && isAvailable
    
    /**
     * 获取连接类型描述
     */
    fun getConnectionTypeDescription(): String = when (connectionType) {
        ConnectionType.WIFI -> "WiFi"
        ConnectionType.MOBILE -> "移动数据"
        ConnectionType.ETHERNET -> "以太网"
        ConnectionType.VPN -> "VPN"
        ConnectionType.BLUETOOTH -> "蓝牙"
        ConnectionType.OTHER -> "其他"
        ConnectionType.NONE -> "无连接"
    }
    
    /**
     * 断开网络连接
     */
    fun disconnected(): Network = this.copy(
        isConnected = false,
        isAvailable = false
    )
    
    /**
     * 更新连接状态
     */
    fun updateConnectionStatus(
        isConnected: Boolean,
        connectionType: ConnectionType
    ): Network = this.copy(
        isConnected = isConnected,
        connectionType = connectionType,
        isAvailable = isConnected
    )
}