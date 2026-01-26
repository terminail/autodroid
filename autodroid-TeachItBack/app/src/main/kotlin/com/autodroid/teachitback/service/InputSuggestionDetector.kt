package com.autodroid.teachitback.service

import com.autodroid.teachitback.config.AIServiceConfig

/**
 * 输入建议检测器
 * 在用户发送消息前进行分析，提供优化建议
 */
class InputSuggestionDetector {
    
    companion object {
        // 关键词模式定义
        private val SIMPLE_QA_PATTERNS = listOf(
            "是什么", "为什么", "怎么", "如何", "哪个", "什么", "哪里", "何时",
            "谁", "多少", "几岁", "多大", "多长", "多重", "多高", "多远"
        )
        
        private val CONCEPT_EXPLANATION_PATTERNS = listOf(
            "定义", "解释", "概念", "含义", "意思", "理解", "阐述", "说明",
            "什么是", "解释一下", "详细说明", "详细介绍"
        )
        
        private val COMPLEX_REASONING_PATTERNS = listOf(
            "证明", "分析", "论述", "讨论", "比较", "对比", "论证", "推导",
            "详细分析", "深入讨论", "综合比较", "逻辑推理", "辩证分析"
        )
        
        private val ANSWER_EVALUATION_PATTERNS = listOf(
            "评估", "评分", "打分", "评价", "判断", "检查", "批改", "反馈",
            "这个答案", "我的答案", "对不对", "正确吗", "得分", "满分"
        )
        
        private val MATH_PATTERNS = listOf(
            "计算", "求解", "解方程", "求导", "积分", "证明题", "几何题",
            "代数", "函数", "三角函数", "微积分", "概率", "统计"
        )
        
        private val CREATIVE_WRITING_PATTERNS = listOf(
            "写作", "创作", "写一篇", "写个", "作文", "文章", "故事", "诗歌",
            "散文", "小说", "剧本", "文案", "广告语", "标题"
        )
        
        private val CODE_GENERATION_PATTERNS = listOf(
            "代码", "编程", "写个函数", "实现", "算法", "程序", "脚本",
            "Python", "Java", "Kotlin", "JavaScript", "C++", "SQL"
        )
        
        private val FILE_PROCESSING_PATTERNS = listOf(
            "PDF", "文件", "文档", "图片", "图像", "分析文件", "处理文档",
            "读取PDF", "解析文档", "提取信息"
        )
        
        private val MIND_MAP_PATTERNS = listOf(
            "思维导图", "知识结构", "框架", "体系", "关系图", "流程图",
            "整理知识", "梳理结构", "知识图谱"
        )
        
        private val LONG_TEXT_PATTERNS = listOf(
            "长文本", "长文章", "详细", "深入", "全面", "系统", "完整",
            "万字长文", "详细解读", "深度分析"
        )
    }
    
    /**
     * 发送前检测入口
     * 分析用户输入并提供优化建议
     * 
     * 返回规则（重要修正）：
     * - 有明确关键词 → 直接发送，不弹出（用户已表达清晰意图）
     * - 无关键词/模糊输入 → 弹出对话框提示优化
     */
    fun analyzeBeforeSend(message: String): SuggestionResult {
        if (message.isBlank()) {
            return SuggestionResult("请输入内容", null, null, false)
        }
        
        val analysis = performQuickAnalysis(message)
        
        // 🎯 关键逻辑：只有GENERAL（无匹配关键词）才弹出提示
        // 其他有明确关键词的（SIMPLE_QA, CONCEPT_EXPLANATION等）都直接发送
        return when (analysis) {
            AnalysisResult.GENERAL -> {
                // 模糊输入 → 弹出优化建议
                SuggestionResult(
                    message = "💭 检测到输入可能不够清晰",
                    suggestion = "为了获得更好的回答，建议：\n\n" +
                               "• 添加具体关键词（如：\"解释一下\"、\"计算\"、\"马上\"）\n" +
                               "• 明确问题类型（概念、计算、分析等）\n" +
                               "• 提供更多上下文信息\n\n" +
                               "📌 查看帮助了解详细的关键词指南",
                    suggestedService = null,
                    helpLink = "InputHelp",
                    shouldShowDialog = true
                )
            }
            else -> {
                // 有明确关键词 → 直接发送
                SuggestionResult(
                    message = "检测到明确需求，直接发送",
                    suggestion = null,
                    suggestedService = getServiceForAnalysis(analysis),
                    helpLink = null,
                    shouldShowDialog = false
                )
            }
        }
    }
    
    /**
     * 轻量级分析（关键词匹配）
     * 性能开销小，检测过程<50ms
     */
    private fun performQuickAnalysis(message: String): AnalysisResult {
        val lowerMessage = message.lowercase()
        
        return when {
            // 优先检查复杂推理
            containsAnyPattern(lowerMessage, COMPLEX_REASONING_PATTERNS) -> 
                AnalysisResult.COMPLEX_REASONING
            
            // 检查答案评估
            containsAnyPattern(lowerMessage, ANSWER_EVALUATION_PATTERNS) -> 
                AnalysisResult.ANSWER_EVALUATION
            
            // 检查代码生成
            containsAnyPattern(lowerMessage, CODE_GENERATION_PATTERNS) -> 
                AnalysisResult.CODE_GENERATION
            
            // 检查文件处理
            containsAnyPattern(lowerMessage, FILE_PROCESSING_PATTERNS) -> 
                AnalysisResult.FILE_PROCESSING
            
            // 检查思维导图
            containsAnyPattern(lowerMessage, MIND_MAP_PATTERNS) -> 
                AnalysisResult.MIND_MAP_GENERATION
            
            // 检查长文本
            containsAnyPattern(lowerMessage, LONG_TEXT_PATTERNS) || 
            message.length > 500 -> 
                AnalysisResult.LONG_TEXT
            
            // 检查创意写作
            containsAnyPattern(lowerMessage, CREATIVE_WRITING_PATTERNS) -> 
                AnalysisResult.CREATIVE_WRITING
            
            // 检查数学问题
            containsAnyPattern(lowerMessage, MATH_PATTERNS) -> 
                AnalysisResult.MATH
            
            // 检查简单问答
            containsAnyPattern(lowerMessage, SIMPLE_QA_PATTERNS) -> 
                AnalysisResult.SIMPLE_QA
            
            // 检查概念解释
            containsAnyPattern(lowerMessage, CONCEPT_EXPLANATION_PATTERNS) -> 
                AnalysisResult.CONCEPT_EXPLANATION
            
            // 默认情况
            else -> AnalysisResult.GENERAL
        }
    }
    
    /**
     * 生成优化建议
     * 只在需要用户补充信息时弹出对话框
     */
    private fun generateSuggestion(analysis: AnalysisResult, originalMessage: String): SuggestionResult {
        return when (analysis) {
            // ✅ 明确的简单请求 → 直接发送，不弹出
            AnalysisResult.SIMPLE_QA -> SuggestionResult(
                message = "检测到简单问答，将使用快速响应模式",
                suggestion = null,  // 不需要补充信息
                suggestedService = getServiceForAnalysis(analysis),
                shouldShowDialog = false  // 直接发送
            )
            
            // ⚠️ 需要补充背景信息 → 弹出对话框
            AnalysisResult.CONCEPT_EXPLANATION -> SuggestionResult(
                message = "💡 检测到概念解释需求",
                suggestion = "为了获得更准确的回答，建议补充：\n\n" +
                           "• 具体应用场景（如：高中数学、编程开发）\n" +
                           "• 已了解的程度（如：完全不懂、了解基础）\n" +
                           "• 希望深入的方向（如：原理、应用、例子）\n\n" +
                           "📌 关键词提示：添加\"详细解释\"、\"举例说明\"、\"对比分析\"等",
                suggestedService = getServiceForAnalysis(analysis),
                helpLink = "InputHelp",  // 链接到帮助系统
                shouldShowDialog = true
            )
            
            // ⚠️ 复杂请求需要补充 → 弹出对话框
            AnalysisResult.COMPLEX_REASONING -> SuggestionResult(
                message = "💡 检测到复杂推理需求",
                suggestion = "为了获得最佳效果，建议：\n\n" +
                           "• 分步骤说明问题\n" +
                           "• 提供相关背景信息\n" +
                           "• 明确期望的输出格式\n\n" +
                           "📌 关键词提示：添加\"详细分析\"、\"逐步推导\"、\"综合论述\"等",
                suggestedService = getServiceForAnalysis(analysis),
                helpLink = "InputHelp",
                shouldShowDialog = true
            )
            
            // ⚠️ 需要评估标准 → 弹出对话框
            AnalysisResult.ANSWER_EVALUATION -> SuggestionResult(
                message = "📝 检测到答案评估需求",
                suggestion = "为了准确评估，建议提供：\n\n" +
                           "• 标准答案（如有）\n" +
                           "• 评分标准\n" +
                           "• 评估重点\n\n" +
                           "📌 关键词提示：添加\"满分\"、\"评分标准\"、\"重点检查\"等",
                suggestedService = getServiceForAnalysis(analysis),
                helpLink = "InputHelp",
                shouldShowDialog = true
            )
            
            // ✅ 明确的数学问题 → 直接发送
            AnalysisResult.MATH -> SuggestionResult(
                message = "🔢 检测到数学问题",
                suggestion = null,
                suggestedService = getServiceForAnalysis(analysis),
                shouldShowDialog = false  // 数学问题直接发送
            )
            
            // ⚠️ 需要创作要求 → 弹出对话框
            AnalysisResult.CREATIVE_WRITING -> SuggestionResult(
                message = "✍️ 检测到创意写作需求",
                suggestion = "为了更好满足创作需求，建议说明：\n\n" +
                           "• 文体要求（如：议论文、散文）\n" +
                           "• 字数限制\n" +
                           "• 主题方向\n" +
                           "• 风格偏好\n\n" +
                           "📌 关键词提示：添加\"800字\"、\"议论文\"、\"正式风格\"等",
                suggestedService = getServiceForAnalysis(analysis),
                helpLink = "InputHelp",
                shouldShowDialog = true
            )
            
            // ✅ 明确的代码需求 → 直接发送
            AnalysisResult.CODE_GENERATION -> SuggestionResult(
                message = "💻 检测到代码生成需求",
                suggestion = null,
                suggestedService = getServiceForAnalysis(analysis),
                shouldShowDialog = false
            )
            
            // ⚠️ 需要提醒上传文件 → 弹出对话框
            AnalysisResult.FILE_PROCESSING -> SuggestionResult(
                message = "📄 检测到文件处理需求",
                suggestion = "请确保已上传文件！\n\n" +
                           "支持的格式：PDF、图片、文本文件\n\n" +
                           "📌 提示：点击\"+\"按钮上传文件",
                suggestedService = getServiceForAnalysis(analysis),
                helpLink = "FileHelp",
                shouldShowDialog = true
            )
            
            // ⚠️ 需要明确主题 → 弹出对话框
            AnalysisResult.MIND_MAP_GENERATION -> SuggestionResult(
                message = "🧠 检测到思维导图需求",
                suggestion = "为了生成更精准的思维导图，建议明确：\n\n" +
                           "• 中心主题\n" +
                           "• 主要分支\n" +
                           "• 详细程度\n\n" +
                           "📌 关键词提示：添加\"详细\"、\"框架\"、\"知识结构\"等",
                suggestedService = getServiceForAnalysis(analysis),
                helpLink = "MindMapHelp",
                shouldShowDialog = true
            )
            
            // ✅ 明确的长文本 → 直接发送
            AnalysisResult.LONG_TEXT -> SuggestionResult(
                message = "📖 检测到长文本处理需求",
                suggestion = "提示：长文本处理可能需要更长时间，请耐心等待",
                suggestedService = getServiceForAnalysis(analysis),
                shouldShowDialog = false  // 直接发送
            )
            
            // ⚠️ 表述不清晰 → 弹出对话框
            AnalysisResult.GENERAL -> SuggestionResult(
                message = "💭 通用问题",
                suggestion = "建议优化问题表述：\n\n" +
                           "• 补充更多细节\n" +
                           "• 明确具体需求\n" +
                           "• 添加上下文信息\n\n" +
                           "📌 查看帮助了解如何优化输入",
                suggestedService = getServiceForAnalysis(analysis),
                helpLink = "InputHelp",
                shouldShowDialog = true
            )
        }
    }
    
    /**
     * 根据分析结果推荐服务
     */
    private fun getServiceForAnalysis(analysis: AnalysisResult): String? {
        return when (analysis) {
            AnalysisResult.SIMPLE_QA -> "tinybert_local"  // 快速响应
            AnalysisResult.CONCEPT_EXPLANATION -> "chatglm_local"  // 本地概念解释
            AnalysisResult.COMPLEX_REASONING -> "tencent-hunyuan"  // 云端复杂推理
            AnalysisResult.ANSWER_EVALUATION -> "tinybert_local"  // 答案判断
            AnalysisResult.MATH -> "baidu"  // 百度数学能力强
            AnalysisResult.CREATIVE_WRITING -> "alibaba"  // 阿里文案能力强
            AnalysisResult.CODE_GENERATION -> "deepseek"  // DeepSeek代码能力强
            AnalysisResult.FILE_PROCESSING -> "kimi"  // Kimi文件处理强
            AnalysisResult.MIND_MAP_GENERATION -> "tencent"  // 腾讯思维导图
            AnalysisResult.LONG_TEXT -> "deepseek"  // DeepSeek长文本
            AnalysisResult.GENERAL -> "doubao"  // 豆包通用能力强
        }
    }
    
    /**
     * 提取核心问题（用于简化建议）
     */
    private fun extractCoreQuestion(message: String): String {
        return when {
            message.length <= 50 -> message
            message.contains("？") -> message.substring(0, message.indexOf("？") + 1)
            message.contains("?") -> message.substring(0, message.indexOf("?") + 1)
            else -> message.substring(0, minOf(50, message.length)) + "..."
        }
    }
    
    /**
     * 检查消息是否包含任何模式
     */
    private fun containsAnyPattern(message: String, patterns: List<String>): Boolean {
        return patterns.any { pattern -> message.contains(pattern) }
    }
    
    /**
     * 分析结果枚举
     */
    sealed class AnalysisResult {
        object SIMPLE_QA : AnalysisResult()  // 简单问答
        object CONCEPT_EXPLANATION : AnalysisResult()  // 概念解释
        object COMPLEX_REASONING : AnalysisResult()  // 复杂推理
        object ANSWER_EVALUATION : AnalysisResult()  // 答案评估
        object MATH : AnalysisResult()  // 数学问题
        object CREATIVE_WRITING : AnalysisResult()  // 创意写作
        object CODE_GENERATION : AnalysisResult()  // 代码生成
        object FILE_PROCESSING : AnalysisResult()  // 文件处理
        object MIND_MAP_GENERATION : AnalysisResult()  // 思维导图
        object LONG_TEXT : AnalysisResult()  // 长文本
        object GENERAL : AnalysisResult()  // 通用问题
    }
    
    /**
     * 建议结果数据类
     */
    data class SuggestionResult(
        val message: String,  // 主消息
        val suggestion: String? = null,  // 具体建议
        val suggestedService: String? = null,  // 推荐服务
        val shouldShowDialog: Boolean = true,  // 是否显示对话框
        val helpLink: String? = null  // 帮助链接（如：InputHelp, FileHelp等）
    )
}
