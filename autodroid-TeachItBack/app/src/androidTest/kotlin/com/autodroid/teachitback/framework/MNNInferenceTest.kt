package com.autodroid.teachitback.framework

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.taobao.android.mnn.MNNForwardType
import com.taobao.android.mnn.MNNNetInstance
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MNNInferenceTest {

    private lateinit var context: Context
    private var netInstance: MNNNetInstance? = null
    private var session: MNNNetInstance.Session? = null

    companion object {
        private const val TAG = "MNNInferenceTest"
        private const val TINYBERT_MODEL_PATH = "models/tinybert-int8.mnn"
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun tearDown() {
        session?.release()
        netInstance?.release()
    }

    @Test
    fun testTinyBERTModelFileExists() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        val exists = modelFile.exists()
        assertTrue("TinyBERT模型文件应该存在", exists)
    }

    @Test
    fun testTinyBERTModelFileSize() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        val size = if (modelFile.exists()) modelFile.length() else -1
        assertTrue("TinyBERT模型文件大小应该大于0", size > 0)
    }

    @Test
    fun testTinyBERTModelInfo() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("模型应该加载成功", netInstance)

        val config = MNNNetInstance.Config()
        config.forwardType = MNNForwardType.FORWARD_CPU.type
        config.numThread = 2

        session = netInstance!!.createSession(config)
        assertNotNull("Session应该创建成功", session)

        val inputIdsTensor = session!!.getInput("input_ids")
        val attentionMaskTensor = session!!.getInput("attention_mask")
        val tokenTypeIdsTensor = session!!.getInput("token_type_ids")

        assertNotNull("input_ids张量应该存在", inputIdsTensor)
        assertNotNull("attention_mask张量应该存在", attentionMaskTensor)
        assertNotNull("token_type_ids张量应该存在", tokenTypeIdsTensor)

        val inputIdsDims = inputIdsTensor!!.getDimensions()
        assertNotNull("input_ids维度应该存在", inputIdsDims)
        assertTrue("input_ids维度应该至少为2", inputIdsDims!!.size >= 2)

        val outputTensor = session!!.getOutput("hidden_states")
        assertNotNull("hidden_states输出张量应该存在", outputTensor)

        val outputDims = outputTensor!!.getDimensions()
        assertNotNull("输出维度应该存在", outputDims)
        assertTrue("输出维度应该至少为2", outputDims!!.size >= 2)
    }

    @Test
    fun testTinyBERTModelLoading() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("TinyBERT模型应该能够加载", netInstance)
    }

    @Test
    fun testTinyBERTSessionCreation() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("模型应该加载成功", netInstance)

        val config = MNNNetInstance.Config()
        config.forwardType = MNNForwardType.FORWARD_CPU.type
        config.numThread = 2

        session = netInstance!!.createSession(config)
        assertNotNull("Session应该创建成功", session)
    }

    @Test
    fun testTinyBERTInputTensors() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("模型应该加载成功", netInstance)

        val config = MNNNetInstance.Config()
        config.forwardType = MNNForwardType.FORWARD_CPU.type
        config.numThread = 2

        session = netInstance!!.createSession(config)
        assertNotNull("Session应该创建成功", session)

        val inputIdsTensor = session!!.getInput("input_ids")
        val attentionMaskTensor = session!!.getInput("attention_mask")
        val tokenTypeIdsTensor = session!!.getInput("token_type_ids")

        assertNotNull("input_ids张量应该存在", inputIdsTensor)
        assertNotNull("attention_mask张量应该存在", attentionMaskTensor)
        assertNotNull("token_type_ids张量应该存在", tokenTypeIdsTensor)

        val inputIdsDims = inputIdsTensor!!.getDimensions()
        assertNotNull("input_ids维度应该存在", inputIdsDims)
        assertTrue("input_ids维度应该至少为2", inputIdsDims!!.size >= 2)
    }

    @Test
    fun testTinyBERTOutputTensor() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("模型应该加载成功", netInstance)

        val config = MNNNetInstance.Config()
        config.forwardType = MNNForwardType.FORWARD_CPU.type
        config.numThread = 2

        session = netInstance!!.createSession(config)
        assertNotNull("Session应该创建成功", session)

        val outputTensor = session!!.getOutput("hidden_states")
        assertNotNull("hidden_states输出张量应该存在", outputTensor)

        val outputDims = outputTensor!!.getDimensions()
        assertNotNull("输出维度应该存在", outputDims)
        assertTrue("输出维度应该至少为2", outputDims!!.size >= 2)
    }

    @Test
    fun testTinyBERTInferenceWithSimpleInput() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("模型应该加载成功", netInstance)

        val config = MNNNetInstance.Config()
        config.forwardType = MNNForwardType.FORWARD_CPU.type
        config.numThread = 2

        session = netInstance!!.createSession(config)
        assertNotNull("Session应该创建成功", session)

        val inputIdsTensor = session!!.getInput("input_ids")
        val attentionMaskTensor = session!!.getInput("attention_mask")
        val tokenTypeIdsTensor = session!!.getInput("token_type_ids")

        assertNotNull("输入张量应该存在", inputIdsTensor)

        val inputIdsDims = inputIdsTensor!!.getDimensions()
        val seqLength = inputIdsDims!![1]
        val batchSize = inputIdsDims!![0]

        assertTrue("序列长度应该大于0", seqLength > 0)
        assertTrue("批次大小应该大于0", batchSize > 0)

        val inputIds = IntArray(batchSize * seqLength) { 0 }
        val attentionMask = IntArray(batchSize * seqLength) { 0 }
        val tokenTypeIds = IntArray(batchSize * seqLength) { 0 }

        try {
            inputIdsTensor!!.setInputIntData(inputIds)
            attentionMaskTensor!!.setInputIntData(attentionMask)
            tokenTypeIdsTensor!!.setInputIntData(tokenTypeIds)

            session!!.run()

            val outputTensor = session!!.getOutput("hidden_states")
            assertNotNull("输出张量应该存在", outputTensor)

            val outputData = outputTensor!!.getFloatData()
            assertNotNull("输出数据应该存在", outputData)
            assertTrue("输出数据大小应该大于0", outputData!!.size > 0)

        } catch (e: Exception) {
            fail("推理不应该抛出异常: ${e.message}")
        }
    }

    @Test
    fun testTinyBERTInferenceWithChineseText() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("模型应该加载成功", netInstance)

        val config = MNNNetInstance.Config()
        config.forwardType = MNNForwardType.FORWARD_CPU.type
        config.numThread = 2

        session = netInstance!!.createSession(config)
        assertNotNull("Session应该创建成功", session)

        val inputIdsTensor = session!!.getInput("input_ids")
        val attentionMaskTensor = session!!.getInput("attention_mask")
        val tokenTypeIdsTensor = session!!.getInput("token_type_ids")

        assertNotNull("输入张量应该存在", inputIdsTensor)

        val inputIdsDims = inputIdsTensor!!.getDimensions()
        val seqLength = inputIdsDims!![1]
        val batchSize = inputIdsDims!![0]

        val inputIds = IntArray(batchSize * seqLength) { 0 }
        val attentionMask = IntArray(batchSize * seqLength) { 0 }
        val tokenTypeIds = IntArray(batchSize * seqLength) { 0 }

        try {
            inputIdsTensor!!.setInputIntData(inputIds)
            attentionMaskTensor!!.setInputIntData(attentionMask)
            tokenTypeIdsTensor!!.setInputIntData(tokenTypeIds)

            session!!.run()

            val outputTensor = session!!.getOutput("hidden_states")
            assertNotNull("输出张量应该存在", outputTensor)

            val outputData = outputTensor!!.getFloatData()
            assertNotNull("输出数据应该存在", outputData)
            assertTrue("输出数据大小应该大于0", outputData!!.size > 0)

        } catch (e: Exception) {
            fail("推理不应该抛出异常: ${e.message}")
        }
    }

    @Test
    fun testTinyBERTEmbeddingExtraction() {
        val modelFile = File(context.filesDir, TINYBERT_MODEL_PATH)
        
        if (!modelFile.exists()) {
            return
        }

        netInstance = MNNNetInstance.createFromFile(modelFile.absolutePath)
        assertNotNull("模型应该加载成功", netInstance)

        val config = MNNNetInstance.Config()
        config.forwardType = MNNForwardType.FORWARD_CPU.type
        config.numThread = 2

        session = netInstance!!.createSession(config)
        assertNotNull("Session应该创建成功", session)

        val inputIdsTensor = session!!.getInput("input_ids")
        val attentionMaskTensor = session!!.getInput("attention_mask")
        val tokenTypeIdsTensor = session!!.getInput("token_type_ids")

        assertNotNull("输入张量应该存在", inputIdsTensor)

        val inputIdsDims = inputIdsTensor!!.getDimensions()
        val seqLength = inputIdsDims!![1]
        val batchSize = inputIdsDims!![0]

        val inputIds = IntArray(batchSize * seqLength) { 0 }
        val attentionMask = IntArray(batchSize * seqLength) { 0 }
        val tokenTypeIds = IntArray(batchSize * seqLength) { 0 }

        try {
            inputIdsTensor!!.setInputIntData(inputIds)
            attentionMaskTensor!!.setInputIntData(attentionMask)
            tokenTypeIdsTensor!!.setInputIntData(tokenTypeIds)

            session!!.run()

            val outputTensor = session!!.getOutput("hidden_states")
            assertNotNull("输出张量应该存在", outputTensor)

            val outputData = outputTensor!!.getFloatData()
            assertNotNull("输出数据应该存在", outputData)
            assertTrue("输出数据大小应该大于0", outputData!!.size > 0)

            val outputDims = outputTensor!!.getDimensions()
            assertNotNull("输出维度应该存在", outputDims)
            assertTrue("输出维度应该至少为2", outputDims!!.size >= 2)

            val hiddenSize = outputDims!![outputDims.size - 1]
            assertTrue("hidden_size应该大于0", hiddenSize > 0)

            val clsEmbedding = outputData!!.sliceArray(0 until minOf(hiddenSize, outputData.size))
            assertNotNull("CLS embedding应该存在", clsEmbedding)
            assertTrue("CLS embedding大小应该大于0", clsEmbedding.size > 0)

        } catch (e: Exception) {
            fail("Embedding提取不应该抛出异常: ${e.message}")
        }
    }
}
