package com.autodroid.manager.model

import com.autodroid.manager.network.ServerInfoResponse

/**
 * Data class representing server information with API endpoint
 * This model focuses on API endpoint for server communication
 */
data class Server(
    val serviceName: String,
    val name: String? = null,
    val hostname: String? = null,
    val platform: String? = null,
    val api_endpoint: String? = null,
    val services: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap(),
    val connected: Boolean = false,
    val ip: String? = null,
    val port: Int? = null,
    val discoveryMethod: String? = null
) {
    /**
     * Get the API endpoint URL
     */
    fun getApiEndpoint(): String {
        return api_endpoint ?: ""
    }
    
    /**
     * Check if the server information is valid for connection
     * A server is valid if it has a valid API endpoint
     */
    fun isValid(): Boolean {
        val apiEndpoint = getApiEndpoint()
        return apiEndpoint.isNotBlank() && apiEndpoint.startsWith("http")
    }
    
    /**
     * Check if the server info is enriched with API data
     */
    fun isEnriched(): Boolean {
        return name != null && hostname != null && api_endpoint != null
    }
    
    /**
     * Create an enriched Server from ServerInfoResponse
     */
    fun enrichWithServerInfoResponse(serverInfoResponse: ServerInfoResponse): Server {
        return this.copy(
            name = serverInfoResponse.name,
            hostname = serverInfoResponse.hostname,
            platform = serverInfoResponse.platform,
            api_endpoint = serverInfoResponse.api_endpoint,
            services = serverInfoResponse.services,
            capabilities = serverInfoResponse.capabilities
        )
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
    
    override fun toString(): String {
        return if (isEnriched()) {
            "ServerInfo(serviceName='$serviceName', name='$name', hostname='$hostname', " +
            "platform='$platform', api_endpoint='$api_endpoint')"
        } else {
            "ServerInfo(serviceName='$serviceName')"
        }
    }
}