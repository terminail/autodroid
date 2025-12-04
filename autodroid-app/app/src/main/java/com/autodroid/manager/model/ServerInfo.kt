package com.autodroid.manager.model

/**
 * Data class representing server information discovered via mDNS
 */
data class ServerInfo(
    val serviceName: String,
    val host: String,
    val port: Int
) {
    /**
     * Get the full URL for the server
     */
    fun getUrl(): String {
        return "http://$host:$port"
    }
    
    /**
     * Check if the server information is valid
     */
    fun isValid(): Boolean {
        return serviceName.isNotBlank() && host.isNotBlank() && port > 0
    }
    
    override fun toString(): String {
        return "ServerInfo(serviceName='$serviceName', host='$host', port=$port)"
    }
}