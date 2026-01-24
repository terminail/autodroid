package com.autodroid.teachitback.service

import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.model.*
// import com.autodroid.teachitback.util.AIServiceConfigHelper

/**
 * AI能力枚举 - 消除隐藏的字符串代码
 */
enum class AIAbility {
    BASIC_CHAT,
    FILE_PROCESSING,
    MIND_MAP_GENERATION,
    LEARNING_ANALYSIS,
    SOCRATIC_QUESTIONING,
    ANSWER_EVALUATION,
    DOCUMENT_PARSING,
    CONCEPT_EXTRACTION,
    KNOWLEDGE_GRAPH,
    LONG_TEXT,
    MULTIMODAL,
    EDUCATION,
    CODE_GENERATION,
    MATH,
    CREATIVE_WRITING,
    IMAGE_ANALYSIS,
    IMAGE_GENERATION,
    AUDIO_PROCESSING,
    VIDEO_ANALYSIS,
    
    // RAG能力 - 基于腾讯云RAG链路解耦能力
    RAG_DOCUMENT_PARSING,        // 文档解析
    RAG_TEXT_SPLITTING,          // 文本拆分
    RAG_EMBEDDING,               // 向量嵌入
    RAG_MULTI_TURN_REWRITING,    // 多轮改写
    RAG_RE_RANKING,              // 重排序
    RAG_RETRIEVAL,               // 检索
    RAG_GENERATION               // 生成
}

/**
 * AI路由服务 - 根据功能需求智能选择AI提供商
 * 使用清晰的布尔属性替代隐藏的字符串代码
 */
class AIRouterService {
    
    // 配置各AI服务的能力偏好
    private val aiServices: Map<String, AIService> = mutableMapOf()
    
    /**
     * 能力到服务优先级映射 - 使用枚举替代字符串
     */
    private val abilityRouting = mapOf(
        // 基础对话功能 - 多模型支持，成本优先
        AIAbility.BASIC_CHAT to listOf("deepseek", "doubao", "baidu", "alibaba", "openai", "zhipu", "minimax", "hunyuan"),
        
        // 文件处理 - 长文本处理能力强的模型
        AIAbility.FILE_PROCESSING to listOf("deepseek", "kimi", "doubao", "baidu", "alibaba", "openai"),
        
        // 教育专用功能 - 教育场景优化
        AIAbility.MIND_MAP_GENERATION to listOf("tencent", "baidu", "alibaba", "deepseek", "openai", "hunyuan"),
        AIAbility.LEARNING_ANALYSIS to listOf("tencent", "baidu", "alibaba", "openai", "deepseek", "hunyuan"),
        AIAbility.SOCRATIC_QUESTIONING to listOf("tencent", "baidu", "alibaba", "deepseek", "openai", "hunyuan"),
        AIAbility.ANSWER_EVALUATION to listOf("tencent", "baidu", "alibaba", "openai", "deepseek", "hunyuan"),
        
        // 知识处理功能 - 理解能力强的模型
        AIAbility.DOCUMENT_PARSING to listOf("deepseek", "kimi", "baidu", "alibaba", "openai", "hunyuan"),
        AIAbility.CONCEPT_EXTRACTION to listOf("deepseek", "baidu", "alibaba", "tencent", "openai", "hunyuan"),
        AIAbility.KNOWLEDGE_GRAPH to listOf("tencent", "baidu", "alibaba", "deepseek", "openai", "hunyuan"),
        
        // 其他功能
        AIAbility.CODE_GENERATION to listOf("baidu", "deepseek", "alibaba", "openai", "hunyuan"),
        AIAbility.MATH to listOf("baidu", "deepseek", "alibaba", "openai"),
        AIAbility.CREATIVE_WRITING to listOf("baidu", "alibaba", "openai", "hunyuan"),
        AIAbility.MULTIMODAL to listOf("openai", "alibaba", "baidu", "hunyuan"),
        AIAbility.LONG_TEXT to listOf("deepseek", "kimi", "baidu", "alibaba"),
        AIAbility.EDUCATION to listOf("tencent", "baidu", "alibaba", "openai"),
        AIAbility.IMAGE_ANALYSIS to listOf("openai", "alibaba", "baidu"),
        AIAbility.IMAGE_GENERATION to listOf("openai", "alibaba"),
        AIAbility.AUDIO_PROCESSING to listOf("openai", "alibaba"),
        AIAbility.VIDEO_ANALYSIS to listOf("openai", "alibaba"),
        
        // RAG能力 - 腾讯云混元在RAG方面有优势
        AIAbility.RAG_DOCUMENT_PARSING to listOf("tencent", "openai", "baidu", "alibaba"),
        AIAbility.RAG_TEXT_SPLITTING to listOf("tencent", "openai", "baidu", "alibaba"),
        AIAbility.RAG_EMBEDDING to listOf("tencent", "openai", "baidu", "alibaba"),
        AIAbility.RAG_MULTI_TURN_REWRITING to listOf("tencent", "openai", "baidu"),
        AIAbility.RAG_RE_RANKING to listOf("tencent", "openai", "baidu"),
        AIAbility.RAG_RETRIEVAL to listOf("tencent", "openai", "baidu", "alibaba"),
        AIAbility.RAG_GENERATION to listOf("tencent", "openai", "baidu", "alibaba")
    )
    
    /**
     * 模型能力描述 - 使用清晰的布尔属性
     */
    data class ModelCapability(
        val name: String,
        val description: String,
        val quality: Double, // 0-1 质量评分
        val costEfficiency: Double, // 0-1 成本效益
        val config: com.autodroid.teachitback.config.AIServiceConfig // 清晰的配置对象
    )
    
    /**
     * 根据AI能力枚举检查服务是否支持该功能
     * 使用清晰的布尔属性替代隐藏的字符串代码
     */
    private fun AIService.supportsAbility(ability: AIAbility): Boolean {
        return when (ability) {
            // AIAbility.BASIC_CHAT -> config.supportBasicChat
            // AIAbility.FILE_PROCESSING -> config.supportFileProcessing
            // AIAbility.MIND_MAP_GENERATION -> config.supportMindMapGeneration
            // AIAbility.LEARNING_ANALYSIS -> config.supportLearningAnalysis
            // AIAbility.SOCRATIC_QUESTIONING -> config.supportSocraticQuestioning
            // AIAbility.ANSWER_EVALUATION -> config.supportAnswerEvaluation
            // AIAbility.DOCUMENT_PARSING -> config.supportDocumentParsing
            // AIAbility.CONCEPT_EXTRACTION -> config.supportConceptExtraction
            // AIAbility.KNOWLEDGE_GRAPH -> config.supportKnowledgeGraph
            // AIAbility.LONG_TEXT -> config.supportLongText
            // AIAbility.MULTIMODAL -> config.supportMultimodal
            // AIAbility.EDUCATION -> config.supportEducation
            // AIAbility.CODE_GENERATION -> config.supportCodeGeneration
            // AIAbility.MATH -> config.supportMath
            // AIAbility.CREATIVE_WRITING -> config.supportCreativeWriting
            // AIAbility.IMAGE_ANALYSIS -> config.supportImageAnalysis
            // AIAbility.IMAGE_GENERATION -> config.supportImageGeneration
            // AIAbility.AUDIO_PROCESSING -> config.supportAudioProcessing
            // AIAbility.VIDEO_ANALYSIS -> config.supportVideoAnalysis
            else -> false
            
            // RAG能力 - 基于腾讯云RAG链路解耦能力
            // AIAbility.RAG_DOCUMENT_PARSING -> config.supportRAGDocumentParsing
            // AIAbility.RAG_TEXT_SPLITTING -> config.supportRAGTextSplitting
            // AIAbility.RAG_EMBEDDING -> config.supportRAGEmbedding
            // AIAbility.RAG_MULTI_TURN_REWRITING -> config.supportRAGMultiTurnRewriting
            // AIAbility.RAG_RE_RANKING -> config.supportRAGReRanking
            // AIAbility.RAG_RETRIEVAL -> config.supportRAGRetrieval
            // AIAbility.RAG_GENERATION -> config.supportRAGGeneration
        }
    }
    
    /**
     * 获取支持特定能力的所有可用服务
     * 使用清晰的枚举和布尔属性
     */
    fun getAvailableServices(ability: AIAbility): List<AIService> {
        return aiServices.values.filter { service ->
            service.isAvailable && service.supportsAbility(ability)
        }
    }
    
    /**
     * 获取所有支持的能力列表
     */
    fun getSupportedAbilities(serviceName: String): List<AIAbility> {
        val service = aiServices[serviceName] ?: return emptyList()
        return AIAbility.values().filter { ability ->
            service.supportsAbility(ability)
        }
    }
    
    /**
     * 初始化所有AI服务（支持12大模型）
     */
    fun initializeAIServices(
        context: android.content.Context,
        // 主要模型配置
        deepseekApiKey: String? = null,
        tencentApiKey: String? = null,
        tencentSecretId: String? = null,
        baiduApiKey: String? = null,
        baiduSecretKey: String? = null,
        alibabaApiKey: String? = null,
        openaiApiKey: String? = null,
        // 其他模型配置
        doubaoApiKey: String? = null,
        zhipuApiKey: String? = null,
        minimaxApiKey: String? = null,
        hunyuanApiKey: String? = null,
        kimiApiKey: String? = null,
        lingyiApiKey: String? = null,
        jieyueApiKey: String? = null
    ) {
        // 初始化基础对话模型（性价比优先）
        // deepseekApiKey?.let { aiServices["deepseek"] = DeepSeekAIServiceAdapter(it) }
        // doubaoApiKey?.let { aiServices["doubao"] = DoubaoAIServiceAdapter(it) }
        
        // 初始化教育专用模型
        // if (tencentApiKey != null && tencentSecretId != null) {
        //     aiServices["tencent"] = TencentCloudAIServiceImpl(
        //         context = context,
        //         apiKey = tencentApiKey,
        //         secretId = tencentSecretId
        //     )
        // }
        
        // 初始化其他主要模型
        // baiduApiKey?.let { 
        //     baiduSecretKey?.let { secret -> 
        //         aiServices["baidu"] = BaiduErnieAIServiceAdapter(it, secret)
        //     }
        // }
        // alibabaApiKey?.let { aiServices["alibaba"] = AlibabaQWenAIServiceAdapter(it) }
        // openaiApiKey?.let { aiServices["openai"] = OpenAIServiceAdapter(it) }
        
        // 可选：初始化其他模型
        // zhipuApiKey?.let { aiServices["zhipu"] = ZhipuAIServiceAdapter(it) }
        // minimaxApiKey?.let { aiServices["minimax"] = MinimaxAIServiceAdapter(it) }
        // hunyuanApiKey?.let { aiServices["hunyuan"] = HunyuanAIServiceAdapter(it) }
        // kimiApiKey?.let { aiServices["kimi"] = KimiAIServiceAdapter(it) }
        // lingyiApiKey?.let { aiServices["lingyi"] = LingyiAIServiceAdapter(it) }
        // jieyueApiKey?.let { aiServices["jieyue"] = JieyueAIServiceAdapter(it) }
    }
    
    /**
     * 智能路由到最合适的AI服务（基于能力枚举）
     * 使用清晰的枚举替代隐藏的字符串代码
     */
    suspend fun <T> routeByAbility(
        ability: AIAbility,
        operation: suspend (com.autodroid.teachitback.api.AIService) -> T
    ): T {
        val servicePriority = abilityRouting[ability] ?: listOf("deepseek")
        
        // 按优先级尝试可用服务
        for (serviceName in servicePriority) {
            val service = aiServices[serviceName]
            if (service != null && service.isAvailable && service.supportsAbility(ability)) {
                return try {
                    operation(service)
                } catch (e: Exception) {
                    // 当前服务失败，尝试下一个
                    continue
                }
            }
        }
        
        throw IllegalStateException("没有可用的AI服务来处理能力: $ability")
    }
    
    /**
     * 智能路由到最合适的AI服务（支持多模型优先级）
     * 保持向后兼容的字符串版本
     */
    suspend fun <T> routeFunction(
        functionName: String,
        operation: suspend (com.autodroid.teachitback.api.AIService) -> T
    ): T {
        val ability = when (functionName) {
            "sendMessage", "basicChat" -> AIAbility.BASIC_CHAT
            "processFileContent", "fileProcessing" -> AIAbility.FILE_PROCESSING
            "generateMindMap", "mindMap" -> AIAbility.MIND_MAP_GENERATION
            "analyzeLearningProgress", "learningAnalysis" -> AIAbility.LEARNING_ANALYSIS
            "generateSocraticQuestions", "socraticQuestioning" -> AIAbility.SOCRATIC_QUESTIONING
            "evaluateAnswer", "answerEvaluation" -> AIAbility.ANSWER_EVALUATION
            "parseDocument" -> AIAbility.DOCUMENT_PARSING
            "extractKeyConcepts" -> AIAbility.CONCEPT_EXTRACTION
            "buildKnowledgeGraph" -> AIAbility.KNOWLEDGE_GRAPH
            "codeGeneration" -> AIAbility.CODE_GENERATION
            "math" -> AIAbility.MATH
            "creativeWriting" -> AIAbility.CREATIVE_WRITING
            "multimodal" -> AIAbility.MULTIMODAL
            "longText" -> AIAbility.LONG_TEXT
            "education" -> AIAbility.EDUCATION
            "imageAnalysis" -> AIAbility.IMAGE_ANALYSIS
            "imageGeneration" -> AIAbility.IMAGE_GENERATION
            "audioProcessing" -> AIAbility.AUDIO_PROCESSING
            "videoAnalysis" -> AIAbility.VIDEO_ANALYSIS
            
            // RAG能力字符串映射
            "ragDocumentParsing" -> AIAbility.RAG_DOCUMENT_PARSING
            "ragTextSplitting" -> AIAbility.RAG_TEXT_SPLITTING
            "ragEmbedding" -> AIAbility.RAG_EMBEDDING
            "ragMultiTurnRewriting" -> AIAbility.RAG_MULTI_TURN_REWRITING
            "ragReRanking" -> AIAbility.RAG_RE_RANKING
            "ragRetrieval" -> AIAbility.RAG_RETRIEVAL
            "ragGeneration" -> AIAbility.RAG_GENERATION
            else -> throw IllegalArgumentException("不支持的功能: $functionName")
        }
        
        return routeByAbility(ability, operation)
    }
    
    /**
     * 获取所有可用AI服务状态
     */
    fun getAIServiceStatus(): Map<String, Boolean> {
        return aiServices.mapValues { (_, service) -> service.isAvailable }
    }
    
    /**
     * 手动指定使用特定AI服务
     */
    suspend fun <T> useSpecificService(
        serviceName: String,
        operation: suspend (AIService) -> T
    ): T {
        val service = aiServices[serviceName] ?: throw IllegalArgumentException("AI服务不存在: $serviceName")
        return operation(service)
    }
}