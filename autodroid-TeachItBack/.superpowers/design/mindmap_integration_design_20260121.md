# Teach It Back - MindMap集成设计文档

## 项目概述
- **阶段**: 详细设计
- **时间戳**: 2026-01-21
- **项目类型**: Android
- **核心需求**: 智能MindMap集成与学习路径管理

## 设计理念

### 核心交互流程
```mermaid
sequenceDiagram
    participant U as 用户
    participant A as App
    participant AI as AI服务
    participant M as MindMap
    
    U->>A: 选择学习主题
    A->>AI: 请求生成学习路径
    AI-->>A: 返回MindMap结构
    A->>M: 创建MindMap并显示
    
    loop 学习过程
        U->>A: 自然对话学习
        A->>AI: 发送对话内容
        AI->>AI: 分析知识点关联
        AI->>AI: 评估学习效果
        AI-->>A: 返回进度更新
        A->>M: 更新MindMap进度
        M-->>U: 实时显示学习进展
    end
```

## 架构设计

### 预置课程使用流程
```mermaid
graph TB
    A[WHY Fragment] --> B[预置Topics列表]
    B --> C[用户选择预置Topic]
    C --> D[复制为个人Topic]
    D --> E[用户修改目标]
    E --> F[开始学习]
    
    subgraph "预置Topic属性"
        G[标准MindMap结构]
        H[基础学习目标]
        I[推荐学习路径]
    end
    
    C --> G
    C --> H
    C --> I
    
    D --> J[个人Topic副本]
    J --> K[可自由修改]
    K --> F
```

### 数据模型关系
```mermaid
erDiagram
    TOPIC ||--|| MINDMAP : "has_one"
    MINDMAP ||--o{ MINDMAP_NODE : "contains"
    MINDMAP_NODE ||--o{ MINDMAP_NODE : "child_of"
    
    TOPIC {
        string id PK
        string title
        string description
        int mastery_level
    }
    
    MINDMAP {
        string id PK
        string topic_id FK
        string title
        string structure_json
    }
    
    MINDMAP_NODE {
        string id PK
        string mindmap_id FK
        string parent_id FK
        string title
        int progress
        string node_type
    }
```

### 系统架构
```mermaid
graph TB
    A[UI Layer] --> B[ViewModel]
    B --> C[Repository]
    C --> D[Room Database]
    C --> E[AI Service]
    
    subgraph "新增组件"
        F[MindMap Manager]
        G[Progress Tracker]
        H[Learning Path Planner]
    end
    
    B --> F
    F --> G
    F --> H
    G --> D
    H --> E
```

## UI设计

### 预置课程管理
```mermaid
graph TB
    A[WHY Fragment] --> B[预置课程列表]
    B --> C[标准高中课程]
    B --> D[大学基础课程]
    B --> E[专业技能课程]
    
    C --> F[高中三年级生物]
    C --> G[高中物理]
    C --> H[高中化学]
    
    D --> I[微积分基础]
    D --> J[编程入门]
    D --> K[经济学原理]
    
    E --> L[项目管理]
    E --> M[数据分析]
    E --> N[沟通技巧]
    
    F --> O[标准MindMap结构]
    G --> P[标准MindMap结构]
    H --> Q[标准MindMap结构]
    
    O --> R[个性化微调]
    P --> R
    Q --> R
    R --> S[开始学习]
```

### ChatFragment异构Item扩展
```mermaid
graph LR
    A[ChatFragment] --> B[RecyclerView]
    B --> C[TextMessage Item]
    B --> D[AIResponse Item]
    B --> E[MindMap Item]
    
    E --> F[缩略MindMap]
    E --> G[进度可视化整合]
    E --> H[点击扩展]
    
    F --> I[节点颜色编码]
    G --> J[整体进度统计]
    H --> K[详细视图]
```

### MindMap可视化设计
```mermaid
graph TD
    A[MindMap Item] --> B{显示模式}
    B -->|缩略图| C[紧凑布局]
    B -->|展开图| D[详细布局]
    
    C --> E[主要节点显示]
    C --> F[整合进度可视化]
    C --> G[点击展开]
    
    D --> H[完整层级结构]
    D --> I[详细进度信息]
    D --> J[节点交互]
    
    E --> K[颜色编码: 红黄绿]
    F --> L[进度统计与摘要]
    J --> M[点击学习]
```

## AI集成设计

### Prompt处理流程
```mermaid
flowchart TD
    A[用户输入] --> B[构建Prompt]
    B --> C[调用AI服务]
    C --> D[解析响应]
    D --> E{响应类型}
    
    E -->|学习评估| F[提取进度数据]
    E -->|路径规划| G[生成MindMap结构]
    E -->|提问引导| H[生成苏格拉底问题]
    
    F --> I[更新节点进度]
    G --> J[创建/更新MindMap]
    H --> K[返回引导问题]
    
    I --> L[UI更新]
    J --> L
    K --> L
```

### AI响应数据结构
```mermaid
classDiagram
    class AIResponse {
        +String responseText
        +Map~String,Int~ progressUpdates
        +String nextFocusNode
        +String assessment
        +String learningAdvice
    }
    
    class ProgressUpdate {
        +String nodeId
        +int newProgress
        +String reason
    }
    
    AIResponse --> ProgressUpdate : contains
```

## 学习流程设计

### 智能学习引导
```mermaid
stateDiagram-v2
    [*] --> 课程选择
    课程选择 --> 预置路径 : 选择标准课程
    课程选择 --> 自定义目标 : 输入个性化目标
    
    预置路径 --> 路径个性化 : AI根据目标微调
    自定义目标 --> 路径生成 : AI分析生成
    
    路径个性化 --> 节点学习
    路径生成 --> 节点学习
    
    节点学习 --> 对话评估 : 用户回答
    对话评估 --> 进度更新 : AI评估
    进度更新 --> 下一步决策 : 分析结果
    
    下一步决策 --> 深化学习 : 掌握不足
    下一步决策 --> 新节点 : 掌握良好
    下一步决策 --> 复习巩固 : 需要强化
    
    深化学习 --> 节点学习
    新节点 --> 节点学习
    复习巩固 --> 节点学习
    
    节点学习 --> [*] : 学习完成
```

### 进度评估机制
```mermaid
graph TD
    A[用户回答] --> B[AI分析]
    B --> C{理解程度}
    
    C -->|优秀 80-100%| D[标记为掌握]
    C -->|良好 60-79%| E[建议深化]
    C -->|一般 40-59%| F[指出误区]
    C -->|不足 0-39%| G[重新讲解]
    
    D --> H[进度+20]
    E --> I[进度+10]
    F --> J[进度+5]
    G --> K[进度不变]
    
    H --> L[更新MindMap]
    I --> L
    J --> L
    K --> L
```

## 技术实现策略

### 分阶段实施
```mermaid
gantt
    title 项目实施时间线
    dateFormat YYYY-MM-DD
    section Phase 1: 基础架构
    数据模型设计 :2026-01-22, 7d
    数据库扩展 :2026-01-29, 5d
    AI服务集成 :2026-02-03, 7d
    
    section Phase 2: UI实现
    MindMap组件 :2026-02-10, 10d
    异构Item集成 :2026-02-20, 7d
    交互逻辑 :2026-02-27, 7d
    
    section Phase 3: 智能优化
    Prompt调优 :2026-03-06, 10d
    路径规划算法 :2026-03-16, 7d
    性能优化 :2026-03-23, 7d
```

### 风险评估与缓解
```mermaid
graph TD
    A[风险分析]
    
    subgraph "技术风险"
        B1[AI关联准确性]
        B2[MindMap性能]
        B3[数据一致性]
    end
    
    subgraph "用户体验风险"
        C1[界面复杂度]
        C2[学习曲线]
        C3[进度理解]
    end
    
    subgraph "业务风险"
        D1[功能价值]
        D2[技术可行性]
        D3[时间成本]
    end
    
    A --> B1
    A --> B2
    A --> B3
    A --> C1
    A --> C2
    A --> C3
    A --> D1
    A --> D2
    A --> D3
    
    B1 --> E1[多轮测试优化]
    B2 --> E2[树形结构优化]
    B3 --> E3[事务机制]
    C1 --> F1[简洁设计]
    C2 --> F2[引导文档]
    C3 --> F3[清晰可视化]
    D1 --> G1[用户测试]
    D2 --> G2[原型验证]
    D3 --> G3[分阶段实施]
```

## 成功指标

### 核心指标追踪
```mermaid
graph TB
    A[功能优先级] --> B[高价值易实现]
    A --> C[高价值难实现]
    A --> D[低价值易实现]
    A --> E[低价值难实现]
    
    B --> F[基础MindMap显示]
    B --> G[进度可视化]
    
    C --> H[AI智能关联]
    C --> I[路径动态调整]
    
    D --> J[界面美化]
    
    E --> K[暂不实现]
    
    F --> L[优先级 1]
    G --> L
    H --> M[优先级 2]
    I --> M
    J --> N[优先级 3]
    K --> O[优先级 4]
```

---

## 设计审核要点

### 架构合理性
- MindMap与现有Topic模型的集成方式
- 数据流和状态管理设计
- AI服务的职责边界划分

### 用户体验
- 自然学习流程的设计
- MindMap的交互简洁性
- 进度可视化的直观性

### 技术可行性
- Android平台的技术限制考虑
- AI集成的实际可行性
- 性能和维护性考量