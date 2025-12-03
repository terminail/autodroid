// MyApplication.kt
package com.autodroid.manager

import android.app.Application
import com.autodroid.manager.service.DiscoveryStatusManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize the DiscoveryStatusManager with application context
        DiscoveryStatusManager.initialize(this)
        
        // Auto-start NetworkService for mDNS discovery
        DiscoveryStatusManager.startNetworkService()
    }

    companion object {
        var instance: MyApplication? = null
            private set
    }
}