package com.autodroid.trader

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.config.ConfigManager
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.data.repository.ServerRepository
import com.autodroid.trader.network.ApiClient
import com.google.android.gms.common.api.Api


class MyApplication : Application() {
    @Volatile
    private var apiEndpoint: String? = null

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

    fun setApiEndpoint(apiEndpoint: String): ApiClient? {
        Log.i("MyApplication", "Set apiEndpoint=${apiEndpoint}")
        this.apiEndpoint = apiEndpoint
        return getApiClient()
    }

    fun getApiClient(): ApiClient? {
        return if (!apiEndpoint.isNullOrEmpty()) {
            ApiClient.getInstance().setApiEndpoint(apiEndpoint!!)
        } else
            null
    }
}