package com.autodroid.trader.network

/**
 * Data class representing server information response from FastAPI
 * This is the type-safe response model for getServerInfo() API call
 */
data class ServerInfoResponse(
    val apiEndpoint: String,
    val name: String? = null,
    val hostname: String? = null,
    val ipAddress: String? = null,
    val platform: String? = null,
    val services: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap()
) {
    /**
     * Check if the server info response is valid
     */
    fun isValid(): Boolean {
        return name != null && hostname != null && ipAddress != null
    }
    
    /**
     * Check if device registration capability is enabled
     */
    fun supportsDeviceRegistration(): Boolean {
        return capabilities["device_registration"] ?: false
    }
    
    /**
     * Check if trade plan execution capability is enabled
     */
    fun supportsTradePlanExecution(): Boolean {
        return capabilities["tradeplan_execution"] ?: false
    }
    
    /**
     * Check if trade scheduling capability is enabled
     */
    fun supportsTradeScheduling(): Boolean {
        return capabilities["trade_scheduling"] ?: false
    }
    
    /**
     * Check if event triggering capability is enabled
     */
    fun supportsEventTriggering(): Boolean {
        return capabilities["event_triggering"] ?: false
    }
    
    override fun toString(): String {
        return "ServerInfoResponse(name='$name', hostname='$hostname', ipAddress='$ipAddress', " +
                "platform='$platform', apiEndpoint='$apiEndpoint', services=$services, capabilities=$capabilities)"
    }
}