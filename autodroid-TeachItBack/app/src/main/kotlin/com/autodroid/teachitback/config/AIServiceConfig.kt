package com.autodroid.teachitback.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AI服务配置密封类
 * 统一管理所有AI服务的配置，支持类型安全和统一配置界面
 */
sealed class AIServiceConfig : Parcelable {
    abstract val id: String
    abstract val name: String
    abstract val displayName: String
    abstract val description: String
    
    // 配置需求
    abstract val requiredFields: AIServiceRequiredFields
    
    // 配置值
    abstract val secretId: String
    abstract val apiKey: String
    abstract val baseUrl: String
    abstract val region: String
    abstract val model: String
    
    // AI能力配置
    abstract val capabilities: AIServiceCapability
    
    // 配额和定价
    abstract val freeQuota: Long
    abstract val pricePerMillion: Double
    
    /**
     * 腾讯云混元配置
     * 支持全功能，教育专用
     */
    @Parcelize
    data class TencentHunyuanConfig(
        override val id: String = "tencent-hunyuan",
        override val name: String = "Tencent Hunyuan",
        override val displayName: String = "腾讯云混元",
        override val description: String = "腾讯云混元大模型，教育专用全功能",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.CLOUD_SERVICE,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://hunyuan.tencentcloudapi.com",
        override val region: String = "ap-guangzhou",
        override val model: String = "hunyuan-lite",
        override val capabilities: AIServiceCapability = AIServiceCapability.FULL_CAPABILITIES,
        override val freeQuota: Long = 1000000, // 100万token
        override val pricePerMillion: Double = 12.0
    ) : AIServiceConfig()
    
    /**
     * DeepSeek配置
     * 性价比高，擅长长文本和数理计算
     */
    @Parcelize
    data class DeepSeekConfig(
        override val id: String = "deepseek",
        override val name: String = "DeepSeek",
        override val displayName: String = "DeepSeek",
        override val description: String = "深度求索大模型，擅长长文本和数理计算",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://api.deepseek.com/v1",
        override val region: String = "",
        override val model: String = "deepseek-chat",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportMath(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000, // 50万token
        override val pricePerMillion: Double = 1.0
    ) : AIServiceConfig()
    
    /**
     * MiniMax配置
     * 多模态能力强，支持创意写作
     */
    @Parcelize
    data class MiniMaxConfig(
        override val id: String = "minimax",
        override val name: String = "MiniMax",
        override val displayName: String = "MiniMax",
        override val description: String = "稀宇科技大模型，多模态和创意写作",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_AND_URL,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://api.minimax.chat/v1",
        override val region: String = "",
        override val model: String = "abab5.5-chat",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportCreativeWriting(true)
            .supportMultimodal(true)
            .supportLongText(true),
        override val freeQuota: Long = 300000, // 30万token
        override val pricePerMillion: Double = 5.0
    ) : AIServiceConfig()
    
    /**
     * 百川配置
     * 基础对话功能
     */
    @Parcelize
    data class BaichuanConfig(
        override val id: String = "baichuan",
        override val name: String = "Baichuan",
        override val displayName: String = "百川大模型",
        override val description: String = "百川智能大模型，基础对话功能",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_AND_URL,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://api.baichuan-ai.com/v1",
        override val region: String = "",
        override val model: String = "Baichuan2-Turbo",
        override val capabilities: AIServiceCapability = AIServiceCapability.BASIC_CHAT,
        override val freeQuota: Long = 1000000, // 100万token
        override val pricePerMillion: Double = 4.0
    ) : AIServiceConfig()
    
    /**
     * OpenAI配置
     * 国际标准，功能全面
     */
    @Parcelize
    data class OpenAIConfig(
        override val id: String = "openai",
        override val name: String = "OpenAI",
        override val displayName: String = "OpenAI",
        override val description: String = "OpenAI大模型，国际标准，功能全面",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://api.openai.com/v1",
        override val region: String = "",
        override val model: String = "gpt-3.5-turbo",
        override val capabilities: AIServiceCapability = AIServiceCapability.FULL_CAPABILITIES,
        override val freeQuota: Long = 500000, // 50万token
        override val pricePerMillion: Double = 20.0
    ) : AIServiceConfig()
}
