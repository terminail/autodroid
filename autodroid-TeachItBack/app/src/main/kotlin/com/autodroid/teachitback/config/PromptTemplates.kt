package com.autodroid.teachitback.config

/**
 * 统一Prompt模板管理
 * 核心原则：同一个AI功能的所有服务使用完全相同的Prompt模板，确保输出一致性
 *
 * 设计优势：
 * - 所有AI服务使用相同prompt，用户体验一致
 * - 维护成本低，修改一处即可影响所有服务
 * - 支持公平的模型对比测试
 * - 便于集中优化prompt质量
 *
 * 使用方法：
 * 所有AI服务实现（DeepSeek、OpenAI、腾讯云混元、MiniMax等）
 * 都必须使用这些统一的prompt，不得硬编码自定义prompt
 */
object PromptTemplates {

    /**
     * 文件内容处理
     * 所有AI服务使用此相同prompt
     *
     * @param content 文件内容
     * @param context 上下文信息
     * @return 统一格式的prompt字符串
     */
    fun processFileContent(content: String, context: String): String {
        return """
            请分析以下文件内容：
            
            $content
            
            上下文：$context
            
            请提供：
            1. 主要内容摘要
            2. 关键概念识别
            3. 学习建议
            4. 相关测试问题（3-5个）
            
            请以结构化方式返回，使用清晰的标题和列表。
        """.trimIndent()
    }

    /**
     * 思维导图生成
     *
     * @param topicId 主题ID
     * @param learningGoal 学习目标
     * @return 统一格式的prompt字符串
     */
    fun generateMindMap(topicId: String, learningGoal: String): String {
        return """
            请为"$learningGoal"主题生成思维导图结构。
            主题ID: $topicId
            
            请提供层级结构，格式：
            # 主主题
            ## 子主题1
            ### 子子主题1.1
            ## 子主题2
            ### 子子主题2.1
            ### 子子主题2.2
            
            确保结构清晰，层次分明。
        """.trimIndent()
    }

    /**
     * 学习进度分析
     *
     * @param conversationText 对话历史文本
     * @return 统一格式的prompt字符串
     */
    fun analyzeLearningProgress(conversationText: String): String {
        return """
            请分析以下对话历史，评估学习进度：
            
            $conversationText
            
            请提供：
            1. 整体学习进度（0-100分）
            2. 各概念的掌握程度
            3. 识别知识缺口
            4. 推荐下一步学习行动
            
            请以JSON格式返回结果。
        """.trimIndent()
    }

    /**
     * 苏格拉底式问题生成
     *
     * @param topic 主题
     * @param currentLevel 当前水平（0-100）
     * @return 统一格式的prompt字符串
     */
    fun generateSocraticQuestions(topic: String, currentLevel: Int): String {
        return """
            请为"$topic"主题生成5个苏格拉底式问题。
            当前学习水平：$currentLevel/100
            
            问题应该：
            1. 引导思考而非直接给答案
            2. 从基础到深入递进
            3. 鼓励批判性思维
            
            请直接返回问题列表，每行一个问题。
        """.trimIndent()
    }

    /**
     * 答案评估
     *
     * @param userAnswer 用户答案
     * @param correctAnswer 正确答案
     * @return 统一格式的prompt字符串
     */
    fun evaluateAnswer(userAnswer: String, correctAnswer: String): String {
        return """
            请评估以下答案：
            
            用户答案：$userAnswer
            正确答案：$correctAnswer
            
            请提供：
            1. 是否正确
            2. 置信度（0.0-1.0）
            3. 反馈信息
            4. 改进建议
            
            请以JSON格式返回结果。
        """.trimIndent()
    }

    /**
     * 文档解析
     *
     * @param fileContent 文件内容
     * @param fileType 文件类型
     * @return 统一格式的prompt字符串
     */
    fun parseDocument(fileContent: String, fileType: String): String {
        return """
            请分析以下${fileType}文档内容：
            
            $fileContent
            
            请提供：
            1. 文档摘要
            2. 关键要点列表
            3. 提取的核心概念
            
            以结构化方式返回。
        """.trimIndent()
    }

    /**
     * 关键概念提取
     *
     * @param content 内容文本
     * @return 统一格式的prompt字符串
     */
    fun extractKeyConcepts(content: String): String {
        return """
            请从以下内容中提取关键概念：
            
            $content
            
            请以JSON格式返回概念列表，每个概念包含：
            - id: 概念ID
            - name: 概念名称
            - definition: 概念定义
            - relatedConcepts: 相关概念ID列表
            
            确保概念具有教育意义且互相关联。
        """.trimIndent()
    }

    /**
     * 构建对话历史文本
     * 用于analyzeLearningProgress等需要对话历史的场景
     *
     * @param conversationHistory 对话历史列表
     * @return 格式化的对话文本
     */
    fun buildConversationText(conversationHistory: List<com.autodroid.teachitback.model.MessageEntity>): String {
        return conversationHistory.joinToString("\n") {
            "${it.senderType}: ${it.content}"
        }
    }

    /**
     * 基础对话系统提示词
     * 所有AI服务必须使用此统一提示词
     *
     * @param context 上下文信息
     * @return 统一格式的系统提示词
     */
    fun basicChatSystemPrompt(context: String): String {
        return """
            你是一个帮助学生学习的AI助手。使用"教给别人"（Teach It Back）的方法。
            
            上下文信息：
            $context
            
            你的任务：
            1. 引导学生用自己的话解释概念
            2. 提供反馈和澄清
            3. 识别理解中的漏洞
            4. 鼓励深度思考
            
            请用简洁、友好的方式回应。
        """.trimIndent()
    }

    /**
     * 文件处理系统提示词
     * 用于processFileContent方法
     *
     * @param context 上下文信息
     * @return 统一格式的系统提示词
     */
    fun fileProcessingSystemPrompt(context: String): String {
        return """
            你是一个帮助学生分析学习材料的AI助手。
            
            上下文信息：
            $context
            
            以下是学生提供的文档内容，请：
            1. 总结主要内容
            2. 识别关键概念
            3. 提出学习建议
            4. 生成相关的测试问题
        """.trimIndent()
    }

    // ===== 新增教育专用模板 =====

    /**
     * 知识点解释模板
     * 用于深入解释复杂概念
     *
     * @param concept 概念名称
     * @param difficultyLevel 难度级别（1-5）
     * @param studentLevel 学生水平（beginner/intermediate/advanced）
     * @return 统一格式的prompt字符串
     */
    fun explainConcept(
        concept: String,
        difficultyLevel: Int = 3,
        studentLevel: String = "beginner"
    ): String {
        return """
            请以适合${studentLevel}学生的水平解释"${concept}"这个概念。
            概念难度：${difficultyLevel}/5
            
            请提供：
            1. 简单明了的定义
            2. 实际应用的例子
            3. 与其他概念的关系
            4. 常见误解的澄清
            5. 记忆技巧（如果有）
            
            使用生动、易懂的语言，避免过于专业的术语。
        """.trimIndent()
    }

    /**
     * 练习题生成模板
     * 用于生成针对特定学习目标的练习题
     *
     * @param topic 主题
     * @param questionType 题目类型（multiple_choice/short_answer/essay/problem_solving）
     * @param difficulty 难度（easy/medium/hard）
     * @param count 题目数量
     * @return 统一格式的prompt字符串
     */
    fun generatePracticeQuestions(
        topic: String,
        questionType: String = "multiple_choice",
        difficulty: String = "medium",
        count: Int = 5
    ): String {
        return """
            请为"${topic}"主题生成${count}道${difficulty}难度的${questionType}类型练习题。
            
            要求：
            1. 题目内容与学习目标紧密相关
            2. 包含明确的答案和解析
            3. 如果是选择题，包含4个选项
            4. 难度分布合理，循序渐进
            
            请以JSON格式返回结果。
        """.trimIndent()
    }

    /**
     * 学习计划制定模板
     * 帮助学生制定个性化的学习计划
     *
     * @param topic 学习主题
     * @param targetLevel 目标水平
     * @param availableTime 可用时间（小时/周）
     * @param learningStyle 学习风格（visual/auditory/kinesthetic）
     * @return 统一格式的prompt字符串
     */
    fun createLearningPlan(
        topic: String,
        targetLevel: String,
        availableTime: Int = 10,
        learningStyle: String = "visual"
    ): String {
        return """
            请为学习${topic}的学生制定一个个性化的学习计划。
            
            学生信息：
            - 目标水平：${targetLevel}
            - 每周可用时间：${availableTime}小时
            - 学习风格：${learningStyle}
            
            计划应包含：
            1. 每周学习目标
            2. 具体的学习活动安排
            3. 学习资源推荐
            4. 进度检查点
            5. 调整策略
            
            计划应具有可操作性，适合学生的学习风格。
        """.trimIndent()
    }

    /**
     * 错题分析模板
     * 分析学生做错的题目，提供改进建议
     *
     * @param wrongQuestion 错题内容
     * @param studentAnswer 学生答案
     * @param correctAnswer 正确答案
     * @param topic 学习主题
     * @return 统一格式的prompt字符串
     */
    fun analyzeWrongAnswer(
        wrongQuestion: String,
        studentAnswer: String,
        correctAnswer: String,
        topic: String
    ): String {
        return """
            请分析学生在${topic}中的错题：
            
            题目：${wrongQuestion}
            学生答案：${studentAnswer}
            正确答案：${correctAnswer}
            
            请提供：
            1. 错误原因分析
            2. 知识点薄弱环节识别
            3. 针对性的改进建议
            4. 类似题目推荐练习
            5. 鼓励性反馈
            
            分析要具体、有建设性。
        """.trimIndent()
    }

    /**
     * 学习策略建议模板
     * 根据学生的学习情况提供个性化学习策略
     *
     * @param learningHistory 学习历史摘要
     * @param strengths 学生优势
     * @param weaknesses 需要改进的方面
     * @param goals 学习目标
     * @return 统一格式的prompt字符串
     */
    fun provideLearningStrategies(
        learningHistory: String,
        strengths: String,
        weaknesses: String,
        goals: String
    ): String {
        return """
            请根据学生的学习情况提供个性化的学习策略建议。
            
            学习历史：$learningHistory
            学生优势：$strengths
            需要改进：$weaknesses
            学习目标：$goals
            
            建议应包含：
            1. 针对优势的强化策略
            2. 针对弱点的改进方法
            3. 目标导向的学习路径
            4. 时间管理建议
            5. 学习资源推荐
            
            建议要具体、可行、个性化。
        """.trimIndent()
    }

    /**
     * 知识回顾模板
     * 帮助学生复习已学知识
     *
     * @param topics 需要复习的主题列表
     * @param lastReviewTime 上次复习时间
     * @param retentionLevel 记忆保持水平（1-10）
     * @return 统一格式的prompt字符串
     */
    fun createReviewPlan(
        topics: List<String>,
        lastReviewTime: String = "一周前",
        retentionLevel: Int = 5
    ): String {
        val topicsText = topics.joinToString(", ")
        return """
            请为学生制定复习计划，帮助复习以下主题：
            $topicsText
            
            复习信息：
            - 上次复习：$lastReviewTime
            - 记忆保持水平：$retentionLevel/10
            
            计划应包含：
            1. 重点复习内容
            2. 复习方法建议（如闪卡、自测、总结等）
            3. 复习时间安排
            4. 效果检查方式
            5. 遗忘曲线优化建议
            
            基于记忆科学原理制定复习计划。
        """.trimIndent()
    }

    /**
     * 项目式学习指导模板
     * 指导学生在真实项目中应用知识
     *
     * @param projectTopic 项目主题
     * @param learningObjectives 学习目标
     * @param availableResources 可用资源
     * @param timeFrame 时间框架
     * @return 统一格式的prompt字符串
     */
    fun guideProjectBasedLearning(
        projectTopic: String,
        learningObjectives: String,
        availableResources: String = "基本学习材料",
        timeFrame: String = "2周"
    ): String {
        return """
            请为学生指导"$projectTopic"项目式学习。
            
            学习目标：$learningObjectives
            可用资源：$availableResources
            时间框架：$timeFrame
            
            指导应包含：
            1. 项目阶段划分
            2. 每个阶段的具体任务
            3. 学习资源使用建议
            4. 进度检查点
            5. 成果评估标准
            6. 问题解决策略
            
            强调在真实情境中应用知识。
        """.trimIndent()
    }

    /**
     * 批判性思维培养模板
     * 培养学生的批判性思维能力
     *
     * @param topic 讨论主题
     * @param perspective 视角或论点
     * @param difficulty 思维难度（basic/intermediate/advanced）
     * @return 统一格式的prompt字符串
     */
    fun developCriticalThinking(
        topic: String,
        perspective: String,
        difficulty: String = "intermediate"
    ): String {
        return """
            请围绕"$topic"主题，从"$perspective"视角培养学生的批判性思维能力。
            
            难度级别：$difficulty
            
            请设计：
            1. 引导性问题（鼓励质疑和探究）
            2. 不同角度的观点对比
            3. 证据评估练习
            4. 逻辑推理训练
            5. 结论形成指导
            
            重点培养学生的独立思考和分析能力。
        """.trimIndent()
    }

    /**
     * 学习动力激励模板
     * 针对学生学习动力不足的情况提供激励
     *
     * @param currentStatus 当前学习状态
     * @param motivationLevel 动力水平（1-10）
     * @param obstacles 遇到的障碍
     * @param interests 学生兴趣
     * @return 统一格式的prompt字符串
     */
    fun motivateStudent(
        currentStatus: String,
        motivationLevel: Int = 5,
        obstacles: String = "缺乏明确目标",
        interests: String = "未指定"
    ): String {
        return """
            请为学习动力不足的学生提供激励和支持。
            
            当前状态：$currentStatus
            动力水平：$motivationLevel/10
            主要障碍：$obstacles
            学生兴趣：$interests
            
            请提供：
            1. 目标设定指导（短期、中期、长期）
            2. 学习意义的阐释
            3. 克服障碍的具体策略
            4. 兴趣与学习结合的方法
            5. 成就感建立建议
            6. 积极的鼓励话语
            
            采用积极心理学方法，注重内在动机的培养。
        """.trimIndent()
    }
}
