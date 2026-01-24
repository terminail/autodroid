package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.autodroid.teachitback.router.AIServiceRouter

/**
 * AppViewModel - 全局应用状态管理
 * 遵循MVVM架构原则：ViewModel只与Router/Repository交互，不感知具体的AI服务实现
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    // Global shared state
    private val _currentTopicId = MutableLiveData<String?>()
    val currentTopicId: LiveData<String?> = _currentTopicId
    
    // AI服务状态 - 通过AIServiceRouter管理，ViewModel不感知具体实现
    private val _isAIInitialized = MutableLiveData(false)
    val isAIInitialized: LiveData<Boolean> = _isAIInitialized

    /**
     * 初始化AI服务配置
     * ViewModel通过AIServiceRouter配置AI服务，不直接操作具体服务
     */
    fun initializeAI(apiKey: String, model: String = "gpt-3.5-turbo") {
        if (apiKey.isNotBlank()) {
            // 通过AIServiceRouter配置服务，ViewModel不感知具体实现细节
            val success = AIServiceRouter.configureDefaultService(apiKey, model)
            _isAIInitialized.value = success
        }
    }
    
    /**
     * 获取当前AI服务状态
     * ViewModel通过AIServiceRouter获取状态，不直接访问服务
     */
    fun getAIServiceStatus(): String {
        return AIServiceRouter.getCurrentServiceStatus()
    }
    
    fun setCurrentTopicId(topicId: String) {
        _currentTopicId.value = topicId
    }
    
    fun clearCurrentTopicId() {
        _currentTopicId.value = null
    }
}
