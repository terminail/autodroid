# 插件化AI服务架构 - 实施计划

## 项目信息

**阶段**：writing-plans  
**时间戳**：2026-01-23T00:00:00  
**项目类型**：Android  
**项目根目录**：`d:/git/autodroid/autodroid-TeachItBack`  
**设计文档**：`autodroid-TeachItBack/.superpowers/design/plugin_ai_architecture_brainstorming_20260123.json`  
**状态**：ready

## 实施任务列表

### 第一阶段：核心架构实现（任务1-11）

#### 任务1：创建 AIServiceCapability 数据类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/config/AIServiceCapability.kt`
- **预估时间**：5分钟
- **描述**：定义包含26个AI能力布尔属性的数据类，支持Fluent API配置
- **代码示例**：
```kotlin
@Parcelize
data class AIServiceCapability(
    val supportBasicChat: Boolean = false,
    val supportFileProcessing: Boolean = false,
    val supportMindMapGeneration: Boolean = false,
    // ... 其他19个能力属性
) : Parcelable {
    companion object {
        val EMPTY = AIServiceCapability()
        val BASIC_CHAT = AIServiceCapability(supportBasicChat = true)
        val FULL_CAPABILITIES = AIServiceCapability(// 所有能力为true)
    }
    // Fluent API方法
    fun supportBasicChat(value: Boolean) = copy(supportBasicChat = value)
    // ... 其他Fluent API方法
}
```
- **验证步骤**：
  - 数据类正确定义
  - 所有Fluent API方法生成
  - 编译通过

---

#### 任务2：创建 AIServiceRequiredFields 数据类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/config/AIServiceRequiredFields.kt`
- **预估时间**：3分钟
- **描述**：定义配置需求管理类，包含预定义配置模板
- **代码示例**：
```kotlin
@Parcelize
data class AIServiceRequiredFields(
    val requireApiKey: Boolean = false,
    val requireSecretId: Boolean = false,
    val requireBaseUrl: Boolean = false,
    val requireRegion: Boolean = false,
    val requireModel: Boolean = false
) : Parcelable {
    companion object {
        val NO_REQUIRED_FIELDS = AIServiceRequiredFields()
        val API_KEY_ONLY = AIServiceRequiredFields(requireApiKey = true)
        val API_AND_URL = AIServiceRequiredFields(requireApiKey = true, requireBaseUrl = true)
        val CLOUD_SERVICE = AIServiceRequiredFields(// 所有需求为true)
    }
}
```
- **验证步骤**：
  - 数据类定义正确
  - 所有预定义模板创建
  - Parcelize支持正确

---

#### 任务3：创建 AIServiceConfig 密封类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/config/AIServiceConfig.kt`
- **预估时间**：5分钟
- **描述**：定义统一的AI服务配置密封类，包含所有配置属性和配置数据类
- **代码示例**：
```kotlin
sealed class AIServiceConfig {
    abstract val id: String
    abstract val name: String
    abstract val displayName: String
    abstract val description: String
    abstract val requiredFields: AIServiceRequiredFields
    abstract val secretId: String
    abstract val apiKey: String
    abstract val baseUrl: String
    abstract val region: String
    abstract val model: String
    abstract val capabilities: AIServiceCapability
    abstract val freeQuota: Long
    abstract val pricePerMillion: Double

    data class TencentHunyuanConfig(
        override val id: String = "tencent-hunyuan",
        override val name: String = "Tencent Hunyuan",
        override val displayName: String = "腾讯云混元",
        override val description: String = "腾讯云混元大模型",
        override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.CLOUD_SERVICE,
        // ... 其他配置字段
    ) : AIServiceConfig()

    data class BaichuanConfig(
        // ... 百川配置
    ) : AIServiceConfig()
}
```
- **验证步骤**：
  - 密封类定义正确
  - 配置类包含所有必需字段
  - Fluent API使用正确

---

#### 任务4：更新 AIService 接口定义
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/api/AIService.kt`
- **预估时间**：3分钟
- **描述**：在现有AIService接口基础上，添加AIProcessInfo和AIServiceResponse支持
- **代码示例**：
```kotlin
interface AIService {
    val config: AIServiceConfig
    val isAvailable: Boolean
    val remainingQuota: Long

    // 基础对话功能
    suspend fun sendMessage(messages: List<MessageEntity>, context: String): AIServiceResponse
    suspend fun processFileContent(content: String, context: String): AIServiceResponse

    // 教育专用功能
    suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity?
    suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis
    suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String>
    suspend fun evaluateAnswer(userAnswer: String, correctAnswer: String): AnswerEvaluation

    // 知识处理功能
    suspend fun parseDocument(fileContent: String, fileType: String): DocumentAnalysis
    suspend fun extractKeyConcepts(content: String): List<Concept>
    suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph

    // 状态检测和配额管理
    suspend fun checkStatus(): ServiceStatus
    suspend fun getUsageStatistics(): UsageStatistics

    // 配置管理
    suspend fun updateConfig(newConfig: AIServiceConfig)
    suspend fun checkStatus(): Boolean
}
```
- **验证步骤**：
  - 接口方法定义完整
  - 返回类型正确
  - 参数类型正确

---

#### 任务5：创建 AIProcessInfo 数据类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/model/AIProcessInfo.kt`
- **预估时间**：3分钟
- **描述**：定义AI服务处理信息数据类，用于记录每次AI调用的详情
- **代码示例**：
```kotlin
@Parcelize
data class AIProcessInfo(
    val serviceId: String,
    val serviceName: String,
    val modelUsed: String? = null,
    val processingTime: Long? = null
) : Parcelable
```
- **验证步骤**：
  - 数据类定义正确
  - Parcelize注解添加
  - 字段完整

---

#### 任务6：创建 AIServiceResponse 数据类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/model/AIServiceResponse.kt`
- **预估时间**：3分钟
- **描述**：定义AI服务响应数据类，包含内容和处理信息
- **代码示例**：
```kotlin
@Parcelize
data class AIServiceResponse(
    val content: String,
    val processInfo: AIProcessInfo
) : Parcelable
```
- **验证步骤**：
  - 数据类定义正确
  - 包含所有必需字段

---

#### 任务7：创建 ServiceStatus 枚举
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/model/ServiceStatus.kt`
- **预估时间**：3分钟
- **描述**：定义AI服务状态枚举，包含各种服务状态
- **代码示例**：
```kotlin
enum class ServiceStatus {
    AVAILABLE,
    INSUFFICIENT_BALANCE,
    RATE_LIMITED,
    UNAUTHORIZED,
    UNAVAILABLE,
    MODEL_NOT_SUPPORTED,
    QUOTA_EXHAUSTED
}
```
- **验证步骤**：
  - 枚举值定义完整
  - 命名清晰

---

#### 任务8：创建 AIServiceRegistry 类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/registry/AIServiceRegistry.kt`
- **预估时间**：5分钟
- **描述**：实现AI服务注册中心，支持动态插件注册和能力发现
- **代码示例**：
```kotlin
class AIServiceRegistry {
    private val services = mutableMapOf<String, AIService>()

    fun registerAiService(service: AIService) {
        services[service.config.id] = service
    }

    fun getAvailableServices(capabilityCheck: (AIServiceCapability) -> Boolean): List<AIService> {
        return services.values.filter { service ->
            service.isAvailable && capabilityCheck(service.config.capabilities)
        }
    }

    fun getService(modelId: String): AIService? {
        return services[modelId]
    }

    fun getAllServices(): List<AIService> {
        return services.values.toList()
    }

    fun unregisterService(modelId: String) {
        services.remove(modelId)
    }
}
```
- **验证步骤**：
  - 注册方法实现正确
  - 服务查询方法完整
  - 能力检查正确

---

#### 任务9：创建 AIServiceRouter 类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/router/AIServiceRouter.kt`
- **预估时间**：5分钟
- **描述**：实现智能路由引擎，支持基于能力和成本的路由决策
- **代码示例**：
```kotlin
class AIServiceRouter(private val serviceRegistry: AIServiceRegistry) {

    suspend fun <T> routeByCapability(
        capabilityCheck: (AIServiceCapability) -> Boolean,
        operation: suspend (AIService) -> T
    ): T {
        val allServices = serviceRegistry.getAllServices()
        val availableServices = allServices.filter { service ->
            capabilityCheck(service.config.capabilities)
        }

        if (availableServices.isEmpty()) {
            throw AIServiceException("没有可用的AI服务支持该功能")
        }

        val servicesByCost = availableServices.sortedBy { it.config.pricePerMillion }
        return tryRoute(servicesByCost, operation)
    }

    private suspend fun <T> tryRoute(
        services: List<AIService>,
        operation: suspend (AIService) -> T
    ): T {
        val exceptions = mutableListOf<Exception>()

        for (service in services) {
            try {
                val status = service.checkStatus()
                when (status) {
                    ServiceStatus.AVAILABLE -> return operation(service)
                    ServiceStatus.INSUFFICIENT_BALANCE -> 
                        exceptions.add(InsufficientBalanceException("${service.config.displayName} 余额不足"))
                    ServiceStatus.RATE_LIMITED -> 
                        exceptions.add(RateLimitException("${service.config.displayName} 频率限制"))
                    else -> exceptions.add(Exception("${service.config.displayName} 不可用"))
                }
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        throw AIServiceException("所有可用服务都失败：${exceptions.joinToString { it.message ?: "未知错误" }}")
    }

    // 路由函数
    suspend fun routeBasicChat(operation: suspend (AIService) -> String): String {
        return routeByCapability(
            capabilityCheck = { it.supportBasicChat },
            operation = operation
        )
    }

    suspend fun routeMindMapGeneration(operation: suspend (AIService) -> MindMapEntity?): MindMapEntity? {
        return routeByCapability(
            capabilityCheck = { it.supportMindMapGeneration },
            operation = operation
        )
    }
}
```
- **验证步骤**：
  - 路由逻辑正确
  - 容错处理完善
  - 成本排序正确

---

#### 任务10：创建 AIServiceConfigHelper 类
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/helper/AIServiceConfigHelper.kt`
- **预估时间**：5分钟
- **描述**：创建配置辅助类，提供统一的配置界面设置方法
- **代码示例**：
```kotlin
object AIServiceConfigHelper {

    fun setupAIServiceConfigUI(
        secretIdField: View,
        apiKeyField: View,
        baseUrlField: View,
        regionField: View,
        modelField: View,
        config: AIServiceConfig
    ) {
        secretIdField.visibility = if (config.requiredFields.requireSecretId) View.VISIBLE else View.GONE
        apiKeyField.visibility = if (config.requiredFields.requireApiKey) View.VISIBLE else View.GONE
        baseUrlField.visibility = if (config.requiredFields.requireBaseUrl) View.VISIBLE else View.GONE
        regionField.visibility = if (config.requiredFields.requireRegion) View.VISIBLE else View.GONE
        modelField.visibility = if (config.requiredFields.requireModel) View.VISIBLE else View.GONE
    }

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
            is AIServiceConfig.BaichuanConfig -> baseConfig.copy(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model
            )
        }
    }
}
```
- **验证步骤**：
  - 配置UI设置方法正确
  - 配置数据获取方法正确

---

#### 任务11：创建 PromptTemplates 统一Prompt管理
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/config/PromptTemplates.kt`
- **预估时间**：10分钟
- **描述**：创建统一Prompt模板管理类，所有AI服务共享相同的prompt，确保输出一致性
- **核心原则**：同一个功能的所有AI服务使用完全相同的Prompt模板
- **代码示例**：
```kotlin
object PromptTemplates {
    
    /**
     * 文件内容处理
     * 所有AI服务使用此相同prompt
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
     */
    fun parseDocument(fileContent: String, fileType: String): String {
        return """
            请分析以下$fileType文档内容：
            
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
}
```
- **设计优势**：
  - 所有AI服务使用相同prompt，用户体验一致
  - 维护成本低，修改一处即可影响所有服务
  - 支持公平的模型对比测试
  - 便于集中优化prompt质量
- **验证步骤**：
  - PromptTemplates对象创建成功
  - 所有功能方法定义完整
  - Prompt内容清晰、具体、可执行

---

### 第二阶段：UI实现（任务12-13）

#### 任务12：创建统一配置布局文件
- **文件路径**：`app/src/main/res/layout/fragment_ai_service_detail.xml`
- **预估时间**：5分钟
- **描述**：创建通用的AI服务配置布局，支持动态显示字段
- **验证步骤**：
  - 布局文件结构正确
  - 所有字段ID定义正确
  - 样式设置合理

---

#### 任务13：创建 AIServiceDetailFragment
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/fragment/SettingsAIServiceDetailFragment.kt`
- **预估时间**：5分钟
- **描述**：创建通用的AI服务配置Fragment，支持所有AI服务类型的统一配置
- **验证步骤**：
  - Fragment生命周期正确
  - 配置UI设置正确
  - 保存逻辑完整

---

### 第三阶段：AI服务实现（任务14-19）

**重要说明**：所有AI服务实现必须使用`PromptTemplates`中的统一prompt，确保不同服务输出一致性

#### 任务14：创建 AIServiceDeepSeek 实现
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/service/AIServiceDeepSeek.kt`
- **预估时间**：10分钟
- **描述**：实现DeepSeek AI服务，支持基础对话、长文本、代码生成、数学计算等功能
- **核心能力**：基础对话、文件处理、长文本、代码生成、数学计算
- **代码要点**：
  - 实现所有AIService接口方法
  - HTTP客户端配置和请求构建
  - 响应解析和AIProcessInfo生成
  - 错误处理和服务状态检测
- **验证步骤**：
  - 所有AIService接口方法实现
  - API调用正确
  - 错误处理完善

---

#### 任务15：创建 AIServiceMiniMax 实现
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/service/AIServiceMiniMax.kt`
- **预估时间**：10分钟
- **描述**：实现MiniMax AI服务，支持基础对话和创意写作功能
- **核心能力**：基础对话、文件处理、创意写作、多模态
- **代码要点**：
  - MiniMax API集成
  - 支持创意写作提示工程
  - 多模态能力支持
- **验证步骤**：
  - 所有AIService接口方法实现
  - MiniMax API调用正确
  - 响应解析正确

---

#### 任务16：创建 AIServiceTencentHunyuan 实现
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/service/AIServiceTencentHunyuan.kt`
- **预估时间**：15分钟
- **描述**：实现腾讯云混元AI服务，支持教育专用功能（思维导图、学习分析、苏格拉底提问等）
- **核心能力**：基础对话、文件处理、思维导图、学习分析、苏格拉底提问、答案评估、文档解析、概念提取、知识图谱、教育专用
- **代码要点**：
  - 腾讯云签名算法
  - 知识引擎API调用
  - 文档解析能力
  - 教育功能提示工程
- **验证步骤**：
  - 所有教育功能实现
  - 腾讯云API集成正确
  - 思维导图生成正确

---

#### 任务17：创建 AIServiceBaichuan 实现
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/service/AIServiceBaichuan.kt`
- **预估时间**：10分钟
- **描述**：实现百川AI服务，支持基础对话功能
- **核心能力**：基础对话
- **代码要点**：
  - 百川API集成
  - 简化配置流程
  - 只支持基础对话
- **验证步骤**：
  - 基础对话功能实现
  - 百川API调用正确

---

#### 任务18：更新 AIServiceConfig 添加新服务配置
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/config/AIServiceConfig.kt`
- **预估时间**：10分钟
- **描述**：在AIServiceConfig密封类中添加DeepSeek、MiniMax等新服务的配置
- **配置内容**：
  - DeepSeekConfig: API_KEY_ONLY, 基础对话+代码+数学, 价格1.0/百万
  - MiniMaxConfig: API_AND_URL, 基础对话+创意+多模态, 价格5.0/百万
  - TencentHunyuanConfig: CLOUD_SERVICE, 全功能, 价格12.0/百万
  - BaichuanConfig: API_AND_URL, 基础对话, 价格4.0/百万
- **验证步骤**：
  - 所有服务配置添加
  - 能力设置正确
  - 价格和配额合理

---

#### 任务19：更新 AIServiceInitializer 注册新服务
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/initializer/AIServiceInitializer.kt`
- **预估时间**：5分钟
- **描述**：更新AIServiceInitializer，注册所有新的AI服务实现
- **注册顺序**：
  1. 腾讯云混元（教育功能最强，优先级高）
  2. DeepSeek（成本低，基础功能）
  3. MiniMax（创意写作）
  4. 百川（简单配置）
- **验证步骤**：
  - 所有服务注册
  - 服务数量正确
  - 日志输出正确

---

### 第四阶段：架构集成（任务20-21）

**架构说明**：
- **Repository 层内部集成 AIServiceRouter**：AIServiceRouter 作为 Repository 的私有依赖，对外部不可见
- **ViewModel 不感知 AI 服务**：ViewModel 只调用 Repository 的高层业务方法，不知道 AI 服务的存在
- **Local-First 策略**：Repository 先从本地数据库读取数据返回，然后异步调用 AI 服务更新数据

#### 任务20：更新 MessageRepository 集成 AIServiceRouter
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/repository/MessageRepository.kt`
- **预估时间**：5分钟
- **描述**：更新MessageRepository使用AIServiceRouter单例替代直接调用AI服务，实现智能路由
- **架构原则**：Repository 内部使用 AIServiceRouter 单例，ViewModel 不感知 AI 服务
- **代码示例**：
```kotlin
class MessageRepository(
    private val messageDao: MessageDao
    // 注意：AIServiceRouter 不作为构造函数参数，Repository 内部直接使用单例
) {
    
    // Repository 内部使用 AIServiceRouter 单例
    private val aiRouter: AIServiceRouter
        get() = AIServiceRouter
    
    fun getMessagesByTopic(topicId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesByTopic(topicId)
    
    suspend fun sendMessageAndGetReply(
        topicId: String,
        userContent: String
    ): MessageEntity? {
        // 1. 保存用户消息到本地数据库
        val userMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            content = userContent,
            senderType = "USER",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMessage)
        
        // 2. 获取对话历史（用于AI分析）
        val conversationHistory = messageDao.getMessagesByTopicSync(topicId)
        
        // 3. 使用AIServiceRouter调用AI服务分析进度
        val progressAnalysis = aiRouter.routeByCapability(
            capabilityCheck = { it.supportLearningAnalysis },
            operation = { service -> service.analyzeLearningProgress(conversationHistory) }
        )
        
        // 4. 构建上下文并使用AIServiceRouter获取AI回复
        val context = buildContext(conversationHistory, progressAnalysis)
        val aiResponse = aiRouter.routeBasicChat { service ->
            service.sendMessage(conversationHistory, context)
        }
        
        // 5. 保存AI回复到本地数据库（包含AIProcessInfo）
        val aiMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            content = aiResponse.content,
            senderType = "AI",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis(),
            aiProcessInfo = aiResponse.processInfo
        )
        messageDao.insertMessage(aiMessage)
        
        return aiMessage
    }
    
    private fun buildContext(
        conversationHistory: List<MessageEntity>,
        progressAnalysis: ProgressAnalysis
    ): String {
        return buildString {
            append("当前学习进度: ${progressAnalysis.overallProgress}%\n")
            append("掌握度: ${progressAnalysis.conceptMastery}\n")
            if (progressAnalysis.knowledgeGaps.isNotEmpty()) {
                append("知识缺口: ${progressAnalysis.knowledgeGaps.joinToString(", ")}\n")
            }
            if (progressAnalysis.recommendedNextSteps.isNotEmpty()) {
                append("推荐下一步: ${progressAnalysis.recommendedNextSteps.joinToString(", ")}\n")
            }
        }
    }
    
    suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)
    suspend fun updateMessage(message: MessageEntity) = messageDao.updateMessage(message)
    suspend fun deleteMessagesByTopic(topicId: String) =
        messageDao.deleteMessagesByTopic(topicId)
}
```
- **验证步骤**：
  - 构造函数接受AIServiceRouter
  - 使用AIServiceRouter路由调用
  - AIProcessInfo正确记录

---

#### 任务21：更新 MindMapRepository 集成 AIServiceRouter
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/repository/MindMapRepository.kt`
- **预估时间**：5分钟
- **描述**：更新MindMapRepository使用AIServiceRouter单例替代特定AI服务，实现智能路由
- **架构原则**：Repository 内部使用 AIServiceRouter 单例，ViewModel 不感知 AI 服务
- **代码示例**：
```kotlin
class MindMapRepository(
    private val database: AppDatabase
    // 注意：AIServiceRouter 不作为构造函数参数，Repository 内部直接使用单例
) {
    
    // Repository 内部使用 AIServiceRouter 单例
    private val aiRouter: AIServiceRouter
        get() = AIServiceRouter
    
    fun getAllMindMapsFlow() = database.mindMapDao().getAllMindMapsFlow()
    fun getAllNodesFlow() = database.mindMapDao().getAllNodesFlow()
    fun getNodesByMindMapFlow(mindMapId: String) = database.mindMapDao().getNodesByMindMap(mindMapId)
    fun getAllRootNodesFlow() = database.mindMapDao().getAllRootNodesFlow()
    
    suspend fun getMindMapByTopicId(topicId: String): MindMapEntity? {
        return database.mindMapDao().getByTopicId(topicId)
    }
    
    suspend fun generateMindMap(topicId: String, learningGoal: String? = null): MindMapEntity? {
        // 1. 优先返回本地已有的MindMap
        val existingMindMap = database.mindMapDao().getByTopicId(topicId)
        if (existingMindMap != null) {
            return existingMindMap
        }
        
        // 2. 获取Topic信息
        val topic = database.topicDao().getTopicByIdSync(topicId)
        if (topic == null) {
            return null
        }
        
        // 3. 使用AIServiceRouter智能路由到合适的AI服务生成MindMap
        val mindMapEntity = aiRouter.routeMindMapGeneration { service ->
            val startTime = System.currentTimeMillis()
            val result = service.generateMindMap(topicId, learningGoal ?: topic.title)
            val processingTime = System.currentTimeMillis() - startTime
            
            // 添加AIProcessInfo到结果中
            result?.copy(
                aiProcessInfo = AIProcessInfo(
                    serviceId = service.config.id,
                    serviceName = service.config.displayName,
                    modelUsed = service.config.model,
                    processingTime = processingTime
                )
            )
        }
        
        // 4. 保存AI结果到本地数据库
        if (mindMapEntity != null) {
            database.mindMapDao().insert(mindMapEntity)
            
            // 保存MindMap节点
            val nodes = database.mindMapDao().getNodesByMindMapSync(mindMapEntity.id)
            if (nodes.isNotEmpty()) {
                database.mindMapDao().insertNodes(nodes)
            }
        }
        
        return mindMapEntity
    }
    
    suspend fun updateNodeProgress(nodeId: String, progress: Int) {
        database.mindMapDao().updateNodeProgress(nodeId, progress)
    }
    
    suspend fun getNodesByMindMapId(mindMapId: String): List<MindMapNode> {
        return database.mindMapDao().getNodesByMindMapSync(mindMapId)
    }
    
    suspend fun insertMindMap(mindMap: MindMapEntity) {
        database.mindMapDao().insert(mindMap)
    }
    
    suspend fun insertMindMapNode(node: MindMapNode) {
        database.mindMapDao().insertNode(node)
    }
    
    suspend fun insertMindMapNodes(nodes: List<MindMapNode>) {
        database.mindMapDao().insertNodes(nodes)
    }
}
```
- **验证步骤**：
  - 构造函数接受AIServiceRouter
  - 使用AIServiceRouter路由MindMap生成
  - AIProcessInfo正确记录

---

#### 任务21：更新 ChatViewModel 适配 Repository 新架构
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/viewmodel/ChatViewModel.kt`
- **预估时间**：5分钟
- **描述**：更新ChatViewModel，使其只与Repository交互，不感知AI服务细节
- **架构原则**：ViewModel 只依赖 Repository，不感知 AIServiceRouter 和 AI 服务
- **代码示例**：
```kotlin
class ChatViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    
    // ViewModel只与Repository交互，完全不感知AI服务
    private val topicRepository = TopicRepository(database.topicDao())
    private val messageRepository = MessageRepository(
        messageDao = database.messageDao()
        // Repository 内部使用 AIServiceRouter 单例，对 ViewModel 完全透明
    )
    private val mindMapRepository = MindMapRepository(
        database = database
        // Repository 内部使用 AIServiceRouter 单例，对 ViewModel 完全透明
    )
    
    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadMessages(topicId: String) {
        viewModelScope.launch {
            messageRepository.getMessagesByTopic(topicId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }
    
    fun sendMessage(topicId: String, userContent: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val aiMessage = messageRepository.sendMessageAndGetReply(topicId, userContent)
                // AI回复会自动通过Flow更新，ViewModel不感知AI服务
            } catch (e: Exception) {
                Log.e("ChatViewModel", "发送消息失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun generateMindMap(topicId: String, learningGoal: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mindMap = mindMapRepository.generateMindMap(topicId, learningGoal)
                // 可以触发MindMap更新通知
            } catch (e: Exception) {
                Log.e("ChatViewModel", "生成思维导图失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```
- **验证步骤**：
  - ViewModel不感知AI服务
  - 只通过Repository访问数据
  - 消息和MindMap生成逻辑正确

---

### 第五阶段：测试（任务23-24）

#### 任务22：创建单元测试 - MessageRepository集成AIServiceRouter
- **文件路径**：`app/src/test/java/com/autodroid/teachitback/repository/MessageRepositoryTest.kt`
- **预估时间**：5分钟
- **描述**：为MessageRepository编写单元测试，验证其与AIServiceRouter单例的交互
- **架构说明**：由于 Repository 使用 AIServiceRouter 单例，测试时需要考虑如何 mock
- **代码示例**：
```kotlin
class MessageRepositoryTest {
    private lateinit var repository: MessageRepository
    private lateinit var mockDao: MessageDao

    @Before
    fun setup() {
        mockDao = mockk<MessageDao>(relaxed = true)
        // Repository 只需要 MessageDao，不需要注入 AIServiceRouter
        repository = MessageRepository(mockDao)
        // AIServiceRouter 是 Repository 内部的单例，对测试透明
    }

    @Test
    fun testSendMessageUsesRouter() = runBlocking {
        val mockResponse = AIServiceResponse(
            content = "AI回复",
            processInfo = AIProcessInfo(
                serviceId = "test-service",
                serviceName = "Test Service"
            )
        )
        
        coEvery { mockRouter.routeBasicChat(any()) } returns "AI回复"
        coEvery { mockRouter.routeByCapability(any(), any()) } returns mock(
                overallProgress = 50,
                conceptMastery = "中等",
                knowledgeGaps = emptyList(),
                recommendedNextSteps = emptyList()
            )
        
        val result = repository.sendMessageAndGetReply("topic-1", "用户消息")
        
        assertNotNull(result)
        coVerify { mockRouter.routeBasicChat(any()) }
        coVerify { mockRouter.routeByCapability(any(), any()) }
    }
}
```
- **验证步骤**：
  - 路由调用验证正确
  - AIProcessInfo记录正确

---

#### 任务23：创建单元测试 - MindMapRepository集成AIServiceRouter
- **文件路径**：`app/src/test/java/com/autodroid/teachitback/repository/MindMapRepositoryTest.kt`
- **预估时间**：5分钟
- **描述**：为MindMapRepository编写单元测试，验证其与AIServiceRouter单例的交互
- **架构说明**：由于 Repository 使用 AIServiceRouter 单例，测试时需要考虑如何 mock
- **代码示例**：
```kotlin
class MindMapRepositoryTest {
    private lateinit var repository: MindMapRepository
    private lateinit var mockDatabase: AppDatabase

    @Before
    fun setup() {
        mockDatabase = mockk<AppDatabase>(relaxed = true)
        // Repository 只需要 AppDatabase，不需要注入 AIServiceRouter
        repository = MindMapRepository(mockDatabase)
        // AIServiceRouter 是 Repository 内部的单例，对测试透明
    }

    @Test
    fun testGenerateMindMapUsesRouter() = runBlocking {
        val topicId = "topic-1"
        val learningGoal = "学习目标"
        
        val mockMindMap = MindMapEntity(
            id = "mindmap-1",
            topicId = topicId,
            title = "测试思维导图",
            mindMap = MindMap(emptyList()),
            aiProcessInfo = AIProcessInfo(
                serviceId = "test-service",
                serviceName = "Test Service"
            )
        )
        
        coEvery { mockDatabase.mindMapDao().getByTopicId(topicId) } returns null
        coEvery { mockDatabase.topicDao().getTopicByIdSync(topicId) } returns mockk(relaxed = true)
        // 注意：由于 AIServiceRouter 是单例，这里可能需要特殊处理 mock
        
        val result = repository.generateMindMap(topicId, learningGoal)
        
        assertNotNull(result)
        coVerify { mockRouter.routeMindMapGeneration(any()) }
    }

    @Test
    fun testGenerateMindMapReturnsExisting() = runBlocking {
        val topicId = "topic-1"
        val existingMindMap = MindMapEntity(
            id = "mindmap-1",
            topicId = topicId,
            title = "现有思维导图",
            mindMap = MindMap(emptyList())
        )
        
        coEvery { mockDatabase.mindMapDao().getByTopicId(topicId) } returns existingMindMap
        
        val result = repository.generateMindMap(topicId)
        
        assertEquals(existingMindMap, result)
        coVerify(exactly = 0) { mockRouter.routeMindMapGeneration(any()) }
    }
}
```
- **验证步骤**：
  - 路由调用验证正确
  - Local-First策略正确
  - AIProcessInfo记录正确

---

#### 任务25：更新 MessageEntity 添加 AIProcessInfo
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/model/MessageEntity.kt`
- **预估时间**：3分钟
- **描述**：在MessageEntity中添加aiProcessInfo字段，用于记录AI处理信息
- **代码示例**：
```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val content: String,
    val senderType: String, // USER or AI
    val messageType: String, // TEXT, AUDIO, FILE_CONTENT
    val timestamp: Long = System.currentTimeMillis(),
    
    // AI服务处理信息
    val aiProcessInfo: AIProcessInfo? = null
)
```
- **验证步骤**：
  - 字段添加正确
  - 数据类编译通过

---

#### 任务26：更新 MindMapEntity 添加 AIProcessInfo
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/model/MindMapEntity.kt`
- **预估时间**：3分钟
- **描述**：在MindMapEntity中添加aiProcessInfo字段，用于记录AI生成信息
- **代码示例**：
```kotlin
@Entity(tableName = "mindmaps")
data class MindMapEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val title: String,
    val mindMap: MindMap,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // AI服务处理信息
    val aiProcessInfo: AIProcessInfo? = null
)
```
- **验证步骤**：
  - 字段添加正确
  - 数据类编译通过

---

#### 任务27：更新 AppDatabase 版本
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/database/AppDatabase.kt`
- **预估时间**：3分钟
- **描述**：更新数据库版本号，添加AIProcessInfo字段的迁移策略
- **代码示例**：
```kotlin
@Database(
    entities = [TopicEntity::class, MessageEntity::class, MindMapEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun messageDao(): MessageDao
    abstract fun mindMapDao(): MindMapDao
    
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "teachitback_db")
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE messages ADD COLUMN aiProcessInfo TEXT")
        database.execSQL("ALTER TABLE mindmaps ADD COLUMN aiProcessInfo TEXT")
    }
}
```
- **验证步骤**：
  - 版本号更新正确
  - 迁移脚本正确

---

#### 任务27：更新数据库DAO支持AIProcessInfo
- **文件路径**：`app/src/main/java/com/autodroid/teachitback/database/MessageDao.kt, app/src/main/java/com/autodroid/teachitback/database/MindMapDao.kt`
- **预估时间**：5分钟
- **描述**：更新MessageDao和MindMapDao以支持AIProcessInfo字段的序列化和反序列化
- **代码示例**：
```kotlin
// MessageDao.kt
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE topicId = :topicId ORDER BY timestamp ASC")
    fun getMessagesByTopic(topicId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE topicId = :topicId ORDER BY timestamp ASC")
    suspend fun getMessagesByTopicSync(topicId: String): List<MessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE topicId = :topicId")
    suspend fun deleteMessagesByTopic(topicId: String)
    
    @Query("SELECT * FROM messages WHERE aiProcessInfo LIKE '%' || :serviceId || '%'")
    fun getMessagesByService(serviceId: String): Flow<List<MessageEntity>>
}

// MindMapDao.kt
@Dao
interface MindMapDao {
    @Query("SELECT * FROM mindmaps")
    fun getAllMindMapsFlow(): Flow<List<MindMapEntity>>
    
    @Query("SELECT * FROM mindmaps WHERE topicId = :topicId")
    suspend fun getByTopicId(topicId: String): MindMapEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mindMap: MindMapEntity)
    
    @Update
    suspend fun update(mindMap: MindMapEntity)
    
    @Query("SELECT * FROM mindmaps WHERE aiProcessInfo LIKE '%' || :serviceId || '%'")
    fun getMindMapsByService(serviceId: String): Flow<List<MindMapEntity>>
}
```
- **验证步骤**：
  - DAO方法定义正确
  - 查询逻辑支持AIProcessInfo

---

## 实施总览

### 时间估算
- **核心架构实现**：45分钟（任务1-11，含PromptTemplates）
- **UI实现**：10分钟（任务12-13）
- **AI服务实现**：60分钟（任务14-19，使用统一prompt）
- **架构集成**：10分钟（任务20-22）
- **测试**：10分钟（任务23-24）
- **数据模型更新**：14分钟（任务25-28）

**总预估时间**：149分钟（约2.5小时）

### 依赖关系
1. 任务1-3必须先完成（核心数据类）
2. 任务4-11依赖于任务1-3（PromptTemplates需要AIServiceConfig）
3. 任务12-13依赖于任务10
4. 任务14-19依赖于任务1-3和任务11（AI服务实现需要PromptTemplates）
5. 任务20-22依赖于任务14-19
6. 任务23-24依赖于任务20-22
7. 任务25-28依赖于任务4-6

### AI服务实现概览

| AI服务 | 任务编号 | 核心能力 | 价格/百万token | 优先级 |
|---------|----------|----------|--------------|--------|
| 腾讯云混元 | 16 | 全功能（教育专用） | 12.0 | 高 |
| DeepSeek | 14 | 基础对话+代码+数学 | 1.0 | 中 |
| MiniMax | 15 | 基础对话+创意+多模态 | 5.0 | 中 |
| 百川 | 17 | 基础对话 | 4.0 | 低 |

### 架构层次
```
ViewModel Layer (ChatViewModel, SettingsViewModel)
    ↓ 只与 Repository 交互，不感知 AI 服务
Repository Layer (MessageRepository, MindMapRepository)
    ↓ 内部集成 AIServiceRouter 和 AIServiceRegistry（实现细节，对外隐藏）
AI Service Layer (AIServiceRouter + AIServiceRegistry)
    ↓ 智能路由、服务管理、统一配置
AIService Layer (腾讯云混元, DeepSeek, MiniMax, 百川, etc.)
    ↓ 实现具体AI能力
API Layer (各模型API)
```

**架构原则**：
- **MVVM 模式**：ViewModel 只与 Repository 交互，不感知 AI 服务
- **Local-First 策略**：Repository 先从本地数据库读取数据返回，然后异步调用 AI 服务更新数据
- **AI 服务抽象**：AI 服务调用对 ViewModel 透明，就像普通网络 API 一样

### 关键里程碑
1. **里程碑1**：核心架构完成（任务1-11，含PromptTemplates）
2. **里程碑2**：UI实现完成（任务12-13）
3. **里程碑3**：AI服务实现完成（任务14-19，使用统一prompt）
4. **里程碑4**：架构集成完成（任务20-22）
5. **里程碑5**：测试完成（任务23-24）
6. **里程碑6**：数据迁移完成（任务25-28）

### 风险评估
- **低风险**：数据类定义、枚举创建
- **中风险**：路由逻辑实现、配置UI动态生成
- **高风险**：数据库迁移、现有代码的兼容性、第三方API集成

### 成功标准
1. 所有单元测试通过
2. 编译无错误
3. 数据库迁移成功
4. 配置界面功能正常
5. 路由逻辑按预期工作
6. Repository内部集成AIServiceRouter（ViewModel不感知AI服务）
7. AIProcessInfo正确记录和查询
8. 至少4个AI服务实现（腾讯云混元、DeepSeek、MiniMax、百川）
9. 智能路由能够正确选择合适的AI服务
10. Local-First策略正确实现（Repository先从数据库读取，异步调用AI）
11. **所有AI服务使用统一prompt（PromptTemplates），输出一致性验证通过**

## 下一步行动

1. 按照任务顺序依次实施
2. 每个任务完成后进行验证
3. 遇到问题及时调整策略
4. 定期同步代码到版本控制
5. 优先实现4个核心AI服务，满足app基本功能需求