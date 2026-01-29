package com.autodroid.teachitback.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.repository.SettingsRepository
import com.autodroid.teachitback.ui.adapter.SettingsItem
import kotlinx.coroutines.launch

/**
 * SettingsFragment的ViewModel
 * 
 * 传统方式：不使用 LiveData，使用回调接口
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository（通过应用上下文获取）
    private val settingsRepository = (application as com.autodroid.teachitback.TeachItBackApplication).getSettingsRepository()

    /**
     * 加载所有设置项（传统方式，一次性查询）
     * 在 onResume 或下拉刷新时调用
     * 
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    fun loadAllSettings(
        onSuccess: (List<SettingsItem>) -> Unit,
        onError: (String) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            onLoading(true)
            try {
                val items = mutableListOf<SettingsItem>()
                
                // 添加分类标题
                items.add(SettingsItem.SectionHeaderItem("AI服务设置"))
                
                // 加载所有 AI 服务配置
                val serviceIds = listOf(
                    "tencent-hunyuan", "openai", "deepseek", "ernie", "qwen", 
                    "doubao", "minimax", "kimi", "zhipu", "spark", 
                    "hunyuan", "baichuan", "lingyi", "jieyue", "chatglm", "tinybert_local"
                )
                
                // 收集所有服务项并排序（启用的排在前面）
                val serviceItems = serviceIds.map { serviceId ->
                    val config = settingsRepository.loadAIServiceConfig(serviceId)
                    if (config != null) {
                        createSettingsItemFromConfig(serviceId, config) to config.isEnabled
                    } else {
                        // 数据库中没有配置，创建默认配置并保存
                        val defaultConfig = createDefaultConfig(serviceId)
                        val item = defaultConfig?.let {
                            settingsRepository.saveAIServiceConfig(it)
                            createSettingsItemFromConfig(serviceId, it)
                        } ?: createInitialSettingsItem(serviceId)
                        item to false
                    }
                }.sortedByDescending { it.second } // 按 isEnabled 降序排序（true 在前）
                
                // 添加排序后的服务项
                serviceItems.forEach { (item, _) ->
                    items.add(item)
                }
                
                // 添加通用设置分类
                items.add(SettingsItem.SectionHeaderItem("通用设置"))
                
                // 加载通用设置
                val darkModeItem = settingsRepository.getSettingByKeySync("dark_mode")
                val isDarkMode = (darkModeItem as? SettingsItem.DarkModeSwitchItem)?.isChecked ?: false
                items.add(SettingsItem.DarkModeSwitchItem(isChecked = isDarkMode))
                
                val autoSaveItem = settingsRepository.getSettingByKeySync("auto_save")
                val isAutoSave = (autoSaveItem as? SettingsItem.AutoSaveSwitchItem)?.isChecked ?: false
                items.add(SettingsItem.AutoSaveSwitchItem(isChecked = isAutoSave))
                
                items.add(SettingsItem.LanguageSettingItem())
                items.add(SettingsItem.BackupDataItem())
                items.add(SettingsItem.RestoreDataItem())
                items.add(SettingsItem.ClearAllDataButtonItem())
                items.add(SettingsItem.VersionInfoItem())
                items.add(SettingsItem.HelpAndFeedbackItem())
                
                onSuccess(items)
                Log.d("SettingsViewModel", "加载了 ${items.size} 个设置项")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "加载设置失败", e)
                onError("加载设置失败: ${e.message}")
            } finally {
                onLoading(false)
            }
        }
    }
    
    /**
     * 从配置创建对应的 SettingsItem
     */
    private fun createSettingsItemFromConfig(serviceId: String, config: AIServiceConfig): SettingsItem {
        val isEnabled = config.isEnabled
        return when (serviceId) {
            "tencent-hunyuan" -> SettingsItem.TencentCloudAIServiceItem(enabled = isEnabled)
            "deepseek" -> SettingsItem.DeepSeekAIServiceItem(isEnabled = isEnabled)
            "kimi" -> SettingsItem.KimiAIServiceItem(isEnabled = isEnabled)
            "minimax" -> SettingsItem.MinimaxAIServiceItem(isEnabled = isEnabled)
            "baichuan" -> SettingsItem.BaichuanAIServiceItem(isEnabled = isEnabled)
            "openai" -> SettingsItem.OpenAIServiceItem(isEnabled = isEnabled)
            "ernie" -> SettingsItem.ErnieAIServiceItem(isEnabled = isEnabled)
            "qwen" -> SettingsItem.QwenAIServiceItem(isEnabled = isEnabled)
            "zhipu" -> SettingsItem.ZhipuAIServiceItem(isEnabled = isEnabled)
            "spark" -> SettingsItem.SparkAIServiceItem(isEnabled = isEnabled)
            "hunyuan" -> SettingsItem.HunyuanAIServiceItem(isEnabled = isEnabled)
            "doubao" -> SettingsItem.DoubaoAIServiceItem(isEnabled = isEnabled)
            "lingyi" -> SettingsItem.LingyiAIServiceItem(isEnabled = isEnabled)
            "jieyue" -> SettingsItem.JieyueAIServiceItem(isEnabled = isEnabled)
            "chatglm" -> SettingsItem.ChatGLMAIServiceItem(isEnabled = isEnabled)
            "tinybert_local" -> SettingsItem.TinyBERTAIServiceItem(isEnabled = isEnabled)
            else -> SettingsItem.DeepSeekAIServiceItem(isEnabled = isEnabled)
        }
    }
    
    /**
     * 创建初始设置项（使用默认状态）
     */
    private fun createInitialSettingsItem(serviceId: String): SettingsItem {
        return when (serviceId) {
            "tencent-hunyuan" -> SettingsItem.TencentCloudAIServiceItem(enabled = false)
            "deepseek" -> SettingsItem.DeepSeekAIServiceItem(isEnabled = false)
            "kimi" -> SettingsItem.KimiAIServiceItem(isEnabled = false)
            "minimax" -> SettingsItem.MinimaxAIServiceItem(isEnabled = false)
            "baichuan" -> SettingsItem.BaichuanAIServiceItem(isEnabled = false)
            "openai" -> SettingsItem.OpenAIServiceItem(isEnabled = false)
            "ernie" -> SettingsItem.ErnieAIServiceItem(isEnabled = false)
            "qwen" -> SettingsItem.QwenAIServiceItem(isEnabled = false)
            "zhipu" -> SettingsItem.ZhipuAIServiceItem(isEnabled = false)
            "spark" -> SettingsItem.SparkAIServiceItem(isEnabled = false)
            "hunyuan" -> SettingsItem.HunyuanAIServiceItem(isEnabled = false)
            "doubao" -> SettingsItem.DoubaoAIServiceItem(isEnabled = false)
            "lingyi" -> SettingsItem.LingyiAIServiceItem(isEnabled = false)
            "jieyue" -> SettingsItem.JieyueAIServiceItem(isEnabled = false)
            "chatglm" -> SettingsItem.ChatGLMAIServiceItem(isEnabled = false)
            "tinybert_local" -> SettingsItem.TinyBERTAIServiceItem(isEnabled = false)
            else -> SettingsItem.DeepSeekAIServiceItem(isEnabled = false)
        }
    }
    
    /**
     * 创建默认配置
     */
    private fun createDefaultConfig(serviceId: String): AIServiceConfig? {
        return when (serviceId) {
            "tencent-hunyuan" -> AIServiceConfig.TencentHunyuanConfig()
            "deepseek" -> AIServiceConfig.DeepSeekConfig()
            "kimi" -> AIServiceConfig.KimiConfig()
            "minimax" -> AIServiceConfig.MiniMaxConfig()
            "baichuan" -> AIServiceConfig.BaichuanConfig()
            "openai" -> AIServiceConfig.OpenAIConfig()
            "ernie" -> AIServiceConfig.ErnieConfig()
            "qwen" -> AIServiceConfig.QwenConfig()
            "zhipu" -> AIServiceConfig.ZhipuConfig()
            "spark" -> AIServiceConfig.SparkConfig()
            "hunyuan" -> AIServiceConfig.HunyuanConfig()
            "doubao" -> AIServiceConfig.DoubaoConfig()
            "lingyi" -> AIServiceConfig.LingyiConfig()
            "jieyue" -> AIServiceConfig.JieyueConfig()
            "chatglm" -> AIServiceConfig.ChatGLMConfig()
            "tinybert_local" -> AIServiceConfig.TinyBERTConfig()
            else -> null
        }
    }
    
    /**
     * 更新开关设置
     * 
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    fun updateSwitchSetting(
        item: SettingsItem, 
        isChecked: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                when (item) {
                    is SettingsItem.DarkModeSwitchItem -> {
                        settingsRepository.saveSetting(SettingsItem.DarkModeSwitchItem(isChecked = isChecked))
                    }
                    is SettingsItem.AutoSaveSwitchItem -> {
                        settingsRepository.saveSetting(SettingsItem.AutoSaveSwitchItem(isChecked = isChecked))
                    }
                    else -> {}
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "更新设置失败", e)
                onError("更新设置失败: ${e.message}")
            }
        }
    }
    
    /**
     * 切换 AI 服务启用状态
     * 
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    fun toggleAIService(
        serviceId: String, 
        isEnabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val config = settingsRepository.loadAIServiceConfig(serviceId)
                if (config != null) {
                    val updatedConfig = when (config) {
                        is AIServiceConfig.DeepSeekConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.OpenAIConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.KimiConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.MiniMaxConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.BaichuanConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.ErnieConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.QwenConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.ZhipuConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.SparkConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.HunyuanConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.DoubaoConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.LingyiConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.JieyueConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.ChatGLMConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.TinyBERTConfig -> config.copy(isEnabled = isEnabled)
                        is AIServiceConfig.TencentHunyuanConfig -> config.copy(isEnabled = isEnabled)
                        else -> null
                    }
                    updatedConfig?.let {
                        settingsRepository.saveAIServiceConfig(it)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "切换服务状态失败", e)
                onError("切换服务状态失败: ${e.message}")
            }
        }
    }
}
