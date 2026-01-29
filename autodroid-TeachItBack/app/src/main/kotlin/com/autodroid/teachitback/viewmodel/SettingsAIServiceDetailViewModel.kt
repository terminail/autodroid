package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.config.AIServiceConfig
import kotlinx.coroutines.launch

/**
 * SettingsAIServiceDetailFragment 的 ViewModel
 * 负责管理 AI 服务配置的保存和加载
 * 
 * 传统方式：不使用 LiveData，使用回调接口
 */
class SettingsAIServiceDetailViewModel(application: Application) : AndroidViewModel(application) {
    
    // 使用共享的 Repository（从 Application 获取）
    private val settingsRepository = (application as com.autodroid.teachitback.TeachItBackApplication).getSettingsRepository()
    
    // 当前服务的配置 ID
    private lateinit var currentConfigId: String
    
    /**
     * 初始化配置 ID
     */
    fun initConfigId(configId: String) {
        currentConfigId = configId
    }
    
    /**
     * 加载配置 - 同步方法，由 Fragment 在需要时调用
     */
    suspend fun loadConfig(): AIServiceConfig? {
        return settingsRepository.loadAIServiceConfig(currentConfigId)
    }
    
    /**
     * 保存 AI 服务配置
     * 
     * @param config 要保存的配置
     * @param onSuccess 成功回调
     * @param onError 错误回调
     * @param onLoading 加载状态回调
     */
    fun saveConfig(
        config: AIServiceConfig,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {
        onLoading(true)
        viewModelScope.launch {
            try {
                settingsRepository.saveAIServiceConfig(config)
                onSuccess()
            } catch (e: Exception) {
                onError("保存配置失败: ${e.message}")
            } finally {
                onLoading(false)
            }
        }
    }
}
