package com.autodroid.trader.model

/**
 * Data class representing server information with API endpoint
 * This model focuses on API endpoint for server communication
 */
data class Server(
    val apiEndpoint: String,

    val serviceName: String? = null,
    val name: String? = null,
    val hostname: String? = null,
    val platform: String? = null,
    val services: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap(),
    val connected: Boolean = false,

    val discoveryMethod: String? = null,
    val supportsDeviceRegistration: Boolean = false,
    val supportsApkManagement: Boolean = false,
    val supportsTradePlanExecution: Boolean = false
) {
    /**
     * Check if the server information is valid for connection
     * A server is valid if it has a valid API endpoint
     */
    fun isValid(): Boolean {
        return apiEndpoint.isNotBlank() && apiEndpoint.startsWith("http")
    }
    
    /**
     * Check if the server info is enriched with API data
     */
    fun isEnriched(): Boolean {
        return name != null && hostname != null
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
        return if (isEnriched()) {
            "ServerInfo(serviceName='$serviceName', name='$name', hostname='$hostname', " +
            "platform='$platform', apiEndpoint='$apiEndpoint')"
        } else {
            "ServerInfo(serviceName='$serviceName')"
        }
    }
}