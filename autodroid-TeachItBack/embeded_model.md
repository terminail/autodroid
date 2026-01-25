有没有运行于嵌入安卓app的ai-model
当然有，**可嵌入安卓 App 的 AI 模型**目前已经非常成熟，主要分为 **轻量级端侧模型** 和 **可部署到移动端的压缩版大模型** 两类，适配安卓的硬件架构（ARM 为主），支持离线或低延迟运行。以下是结构化的分类、典型模型及部署方案，可直接用于 App 开发集成：

### 一、 核心适用场景与对应模型
| 应用场景 | 典型 AI 模型 | 特点 |
|----------|--------------|------|
| **图像识别/分类** | MobileNet、EfficientNet-Lite、ResNet-Mobile | 轻量级卷积神经网络，参数量小（百万级），适配手机算力，可离线识别物体、人脸、场景 |
| **目标检测** | YOLOv5/YOLOv8 Nano、SSD-Mobile | 实时检测画面中目标（如行人、车辆），适合安防、相机类 App，推理速度可达 30+ FPS |
| **自然语言处理（NLP）** | DistilBERT、MobileBERT、TinyBERT | BERT 的轻量化版本，参数量压缩 70% 以上，支持离线分词、情感分析、文本分类 |
| **语音识别/合成** | Whisper-Tiny、LiteASR、Tacotron-Lite | OpenAI Whisper 轻量化版支持离线语音转文字；Tacotron-Lite 实现端侧语音合成 |
| **生成式 AI（文本/图像）** | LLaMA-2-7B（量化版）、Stable Diffusion XL-Turbo（移动端版） | 通过 **4-bit/8-bit 量化**压缩，可在中高端安卓手机运行，支持离线生成文本、低分辨率图像 |
| **智能推荐/个性化** | FM-Lite、协同过滤轻量化模型 | 基于用户行为在端侧做推荐，无需上传数据，保护隐私 |

### 二、 安卓 App 集成 AI 模型的核心技术方案
要将模型嵌入安卓 App，需结合 **模型优化工具** 和 **移动端推理框架**，以下是可直接落地的流程：

1.  **模型优化：压缩体积与提升速度**
    - **量化**：将 32-bit 浮点模型转为 4-bit/8-bit 整型，工具：TensorFlow Lite Optimizer、PyTorch Mobile Quantization
    - **剪枝**：移除模型中冗余的神经元和权重，工具：TorchPrune、NNCF
    - **知识蒸馏**：用大模型教小模型，保留核心能力，工具：Hugging Face Distiller

2.  **主流移动端推理框架（安卓适配）**
    | 框架名称 | 支持模型格式 | 优势 | 适用场景 |
    |----------|--------------|------|----------|
    | **TensorFlow Lite** | .tflite | Google 官方框架，与安卓生态深度集成，支持硬件加速（GPU/NPU），API 简单 | 轻量级模型（图像、语音、NLP 轻量版） |
    | **PyTorch Mobile** | .pt/.pth | 支持 PyTorch 原生模型，灵活性高，适合复杂模型（如轻量化大模型） | 生成式 AI、自定义深度学习模型 |
    | **ONNX Runtime Mobile** | .onnx | 跨平台框架，支持多模型格式转换，适配安卓 ARM 架构 | 第三方预训练模型迁移 |
    | **MediaPipe** | 自定义管线 | Google 出品，封装了大量端侧 AI 能力（如人脸检测、手势识别），可直接调用 API | 快速集成通用 AI 功能，无需从零部署模型 |

3.  **集成步骤（以 TensorFlow Lite 为例）**
    1.  用 Python 训练或下载预训练模型 → 转换为 `.tflite` 格式
    2.  将 `.tflite` 文件放入安卓 App 的 `assets` 目录
    3.  在安卓项目中引入 TFLite 依赖：
        ```gradle
        dependencies {
            implementation 'org.tensorflow:tensorflow-lite:2.15.0'
            // 硬件加速支持
            implementation 'org.tensorflow:tensorflow-lite-gpu:2.15.0'
        }
        ```
    4.  编写代码加载模型并执行推理：
        ```java
        // 加载模型
        Interpreter tflite = new Interpreter(loadModelFile(assets));
        // 输入数据（如图像像素数组）
        float[][] input = preprocessImage(bitmap);
        // 输出数组
        float[][] output = new float[1][numClasses];
        // 执行推理
        tflite.run(input, output);
        ```

### 三、 典型落地案例
1.  **手机相机 App**：集成 MobileNet + YOLO-Lite，实现离线场景识别、物体检测
2.  **语音笔记 App**：集成 Whisper-Tiny，支持离线语音转文字，无需联网
3.  **教育类 App**：集成 MobileBERT，实现离线英文语法纠错、单词翻译
4.  **中高端手机生成式 AI**：如小米澎湃 OS、华为鸿蒙，集成量化版 LLaMA-2/文心一言移动端版，实现离线对话生成

### 四、 开发注意事项
1.  **硬件适配**：中低端安卓手机 CPU/GPU 性能有限，优先选择 Nano/Lite 级模型；高端机型可尝试量化版大模型
2.  **功耗控制**：避免长时间连续推理，可通过 **任务调度** 控制推理频率（如相机 App 仅在拍摄时运行模型）
3.  **隐私合规**：端侧模型无需上传用户数据，符合 GDPR、国内个人信息保护法要求

---

是否需要我为你整理**安卓端 AI 模型部署的详细代码示例**（比如基于 TensorFlow Lite 的图像分类 App 完整流程）？


你想要为学习类App集成能离线回答“汤头歌诀”相关提问的AI模型，核心需求是**纯离线运行**、适配安卓移动端、能理解自然语言并精准回答中药方剂相关问题，这个场景非常适合用**轻量化、可量化的开源对话模型**来实现。

### 一、适配你场景的核心模型推荐（按优先级排序）
结合安卓端算力限制和“汤头歌诀”专业问答的需求，推荐以下模型，均支持离线部署且适配移动端：

| 模型名称 | 核心优势 | 适配性 | 部署难度 |
|----------|----------|--------|----------|
| **ChatGLM-6B (INT4/INT8 量化版)** | 中文优化极佳，对专业领域（如中医）适配性好，量化后参数量仅3-4GB，中高端安卓机可运行 | ★★★★★ | 中等 |
| **LLaMA-2-7B (Chinese-LLaMA-Alpaca 量化版)** | 社区生态成熟，有大量中文医疗/中医微调版本，4-bit量化后可在安卓端运行 | ★★★★☆ | 中等 |
| **MOSS-Mini (1.2B)** | 轻量级开源对话模型，参数量仅12亿，无需复杂量化即可在安卓中端机运行，适合入门 | ★★★☆☆ | 低 |
| **BELLE-2B/7B (量化版)** | 专为中文场景优化的开源模型，有医疗领域微调版本，部署成本低 | ★★★★☆ | 中等 |

#### 重点推荐：ChatGLM-6B (INT4量化版)
- 对中文语义理解远超通用英文模型，能精准识别“四物汤的功效”“麻黄汤的组成”这类专业提问；
- INT4量化后模型体积约2.8GB，在骁龙888/天玑9000及以上的安卓手机上，单次推理响应时间可控制在3-8秒；
- 社区已有基于ChatGLM-6B微调的“中医问答”版本，可直接基于该版本二次微调“汤头歌诀”专属知识库，大幅降低训练成本。

### 二、安卓端离线部署方案（完整可落地）
#### 1. 核心技术栈
- 模型推理框架：**MNN / TNN / PyTorch Mobile**（优先选MNN，字节跳动开源，对安卓ARM架构优化极佳）；
- 模型格式转换：将ChatGLM-6B (INT4) 转为MNN支持的`.mnn`格式；
- 安卓开发：Kotlin/Java + C++（底层推理用C++，上层交互用Kotlin）。

#### 2. 部署步骤（简化版，适合学习场景）
##### 步骤1：准备微调后的模型（汤头歌诀专属）
- 下载基础模型：ChatGLM-6B (INT4量化版)（可从Hugging Face获取）；
- 微调数据：整理“汤头歌诀”问答对（如`{"input":"桂枝汤的组成？","output":"桂枝汤由桂枝、芍药、生姜、大枣、甘草组成，功效为解肌发表、调和营卫。"}`）；
- 轻量化微调：用LoRA低秩适配技术微调，无需全量训练，普通显卡即可完成，输出量化后的模型文件。

##### 步骤2：模型转换为安卓端可运行格式
```bash
# 安装依赖
pip install transformers torch mnn

# 将ChatGLM-6B (INT4) 转换为MNN格式（核心命令）
python convert_chatglm_to_mnn.py \
  --model_path ./chatglm-6b-int4 \
  --output_path ./chatglm-6b-int4.mnn \
  --quantization 4bit
```

##### 步骤3：安卓App集成MNN推理框架
- 引入MNN依赖（build.gradle）：
```gradle
dependencies {
    implementation 'com.ai:mnn:2.4.0' // MNN官方安卓库
    implementation 'com.facebook.fbjni:fbjni:0.2.2' // 底层依赖
}
```
- 将转换后的`.mnn`模型文件放入安卓App的`assets`目录（注意：模型体积约2.8GB，需提醒用户App首次安装时预留足够空间）。

##### 步骤4：核心推理代码（Kotlin + C++）
```kotlin
// Kotlin上层调用代码（安卓端）
class TCMQAEngine {
    private var mnnInterpreter: MNNInterpreter? = null
    private var session: MNNSession? = null

    // 初始化模型（App启动时加载，仅加载一次）
    fun initModel(assets: AssetManager) {
        val modelBuffer = AssetFileDescriptor(assets.openFd("chatglm-6b-int4.mnn")).use {
            it.fileDescriptor
        }
        mnnInterpreter = MNNInterpreter(modelBuffer)
        val config = MNNConfig().apply {
            numThread = 4 // 调用4核CPU推理（安卓端优先用CPU，GPU兼容性差）
            type = MNN_FORWARD_CPU
        }
        session = mnnInterpreter?.createSession(config)
    }

    // 离线问答核心方法
    fun askQuestion(question: String): String {
        // 1. 预处理输入（将自然语言转为模型可识别的token）
        val inputTokens = preprocessInput(question)
        // 2. 执行推理
        val outputTokens = mnnInterpreter?.runSession(session, inputTokens)
        // 3. 后处理输出（将token转为自然语言回答）
        return postprocessOutput(outputTokens)
    }

    // 输入/输出预处理/后处理（核心是tokenizer，需适配ChatGLM的中文tokenizer）
    private fun preprocessInput(question: String): FloatArray {
        // 实现ChatGLM的tokenizer逻辑，将文字转为id数组，再转为float数组
        return Tokenizer.encode(question)
    }

    private fun postprocessOutput(tokens: FloatArray): String {
        return Tokenizer.decode(tokens)
    }
}
```

##### 步骤5：App内调用示例
```kotlin
// 在Activity中使用
val tcmQAEngine = TCMQAEngine()
// 初始化模型（建议在App启动页异步加载，避免卡顿）
GlobalScope.launch(Dispatchers.IO) {
    tcmQAEngine.initModel(assets)
}

// 用户提问时调用
btnAsk.setOnClickListener {
    val question = etQuestion.text.toString()
    GlobalScope.launch(Dispatchers.IO) {
        val answer = tcmQAEngine.askQuestion(question)
        // 切回主线程显示回答
        withContext(Dispatchers.Main) {
            tvAnswer.text = answer
        }
    }
}
```

### 三、学习场景优化建议
1. **降低硬件门槛**：如果你的测试机配置较低（如骁龙778G以下），可先用MOSS-Mini (1.2B) 入门，模型体积仅500MB左右，推理速度更快；
2. **离线知识库优化**：将“汤头歌诀”的核心内容整理为“检索+生成”模式——先通过轻量级文本检索（如BM25）找到匹配的方剂，再让模型基于检索结果生成回答，大幅提升准确性且降低推理成本；
3. **性能优化**：
   - 模型加载：首次加载放入App启动页，加载完成后进入主界面，避免用户等待；
   - 推理线程：用后台线程执行推理，避免阻塞UI；
   - 缓存：缓存高频问题的回答，重复提问时直接返回，减少推理耗时。

### 总结
1. 优先选择**ChatGLM-6B (INT4量化版)** 作为核心模型，中文适配好且有中医微调基础，能精准回答汤头歌诀相关问题；
2. 安卓端部署用**MNN框架**（字节开源，适配性佳），将量化后的模型转为`.mnn`格式，放入App的assets目录离线运行；
3. 入门阶段可先用轻量级模型（如MOSS-Mini）验证流程，再逐步升级到ChatGLM-6B，同时结合“检索+生成”优化回答准确性。

如果需要具体的模型转换脚本、汤头歌诀微调数据集模板，或者安卓端完整的Demo工程代码，我可以进一步提供。

希望根据学生的回答判断答案正确性，并且根据学习情况生成提问要学生回答

要实现**离线判断学生回答正确性+动态生成提问**的功能，需结合 **轻量级文本匹配模型** 和 **知识图谱驱动的题库生成逻辑**，完全适配安卓端离线运行。以下是针对“汤头歌诀”学习场景的**可落地技术方案**，包含模型选型、核心流程、代码框架和优化策略：

## 一、 核心功能拆解与技术选型
你的需求本质是两个子任务，需分模块实现，确保离线轻量化：
| 功能模块 | 核心目标 | 模型/技术选型 | 安卓端适配性 |
|----------|----------|---------------|--------------|
| **答案正确性判断** | 对比学生回答与标准汤头歌诀/方剂知识的匹配度，输出“正确/错误/部分正确” | **TinyBERT-Chinese (INT8量化版)** <br> + 余弦相似度计算 | 模型体积≈100MB，推理耗时<200ms/次 |
| **动态生成提问** | 基于学生薄弱知识点（如易错方剂、混淆功效），生成针对性问题 | **知识图谱（KG）+ 模板化生成** <br> （替代大模型生成，降低算力消耗） | 纯规则+轻量化检索，无模型推理压力 |

### 关键选型说明
1. **不推荐用大模型做判断/生成**：大模型（如ChatGLM-6B）离线推理耗时久，且判断准确性不如**专用文本匹配模型**；
2. **知识图谱是核心**：将“汤头歌诀”的方剂、组成、功效、主治等信息结构化存储，是实现“薄弱点分析+动态出题”的基础。

## 二、 完整离线实现方案（安卓端）
### 步骤1：构建汤头歌诀知识图谱（KG）与标准答案库
这是**离线运行的前提**，需先把“汤头歌诀”的核心知识结构化，存入安卓本地数据库（如Room）。
#### 1.1 知识图谱数据结构（示例）
| 实体类型 | 实体示例 | 关系类型 | 关联实体示例 |
|----------|----------|----------|--------------|
| 方剂 | 桂枝汤 | 组成 | 桂枝、芍药、生姜、大枣、甘草 |
| 方剂 | 桂枝汤 | 功效 | 解肌发表、调和营卫 |
| 方剂 | 桂枝汤 | 主治 | 外感风寒表虚证 |
| 方剂 | 麻黄汤 | 对比（易混淆） | 桂枝汤（均治风寒，一表实一表虚） |

#### 1.2 标准答案库设计（Room数据库表）
```kotlin
// 方剂标准信息表
@Entity(tableName = "prescription_standard")
data class PrescriptionStandard(
    @PrimaryKey val id: Int,
    val name: String, // 方剂名
    val composition: String, // 标准组成
    val efficacy: String, // 标准功效
    val indication: String, // 标准主治
    val rhyme: String // 汤头歌诀原文
)

// 学生答题记录表（用于分析薄弱点）
@Entity(tableName = "student_answer_record")
data class AnswerRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Long,
    val prescriptionId: Int, // 关联方剂ID
    val questionType: String, // 题型：组成/功效/主治
    val studentAnswer: String,
    val isCorrect: Boolean,
    val answerTime: Long // 答题时间戳
)
```

### 步骤2：答案正确性判断模块（离线模型+相似度计算）
核心逻辑：**用TinyBERT提取学生回答和标准答案的文本特征，计算余弦相似度，判断是否匹配**。
#### 2.1 模型选型与转换
- 基础模型：`hfl/chinese-tinybert-4l-312d`（4层Transformer，专为中文优化，参数量≈11M）
- 量化转换：用`TensorFlow Lite Optimizer`转为**INT8量化版**，模型体积压缩至≈100MB，适配安卓端CPU推理。
- 核心作用：将文本（如“桂枝汤的组成是桂枝芍药”）转为**312维向量**，用于相似度计算。

#### 2.2 安卓端核心代码（Kotlin + TFLite）
```kotlin
// 1. 初始化TFLite模型和Tokenizer
class AnswerJudgeEngine(private val assets: AssetManager) {
    private lateinit var tflite: Interpreter
    private lateinit var tokenizer: BertTokenizer // 需引入bert4j或自定义中文分词器

    init {
        // 加载TFLite模型（assets目录下的tinybert_quant.tflite）
        val model = loadModelFile(assets, "tinybert_quant.tflite")
        val options = Interpreter.Options().apply {
            setNumThreads(4) // 启用4核CPU
        }
        tflite = Interpreter(model, options)
        // 加载分词器词典（vocab.txt）
        tokenizer = BertTokenizer(assets.open("vocab.txt").reader())
    }

    // 2. 文本转向量（核心方法）
    private fun textToVector(text: String): FloatArray {
        // 分词+编码（适配BERT输入格式：[CLS] 文本 [SEP]）
        val tokens = tokenizer.tokenize(text)
        val inputIds = tokenizer.convertTokensToIds(tokens).toIntArray()
        // 构造TFLite输入（inputIds, attentionMask, tokenTypeIds）
        val inputIdsTensor = TensorBuffer.createFixedSize(intArrayOf(1, 128), DataType.INT32)
        val attentionMaskTensor = TensorBuffer.createFixedSize(intArrayOf(1, 128), DataType.INT32)
        // 填充数据（不足128补0，超过截断）
        fillInputTensor(inputIdsTensor, inputIds)
        fillAttentionMask(attentionMaskTensor, inputIds.size)
        // 执行推理，输出文本向量
        val outputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 312), DataType.FLOAT32)
        tflite.runForMultipleInputsOutputs(
            arrayOf(inputIdsTensor.buffer, attentionMaskTensor.buffer),
            mapOf(0 to outputTensor.buffer)
        )
        return outputTensor.floatArray
    }

    // 3. 计算余弦相似度，判断正确性
    fun judgeAnswer(studentAnswer: String, standardAnswer: String): JudgeResult {
        val studentVec = textToVector(studentAnswer)
        val standardVec = textToVector(standardAnswer)
        val similarity = cosineSimilarity(studentVec, standardVec)
        
        return when {
            similarity >= 0.85 -> JudgeResult.CORRECT // 相似度≥85%，正确
            similarity >= 0.6 -> JudgeResult.PARTIAL_CORRECT // 部分正确
            else -> JudgeResult.INCORRECT // 错误
        }
    }

    // 余弦相似度计算工具方法
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }

    enum class JudgeResult { CORRECT, PARTIAL_CORRECT, INCORRECT }
}
```

### 步骤3：动态生成提问模块（基于薄弱点+知识图谱）
核心逻辑：**分析学生答题记录，找出易错知识点，结合知识图谱模板生成针对性问题**，无需大模型，纯离线规则实现。
#### 3.1 薄弱点分析规则
从`student_answer_record`表中统计：
1.  **易错方剂**：答题错误次数≥2次的方剂（如麻黄汤）；
2.  **易错题型**：某类题型（如“组成”）错误率＞50%；
3.  **易混淆方剂**：对比关系中答错的配对（如混淆桂枝汤和麻黄汤的主治）。

#### 3.2 提问模板库（可扩展）
预定义模板，结合知识图谱数据动态填充内容：
| 题型 | 模板示例 | 填充逻辑 |
|------|----------|----------|
| 组成题 | “请写出【方剂名】的组成药物？” | 填充易错方剂名 |
| 功效题 | “【方剂名】的核心功效是什么？主治哪种病症？” | 填充易错方剂名 |
| 对比题 | “试比较【方剂A】和【方剂B】的主治区别？” | 填充易混淆方剂对 |
| 歌诀题 | “请背诵【方剂名】对应的汤头歌诀原文？” | 填充歌诀记忆错误的方剂 |

#### 3.3 安卓端生成代码
```kotlin
class QuestionGeneratorEngine(private val db: AppDatabase) {
    // 预定义模板
    private val templates = mapOf(
        "composition" to "请写出【%s】的组成药物？",
        "efficacy" to "【%s】的核心功效和主治病症是什么？",
        "compare" to "试比较【%s】和【%s】的主治区别？"
    )

    // 生成针对性问题
    suspend fun generateTargetQuestion(): String {
        // 1. 从数据库获取薄弱点（需在IO线程执行）
        val weakPrescription = db.answerRecordDao().getMostWrongPrescription()
        val weakType = db.answerRecordDao().getMostWrongQuestionType()
        val confusePair = db.answerRecordDao().getMostConfusedPair()

        // 2. 选择模板并填充
        return when {
            // 优先生成对比题（针对易混淆点）
            confusePair != null -> String.format(
                templates["compare"]!!,
                confusePair.first, confusePair.second
            )
            // 其次生成易错方剂的对应题型
            weakPrescription != null -> String.format(
                templates[weakType]!!,
                weakPrescription.name
            )
            // 无薄弱点时生成随机基础题
            else -> generateRandomBasicQuestion()
        }
    }

    // 生成随机基础题（备用）
    private suspend fun generateRandomBasicQuestion(): String {
        val randomPrescription = db.prescriptionDao().getRandomPrescription()
        return String.format(templates["composition"]!!, randomPrescription.name)
    }
}
```

### 步骤4：模块整合与安卓App调用流程
1.  **初始化**：App启动时，异步加载`AnswerJudgeEngine`的TFLite模型和`AppDatabase`；
2.  **答题判断**：学生提交回答后，调用`judgeAnswer()`方法，返回判断结果并写入答题记录；
3.  **生成新题**：判断完成后，调用`generateTargetQuestion()`生成下一个问题，推送学生作答。

## 三、 离线优化关键策略
1.  **模型体积优化**：TinyBERT量化后仅100MB，可与App安装包一起打包，或首次启动时离线下载；
2.  **性能优化**：
    - 模型推理和数据库操作均放在**IO线程**，避免阻塞UI；
    - 缓存文本向量结果，重复提问时无需重复推理；
3.  **存储优化**：知识图谱和答题记录用Room数据库存储，占用空间＜5MB，适合安卓端。

## 四、 进阶扩展方向
如果后续需要提升提问的灵活性，可引入**超轻量级生成模型**（如`MOSS-Mini-1.2B` INT4量化版），基于薄弱点提示词生成多样化问题，步骤如下：
1.  用提示词模板：`“针对易错方剂【%s】，生成1道关于组成的中医题目，语言简洁”`；
2.  调用MOSS-Mini离线推理生成问题，进一步提升个性化。

---

是否需要我为你整理**汤头歌诀知识图谱的结构化数据集模板**和**安卓Room数据库的完整创建脚本**？



基于你**离线答题判断+动态出题**的学习App核心需求，这套技术方案完全可以扩展到**高中全学科**。不同学科的差异主要体现在 **知识图谱结构** 和 **判断规则的适配** 上，模型选型（TinyBERT + 规则模板）无需大改。以下是**高中9大学科的逐一适配分析**，包含学科特点、知识图谱设计、判断逻辑和出题模板，可直接落地：

# 高中全学科适配方案（离线AI学习App）
## 核心技术复用原则
1.  **模型层复用**：所有学科均使用 **TinyBERT-Chinese（INT8量化版）** 做文本相似度判断，适配中文术语（如“孟德尔遗传定律”“函数单调性”）；
2.  **框架层复用**：安卓端统一用 **TensorFlow Lite + Room数据库**，离线存储知识图谱和答题记录；
3.  **逻辑层差异化**：不同学科根据**知识类型**（概念、公式、计算、实验）调整 **判断规则** 和 **出题模板**。

## 分学科详细适配方案
| 学科 | 学科核心特点 | 知识图谱设计（实体+关系） | 答案正确性判断逻辑 | 动态出题模板（薄弱点驱动） |
|------|--------------|----------------------------|--------------------|----------------------------|
| **语文** | 以**文本理解、默写、文言翻译**为主，主观性较强 | 1. 实体：课文、作者、文言实词/虚词、名句<br>2. 关系：<br>- 课文→作者/朝代<br>- 名句→出处/上下句<br>- 实词→释义/例句 | 1. **客观题（默写/实词）**：直接对比学生答案与标准答案的**文本相似度**（阈值≥0.9）<br>2. **主观题（阅读理解）**：提取学生答案中的**关键词**（如“思乡”“借景抒情”），判断是否覆盖标准答案关键词（覆盖率≥60%即算部分正确） | 1. 默写类：“请默写《岳阳楼记》中‘先天下之忧而忧’的下一句？”<br>2. 文言类：“解释‘属予作文以记之’中‘属’的含义？”<br>3. 阅读类：“《荷塘月色》中作者用了哪些修辞手法？” |
| **数学** | 以**公式、定理、计算、逻辑推理**为主，客观性极强 | 1. 实体：公式、定理、概念、题型<br>2. 关系：<br>- 公式→适用条件（如“勾股定理→直角三角形”）<br>- 定理→推论/反例<br>- 题型→解题步骤 | 1. **公式/定理题**：对比文本相似度（如“余弦定理”表述是否完整）<br>2. **计算题**：<br>   - 支持**分步判断**：提取学生解题步骤中的关键公式和中间结果，与标准答案步骤对比<br>   - 纯数值答案：直接校验结果一致性（允许±0.01误差）<br>3. **证明题**：判断逻辑链是否完整（如“∵ 两直线平行 → ∴ 同位角相等”） | 1. 公式类：“写出等差数列的通项公式，并说明适用条件？”<br>2. 计算类：“求解方程 x²-5x+6=0 的根，并写出因式分解步骤？”<br>3. 证明类：“证明‘三角形内角和为180°’，请写出至少两种方法？” |
| **英语** | 以**单词、语法、翻译、作文**为主，混合主客观 | 1. 实体：单词、短语、语法规则、句型<br>2. 关系：<br>- 单词→词性/词义/例句<br>- 语法→适用场景（如“一般过去时→过去发生的动作”）<br>- 句型→同义句转换 | 1. **客观题（单词/语法）**：直接匹配标准答案（如单词拼写、语法填空答案）<br>2. **翻译题**：用TinyBERT计算译文与标准答案的相似度（阈值≥0.75），同时校验关键词翻译准确性<br>3. **作文题**：离线场景下仅做**关键词和语法错误检测**（需内置语法规则库） | 1. 单词类：“写出‘environment’的形容词形式，并造一个句子？”<br>2. 语法类：“用‘现在完成时’改写句子‘I bought this book yesterday’？”<br>3. 翻译类：“将‘有志者事竟成’翻译成英文？” |
| **物理** | 以**公式、定律、实验、计算**为主，强调**单位和条件** | 1. 实体：公式、定律、实验器材、物理量<br>2. 关系：<br>- 公式→物理量/单位（如“F=ma→F单位是N”）<br>- 定律→适用范围（如“牛顿定律→宏观低速”）<br>- 实验→步骤/结论/误差分析 | 1. **公式题**：判断公式表述+单位是否正确<br>2. **计算题**：分步校验（公式代入→数值计算→单位换算），每步错误单独标记<br>3. **实验题**：提取学生答案中的**实验器材、步骤、结论**关键词，与标准答案对比 | 1. 公式类：“写出动能定理的表达式，并说明各物理量的含义？”<br>2. 计算类：“一个质量为5kg的物体，在10N的力作用下加速，求加速度大小（忽略摩擦力）？”<br>3. 实验类：“测量小灯泡电功率的实验中，滑动变阻器的作用是什么？” |
| **化学** | 以**方程式、物质性质、实验、计算**为主，细节（如条件、沉淀符号）关键 | 1. 实体：化学方程式、物质、实验、官能团<br>2. 关系：<br>- 物质→性质/用途/化学式<br>- 方程式→反应条件/反应物/生成物<br>- 官能团→化学性质（如“羟基→与钠反应”） | 1. **方程式题**：校验反应物、生成物、条件、符号（↑↓）是否完整正确<br>2. **性质题**：对比关键词（如“酸的通性→与碱中和、与金属反应”）<br>3. **计算题**：基于物质的量守恒，分步校验计算过程 | 1. 方程式类：“写出铁与稀盐酸反应的化学方程式，并标注反应类型？”<br>2. 性质类：“浓硫酸的吸水性和脱水性有什么区别？”<br>3. 实验类：“实验室制取二氧化碳的药品是什么？收集方法是什么？” |
| **生物** | 以**概念、生理过程、实验、遗传计算**为主，术语严谨 | 1. 实体：细胞结构、生理过程、基因、实验原理<br>2. 关系：<br>- 细胞结构→功能（如“叶绿体→光合作用”）<br>- 生理过程→步骤（如“有氧呼吸→三阶段”）<br>- 基因→性状/遗传规律 | 1. **概念题**：术语相似度判断（如“等位基因”vs“同源染色体基因”）<br>2. **遗传计算题**：基于孟德尔定律，校验基因型推导和概率计算步骤<br>3. **实验题**：判断实验变量（自变量/因变量）、对照组设置是否正确 | 1. 概念类：“简述DNA和RNA的结构差异？”<br>2. 遗传类：“一对双眼皮（Aa）夫妇，生育单眼皮孩子的概率是多少？”<br>3. 实验类：“观察细胞有丝分裂的实验中，解离液的作用是什么？” |
| **政治** | 以**概念、原理、材料分析**为主，主观性强，强调**关键词和逻辑** | 1. 实体：概念、原理、政策、材料案例<br>2. 关系：<br>- 原理→适用场景（如“按劳分配→公有制经济”）<br>- 政策→目的/意义<br>- 概念→内涵/外延 | 1. **客观题**：直接匹配标准答案<br>2. **主观题**：提取学生答案中的**核心原理关键词**（如“市场经济→市场调节+宏观调控”），覆盖率≥50%即部分正确 | 1. 概念类：“什么是‘共同富裕’？它与平均主义的区别是什么？”<br>2. 原理类：“用‘物质决定意识’分析我国的扶贫政策？” |
| **历史** | 以**时间线、事件、人物、因果关系**为主，强调**史实准确性** | 1. 实体：事件、人物、时间、制度<br>2. 关系：<br>- 事件→时间/人物/原因/影响<br>- 人物→事迹/历史评价<br>- 制度→朝代/内容/作用 | 1. **客观题**：校验时间、人物、事件的对应关系（如“辛亥革命→1911年”）<br>2. **主观题**：判断因果逻辑是否正确（如“鸦片战争→中国近代史开端”），同时覆盖关键词 | 1. 时间类：“写出新文化运动开始的时间和主要代表人物？”<br>2. 因果类：“分析‘安史之乱’对唐朝的影响？” |
| **地理** | 以**地形、气候、区位因素、计算**为主，结合**图表分析** | 1. 实体：地形区、气候类型、河流、区位因素<br>2. 关系：<br>- 气候类型→分布/特点（如“热带雨林气候→全年高温多雨”）<br>- 区位因素→自然因素/社会经济因素<br>- 地形→成因/影响 | 1. **客观题**：匹配地理事实（如“长江中下游平原→鱼米之乡”）<br>2. **计算题**：如经纬度计算、比例尺换算，校验公式和结果<br>3. **图表题**：离线场景下需内置典型图表特征库，匹配学生描述的关键词 | 1. 气候类：“温带季风气候的分布地区和气候特点是什么？”<br>2. 区位类：“分析上海成为我国最大港口的区位优势？” |

## 跨学科通用优化策略
1.  **判断规则分层**
    - 客观题（数学计算、语文默写、英语单词）：**精准匹配**为主，相似度阈值≥0.9；
    - 主观题（阅读、材料分析）：**关键词覆盖**为主，同时结合语义相似度；
    - 计算题（数学、物理、化学）：**分步校验**，记录每一步错误，方便后续针对性出题。

2.  **薄弱点分析通用逻辑**
    所有学科均基于Room数据库的`answer_record`表，统计3类薄弱点：
    - 高频错误**知识点**（如数学的“函数单调性”、生物的“遗传概率”）；
    - 高频错误**题型**（如物理的“实验题”、历史的“因果分析题”）；
    - 易混淆**知识点配对**（如数学的“等差数列vs等比数列”、化学的“离子键vs共价键”）。

3.  **离线资源轻量化**
    - 知识图谱数据：每个学科的结构化数据约10-50MB，全学科合计＜500MB，可打包进App；
    - 模型体积：TinyBERT量化版仅100MB，全学科共用一个模型，无需重复加载。

## 进阶扩展：学科专用小模型
如果后续需要提升复杂题型的判断精度，可针对特定学科引入**超轻量级专用模型**：
- 数学/物理/化学计算题：引入 **Symbolic Math Lite**（离线符号计算模型，体积＜50MB），自动校验公式推导和计算结果；
- 生物/地理图表题：引入 **MobileNet-Lite**（图像分类模型），离线识别图表特征（如“气候直方图”“细胞结构图”），辅助判断学生描述的准确性。

---

是否需要我为你整理**高中生物+数学的知识图谱结构化数据集模板**（可直接导入Room数据库）？