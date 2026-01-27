package com.autodroid.teachitback.framework

import android.content.Context
import com.taobao.android.mnn.MNNForwardType
import com.taobao.android.mnn.MNNNetInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class MNNIntegrationTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockNetInstance: MNNNetInstance

    @Mock
    private lateinit var mockSession: MNNNetInstance.Session

    @Mock
    private lateinit var mockTensor: MNNNetInstance.Session.Tensor

    private lateinit var mnnIntegration: MNNIntegration

    @Before
    fun setup() {
        mnnIntegration = MNNIntegration(mockContext)
    }

    @Test
    fun testInitialize() = runTest {
        val result = mnnIntegration.initialize()
        assertTrue("初始化应该成功", result)
    }

    @Test
    fun testLoadModel_UsesRealMNNAPI() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        val model = mnnIntegration.loadModel(modelPath)
        assertNotNull("模型不应该为空", model)
        assertTrue("模型应该被加载", model?.isLoaded() == true)

        model?.release()
    }

    @Test
    fun testModelInference_UsesRealSessionRun() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        val model = mnnIntegration.loadModel(modelPath)
        assertNotNull("模型不应该为空", model)

        val input = "测试输入"
        val result = model?.inference(input)

        assertNotNull("推理结果不应该为空", result)
        assertNotEquals("结果不应该包含'模拟'字样（表示不是fake实现）", 
            result, "模拟推理结果: $input")
        assertNotEquals("结果不应该包含'fake'字样", 
            result?.lowercase(), "fake")

        model?.release()
    }

    @Test
    fun testMNNModel_UsesRealAPI() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        val model = mnnIntegration.loadModel(modelPath)
        assertNotNull("模型不应该为空", model)

        val input = "测试输入"
        val result = model?.inference(input)

        assertNotNull("推理结果不应该为空", result)
        
        model?.release()
    }

    @Test
    fun testModelNotLoaded_ThrowsException() = runTest {
        val modelPath = "models/non-existent-model.mnn"
        val model = mnnIntegration.loadModel(modelPath)
        
        assertNull("不存在的模型应该返回null", model)
    }

    @Test
    fun testInference_WhenModelNotLoaded_ThrowsException() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        val model = mnnIntegration.loadModel(modelPath)
        assertNotNull("模型不应该为空", model)

        try {
            model?.release()
            val result = model?.inference("测试")
            fail("应该抛出异常")
        } catch (e: IllegalStateException) {
            assertTrue("异常消息应该包含'未加载'", 
                e.message?.contains("未加载") == true)
        }
    }

    @Test
    fun testModelRelease() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        val model = mnnIntegration.loadModel(modelPath)
        assertNotNull("模型不应该为空", model)
        assertTrue("模型应该被加载", model?.isLoaded() == true)

        model?.release()
        assertFalse("模型释放后不应该被加载", model?.isLoaded() == true)
    }

    @Test
    fun testIsModelDownloaded() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        val isDownloaded = mnnIntegration.isModelDownloaded(modelPath)
        assertTrue("已下载的模型应该返回true", isDownloaded)

        val nonExistentPath = "models/non-existent.mnn"
        val isNotDownloaded = mnnIntegration.isModelDownloaded(nonExistentPath)
        assertFalse("不存在的模型应该返回false", isNotDownloaded)
    }

    @Test
    fun testGetModelFileSize() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.writeText("test content")

        val fileSize = mnnIntegration.getModelFileSize(modelPath)
        assertEquals("文件大小应该匹配", 12L, fileSize)

        val nonExistentPath = "models/non-existent.mnn"
        val nonExistentSize = mnnIntegration.getModelFileSize(nonExistentPath)
        assertEquals("不存在的文件大小应该为0", 0L, nonExistentSize)
    }

    @Test
    fun testDeleteModel() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        assertTrue("模型应该存在", modelFile.exists())
        assertTrue("删除应该成功", mnnIntegration.deleteModel(modelPath))
        assertFalse("删除后模型不应该存在", modelFile.exists())
    }

    @Test
    fun testGetDownloadedModels() = runTest {
        val model1Path = "models/model1.mnn"
        val model2Path = "models/model2.mnn"
        
        val model1File = File(mockContext.filesDir, model1Path)
        val model2File = File(mockContext.filesDir, model2Path)
        
        model1File.parentFile?.mkdirs()
        model1File.createNewFile()
        model2File.createNewFile()

        val downloadedModels = mnnIntegration.getDownloadedModels()
        assertTrue("应该找到已下载的模型", 
            downloadedModels.contains(model1Path) || downloadedModels.contains(model2Path))
    }

    @Test
    fun testClearModelCache() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        assertTrue("模型应该存在", modelFile.exists())
        mnnIntegration.clearModelCache()
        assertFalse("清理缓存后模型不应该存在", modelFile.exists())
    }

    @Test
    fun testMNNModelIsLoaded() = runTest {
        val modelPath = "models/test-model.mnn"
        val modelFile = File(mockContext.filesDir, modelPath)
        modelFile.parentFile?.mkdirs()
        modelFile.createNewFile()

        val model = mnnIntegration.loadModel(modelPath)
        assertNotNull("模型不应该为空", model)
        assertTrue("模型应该被加载", model?.isLoaded() == true)

        model?.release()
        assertFalse("模型释放后不应该被加载", model?.isLoaded() == true)
    }
}
