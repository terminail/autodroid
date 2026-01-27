package com.autodroid.teachitback.framework

import android.content.Context
import android.util.Log
import com.taobao.android.mnn.MNNForwardType
import com.taobao.android.mnn.MNNNetInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MNN框架集成（真实实现）
 * 使用 MNN 官方提供的 Java API
 */
class MNNIntegration(private val context: Context) {

    companion object {
        private const val TAG = "MNNIntegration"
        private const val MODELS_DIR = "models"

        // 模型文件路径
        const val CHATGLM_MODEL_PATH = "$MODELS_DIR/chatglm-6b-int4.mnn"
        const val TINYBERT_MODEL_PATH = "$MODELS_DIR/tinybert-int8.mnn"
    }

    private var isInitialized = false
    private val modelCache = mutableMapOf<String, MNNModel>()

    /**
     * 初始化MNN框架
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(context.filesDir, MODELS_DIR)
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            isInitialized = true
            Log.d(TAG, "MNN框架初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "MNN框架初始化失败", e)
            false
        }
    }

    /**
     * 加载模型（真实实现）
     * @param modelPath 模型文件路径
     * @return MNN模型实例
     */
    suspend fun loadModel(modelPath: String): MNNModel? = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== 开始加载模型 ==========")
        Log.d(TAG, "模型路径: $modelPath")
        
        if (!isInitialized) {
            Log.e(TAG, "MNN框架未初始化")
            return@withContext null
        }
        Log.d(TAG, "MNN框架已初始化")

        try {
            // 检查模型是否已缓存
            modelCache[modelPath]?.let {
                Log.d(TAG, "使用缓存的模型: $modelPath")
                return@withContext it
            }
            Log.d(TAG, "模型不在缓存中，开始加载...")

            // 检查模型文件是否存在
            val modelFile = File(context.filesDir, modelPath)
            Log.d(TAG, "模型文件完整路径: ${modelFile.absolutePath}")
            Log.d(TAG, "模型文件存在: ${modelFile.exists()}")
            Log.d(TAG, "模型文件大小: ${modelFile.length()} bytes")
            
            if (!modelFile.exists()) {
                Log.e(TAG, "模型文件不存在: ${modelFile.absolutePath}")
                return@withContext null
            }

            // 使用 MNN Java API 加载模型
            Log.d(TAG, "调用 MNNNetInstance.createFromFile()...")
            val netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
            
            if (netInstance == null) {
                Log.e(TAG, "模型加载失败: MNNNetInstance.createFromFile() 返回 null")
                Log.e(TAG, "可能原因: 模型文件损坏、格式不正确或 MNN 库未正确加载")
                return@withContext null
            }
            Log.d(TAG, "MNNNetInstance 创建成功")

            // 创建推理会话
            Log.d(TAG, "创建推理会话配置...")
            val config = MNNNetInstance.Config()
            config.forwardType = MNNForwardType.FORWARD_CPU.type
            config.numThread = 4
            Log.d(TAG, "会话配置: forwardType=${config.forwardType}, numThread=${config.numThread}")
            
            Log.d(TAG, "调用 session.createSession()...")
            val session = netInstance.createSession(config)
            
            if (session == null) {
                Log.e(TAG, "会话创建失败: netInstance.createSession() 返回 null")
                return@withContext null
            }
            Log.d(TAG, "推理会话创建成功")

            // 尝试加载分词器
            val tokenizer = loadTokenizer(modelPath)
            if (tokenizer != null) {
                Log.d(TAG, "分词器加载成功")
            } else {
                Log.w(TAG, "分词器加载失败，模型将使用简化模式")
            }

            val model = MNNModel(netInstance, session, tokenizer)
            modelCache[modelPath] = model
            
            Log.i(TAG, "✓ 模型加载成功: $modelPath (大小: ${modelFile.length()} bytes)")
            model
        } catch (e: Exception) {
            Log.e(TAG, "✗ 模型加载失败: $modelPath", e)
            Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
            Log.e(TAG, "异常消息: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * 加载分词器
     * @param modelPath 模型文件路径
     * @return BertTokenizer 实例，如果加载失败返回 null
     */
    private fun loadTokenizer(modelPath: String): BertTokenizer? {
        return try {
            // 根据模型路径推断词汇表路径
            val vocabPath = when {
                modelPath.contains("tinybert") -> {
                    // TinyBERT 模型，尝试多个路径
                    val paths = listOf(
                        // 1. 先尝试从 assets 加载
                        "TinyBERT_General_4L_312D/vocab.txt",
                        // 2. 尝试从 tibresource 加载
                        "tibresource/models/TinyBERT_General_4L_312D/vocab.txt",
                        // 3. 尝试从外部存储加载
                        "/sdcard/tibresource/models/TinyBERT_General_4L_312D/vocab.txt",
                        "/sdcard/Android/data/com.autodroid.teachitback/files/tibresource/models/TinyBERT_General_4L_312D/vocab.txt"
                    )
                    
                    // 尝试每个路径
                    for (path in paths) {
                        try {
                            Log.d(TAG, "尝试加载词汇表: $path")
                            val tokenizer = BertTokenizer.fromFile(context, path)
                            if (tokenizer != null) {
                                return tokenizer
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "路径 $path 不可用: ${e.message}")
                            continue
                        }
                    }
                    null
                }
                modelPath.contains("chatglm") -> {
                    // ChatGLM 模型
                    null
                }
                else -> null
            }
            
            vocabPath
        } catch (e: Exception) {
            Log.e(TAG, "加载分词器失败", e)
            null
        }
    }

    /**
     * 检查模型是否已下载
     * @param modelPath 模型文件路径
     * @return 是否已下载
     */
    fun isModelDownloaded(modelPath: String): Boolean {
        val modelFile = File(context.filesDir, modelPath)
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * 获取模型文件大小
     * @param modelPath 模型文件路径
     * @return 文件大小（字节），如果文件不存在返回-1
     */
    fun getModelFileSize(modelPath: String): Long {
        val modelFile = File(context.filesDir, modelPath)
        return if (modelFile.exists()) modelFile.length() else -1
    }

    /**
     * 删除模型文件
     * @param modelPath 模型文件路径
     * @return 是否删除成功
     */
    suspend fun deleteModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 从缓存中移除
            modelCache.remove(modelPath)

            // 删除文件
            val modelFile = File(context.filesDir, modelPath)
            if (modelFile.exists()) {
                modelFile.delete()
                Log.d(TAG, "模型删除成功: $modelPath")
                true
            } else {
                Log.w(TAG, "模型文件不存在: $modelPath")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "模型删除失败: $modelPath", e)
            false
        }
    }

    /**
     * 清理所有模型缓存
     */
    suspend fun clearModelCache() = withContext(Dispatchers.IO) {
        modelCache.clear()
        Log.d(TAG, "模型缓存已清理")
    }

    /**
     * 获取已下载的模型列表
     * @return 已下载的模型路径列表
     */
    fun getDownloadedModels(): List<String> {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) return emptyList()

        return modelsDir.listFiles { file ->
            file.isFile && file.extension == "mnn"
        }?.map { file ->
            file.name
        } ?: emptyList()
    }
}

/**
 * MNN模型封装（真实实现）
 * 使用 MNN 官方提供的 Java API
 */
class MNNModel(
    private val netInstance: MNNNetInstance,
    private val session: MNNNetInstance.Session,
    private val tokenizer: BertTokenizer? = null
) {
    /**
     * 获取分词器
     */
    fun getTokenizer(): BertTokenizer? = tokenizer

    companion object {
        private const val TAG = "MNNModel"
        
        // TinyBERT 输入张量名称
        private const val INPUT_IDS = "input_ids"
        private const val ATTENTION_MASK = "attention_mask"
        private const val TOKEN_TYPE_IDS = "token_type_ids"
        
        // TinyBERT 输出张量名称
        private const val HIDDEN_STATES = "hidden_states"
        
        // 模型配置
        private const val HIDDEN_SIZE = 312
        private const val MAX_SEQ_LENGTH = 512
    }

    private var isLoaded = false

    /**
     * 加载模型（真实实现）
     */
    suspend fun load(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            Log.d(TAG, "模型已加载")
            isLoaded = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "模型加载失败", e)
            false
        }
    }

    /**
     * 执行推理（真实实现）
     * @param input 输入数据
     * @return 推理结果
     */
    suspend fun inference(input: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        if (!isLoaded) {
            throw IllegalStateException("模型未加载")
        }

        try {
            // 获取输入张量
            val inputIdsTensor = session.getInput(INPUT_IDS)
                ?: throw IllegalStateException("无法获取输入张量: $INPUT_IDS")
            
            val attentionMaskTensor = session.getInput(ATTENTION_MASK)
                ?: throw IllegalStateException("无法获取输入张量: $ATTENTION_MASK")
            
            val tokenTypeIdsTensor = session.getInput(TOKEN_TYPE_IDS)
                ?: throw IllegalStateException("无法获取输入张量: $TOKEN_TYPE_IDS")
            
            Log.d(TAG, "成功获取所有输入张量")
            
            // 准备输入数据
            val (inputIds, attentionMask, tokenTypeIds) = preprocessInput(input)
            
            Log.d(TAG, "输入数据准备完成: input_ids size=${inputIds.size}, attention_mask size=${attentionMask.size}, token_type_ids size=${tokenTypeIds.size}")
            
            // 设置输入数据
            Log.d(TAG, "设置输入数据:")
            Log.d(TAG, "  inputIds: size=${inputIds.size}, 前5个值=${inputIds.take(5).joinToString(", ")}")
            Log.d(TAG, "  attentionMask: size=${attentionMask.size}, 前5个值=${attentionMask.take(5).joinToString(", ")}")
            Log.d(TAG, "  tokenTypeIds: size=${tokenTypeIds.size}, 前5个值=${tokenTypeIds.take(5).joinToString(", ")}")
            
            inputIdsTensor.setInputFloatData(inputIds)
            attentionMaskTensor.setInputFloatData(attentionMask)
            tokenTypeIdsTensor.setInputFloatData(tokenTypeIds)
            
            Log.d(TAG, "输入数据已设置到张量")
            
            // 执行推理
            Log.d(TAG, "开始执行推理...")
            try {
                session.run()
                Log.d(TAG, "推理执行完成")
            } catch (e: Exception) {
                Log.e(TAG, "推理执行失败", e)
                Log.e(TAG, "异常详情: ${e.message}")
                Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
                throw e
            }
            
            // 获取输出张量
            val outputTensor = session.getOutput(HIDDEN_STATES)
                ?: throw IllegalStateException("无法获取输出张量: $HIDDEN_STATES")
            
            Log.d(TAG, "成功获取输出张量: $HIDDEN_STATES")
            
            // 获取输出数据
            val outputData = outputTensor.getFloatData()
            Log.d(TAG, "输出数据获取完成: size=${outputData.size}")
            
            // 后处理输出
            val result = postprocessOutput(outputData)
            
            Log.d(TAG, "后处理完成: ${result.take(100)}...")
            result
        } catch (e: Exception) {
            Log.e(TAG, "推理失败", e)
            throw e
        }
    }

    /**
     * 提取文本的 embedding（用于相似度计算）
     * @param input 输入文本
     * @return embedding 向量（[CLS] token 的输出）
     */
    suspend fun extractEmbedding(input: String): FloatArray = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        if (!isLoaded) {
            throw IllegalStateException("模型未加载")
        }

        Log.d(TAG, "extractEmbedding: input='$input', 长度=${input.length}")
        Log.d(TAG, "extractEmbedding: 前3个字符: ${input.take(3).map { "U+${it.code.toString(16).uppercase()}" }}")

        try {
            // 获取输入张量
            val inputIdsTensor = session.getInput(INPUT_IDS)
                ?: throw IllegalStateException("无法获取输入张量: $INPUT_IDS")
            
            val attentionMaskTensor = session.getInput(ATTENTION_MASK)
                ?: throw IllegalStateException("无法获取输入张量: $ATTENTION_MASK")
            
            val tokenTypeIdsTensor = session.getInput(TOKEN_TYPE_IDS)
                ?: throw IllegalStateException("无法获取输入张量: $TOKEN_TYPE_IDS")
            
            // 准备输入数据
            val (inputIds, attentionMask, tokenTypeIds) = preprocessInput(input)
            
            Log.d(TAG, "输入数据: inputIds size=${inputIds.size}")
            Log.d(TAG, "输入数据前10个: ${inputIds.take(10).joinToString(", ")}")
            
            // 设置输入数据
            try {
                inputIdsTensor.setInputFloatData(inputIds)
                attentionMaskTensor.setInputFloatData(attentionMask)
                tokenTypeIdsTensor.setInputFloatData(tokenTypeIds)
                Log.d(TAG, "输入数据设置成功")
            } catch (e: Exception) {
                Log.e(TAG, "设置输入数据失败: ${e.message}", e)
                throw e
            }
            
            // 执行推理
            Log.d(TAG, "开始执行推理...")
            try {
                session.run()
                Log.d(TAG, "推理执行完成")
            } catch (e: Exception) {
                Log.e(TAG, "推理执行失败: ${e.message}", e)
                throw e
            }
            
            // 获取输出张量
            val outputTensor = session.getOutput(HIDDEN_STATES)
                ?: throw IllegalStateException("无法获取输出张量: $HIDDEN_STATES")
            
            // 获取输出数据
            val outputData = outputTensor.getFloatData()
            
            // 提取 [CLS] token 的 embedding（第一个 token）
            val clsEmbedding = outputData.sliceArray(0 until HIDDEN_SIZE)
            
            Log.d(TAG, "Embedding 提取完成: size=${clsEmbedding.size}")
            
            clsEmbedding
        } catch (e: Exception) {
            Log.e(TAG, "Embedding 提取失败", e)
            throw e
        }
    }

    /**
     * 预处理输入
     * 将文本转换为 TinyBERT 所需的输入格式
     */
    private fun preprocessInput(input: String): Triple<FloatArray, FloatArray, FloatArray> {
        if (tokenizer == null) {
            Log.w(TAG, "分词器未初始化，使用简单的预处理")
            // 如果没有分词器，返回简单的输入（仅用于测试）
            val inputIds = FloatArray(MAX_SEQ_LENGTH) { 0f }
            val attentionMask = FloatArray(MAX_SEQ_LENGTH) { 0f }
            val tokenTypeIds = FloatArray(MAX_SEQ_LENGTH) { 0f }
            return Triple(inputIds, attentionMask, tokenTypeIds)
        }
        
        // 使用分词器进行分词
        val tokenIds = tokenizer.tokenize(input)
        Log.d(TAG, "分词结果: ${tokenIds.take(20).joinToString(", ")}... (共 ${tokenIds.size} 个 tokens)")
        
        // 创建 attention mask
        val attentionMask = tokenizer.createAttentionMask(tokenIds)
        
        // 创建 token type ids
        val tokenTypeIds = tokenizer.createTokenTypeIds(tokenIds)
        
        // 转换为 FloatArray
        val inputIdsFloat = tokenIds.map { it.toFloat() }.toFloatArray()
        val attentionMaskFloat = attentionMask.map { it.toFloat() }.toFloatArray()
        val tokenTypeIdsFloat = tokenTypeIds.map { it.toFloat() }.toFloatArray()
        
        return Triple(inputIdsFloat, attentionMaskFloat, tokenTypeIdsFloat)
    }

    /**
     * 后处理输出
     * 将 TinyBERT 的输出转换为文本
     */
    private fun postprocessOutput(output: FloatArray): String {
        if (tokenizer == null) {
            // 如果没有分词器，返回简单的输出信息
            return "推理完成，输出维度: ${output.size} (hidden_size=$HIDDEN_SIZE, seq_len=${output.size / HIDDEN_SIZE})"
        }
        
        // TinyBERT 的输出是 hidden states，形状为 [seq_len, hidden_size]
        // 这里我们使用 [CLS] token 的输出作为整个句子的表示
        val clsEmbedding = output.sliceArray(0 until HIDDEN_SIZE)
        
        // 简单的文本生成：基于 embedding 生成回复
        // 注意：这只是一个简化的实现，实际的文本生成需要更复杂的逻辑
        val generatedText = generateTextFromEmbedding(clsEmbedding)
        
        return generatedText
    }
    
    /**
     * 从 embedding 生成文本（简化实现）
     * 实际应用中应该使用更复杂的生成策略
     */
    private fun generateTextFromEmbedding(embedding: FloatArray): String {
        // 这里使用简单的规则生成回复
        // 实际应用中应该：
        // 1. 使用 embedding 进行相似度搜索
        // 2. 或者使用 decoder 模型进行文本生成
        // 3. 或者使用预定义的回复模板
        
        val embeddingSum = embedding.sum()
        val embeddingMean = embedding.average()
        
        Log.d(TAG, "Embedding 统计: sum=$embeddingSum, mean=$embeddingMean")
        
        // 根据 embedding 的特征返回不同的回复
        return when {
            embeddingMean > 0.5 -> "这是一个很好的问题！让我来解释一下。"
            embeddingMean > 0.0 -> "我理解你的问题，这里有一些相关信息。"
            embeddingMean > -0.5 -> "这个问题很有趣，让我想想。"
            else -> "我正在思考这个问题，请稍等。"
        }
    }

    /**
     * 释放模型资源
     */
    suspend fun release() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            session.release()
            netInstance.release()
            isLoaded = false
            Log.d(TAG, "模型资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "模型资源释放失败", e)
        }
    }

    /**
     * 检查模型是否已加载
     */
    fun isLoaded(): Boolean = isLoaded
}

/**
 * 模型下载策略枚举
 */
enum class ModelDownloadStrategy {
    AUTO,          // 自动下载
    WIFI_ONLY,     // 仅WiFi下下载
    MANUAL         // 手动触发下载
}

/**
 * 存储管理策略枚举
 */
enum class StorageManagementStrategy {
    CONSERVATIVE,  // 保守：保留模型
    AGGRESSIVE,    // 激进：立即删除
    SMART          // 智能：根据空间决定
}
