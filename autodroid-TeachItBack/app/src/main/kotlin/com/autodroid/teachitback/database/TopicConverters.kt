package com.autodroid.teachitback.database

import android.util.Log
import androidx.room.TypeConverter
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.service.AIAbility
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TopicConverters - Room数据库类型转换器
 * 用于将复杂对象（List、Set、Map等）转换为Room可存储的基本类型
 * 使用Gson进行序列化和反序列化
 */
object TopicConverters {

    private val gson = Gson()

    // List<String> 转换
    @JvmStatic
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @JvmStatic
    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson<List<String>>(json, type) ?: emptyList()
    }

    // Set<AIAbility> 转换
    @JvmStatic
    @TypeConverter
    fun fromAIAbilitySet(set: Set<AIAbility>?): String {
        if (set == null) return "[]"
        return gson.toJson(set.map { it.name })
    }

    @JvmStatic
    @TypeConverter
    fun toAIAbilitySet(json: String?): Set<AIAbility> {
        if (json.isNullOrBlank()) return emptySet()
        val names = gson.fromJson(json, Array<String>::class.java) ?: arrayOf()
        return names.map { AIAbility.valueOf(it) }.toSet()
    }

    // Map<String, Double> 转换
    @JvmStatic
    @TypeConverter
    fun fromServicePreferencesMap(map: Map<String, Double>?): String {
        return gson.toJson(map ?: emptyMap<String, Double>())
    }

    @JvmStatic
    @TypeConverter
    fun toServicePreferencesMap(json: String?): Map<String, Double> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson<Map<String, Double>>(json, type) ?: emptyMap()
    }
    
    // AIServiceConfig 转换 - 将配置对象转换为JSON字符串以便存储
    @JvmStatic
    @TypeConverter
    fun fromAIServiceConfig(config: AIServiceConfig?): String {
        if (config == null) return ""
        
        // 将配置序列化为Map，以便后续序列化为JSON
        val map = mutableMapOf<String, Any?>()
        map["type"] = when (config) {
            is AIServiceConfig.TencentHunyuanConfig -> "TencentHunyuanConfig"
            is AIServiceConfig.DeepSeekConfig -> "DeepSeekConfig"
            is AIServiceConfig.KimiConfig -> "KimiConfig"
            is AIServiceConfig.MiniMaxConfig -> "MiniMaxConfig"
            is AIServiceConfig.BaichuanConfig -> "BaichuanConfig"
            is AIServiceConfig.OpenAIConfig -> "OpenAIConfig"
            is AIServiceConfig.ErnieConfig -> "ErnieConfig"
            is AIServiceConfig.QwenConfig -> "QwenConfig"
            is AIServiceConfig.ZhipuConfig -> "ZhipuConfig"
            is AIServiceConfig.SparkConfig -> "SparkConfig"
            is AIServiceConfig.HunyuanConfig -> "HunyuanConfig"
            is AIServiceConfig.DoubaoConfig -> "DoubaoConfig"
            is AIServiceConfig.LingyiConfig -> "LingyiConfig"
            is AIServiceConfig.JieyueConfig -> "JieyueConfig"
            is AIServiceConfig.ChatGLMConfig -> "ChatGLMConfig"
            is AIServiceConfig.TinyBERTConfig -> "TinyBERTConfig"
        }
        
        // 基本字段
        map["id"] = config.id
        map["name"] = config.name
        map["displayName"] = config.displayName
        map["description"] = config.description
        map["secretId"] = config.secretId
        map["apiKey"] = config.apiKey
        map["baseUrl"] = config.baseUrl
        map["region"] = config.region
        map["model"] = config.model
        map["freeQuota"] = config.freeQuota
        map["pricePerMillion"] = config.pricePerMillion
        map["isEnabled"] = config.isEnabled
        map["statusCode"] = config.status.code
        map["statusDesc"] = config.status.description
        
        // 特殊字段（仅ChatGLM和TinyBERT）
        if (config is AIServiceConfig.ChatGLMConfig) {
            map["modelFilePath"] = config.modelFilePath
            map["requiresDownload"] = config.requiresDownload
            map["downloadSize"] = config.downloadSize
            map["maxTokens"] = config.maxTokens
            map["temperature"] = config.temperature
        } else if (config is AIServiceConfig.TinyBERTConfig) {
            map["modelFilePath"] = config.modelFilePath
            map["requiresDownload"] = config.requiresDownload
            map["downloadSize"] = config.downloadSize
            map["threshold"] = config.threshold
            map["maxResponseTime"] = config.maxResponseTime
        }
        
        return gson.toJson(map)
    }

    @JvmStatic
    @TypeConverter
    fun toAIServiceConfig(json: String?): AIServiceConfig? {
        if (json.isNullOrBlank()) return null
        
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, type) ?: return null
            
            val typeName = map["type"] as? String ?: return null
            val isEnabled = map["isEnabled"] as? Boolean ?: false
            
            when (typeName) {
                "TencentHunyuanConfig" -> AIServiceConfig.TencentHunyuanConfig(
                    id = map["id"] as? String ?: "",
                    name = map["name"] as? String ?: "",
                    displayName = map["displayName"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    freeQuota = (map["freeQuota"] as? Double)?.toLong() ?: 0L,
                    pricePerMillion = (map["pricePerMillion"] as? Double) ?: 0.0,
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode(
                        (map["statusCode"] as? Double)?.toInt() ?: 200,
                        map["statusDesc"] as? String
                    )
                )
                "DeepSeekConfig" -> AIServiceConfig.DeepSeekConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "KimiConfig" -> AIServiceConfig.KimiConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "MiniMaxConfig" -> AIServiceConfig.MiniMaxConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "BaichuanConfig" -> AIServiceConfig.BaichuanConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "OpenAIConfig" -> AIServiceConfig.OpenAIConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "ErnieConfig" -> AIServiceConfig.ErnieConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "QwenConfig" -> AIServiceConfig.QwenConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "ZhipuConfig" -> AIServiceConfig.ZhipuConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "SparkConfig" -> AIServiceConfig.SparkConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "HunyuanConfig" -> AIServiceConfig.HunyuanConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "DoubaoConfig" -> AIServiceConfig.DoubaoConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "LingyiConfig" -> AIServiceConfig.LingyiConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "JieyueConfig" -> AIServiceConfig.JieyueConfig(
                    secretId = map["secretId"] as? String ?: "",
                    apiKey = map["apiKey"] as? String ?: "",
                    baseUrl = map["baseUrl"] as? String ?: "",
                    region = map["region"] as? String ?: "",
                    model = map["model"] as? String ?: "",
                    isEnabled = isEnabled,
                    status = AIServiceStatus.fromCode((map["statusCode"] as? Double)?.toInt() ?: 200, map["statusDesc"] as? String)
                )
                "ChatGLMConfig" -> AIServiceConfig.ChatGLMConfig(
                    modelFilePath = map["modelFilePath"] as? String ?: "models/chatglm-6b-int4.mnn",
                    requiresDownload = map["requiresDownload"] as? Boolean ?: true,
                    downloadSize = (map["downloadSize"] as? Double)?.toLong() ?: 2_800_000_000L,
                    maxTokens = (map["maxTokens"] as? Double)?.toInt() ?: 2048,
                    temperature = (map["temperature"] as? Double)?.toFloat() ?: 0.7f,
                    isEnabled = isEnabled
                )
                "TinyBERTConfig" -> AIServiceConfig.TinyBERTConfig(
                    modelFilePath = map["modelFilePath"] as? String ?: "models/tinybert-int8.mnn",
                    requiresDownload = map["requiresDownload"] as? Boolean ?: true,
                    downloadSize = (map["downloadSize"] as? Double)?.toLong() ?: 14_720_116L,
                    threshold = (map["threshold"] as? Double)?.toFloat() ?: 0.5f,
                    maxResponseTime = (map["maxResponseTime"] as? Double)?.toLong() ?: 200L,
                    isEnabled = isEnabled
                )
                else -> null
            }
        } catch (e: Exception) {
            Log.e("TopicConverters", "反序列化AIServiceConfig失败: ${e.message}", e)
            null
        }
    }
}

