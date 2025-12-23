package com.autodroid.trader.ui.tradeplans

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.managers.TradePlanManager
import com.autodroid.trader.data.dao.TradePlanEntity
import com.autodroid.trader.model.TradePlanStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager class for handling Trade Plan Summary functionality
 * This class manages trade plan statistics and summary information
 */
class ItemTradePlanSummaryManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val appViewModel: AppViewModel,
    private val onItemUpdate: (TradePlansItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ItemTradePlanSummaryManager"
    }
    
    private var currentItem = TradePlansItem.ItemTradePlansSummary()
    
    private val tradePlanManager = TradePlanManager.getInstance(context)
    
    private var tradePlanSummary = TradePlanSummary()
    
    /**
     * Initialize the ItemTradePlanSummaryManager
     */
    fun initialize() {
        setupObservers()
        loadTradePlans()
    }
    
    /**
     * Set up observers for trade plan data
     */
    private fun setupObservers() {
        Log.d(TAG, "setupObservers: 开始设置观察者")
        
        tradePlanManager.getAllTradePlansLiveData().observe(lifecycleOwner) { tradePlans ->
            Log.d(TAG, "Trade plans updated: ${tradePlans?.size ?: 0} items")
            tradePlans?.let {
                updateTradePlanSummary(it)
                updateItem()
            }
        }
    }
    
    /**
     * Load trade plans from repository
     */
    private fun loadTradePlans() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                tradePlanManager.getAllTradePlans()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading trade plans: ${e.message}", e)
                updateItem(
                    status = "Error loading trade plans: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update trade plan summary statistics
     */
    private fun updateTradePlanSummary(tradePlans: List<TradePlanEntity>) {
        tradePlanSummary = TradePlanSummary(
            pendingCount = tradePlans.count { it.status == TradePlanStatus.PENDING.value },
            approvedCount = tradePlans.count { it.status == TradePlanStatus.APPROVED.value },
            rejectedCount = tradePlans.count { it.status == TradePlanStatus.REJECTED.value },
            executedSuccessCount = tradePlans.count { it.status == TradePlanStatus.COMPLETED.value && it.lastExecutionResult == "success" },
            executedFailedCount = tradePlans.count { it.status == TradePlanStatus.COMPLETED.value && it.lastExecutionResult == "failed" }
        )
        
        Log.d(TAG, "Trade plan summary updated: $tradePlanSummary")
    }
    
    /**
     * Update the current item and notify the callback
     */
    private fun updateItem(
        status: String = currentItem.status
    ) {
        currentItem = TradePlansItem.ItemTradePlansSummary(
            status = status,
            pendingCount = tradePlanSummary.pendingCount,
            approvedCount = tradePlanSummary.approvedCount,
            rejectedCount = tradePlanSummary.rejectedCount,
            executedSuccessCount = tradePlanSummary.executedSuccessCount,
            executedFailedCount = tradePlanSummary.executedFailedCount
        )
        
        onItemUpdate(currentItem)
    }
    
    /**
     * Get current item
     */
    fun getCurrentItem(): TradePlansItem.ItemTradePlansSummary {
        return currentItem
    }
    
    /**
     * Refresh the trade plan summary data
     */
    fun refresh() {
        Log.d(TAG, "Refreshing trade plan summary data")
        
        updateItem(
            status = "Refreshing trade plan summary..."
        )
        
        loadTradePlans()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
    }
    
    /**
     * Handle list update logic for trade plan summary item
     */
    fun handleListUpdate(item: TradePlansItem, tradePlanItems: MutableList<Any>, tradePlansAdapter: TradePlansAdapter?): Boolean {
        return try {
            if (item is TradePlansItem.ItemTradePlansSummary) {
                val existingIndex = tradePlanItems.indexOfFirst { it is TradePlansItem.ItemTradePlansSummary }
                
                if (existingIndex != -1) {
                    tradePlanItems[existingIndex] = item
                } else {
                    tradePlanItems.add(0, item)
                }
                
                tradePlansAdapter?.let { adapter ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        adapter.updateItems(tradePlanItems)
                    }
                }
                true
            } else {
                Log.e(TAG, "Invalid item type for trade plan summary: ${item::class.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating trade plan summary item in list: ${e.message}", e)
            false
        }
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
