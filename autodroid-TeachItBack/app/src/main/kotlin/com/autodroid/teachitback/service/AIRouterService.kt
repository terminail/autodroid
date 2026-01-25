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
    
    /**
     * 获取能力需求分析提示指南
     */
    fun getCapabilityAnalysisTips(): Map<String, List<String>> {
        return mapOf(
            "快速解答类" to listOf("快速", "即时", "马上", "立刻"),
            "概念理解类" to listOf("概念", "解释", "理解", "含义", "定义"),
            "学习分析类" to listOf("学习", "进步", "评估", "分析", "总结"),
            "思维导图类" to listOf("思维", "导图", "整理", "结构", "框架"),
            "深度提问类" to listOf("提问", "思考", "深度", "启发", "探讨"),
            "答案评估类" to listOf("评估", "反馈", "检查", "评分", "改进"),
            "文件处理类" to listOf("文件", "文档", "pdf", "word", "excel"),
            "代码编程类" to listOf("代码", "编程", "程序", "算法", "开发"),
            "数学计算类" to listOf("数学", "计算", "公式", "方程", "统计"),
            "创意写作类" to listOf("写作", "创意", "文章", "故事", "文案"),
            "多模态处理" to listOf("图片", "图像", "音频", "视频", "视觉"),
            "长文本处理" to listOf("长文本", "长文章", "大段", "长篇", "详细"),
            "知识检索类" to listOf("检索", "搜索", "知识库", "资料", "文献")
        )
    }
    
    /**
     * 基于用户自由文本输入进行能力需求分析
     * 将教育概念、快速答案等用户需求映射到具体AI能力
     */
    fun analyzeCapabilityRequirements(userInput: String): List<AIAbility> {
        val requirements = mutableListOf<AIAbility>()
        val normalizedInput = userInput.lowercase()
        
        // 教育概念相关需求分析
        when {
            // 快速答案/即时解答
            normalizedInput.contains("快速") || normalizedInput.contains("即时") || 
            normalizedInput.contains("马上") || normalizedInput.contains("立刻") -> {
                requirements.add(AIAbility.BASIC_CHAT)
            }
            
            // 概念解释/知识理解
            normalizedInput.contains("概念") || normalizedInput.contains("解释") || 
            normalizedInput.contains("理解") || normalizedInput.contains("含义") -> {
                requirements.add(AIAbility.CONCEPT_EXTRACTION)
                requirements.add(AIAbility.KNOWLEDGE_GRAPH)
            }
            
            // 学习分析/进度评估
            normalizedInput.contains("学习") || normalizedInput.contains("进步") || 
            normalizedInput.contains("评估") || normalizedInput.contains("分析") -> {
                requirements.add(AIAbility.LEARNING_ANALYSIS)
            }
            
            // 思维导图/知识整理
            normalizedInput.contains("思维") || normalizedInput.contains("导图") || 
            normalizedInput.contains("整理") || normalizedInput.contains("结构") -> {
                requirements.add(AIAbility.MIND_MAP_GENERATION)
            }
            
            // 苏格拉底式提问/深度思考
            normalizedInput.contains("提问") || normalizedInput.contains("思考") || 
            normalizedInput.contains("深度") || normalizedInput.contains("启发") -> {
                requirements.add(AIAbility.SOCRATIC_QUESTIONING)
            }
            
            // 答案评估/反馈
            normalizedInput.contains("评估") || normalizedInput.contains("反馈") || 
            normalizedInput.contains("检查") || normalizedInput.contains("评分") -> {
                requirements.add(AIAbility.ANSWER_EVALUATION)
            }
            
            // 文件处理/文档解析
            normalizedInput.contains("文件") || normalizedInput.contains("文档") || 
            normalizedInput.contains("pdf") || normalizedInput.contains("word") -> {
                requirements.add(AIAbility.FILE_PROCESSING)
                requirements.add(AIAbility.DOCUMENT_PARSING)
            }
            
            // 代码相关
            normalizedInput.contains("代码") || normalizedInput.contains("编程") || 
            normalizedInput.contains("程序") -> {
                requirements.add(AIAbility.CODE_GENERATION)
            }
            
            // 数学相关
            normalizedInput.contains("数学") || normalizedInput.contains("计算") || 
            normalizedInput.contains("公式") -> {
                requirements.add(AIAbility.MATH)
            }
            
            // 创意写作
            normalizedInput.contains("写作") || normalizedInput.contains("创意") || 
            normalizedInput.contains("文章") -> {
                requirements.add(AIAbility.CREATIVE_WRITING)
            }
            
            // 多模态处理
            normalizedInput.contains("图片") || normalizedInput.contains("图像") || 
            normalizedInput.contains("音频") || normalizedInput.contains("视频") -> {
                requirements.add(AIAbility.MULTIMODAL)
                if (normalizedInput.contains("图片") || normalizedInput.contains("图像")) {
                    requirements.add(AIAbility.IMAGE_ANALYSIS)
                }
                if (normalizedInput.contains("生成图片") || normalizedInput.contains("生成图像")) {
                    requirements.add(AIAbility.IMAGE_GENERATION)
                }
                if (normalizedInput.contains("音频") || normalizedInput.contains("声音")) {
                    requirements.add(AIAbility.AUDIO_PROCESSING)
                }
                if (normalizedInput.contains("视频")) {
                    requirements.add(AIAbility.VIDEO_ANALYSIS)
                }
            }
            
            // 长文本处理
            normalizedInput.contains("长文本") || normalizedInput.contains("长文章") || 
            normalizedInput.contains("大段") -> {
                requirements.add(AIAbility.LONG_TEXT)
            }
            
            // RAG相关
            normalizedInput.contains("检索") || normalizedInput.contains("搜索") || 
            normalizedInput.contains("知识库") -> {
                requirements.add(AIAbility.RAG_RETRIEVAL)
                requirements.add(AIAbility.RAG_GENERATION)
            }
        }
        
        // 如果没有匹配到特定需求，默认使用基础对话
        if (requirements.isEmpty()) {
            requirements.add(AIAbility.BASIC_CHAT)
        }
        
        return requirements.distinct()
    }
    
    /**
     * 分析用户输入并提供优化建议
     */
    fun getInputOptimizationSuggestions(userInput: String): Map<String, Any> {
        val suggestions = mutableListOf<String>()
        val detectedKeywords = mutableListOf<String>()
        val normalizedInput = userInput.lowercase()
        
        val tips = getCapabilityAnalysisTips()
        
        // 检测已有关键词
        tips.forEach { (category, keywords) ->
            keywords.forEach { keyword ->
                if (normalizedInput.contains(keyword)) {
                    detectedKeywords.add("$keyword ($category)")
                }
            }
        }
        
        // 提供优化建议
        if (detectedKeywords.isEmpty()) {
            suggestions.add("💡 尝试添加具体需求关键词，如：'快速'、'概念'、'学习'、'思维导图'等")
            suggestions.add("📚 明确说明任务类型：解答问题、分析学习、生成导图、评估答案等")
            suggestions.add("🎯 指定内容形式：文件、代码、数学、写作、图片等")
        } else {
            suggestions.add("✅ 已识别关键词: ${detectedKeywords.joinToString(", ")}")
            
            // 根据已识别关键词推荐相关能力
            val capabilities = analyzeCapabilityRequirements(userInput)
            if (capabilities.isNotEmpty()) {
                suggestions.add("🚀 将为您匹配以下AI能力: ${capabilities.joinToString(", ") { it.name }}")
            }
        }
        
        // 检查输入是否过于简单
        if (userInput.length < 10 && detectedKeywords.isEmpty()) {
            suggestions.add("🔍 输入可能过于简单，请详细描述您的需求以获得更精准的服务")
        }
        
        return mapOf(
            "suggestions" to suggestions,
            "detectedKeywords" to detectedKeywords,
            "inputLength" to userInput.length,
            "capabilities" to analyzeCapabilityRequirements(userInput).map { it.name }
        )
    }
    
    /**
     * 获取完整的能力需求分析指南
     */
    fun getCompleteAnalysisGuide(): String {
        val tips = getCapabilityAnalysisTips()
        val guide = StringBuilder()
        
        guide.append("🤖 AI能力需求分析指南\n\n")
        guide.append("为了让AI更好地理解您的需求，请参考以下关键词分类：\n\n")
        
        tips.forEach { (category, keywords) ->
            guide.append("📌 $category:\n")
            guide.append("   关键词: ${keywords.joinToString(", ")}\n")
            guide.append("   示例: '${getExampleForCategory(category)}'\n\n")
        }
        
        guide.append("💡 使用技巧：\n")
        guide.append("• 组合使用关键词：'快速解释数学概念'\n")
        guide.append("• 明确任务目标：'生成思维导图总结学习内容'\n")
        guide.append("• 指定内容类型：'分析PDF文档中的关键概念'\n")
        
        return guide.toString()
    }
    
    /**
     * 为每个类别提供示例
     */
    private fun getExampleForCategory(category: String): String {
        return when (category) {
            "快速解答类" -> "快速解答这个数学问题"
            "概念理解类" -> "解释人工智能的基本概念"
            "学习分析类" -> "分析我的学习进度和薄弱环节"
            "思维导图类" -> "为这篇文章生成思维导图"
            "深度提问类" -> "提出深度思考问题启发学习"
            "答案评估类" -> "评估我的答案并提出改进建议"
            "文件处理类" -> "解析这个PDF文档的主要内容"
            "代码编程类" -> "帮我编写一个排序算法的代码"
            "数学计算类" -> "计算这个复杂的数学公式"
            "创意写作类" -> "帮我写一篇关于环保的创意文章"
            "多模态处理" -> "分析这张图片中的物体和场景"
            "长文本处理" -> "总结这篇长文章的核心观点"
            "知识检索类" -> "从知识库中检索相关文献资料"
            else -> "请描述您的具体需求"
        }
    }
    
    /**
     * 基于用户输入智能路由到最合适的服务
     */
    suspend fun <T> routeByUserInput(
        userInput: String,
        operation: suspend (AIService) -> T
    ): T {
        val capabilities = analyzeCapabilityRequirements(userInput)
        
        // 按能力优先级选择服务
        for (capability in capabilities) {
            try {
                return routeByAbility(capability, operation)
            } catch (e: Exception) {
                // 当前能力路由失败，尝试下一个能力
                continue
            }
        }
        
        // 如果所有能力都失败，使用基础对话作为兜底
        return routeByAbility(AIAbility.BASIC_CHAT, operation)
    }
}