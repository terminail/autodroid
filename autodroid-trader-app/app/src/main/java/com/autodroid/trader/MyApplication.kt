package com.autodroid.trader

import android.app.Application
import com.autodroid.trader.config.ConfigManager

class MyApplication : Application() {
    
    companion object {
        @Volatile
        private var instance: MyApplication? = null
        
        fun getInstance(): MyApplication {
            return instance ?: synchronized(this) {
                instance ?: MyApplication().also { instance = it }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Load configuration when application starts
        ConfigManager.loadConfig(this)
    }

    // Helper method to get AppViewModel instance
    fun getAppViewModel(): AppViewModel {
        val appViewModel = AppViewModel.getInstance(this)
        // Ensure AppViewModel is properly initialized
        if (!appViewModel.isInitialized()) {
            appViewModel.initialize(this)
        }
        return appViewModel
    }
}