package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.repository.SettingsRepository
import com.autodroid.teachitback.ui.adapter.SettingsItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SettingsFragment的ViewModel
 * 负责管理设置界面的所有业务逻辑和数据
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val settingsRepository = SettingsRepository(database.settingDao())
    
    private val _settingsItems = MutableLiveData<List<SettingsItem>>()
    val settingsItems: LiveData<List<SettingsItem>> = _settingsItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        loadSettings()
    }
    
    /**
     * 加载设置项
     */
    fun loadSettings() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val allSettings = settingsRepository.getAllSettings().first()
                val aiServiceItems = allSettings.filter { item ->
                    item is SettingsItem.DoubaoAIServiceItem ||
                    item is SettingsItem.ErnieAIServiceItem ||
                    item is SettingsItem.QwenAIServiceItem ||
                    item is SettingsItem.DeepSeekAIServiceItem ||
                    item is SettingsItem.ZhipuAIServiceItem ||
                    item is SettingsItem.SparkAIServiceItem ||
                    item is SettingsItem.MinimaxAIServiceItem ||
                    item is SettingsItem.KimiAIServiceItem ||
                    item is SettingsItem.HunyuanAIServiceItem ||
                    item is SettingsItem.BaichuanAIServiceItem ||
                    item is SettingsItem.LingyiAIServiceItem ||
                    item is SettingsItem.JieyueAIServiceItem ||
                    item is SettingsItem.OpenAIServiceItem ||
                    item is SettingsItem.TencentCloudApiKeyItem
                }

                // 如果数据库中没有数据，使用默认设置项
                val items = if (aiServiceItems.isEmpty()) {
                    buildDefaultSettingsItems()
                } else {
                    buildSettingsItems(aiServiceItems)
                }

                _settingsItems.value = items
            } catch (e: Exception) {
                _errorMessage.value = "加载设置失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    

    /**
     * 构建设置项列表
     */
    private fun buildSettingsItems(aiServiceItems: List<SettingsItem> = emptyList()): List<SettingsItem> {
        val items = mutableListOf<SettingsItem>()
        
        // AI服务设置 - 从数据库加载
        items.add(SettingsItem.SectionHeaderItem("AI服务设置"))
        
        val aiServiceMap = aiServiceItems.associateBy {
            when (it) {
                is SettingsItem.TencentCloudApiKeyItem -> "tencentcloud"
                is SettingsItem.DoubaoAIServiceItem -> "doubao"
                is SettingsItem.ErnieAIServiceItem -> "ernie"
                is SettingsItem.QwenAIServiceItem -> "qwen"
                is SettingsItem.DeepSeekAIServiceItem -> "deepseek"
                is SettingsItem.ZhipuAIServiceItem -> "zhipu"
                is SettingsItem.SparkAIServiceItem -> "spark"
                is SettingsItem.MinimaxAIServiceItem -> "minimax"
                is SettingsItem.KimiAIServiceItem -> "kimi"
                is SettingsItem.HunyuanAIServiceItem -> "hunyuan"
                is SettingsItem.BaichuanAIServiceItem -> "baichuan"
                is SettingsItem.LingyiAIServiceItem -> "lingyi"
                is SettingsItem.JieyueAIServiceItem -> "jieyue"
                is SettingsItem.OpenAIServiceItem -> "openai"
                else -> ""
            }
        }
        
        items.add(aiServiceMap["tencentcloud"] as? SettingsItem.TencentCloudApiKeyItem ?: SettingsItem.TencentCloudApiKeyItem())
        items.add(aiServiceMap["openai"] as? SettingsItem.OpenAIServiceItem ?: SettingsItem.OpenAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["deepseek"] as? SettingsItem.DeepSeekAIServiceItem ?: SettingsItem.DeepSeekAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["ernie"] as? SettingsItem.ErnieAIServiceItem ?: SettingsItem.ErnieAIServiceItem(isEnabled = false))
        items.add(aiServiceMap["qwen"] as? SettingsItem.QwenAIServiceItem ?: SettingsItem.QwenAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["doubao"] as? SettingsItem.DoubaoAIServiceItem ?: SettingsItem.DoubaoAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["minimax"] as? SettingsItem.MinimaxAIServiceItem ?: SettingsItem.MinimaxAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["kimi"] as? SettingsItem.KimiAIServiceItem ?: SettingsItem.KimiAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["zhipu"] as? SettingsItem.ZhipuAIServiceItem ?: SettingsItem.ZhipuAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["spark"] as? SettingsItem.SparkAIServiceItem ?: SettingsItem.SparkAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["hunyuan"] as? SettingsItem.HunyuanAIServiceItem ?: SettingsItem.HunyuanAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["baichuan"] as? SettingsItem.BaichuanAIServiceItem ?: SettingsItem.BaichuanAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["lingyi"] as? SettingsItem.LingyiAIServiceItem ?: SettingsItem.LingyiAIServiceItem(isEnabled = true))
        items.add(aiServiceMap["jieyue"] as? SettingsItem.JieyueAIServiceItem ?: SettingsItem.JieyueAIServiceItem(isEnabled = true))
        
        // 应用设置
        items.add(SettingsItem.SectionHeaderItem("应用设置"))
        items.add(SettingsItem.DarkModeSwitchItem(
            isChecked = false
        ))
        items.add(SettingsItem.AutoSaveSwitchItem(
            isChecked = true
        ))
        items.add(SettingsItem.LanguageSettingItem())
        
        // 数据管理
        items.add(SettingsItem.SectionHeaderItem("数据管理"))
        items.add(SettingsItem.BackupDataItem())
        items.add(SettingsItem.RestoreDataItem())
        items.add(SettingsItem.ClearAllDataButtonItem())
        
        // 关于
        items.add(SettingsItem.SectionHeaderItem("关于"))
        items.add(SettingsItem.VersionInfoItem())
        items.add(SettingsItem.HelpAndFeedbackItem())
        
        return items
    }
    
    /**
     * 更新开关设置
     */
    fun updateSwitchSetting(settingType: String, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                when (settingType) {
                    "dark_mode" -> {
                        val item = SettingsItem.DarkModeSwitchItem(isChecked = isChecked)
                        settingsRepository.saveSetting("dark_mode", item)
                    }
                    "auto_save" -> {
                        val item = SettingsItem.AutoSaveSwitchItem(isChecked = isChecked)
                        settingsRepository.saveSetting("auto_save", item)
                    }
                }
                loadSettings() // 重新加载以反映更改
            } catch (e: Exception) {
                _errorMessage.value = "更新设置失败: ${e.message}"
            }
        }
    }
    
    /**
     * 切换AI服务状态
     */
    fun toggleAIService(serviceName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                val existingItem = settingsRepository.getSettingByKeySync("ai_$serviceName")
                val item = when (serviceName) {
                    "doubao" -> (existingItem as? SettingsItem.DoubaoAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.DoubaoAIServiceItem(isEnabled = isEnabled)
                    "deepseek" -> (existingItem as? SettingsItem.DeepSeekAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.DeepSeekAIServiceItem(isEnabled = isEnabled)
                    "minimax" -> (existingItem as? SettingsItem.MinimaxAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.MinimaxAIServiceItem(isEnabled = isEnabled)
                    "kimi" -> (existingItem as? SettingsItem.KimiAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.KimiAIServiceItem(isEnabled = isEnabled)
                    "openai" -> (existingItem as? SettingsItem.OpenAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.OpenAIServiceItem(isEnabled = isEnabled)
                    "ernie" -> (existingItem as? SettingsItem.ErnieAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.ErnieAIServiceItem(isEnabled = isEnabled)
                    "qwen" -> (existingItem as? SettingsItem.QwenAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.QwenAIServiceItem(isEnabled = isEnabled)
                    "zhipu" -> (existingItem as? SettingsItem.ZhipuAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.ZhipuAIServiceItem(isEnabled = isEnabled)
                    "spark" -> (existingItem as? SettingsItem.SparkAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.SparkAIServiceItem(isEnabled = isEnabled)
                    "hunyuan" -> (existingItem as? SettingsItem.HunyuanAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.HunyuanAIServiceItem(isEnabled = isEnabled)
                    "baichuan" -> (existingItem as? SettingsItem.BaichuanAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.BaichuanAIServiceItem(isEnabled = isEnabled)
                    "lingyi" -> (existingItem as? SettingsItem.LingyiAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.LingyiAIServiceItem(isEnabled = isEnabled)
                    "jieyue" -> (existingItem as? SettingsItem.JieyueAIServiceItem)?.copy(isEnabled = isEnabled) 
                        ?: SettingsItem.JieyueAIServiceItem(isEnabled = isEnabled)
                    else -> return@launch
                }
                settingsRepository.saveSetting("ai_$serviceName", item)
                loadSettings() // 重新加载以反映更改
            } catch (e: Exception) {
                _errorMessage.value = "切换AI服务失败: ${e.message}"
            }
        }
    }
    
    /**
     * 构建默认设置项（用于首次启动或数据库为空时）
     */
    private fun buildDefaultSettingsItems(): List<SettingsItem> {
        val items = mutableListOf<SettingsItem>()

        // AI服务设置 - 默认配置
        items.add(SettingsItem.SectionHeaderItem("AI服务设置"))
        items.add(SettingsItem.TencentCloudApiKeyItem())  // 腾讯云知识引擎
        items.add(SettingsItem.OpenAIServiceItem(isEnabled = true))
        items.add(SettingsItem.DeepSeekAIServiceItem(isEnabled = true))
        items.add(SettingsItem.QwenAIServiceItem(isEnabled = true))
        items.add(SettingsItem.DoubaoAIServiceItem(isEnabled = true))
        items.add(SettingsItem.MinimaxAIServiceItem(isEnabled = true))
        items.add(SettingsItem.KimiAIServiceItem(isEnabled = true))
        items.add(SettingsItem.ZhipuAIServiceItem(isEnabled = true))
        items.add(SettingsItem.SparkAIServiceItem(isEnabled = true))
        items.add(SettingsItem.HunyuanAIServiceItem(isEnabled = true))
        items.add(SettingsItem.BaichuanAIServiceItem(isEnabled = true))
        items.add(SettingsItem.LingyiAIServiceItem(isEnabled = true))
        items.add(SettingsItem.JieyueAIServiceItem(isEnabled = true))
        items.add(SettingsItem.ErnieAIServiceItem(isEnabled = false))

        // 应用设置
        items.add(SettingsItem.SectionHeaderItem("应用设置"))
        items.add(SettingsItem.DarkModeSwitchItem(isChecked = false))
        items.add(SettingsItem.AutoSaveSwitchItem(isChecked = true))
        items.add(SettingsItem.LanguageSettingItem())

        // 数据管理
        items.add(SettingsItem.SectionHeaderItem("数据管理"))
        items.add(SettingsItem.BackupDataItem())
        items.add(SettingsItem.RestoreDataItem())
        items.add(SettingsItem.ClearAllDataButtonItem())

        // 关于
        items.add(SettingsItem.SectionHeaderItem("关于"))
        items.add(SettingsItem.VersionInfoItem())
        items.add(SettingsItem.HelpAndFeedbackItem())

        return items
    }

    /**
     * 执行数据清除操作
     */
    fun clearAllData() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                settingsRepository.deleteAllSettings()
                _errorMessage.value = "数据清除成功"
            } catch (e: Exception) {
                _errorMessage.value = "清除数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
