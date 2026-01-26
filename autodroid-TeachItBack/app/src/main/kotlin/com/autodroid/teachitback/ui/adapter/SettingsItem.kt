package com.autodroid.teachitback.ui.adapter

/**
 * SettingsFragment异构数据项的密封类
 * 支持设置界面中的所有数据类型：每个设置项都有独立的类型
 */
sealed class SettingsItem {
    
    abstract fun getType(): Int
    
    companion object {
        const val TYPE_SECTION_HEADER = 0
        const val TYPE_DARK_MODE_SWITCH_ITEM = 1
        const val TYPE_AUTO_SAVE_SWITCH_ITEM = 2
        const val TYPE_LANGUAGE_SETTING_ITEM = 3
        const val TYPE_BACKUP_DATA_ITEM = 4
        const val TYPE_RESTORE_DATA_ITEM = 5
        const val TYPE_CLEAR_ALL_DATA_BUTTON_ITEM = 6
        const val TYPE_VERSION_INFO_ITEM = 7
        const val TYPE_HELP_AND_FEEDBACK_ITEM = 8
        const val TYPE_DOUBAO_AI_SERVICE_ITEM = 9
        const val TYPE_DEEPSEEK_AI_SERVICE_ITEM = 10
        const val TYPE_MINIMAX_AI_SERVICE_ITEM = 11
        const val TYPE_KIMI_AI_SERVICE_ITEM = 12
        const val TYPE_OPENAI_AI_SERVICE_ITEM = 13
        const val TYPE_ERNIE_AI_SERVICE_ITEM = 14
        const val TYPE_QWEN_AI_SERVICE_ITEM = 15
        const val TYPE_ZHIPU_AI_SERVICE_ITEM = 16
        const val TYPE_SPARK_AI_SERVICE_ITEM = 17
        const val TYPE_HUNYUAN_AI_SERVICE_ITEM = 18
        const val TYPE_BAICHUAN_AI_SERVICE_ITEM = 19
        const val TYPE_LINGYI_AI_SERVICE_ITEM = 20
        const val TYPE_JIEYUE_AI_SERVICE_ITEM = 21
        const val TYPE_TENCENTCLOUD_AI_SERVICE_ITEM = 22
        const val TYPE_CHATGLM_AI_SERVICE_ITEM = 23
        const val TYPE_TINYBERT_AI_SERVICE_ITEM = 24
    }
    
    /**
     * 分类标题项
     */
    data class SectionHeaderItem(
        val title: String
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_SECTION_HEADER
    }
    
    /**
     * 深色模式开关项
     */
    data class DarkModeSwitchItem(
        val subtitle: String = "启用深色主题",
        val isChecked: Boolean
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_DARK_MODE_SWITCH_ITEM
    }
    
    /**
     * 自动保存开关项
     */
    data class AutoSaveSwitchItem(
        val subtitle: String = "自动保存学习进度",
        val isChecked: Boolean
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_AUTO_SAVE_SWITCH_ITEM
    }
    
    /**
     * 语言设置项
     */
    data class LanguageSettingItem(
        val subtitle: String = "选择应用语言"
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_LANGUAGE_SETTING_ITEM
    }
    
    /**
     * 备份数据项
     */
    data class BackupDataItem(
        val subtitle: String = "备份当前学习数据"
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_BACKUP_DATA_ITEM
    }
    
    /**
     * 恢复数据项
     */
    data class RestoreDataItem(
        val subtitle: String = "从备份恢复数据"
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_RESTORE_DATA_ITEM
    }
    
    /**
     * 清除所有数据按钮项
     */
    data class ClearAllDataButtonItem(
        val isDestructive: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_CLEAR_ALL_DATA_BUTTON_ITEM
    }
    
    /**
     * 版本信息项
     */
    data class VersionInfoItem(
        val version: String = "v1.0.0"
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_VERSION_INFO_ITEM
    }
    
    /**
     * 帮助与反馈项
     */
    data class HelpAndFeedbackItem(
        val subtitle: String = "获取帮助或提供反馈"
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_HELP_AND_FEEDBACK_ITEM
    }
    
    /**
     * 豆包AI服务项
     */
    data class DoubaoAIServiceItem(
        val id: String = "doubao",
        val name: String = "豆包",
        val description: String = "字节跳动AI助手，中文优化，MoE架构，多模态能力强",
        val baseUrl: String = "https://ark.cn-beijing.volces.com/api/v3",
        val defaultModel: String = "doubao-pro-32k",
        val freeQuota: String = "注册即送100万tokens；部分基础功能永久免费",
        val apiKeyUrl: String = "https://developer.doubao.com",
        val officialWebsite: String = "https://www.doubao.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_DOUBAO_AI_SERVICE_ITEM
    }
    
    /**
     * DeepSeek AI服务项
     */
    data class DeepSeekAIServiceItem(
        val id: String = "deepseek",
        val name: String = "DeepSeek",
        val description: String = "深度求索AI，支持长文本处理，数理推理与工程优化突出",
        val baseUrl: String = "https://api.deepseek.com/v1",
        val defaultModel: String = "deepseek-chat",
        val freeQuota: String = "注册赠送50万tokens；部分模型有短期免费调用次数",
        val apiKeyUrl: String = "https://platform.deepseek.com",
        val officialWebsite: String = "https://www.deepseek.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_DEEPSEEK_AI_SERVICE_ITEM
    }
    
    /**
     * MiniMax AI服务项
     */
    data class MinimaxAIServiceItem(
        val id: String = "minimax",
        val name: String = "MiniMax",
        val description: String = "稀宇科技，代码生成与Agent能力突出，M2开源且商用友好",
        val baseUrl: String = "https://api.minimax.chat/v1",
        val defaultModel: String = "abab5.5-chat",
        val freeQuota: String = "新用户注册送15元API余额；M2开源版可免费商用",
        val apiKeyUrl: String = "https://platform.minimaxi.com",
        val officialWebsite: String = "https://minimax.chat",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_MINIMAX_AI_SERVICE_ITEM
    }
    
    /**
     * Kimi AI服务项
     */
    data class KimiAIServiceItem(
        val id: String = "kimi",
        val name: String = "Kimi",
        val description: String = "月之暗面，20万汉字上下文窗口，长文档处理效率高",
        val baseUrl: String = "https://api.moonshot.cn/v1",
        val defaultModel: String = "moonshot-v1-8k",
        val freeQuota: String = "新用户免费额度80万tokens，部分基础功能无调用限制",
        val apiKeyUrl: String = "https://platform.moonshot.cn",
        val officialWebsite: String = "https://kimi.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_KIMI_AI_SERVICE_ITEM
    }
    
    /**
     * OpenAI AI服务项
     */
    data class OpenAIServiceItem(
        val id: String = "openai",
        val name: String = "OpenAI",
        val description: String = "国际领先AI模型，通用性强，多语言支持",
        val baseUrl: String = "https://api.openai.com/v1",
        val defaultModel: String = "gpt-3.5-turbo",
        val freeQuota: String = "新用户赠送18美元免费额度",
        val apiKeyUrl: String = "https://platform.openai.com",
        val officialWebsite: String = "https://openai.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_OPENAI_AI_SERVICE_ITEM
    }
    
    /**
     * 文心一言AI服务项
     */
    data class ErnieAIServiceItem(
        val id: String = "ernie",
        val name: String = "文心一言",
        val description: String = "百度AI大模型，中文语义理解突出，适配国产芯片",
        val baseUrl: String = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1",
        val defaultModel: String = "ernie-bot-turbo",
        val freeQuota: String = "新用户免费额度100万tokens，有效期3个月",
        val apiKeyUrl: String = "https://console.bce.baidu.com/qianfan",
        val officialWebsite: String = "https://yiyan.baidu.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_ERNIE_AI_SERVICE_ITEM
    }
    
    /**
     * 通义千问AI服务项
     */
    data class QwenAIServiceItem(
        val id: String = "qwen",
        val name: String = "通义千问",
        val description: String = "阿里巴巴AI大模型，多模态性能优异，开源生态活跃",
        val baseUrl: String = "https://dashscope.aliyuncs.com/api/v1",
        val defaultModel: String = "qwen-turbo",
        val freeQuota: String = "新用户赠送100万tokens免费额度",
        val apiKeyUrl: String = "https://dashscope.aliyuncs.com",
        val officialWebsite: String = "https://tongyi.aliyun.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_QWEN_AI_SERVICE_ITEM
    }
    
    /**
     * 智谱AI服务项
     */
    data class ZhipuAIServiceItem(
        val id: String = "zhipu",
        val name: String = "智谱AI",
        val description: String = "清华系AI大模型，代码生成能力强，开源生态完善",
        val baseUrl: String = "https://open.bigmodel.cn/api/paas/v3",
        val defaultModel: String = "chatglm_turbo",
        val freeQuota: String = "GLM-4.7-Flash免费调用；新用户初始额度50万tokens",
        val apiKeyUrl: String = "https://open.bigmodel.cn",
        val officialWebsite: String = "https://www.zhipuai.cn",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_ZHIPU_AI_SERVICE_ITEM
    }
    
    /**
     * 讯飞星火AI服务项
     */
    data class SparkAIServiceItem(
        val id: String = "spark",
        val name: String = "讯飞星火",
        val description: String = "科大讯飞AI大模型，语音交互与方言识别能力突出",
        val baseUrl: String = "https://spark-api.xf-yun.com/v1.1",
        val defaultModel: String = "general",
        val freeQuota: String = "新用户免费额度60万tokens，语音功能有额外免费时长",
        val apiKeyUrl: String = "https://console.xfyun.cn/services/spark",
        val officialWebsite: String = "https://xinghuo.xfyun.cn",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_SPARK_AI_SERVICE_ITEM
    }
    
    /**
     * 混元AI服务项
     */
    data class HunyuanAIServiceItem(
        val id: String = "hunyuan",
        val name: String = "混元大模型",
        val description: String = "腾讯AI大模型，支持100万字长文本，无缝对接微信生态",
        val baseUrl: String = "https://hunyuan.tencent.com",
        val defaultModel: String = "hunyuan-standard",
        val freeQuota: String = "新用户免费调用额度50万tokens，有效期1个月",
        val apiKeyUrl: String = "https://cloud.tencent.com/product/hunyuan",
        val officialWebsite: String = "https://混元.tencent.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_HUNYUAN_AI_SERVICE_ITEM
    }
    
    /**
     * 百川AI服务项
     */
    data class BaichuanAIServiceItem(
        val id: String = "baichuan",
        val name: String = "百川智能",
        val description: String = "中文理解与创作能力强，开源模型生态完善，适配中小企业",
        val baseUrl: String = "https://api.baichuan-ai.com",
        val defaultModel: String = "baichuan2-7b-chat",
        val freeQuota: String = "新用户免费额度50万tokens，开源版本可免费商用",
        val apiKeyUrl: String = "https://platform.baichuan-ai.com",
        val officialWebsite: String = "https://www.baichuan-ai.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_BAICHUAN_AI_SERVICE_ITEM
    }
    
    /**
     * 零一万物AI服务项
     */
    data class LingyiAIServiceItem(
        val id: String = "lingyi",
        val name: String = "零一万物",
        val description: String = "轻量化部署优势明显，多模态与小样本学习能力强",
        val baseUrl: String = "https://open.lingyiwanwu.com",
        val defaultModel: String = "yi-34b-chat",
        val freeQuota: String = "新用户免费额度30万tokens，部分轻量模型可免费商用",
        val apiKeyUrl: String = "https://open.lingyiwanwu.com",
        val officialWebsite: String = "https://www.lingyiwanwu.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_LINGYI_AI_SERVICE_ITEM
    }
    
    /**
     * 阶跃AI服务项
     */
    data class JieyueAIServiceItem(
        val id: String = "jieyue",
        val name: String = "阶跃星辰",
        val description: String = "高效推理与低延迟响应，适配工业互联网与物联网场景",
        val baseUrl: String = "https://open.jieyuesx.com",
        val defaultModel: String = "jieyue-standard",
        val freeQuota: String = "新用户免费额度30万tokens，特定垂类场景有额外试用权益",
        val apiKeyUrl: String = "https://open.jieyuesx.com",
        val officialWebsite: String = "https://www.jieyuesx.com",
        val isEnabled: Boolean = true
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_JIEYUE_AI_SERVICE_ITEM
    }

    /**
     * 腾讯云API密钥配置项
     */
    data class TencentCloudAIServiceItem(
        val apiKey: String = "",
        val secretId: String = "",
        val testMode: Boolean = false,
        val enabled: Boolean = false,
        val region: String = "ap-guangzhou",
        val onApiKeyChanged: (String) -> Unit = {},
        val onSecretIdChanged: (String) -> Unit = {},
        val onTestModeChanged: (Boolean) -> Unit = {},
        val onEnabledChanged: (Boolean) -> Unit = {},
        val onRegionChanged: (String) -> Unit = {}
    ) : SettingsItem() {       
        override fun getType(): Int = TYPE_TENCENTCLOUD_AI_SERVICE_ITEM
    }

    /**
     * ChatGLM嵌入式AI服务项
     */
    data class ChatGLMAIServiceItem(
        val id: String = "chatglm",
        val name: String = "ChatGLM",
        val description: String = "清华开源6B模型，本地化部署，中文语义理解优秀",
        val defaultModel: String = "chatglm-6b",
        val isEnabled: Boolean = true,
        val priority: Int = 100,
        val modelDownloadStrategy: String = "AUTO",
        val isModelDownloaded: Boolean = false,
        val modelDownloadProgress: Float = 0.0f,
        val modelSize: String = "6GB",
        val githubRepo: String = "THUDM/ChatGLM-6B"
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_CHATGLM_AI_SERVICE_ITEM
    }
    
    /**
     * TinyBERT嵌入式AI服务项
     */
    data class TinyBERTAIServiceItem(
        val id: String = "tinybert",
        val name: String = "TinyBERT",
        val description: String = "超轻量级50MB模型，设备端推理，推理速度快",
        val defaultModel: String = "tinybert-L-4",
        val isEnabled: Boolean = true,
        val priority: Int = 100,
        val modelDownloadStrategy: String = "AUTO",
        val isModelDownloaded: Boolean = false,
        val modelDownloadProgress: Float = 0.0f,
        val modelSize: String = "50MB",
        val githubRepo: String = "huawei-noah/TinyBERT"
    ) : SettingsItem() {
        override fun getType(): Int = TYPE_TINYBERT_AI_SERVICE_ITEM
    }
}
