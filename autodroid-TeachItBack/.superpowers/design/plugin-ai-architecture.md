# 插件化AI服务架构设计

## 核心架构

```
UI Layer (Fragment/Activity)
    ↓
ViewModel Layer (ChatViewModel, SettingsViewModel)
    - 只与 Repository 交互，不感知 AI 服务细节
    ↓
Repository Layer (MessageRepository, MindMapRepository)
    - 内部集成 AIServiceRouter 和 AIServiceRegistry（实现细节，对外隐藏）
    - Local-First 策略：先从本地数据库读取，异步调用 AI 服务同步数据
    ↓
AI Service Layer (AIServiceRouter + AIServiceRegistry)
    - 智能路由、服务管理、统一配置
    ↓
AIService Layer (AIService x10+)
    ↓
API Layer (各模型API)
```

**架构原则**：
- **MVVM 模式**：ViewModel 只与 Repository 交互，不感知 AI 服务
- **Local-First 策略**：Repository 先从本地数据库读取数据返回，然后异步调用 AI 服务更新数据
- **AI 服务抽象**：AI 服务调用对 ViewModel 透明，就像普通网络 API 一样

## 统一配置接口

### 统一配置界面设计

**问题**：当前每个AI服务都有独立的XML配置文件，造成大量重复代码：
- 13个AI服务 × 2个配置文件 = 26个重复的布局文件
- 每个文件结构高度相似，只有标题和提示文本不同

**解决方案**：用统一的动态配置界面替代所有独立配置文件

### AIServiceConfig 数据类
```kotlin
// AI服务配置需求类 - 使用布尔属性替代字符串集合
@Parcelize
data class AIServiceRequiredFields(
    val requireApiKey: Boolean = false,
    val requireSecretId: Boolean = false,
    val requireBaseUrl: Boolean = false,
    val requireRegion: Boolean = false,
    val requireModel: Boolean = false
) {
    companion object {
        // 预定义配置需求模板
        val NO_REQUIRED_FIELDS = AIServiceRequiredFields()
        val API_KEY_ONLY = AIServiceRequiredFields(requireApiKey = true)
        val API_AND_URL = AIServiceRequiredFields(requireApiKey = true, requireBaseUrl = true)
        val CLOUD_SERVICE = AIServiceRequiredFields(
            requireApiKey = true,
            requireSecretId = true,
            requireBaseUrl = true,
            requireRegion = true,
            requireModel = true
        )
    }
}

data class AIServiceConfig(
    val modelId: String,           // 模型唯一标识
    val displayName: String,       // 显示名称
    val apiKey: String = "",       // API密钥
    val secretId: String = "",     // 密钥ID（部分模型需要）
    val baseUrl: String,           // 基础URL（代码中写死）
    val freeQuota: Long,           // 免费额度（tokens）
    val pricePerMillion: Double,   // 百万token价格
    
    // 配置需求 - 使用AIServiceRequiredFields统一管理
    val requiredFields: AIServiceRequiredFields,
    
    // 能力支持 - 使用AIServiceCapability统一管理
    val capabilities: AIServiceCapability
)
```

### 统一AI服务接口
```kotlin
interface AIService {
    val config: AIServiceConfig
    val isAvailable: Boolean
    val remainingQuota: Long
    
    // 基础对话功能
    suspend fun sendMessage(messages: List<MessageEntity>, context: String): String
    suspend fun processFileContent(content: String, context: String): String
    
    // 教育专用功能
    suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity?
    suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis
    suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String>
    suspend fun evaluateAnswer(userAnswer: String, correctAnswer: String): AnswerEvaluation
    
    // 知识处理功能
    suspend fun parseDocument(fileContent: String, fileType: String): DocumentAnalysis
    suspend fun extractKeyConcepts(content: String): List<Concept>
    suspend fun buildKnowledgeGraph(concepts: List<Concept>): KnowledgeGraph
    
    // 特殊功能（根据模型能力可选实现）
    suspend fun generateCode(requirements: String, language: String): String
    suspend fun solveMathProblem(problem: String): String
    suspend fun generateCreativeContent(topic: String, style: String): String
    
    // 多模态功能（视觉、音频等）
    suspend fun analyzeImage(imageData: ByteArray, prompt: String): String
    suspend fun generateImage(prompt: String, style: String): ByteArray?
    
    // 状态检测和配额管理
    suspend fun checkStatus(): ServiceStatus
    suspend fun getUsageStatistics(): UsageStatistics
    
    // 配置管理
    suspend fun updateConfig(newConfig: AIServiceConfig)
    suspend fun checkStatus(): Boolean
}

enum class ServiceStatus {
    AVAILABLE,              // 服务可用
    INSUFFICIENT_BALANCE,   // 余额不足（如DeepSeek 402）
    RATE_LIMITED,           // 频率限制
    UNAUTHORIZED,           // 认证失败
    UNAVAILABLE            // 服务不可用
    MODEL_NOT_SUPPORTED,    // 不支持该功能
    QUOTA_EXHAUSTED         // 配额已用完
}

data class UsageStatistics(
    val totalTokensUsed: Long,
    val remainingQuota: Long,
    val costIncurred: Double,
    val lastUsed: String
)
```

## 插件注册与发现机制

### AIServiceRegistry
```kotlin
class AIServiceRegistry {
    private val services = mutableMapOf<String, AIService>()
    
    fun registerAiService(service: AIService) {
        services[service.config.modelId] = service
    }
    
    fun getAvailableServices(capability: String): List<AIService> {
        return services.values.filter { service ->
            service.isAvailable && service.supportsCapability(capability)
        }
    }
    
    // 根据能力字符串匹配对应的布尔字段
    private fun AIService.supportsCapability(capability: String): Boolean {
        return when (capability) {
            // 基础对话功能
            "sendMessage", "basicChat" -> config.supportBasicChat
            "processFileContent", "fileProcessing" -> config.supportFileProcessing
            
            // 教育专用功能
            "generateMindMap", "mindMap" -> config.supportMindMapGeneration
            "analyzeLearningProgress", "learningAnalysis" -> config.supportLearningAnalysis
            "generateSocraticQuestions", "socraticQuestioning" -> config.supportSocraticQuestioning
            "evaluateAnswer", "answerEvaluation" -> config.supportAnswerEvaluation
            
            // 知识处理功能
            "parseDocument", "documentParsing" -> config.supportDocumentParsing
            "extractKeyConcepts", "conceptExtraction" -> config.supportConceptExtraction
            "buildKnowledgeGraph", "knowledgeGraph" -> config.supportKnowledgeGraph
            
            // 特殊能力支持
            "longText" -> config.supportLongText
            "multimodal" -> config.supportMultimodal
            "education" -> config.supportEducation
            "codeGeneration" -> config.supportCodeGeneration
            "math" -> config.supportMath
            "creativeWriting" -> config.supportCreativeWriting
            
            // 多模态功能
            "analyzeImage", "imageAnalysis" -> config.supportImageAnalysis
            "generateImage", "imageGeneration" -> config.supportImageGeneration
            "audioProcessing" -> config.supportAudioProcessing
            "videoAnalysis" -> config.supportVideoAnalysis
            
            // RAG功能
            "rag", "retrievalAugmentedGeneration" -> config.supportRAG
            
            else -> false
        }
    }
    
    fun getService(modelId: String): AIService? {
        return services[modelId]
    }
    
    fun getAllServices(): List<AIService> {
        return services.values.toList()
    }
}
```

## 智能路由策略

### AIServiceRouter
```kotlin
class AIServiceRouter(private val serviceRegistry: AIServiceRegistry) {
    
    // 路由策略：免费额度优先 → 成本优先
    suspend fun <T> route(
        capability: String,
        operation: suspend (AIService) -> T
    ): T {
        val availableServices = serviceRegistry.getAvailableServices(capability)
        
        if (availableServices.isEmpty()) {
            throw AIServiceException("没有可用的AI服务支持功能：$capability")
        }
        
        // 1. 优先选择有免费额度的服务
        val servicesWithQuota = availableServices.filter { it.remainingQuota > 0 }
        if (servicesWithQuota.isNotEmpty()) {
            return tryRoute(servicesWithQuota.sortedByDescending { it.remainingQuota }, operation)
        }
        
        // 2. 按成本排序选择（价格从低到高）
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
                // 检查服务状态
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
    
    // 动态路由选择 - 基于AICapability布尔属性直接选择服务
    suspend fun <T> routeByCapability(
        capabilityCheck: (AICapability) -> Boolean,
        operation: suspend (AIService) -> T
    ): T {
        val allServices = serviceRegistry.getAllServices()
        
        // 直接基于AICapability布尔属性过滤服务
        val availableServices = allServices.filter { service ->
            capabilityCheck(service.config.capabilities)
        }
        
        if (availableServices.isEmpty()) {
            throw AIServiceException("没有可用的AI服务支持该功能")
        }
        
        // 按成本排序选择服务
        val servicesByCost = availableServices.sortedBy { it.config.pricePerMillion }
        return tryRoute(servicesByCost, operation)
    }
    
    // 使用示例：
    // 基础对话路由
    suspend fun routeBasicChat(operation: suspend (AIService) -> String): String {
        return routeByCapability(
            capabilityCheck = { it.supportBasicChat },
            operation = operation
        )
    }
    
    // 思维导图生成路由
    suspend fun routeMindMapGeneration(operation: suspend (AIService) -> MindMapEntity?): MindMapEntity? {
        return routeByCapability(
            capabilityCheck = { it.supportMindMapGeneration },
            operation = operation
        )
    }
    
    // 文档解析路由
    suspend fun routeDocumentParsing(operation: suspend (AIService) -> DocumentAnalysis): DocumentAnalysis {
        return routeByCapability(
            capabilityCheck = { it.supportDocumentParsing },
            operation = operation
        )
    }
    
    // 其他功能路由类似定义...
    
    // 保留旧方法用于向后兼容
    suspend fun <T> route(
        capability: String,
        operation: suspend (AIService) -> T
    ): T {
        // 将字符串映射到对应的AICapability检查
        val capabilityCheck = when (capability) {
            "basicChat" -> { it: AICapability -> it.supportBasicChat }
            "fileProcessing" -> { it: AICapability -> it.supportFileProcessing }
            "mindMap" -> { it: AICapability -> it.supportMindMapGeneration }
            // 其他字符串映射...
            else -> throw AIServiceException("不支持的功能：$capability")
        }
        
        return routeByCapability(capabilityCheck, operation)
    }
}
```

## Repository 层集成 AIServiceRouter

### Repository 初始化原则

**核心原则**：Repository 层内部集成 AIServiceRouter，对 ViewModel 和其他外部组件透明。

### AIServiceRegistry 单例模式

```kotlin
// AIServiceRegistry 使用单例模式，全局共享
object AIServiceRegistry {
    private val services = ConcurrentHashMap<String, AIService>()

    fun registerAiService(service: AIService) {
        services[service.config.id] = service
    }

    fun getService(modelId: String): AIService? = services[modelId]

    fun getAllServices(): List<AIService> = services.values.toList()
}
```

### AIServiceRouter 单例模式

```kotlin
// AIServiceRouter 使用单例模式，依赖 AIServiceRegistry
object AIServiceRouter {
    private val serviceRegistry = AIServiceRegistry

    // 智能路由方法（如前面定义的 routeBasicChat, routeMindMapGeneration 等）
    suspend fun routeBasicChat(operation: suspend (AIService) -> String): String {
        // 实现逻辑...
    }

    // 其他路由方法...
}
```

### Repository 初始化示例

```kotlin
// MessageRepository - 内部集成 AIServiceRouter（不通过构造函数注入）
class MessageRepository(
    private val messageDao: MessageDao
    // 注意：AIServiceRouter 不作为构造函数参数，Repository 内部直接使用单例
) {

    // Repository 内部直接使用 AIServiceRouter 单例
    private val aiRouter: AIServiceRouter
        get() = AIServiceRouter

    // Local-First：先从数据库读取，然后异步调用 AI
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

        // 3. 使用 AIServiceRouter 调用 AI 服务（内部实现）
        val aiResponse = aiRouter.routeBasicChat { service ->
            service.sendMessage(conversationHistory, userContent)
        }

        // 4. 保存 AI 回复到本地数据库
        val aiMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            content = aiResponse,
            senderType = "AI",
            messageType = "TEXT",
            timestamp = System.currentTimeMillis(),
            aiProcessInfo = null // TODO: 添加 AIProcessInfo
        )
        messageDao.insertMessage(aiMessage)

        return aiMessage
    }
}
```

### ViewModel 只与 Repository 交互

```kotlin
// ChatViewModel - 不感知 AI 服务细节
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    // ViewModel 只与 Repository 交互，完全不感知 AI 服务
    private val messageRepository = MessageRepository(
        messageDao = database.messageDao()
        // AIServiceRouter 是 Repository 内部的单例，对 ViewModel 完全透明
    )

    fun sendMessage(topicId: String, userContent: String) {
        viewModelScope.launch {
            val aiMessage = messageRepository.sendMessageAndGetReply(topicId, userContent)
            // ViewModel 不知道 AI 服务的存在，只调用 Repository 的高层方法
        }
    }
}
```

### 应用启动时初始化 AI 服务

```kotlin
// AIServiceInitializer - 在 Application 中初始化
class AIServiceInitializer {
    companion object {
        fun initialize(context: Context) {
            // 注册所有 AI 服务
            AIServiceRegistry.registerAiService(AIServiceDeepSeek(context))
            AIServiceRegistry.registerAiService(AIServiceMiniMax(context))
            AIServiceRegistry.registerAiService(AIServiceTencentHunyuan(context))
            AIServiceRegistry.registerAiService(AIServiceBaichuan(context))
            // 其他服务...
        }
    }
}

// 在 Application 中调用
class TeachItBackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AIServiceInitializer.initialize(this)
    }
}
```

**关键优势**：
1. **MVVM 架构清晰**：ViewModel 不感知 AI 服务细节
2. **Local-First 策略**：Repository 先从数据库读取，异步调用 AI
3. **依赖注入简化**：使用单例模式，避免复杂的依赖注入
4. **测试友好**：Repository 可以通过构造函数注入 mock DAO 进行测试

**测试策略**：
- Repository 测试时，只需 mock MessageDao/MindMapDao，无需注入 AIServiceRouter
- AIServiceRouter 单例可以通过以下方式测试：
  - 使用真实单例进行集成测试
  - 通过反射替换为 mock（高级测试技巧）
  - 或者使用依赖注入框架支持单例替换

## 统一配置界面重构

### 统一配置界面设计

**统一配置文件列表**：
- **主设置页面**：使用现有的 `fragment_settings.xml`（RecyclerView支持异构列表项）
- `fragment_setting_ai_service_detail.xml` - 统一详细配置页面（Fragment命名规范）
- `item_setting_ai_service.xml` - 统一列表项布局（列表项命名规范）

### 动态UI生成
```kotlin
// 根据AIServiceConfig.requiredFields动态生成配置界面
class AIServiceDetailFragment : Fragment() {
    
    private fun generateConfigUI(config: AIServiceConfig) {
        val container = binding.configContainer
        container.removeAllViews()
        
        // 动态生成配置项
        config.requiredFields.forEach { field ->
            when (field) {
                "apiKey" -> addApiKeyInput(config)
                "secretId" -> addSecretIdInput(config)
                "baseUrl" -> addBaseUrlInput(config)
                "model" -> addModelSelector(config)
                // 其他字段...
            }
        }
        
        // 动态显示支持的功能列表
        addCapabilityList(config)
    }
    
    private fun addApiKeyInput(config: AIServiceConfig) {
        val editText = EditText(requireContext()).apply {
            hint = "输入 ${config.displayName} API Key"
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            // 其他样式配置...
        }
        binding.configContainer.addView(editText)
    }
}
```

### 统一配置文件示例

**fragment_setting_ai_service_detail.xml**：
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F5F5F5">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- 动态标题 -->
            <TextView
                android:id="@+id/service_title"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=""
                android:textSize="20sp"
                android:textColor="@android:color/black"
                android:layout_marginBottom="24dp"
                android:textStyle="bold" />

            <!-- 动态配置项容器 -->
            <LinearLayout
                android:id="@+id/config_container"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical" />

            <!-- 支持的功能列表 -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="支持的功能"
                android:textSize="16sp"
                android:textColor="@android:color/black"
                android:layout_marginTop="24dp"
                android:layout_marginBottom="8dp" />

            <LinearLayout
                android:id="@+id/capability_list"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical" />

            <Button
                android:id="@+id/save_button"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="保存配置"
                android:textColor="@android:color/white"
                android:background="#07C160"
                android:padding="12dp"
                android:layout_marginTop="24dp" />

        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

### AI服务处理历史记录

### 简化的AI服务处理信息设计

为了记录每个AI服务的处理历史，但避免过于复杂的字段，我们采用简化的设计：

```kotlin
// AI服务处理信息数据类
@Parcelize
data class AIProcessInfo(
    val serviceId: String,          // AI服务ID（必填）
    val serviceName: String,        // AI服务显示名称（必填）
    val modelUsed: String? = null,  // 使用的模型（可选，API可能不返回）
    val processingTime: Long? = null // 处理耗时（毫秒，可选，便于性能监控）
)

// 简化的消息实体（保持现有结构，只添加aiProcessInfo）
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val content: String,
    val senderType: String, // USER or AI
    val messageType: String, // TEXT, AUDIO, FILE_CONTENT
    val timestamp: Long = System.currentTimeMillis(),
    
    // 简化的AI服务处理信息
    val aiProcessInfo: AIProcessInfo? = null // 只有AI回复才需要记录
)

// MindMap数据结构类 - 通过parentId表示层级关系，无需MindMapEdge
@Parcelize
data class MindMap(
    val nodes: List<MindMapNode>,          // 所有节点
    val rootNodeId: String? = null,        // 根节点ID
    val metadata: Map<String, String>? = null // 元数据
)

// 简化的思维导图实体
@Entity(tableName = "mindmaps")
data class MindMapEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val title: String,
    val mindMap: MindMap,                  // MindMap数据结构对象
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // 简化的AI服务处理信息
    val aiProcessInfo: AIProcessInfo? = null // 只有AI生成的思维导图才需要记录
)
```

### 简化的处理历史记录机制

直接在`AIService`实现中返回处理信息，由Repository负责记录：

```kotlin
interface AIService {
    // ... 现有方法 ...
    
    // 基础对话功能（返回处理信息）
    suspend fun sendMessage(
        messages: List<MessageEntity>, 
        context: String
    ): AIServiceResponse
}

// AI服务响应数据类
@Parcelize
data class AIServiceResponse(
    val content: String,               // AI回复内容
    val processInfo: AIProcessInfo    // 处理信息
)

// Repository层负责记录处理历史
class MessageRepository(private val messageDao: MessageDao) {
    
    suspend fun sendMessageAndGetReply(
        topicId: String,
        userContent: String,
        aiService: AIService
    ): MessageEntity? {
        // 1. 保存用户消息
        val userMessage = MessageEntity(
            topicId = topicId,
            content = userContent,
            senderType = "USER",
            messageType = "TEXT"
        )
        messageDao.insertMessage(userMessage)
        
        // 2. 获取对话历史
        val conversationHistory = messageDao.getMessagesByTopicSync(topicId)
        
        // 3. 调用AI服务
        val aiResponse = aiService.sendMessage(conversationHistory, "")
        
        // 4. 保存AI回复（包含处理信息）
        val aiMessage = MessageEntity(
            topicId = topicId,
            content = aiResponse.content,
            senderType = "AI",
            messageType = "TEXT",
            aiProcessInfo = aiResponse.processInfo
        )
        messageDao.insertMessage(aiMessage)
        
        return aiMessage
    }
}
```

### 统计功能直接在Repository层实现

```kotlin
class AIServiceRepository(private val messageDao: MessageDao, private val mindMapDao: MindMapDao) {
    
    // 统计AI服务使用情况
    suspend fun getServiceUsageStatistics(aiServiceId: String): ServiceUsageStats {
        val messages = messageDao.getMessagesByService(aiServiceId)
        val mindMaps = mindMapDao.getMindMapsByService(aiServiceId)
        
        return ServiceUsageStats(
            totalMessages = messages.size,
            totalMindMaps = mindMaps.size,
            totalTokens = messages.sumOf { it.tokensUsed } + mindMaps.sumOf { it.tokensUsed },
            totalCost = messages.sumOf { it.costIncurred } + mindMaps.sumOf { it.costIncurred },
            lastUsed = maxOf(
                messages.maxOfOrNull { it.timestamp } ?: 0,
                mindMaps.maxOfOrNull { it.createdAt } ?: 0
            )
        )
    }
}

data class ServiceUsageStats(
    val totalMessages: Int,
    val totalMindMaps: Int,
    val totalTokens: Int,
    val totalCost: Double,
    val lastUsed: Long
)
```

### 腾讯云混元大模型AI服务实现

### 腾讯云混元AI服务配置

```kotlin
// 腾讯云混元大模型AIService实现
class TencentHunyuanAIService : AIService {
    
    override val config = AIServiceConfig.TencentHunyuanConfig(
        secretId = "", // 腾讯云特有的SecretId
        apiKey = "", // 用户配置
        
        // 使用AICapability的流畅API简化配置
        capabilities = AICapability.EMPTY
            .supportBasicChat(true)
            .supportFileProcessing(true)
            .supportMindMapGeneration(true)
            .supportLearningAnalysis(true)
            .supportSocraticQuestioning(true)
            .supportAnswerEvaluation(true)
            .supportDocumentParsing(true)
            .supportConceptExtraction(true)
            .supportKnowledgeGraph(true)
            .supportLongText(true)
            .supportMultimodal(true)
            .supportEducation(true)
            .supportCodeGeneration(true)
            .supportMath(true)
            .supportCreativeWriting(true)
            .supportImageAnalysis(true)
            .supportImageGeneration(true)
            .supportAudioProcessing(false)  // 混元暂不支持音频处理
            .supportVideoAnalysis(false)    // 混元暂不支持视频分析
    )
    
    private val apiClient: TencentCloudClient
    
    override suspend fun sendMessage(messages: List<MessageEntity>, context: String): String {
        // 腾讯云混元API调用实现
        val request = ChatCompletionsRequest().apply {
            this.messages = messages.map { msg ->
                ChatMessage().apply {
                    role = if (msg.role == MessageRole.USER) "user" else "assistant"
                    content = msg.content
                }
            }
            this.model = "hunyuan-lite" // 可根据配置选择不同版本
            this.stream = false
        }
        
        val response = apiClient.chatCompletions(request)
        return response.choices.first().message.content
    }
    
    override suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity? {
        // 使用腾讯云混元的文档解析能力生成思维导图
        val prompt = """
            基于学习主题：$topicId，学习目标：$learningGoal
            请生成一个结构化的思维导图，包含核心概念、知识点关联和学习路径
            输出格式要求：JSON格式的思维导图数据结构
        """.trimIndent()
        
        val response = sendMessage(listOf(MessageEntity(content = prompt, role = MessageRole.USER)), "mindmap")
        
        // 解析响应并创建MindMapEntity
        return parseMindMapFromResponse(response, topicId, learningGoal)
    }
    
    override suspend fun parseDocument(fileContent: String, fileType: String): DocumentAnalysis {
        // 使用腾讯云知识引擎的文档解析API
        val request = ReconstructDocumentRequest().apply {
            this.content = fileContent
            this.type = fileType
        }
        
        val response = apiClient.reconstructDocument(request)
        return DocumentAnalysis(
            title = response.title ?: "",
            summary = response.summary ?: "",
            keyPoints = response.keyPoints ?: emptyList(),
            concepts = response.concepts?.map { Concept(it.name, it.description) } ?: emptyList()
        )
    }
    
    // 腾讯云特有的API调用方法
    private suspend fun generateImage(prompt: String, style: String): ByteArray? {
        val request = ImageGenerationRequest().apply {
            this.prompt = prompt
            this.style = style
            this.size = "1024x1024"
        }
        
        val response = apiClient.generateImage(request)
        return response.imageData
    }
    
    override suspend fun checkStatus(): ServiceStatus {
        return try {
            // 测试API连接
            apiClient.checkStatus()
            ServiceStatus.AVAILABLE
        } catch (e: AuthenticationException) {
            ServiceStatus.UNAUTHORIZED
        } catch (e: RateLimitException) {
            ServiceStatus.RATE_LIMITED
        } catch (e: InsufficientBalanceException) {
            ServiceStatus.INSUFFICIENT_BALANCE
        } catch (e: Exception) {
            ServiceStatus.UNAVAILABLE
        }
    }
}

// 腾讯云API客户端封装
class TencentCloudClient(
    private val secretId: String,
    private val secretKey: String,
    private val endpoint: String
) {
    
    suspend fun chatCompletions(request: ChatCompletionsRequest): ChatCompletionsResponse {
        // 实现腾讯云API调用，包括签名认证
        return makeSignedRequest("ChatCompletions", request)
    }
    
    suspend fun reconstructDocument(request: ReconstructDocumentRequest): ReconstructDocumentResponse {
        return makeSignedRequest("ReconstructDocument", request)
    }
    
    suspend fun generateImage(request: ImageGenerationRequest): ImageGenerationResponse {
        return makeSignedRequest("GenerateImage", request)
    }
    
    private suspend fun <T> makeSignedRequest(action: String, request: Any): T {
        // 实现腾讯云签名算法和API调用
        // 包括Timestamp、Nonce、Signature等参数
        // ...
    }
}
```

### 腾讯云混元AI服务注册

```kotlin
// 在应用启动时注册腾讯云混元AI服务
class AIServiceInitializer {
    
    fun initializeServices(registry: AIServiceRegistry) {
        // 注册腾讯云混元AI服务
        val tencentService = TencentHunyuanAIService()
        registry.registerAiService(tencentService)
        
        // 其他AI服务注册...
    }
}
```

## 简化的AI服务配置界面设计

### 统一配置界面方法

**设计理念**：使用`required-fields`控制显示，避免专门的UI生成函数

```kotlin
// AI服务配置密封类 - 使用AICapability替代独立的布尔属性
sealed class AIServiceConfig {
    abstract val id: String
    abstract val name: String
    abstract val displayName: String
    abstract val description: String
    
    // 配置需求 - 使用AIServiceRequiredFields统一管理
    abstract val requiredFields: AIServiceRequiredFields
    
    // 配置值
    abstract val secretId: String
    abstract val apiKey: String
    abstract val baseUrl: String
    abstract val region: String
    abstract val model: String
    
    // AI能力配置 - 使用AIServiceCapability统一管理
    abstract val capabilities: AIServiceCapability
    
    // 腾讯云混元配置 - 支持所有功能
data class TencentHunyuanConfig(
    override val id: String = "tencent-hunyuan",
    override val name: String = "Tencent Hunyuan",
    override val displayName: String = "腾讯云混元",
    override val description: String = "腾讯云混元大模型",
    override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.CLOUD_SERVICE,
    override val secretId: String = "",
    override val apiKey: String = "",
    override val baseUrl: String = "https://hunyuan.tencent.com",
    override val region: String = "ap-guangzhou",
    override val model: String = "hunyuan-lite",
    // 使用AICapability统一管理能力配置
    override val capabilities: AICapability = AICapability.FULL_CAPABILITIES
        .supportAudioProcessing(false)
        .supportVideoAnalysis(false)
) : AIServiceConfig()

// 百川配置 - 只支持API Key和基础URL
data class BaichuanConfig(
    override val id: String = "baichuan",
    override val name: String = "Baichuan",
    override val displayName: String = "百川大模型",
    override val description: String = "百川智能大模型",
    override val requiredFields: AIServiceRequiredFields = AIServiceRequiredFields.API_AND_URL,
    override val secretId: String = "",
    override val apiKey: String = "",
    override val baseUrl: String = "https://api.baichuan-ai.com",
    override val region: String = "",
    override val model: String = "Baichuan2-Turbo",
    // 使用AICapability配置基础对话能力
    override val capabilities: AICapability = AICapability.BASIC_CHAT
) : AIServiceConfig()

// 腾讯云混元配置示例
val HUNYUAN_CONFIG = AIServiceConfig.TencentHunyuanConfig()
```

### 统一的配置界面方法

```kotlin
/**
 * 设置AI服务配置界面
 * 根据AIServiceRequiredFields动态显示/隐藏字段
 */
fun setupAIServiceConfigUI(binding: SettingItemHunyuanDetailBinding, config: AIServiceConfig) {
    // 设置标题
    binding.titleText.text = "${config.displayName} 配置"
    
    // 根据AIServiceRequiredFields显示/隐藏字段 - 类型安全且清晰！
    binding.secretIdField.visibility = if (config.requiredFields.requireSecretId) View.VISIBLE else View.GONE
    binding.apiKeyField.visibility = if (config.requiredFields.requireApiKey) View.VISIBLE else View.GONE
    binding.baseUrlField.visibility = if (config.requiredFields.requireBaseUrl) View.VISIBLE else View.GONE
    binding.regionField.visibility = if (config.requiredFields.requireRegion) View.VISIBLE else View.GONE
    binding.modelField.visibility = if (config.requiredFields.requireModel) View.VISIBLE else View.GONE
    
    // 设置字段标签和默认值
    setFieldLabels(binding, config)
    setDefaultValues(binding, config)
    
    // 设置下拉选择器
    setupSpinners(binding, config)
}
```



### 通用AI服务配置界面实现

```kotlin
// 通用AI服务配置Fragment - 处理所有AI服务类型
class AIServiceSettingDetailFragment : Fragment() {
    
    private lateinit var binding: SettingItemHunyuanDetailBinding
    private lateinit var currentConfig: AIServiceConfig
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 从参数获取当前配置类型
        val configType = arguments?.getString("config_type") ?: "tencent-hunyuan"
        currentConfig = when (configType) {
            "tencent-hunyuan" -> AIServiceConfig.TencentHunyuanConfig()
            "baichuan" -> AIServiceConfig.BaichuanConfig()
            "openai" -> AIServiceConfig.OpenAIConfig()
            else -> AIServiceConfig.TencentHunyuanConfig()
        }
        
        // 一行代码搞定所有配置界面设置
        AIServiceConfigHelper.setupAIServiceConfigUI(binding, currentConfig)
        
        // 设置保存按钮点击事件
        setupSaveButton()
    }
    
    /**
     * 保存配置
     */
    private fun saveConfig() {
        // 获取配置数据
        val updatedConfig = AIServiceConfigHelper.getConfigData(binding, currentConfig)
        
        // 保存到数据库
        saveToDatabase(updatedConfig)
        
        // 显示成功消息
        showSuccess("配置保存成功")
    }
}
```

### 统一配置布局文件

```xml
<!-- fragment_item_setting_ai_service_detail.xml - 通用AI服务配置布局 -->
<LinearLayout>
    <!-- 动态标题 -->
    <TextView android:id="@+id/title_text" />
    
    <!-- Secret ID 字段 -->
    <LinearLayout android:id="@+id/secret_id_field">
        <TextView android:text="Secret ID" />
        <EditText android:id="@+id/secret_id_input" />
    </LinearLayout>
    
    <!-- API Key 字段 -->
    <LinearLayout android:id="@+id/api_key_field">
        <TextView android:text="API Key" />
        <EditText android:id="@+id/api_key_input" />
    </LinearLayout>
    
    <!-- 基础 URL 字段 -->
    <LinearLayout android:id="@+id/base_url_field">
        <TextView android:text="基础 URL" />
        <EditText android:id="@+id/base_url_input" />
    </LinearLayout>
    
    <!-- 区域选择器 -->
    <LinearLayout android:id="@+id/region_field">
        <TextView android:text="区域" />
        <Spinner android:id="@+id/region_spinner" />
    </LinearLayout>
    
    <!-- 模型选择器 -->
    <LinearLayout android:id="@+id/model_field">
        <TextView android:text="模型选择" />
        <Spinner android:id="@+id/model_spinner" />
    </LinearLayout>
</LinearLayout>
```

### 优势总结

1. **完整的处理历史**：记录每个AI服务的处理详情
2. **成本追踪**：精确统计每个服务的token消耗和费用
3. **性能监控**：记录处理时间，便于优化和故障排查
4. **服务质量评估**：基于处理成功率评估AI服务质量
5. **用户透明度**：用户可以看到每次交互使用的AI服务信息

这样的设计为用户提供了完整的AI服务使用历史，便于分析、优化和成本控制。

## 动态智能路由设计

### 基于配置的路由策略
路由器不再依赖硬编码的能力表，而是根据每个AI服务的配置项动态生成路由策略：

```kotlin
// 路由器根据注册的服务配置自动生成优先级
val router = AIServiceRouter(serviceRegistry)
val capabilityPriority = router.getCapabilityPriority()

// 例如：basicChat 的优先级会自动根据支持该功能且成本最低的服务生成
println(capabilityPriority["basicChat"]) // 输出: ["deepseek", "doubao", "baidu", ...]
```

### 动态路由算法
1. **配置驱动**：基于每个AI服务的布尔配置字段自动判断支持的功能
2. **成本优化**：根据`pricePerMillion`自动排序，选择成本最低的服务
3. **实时更新**：当服务配置变化时，路由策略自动更新
4. **智能匹配**：根据具体功能需求动态选择最合适的服务

## 12大模型能力配置表示例

| 模型 | 基础对话 | 文件处理 | 思维导图 | 学习分析 | 长文本 | 教育专用 | 多模态 | 代码生成 | 数学计算 | **RAG支持** |
|------|----------|----------|----------|----------|--------|----------|--------|----------|----------|-------------|
| DeepSeek | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| 腾讯云 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| 百度文心 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| 阿里通义 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| OpenAI | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ | ❌ |
| 豆包 | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Kimi | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 智谱AI | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Minimax | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| 混元 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **零一万物** | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | **✅** |
| 阶跃星辰 | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

## 路由决策示例

### 场景1：基础对话
- **需求**：`supportBasicChat = true`
- **候选**：所有12个模型
- **选择策略**：DeepSeek（成本低）→ 豆包（免费额度）→ 其他模型

### 场景2：教育专用功能
- **需求**：`supportEducation = true` 且 `supportMindMapGeneration = true`
- **候选**：腾讯云、百度文心、阿里通义、混元
- **选择策略**：腾讯云（教育专用）→ 百度文心（中文优化）→ 阿里通义

### 场景3：长文本处理
- **需求**：`supportLongText = true`
- **候选**：DeepSeek、Kimi、百度文心、阿里通义、OpenAI
- **选择策略**：DeepSeek（性价比高）→ Kimi（超长文本）→ 其他模型

## 优势总结

1. **精确匹配**：独立布尔字段提供更精确的能力匹配
2. **灵活扩展**：新增能力只需添加布尔字段，无需修改现有代码
3. **性能优化**：避免字符串集合操作，提高路由效率
4. **类型安全**：编译器检查确保能力配置的正确性
5. **可读性强**：明确的布尔字段比字符串集合更易理解

这个设计完全支持基于AI服务能力的智能动态路由决策！