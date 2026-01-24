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
        override val apiKey: String = com.autodroid.teachitback.BuildConfig.DEEPSEEK_API_KEY,
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
     * Kimi配置
     * 月之暗面，20万汉字上下文窗口，长文档处理效率高
     */
    @Parcelize
    data class KimiConfig(
        override val id: String = "kimi",
        override val name: String = "Kimi",
        override val displayName: String = "Kimi",
        override val description: String = "月之暗面大模型，20万汉字上下文窗口，长文档处理效率高",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = com.autodroid.teachitback.BuildConfig.KIMI_API_KEY,
        override val baseUrl: String = "https://api.moonshot.cn/v1",
        override val region: String = "",
        override val model: String = "moonshot-v1-8k",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportMath(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 800000, // 80万token
        override val pricePerMillion: Double = 12.0
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

    /**
     * Ernie配置
     * 百度文心一言，中文优化
     */
    @Parcelize
    data class ErnieConfig(
        override val id: String = "ernie",
        override val name: String = "Ernie",
        override val displayName: String = "文心一言",
        override val description: String = "百度文心一言大模型，中文优化",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1",
        override val region: String = "",
        override val model: String = "ernie-bot-4",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 8.0
    ) : AIServiceConfig()

    /**
     * Qwen配置
     * 阿里通义千问
     */
    @Parcelize
    data class QwenConfig(
        override val id: String = "qwen",
        override val name: String = "Qwen",
        override val displayName: String = "通义千问",
        override val description: String = "阿里通义千问大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        override val region: String = "",
        override val model: String = "qwen-turbo",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 6.0
    ) : AIServiceConfig()

    /**
     * Zhipu配置
     * 智谱AI
     */
    @Parcelize
    data class ZhipuConfig(
        override val id: String = "zhipu",
        override val name: String = "Zhipu",
        override val displayName: String = "智谱AI",
        override val description: String = "智谱AI大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://open.bigmodel.cn/api/paas/v4",
        override val region: String = "",
        override val model: String = "glm-4",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 15.0
    ) : AIServiceConfig()

    /**
     * Spark配置
     * 讯飞星火
     */
    @Parcelize
    data class SparkConfig(
        override val id: String = "spark",
        override val name: String = "Spark",
        override val displayName: String = "讯飞星火",
        override val description: String = "讯飞星火大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://spark-api.xf-yun.com/v1",
        override val region: String = "",
        override val model: String = "spark-lite",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 10.0
    ) : AIServiceConfig()

    /**
     * Hunyuan配置
     * 腾讯混元
     */
    @Parcelize
    data class HunyuanConfig(
        override val id: String = "hunyuan",
        override val name: String = "Hunyuan",
        override val displayName: String = "混元大模型",
        override val description: String = "腾讯混元大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://hunyuan.tencentcloudapi.com/v1",
        override val region: String = "",
        override val model: String = "hunyuan-lite",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 12.0
    ) : AIServiceConfig()

    /**
     * Doubao配置
     * 豆包AI
     */
    @Parcelize
    data class DoubaoConfig(
        override val id: String = "doubao",
        override val name: String = "Doubao",
        override val displayName: String = "豆包",
        override val description: String = "字节跳动豆包大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://ark.cn-beijing.volces.com/api/v3",
        override val region: String = "",
        override val model: String = "doubao-pro-32k",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 8.0
    ) : AIServiceConfig()

    /**
     * Lingyi配置
     * 零一万物
     */
    @Parcelize
    data class LingyiConfig(
        override val id: String = "lingyi",
        override val name: String = "Lingyi",
        override val displayName: String = "零一万物",
        override val description: String = "零一万物大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://api.lingyiwanwu.com/v1",
        override val region: String = "",
        override val model: String = "yi-34b-chat",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 10.0
    ) : AIServiceConfig()

    /**
     * Jieyue配置
     * 阶跃星辰
     */
    @Parcelize
    data class JieyueConfig(
        override val id: String = "jieyue",
        override val name: String = "Jieyue",
        override val displayName: String = "阶跃星辰",
        override val description: String = "阶跃星辰大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://api.jieyuesx.com/v1",
        override val region: String = "",
        override val model: String = "jieyue-chat",
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 10.0
    ) : AIServiceConfig()
}
