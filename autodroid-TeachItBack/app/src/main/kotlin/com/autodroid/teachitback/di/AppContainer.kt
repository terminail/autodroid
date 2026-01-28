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
                // 先初始化zhipu配置（从BuildConfig读取API key）
                initializeZhipuConfig()
                
                // 加载AI服务配置到LiveData
                _settingsRepository.loadAllAIServiceConfigsToLiveData()
                
                com.autodroid.teachitback.initializer.AIServiceInitializer.registerAllServices(_application, _aiServiceRegistry)
            }
            
            // 订阅配置变化
            com.autodroid.teachitback.initializer.AIServiceInitializer.observeConfigChanges(_settingsRepository.aiServiceConfigs)
            
            // 初始化AI服务路由器
            com.autodroid.teachitback.router.AIServiceRouter.initialize(_aiServiceRegistry)
            
            val totalServices = _aiServiceRegistry.getAllServices().size
            Log.i(TAG, "AI服务初始化完成，共注册服务: $totalServices 个")
            
        } catch (e: Exception) {
            Log.e(TAG, "AI服务初始化失败: ${e.message}", e)
        }
    }
    
    /**
     * 初始化zhipu配置（从BuildConfig读取API key）
     */
    private suspend fun initializeZhipuConfig() {
        try {
            val config = _settingsRepository.loadAIServiceConfig("zhipu")
            val apiKey = com.autodroid.teachitback.BuildConfig.GLM47FLASH_API_KEY
            
            Log.i(TAG, "initializeZhipuConfig: BuildConfig.GLM47FLASH_API_KEY = ${apiKey.take(10)}...")
            Log.i(TAG, "initializeZhipuConfig: API Key length = ${apiKey.length}")
            
            if (config == null) {
                // 数据库中没有配置，创建默认配置
                Log.i(TAG, "创建zhipu默认配置，API Key: ${apiKey.take(10)}...")
                val defaultConfig = com.autodroid.teachitback.config.AIServiceConfig.ZhipuConfig(
                    apiKey = apiKey,
                    status = com.autodroid.teachitback.config.AIServiceStatus.STATUS_NOT_CHECKED
                )
                _settingsRepository.saveAIServiceConfig(defaultConfig)
                Log.i(TAG, "initializeZhipuConfig: 默认配置已保存到数据库")
            } else {
                // 数据库中有配置，检查是否需要更新API key
                Log.i(TAG, "initializeZhipuConfig: 数据库中已有配置，API Key = ${config.apiKey.take(10)}...")
                val shouldUpdate = config.apiKey.isEmpty() || config.apiKey != apiKey
                if (shouldUpdate && apiKey.isNotEmpty()) {
                    Log.i(TAG, "更新zhipu API Key: ${apiKey.take(10)}...")
                    val updatedConfig = when (config) {
                        is com.autodroid.teachitback.config.AIServiceConfig.ZhipuConfig -> config.copy(
                            apiKey = apiKey,
                            status = com.autodroid.teachitback.config.AIServiceStatus.STATUS_NOT_CHECKED
                        )
                        else -> config
                    }
                    _settingsRepository.saveAIServiceConfig(updatedConfig)
                    Log.i(TAG, "initializeZhipuConfig: API Key已更新到数据库")
                } else {
                    Log.i(TAG, "initializeZhipuConfig: API Key无需更新")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "初始化zhipu配置失败: ${e.message}", e)
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