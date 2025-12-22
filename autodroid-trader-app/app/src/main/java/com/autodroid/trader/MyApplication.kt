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
        Log.d("MyApplication", "onCreate: 应用程序启动")
        // Load configuration when application starts
        ConfigManager.loadConfig(this)
        
        // 确保AppViewModel在应用启动时就完全初始化
        Log.d("MyApplication", "onCreate: 开始初始化 AppViewModel")
        val appViewModel = getAppViewModel()
        if (!appViewModel.isInitialized()) {
            Log.d("MyApplication", "onCreate: AppViewModel 未初始化，开始初始化")
            appViewModel.initialize(this)
            Log.d("MyApplication", "onCreate: AppViewModel 初始化完成")
        } else {
            Log.d("MyApplication", "onCreate: AppViewModel 已初始化")
        }
        Log.d("MyApplication", "onCreate: 应用程序启动完成")
    }

    // Helper method to get AppViewModel instance
    fun getAppViewModel(): AppViewModel {
        Log.d("MyApplication", "getAppViewModel: 获取 AppViewModel 实例")
        val appViewModel = AppViewModel.getInstance(this)
        // Ensure AppViewModel is properly initialized
        if (!appViewModel.isInitialized()) {
            Log.d("MyApplication", "getAppViewModel: AppViewModel 未初始化，开始初始化")
            appViewModel.initialize(this)
            Log.d("MyApplication", "getAppViewModel: AppViewModel 初始化完成")
        } else {
            Log.d("MyApplication", "getAppViewModel: AppViewModel 已初始化")
        }
        return appViewModel
    }

    fun setApiEndpoint(apiEndpoint: String): ApiClient? {
        Log.i("MyApplication", "Set apiEndpoint=${apiEndpoint}")
        this.apiEndpoint = apiEndpoint
        return getApiClient()
    }

    fun getApiClient(): ApiClient? {
        Log.i("MyApplication", "getApiClient apiEndpoint=${apiEndpoint}")
        return if (apiEndpoint.isNullOrEmpty()) {
            null
        } else{
            ApiClient.getInstance().setApiEndpoint(apiEndpoint!!)
        }
    }
}