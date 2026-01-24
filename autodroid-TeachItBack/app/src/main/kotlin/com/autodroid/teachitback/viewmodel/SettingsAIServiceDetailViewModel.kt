package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.repository.SettingsRepository
import kotlinx.coroutines.launch

/**
 * SettingsAIServiceDetailFragment 的 ViewModel
 * 负责管理 AI 服务配置的保存和加载
 */
class SettingsAIServiceDetailViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository = SettingsRepository(
        com.autodroid.teachitback.database.AppDatabase.getDatabase(application).settingDao()
    )
    
    private val _savedConfig = MutableLiveData<AIServiceConfig?>()
    val savedConfig: LiveData<AIServiceConfig?> = _savedConfig
    
    private val _saveResult = MutableLiveData<SaveResult>()
    val saveResult: LiveData<SaveResult> = _saveResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * 加载已保存的 AI 服务配置
     */
    fun loadSavedConfig(configId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val config = settingsRepository.getAIServiceConfig(configId)
                _savedConfig.value = config
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("加载配置失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 保存 AI 服务配置
     */
    fun saveConfig(config: AIServiceConfig) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                settingsRepository.saveAIServiceConfig(config)
                _saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("保存配置失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除保存结果
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }
    
    /**
     * 保存结果密封类
     */
    sealed class SaveResult {
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}