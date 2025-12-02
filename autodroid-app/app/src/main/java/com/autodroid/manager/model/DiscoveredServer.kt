package com.autodroid.manager.model

/**
 * Data class representing a discovered server via mDNS
 */
data class DiscoveredServer(
    val serviceName: String,
    val host: String,
    val port: Int,
    val discoveredTime: Long = System.currentTimeMillis()
) {
    // Get a display name for the server
    val displayName: String
        get() = serviceName.split(".")[0]
}