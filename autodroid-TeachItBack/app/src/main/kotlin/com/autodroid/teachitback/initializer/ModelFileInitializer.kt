package com.autodroid.teachitback.initializer

import android.content.Context
import android.util.Log
import com.autodroid.teachitback.framework.MNNIntegration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 模型文件初始化器
 * 负责将模型文件从外部存储复制到应用内部存储
 */
object ModelFileInitializer {

    private const val TAG = "ModelFileInitializer"

    /**
     * 模型文件映射表
     * 源文件名 -> 目标路径（相对于filesDir）
     */
    private val MODEL_FILES = mapOf(
        "tinybert-int8.mnn" to "models/tinybert-int8.mnn",
        "chatglm-6b-int4.mnn" to "models/chatglm-6b-int4.mnn"
    )

    /**
     * 可能的源路径列表（按优先级排序）
     */
    private val SOURCE_PATHS = listOf(
        "models",  // assets目录
        "/sdcard/Android/data/com.autodroid.teachitback/files/tibresource/models",
        "/storage/emulated/0/Android/data/com.autodroid.teachitback/files/tibresource/models",
        "/sdcard/tibresource/models",
        "/storage/emulated/0/tibresource/models",
        "/sdcard/tibresource",
        "/storage/emulated/0/tibresource"
    )

    /**
     * 初始化所有模型文件
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始初始化模型文件...")

            // 创建models目录
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
                Log.d(TAG, "创建models目录: ${modelsDir.absolutePath}")
            } else {
                Log.d(TAG, "models目录已存在: ${modelsDir.absolutePath}")
            }

            // 复制所有模型文件
            var copiedCount = 0
            var skippedCount = 0

            MODEL_FILES.forEach { (fileName, targetPath) ->
                val targetFile = File(context.filesDir, targetPath)
                Log.d(TAG, "处理模型文件: $fileName -> $targetPath")

                // 尝试从多个可能的源路径查找文件
                var sourceFile: File? = null
                var sourceInputStream: java.io.InputStream? = null

                for (sourcePath in SOURCE_PATHS) {
                    if (sourcePath == "models") {
                        // 尝试从assets加载
                        try {
                            sourceInputStream = context.assets.open("$sourcePath/$fileName")
                            Log.d(TAG, "从assets找到源文件: $sourcePath/$fileName")
                            break
                        } catch (e: Exception) {
                            Log.d(TAG, "assets中未找到: $sourcePath/$fileName")
                        }
                    } else {
                        // 尝试从外部存储加载
                        val file = File(sourcePath, fileName)
                        Log.d(TAG, "检查路径: ${file.absolutePath}")
                        if (file.exists()) {
                            sourceFile = file
                            Log.d(TAG, "找到源文件: ${file.absolutePath}")
                            break
                        }
                    }
                }

                if (sourceInputStream != null) {
                    // 从assets复制
                    if (shouldCopyFileFromAssets(sourceInputStream, targetFile)) {
                        val success = copyFileFromAssets(sourceInputStream, targetFile)
                        if (success) {
                            copiedCount++
                            Log.i(TAG, "从assets复制模型文件成功: $fileName -> ${targetFile.name} (${targetFile.length()} bytes)")
                        } else {
                            Log.e(TAG, "从assets复制模型文件失败: $fileName")
                        }
                    } else {
                        skippedCount++
                        Log.d(TAG, "跳过已存在的模型文件: ${targetFile.name}")
                    }
                } else if (sourceFile != null) {
                    // 从外部存储复制
                    if (shouldCopyFile(sourceFile, targetFile)) {
                        val success = copyFile(sourceFile, targetFile)
                        if (success) {
                            copiedCount++
                            Log.i(TAG, "复制模型文件成功: ${sourceFile.name} -> ${targetFile.name} (${targetFile.length()} bytes)")
                        } else {
                            Log.e(TAG, "复制模型文件失败: ${sourceFile.name}")
                        }
                    } else {
                        skippedCount++
                        Log.d(TAG, "跳过已存在的模型文件: ${targetFile.name}")
                    }
                } else {
                    Log.w(TAG, "未找到模型文件: $fileName")
                    Log.d(TAG, "已检查路径: ${SOURCE_PATHS.joinToString(", ")}")
                }
            }

            Log.i(TAG, "模型文件初始化完成 - 复制: $copiedCount, 跳过: $skippedCount")
        } catch (e: Exception) {
            Log.e(TAG, "模型文件初始化失败", e)
        }
    }

    /**
     * 判断是否需要复制文件
     */
    private fun shouldCopyFile(sourceFile: File, targetFile: File): Boolean {
        // 如果目标文件不存在，需要复制
        if (!targetFile.exists()) {
            return true
        }

        // 如果源文件比目标文件新，需要复制
        return sourceFile.lastModified() > targetFile.lastModified()
    }

    /**
     * 判断是否需要从assets复制文件
     */
    private fun shouldCopyFileFromAssets(sourceInputStream: java.io.InputStream, targetFile: File): Boolean {
        // 如果目标文件不存在，需要复制
        return !targetFile.exists()
    }

    /**
     * 复制文件
     */
    private fun copyFile(sourceFile: File, targetFile: File): Boolean {
        return try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "文件复制失败: ${sourceFile.name} -> ${targetFile.name}", e)
            false
        }
    }

    /**
     * 从assets复制文件
     */
    private fun copyFileFromAssets(sourceInputStream: java.io.InputStream, targetFile: File): Boolean {
        return try {
            FileOutputStream(targetFile).use { output ->
                sourceInputStream.use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "从assets复制文件失败: ${targetFile.name}", e)
            false
        }
    }

    /**
     * 检查模型文件是否已初始化
     */
    fun isModelFileInitialized(context: Context, modelPath: String): Boolean {
        val targetFile = File(context.filesDir, modelPath)
        return targetFile.exists() && targetFile.length() > 0
    }

    /**
     * 获取所有模型文件状态
     */
    fun getModelFilesStatus(context: Context): Map<String, Boolean> {
        val status = mutableMapOf<String, Boolean>()
        MODEL_FILES.forEach { (_, targetPath) ->
            status[targetPath] = isModelFileInitialized(context, targetPath)
        }
        return status
    }
}
