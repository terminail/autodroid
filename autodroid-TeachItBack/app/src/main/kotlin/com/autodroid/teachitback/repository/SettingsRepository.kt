package com.autodroid.teachitback.repository

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.filterNotNull
import java.io.File

/**
 * 设置项仓库
 * 负责SettingsItem和AIServiceConfig的序列化、反序列化和数据库存取操作
 * 
 * 使用同步查询方式，由 ViewModel 控制数据加载时机
 */
class SettingsRepository(private val settingDao: SettingDao) {
    
    // 初始化gson用于SettingsItem的序列化
    private val gson = Gson()
    
    // 配置变更监听器 - 用于通知AI服务配置已更新
    private val configChangeListeners = mutableMapOf<String, MutableList<(AIServiceConfig) -> Unit>>()
    
    /**
     * 注册配置变更监听器
     * @param serviceId 服务ID
     * @param listener 配置变更回调
     */
    fun registerConfigChangeListener(serviceId: String, listener: (AIServiceConfig) -> Unit) {
        configChangeListeners.getOrPut(serviceId) { mutableListOf() }.add(listener)
        Log.d("SettingsRepository", "注册配置变更监听器: $serviceId")
    }
    
    /**
     * 注销配置变更监听器
     * @param serviceId 服务ID
     * @param listener 配置变更回调
     */
    fun unregisterConfigChangeListener(serviceId: String, listener: (AIServiceConfig) -> Unit) {
        configChangeListeners[serviceId]?.remove(listener)
        Log.d("SettingsRepository", "注销配置变更监听器: $serviceId")
    }
    
    /**
     * 通知配置变更
     * @param config 变更后的配置
     */
    private fun notifyConfigChanged(config: AIServiceConfig) {
        val listeners = configChangeListeners[config.id]
        listeners?.forEach { listener ->
            try {
                listener(config)
            } catch (e: Exception) {
                Log.e("SettingsRepository", "通知配置变更失败: ${config.id}", e)
            }
        }
        Log.d("SettingsRepository", "通知配置变更: ${config.id}, 监听器数量: ${listeners?.size ?: 0}")
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

        // 2. 检查模型文件是否存在（仅对嵌入式服务）
        val modelFilePath = when (config) {
            is AIServiceConfig.ChatGLMConfig -> config.modelFilePath
            is AIServiceConfig.TinyBERTConfig -> config.modelFilePath
            else -> null
        }
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
                val status = service.checkStatus()
                copyConfigWithStatus(config, status)
            }
        } catch (e: Exception) {
            Log.e("SettingsRepository", "检查AI服务可用性失败: ${config.displayName}", e)
            copyConfigWithStatus(config, AIServiceStatus.fromCode(500, "检查失败: ${e.message}"))
        }
    }
    
    /**
     * 检查模型文件是否存在
     */
    private fun checkModelFileExists(modelFilePath: String): Boolean {
        return try {
            val file = File(modelFilePath)
            file.exists() && file.isFile && file.length() > 0
        } catch (e: Exception) {
            Log.e("SettingsRepository", "检查模型文件失败: $modelFilePath", e)
            false
        }
    }
    
    /**
     * 复制配置并更新状态
     */
    private fun copyConfigWithStatus(config: AIServiceConfig, status: AIServiceStatus): AIServiceConfig {
        return when (config) {
            is AIServiceConfig.ZhipuConfig -> config.copy(status = status)
            is AIServiceConfig.DeepSeekConfig -> config.copy(status = status)
            is AIServiceConfig.KimiConfig -> config.copy(status = status)
            is AIServiceConfig.MiniMaxConfig -> config.copy(status = status)
            is AIServiceConfig.BaichuanConfig -> config.copy(status = status)
            is AIServiceConfig.OpenAIConfig -> config.copy(status = status)
            is AIServiceConfig.ErnieConfig -> config.copy(status = status)
            is AIServiceConfig.QwenConfig -> config.copy(status = status)
            is AIServiceConfig.SparkConfig -> config.copy(status = status)
            is AIServiceConfig.HunyuanConfig -> config.copy(status = status)
            is AIServiceConfig.DoubaoConfig -> config.copy(status = status)
            is AIServiceConfig.LingyiConfig -> config.copy(status = status)
            is AIServiceConfig.JieyueConfig -> config.copy(status = status)
            is AIServiceConfig.ChatGLMConfig -> config.copy(status = status)
            is AIServiceConfig.TinyBERTConfig -> config.copy(status = status)
            is AIServiceConfig.TencentHunyuanConfig -> config.copy(status = status)
            else -> config
        }
    }

    /**
     * 保存AI服务配置
     */
    suspend fun saveAIServiceConfig(config: AIServiceConfig) {
        try {
            val json = serializeAIServiceConfig(config)
            val key = "ai_service_${config.id}"
            
            // 查询现有记录以保留 created 时间
            val existingEntity = settingDao.getSettingByKeySync(key)
            val created = existingEntity?.created ?: System.currentTimeMillis()
            
            val entity = SettingEntity(
                key = key,
                value = json,
                lastUpdated = System.currentTimeMillis(),
                created = created
            )
            settingDao.insertSetting(entity)
            Log.d("SettingsRepository", "配置已保存: ${config.id}")
            
            // 通知配置变更
            notifyConfigChanged(config)
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
            Log.e("SettingsRepository", "加载AI服务配置失败: $serviceId", e)
            null
        }
    }

    /**
     * 将 AIServiceConfig 转换为 SettingsItem
     */
    fun convertToSettingsItem(config: AIServiceConfig): SettingsItem {
        return when (config) {
            is AIServiceConfig.DeepSeekConfig -> SettingsItem.DeepSeekAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.OpenAIConfig -> SettingsItem.OpenAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.KimiConfig -> SettingsItem.KimiAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.BaichuanConfig -> SettingsItem.BaichuanAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.MiniMaxConfig -> SettingsItem.MinimaxAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.HunyuanConfig -> SettingsItem.HunyuanAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.ChatGLMConfig -> SettingsItem.ChatGLMAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.TinyBERTConfig -> SettingsItem.TinyBERTAIServiceItem(
                isEnabled = config.isEnabled
            )
            is AIServiceConfig.TencentHunyuanConfig -> SettingsItem.TencentCloudAIServiceItem(
                apiKey = config.apiKey,
                secretId = config.secretId,
                enabled = config.isEnabled
            )
            else -> SettingsItem.DeepSeekAIServiceItem(
                isEnabled = false
            )
        }
    }

    // ===== 序列化和反序列化方法 =====

    /**
     * 序列化SettingsItem
     */
    private fun serializeSettingItem(item: SettingsItem): String {
        return when (item) {
            is SettingsItem.DeepSeekAIServiceItem -> gson.toJson(mapOf(
                "type" to "deepseek",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.OpenAIServiceItem -> gson.toJson(mapOf(
                "type" to "openai",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.KimiAIServiceItem -> gson.toJson(mapOf(
                "type" to "kimi",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.BaichuanAIServiceItem -> gson.toJson(mapOf(
                "type" to "baichuan",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.MinimaxAIServiceItem -> gson.toJson(mapOf(
                "type" to "minimax",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.HunyuanAIServiceItem -> gson.toJson(mapOf(
                "type" to "hunyuan",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.ChatGLMAIServiceItem -> gson.toJson(mapOf(
                "type" to "chatglm",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.TinyBERTAIServiceItem -> gson.toJson(mapOf(
                "type" to "tinybert",
                "isEnabled" to item.isEnabled
            ))
            is SettingsItem.TencentCloudAIServiceItem -> gson.toJson(mapOf(
                "type" to "tencentcloud",
                "apiKey" to item.apiKey,
                "secretId" to item.secretId,
                "enabled" to item.enabled
            ))
            is SettingsItem.DarkModeSwitchItem -> gson.toJson(mapOf(
                "type" to "dark_mode",
                "isChecked" to item.isChecked
            ))
            is SettingsItem.AutoSaveSwitchItem -> gson.toJson(mapOf(
                "type" to "auto_save",
                "isChecked" to item.isChecked
            ))
            else -> "{}"
        }
    }

    /**
     * 反序列化SettingsItem
     */
    private fun deserializeSettingItem(json: String): SettingsItem? {
        return try {
            val map = gson.fromJson(json, Map::class.java) as? Map<String, Any>
            when (map?.get("type")) {
                "deepseek" -> SettingsItem.DeepSeekAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "openai" -> SettingsItem.OpenAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "kimi" -> SettingsItem.KimiAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "baichuan" -> SettingsItem.BaichuanAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "minimax" -> SettingsItem.MinimaxAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "hunyuan" -> SettingsItem.HunyuanAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "chatglm" -> SettingsItem.ChatGLMAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "tinybert" -> SettingsItem.TinyBERTAIServiceItem(
                    isEnabled = map["isEnabled"] as? Boolean ?: false
                )
                "tencentcloud" -> SettingsItem.TencentCloudAIServiceItem(
                    apiKey = map["apiKey"] as? String ?: "",
                    secretId = map["secretId"] as? String ?: "",
                    enabled = map["enabled"] as? Boolean ?: false
                )
                "dark_mode" -> SettingsItem.DarkModeSwitchItem(
                    isChecked = map["isChecked"] as? Boolean ?: false
                )
                "auto_save" -> SettingsItem.AutoSaveSwitchItem(
                    isChecked = map["isChecked"] as? Boolean ?: false
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 序列化AIServiceConfig
     */
    fun serializeAIServiceConfig(config: AIServiceConfig): String {
        return gson.toJson(config)
    }

    /**
     * 反序列化AIServiceConfig
     */
    fun deserializeAIServiceConfig(json: String): AIServiceConfig? {
        return try {
            // 首先解析JSON获取type字段
            val jsonObject = com.google.gson.JsonParser.parseString(json).asJsonObject
            val type = jsonObject.get("id")?.asString ?: return null
            
            // 根据type选择正确的配置类进行反序列化
            when (type) {
                "zhipu" -> gson.fromJson(json, AIServiceConfig.ZhipuConfig::class.java)
                "deepseek" -> gson.fromJson(json, AIServiceConfig.DeepSeekConfig::class.java)
                "kimi" -> gson.fromJson(json, AIServiceConfig.KimiConfig::class.java)
                "minimax" -> gson.fromJson(json, AIServiceConfig.MiniMaxConfig::class.java)
                "baichuan" -> gson.fromJson(json, AIServiceConfig.BaichuanConfig::class.java)
                "openai" -> gson.fromJson(json, AIServiceConfig.OpenAIConfig::class.java)
                "ernie" -> gson.fromJson(json, AIServiceConfig.ErnieConfig::class.java)
                "qwen" -> gson.fromJson(json, AIServiceConfig.QwenConfig::class.java)
                "spark" -> gson.fromJson(json, AIServiceConfig.SparkConfig::class.java)
                "hunyuan" -> gson.fromJson(json, AIServiceConfig.HunyuanConfig::class.java)
                "doubao" -> gson.fromJson(json, AIServiceConfig.DoubaoConfig::class.java)
                "lingyi" -> gson.fromJson(json, AIServiceConfig.LingyiConfig::class.java)
                "jieyue" -> gson.fromJson(json, AIServiceConfig.JieyueConfig::class.java)
                "chatglm" -> gson.fromJson(json, AIServiceConfig.ChatGLMConfig::class.java)
                "tinybert_local" -> gson.fromJson(json, AIServiceConfig.TinyBERTConfig::class.java)
                "tencent_hunyuan" -> gson.fromJson(json, AIServiceConfig.TencentHunyuanConfig::class.java)
                else -> null
            }
        } catch (e: Exception) {
            Log.e("SettingsRepository", "反序列化AIServiceConfig失败", e)
            null
        }
    }
}
