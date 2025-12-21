package com.autodroid.trader.network

/**
 * Data class representing server information response from FastAPI
 * This is the type-safe response model for getServerInfo() API call
 * Designed to match ServerEntity structure with IP and port as server location identifiers
 */
data class ServerInfoResponse(
    // 服务器定位信息 - 使用IP和端口定位服务器
    val ip: String,
    val port: Int,
    
    // 服务器基本信息
    val name: String,
    val platform: String,
    
    // 服务器服务和能力
    val services: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap()
) {
    /**
     * Check if device registration capability is enabled
     */
    fun supportsDeviceRegistration(): Boolean {
        return capabilities["device_registration"] ?: false
    }
    
    /**
     * Check if test scheduling capability is enabled
     */
    fun supportsTestScheduling(): Boolean {
        return capabilities["test_scheduling"] ?: false
    }
    
    /**
     * Check if event triggering capability is enabled
     */
    fun supportsEventTriggering(): Boolean {
        return capabilities["event_triggering"] ?: false
    }

    override fun toString(): String {
        return "ServerInfoResponse(ip='$ip', port=$port, name='$name', " +
                "platform='$platform', " +
                "services=$services, capabilities=$capabilities)"
    }
}