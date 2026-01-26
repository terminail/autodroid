package com.autodroid.teachitback.viewmodel

import androidx.lifecycle.*
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.repository.SettingsRepository
import com.autodroid.teachitback.service.TopicTreeManager
import kotlinx.coroutines.launch

/**
 * 服务设置ViewModel
 * 统一管理AI服务的状态、配置和路由决策
 */
class ServiceSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val topicTreeManager: TopicTreeManager
) : ViewModel() {

    /**
     * 所有可用的AI服务配置
     */
    private val _availableServices = MutableLiveData<List<AIServiceConfig>>()
    val availableServices: LiveData<List<AIServiceConfig>> get() = _availableServices

    /**
     * 当前启用的服务
     */
    private val _enabledServices = MutableLiveData<List<AIServiceConfig>>()
    val enabledServices: LiveData<List<AIServiceConfig>> get() = _enabledServices

    /**
     * 当前选中的服务
     */
    private val _selectedService = MutableLiveData<AIServiceConfig?>()
    val selectedService: LiveData<AIServiceConfig?> get() = _selectedService

    /**
     * 服务状态（在线/离线）
     */
    private val _serviceStatus = MutableLiveData<Map<String, Boolean>>()
    val serviceStatus: LiveData<Map<String, Boolean>> get() = _serviceStatus

    /**
     * 路由决策历史
     */
    private val _routingHistory = MutableLiveData<List<RoutingDecision>>()
    val routingHistory: LiveData<List<RoutingDecision>> get() = _routingHistory

    /**
     * 错误信息
     */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        loadServiceConfigurations()
        loadEnabledServices()
        initializeServiceStatus()
    }

    /**
     * 加载所有可用的服务配置
     */
    private fun loadServiceConfigurations() {
        viewModelScope.launch {
            try {
                // 创建默认的服务配置列表
                val allConfigs = listOf(
                    AIServiceConfig.TencentHunyuanConfig(),
                    AIServiceConfig.DeepSeekConfig(),
                    AIServiceConfig.KimiConfig(),
                    AIServiceConfig.MiniMaxConfig(),
                    AIServiceConfig.BaichuanConfig(),
                    AIServiceConfig.OpenAIConfig(),
                    AIServiceConfig.ErnieConfig(),
                    AIServiceConfig.QwenConfig(),
                    AIServiceConfig.ZhipuConfig(),
                    AIServiceConfig.SparkConfig(),
                    AIServiceConfig.HunyuanConfig(),
                    AIServiceConfig.DoubaoConfig(),
                    AIServiceConfig.LingyiConfig(),
                    AIServiceConfig.JieyueConfig(),
                    AIServiceConfig.ChatGLMConfig(),
                    AIServiceConfig.TinyBERTConfig()
                )
                
                _availableServices.value = allConfigs
                
                // 如果没有选中服务，默认选中第一个
                if (_selectedService.value == null && allConfigs.isNotEmpty()) {
                    _selectedService.value = allConfigs.first()
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载服务配置失败: ${e.message}"
            }
        }
    }

    /**
     * 加载启用的服务
     */
    private fun loadEnabledServices() {
        viewModelScope.launch {
            try {
                // 默认所有服务都启用（简化实现）
                val allConfigs = _availableServices.value ?: emptyList()
                _enabledServices.value = allConfigs
            } catch (e: Exception) {
                _errorMessage.value = "加载启用服务失败: ${e.message}"
            }
        }
    }

    /**
     * 初始化服务状态
     */
    private fun initializeServiceStatus() {
        val initialStatus = _availableServices.value?.associate { config ->
            config.id to true // 默认所有服务在线
        } ?: emptyMap()
        
        _serviceStatus.value = initialStatus
    }

    /**
     * 切换服务启用状态
     */
    fun toggleService(serviceId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                // 更新启用服务列表
                val currentEnabled = _enabledServices.value?.toMutableList() ?: mutableListOf()
                val allConfigs = _availableServices.value ?: emptyList()
                
                val serviceConfig = allConfigs.find { it.id == serviceId }
                
                if (enabled) {
                    if (serviceConfig != null && !currentEnabled.any { it.id == serviceId }) {
                        currentEnabled.add(serviceConfig)
                    }
                } else {
                    currentEnabled.removeAll { it.id == serviceId }
                }
                
                _enabledServices.value = currentEnabled
            } catch (e: Exception) {
                _errorMessage.value = "切换服务状态失败: ${e.message}"
            }
        }
    }

    /**
     * 选择服务
     */
    fun selectService(serviceConfig: AIServiceConfig) {
        _selectedService.value = serviceConfig
    }

    /**
     * 更新服务状态
     */
    fun updateServiceStatus(serviceId: String, online: Boolean) {
        val currentStatus = _serviceStatus.value?.toMutableMap() ?: mutableMapOf()
        currentStatus[serviceId] = online
        _serviceStatus.value = currentStatus
    }

    /**
     * 根据主题推荐最佳AI服务
     */
    fun recommendServiceForTopic(topicId: String) {
        viewModelScope.launch {
            try {
                val enabled = _enabledServices.value ?: emptyList()
                val available = enabled.filter { config ->
                    _serviceStatus.value?.get(config.id) == true
                }
                
                if (available.isEmpty()) {
                    _errorMessage.value = "没有可用的AI服务"
                    return@launch
                }
                
                val recommended = topicTreeManager.recommendAIService(topicId, available)
                
                if (recommended != null) {
                    _selectedService.value = recommended
                    
                    // 记录路由决策
                    val history = _routingHistory.value?.toMutableList() ?: mutableListOf()
                    history.add(RoutingDecision(
                        topicId = topicId,
                        selectedService = recommended.id,
                        timestamp = System.currentTimeMillis(),
                        reason = "基于主题能力匹配"
                    ))
                    
                    // 保持最近10条记录
                    if (history.size > 10) {
                        history.removeAt(0)
                    }
                    
                    _routingHistory.value = history
                } else {
                    _errorMessage.value = "无法为当前主题推荐合适的AI服务"
                }
            } catch (e: Exception) {
                _errorMessage.value = "推荐服务失败: ${e.message}"
            }
        }
    }

    /**
     * 获取服务配置详情
     */
    suspend fun getServiceConfig(serviceId: String): AIServiceConfig? {
        return _availableServices.value?.find { it.id == serviceId }
    }

    /**
     * 验证服务配置
     */
    fun validateServiceConfig(serviceId: String, configData: Map<String, String>): Boolean {
        val serviceConfig = _availableServices.value?.find { it.id == serviceId }
        return serviceConfig?.let { config ->
            config.requiredFields.run {
                (!requireSecretId || !configData["secretId"].isNullOrBlank()) &&
                (!requireApiKey || !configData["apiKey"].isNullOrBlank()) &&
                (!requireBaseUrl || !configData["baseUrl"].isNullOrBlank()) &&
                (!requireRegion || !configData["region"].isNullOrBlank()) &&
                (!requireModel || !configData["model"].isNullOrBlank())
            }
        } ?: false
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 获取服务的在线状态
     */
    fun isServiceOnline(serviceId: String): Boolean {
        return _serviceStatus.value?.get(serviceId) ?: false
    }

    /**
     * 获取服务的启用状态
     */
    fun isServiceEnabled(serviceId: String): Boolean {
        return _enabledServices.value?.any { it.id == serviceId } ?: false
    }

    /**
     * 获取所有在线且启用的服务
     */
    fun getAvailableServices(): List<AIServiceConfig> {
        val enabled = _enabledServices.value ?: emptyList()
        val status = _serviceStatus.value ?: emptyMap()
        
        return enabled.filter { config ->
            status[config.id] == true
        }
    }

    /**
     * 获取服务统计信息
     */
    fun getServiceStats(): ServiceStats {
        val all = _availableServices.value?.size ?: 0
        val enabled = _enabledServices.value?.size ?: 0
        val online = _serviceStatus.value?.count { it.value } ?: 0
        
        return ServiceStats(
            totalServices = all,
            enabledServices = enabled,
            onlineServices = online
        )
    }
}

/**
 * 路由决策记录
 */
data class RoutingDecision(
    val topicId: String,
    val selectedService: String,
    val timestamp: Long,
    val reason: String
)

/**
 * 服务统计信息
 */
data class ServiceStats(
    val totalServices: Int,
    val enabledServices: Int,
    val onlineServices: Int
)