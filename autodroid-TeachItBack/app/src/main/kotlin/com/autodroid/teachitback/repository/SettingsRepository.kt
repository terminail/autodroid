package com.autodroid.teachitback.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.database.SettingDao
import com.autodroid.teachitback.database.TopicConverters
import com.autodroid.teachitback.di.AppContainer
import com.autodroid.teachitback.model.SettingEntity
import com.autodroid.teachitback.ui.adapter.SettingsItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * 设置项仓库
 * 负责SettingsItem和AIServiceConfig的序列化、反序列化和数据库存取操作
 */
class SettingsRepository(private val settingDao: SettingDao) {
    
    // 初始化gson用于SettingsItem的序列化
    private val gson = Gson()
    
    // AI服务配置LiveData
    private val _aiServiceConfigs = MutableLiveData<Map<String, AIServiceConfig>>()
    val aiServiceConfigs: LiveData<Map<String, AIServiceConfig>> = _aiServiceConfigs
    
    /**
     * 加载所有AI服务配置到LiveData
     */
    suspend fun loadAllAIServiceConfigsToLiveData() {
        try {
            val entities = settingDao.getAllSettingsSync()
            val configs = entities.filter { it.key.startsWith("ai_service_") }
                .mapNotNull { deserializeAIServiceConfig(it.value) }
            val configMap = configs.associateBy { it.id }
            _aiServiceConfigs.value = configMap
        } catch (e: Exception) {
            Log.e("SettingsRepository", "加载AI服务配置到LiveData失败", e)
        }
    }
    
    /**
     * 获取所有设置项
     */
    fun getAllSettings(): Flow<List<SettingsItem>> {
        return settingDao.getAllSettings().map { entities ->
            entities.mapNotNull { entity ->
                deserializeSettingItem(entity.value)
            }
        }
    }
    
    /**
     * 根据key获取设置项
     */
    fun getSettingByKey(key: String): Flow<SettingsItem?> {
        return settingDao.getSettingByKey(key).map { entity ->
            entity?.let { deserializeSettingItem(it.value) }
        }
    }
    
    /**
     * 同步获取设置项
     */
    suspend fun getSettingByKeySync(key: String): SettingsItem? {
        return try {
            val entity = settingDao.getSettingByKeySync(key)
            entity?.let { deserializeSettingItem(it.value) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 保存设置项
     */
    suspend fun saveSetting(item: SettingsItem) {
        try {
            val key = when (item) {
                is SettingsItem.DeepSeekAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.OpenAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.KimiAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.BaichuanAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.MinimaxAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.HunyuanAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.ChatGLMAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.TinyBERTAIServiceItem -> "ai_service_${item.id}"
                is SettingsItem.TencentCloudAIServiceItem -> "ai_service_tencentcloud"
                is SettingsItem.DarkModeSwitchItem -> "dark_mode"
                is SettingsItem.AutoSaveSwitchItem -> "auto_save"
                else -> return
            }
            val json = serializeSettingItem(item)
            val entity = SettingEntity(
                key = key,
                value = json,
                lastUpdated = System.currentTimeMillis(),
                created = System.currentTimeMillis()
            )
            settingDao.insertSetting(entity)
        } catch (e: Exception) {
        }
    }
    
    /**
     * 删除设置项
     */
    suspend fun deleteSetting(key: String) {
        try {
            settingDao.deleteSettingByKey(key)
        } catch (e: Exception) {
        }
    }

    /**
     * 删除所有设置
     */
    suspend fun deleteAllSettings() {
        try {
            settingDao.deleteAllSettings()
        } catch (e: Exception) {
        }
    }

    /**
     * 检查并更新AI服务状态
     */
    suspend fun checkAndUpdateAiServiceStatus(config: AIServiceConfig): AIServiceConfig {
        Log.d("SettingsRepository", "开始检查AI服务状态: ${config.displayName} (${config.id})")
        
        return try {
            // 检查服务是否可用
            val updatedConfig = checkServiceAvailability(config)
            
            // 保存更新后的配置
            saveAIServiceConfig(updatedConfig)
            
            Log.d("SettingsRepository", "AI服务状态检查完成: 状态码=${updatedConfig.status.code}, 描述=${updatedConfig.status.description}")
            updatedConfig
        } catch (e: Exception) {
            val errorConfig = copyConfigWithStatus(
                config,
                AIServiceStatus.fromCode(500, "状态检查异常: ${e.message}")
            )
            saveAIServiceConfig(errorConfig)
            errorConfig
        }
    }
    
    /**
     * 检查AI服务可用性
     * 测试服务的checkStatus()方法来验证服务是否可用
     */
    private suspend fun checkServiceAvailability(config: AIServiceConfig): AIServiceConfig {
        Log.d("SettingsRepository", "检查AI服务可用性: ${config.displayName}")

        // 1. 首先检查基本配置是否完整（对于非本地AI服务）
        val isConfigComplete = when {
            config.requiredFields.requireApiKey && config.apiKey.isEmpty() -> false
            config.requiredFields.requireBaseUrl && config.baseUrl.isEmpty() -> false
            config.requiredFields.requireSecretId && config.secretId.isEmpty() -> false
            config.requiredFields.requireRegion && config.region.isEmpty() -> false
            else -> true
        }

        if (!isConfigComplete) {
            val status = AIServiceStatus.fromCode(400, "配置不完整")
            return copyConfigWithStatus(config, status)
        }

        // 2. 检查模型文件是否存在（如果服务使用本地模型）
        val modelFilePath = config.modelFilePath
        if (!modelFilePath.isNullOrEmpty()) {
            val modelFileExists = checkModelFileExists(modelFilePath)
            if (!modelFileExists) {
                val status = AIServiceStatus.fromCode(404, "模型文件未找到: $modelFilePath")
                return copyConfigWithStatus(config, status)
            }
        }

        // 3. 测试服务的checkStatus()方法来验证服务是否可用
        return try {
            // 获取服务实例
            val registry = AppContainer.getAIServiceRegistry()
            val service = registry.getService(config.id)
            if (service == null) {
                copyConfigWithStatus(config, AIServiceStatus.fromCode(404, "服务未注册: ${config.id}"))
            } else {
                // 测试服务的checkStatus()方法
                // 注意：服务会自动从LiveData获取最新配置，无需手动调用updateConfig
                val status = service.checkStatus()
                copyConfigWithStatus(config, status)
            }
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is java.net.UnknownHostException -> "网络连接失败"
                is java.net.SocketTimeoutException -> "请求超时"
                is java.io.IOException -> "网络错误: ${e.message}"
                else -> "服务测试失败: ${e.message}"
            }
            copyConfigWithStatus(config, AIServiceStatus.fromCode(503, errorMessage))
        }
    }
    
    /**
     * 检查模型文件是否存在
     */
    private suspend fun checkModelFileExists(modelFilePath: String?): Boolean {
        if (modelFilePath.isNullOrEmpty()) return true

        return try {
            val context: Context = AppContainer.getApplication()
            val modelFile = File(context.filesDir, modelFilePath)
            val exists = modelFile.exists() && modelFile.length() > 0

            if (exists) {
                Log.d("SettingsRepository", "模型文件存在: ${modelFile.absolutePath} (大小: ${modelFile.length()} bytes)")
            } else {
                Log.e("SettingsRepository", "模型文件不存在: ${modelFile.absolutePath}")
            }

            exists
        } catch (e: Exception) {
            Log.e("SettingsRepository", "检查模型文件失败: $modelFilePath", e)
            false
        }
    }
    
    /**
     * 验证API Key是否有效
     * 模拟验证过程，实际应调用服务进行验证
     */
    private suspend fun validateApiKey(apiKey: String): Boolean {
        // 模板验证：长度至少为10个字符
        if (apiKey.length < 10) {
            return false
        }

        // 其他可以调用服务进行简单的测试请求，然后根据返回结果判断

        return true
    }
    
    /**
     * 序列化SettingsItem为JSON字符串
     */
    private fun serializeSettingItem(item: SettingsItem): String {
        val type = when (item) {
            is SettingsItem.SectionHeaderItem -> "SectionHeaderItem"
            is SettingsItem.DarkModeSwitchItem -> "DarkModeSwitchItem"
            is SettingsItem.AutoSaveSwitchItem -> "AutoSaveSwitchItem"
            is SettingsItem.LanguageSettingItem -> "LanguageSettingItem"
            is SettingsItem.BackupDataItem -> "BackupDataItem"
            is SettingsItem.RestoreDataItem -> "RestoreDataItem"
            is SettingsItem.ClearAllDataButtonItem -> "ClearAllDataButtonItem"
            is SettingsItem.VersionInfoItem -> "VersionInfoItem"
            is SettingsItem.HelpAndFeedbackItem -> "HelpAndFeedbackItem"
            is SettingsItem.DoubaoAIServiceItem -> "DoubaoAIServiceItem"
            is SettingsItem.DeepSeekAIServiceItem -> "DeepSeekAIServiceItem"
            is SettingsItem.MinimaxAIServiceItem -> "MinimaxAIServiceItem"
            is SettingsItem.KimiAIServiceItem -> "KimiAIServiceItem"
            is SettingsItem.OpenAIServiceItem -> "OpenAIServiceItem"
            is SettingsItem.ErnieAIServiceItem -> "ErnieAIServiceItem"
            is SettingsItem.QwenAIServiceItem -> "QwenAIServiceItem"
            is SettingsItem.ZhipuAIServiceItem -> "ZhipuAIServiceItem"
            is SettingsItem.SparkAIServiceItem -> "SparkAIServiceItem"
            is SettingsItem.HunyuanAIServiceItem -> "HunyuanAIServiceItem"
            is SettingsItem.BaichuanAIServiceItem -> "BaichuanAIServiceItem"
            is SettingsItem.LingyiAIServiceItem -> "LingyiAIServiceItem"
            is SettingsItem.JieyueAIServiceItem -> "JieyueAIServiceItem"
            is SettingsItem.ChatGLMAIServiceItem -> "ChatGLMAIServiceItem"
            is SettingsItem.TinyBERTAIServiceItem -> "TinyBERTAIServiceItem"
            is SettingsItem.TencentCloudAIServiceItem -> "TencentCloudAIServiceItem"
        }
        
        val dataJson = gson.toJson(item)
        return """{"type":"$type","data":$dataJson}"""
    }
    
    /**
     * 反序列化JSON字符串为SettingsItem
     */
    private fun deserializeSettingItem(json: String): SettingsItem? {
        return try {
            val wrapperType = object : TypeToken<Map<String, Any>>() {}.type
            val wrapper: Map<String, Any> = gson.fromJson(json, wrapperType)
            
            val type = wrapper["type"] as? String ?: return null
            val dataJson = gson.toJson(wrapper["data"])
            
            when (type) {
                "SectionHeaderItem" -> gson.fromJson(dataJson, SettingsItem.SectionHeaderItem::class.java)
                "DarkModeSwitchItem" -> gson.fromJson(dataJson, SettingsItem.DarkModeSwitchItem::class.java)
                "AutoSaveSwitchItem" -> gson.fromJson(dataJson, SettingsItem.AutoSaveSwitchItem::class.java)
                "LanguageSettingItem" -> gson.fromJson(dataJson, SettingsItem.LanguageSettingItem::class.java)
                "BackupDataItem" -> gson.fromJson(dataJson, SettingsItem.BackupDataItem::class.java)
                "RestoreDataItem" -> gson.fromJson(dataJson, SettingsItem.RestoreDataItem::class.java)
                "ClearAllDataButtonItem" -> gson.fromJson(dataJson, SettingsItem.ClearAllDataButtonItem::class.java)
                "VersionInfoItem" -> gson.fromJson(dataJson, SettingsItem.VersionInfoItem::class.java)
                "HelpAndFeedbackItem" -> gson.fromJson(dataJson, SettingsItem.HelpAndFeedbackItem::class.java)
                "DoubaoAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.DoubaoAIServiceItem::class.java)
                "DeepSeekAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.DeepSeekAIServiceItem::class.java)
                "MinimaxAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.MinimaxAIServiceItem::class.java)
                "KimiAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.KimiAIServiceItem::class.java)
                "OpenAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.OpenAIServiceItem::class.java)
                "ErnieAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.ErnieAIServiceItem::class.java)
                "QwenAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.QwenAIServiceItem::class.java)
                "ZhipuAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.ZhipuAIServiceItem::class.java)
                "SparkAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.SparkAIServiceItem::class.java)
                "HunyuanAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.HunyuanAIServiceItem::class.java)
                "BaichuanAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.BaichuanAIServiceItem::class.java)
                "LingyiAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.LingyiAIServiceItem::class.java)
                "JieyueAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.JieyueAIServiceItem::class.java)
                "ChatGLMAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.ChatGLMAIServiceItem::class.java)
                "TinyBERTAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.TinyBERTAIServiceItem::class.java)
                "TencentCloudAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.TencentCloudAIServiceItem::class.java)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 序列化AIServiceConfig为JSON字符串
     */
    private fun serializeAIServiceConfig(config: AIServiceConfig): String {
        return TopicConverters.fromAIServiceConfig(config)
    }
    
    /**
     * 反序列化JSON字符串为AIServiceConfig
     */
    private fun deserializeAIServiceConfig(json: String): AIServiceConfig? {
        return TopicConverters.toAIServiceConfig(json)
    }
    
    /**
     * 创建副本并配置更新后的状态
     */
    private fun copyConfigWithStatus(config: AIServiceConfig, status: AIServiceStatus): AIServiceConfig {
        return when (config) {
            is AIServiceConfig.TencentHunyuanConfig -> config.copy(status = status)
            is AIServiceConfig.DeepSeekConfig -> config.copy(status = status)
            is AIServiceConfig.MiniMaxConfig -> config.copy(status = status)
            is AIServiceConfig.BaichuanConfig -> config.copy(status = status)
            is AIServiceConfig.KimiConfig -> config.copy(status = status)
            is AIServiceConfig.OpenAIConfig -> config.copy(status = status)
            is AIServiceConfig.ErnieConfig -> config.copy(status = status)
            is AIServiceConfig.QwenConfig -> config.copy(status = status)
            is AIServiceConfig.ZhipuConfig -> config.copy(status = status)
            is AIServiceConfig.SparkConfig -> config.copy(status = status)
            is AIServiceConfig.HunyuanConfig -> config.copy(status = status)
            is AIServiceConfig.DoubaoConfig -> config.copy(status = status)
            is AIServiceConfig.LingyiConfig -> config.copy(status = status)
            is AIServiceConfig.JieyueConfig -> config.copy(status = status)
            is AIServiceConfig.ChatGLMConfig -> config.copy(status = status)
            is AIServiceConfig.TinyBERTConfig -> config.copy(status = status)
        }
    }
    
    /**
     * 保存AI服务配置
     */
    suspend fun saveAIServiceConfig(config: AIServiceConfig) {
        try {
            val json = serializeAIServiceConfig(config)
            val entity = SettingEntity(
                key = "ai_service_${config.id}",
                value = json,
                lastUpdated = System.currentTimeMillis(),
                created = System.currentTimeMillis()
            )
            settingDao.insertSetting(entity)
            
            // 更新LiveData（使用postValue在后台线程中更新）
            val currentConfigs = _aiServiceConfigs.value ?: emptyMap()
            _aiServiceConfigs.postValue(currentConfigs + (config.id to config))
        } catch (e: Exception) {
            Log.e("SettingsRepository", "保存AI服务配置失败: ${config.id}", e)
        }
    }
    
    /**
     * 加载AI服务配置
     */
    suspend fun loadAIServiceConfig(serviceId: String): AIServiceConfig? {
        return try {
            val entity = settingDao.getSettingByKeySync("ai_service_$serviceId")
            entity?.let { deserializeAIServiceConfig(it.value) }
        } catch (e: Exception) {
            Log.e("SettingsRepository", "鍔犺浇AI鏈嶅姟閰嶇疆澶辫触: $serviceId", e)
            null
        }
    }
    
    /**
     * 获取所有AI服务配置
     */
    suspend fun getAllAIServiceConfigs(): List<AIServiceConfig> {
        return try {
            val entities = settingDao.getAllSettingsSync()
            entities.filter { it.key.startsWith("ai_service_") }
                .mapNotNull { deserializeAIServiceConfig(it.value) }
        } catch (e: Exception) {
            Log.e("SettingsRepository", "获取所有AI服务配置失败", e)
            emptyList()
        }
    }

    /**
     * 将 AIServiceConfig 转换为 SettingsItem
     */
    fun configToSettingsItem(config: AIServiceConfig): SettingsItem? {
        return when (config) {
            is AIServiceConfig.DoubaoConfig -> SettingsItem.DoubaoAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://developer.doubao.com",
                officialWebsite = "https://www.doubao.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.DeepSeekConfig -> SettingsItem.DeepSeekAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://platform.deepseek.com",
                officialWebsite = "https://www.deepseek.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.MiniMaxConfig -> SettingsItem.MinimaxAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://platform.minimaxi.com",
                officialWebsite = "https://minimax.chat",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.KimiConfig -> SettingsItem.KimiAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://platform.moonshot.cn",
                officialWebsite = "https://kimi.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.OpenAIConfig -> SettingsItem.OpenAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://platform.openai.com",
                officialWebsite = "https://openai.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.ErnieConfig -> SettingsItem.ErnieAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://console.bce.baidu.com/qianfan",
                officialWebsite = "https://yiyan.baidu.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.QwenConfig -> SettingsItem.QwenAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://dashscope.aliyuncs.com",
                officialWebsite = "https://tongyi.aliyun.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.ZhipuConfig -> SettingsItem.ZhipuAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://open.bigmodel.cn",
                officialWebsite = "https://www.zhipuai.cn",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.SparkConfig -> SettingsItem.SparkAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://console.xfyun.cn/services/spark",
                officialWebsite = "https://xinghuo.xfyun.cn",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.HunyuanConfig -> SettingsItem.HunyuanAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://cloud.tencent.com/product/hunyuan",
                officialWebsite = "https://混元.tencent.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.BaichuanConfig -> SettingsItem.BaichuanAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://platform.baichuan-ai.com",
                officialWebsite = "https://www.baichuan-ai.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.LingyiConfig -> SettingsItem.LingyiAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://open.lingyiwanwu.com",
                officialWebsite = "https://www.lingyiwanwu.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.JieyueConfig -> SettingsItem.JieyueAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                freeQuota = "${config.freeQuota} tokens",
                apiKeyUrl = "https://open.jieyuesx.com",
                officialWebsite = "https://www.jieyuesx.com",
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.ChatGLMConfig -> SettingsItem.ChatGLMAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                defaultModel = config.model,
                isEnabled = config.isEnabled,
                priority = 100,
                modelDownloadStrategy = "AUTO",
                isModelDownloaded = false,
                modelDownloadProgress = 0.0f,
                modelSize = "6GB",
                githubRepo = "THUDM/ChatGLM-6B"
            )
            is AIServiceConfig.TinyBERTConfig -> SettingsItem.TinyBERTAIServiceItem(
                id = config.id,
                name = config.name,
                description = config.description,
                defaultModel = config.model,
                isEnabled = config.isEnabled,
                priority = 100,
                modelDownloadStrategy = "AUTO",
                isModelDownloaded = false,
                modelDownloadProgress = 0.0f,
                modelSize = "50MB",
                githubRepo = "huawei-noah/TinyBERT"
            )
            is AIServiceConfig.TencentHunyuanConfig -> null
        }
    }

    /**
     * 将 SettingsItem 转换为 AIServiceConfig
     */
    fun settingsItemToConfig(item: SettingsItem): AIServiceConfig? {
        return when (item) {
            is SettingsItem.DoubaoAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.DeepSeekAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.MinimaxAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.KimiAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.OpenAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.ErnieAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.QwenAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.ZhipuAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.SparkAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.HunyuanAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.BaichuanAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.LingyiAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.JieyueAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.ChatGLMAIServiceItem -> item.toAIServiceConfig()
            is SettingsItem.TinyBERTAIServiceItem -> item.toAIServiceConfig()
            else -> null
        }
    }
}

