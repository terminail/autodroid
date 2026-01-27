# TinyBERT 推理修复说明

## 问题分析

当你发送"解释一下抛物线"时，AI回复"推理结果：0个输出"，这是因为：

1. **缺少分词器（Tokenizer）**：TinyBERT 需要将文本转换为 token IDs，但原代码中没有实现分词功能
2. **预处理函数为空**：`preprocessInput()` 返回空的 `FloatArray`
3. **后处理函数简化**：`postprocessOutput()` 只是返回输出数量，没有实际处理

## 解决方案

### 1. 创建了 BERT 分词器

**文件**: [BertTokenizer.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/BertTokenizer.kt)

**功能**:
- 从词汇表文件（vocab.txt）加载 token 映射
- 将文本转换为 token IDs
- 创建 attention mask 和 token type IDs
- 支持中文分词（每个字符作为一个 token）
- 支持英文分词（按空格和标点符号分割）

**特殊标记**:
- `[CLS]` (ID: 101) - 句子开始标记
- `[SEP]` (ID: 102) - 句子结束标记
- `[PAD]` (ID: 0) - 填充标记
- `[UNK]` (ID: 100) - 未知词标记

### 2. 更新了 MNNModel 类

**文件**: [MNNIntegration.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/MNNIntegration.kt)

**主要更改**:

#### 2.1 添加了分词器支持
```kotlin
class MNNModel(
    private val netInstance: MNNNetInstance,
    private val session: MNNNetInstance.Session,
    private val tokenizer: BertTokenizer? = null  // 新增
)
```

#### 2.2 实现了正确的预处理
```kotlin
private fun preprocessInput(input: String): Triple<FloatArray, FloatArray, FloatArray> {
    // 使用分词器进行分词
    val tokenIds = tokenizer.tokenize(input)
    
    // 创建 attention mask
    val attentionMask = tokenizer.createAttentionMask(tokenIds)
    
    // 创建 token type ids
    val tokenTypeIds = tokenizer.createTokenTypeIds(tokenIds)
    
    // 转换为 FloatArray
    return Triple(inputIdsFloat, attentionMaskFloat, tokenTypeIdsFloat)
}
```

#### 2.3 实现了正确的后处理
```kotlin
private fun postprocessOutput(output: FloatArray): String {
    // TinyBERT 的输出是 hidden states
    // 使用 [CLS] token 的输出作为整个句子的表示
    val clsEmbedding = output.sliceArray(0 until HIDDEN_SIZE)
    
    // 基于 embedding 生成回复
    return generateTextFromEmbedding(clsEmbedding)
}
```

#### 2.4 更新了输入输出张量名称
```kotlin
// 输入张量
private const val INPUT_IDS = "input_ids"
private const val ATTENTION_MASK = "attention_mask"
private const val TOKEN_TYPE_IDS = "token_type_ids"

// 输出张量
private const val HIDDEN_STATES = "hidden_states"
```

### 3. 更新了模型加载逻辑

**文件**: [MNNIntegration.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/MNNIntegration.kt)

**功能**:
- 在加载模型时自动加载分词器
- 支持从多个路径读取词汇表文件：
  1. `assets/TinyBERT_General_4L_312D/vocab.txt`
  2. `tibresource/models/TinyBERT_General_4L_312D/vocab.txt`
  3. `/sdcard/tibresource/models/TinyBERT_General_4L_312D/vocab.txt`
  4. `/sdcard/Android/data/com.autodroid.teachitback/files/tibresource/models/TinyBERT_General_4L_312D/vocab.txt`

## 测试步骤

### 1. 确保词汇表文件存在

词汇表文件应该位于以下任一位置：
- `app/src/main/assets/TinyBERT_General_4L_312D/vocab.txt`（推荐）
- `tibresource/models/TinyBERT_General_4L_312D/vocab.txt`
- `/sdcard/tibresource/models/TinyBERT_General_4L_312D/vocab.txt`

### 2. 重新编译应用

```bash
./gradlew clean
./gradlew assembleDebug
```

### 3. 安装并测试

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. 查看日志

运行应用后，查看 logcat 输出：

```bash
adb logcat | grep -E "MNNIntegration|BertTokenizer|AIServiceTinyBERT"
```

**期望的日志输出**:
```
D/MNNIntegration: 尝试加载词汇表: TinyBERT_General_4L_312D/vocab.txt
D/BertTokenizer: 词汇表加载完成，共 30522 个 token
D/MNNIntegration: 分词器加载成功
D/MNNIntegration: 分词结果: 101, 1234, 5678, ... (共 15 个 tokens)
D/MNNIntegration: 成功获取所有输入张量
D/MNNIntegration: 推理执行完成
D/MNNIntegration: 输出数据获取完成: size=159744
D/MNNIntegration: 后处理完成: 这是一个很好的问题！让我来解释一下。
```

## 当前限制

### 1. 文本生成是简化实现

当前的 `generateTextFromEmbedding()` 使用简单的规则生成回复：

```kotlin
return when {
    embeddingMean > 0.5 -> "这是一个很好的问题！让我来解释一下。"
    embeddingMean > 0.0 -> "我理解你的问题，这里有一些相关信息。"
    embeddingMean > -0.5 -> "这个问题很有趣，让我想想。"
    else -> "我正在思考这个问题，请稍等。"
}
```

**原因**: TinyBERT 是一个编码器模型，主要用于特征提取，不是生成式模型。要实现真正的对话功能，需要：

#### 方案 A：使用解码器模型
- 使用 ChatGLM、Qwen 等生成式模型
- 这些模型可以直接生成文本回复

#### 方案 B：使用检索增强生成（RAG）
- 使用 TinyBERT 提取问题的特征
- 从知识库中检索相关内容
- 使用检索到的内容生成回复

#### 方案 C：使用云端 AI 服务
- 使用 DeepSeek、腾讯云混元等云端服务
- 这些服务已经实现了完整的对话功能

### 2. 模型输出是 Embedding

TinyBERT 的输出是 312 维的 embedding 向量，而不是直接的文本。要将其转换为有意义的回复，需要：

1. **相似度搜索**：计算 embedding 与预定义回复的相似度
2. **分类任务**：将 embedding 用于文本分类、情感分析等
3. **特征提取**：将 embedding 作为其他模型的输入

## 推荐方案

### 短期方案（快速验证）

1. **使用云端 AI 服务**：
   - 配置 DeepSeek、腾讯云混元等服务
   - 这些服务已经可以正常工作
   - 用于验证应用逻辑是否正确

2. **使用简化回复**：
   - 当前的简化实现可以验证推理流程
   - 确认模型加载、预处理、推理、后处理都正常工作

### 中期方案（改进体验）

1. **实现检索增强生成（RAG）**：
   - 建立知识库（抛物线、函数、几何等）
   - 使用 TinyBERT 提取问题特征
   - 从知识库检索相关内容
   - 使用检索到的内容生成回复

2. **使用预定义回复模板**：
   - 为常见问题准备标准答案
   - 使用 TinyBERT 进行问题分类
   - 根据分类结果返回对应的答案

### 长期方案（完整功能）

1. **集成生成式模型**：
   - 使用 ChatGLM-6B 或 Qwen-7B 等模型
   - 这些模型可以直接生成对话回复
   - 需要更大的存储空间和计算资源

2. **混合架构**：
   - TinyBERT 用于快速问题分类和特征提取
   - 生成式模型用于复杂对话
   - 根据问题类型智能选择模型

## 资料准备清单

### 必需资料

- ✅ **模型文件**: `tinybert-int8.mnn` (14.7 MB)
- ✅ **词汇表文件**: `vocab.txt` (30522 个 token)
- ✅ **模型配置**: `config.json` (模型架构信息)
- ✅ **分词器配置**: `tokenizer_config.json` (分词器参数)

### 可选资料

- ⭕ **训练数据**: 用于微调模型
- ⭕ **知识库**: 用于 RAG 系统
- ⭕ **预定义回复**: 用于快速响应常见问题

## 下一步

1. **测试当前实现**：
   - 重新编译并安装应用
   - 发送测试消息："解释一下抛物线"
   - 查看日志输出，确认分词器加载成功

2. **选择方案**：
   - 如果需要快速验证，使用云端 AI 服务
   - 如果需要本地推理，实现 RAG 系统
   - 如果需要完整对话，集成生成式模型

3. **优化性能**：
   - 调整模型参数（线程数、量化级别）
   - 优化预处理和后处理逻辑
   - 添加缓存机制

## 相关文件

- [BertTokenizer.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/BertTokenizer.kt) - BERT 分词器实现
- [MNNIntegration.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/framework/MNNIntegration.kt) - MNN 框架集成
- [AIServiceTinyBERT.kt](file:///d:/git/autodroid/autodroid-TeachItBack/app/src/main/kotlin/com/autodroid/teachitback/service/AIServiceTinyBERT.kt) - TinyBERT AI 服务
- [vocab.txt](file:///d:/git/autodroid/autodroid-TeachItBack/tibresource/models/TinyBERT_General_4L_312D/vocab.txt) - 词汇表文件

## 参考资料

- [TinyBERT 论文](https://arxiv.org/abs/1909.10351)
- [MNN 官方文档](file:///d:/git/autodroid/autodroid-TeachItBack/MNN/docs/tools/convert.md)
- [Hugging Face Transformers](https://huggingface.co/docs/transformers)
- [BERT 模型详解](https://huggingface.co/bert-base-uncased)
