# 插件化AI服务架构 - Brainstorming设计文档

## 项目概述

**项目类型**：Android应用  
**项目根目录**：`d:/git/autodroid/autodroid-TeachItBack`  
**设计阶段**：Brainstorming  
**时间戳**：2026-01-23T00:00:00

## 核心需求

设计一个插件化的AI服务架构，支持：
- 多AI模型的动态集成和注册
- 统一的配置界面管理
- 智能路由策略和成本优化
- 类型安全的能力配置管理

## 技术架构决策

### 架构模式
```
MVVM + Repository Pattern + Plugin Architecture

UI Layer (Fragment/Activity)
    ↓
ViewModel Layer (AIModelManager + SettingsViewModel)
    ↓
Repository Layer (AIServiceRouter + AIServiceRegistry)
    ↓
AIService Layer (Plugin AIServices x10+)
    ↓
API Layer (各模型API)
```

### 统一配置系统

**问题识别**：
- 当前13个AI服务 × 2个配置文件 = 26个重复布局文件
- 每个文件结构高度相似，只有标题和提示文本不同

**解决方案**：
- 动态配置界面替代所有独立配置文件
- 统一配置数据类（AIServiceConfig密封类）
- 类型安全的配置需求管理（AIServiceRequiredFields）

### AI能力管理策略

**核心设计**：类型安全的布尔属性替代字符串集合

**关键组件**：
1. **AIServiceCapability** - 26个能力布尔属性
2. **AIServiceRequiredFields** - 配置需求管理  
3. **Fluent API** - 流畅配置语法

### 智能路由策略

**路由算法**：基于能力检查 + 成本优化 + 配额优先

**优先级顺序**：
1. 有免费额度的服务（按剩余额度排序）
2. 成本最低的服务（按价格排序）
3. 其他可用服务

**特性**：
- 类型安全路由函数
- 容错重试机制
- 实时服务状态检测

### 统一Prompt管理策略

**核心原则**：同一个AI功能的所有服务使用完全相同的Prompt模板，确保输出一致性

**问题识别**：
- 当前代码库中，不同AI服务的prompt各不相同（如DeepSeek和OpenAI的`processFileContent` prompt完全不同）
- 导致用户体验不一致，维护成本高
- 无法公平比较不同AI模型对相同任务的表现

**解决方案**：
1. **创建PromptTemplates单例对象**：统一管理所有AI功能的prompt模板
2. **Prompt模板标准化**：每个功能（如processFileContent、generateMindMap等）有唯一的、精心优化的prompt
3. **所有AI服务共享**：无论使用DeepSeek、OpenAI、腾讯云混元还是其他模型，都使用相同的prompt
4. **适配不同模型**：支持system prompt模式（OpenAI）和单条消息模式（DeepSeek），但核心指令内容一致

**设计优势**：
- **用户体验一致**：不同AI服务输出格式和风格统一
- **维护成本低**：修改prompt只需改一处
- **A/B测试公平**：可以准确比较不同模型对相同prompt的响应质量
- **最佳实践传播**：将写得最好的prompt应用到所有服务

**实现方式**：
```kotlin
object PromptTemplates {
    fun processFileContent(content: String, context: String): String
    fun generateMindMap(topicId: String, learningGoal: String): String
    fun analyzeLearningProgress(conversationText: String): String
    // ... 其他功能prompt
}
```

**改造步骤**：
1. 创建`PromptTemplates.kt`文件（新增任务）
2. 选择最佳prompt版本（推荐使用OpenAI的详细版本）
3. 改造所有AI服务实现，使用PromptTemplates替代硬编码prompt

## 核心组件设计

### AIServiceConfig（密封类/数据类）

**目的**：统一AI服务配置管理

**字段结构**：
```kotlin
modelId: String                    // 模型唯一标识
displayName: String               // 显示名称
apiKey/secretId/baseUrl: String   // 认证配置
freeQuota/pricePerMillion: 成本管理
requiredFields: AIServiceRequiredFields  // 配置需求
capabilities: AIServiceCapability  // AI能力配置
```

### AIServiceCapability（能力管理类）

**特性**：
- 26个明确的布尔属性
- Fluent API支持链式配置
- 类型安全的枚举式访问

**能力列表**：
- 基础对话：`supportBasicChat`
- 文件处理：`supportFileProcessing`
- 思维导图：`supportMindMapGeneration`
- 学习分析：`supportLearningAnalysis`
- 苏格拉底提问：`supportSocraticQuestioning`
- 答案评估：`supportAnswerEvaluation`
- 文档解析：`supportDocumentParsing`
- 概念提取：`supportConceptExtraction`
- 知识图谱：`supportKnowledgeGraph`
- 长文本：`supportLongText`
- 多模态：`supportMultimodal`
- 教育专用：`supportEducation`
- 代码生成：`supportCodeGeneration`
- 数学计算：`supportMath`
- 创意写作：`supportCreativeWriting`
- 图像分析：`supportImageAnalysis`
- 图像生成：`supportImageGeneration`
- 音频处理：`supportAudioProcessing`
- 视频分析：`supportVideoAnalysis`
- RAG支持：`supportRAG`

### AIServiceRequiredFields（配置需求管理）

**预定义模板**：
- `NO_REQUIRED_FIELDS` - 无配置需求
- `API_KEY_ONLY` - 仅需要API Key
- `API_AND_URL` - 需要API Key和Base URL
- `CLOUD_SERVICE` - 完整云服务配置

### 服务注册与路由

**AIServiceRegistry**：
- 动态插件注册机制
- 能力发现和匹配
- 服务可用性检查

**AIServiceRouter**：
- 智能路由决策引擎
- 成本优化算法
- 容错重试策略

## 实现策略

### 第一阶段：核心架构实现（1-2周）

**交付物**：
- AIServiceConfig数据类定义
- AIServiceCapability能力管理
- AIServiceRequiredFields配置需求
- PromptTemplates统一管理（新增）
- 基础AIService接口

### 第二阶段：插件注册与路由（2-3周）

**交付物**：
- AIServiceRegistry实现
- AIServiceRouter路由引擎
- 基础AI服务插件（DeepSeek、腾讯云混元）

### 第三阶段：统一配置界面（2-3周）

**交付物**：
- 动态配置界面UI
- 配置数据持久化
- 配置验证和测试

### 第四阶段：高级功能集成（2-3周）

**交付物**：
- 多AI服务完整集成
- 智能路由优化
- 性能监控和调优

## AI服务模型配置

### DeepSeek模型
- **能力**：基础对话、文件处理、长文本、代码生成、数学计算
- **成本**：低
- **配额**：免费额度充足

### 腾讯云混元模型
- **能力**：基础对话、文件处理、思维导图、学习分析、苏格拉底提问、答案评估、文档解析、概念提取、知识图谱、长文本、教育专用
- **成本**：中等
- **特性**：教育专用能力突出

### 百川模型
- **能力**：基础对话
- **成本**：低
- **特性**：配置简单

## 设计优势

### 类型安全
- 编译器检查确保配置正确性
- 避免运行时字符串匹配错误

### 可扩展性
- 新增能力只需添加布尔字段
- 无需修改现有代码结构

### 性能优化
- 避免字符串集合操作
- 编译器优化的布尔检查

### 可读性
- 明确的布尔属性比字符串集合更易理解
- 自文档化的代码结构

### 可维护性
- 统一配置管理减少重复代码
- 模块化设计便于维护

### Prompt一致性
- 所有AI服务共享相同prompt，用户体验一致
- 维护成本低，修改一处即可影响所有服务
- 支持公平的模型对比测试

## 风险缓解策略

### 复杂性管理
- 模块化设计降低复杂度
- 清晰的接口定义

### 向后兼容性
- 保持接口兼容性
- 渐进式迁移策略

### 错误处理
- 完善的异常处理机制
- 优雅降级策略

### 测试策略
- 单元测试覆盖核心组件
- 集成测试验证插件交互
- 端到端测试确保功能完整

## 状态评估

**当前状态**：ready_for_implementation

**下一步**：基于此brainstorming设计开始详细实现规划

## 关键设计决策总结

1. **架构模式**：MVVM + Repository + Plugin Architecture
2. **配置管理**：统一AIServiceConfig密封类 + 动态配置界面
3. **能力管理**：类型安全的AIServiceCapability（26个布尔属性）
4. **路由策略**：智能路由（能力+成本+配额）
5. **Prompt管理**：统一PromptTemplates，所有服务共享相同prompt
6. **数据流**：Local-First策略，Repository内部集成AIServiceRouter
7. **架构原则**：ViewModel不感知AI服务，Repository内部使用AIServiceRouter单例