// DiscoveredServer.kt
package com.autodroid.manager.model

class DiscoveredServer(// Getters and setters
    @JvmField var name: String?, @JvmField var host: String?, var port: Int
) {
    var discoveredTime: Long

    init {
        this.discoveredTime = System.currentTimeMillis()
    }
}