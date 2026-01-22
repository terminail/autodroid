# 腾讯云知识引擎集成实施计划

## 项目信息
- **项目**: Teach It Back - 腾讯云知识引擎集成
- **阶段**: writing-plans
- **时间**: 2026-01-22
- **分支**: feature/teach-it-back-dev
- **设计文档**: brainstorming_tencentcloud_integration_20260122_172000
- **总预估时间**: 66分钟

## 架构设计原则

### Local-First 数据流
```
UI Layer → ViewModel → Repository → DAO → Database (优先返回本地数据)
                             ↓
                       AI Service (异步调用)
                             ↓
                       DAO → Database (保存AI结果到本地)
```

**关键特性**:
1. Repository层优先从本地数据库返回数据，确保即时响应
2. AI服务调用在后台异步执行，不阻塞UI
3. AI结果自动保存到本地数据库，供下次使用
4. 切换AI服务对前端UI无影响

### MVVM分层
- **UI Layer**: Fragment/Activity，只负责显示
- **ViewModel**: 通过Repository协调数据，管理UI状态
- **Repository**: Local-First策略，协调本地数据和AI服务
- **AI Service**: 纯粹的AI服务调用，不涉及业务逻辑
- **DAO/Database**: 本地数据持久化

## 实施阶段概览

### 第一阶段：基础架构搭建（25分钟）
- 创建TencentCloudAIService基础接口
- 创建进度跟踪数据结构
- 扩展Repository层集成AI服务
- 添加腾讯云API配置

### 第二阶段：UI配置和设置（5分钟）
- 添加腾讯云设置项到Settings
- 实现配置UI逻辑

### 第三阶段：核心功能实现（27分钟）
- 文档解析和知识图谱构建
- MindMap生成
- 苏格拉底对话
- 答案评估和进度分析

### 第四阶段：UI集成和交互（19分钟）
- ChatViewModel统一管理ChatItem（包含MindMap）
- ChatFragment支持MindMap显示
- 进度显示组件

### 第五阶段：测试验证（13分钟）
- 单元测试
- 集成测试
- 端到端测试

---

## 详细任务列表

### Task 1: 创建TencentCloudAIService基础接口
**时间**: 4分钟  
**文件**: `app/src/main/java/com/autodroid/teachitback/api/TencentCloudAIService.kt`

**描述**: 定义TencentCloudAIService接口，包含MindMap生成、苏格拉底对话、进度跟踪等核心方法

**验证步骤**:
- 接口定义完整
- 所有方法签名清晰
- 数据类型定义准确

---

### Task 2: 创建进度跟踪数据结构
**时间**: 3分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/model/ProgressAnalysis.kt`

**描述**: 定义ProgressAnalysis、AnswerEvaluation等数据类，支持学习进度评估

**验证步骤**:
- 数据类定义完整
- 枚举类型准确
- 包含所有必要字段

---

### Task 3: 扩展MindMapRepository支持AI服务
**时间**: 5分钟  
**文件**: `app/src/main/java/com/autodroid/teachitback/repository/MindMapRepository.kt`

**描述**: 修改MindMapRepository，集成TencentCloudAIService，实现本地优先的数据协调逻辑

**关键实现**:
- 优先返回本地数据库的MindMap
- 异步调用AI服务生成新MindMap
- 自动保存AI结果到本地数据库

**验证步骤**:
- Local-First策略实现
- 数据保存逻辑正确
- 异步处理无阻塞

---

### Task 4: 扩展MessageRepository支持AI服务
**时间**: 5分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/repository/MessageRepository.kt`

**描述**: 修改MessageRepository，集成TencentCloudAIService，实现消息的本地优先+AI增强

**关键实现**:
- 保存用户消息到本地数据库
- 异步调用AI服务分析进度
- 自动保存AI回复到本地数据库

**验证步骤**:
- Local-First策略实现
- 消息保存逻辑正确
- AI异步调用无阻塞

---

### Task 5: 实现TencentCloudAIService基础框架
**时间**: 5分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/impl/TencentCloudAIServiceImpl.kt`

**描述**: 创建TencentCloudAIServiceImpl实现类，配置API客户端和基础认证，支持测试模式

**关键实现**:
- 根据配置决定使用真实API或Mock数据
- Mock数据作为内部函数，不单独创建类

**验证步骤**:
- 基础框架搭建完成
- API客户端初始化正确
- 重试机制实现
- 测试模式切换正常

---

### Task 6: 添加腾讯云API配置到SettingsEntity
**时间**: 3分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/model/SettingEntity.kt`

**描述**: 扩展SettingsEntity，添加腾讯云API密钥、测试模式等配置选项

**验证步骤**:
- 配置常量定义完整
- 实体类兼容现有设计

---

### Task 7: 添加腾讯云设置项到SettingsItem
**时间**: 4分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/ui/adapter/SettingsItem.kt`

**描述**: 在SettingsItem中添加腾讯云API配置（包含测试模式选项）的UI项

**验证步骤**:
- 新的SettingsItem类型定义正确
- 测试模式选项包含在腾讯云配置中
- 包含必要的回调函数
- 符合现有UI设计

---

### Task 8: 在SettingsFragment中添加腾讯云配置UI
**时间**: 5分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/ui/SettingsFragment.kt`

**描述**: 在SettingsFragment中添加腾讯云API配置（包含测试模式）的UI逻辑

**验证步骤**:
- UI正确显示腾讯云配置
- 测试模式选项正确显示
- 配置保存功能正常
- 切换开关生效

---

### Task 9: 实现文档解析功能
**时间**: 4分钟  
**文件**: `app/src/main/java/com/autodroid/teachitback/impl/TencentCloudAIServiceImpl.kt`

**描述**: 在TencentCloudAIServiceImpl中实现parseDocument方法

**验证步骤**:
- 文档解析API调用正确
- 返回数据结构正确
- 异常处理完善

---

### Task 10: 实现知识图谱构建功能
**时间**: 4分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/impl/TencentCloudAIServiceImpl.kt`

**描述**: 在TencentCloudAIServiceImpl中实现buildKnowledgeGraph方法

**验证步骤**:
- 知识图谱构建正确
- 节点和边数据完整
- 关系映射准确

---

### Task 11: 实现MindMap生成功能
**时间**: 5分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/impl/TencentCloudAIServiceImpl.kt`

**描述**: 在TencentCloudAIServiceImpl中实现generateMindMap方法，直接操作MindMapEntity和MindMapNode

**验证步骤**:
- MindMap生成逻辑正确
- 使用数据库实体而非临时结构
- 节点层级关系准确

---

### Task 12: 实现苏格拉底问题生成功能
**时间**: 5分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/impl/TencentCloudAIServiceImpl.kt`

**描述**: 在TencentCloudAIServiceImpl中实现generateSocraticQuestions方法

**验证步骤**:
- 问题生成逻辑正确
- 问题类型多样化
- 难度适配合理

---

### Task 13: 实现答案评估功能
**时间**: 4分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/impl/TencentCloudAIServiceImpl.kt`

**描述**: 在TencentCloudAIServiceImpl中实现evaluateAnswer方法，支持测试模式Mock数据

**验证步骤**:
- 答案评估准确
- 反馈信息有用
- 建议具体明确
- 测试模式Mock数据正常

---

### Task 14: 实现学习进度分析功能
**时间**: 5分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/impl/TencentCloudAIServiceImpl.kt`

**描述**: 在TencentCloudAIServiceImpl中实现analyzeLearningProgress方法，支持测试模式Mock数据

**验证步骤**:
- 进度分析算法准确
- 知识缺口识别正确
- 推荐建议实用
- 测试模式Mock数据正常

---

### Task 14: 扩展ChatViewModel统一管理ChatItem（包含MindMap）
**时间**: 6分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/viewmodel/ChatViewModel.kt`

**描述**: 修改ChatViewModel，统一管理所有ChatItem类型（消息、MindMap、文件），通过Repository调用

**关键实现**:
- **统一管理ChatItem**: 包括UserMessageItem、AIMessageItem、MindMapDisplayItem、FileItem等
- **加载所有内容**: 同时加载消息和MindMap，转换为统一的ChatItem列表
- **MindMap作为ChatItem**: 生成的MindMap直接作为MindMapDisplayItem添加到Chat列表中
- **通过Repository调用**: 所有操作通过Repository，实现Local-First策略

**为什么不需要单独的MindMapViewModel？**
- MindMapDisplayItem已经是ChatItem的一种类型（TYPE_MINDMAP）
- ChatFragment使用ChatAdapter显示所有ChatItem
- 统一在ChatViewModel中管理，避免Fragment使用多个ViewModel
- 符合单一职责原则

**验证步骤**:
- ChatItem统一管理正确
- MindMap正确转换为ChatItem
- Repository调用规范
- UI无需多个ViewModel

---

### Task 15: 创建进度显示组件
**时间**: 3分钟
**文件**: `app/src/main/java/com/autodroid/teachitback/ui/component/ProgressView.kt`

**描述**: 创建用于显示学习进度的UI组件

**验证步骤**:
- UI组件显示正确
- 进度颜色准确
- 布局合理美观

---

### Task 16: 创建单元测试 - TencentCloudAIService
**时间**: 4分钟
**文件**: `app/src/test/java/com/autodroid/teachitback/api/TencentCloudAIServiceTest.kt`

**描述**: 为TencentCloudAIService编写单元测试

**验证步骤**:
- 测试用例通过
- 覆盖关键功能
- 断言准确

---

### Task 17: 创建集成测试 - 端到端流程
**时间**: 5分钟  
**文件**: `app/src/androidTest/java/com/autodroid/teachitback/TencentCloudIntegrationTest.kt`

**描述**: 编写集成测试，验证完整的腾讯云集成流程

**验证步骤**:
- 集成测试通过
- 端到端流程完整
- 数据库操作正确

---

## 依赖关系说明

### 串行依赖
- Task 1 → Task 2, 3
- Task 2, 3 → Task 4, 5（Repository扩展）
- Task 4, 5 → Task 14, 16（ViewModel依赖Repository）
- Task 6 → Task 8, 9, 10, 11, 12, 13（AI服务实现）
- Task 8-13 → Task 14, 16（AI功能完成后ViewModel集成）
- Task 14 → Task 15（ViewModel → Fragment）
- Task 16 → Task 17（ViewModel → UI组件）
- 所有实现任务 → Task 18（测试）

### 并行可执行
- Task 4, Task 5 可以并行（两个Repository可以同时扩展）
- Task 18, Task 19 可以并行（不同的单元测试）

### 关键架构要求
1. **Repository层是核心**: ViewModel必须通过Repository调用，不能直接调用AI服务
2. **Local-First优先**: Repository必须优先返回本地数据，再异步调用AI
3. **自动同步**: AI结果必须自动保存到本地数据库
4. **解耦设计**: AI服务切换不应影响前端UI

---

## 风险评估

### 高风险
- 腾讯云API调用失败或限流
- 网络不稳定导致用户体验差

### 中风险
- MindMap生成结果不符合预期
- 苏格拉底问题质量不稳定

### 低风险
- UI布局调整
- 测试覆盖率不足

---

## 注意事项

### 架构原则
1. **严格遵守MVVM分层**: UI → ViewModel → Repository → (AI Service / Database)
2. **Local-First策略**: Repository必须优先返回本地数据，保证即时响应
3. **Repository职责**: 负责协调本地数据和AI服务，ViewModel不直接调用AI
4. **AI服务隔离**: AI服务实现可以自由切换，前端UI无感知

### 技术要点
5. **API密钥安全**: 确保腾讯云API密钥安全存储，不硬编码在代码中
6. **网络异常处理**: 所有网络调用都需要完善的异常处理和重试机制
7. **数据持久化**: AI结果必须自动保存到本地数据库，供离线使用
8. **异步处理**: AI服务调用必须在后台异步执行，不阻塞UI线程
9. **用户体验**: 添加加载状态提示，优先显示本地数据，后台更新

---

## 后续优化建议

1. **离线支持**: 添加离线模式，支持基础的对话和MindMap浏览
2. **个性化增强**: 根据用户学习历史提供更个性化的问题推荐
3. **多语言支持**: 扩展支持多种语言的文档解析和对话
4. **数据可视化**: 添加学习进度的图表展示
5. **导出功能**: 支持将MindMap导出为图片或PDF