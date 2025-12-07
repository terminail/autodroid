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
    val apiEndpoint: String? = null,
    val services: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap(),
    val connected: Boolean = false,

    val discoveryMethod: String? = null,
    val supportsDeviceRegistration: Boolean = false,
    val supportsApkManagement: Boolean = false,
    val supportsWorkflowExecution: Boolean = false
) {
    /**
     * Get the API endpoint URL
     */
    fun getApiEndpointUrl(): String {
        return apiEndpoint ?: throw IllegalArgumentException("服务器API端点不能为空")
    }
    
    /**
     * Check if the server information is valid for connection
     * A server is valid if it has a valid API endpoint
     */
    fun isValid(): Boolean {
        val apiEndpoint = getApiEndpointUrl()
        return apiEndpoint.isNotBlank() && apiEndpoint.startsWith("http")
    }
    
    /**
     * Check if the server info is enriched with API data
     */
    fun isEnriched(): Boolean {
        return name != null && hostname != null && apiEndpoint != null
    }
    
    /**
     * Create an enriched Server from ServerInfoResponse
     */
    fun enrichWithServerInfoResponse(serverInfoResponse: ServerInfoResponse): Server {
        return this.copy(
            name = serverInfoResponse.name,
            hostname = serverInfoResponse.hostname,
            platform = serverInfoResponse.platform,
            apiEndpoint = serverInfoResponse.apiEndpoint,
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
            "platform='$platform', apiEndpoint='$apiEndpoint')"
        } else {
            "ServerInfo(serviceName='$serviceName')"
        }
    }
}