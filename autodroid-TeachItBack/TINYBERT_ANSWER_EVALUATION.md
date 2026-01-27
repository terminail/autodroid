# TinyBERT 答案评估功能实现

## TinyBERT 的正确用途

你说得完全对！TinyBERT 的设计用途是：

### 主要功能

1. **答案评估** (`evaluateAnswer`)
   - 判断用户答案是否正确
   - 计算答案的置信度
   - 提供反馈和改进建议

2. **相似度计算** (`calculateSimilarity`)
   - 使用 BERT embedding 计算文本相似度
   - 支持快速推理（最大响应时间 200ms）
   - 使用余弦相似度

3. **不支持的功能**
   - ❌ 普通对话（应使用 ChatGLM 或云端服务）
   - ❌ 文本生成（应使用生成式模型）
   - ❌ 思维导图生成
   - ❌ 文档解析

## 实现的改进

### 1. 添加了 embedding 提取功能

**文件**: [MNNIntegration.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/MNNIntegration.kt)

**新增方法**:
```kotlin
suspend fun extractEmbedding(input: String): FloatArray
```

**功能**:
- 提取文本的 BERT embedding（312 维向量）
- 使用 [CLS] token 的输出作为整个句子的表示
- 返回归一化的 embedding 向量

**使用示例**:
```kotlin
val embedding = model.extractEmbedding("抛物线是二次函数")
// 返回: FloatArray(312) - 312 维的向量
```

### 2. 实现了真正的 BERT embedding 相似度计算

**文件**: [AIServiceTinyBERT.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/service/AIServiceTinyBERT.kt)

**更新方法**:
```kotlin
private suspend fun calculateSimilarity(text1: String, text2: String): Double
```

**实现逻辑**:
1. 提取文本1的 BERT embedding
2. 提取文本2的 BERT embedding
3. 计算两个 embedding 的余弦相似度
4. 返回相似度（0-1）

**新增辅助方法**:
```kotlin
// 计算 embedding 向量的余弦相似度
private fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Double

// 简化的相似度计算（当模型不可用时）
private fun calculateSimpleSimilarity(text1: String, text2: String): Double
```

**相似度判断标准**:
- `>= 0.85`: 答案完全正确
- `>= 0.70`: 答案基本正确
- `>= 0.50`: 答案部分正确
- `< 0.50`: 答案不正确

### 3. 更新了 sendMessage 方法

**文件**: [AIServiceTinyBERT.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/service/AIServiceTinyBERT.kt)

**更新内容**:
- 不再尝试生成对话回复
- 返回 TinyBERT 的功能说明
- 引导用户使用正确的服务

**返回内容**:
```
TinyBERT 是一个轻量级 BERT 模型，主要用于：

1. 答案评估：判断用户答案是否正确
2. 相似度计算：计算文本之间的相似度
3. 快速推理：最大响应时间 200ms

对于普通对话，建议使用：
- DeepSeek（云端服务）
- 腾讯云混元（云端服务）
- ChatGLM（本地生成式模型）

如需使用 TinyBERT 进行答案评估，请在答题后调用评估功能。
```

## 使用场景

### 场景 1：答案评估

**用户操作**:
1. 用户回答问题："抛物线的顶点坐标是 (-2, 4)"
2. 系统调用 `evaluateAnswer` 方法
3. TinyBERT 计算用户答案和正确答案的相似度

**代码示例**:
```kotlin
val userAnswer = MessageEntity(
    content = "抛物线的顶点坐标是 (-2, 4)",
    senderType = "USER"
)

val correctAnswer = "抛物线的顶点坐标是 (-2, 4)"

val evaluation = tinybertService.evaluateAnswer(userAnswer, correctAnswer)

// 返回:
// AnswerEvaluation(
//     isCorrect = true,
//     confidence = 0.95,
//     feedback = "答案完全正确！",
//     suggestedImprovement = null
// )
```

### 场景 2：相似度计算

**用户操作**:
1. 用户输入两个文本
2. 系统计算它们的相似度

**代码示例**:
```kotlin
val text1 = "抛物线是二次函数"
val text2 = "抛物线是二次曲线"

val similarity = tinybertService.calculateSimilarity(text1, text2)

// 返回: 0.87 (高度相似)
```

### 场景 3：普通对话（不推荐）

**用户操作**:
1. 用户发送："解释一下抛物线"
2. 系统返回提示信息

**返回内容**:
```
TinyBERT 是一个轻量级 BERT 模型，主要用于：

1. 答案评估：判断用户答案是否正确
2. 相似度计算：计算文本之间的相似度
3. 快速推理：最大响应时间 200ms

对于普通对话，建议使用：
- DeepSeek（云端服务）
- 腾讯云混元（云端服务）
- ChatGLM（本地生成式模型）

如需使用 TinyBERT 进行答案评估，请在答题后调用评估功能。
```

## 技术细节

### BERT Embedding

**什么是 Embedding?**
- 将文本转换为数值向量
- 保留文本的语义信息
- 相似的文本有相似的 embedding

**TinyBERT 的 Embedding**:
- 维度：312
- 类型：FloatArray
- 来源：[CLS] token 的输出

**示例**:
```kotlin
val embedding1 = model.extractEmbedding("抛物线")
// [0.123, -0.456, 0.789, ..., 0.234] (312 维)

val embedding2 = model.extractEmbedding("二次函数")
// [0.145, -0.432, 0.767, ..., 0.256] (312 维)

val similarity = cosineSimilarity(embedding1, embedding2)
// 0.87 (高度相似)
```

### 余弦相似度

**公式**:
```
cosine_similarity(A, B) = (A · B) / (||A|| * ||B||)

其中：
- A · B: 点积
- ||A||: 向量 A 的模（范数）
- ||B||: 向量 B 的模（范数）
```

**实现**:
```kotlin
private fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Double {
    var dotProduct = 0.0
    var magnitude1 = 0.0
    var magnitude2 = 0.0
    
    for (i in vector1.indices) {
        dotProduct += vector1[i] * vector2[i]
        magnitude1 += vector1[i] * vector1[i]
        magnitude2 += vector2[i] * vector2[i]
    }
    
    magnitude1 = sqrt(magnitude1)
    magnitude2 = sqrt(magnitude2)
    
    return if (magnitude1 > 0 && magnitude2 > 0) {
        dotProduct / (magnitude1 * magnitude2)
    } else {
        0.0
    }
}
```

### 性能优化

**TinyBERT 的优势**:
1. **快速推理**: 最大响应时间 200ms
2. **轻量级**: 模型大小仅 14.7 MB
3. **设备端**: 无需网络连接
4. **隐私保护**: 数据不离开设备

**使用建议**:
- ✅ 适合：答案评估、相似度计算、快速分类
- ❌ 不适合：长文本生成、复杂对话、创意写作

## 测试步骤

### 1. 准备词汇表文件

将 `vocab.txt` 复制到以下任一位置：
- `app/src/main/assets/TinyBERT_General_4L_312D/vocab.txt`（推荐）
- `tibresource/models/TinyBERT_General_4L_312D/vocab.txt`
- `/sdcard/tibresource/models/TinyBERT_General_4L_312D/vocab.txt`

### 2. 重新编译应用

```bash
cd d:\git\autodroid\autodroid-TeachItBack
./gradlew clean
./gradlew assembleDebug
```

### 3. 安装并测试

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. 测试答案评估

**测试用例 1**: 正确答案
```kotlin
val userAnswer = "抛物线的顶点坐标是 (-2, 4)"
val correctAnswer = "抛物线的顶点坐标是 (-2, 4)"

val evaluation = tinybertService.evaluateAnswer(userAnswer, correctAnswer)

// 期望结果:
// isCorrect = true
// confidence >= 0.85
// feedback = "答案完全正确！"
```

**测试用例 2**: 部分正确
```kotlin
val userAnswer = "抛物线的顶点在 (-2, 4)"
val correctAnswer = "抛物线的顶点坐标是 (-2, 4)"

val evaluation = tinybertService.evaluateAnswer(userAnswer, correctAnswer)

// 期望结果:
// isCorrect = false
// confidence 在 0.70 - 0.85 之间
// feedback = "答案基本正确，但还有一些细节需要完善。"
```

**测试用例 3**: 不正确
```kotlin
val userAnswer = "抛物线是一条直线"
val correctAnswer = "抛物线的顶点坐标是 (-2, 4)"

val evaluation = tinybertService.evaluateAnswer(userAnswer, correctAnswer)

// 期望结果:
// isCorrect = false
// confidence < 0.50
// feedback = "答案与正确答案差距较大，建议重新学习相关知识点。"
```

### 5. 查看日志

```bash
adb logcat | grep -E "AIServiceTinyBERT|MNNIntegration"
```

**期望的日志输出**:
```
D/AIServiceTinyBERT: 开始计算 BERT embedding 相似度
D/AIServiceTinyBERT: 文本1: 抛物线的顶点坐标是 (-2, 4)
D/AIServiceTinyBERT: 文本2: 抛物线的顶点坐标是 (-2, 4)
D/MNNIntegration: 分词结果: 101, 1234, 5678, ... (共 15 个 tokens)
D/MNNIntegration: Embedding 提取完成: size=312, norm=1.0
D/AIServiceTinyBERT: BERT embedding 相似度: 0.95
D/AIServiceTinyBERT: 答案评估完成: isCorrect=true, confidence=0.95
```

## 与其他服务的对比

### TinyBERT vs ChatGLM

| 特性 | TinyBERT | ChatGLM |
|------|-----------|----------|
| 模型大小 | 14.7 MB | 2.8 GB |
| 响应时间 | ~200ms | ~2-5s |
| 主要用途 | 答案评估 | 对话生成 |
| 推理类型 | 编码器 | 编码器-解码器 |
| 支持功能 | 相似度计算 | 文本生成 |
| 适合场景 | 快速评估 | 复杂对话 |

### TinyBERT vs 云端服务

| 特性 | TinyBERT | DeepSeek/腾讯云 |
|------|-----------|----------------|
| 网络依赖 | 无 | 需要 |
| 隐私保护 | 高 | 中 |
| 响应速度 | 快（200ms） | 中（1-3s） |
| 功能完整性 | 有限 | 完整 |
| 成本 | 免费 | 按量计费 |

## 推荐架构

### 混合架构（推荐）

```
用户输入
    ↓
[输入建议检测]
    ↓
┌─────────────┬─────────────┬─────────────┐
│             │             │             │
TinyBERT    ChatGLM      DeepSeek
(答案评估)   (对话生成)    (云端服务)
│             │             │             │
└─────────────┴─────────────┴─────────────┘
    ↓
[答案评估]
    ↓
用户反馈
```

**工作流程**:
1. 用户输入问题
2. 输入建议检测推荐服务
3. 使用推荐的服务生成回复
4. 如果是答题场景，使用 TinyBERT 评估答案
5. 提供反馈和改进建议

### 具体实现

**场景 1：普通对话**
```kotlin
// 使用 ChatGLM 或 DeepSeek
val response = chatglmService.sendMessage(message, context)
```

**场景 2：答题评估**
```kotlin
// 1. 用户回答问题
val userAnswer = getUserAnswer()

// 2. 获取正确答案
val correctAnswer = getCorrectAnswer()

// 3. 使用 TinyBERT 评估
val evaluation = tinybertService.evaluateAnswer(userAnswer, correctAnswer)

// 4. 显示反馈
showFeedback(evaluation.feedback)
showConfidence(evaluation.confidence)
if (evaluation.suggestedImprovement != null) {
    showSuggestion(evaluation.suggestedImprovement)
}
```

**场景 3：相似度搜索**
```kotlin
// 1. 用户输入查询
val query = "抛物线的性质"

// 2. 从知识库提取候选答案
val candidates = knowledgeBase.search(query, topK = 5)

// 3. 使用 TinyBERT 计算相似度
val similarities = candidates.map { candidate ->
    val similarity = tinybertService.calculateSimilarity(query, candidate.text)
    candidate to similarity
}

// 4. 返回最相似的答案
val bestMatch = similarities.maxByOrNull { it.second }
```

## 总结

### TinyBERT 的正确用途

✅ **适合**:
- 答案评估（判断用户答案是否正确）
- 相似度计算（计算文本之间的相似度）
- 快速分类（文本分类、情感分析）
- 特征提取（提取文本的语义特征）

❌ **不适合**:
- 普通对话（应使用 ChatGLM 或云端服务）
- 长文本生成（应使用生成式模型）
- 创意写作（应使用专门的写作模型）
- 复杂推理（应使用更大的模型）

### 实现的改进

1. ✅ 添加了 `extractEmbedding` 方法
2. ✅ 实现了真正的 BERT embedding 相似度计算
3. ✅ 更新了 `sendMessage` 方法，返回正确的功能说明
4. ✅ 保留了简化相似度计算作为备用方案

### 下一步

1. **测试答案评估功能**
2. **集成到答题流程**
3. **实现混合架构**（TinyBERT + ChatGLM/DeepSeek）
4. **优化性能**（缓存 embedding、批量推理）

## 相关文件

- [BertTokenizer.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/BertTokenizer.kt) - BERT 分词器
- [MNNIntegration.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/MNNIntegration.kt) - MNN 框架集成
- [AIServiceTinyBERT.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/service/AIServiceTinyBERT.kt) - TinyBERT AI 服务
- [AIServiceConfig.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/config/AIServiceConfig.kt) - AI 服务配置
