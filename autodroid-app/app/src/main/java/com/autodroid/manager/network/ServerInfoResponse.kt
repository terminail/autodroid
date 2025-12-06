package com.autodroid.manager.network

/**
 * Data class representing server information response from FastAPI
 * This is the type-safe response model for getServerInfo() API call
 */
data class ServerInfoResponse(
    val name: String? = null,
    val hostname: String? = null,
    val ip_address: String? = null,
    val platform: String? = null,
    val api_endpoint: String? = null,
    val services: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap()
) {
    /**
     * Check if the server info response is valid
     */
    fun isValid(): Boolean {
        return name != null && hostname != null && ip_address != null && api_endpoint != null
    }
    
    /**
     * Check if device registration capability is enabled
     */
    fun supportsDeviceRegistration(): Boolean {
        return capabilities["device_registration"] ?: false
    }
    
    /**
     * Check if workflow execution capability is enabled
     */
    fun supportsWorkflowExecution(): Boolean {
        return capabilities["workflow_execution"] ?: false
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
    
    /**
     * Get the API endpoint URL
     */
    fun getApiEndpoint(): String {
        return api_endpoint ?: ""
    }
    
    override fun toString(): String {
        return "ServerInfoResponse(name='$name', hostname='$hostname', ip_address='$ip_address', " +
               "platform='$platform', api_endpoint='$api_endpoint', " +
               "services=$services, capabilities=$capabilities)"
    }
}