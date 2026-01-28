package com.autodroid.teachitback.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.repository.SettingsRepository
import com.autodroid.teachitback.repository.TopicRepository
import com.autodroid.teachitback.ui.adapter.SettingsItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SettingsFragment的ViewModel
 * 负责管理设置界面的所有业务逻辑和数据
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository（通过应用上下文获取，ViewModel不感知数据库创建细节）
    private val settingsRepository = (application as com.autodroid.teachitback.TeachItBackApplication).getSettingsRepository()
    private val topicRepository = (application as com.autodroid.teachitback.TeachItBackApplication).getTopicRepository()
    
    private val _settingsItems = MutableLiveData<List<SettingsItem>>()
    val settingsItems: LiveData<List<SettingsItem>> = _settingsItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // AI 服务配置相关
    private val _aiServiceConfigs = MutableLiveData<Map<String, AIServiceConfig>>()
    val aiServiceConfigs: LiveData<Map<String, AIServiceConfig>> = _aiServiceConfigs

    init {
        loadSettings()
        loadAIServiceConfigs()
        observeAIServiceConfigs()
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
                    item is SettingsItem.ChatGLMAIServiceItem ||
                    item is SettingsItem.TinyBERTAIServiceItem ||
                    item is SettingsItem.TencentCloudAIServiceItem
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
        
        // 获取当前AI服务配置状态 - 始终从AIServiceConfig获取最新状态
        val currentConfigs = _aiServiceConfigs.value ?: emptyMap()
        
        // 创建AI服务Map，使用AIServiceConfig的isEnabled状态
        val aiServiceMap = mutableMapOf<String, SettingsItem>()
        
        // 添加所有AI服务项，始终使用AIServiceConfig中的isEnabled状态
        aiServiceMap["tencentcloud"] = SettingsItem.TencentCloudAIServiceItem(
            enabled = currentConfigs["tencent-hunyuan"]?.isEnabled == true
        )
        aiServiceMap["openai"] = SettingsItem.OpenAIServiceItem(
            isEnabled = currentConfigs["openai"]?.isEnabled == true
        )
        aiServiceMap["deepseek"] = SettingsItem.DeepSeekAIServiceItem(
            isEnabled = currentConfigs["deepseek"]?.isEnabled == true
        )
        aiServiceMap["ernie"] = SettingsItem.ErnieAIServiceItem(
            isEnabled = currentConfigs["ernie"]?.isEnabled == true
        )
        aiServiceMap["qwen"] = SettingsItem.QwenAIServiceItem(
            isEnabled = currentConfigs["qwen"]?.isEnabled == true
        )
        aiServiceMap["doubao"] = SettingsItem.DoubaoAIServiceItem(
            isEnabled = currentConfigs["doubao"]?.isEnabled == true
        )
        aiServiceMap["minimax"] = SettingsItem.MinimaxAIServiceItem(
            isEnabled = currentConfigs["minimax"]?.isEnabled == true
        )
        aiServiceMap["kimi"] = SettingsItem.KimiAIServiceItem(
            isEnabled = currentConfigs["kimi"]?.isEnabled == true
        )
        aiServiceMap["zhipu"] = SettingsItem.ZhipuAIServiceItem(
            isEnabled = currentConfigs["zhipu"]?.isEnabled == true
        )
        aiServiceMap["spark"] = SettingsItem.SparkAIServiceItem(
            isEnabled = currentConfigs["spark"]?.isEnabled == true
        )
        aiServiceMap["hunyuan"] = SettingsItem.HunyuanAIServiceItem(
            isEnabled = currentConfigs["hunyuan"]?.isEnabled == true
        )
        aiServiceMap["baichuan"] = SettingsItem.BaichuanAIServiceItem(
            isEnabled = currentConfigs["baichuan"]?.isEnabled == true
        )
        aiServiceMap["lingyi"] = SettingsItem.LingyiAIServiceItem(
            isEnabled = currentConfigs["lingyi"]?.isEnabled == true
        )
        aiServiceMap["jieyue"] = SettingsItem.JieyueAIServiceItem(
            isEnabled = currentConfigs["jieyue"]?.isEnabled == true
        )
        aiServiceMap["chatglm"] = SettingsItem.ChatGLMAIServiceItem(
            isEnabled = currentConfigs["chatglm"]?.isEnabled == true
        )
        aiServiceMap["tinybert"] = SettingsItem.TinyBERTAIServiceItem(
             isEnabled = currentConfigs["tinybert_local"]?.isEnabled == true
         )
         
         // 对AI服务项进行排序：已启用的排在前面
         val sortedAIServices = aiServiceMap.values.sortedWith(
             compareByDescending<SettingsItem> { item ->
                 when (item) {
                     is SettingsItem.TencentCloudAIServiceItem -> item.enabled
                     is SettingsItem.OpenAIServiceItem -> item.isEnabled
                     is SettingsItem.DeepSeekAIServiceItem -> item.isEnabled
                     is SettingsItem.ErnieAIServiceItem -> item.isEnabled
                     is SettingsItem.QwenAIServiceItem -> item.isEnabled
                     is SettingsItem.DoubaoAIServiceItem -> item.isEnabled
                     is SettingsItem.MinimaxAIServiceItem -> item.isEnabled
                     is SettingsItem.KimiAIServiceItem -> item.isEnabled
                     is SettingsItem.ZhipuAIServiceItem -> item.isEnabled
                     is SettingsItem.SparkAIServiceItem -> item.isEnabled
                     is SettingsItem.HunyuanAIServiceItem -> item.isEnabled
                     is SettingsItem.BaichuanAIServiceItem -> item.isEnabled
                     is SettingsItem.LingyiAIServiceItem -> item.isEnabled
                     is SettingsItem.JieyueAIServiceItem -> item.isEnabled
                     is SettingsItem.ChatGLMAIServiceItem -> item.isEnabled
                     is SettingsItem.TinyBERTAIServiceItem -> item.isEnabled
                     else -> false
                 }
             }
         )
         
         // 添加排序后的AI服务项到items列表
         items.addAll(sortedAIServices)
        
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
                        settingsRepository.saveSetting(item)
                    }
                    "auto_save" -> {
                        val item = SettingsItem.AutoSaveSwitchItem(isChecked = isChecked)
                        settingsRepository.saveSetting(item)
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
                // 获取现有的 AI 服务配置
                val existingConfig = settingsRepository.loadAIServiceConfig(serviceName)
                
                // 更新配置的 isEnabled 状态
                val updatedConfig = when (serviceName) {
                    "doubao" -> (existingConfig as? AIServiceConfig.DoubaoConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.DoubaoConfig(isEnabled = isEnabled)
                    "deepseek" -> (existingConfig as? AIServiceConfig.DeepSeekConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.DeepSeekConfig(isEnabled = isEnabled)
                    "minimax" -> (existingConfig as? AIServiceConfig.MiniMaxConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.MiniMaxConfig(isEnabled = isEnabled)
                    "kimi" -> (existingConfig as? AIServiceConfig.KimiConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.KimiConfig(isEnabled = isEnabled)
                    "openai" -> (existingConfig as? AIServiceConfig.OpenAIConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.OpenAIConfig(isEnabled = isEnabled)
                    "ernie" -> (existingConfig as? AIServiceConfig.ErnieConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.ErnieConfig(isEnabled = isEnabled)
                    "qwen" -> (existingConfig as? AIServiceConfig.QwenConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.QwenConfig(isEnabled = isEnabled)
                    "zhipu" -> (existingConfig as? AIServiceConfig.ZhipuConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.ZhipuConfig(isEnabled = isEnabled)
                    "spark" -> (existingConfig as? AIServiceConfig.SparkConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.SparkConfig(isEnabled = isEnabled)
                    "hunyuan" -> (existingConfig as? AIServiceConfig.HunyuanConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.HunyuanConfig(isEnabled = isEnabled)
                    "baichuan" -> (existingConfig as? AIServiceConfig.BaichuanConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.BaichuanConfig(isEnabled = isEnabled)
                    "lingyi" -> (existingConfig as? AIServiceConfig.LingyiConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.LingyiConfig(isEnabled = isEnabled)
                    "jieyue" -> (existingConfig as? AIServiceConfig.JieyueConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.JieyueConfig(isEnabled = isEnabled)
                    "chatglm" -> (existingConfig as? AIServiceConfig.ChatGLMConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.ChatGLMConfig(isEnabled = isEnabled)
                    "tinybert" -> (existingConfig as? AIServiceConfig.TinyBERTConfig)?.copy(isEnabled = isEnabled)
                        ?: AIServiceConfig.TinyBERTConfig(isEnabled = isEnabled)
                    else -> return@launch
                }
                
                // 保存更新后的配置
                settingsRepository.saveAIServiceConfig(updatedConfig)
                
                // 重新加载 AI 服务配置
                loadAIServiceConfigs()
                
                // 重新加载设置以反映更改
                loadSettings()
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

        // AI服务设置 - 默认配置（所有服务默认禁用）
        items.add(SettingsItem.SectionHeaderItem("AI服务设置"))
        items.add(SettingsItem.TencentCloudAIServiceItem(enabled = false))
        items.add(SettingsItem.OpenAIServiceItem(isEnabled = false))
        items.add(SettingsItem.DeepSeekAIServiceItem(isEnabled = false))
        items.add(SettingsItem.QwenAIServiceItem(isEnabled = false))
        items.add(SettingsItem.DoubaoAIServiceItem(isEnabled = false))
        items.add(SettingsItem.MinimaxAIServiceItem(isEnabled = false))
        items.add(SettingsItem.KimiAIServiceItem(isEnabled = false))
        items.add(SettingsItem.ZhipuAIServiceItem(isEnabled = false))
        items.add(SettingsItem.SparkAIServiceItem(isEnabled = false))
        items.add(SettingsItem.HunyuanAIServiceItem(isEnabled = false))
        items.add(SettingsItem.BaichuanAIServiceItem(isEnabled = false))
        items.add(SettingsItem.LingyiAIServiceItem(isEnabled = false))
        items.add(SettingsItem.JieyueAIServiceItem(isEnabled = false))
        items.add(SettingsItem.ErnieAIServiceItem(isEnabled = false))
        items.add(SettingsItem.ChatGLMAIServiceItem(isEnabled = false))
        items.add(SettingsItem.TinyBERTAIServiceItem(isEnabled = false))

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
                loadAIServiceConfigs() // 重新加载配置
            } catch (e: Exception) {
                _errorMessage.value = "清除数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载所有 AI 服务配置
     */
    private fun loadAIServiceConfigs() {
        viewModelScope.launch {
            try {
                val configs = mutableMapOf<String, AIServiceConfig>()
                
                // 加载所有支持的 AI 服务配置
                val serviceIds = listOf(
                    "tencent-hunyuan", "deepseek", "kimi", "minimax", "baichuan",
                    "openai", "ernie", "qwen", "zhipu", "spark", "hunyuan",
                    "doubao", "lingyi", "jieyue", "chatglm", "tinybert_local"
                )
                
                serviceIds.forEach { serviceId ->
                    val config = settingsRepository.loadAIServiceConfig(serviceId)
                    config?.let {
                        configs[serviceId] = it
                    }
                }
                
                _aiServiceConfigs.value = configs
            } catch (e: Exception) {
                _errorMessage.value = "加载AI服务配置失败: ${e.message}"
            }
        }
    }
    
    /**
     * 保存 AI 服务配置
     */
    fun saveAIServiceConfig(config: AIServiceConfig) {
        viewModelScope.launch {
            try {
                settingsRepository.saveAIServiceConfig(config)
                loadAIServiceConfigs() // 重新加载配置以更新 UI
            } catch (e: Exception) {
                _errorMessage.value = "保存AI服务配置失败: ${e.message}"
            }
        }
    }
    
    /**
     * 获取特定 AI 服务配置
     */
    fun getAIServiceConfig(configId: String): AIServiceConfig? {
        return _aiServiceConfigs.value?.get(configId)
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 观察 AI 服务配置变化
     */
    private fun observeAIServiceConfigs() {
        _aiServiceConfigs.observeForever { configs ->
            // 当 AI 服务配置加载完成后，重新构建 SettingsItem 以确保状态一致
            if (configs.isNotEmpty()) {
                viewModelScope.launch {
                    loadSettings()
                }
            }
        }
    }
}
