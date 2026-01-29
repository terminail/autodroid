package com.autodroid.teachitback.config

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

/**
 * AI模型数据类
 * 包含模型名称和描述信息
 */
@Parcelize
data class AIModel(
    val name: String,
    val description: String
) : Parcelable

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
    
    // 使用 abstract 避免父类生成字段，防止 Gson 序列化时的重复字段冲突
    abstract val availableModels: List<AIModel>
    
    // AI能力配置
    abstract val capabilities: AIServiceCapability
    
    // 配额和定价
    abstract val freeQuota: Long
    abstract val pricePerMillion: Double

    // 服务启用状态
    abstract val isEnabled: Boolean

    // 服务状态
    abstract val status: AIServiceStatus
    
    /**
     * TencentHunyuanConfig
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("hunyuan-lite", "轻量版，响应速度快，适合日常对话"),
            AIModel("hunyuan-standard", "标准版，平衡性能与成本"),
            AIModel("hunyuan-pro", "专业版，复杂任务处理能力强"),
            AIModel("hunyuan-turbo", "极速版，超低延迟")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.FULL_CAPABILITIES,
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 12.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("deepseek-chat", "通用对话模型，擅长长文本和数理计算"),
            AIModel("deepseek-coder", "代码生成专用模型，支持多种编程语言")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportMath(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 1.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val apiKey: String = com.autodroid.teachitback.BuildConfig.GLM47FLASH_API_KEY,
        override val baseUrl: String = "https://api.minimax.chat/v1",
        override val region: String = "",
        override val model: String = "abab5.5-chat",
        override val availableModels: List<AIModel> = listOf(
            AIModel("abab5.5-chat", "通用对话模型，多模态能力强"),
            AIModel("abab5.5s-chat", "快速响应版本"),
            AIModel("abab6-chat", "最新版本，性能更强")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportCreativeWriting(true)
            .supportMultimodal(true)
            .supportLongText(true),
        override val freeQuota: Long = 300000,
        override val pricePerMillion: Double = 5.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("Baichuan2-Turbo", "高速版，响应快，适合实时对话"),
            AIModel("Baichuan2-53B", "大参数版本，理解能力强"),
            AIModel("Baichuan3-Turbo", "第三代模型，性能更优")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.BASIC_CHAT,
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 4.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("moonshot-v1-8k", "8K上下文，适合短文本对话"),
            AIModel("moonshot-v1-32k", "32K上下文，支持长文档处理"),
            AIModel("moonshot-v1-128k", "128K上下文，超长文本处理能力强")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportMath(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 800000,
        override val pricePerMillion: Double = 12.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("gpt-3.5-turbo", "经济实用，适合日常任务"),
            AIModel("gpt-4", "强大智能，复杂任务处理"),
            AIModel("gpt-4-turbo", "GPT-4加速版"),
            AIModel("gpt-4o", "最新多模态模型")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.FULL_CAPABILITIES,
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 20.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("ernie-bot-4", "最新版本，综合能力强"),
            AIModel("ernie-bot-3.5", "稳定版本，性价比高"),
            AIModel("ernie-bot-turbo", "高速响应版本"),
            AIModel("ernie-speed", "极速版，超低延迟")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 8.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("qwen-turbo", "高速版，响应快，性价比高"),
            AIModel("qwen-plus", "增强版，综合能力强"),
            AIModel("qwen-max", "旗舰版，最强性能"),
            AIModel("qwen-long", "长文本版本，支持超长上下文")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 6.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val description: String = "智谱AI大模型，支持端云协同，响应速度快，免费额度充足",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_KEY_ONLY,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "https://open.bigmodel.cn/api/paas/v4",
        override val region: String = "",
        override val model: String = "glm-4-flash",
        override val availableModels: List<AIModel> = listOf(
            AIModel("glm-4-flash", "免费版，响应速度快，适合日常使用"),
            AIModel("glm-4", "标准版，综合能力强"),
            AIModel("glm-4-air", "轻量版，成本低"),
            AIModel("glm-4-plus", "增强版，性能更强"),
            AIModel("glm-3-turbo", "极速版，超低延迟"),
            AIModel("chatglm_turbo", "ChatGLM高速版"),
            AIModel("chatglm_pro", "ChatGLM专业版"),
            AIModel("chatglm3-6b", "ChatGLM3-6B开源模型")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportEducation(true)
            .supportAnswerEvaluation(true)
            .supportSocraticQuestioning(true)
            .supportLearningAnalysis(true)
            .supportConceptExtraction(true)
            .supportMath(true)
            .supportCreativeWriting(true)
            .supportFileProcessing(true)
            .supportMindMapGeneration(true)
            .supportDocumentParsing(true)
            .supportLongText(true)
            .supportCodeGeneration(true),
        override val freeQuota: Long = 1_000_000L,
        override val pricePerMillion: Double = 0.7,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("spark-lite", "轻量版，响应快"),
            AIModel("spark-pro", "专业版，综合能力强"),
            AIModel("spark-max", "旗舰版，最强性能"),
            AIModel("spark-4.0", "最新版本，全面升级")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 10.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("hunyuan-lite", "轻量版，响应快"),
            AIModel("hunyuan-standard", "标准版，平衡性能"),
            AIModel("hunyuan-pro", "专业版，综合能力强")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 12.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("doubao-pro-32k", "专业版，32K上下文"),
            AIModel("doubao-pro-128k", "专业版，128K长上下文"),
            AIModel("doubao-lite-32k", "轻量版，32K上下文，成本低")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 1000000,
        override val pricePerMillion: Double = 8.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("yi-34b-chat", "34B参数版本，综合能力强"),
            AIModel("yi-6b-chat", "6B参数版本，响应快"),
            AIModel("yi-100b-chat", "100B参数版本，最强性能")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 10.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
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
        override val availableModels: List<AIModel> = listOf(
            AIModel("jieyue-chat", "基础版本，适合日常对话"),
            AIModel("jieyue-pro", "专业版本，综合能力强"),
            AIModel("jieyue-max", "旗舰版本，最强性能")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportLongText(true)
            .supportCodeGeneration(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = 500000,
        override val pricePerMillion: Double = 10.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK
    ) : AIServiceConfig()
    
    /**
     * ChatGLM 嵌入式AI配置
     * 清华开源6B模型，本地化部署
     */
    @Parcelize
    data class ChatGLMConfig(
        override val id: String = "chatglm",
        override val name: String = "ChatGLM",
        override val displayName: String = "ChatGLM",
        override val description: String = "清华开源6B模型，本地化部署，中文语义理解优秀",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.NO_REQUIRED_FIELDS,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "local://chatglm",
        override val region: String = "",
        override val model: String = "chatglm-6b-int4",
        override val availableModels: List<AIModel> = listOf(
            AIModel("chatglm-6b-int4", "6B INT4量化版，内存占用小"),
            AIModel("chatglm-6b-int8", "6B INT8量化版，精度更高"),
            AIModel("chatglm3-6b-int4", "ChatGLM3 INT4量化版，性能更强")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.EMPTY
            .supportBasicChat(true)
            .supportEducation(true)
            .supportAnswerEvaluation(true)
            .supportSocraticQuestioning(true)
            .supportLearningAnalysis(true)
            .supportConceptExtraction(true)
            .supportMath(true)
            .supportCreativeWriting(true),
        override val freeQuota: Long = Long.MAX_VALUE,
        override val pricePerMillion: Double = 0.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK,
        // 嵌入式服务专用字段
        val modelFilePath: String = "models/chatglm-6b-int4.mnn",
        val requiresDownload: Boolean = true,
        val downloadSize: Long = 2_800_000_000L,
        val maxTokens: Int = 2048,
        val temperature: Float = 0.7f
    ) : AIServiceConfig()

    /**
     * TinyBERT 嵌入式AI配置
     * 超轻量级50MB模型，设备端推理
     */
    @Parcelize
    data class TinyBERTConfig(
        override val id: String = "tinybert_local",
        override val name: String = "TinyBERT",
        override val displayName: String = "TinyBERT（本地）",
        override val description: String = "超轻量级50MB模型，设备端推理，推理速度快",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.NO_REQUIRED_FIELDS,
        override val secretId: String = "",
        override val apiKey: String = "",
        override val baseUrl: String = "local://tinybert",
        override val region: String = "",
        override val model: String = "tinybert-int8",
        override val availableModels: List<AIModel> = listOf(
            AIModel("tinybert-int8", "INT8量化版，精度高"),
            AIModel("tinybert-int4", "INT4量化版，内存占用更小")
        ),
        override val capabilities: AIServiceCapability = AIServiceCapability.BASIC_CHAT
            .supportAnswerEvaluation(true),
        override val freeQuota: Long = Long.MAX_VALUE,
        override val pricePerMillion: Double = 0.0,
        override val isEnabled: Boolean = false,
        override val status: AIServiceStatus = AIServiceStatus.STATUS_OK,
        // 嵌入式服务专用字段
        val modelFilePath: String = "models/tinybert-int8.mnn",
        val requiresDownload: Boolean = true,
        val downloadSize: Long = 14_720_116L,
        val threshold: Float = 0.5f,
        val maxResponseTime: Long = 200L
    ) : AIServiceConfig()
}
