package com.autodroid.trader.ui.tradeplans

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.autodroid.trader.data.dao.TradePlanEntity
import com.autodroid.trader.managers.TradePlanManager
import kotlinx.coroutines.launch

class TradePlansViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tradePlanManager = TradePlanManager.getInstance(application)
    
    // Available trade plans (local database data)
    private val _availableTradePlans = MutableLiveData<MutableList<TradePlanEntity>>()
    val availableTradePlans: LiveData<MutableList<TradePlanEntity>> = _availableTradePlans
    
    // Trade plan summary statistics
    private val _tradePlanSummary = MutableLiveData<TradePlanSummary>()
    val tradePlanSummary: LiveData<TradePlanSummary> = _tradePlanSummary
    
    /**
     * Initialize the ViewModel and start observing trade plans
     */
    fun initialize() {
        // Observe trade plans from TradePlanManager
        tradePlanManager.getTradePlansByStatusAndSync(com.autodroid.trader.network.TradePlanStatus.ALL.value)
            .observeForever { tradePlanEntities ->
                _availableTradePlans.value = tradePlanEntities.toMutableList()
                _tradePlanSummary.value = calculateTradePlanSummary(tradePlanEntities)
            }
    }
    
    /**
     * Calculate trade plan summary statistics
     */
    private fun calculateTradePlanSummary(tradePlans: List<TradePlanEntity>): TradePlanSummary {
        android.util.Log.d("TradePlansViewModel", "计算统计信息，总记录数: ${tradePlans.size}")
        
        val statusCounts = tradePlans.groupBy { it.status }.mapValues { it.value.size }
        android.util.Log.d("TradePlansViewModel", "状态分布: $statusCounts")
        
        return TradePlanSummary(
            pendingCount = tradePlans.count { it.status?.equals(com.autodroid.trader.network.TradePlanStatus.PENDING.value, ignoreCase = true) == true },
            approvedCount = tradePlans.count { it.status?.equals(com.autodroid.trader.network.TradePlanStatus.APPROVED.value, ignoreCase = true) == true },
            rejectedCount = tradePlans.count { it.status?.equals(com.autodroid.trader.network.TradePlanStatus.REJECTED.value, ignoreCase = true) == true },
            executedSuccessCount = tradePlans.count { it.status?.equals(com.autodroid.trader.network.TradePlanStatus.COMPLETED.value, ignoreCase = true) == true },
            executedFailedCount = tradePlans.count { it.status?.equals(com.autodroid.trader.network.TradePlanStatus.FAILED.value, ignoreCase = true) == true }
        )
    }
    
    /**
     * Refresh trade plans data
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                tradePlanManager.getTradePlansByStatus(com.autodroid.trader.network.TradePlanStatus.ALL.value)
            } catch (e: Exception) {
                android.util.Log.e("TradePlansViewModel", "刷新交易计划失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * Set available trade plans
     */
    fun setAvailableTradePlans(tradePlans: MutableList<TradePlanEntity>) {
        _availableTradePlans.value = tradePlans
        _tradePlanSummary.value = calculateTradePlanSummary(tradePlans)
    }
}

/**
 * Data class to hold trade plan summary statistics
 */
data class TradePlanSummary(
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
    val executedSuccessCount: Int = 0,
    val executedFailedCount: Int = 0
)
