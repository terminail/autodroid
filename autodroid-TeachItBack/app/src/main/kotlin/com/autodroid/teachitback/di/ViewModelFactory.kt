package com.autodroid.teachitback.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodroid.teachitback.router.AIServiceRouter
import com.autodroid.teachitback.service.TopicCategoryManager
import com.autodroid.teachitback.viewmodel.AppViewModel
import com.autodroid.teachitback.viewmodel.ChatViewModel
import com.autodroid.teachitback.viewmodel.ServiceSettingsViewModel
import com.autodroid.teachitback.viewmodel.SettingsAIServiceDetailViewModel
import com.autodroid.teachitback.viewmodel.SettingsViewModel
import com.autodroid.teachitback.viewmodel.TopicsViewModel
import com.autodroid.teachitback.viewmodel.WhyViewModel

/**
 * ViewModel工厂类
 * 负责创建并注入依赖的ViewModel实例
 */
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppViewModel::class.java) -> {
                AppViewModel(application) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(application) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application) as T
            }
            modelClass.isAssignableFrom(SettingsAIServiceDetailViewModel::class.java) -> {
                SettingsAIServiceDetailViewModel(application) as T
            }
            modelClass.isAssignableFrom(TopicsViewModel::class.java) -> {
                TopicsViewModel(application) as T
            }
            modelClass.isAssignableFrom(WhyViewModel::class.java) -> {
                WhyViewModel(application) as T
            }
            modelClass.isAssignableFrom(ServiceSettingsViewModel::class.java) -> {
                // ServiceSettingsViewModel 需要额外的依赖注入
                ServiceSettingsViewModel(
                    AppContainer.getSettingsRepository(),
                    TopicCategoryManager(AppContainer.getDatabase().topicDao())
                ) as T
            }
            else -> {
                throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
            }
        }
    }
}

/**
 * ViewModel工厂提供者
 */
object ViewModelFactoryProvider {
    
    private lateinit var _factory: ViewModelFactory
    
    /**
     * 初始化ViewModel工厂
     */
    fun initialize(application: Application) {
        _factory = ViewModelFactory(application)
    }
    
    /**
     * 获取ViewModel工厂实例
     */
    fun getFactory(): ViewModelFactory {
        if (!::_factory.isInitialized) {
            throw IllegalStateException("ViewModelFactory尚未初始化，请先调用initialize()")
        }
        return _factory
    }
}