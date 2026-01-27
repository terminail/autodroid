package com.autodroid.teachitback.di

import android.app.Application
import android.util.Log
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.registry.AIServiceRegistry
import com.autodroid.teachitback.repository.MessageRepository
import com.autodroid.teachitback.repository.MindMapRepository
import com.autodroid.teachitback.repository.SettingsRepository
import com.autodroid.teachitback.repository.TopicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 应用依赖注入容器
 * 在应用启动时初始化所有Repository，提供全局共享的依赖实例
 */
object AppContainer {
    
    private const val TAG = "AppContainer"
    
    private lateinit var _application: Application
    private lateinit var _database: AppDatabase
    private lateinit var _messageRepository: MessageRepository
    private lateinit var _settingsRepository: SettingsRepository
    private lateinit var _mindMapRepository: MindMapRepository
    private lateinit var _topicRepository: TopicRepository
    private val _aiServiceRegistry = AIServiceRegistry()
    
    /**
     * 初始化应用容器
     */
    fun initialize(application: Application) {
        _application = application
        _database = AppDatabase.getDatabase(application)
        _messageRepository = MessageRepository(_database.messageDao())
        _settingsRepository = SettingsRepository(_database.settingDao())
        _mindMapRepository = MindMapRepository(_database)
        _topicRepository = TopicRepository(_database.topicDao())
        
        // 初始化AI服务
        initializeAIServices()
    }
    
    /**
     * 初始化AI服务
     */
    private fun initializeAIServices() {
        try {
            Log.i(TAG, "开始初始化AI服务...")
            
            runBlocking(Dispatchers.IO) {
                com.autodroid.teachitback.initializer.AIServiceInitializer.registerAllServices(_application, _aiServiceRegistry)
            }
            
            // 初始化AI服务路由器
            com.autodroid.teachitback.router.AIServiceRouter.initialize(_aiServiceRegistry)
            
            val totalServices = _aiServiceRegistry.getAllServices().size
            Log.i(TAG, "AI服务初始化完成，共注册服务: $totalServices 个")
            
        } catch (e: Exception) {
            Log.e(TAG, "AI服务初始化失败: ${e.message}", e)
        }
    }
    
    /**
     * 获取MessageRepository实例
     */
    fun getMessageRepository(): MessageRepository {
        if (!::_messageRepository.isInitialized) {
            throw IllegalStateException("AppContainer尚未初始化，请先调用initialize()")
        }
        return _messageRepository
    }
    
    /**
     * 获取SettingsRepository实例
     */
    fun getSettingsRepository(): SettingsRepository {
        if (!::_settingsRepository.isInitialized) {
            throw IllegalStateException("AppContainer尚未初始化，请先调用initialize()")
        }
        return _settingsRepository
    }
    
    /**
     * 获取MindMapRepository实例
     */
    fun getMindMapRepository(): MindMapRepository {
        if (!::_mindMapRepository.isInitialized) {
            throw IllegalStateException("AppContainer尚未初始化，请先调用initialize()")
        }
        return _mindMapRepository
    }
    
    /**
     * 获取TopicRepository实例
     */
    fun getTopicRepository(): TopicRepository {
        if (!::_topicRepository.isInitialized) {
            throw IllegalStateException("AppContainer尚未初始化，请先调用initialize()")
        }
        return _topicRepository
    }
    
    /**
     * 获取AIServiceRegistry实例
     */
    fun getAIServiceRegistry(): AIServiceRegistry {
        return _aiServiceRegistry
    }
    
    /**
     * 获取数据库实例（仅在必要时使用）
     */
    fun getDatabase(): AppDatabase {
        if (!::_database.isInitialized) {
            throw IllegalStateException("AppContainer尚未初始化，请先调用initialize()")
        }
        return _database
    }
    
    /**
     * 获取应用实例
     */
    fun getApplication(): Application {
        if (!::_application.isInitialized) {
            throw IllegalStateException("AppContainer尚未初始化，请先调用initialize()")
        }
        return _application
    }
    
    /**
     * 检查容器是否已初始化
     */
    fun isInitialized(): Boolean {
        return ::_application.isInitialized && 
               ::_database.isInitialized && 
               ::_messageRepository.isInitialized && 
               ::_settingsRepository.isInitialized &&
               ::_mindMapRepository.isInitialized &&
               ::_topicRepository.isInitialized
    }
}