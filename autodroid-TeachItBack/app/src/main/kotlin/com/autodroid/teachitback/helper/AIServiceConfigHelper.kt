package com.autodroid.teachitback.helper

import android.view.View
import android.widget.*
import com.autodroid.teachitback.config.AIServiceConfig

/**
 * AI服务配置辅助类
 * 提供统一的配置界面设置方法
 */
object AIServiceConfigHelper {

    /**
     * 设置AI服务配置UI
     * 根据配置需求动态显示/隐藏字段
     */
    fun setupAIServiceConfigUI(
        secretIdField: View,
        apiKeyField: View,
        baseUrlField: View,
        regionField: View,
        modelField: View,
        config: AIServiceConfig
    ) {
        // 根据AIServiceRequiredFields显示/隐藏字段
        secretIdField.visibility = if (config.requiredFields.requireSecretId) View.VISIBLE else View.GONE
        apiKeyField.visibility = if (config.requiredFields.requireApiKey) View.VISIBLE else View.GONE
        baseUrlField.visibility = if (config.requiredFields.requireBaseUrl) View.VISIBLE else View.GONE
        regionField.visibility = if (config.requiredFields.requireRegion) View.VISIBLE else View.GONE
        modelField.visibility = if (config.requiredFields.requireModel) View.VISIBLE else View.GONE
    }

    /**
     * 获取配置数据
     * @param secretId Secret ID
     * @param apiKey API Key
     * @param baseUrl Base URL
     * @param region Region
     * @param model Model
     * @param baseConfig 基础配置
     * @return 更新后的配置
     */
    fun getConfigData(
        secretId: String,
        apiKey: String,
        baseUrl: String,
        region: String,
        model: String,
        baseConfig: AIServiceConfig
    ): AIServiceConfig {
        return when (baseConfig) {
            is AIServiceConfig.TencentHunyuanConfig -> baseConfig.copy(
                secretId = secretId,
                apiKey = apiKey,
                baseUrl = baseUrl,
                region = region,
                model = model
            )
            is AIServiceConfig.DeepSeekConfig -> baseConfig.copy(
                apiKey = apiKey,
                model = model
            )
            is AIServiceConfig.MiniMaxConfig -> baseConfig.copy(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model
            )
            is AIServiceConfig.BaichuanConfig -> baseConfig.copy(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model
            )
            is AIServiceConfig.OpenAIConfig -> baseConfig.copy(
                apiKey = apiKey,
                model = model
            )
        }
    }

    /**
     * 验证配置数据
     * @param data 配置数据映射
     * @param config 配置对象
     * @return 是否有效
     */
    fun validateConfigData(data: Map<String, String>, config: AIServiceConfig): Boolean {
        // 检查必需字段
        if (config.requiredFields.requireSecretId && data["secretId"].isNullOrBlank()) {
            return false
        }
        if (config.requiredFields.requireApiKey && data["apiKey"].isNullOrBlank()) {
            return false
        }
        if (config.requiredFields.requireBaseUrl && data["baseUrl"].isNullOrBlank()) {
            return false
        }
        if (config.requiredFields.requireRegion && data["region"].isNullOrBlank()) {
            return false
        }
        if (config.requiredFields.requireModel && data["model"].isNullOrBlank()) {
            return false
        }

        // URL格式验证
        data["baseUrl"]?.let { url ->
            if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                return false
            }
        }

        return true
    }

    /**
     * 获取区域列表（腾讯云）
     */
    fun getTencentRegions(): List<String> {
        return listOf(
            "ap-guangzhou",
            "ap-shanghai",
            "ap-beijing",
            "ap-chengdu",
            "ap-chongqing",
            "ap-nanjing",
            "ap-shenzhen-fsi",
            "ap-shanghai-fsi",
            "ap-beijing-fsi"
        )
    }

    /**
     * 获取模型列表
     */
    fun getModelList(config: AIServiceConfig): List<String> {
        return when (config) {
            is AIServiceConfig.TencentHunyuanConfig -> listOf(
                "hunyuan-lite",
                "hunyuan-standard",
                "hunyuan-pro"
            )
            is AIServiceConfig.DeepSeekConfig -> listOf(
                "deepseek-chat",
                "deepseek-coder"
            )
            is AIServiceConfig.MiniMaxConfig -> listOf(
                "abab5.5-chat",
                "abab5.5s-chat"
            )
            is AIServiceConfig.BaichuanConfig -> listOf(
                "Baichuan2-Turbo",
                "Baichuan2-7B",
                "Baichuan2-13B"
            )
            is AIServiceConfig.OpenAIConfig -> listOf(
                "gpt-3.5-turbo",
                "gpt-4",
                "gpt-4-turbo"
            )
        }
    }
}
