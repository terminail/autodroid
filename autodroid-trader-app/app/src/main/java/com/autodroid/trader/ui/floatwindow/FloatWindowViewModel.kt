package com.autodroid.trader.ui.floatwindow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.trader.data.repository.TradePlanRepository
import com.autodroid.trader.model.TradeData
import kotlinx.coroutines.launch

class FloatWindowViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tradePlanRepository = TradePlanRepository.getInstance(application)
    
    // 内部可变LiveData
    private val _latestPrice = MutableLiveData<String>("¥0.00")
    private val _priceChange = MutableLiveData<String>("+0.00%")
    private val _tradeVolume = MutableLiveData<String>("成交量: 0")
    
    // 外部不可变LiveData
    val latestPrice: LiveData<String> = _latestPrice
    val priceChange: LiveData<String> = _priceChange
    val tradeVolume: LiveData<String> = _tradeVolume
    
    init {
        // 初始化数据
        loadLatestTradeData()
        
        // 设置定时刷新（模拟实时更新）
        startAutoRefresh()
    }
    
    private fun loadLatestTradeData() {
        viewModelScope.launch {
            val tradeData = tradePlanRepository.getLatestTradeData()
            updateTradeData(tradeData)
        }
    }
    
    private fun updateTradeData(tradeData: TradeData) {
        _latestPrice.postValue("¥${tradeData.price}")
        _priceChange.postValue("${tradeData.changePercent}%")
        _tradeVolume.postValue("成交量: ${tradeData.volume}")
    }
    
    private fun startAutoRefresh() {
        // 模拟实时数据更新，实际项目中应通过WebSocket或轮询实现
        viewModelScope.launch {
            while (true) {
                // 每5秒刷新一次数据
                kotlinx.coroutines.delay(5000)
                loadLatestTradeData()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // 清理资源
    }
}