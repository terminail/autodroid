package com.autodroid.trader.aas.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.trader.aas.database.UIEvent
import com.autodroid.trader.aas.database.UIRecorderDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class UIEventViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiRecorderDatabase = UIRecorderDatabase.getInstance(getApplication())
    private val _uiEventDao = _uiRecorderDatabase.uiEventDao()
    
    private val _recentEvents = MutableStateFlow<List<UIEvent>>(emptyList())
    val recentEvents: StateFlow<List<UIEvent>> = _recentEvents.asStateFlow()
    
    init {
        loadRecentEvents()
    }
    
    private fun loadRecentEvents() {
        viewModelScope.launch {
            try {
                val events = runBlocking { _uiEventDao.getRecentEvents(50) }
                _recentEvents.value = events
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun getAllEvents(): List<UIEvent> {
        return runCatching {
            runBlocking { _uiEventDao.getRecentEvents(1000) } // Get up to 1000 events
        }.getOrElse { emptyList() }
    }
    
    fun deleteOldEvents(olderThan: Long) {
        viewModelScope.launch {
            try {
                runBlocking { _uiEventDao.deleteOlderThan(olderThan) }
                loadRecentEvents() // Refresh the list after deletion
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}