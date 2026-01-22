package com.autodroid.teachitback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.autodroid.teachitback.api.AIService
import com.autodroid.teachitback.api.OpenAIService

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    // Global shared state
    private val _currentTopicId = MutableLiveData<String?>()
    val currentTopicId: LiveData<String?> = _currentTopicId
    
    // AI Service - shared across the app
    private var aiService: AIService? = null
    private val _isAIInitialized = MutableLiveData(false)
    val isAIInitialized: LiveData<Boolean> = _isAIInitialized

    fun initializeAI(apiKey: String, model: String = "gpt-3.5-turbo") {
        if (apiKey.isNotBlank()) {
            aiService = OpenAIService(apiKey, model)
            _isAIInitialized.value = true
        }
    }
    
    fun getAIService(): AIService? = aiService
    
    fun setCurrentTopicId(topicId: String) {
        _currentTopicId.value = topicId
    }
    
    fun clearCurrentTopicId() {
        _currentTopicId.value = null
    }
}
