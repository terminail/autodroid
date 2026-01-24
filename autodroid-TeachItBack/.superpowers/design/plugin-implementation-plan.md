# 插件化AI服务实现计划

## 核心接口定义

### 1. 统一配置数据类
```kotlin
// 定义在：app/src/main/java/com/autodroid/teachitback/plugin/AIServiceConfig.kt
data class AIServiceConfig(
    val modelId: String,                    // 模型唯一标识："deepseek", "tencent", "baidu"等
    val displayName: String,               // 显示名称："DeepSeek", "腾讯云", "百度文心一言"
    val apiKey: String = "",                // API密钥（大部分模型需要）
    val secretId: String = "",              // 密钥ID（部分模型需要）
    val baseUrl: String = "",              // 基础URL（代码中写死，不显示给用户）
    val freeQuota: Long = 0L,               // 免费额度（tokens）
    val pricePerMillion: Double = 0.0,      // 百万token价格
    val requiredFields: Set<String> = setOf(), // 需要用户配置的字段
    
    // 能力支持字段（替代原来的capabilities Set）
    val supportBasicChat: Boolean = false,           // 基础对话
    val supportFileProcessing: Boolean = false,      // 文件处理
    val supportMindMapGeneration: Boolean = false,   // 思维导图生成
    val supportLearningAnalysis: Boolean = false,    // 学习分析
    val supportSocraticQuestioning: Boolean = false, // 苏格拉底式提问
    val supportAnswerEvaluation: Boolean = false,    // 答案评估
    val supportDocumentParsing: Boolean = false,     // 文档解析
    val supportConceptExtraction: Boolean = false,   // 概念提取
    val supportKnowledgeGraph: Boolean = false,      // 知识图谱
    
    // 特殊能力支持
    val supportLongText: Boolean = false,            // 长文本处理
    val supportMultimodal: Boolean = false,          // 多模态
    val supportEducation: Boolean = false,           // 教育专用
    val supportCodeGeneration: Boolean = false,      // 代码生成
    val supportMath: Boolean = false,                // 数学计算
    val supportCreativeWriting: Boolean = false      // 创意写作
)
```

### 2. 插件接口定义
```kotlin
// 定义在：app/src/main/java/com/autodroid/teachitback/plugin/AIServicePlugin.kt
interface AIServicePlugin {
    val config: AIServiceConfig
    val isAvailable: Boolean
    val remainingQuota: Long
    
    // 基础功能
    suspend fun sendMessage(messages: List<MessageEntity>, context: String): String
    suspend fun processFileContent(content: String, context: String): String
    
    // 教育专用功能（可选实现）
    suspend fun generateMindMap(topicId: String, learningGoal: String): MindMapEntity?
    suspend fun analyzeLearningProgress(conversationHistory: List<MessageEntity>): ProgressAnalysis
    suspend fun generateSocraticQuestions(topic: String, currentLevel: Int): List<String>
    suspend fun evaluateAnswer(userAnswer: String, correctAnswer: String): AnswerEvaluation
    
    // 状态检测
    suspend fun checkStatus(): ServiceStatus
}

enum class ServiceStatus {
    AVAILABLE,              // 服务可用
    INSUFFICIENT_BALANCE,   // 余额不足（如DeepSeek返回402）
    RATE_LIMITED,           // 频率限制
    UNAUTHORIZED,           // 认证失败
    UNAVAILABLE            // 服务不可用
}

// 特定异常类用于路由策略
class InsufficientBalanceException(message: String) : Exception(message)
class RateLimitException(message: String) : Exception(message)
```

## 插件管理器实现

### 3. 插件注册表
```kotlin
// 定义在：app/src/main/java/com/autodroid/teachitback/plugin/PluginRegistry.kt
class PluginRegistry {
    private val plugins = mutableMapOf<String, AIServicePlugin>()
    
    fun registerPlugin(plugin: AIServicePlugin) {
        plugins[plugin.config.modelId] = plugin
    }
    
    fun getAvailablePlugins(capability: String): List<AIServicePlugin> {
        return plugins.values.filter { plugin ->
            plugin.isAvailable && plugin.supportsCapability(capability)
        }
    }
    
    // 根据能力字符串匹配对应的布尔字段
    private fun AIServicePlugin.supportsCapability(capability: String): Boolean {
        return when (capability) {
            "sendMessage", "basicChat" -> config.supportBasicChat
            "processFileContent", "fileProcessing" -> config.supportFileProcessing
            "generateMindMap", "mindMap" -> config.supportMindMapGeneration
            "analyzeLearningProgress", "learningAnalysis" -> config.supportLearningAnalysis
            "generateSocraticQuestions", "socraticQuestioning" -> config.supportSocraticQuestioning
            "evaluateAnswer", "answerEvaluation" -> config.supportAnswerEvaluation
            "parseDocument", "documentParsing" -> config.supportDocumentParsing
            "extractKeyConcepts", "conceptExtraction" -> config.supportConceptExtraction
            "buildKnowledgeGraph", "knowledgeGraph" -> config.supportKnowledgeGraph
            "longText" -> config.supportLongText
            "multimodal" -> config.supportMultimodal
            "education" -> config.supportEducation
            "codeGeneration" -> config.supportCodeGeneration
            "math" -> config.supportMath
            "creativeWriting" -> config.supportCreativeWriting
            else -> false
        }
    }
    
    fun getAllPlugins(): List<AIServicePlugin> {
        return plugins.values.toList()
    }
    
    fun getPlugin(modelId: String): AIServicePlugin? {
        return plugins[modelId]
    }
}
```

### 4. 智能路由器
```kotlin
// 定义在：app/src/main/java/com/autodroid/teachitback/plugin/AIRouter.kt
class AIRouter(private val pluginRegistry: PluginRegistry) {
    
    // 路由策略：免费额度优先 → 成本优先
    suspend fun <T> route(
        capability: String,
        operation: suspend (AIServicePlugin) -> T
    ): T {
        val availablePlugins = pluginRegistry.getAvailablePlugins(capability)
        
        if (availablePlugins.isEmpty()) {
            throw AIServiceException("没有可用的AI服务支持功能：$capability")
        }
        
        // 1. 优先选择有免费额度的插件
        val pluginsWithQuota = availablePlugins.filter { it.remainingQuota > 0 }
        if (pluginsWithQuota.isNotEmpty()) {
            return tryRoute(pluginsWithQuota.sortedByDescending { it.remainingQuota }, operation)
        }
        
        // 2. 按成本排序选择（价格从低到高）
        val pluginsByCost = availablePlugins.sortedBy { it.config.pricePerMillion }
        return tryRoute(pluginsByCost, operation)
    }
    
    private suspend fun <T> tryRoute(
        plugins: List<AIServicePlugin>,
        operation: suspend (AIServicePlugin) -> T
    ): T {
        val exceptions = mutableListOf<Exception>()
        
        for (plugin in plugins) {
            try {
                // 检查插件状态
                val status = plugin.checkStatus()
                when (status) {
                    ServiceStatus.AVAILABLE -> return operation(plugin)
                    ServiceStatus.INSUFFICIENT_BALANCE -> 
                        exceptions.add(InsufficientBalanceException("${plugin.config.displayName} 余额不足"))
                    ServiceStatus.RATE_LIMITED -> 
                        exceptions.add(RateLimitException("${plugin.config.displayName} 频率限制"))
                    else -> exceptions.add(Exception("${plugin.config.displayName} 不可用"))
                }
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }
        
        throw AIServiceException("所有可用服务都失败：${exceptions.joinToString { it.message ?: "未知错误" }}")
    }
}
```

## 具体插件实现示例

### 5. DeepSeek插件实现
```kotlin
// 定义在：app/src/main/java/com/autodroid/teachitback/plugin/impl/DeepSeekPlugin.kt
class DeepSeekPlugin(private val apiKey: String) : AIServicePlugin {
    
    override val config = AIServiceConfig(
        modelId = "deepseek",
        displayName = "DeepSeek",
        freeQuota = 500000L, // 50万tokens
        pricePerMillion = 0.6,
        capabilities = setOf("sendMessage", "processFileContent", "parseDocument"),
        requiredFields = setOf("apiKey") // 只需要配置apiKey
    )
    
    override val isAvailable: Boolean get() = apiKey.isNotBlank()
    override var remainingQuota: Long = 500000L // 初始免费额度
    
    override suspend fun sendMessage(messages: List<MessageEntity>, context: String): String {
        // 实现DeepSeek API调用
        // 检测402状态码并抛出InsufficientBalanceException
        return "DeepSeek回复内容"
    }
    
    override suspend fun checkStatus(): ServiceStatus {
        // 调用DeepSeek状态检查API
        // 根据HTTP状态码返回相应状态
        return ServiceStatus.AVAILABLE
    }
    
    // 其他方法实现...
}
```

## 设置界面重构

### 6. 统一设置界面
- **主设置页面**：显示所有已注册插件的卡片
- **详细配置页面**：根据`requiredFields`动态生成配置表单
- **状态显示**：实时显示插件状态、剩余额度、支持功能

### 7. SettingsViewModel重构
```kotlin
class SettingsViewModel : ViewModel() {
    private val pluginRegistry = PluginRegistry()
    
    val aiPlugins: StateFlow<List<AIServicePlugin>> = 
        pluginRegistry.getAllPlugins().asStateFlow()
    
    fun updatePluginConfig(modelId: String, newConfig: AIServiceConfig) {
        // 更新插件配置
        // 触发重新注册插件
    }
}
```

## 实施路线图

### Phase 1: 基础框架（1-2周）
- [ ] 定义核心接口和数据类
- [ ] 实现PluginRegistry和AIRouter
- [ ] 重构SettingsFragment和SettingsViewModel

### Phase 2: DeepSeek插件（1周）
- [ ] 实现DeepSeekPlugin
- [ ] 集成到ChatViewModel
- [ ] 测试基础会话功能

### Phase 3: 腾讯云插件（2周）
- [ ] 实现TencentPlugin（支持教育功能）
- [ ] 测试混合路由策略
- [ ] 优化设置界面

### Phase 4: 扩展插件（后续）
- [ ] 实现其他10+国内大模型插件
- [ ] 完善错误处理和状态监控
- [ ] 性能优化和用户体验改进

这个实现方案完全符合您的需求，支持渐进式开发和插件化扩展！