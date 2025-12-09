// MyApplication.kt
package com.autodroid.manager

import android.app.Application
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.manager.config.ConfigManager

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Load configuration first before any database operations
        ConfigManager.loadConfig(this)
        
        // Initialize the DiscoveryStatusManager with application context
        DiscoveryStatusManager.initialize(this)
        
        // Do not auto-start NetworkService - mDNS discovery will be started manually by user
    }

    companion object {
        var instance: MyApplication? = null
            private set
    }

    // Helper method to get AppViewModel instance
    fun getAppViewModel(): AppViewModel {
        val appViewModel = AppViewModel.getInstance()
        // Ensure AppViewModel is properly initialized
        if (!appViewModel.isInitialized()) {
            appViewModel.initialize(this)
        }
        return appViewModel
    }
}