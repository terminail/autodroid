package com.autodroid.teachitback

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import com.autodroid.teachitback.di.AppContainer
import com.autodroid.teachitback.di.ViewModelFactoryProvider
import com.autodroid.teachitback.initializer.ModelFileInitializer
import com.autodroid.teachitback.router.AIServiceRouter
import com.autodroid.teachitback.utils.DataInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * TeachItBack应用类
 * 负责全局初始化，包括AI服务、数据等
 */
class TeachItBackApplication : Application() {
    
    companion object {
        private const val TAG = "TeachItBackApplication"
        
        /**
         * 获取应用实例
         */
        lateinit var instance: TeachItBackApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.i(TAG, "TeachItBack应用启动中...")

        // 1. 初始化依赖注入容器（包含AI服务初始化）
        AppContainer.initialize(this)
        ViewModelFactoryProvider.initialize(this)

        // 2. 初始化模型文件（复制到应用内部存储）
        initializeModelFiles()

        // 3. 初始化演示数据
        DataInitializer(this).initializeDemoData()

        Log.i(TAG, "TeachItBack应用启动完成")
    }
    
    /**
     * 初始化模型文件
     */
    private fun initializeModelFiles() {
        try {
            Log.i(TAG, "开始初始化模型文件...")
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                try {
                    ModelFileInitializer.initialize(this@TeachItBackApplication)
                } catch (e: Exception) {
                    Log.e(TAG, "模型文件初始化异常", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "模型文件初始化失败: ${e.message}", e)
        }
    }
    
    /**
     * 获取当前AI服务状态
     */
    fun getAIServiceStatus(): String {
        return try {
            AIServiceRouter.getCurrentServiceStatus()
        } catch (e: Exception) {
            "服务状态获取失败: ${e.message}"
        }
    }
    
    /**
     * 获取MessageRepository实例
     * 允许通过应用上下文直接获取Repository
     */
    fun getMessageRepository(): com.autodroid.teachitback.repository.MessageRepository {
        return com.autodroid.teachitback.di.AppContainer.getMessageRepository()
    }
    
    /**
     * 获取SettingsRepository实例
     * 允许通过应用上下文直接获取Repository
     */
    fun getSettingsRepository(): com.autodroid.teachitback.repository.SettingsRepository {
        return com.autodroid.teachitback.di.AppContainer.getSettingsRepository()
    }
    
    /**
     * 获取MindMapRepository实例
     * 允许通过应用上下文直接获取Repository
     */
    fun getMindMapRepository(): com.autodroid.teachitback.repository.MindMapRepository {
        return com.autodroid.teachitback.di.AppContainer.getMindMapRepository()
    }
    
    /**
     * 获取TopicRepository实例
     * 允许通过应用上下文直接获取Repository
     */
    fun getTopicRepository(): com.autodroid.teachitback.repository.TopicRepository {
        return com.autodroid.teachitback.di.AppContainer.getTopicRepository()
    }
}