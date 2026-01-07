package com.autodroid.trader.app.ui.tradeplans

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.app.network.TradePlanStatus
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
    private val tradePlansViewModel: TradePlansViewModel,
    private val onItemUpdate: (TradePlansItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ItemTradePlanSummaryManager"
    }
    
    private var currentItem = TradePlansItem.ItemTradePlansSummary()
    private var adapter: TradePlansAdapter? = null
    
    /**
     * Set the adapter for UI updates
     */
    fun setAdapter(tradePlansAdapter: TradePlansAdapter?) {
        this.adapter = tradePlansAdapter
    }
    
    /**
     * Initialize the ItemTradePlanSummaryManager
     */
    fun initialize() {
        setupObservers()
    }
    
    /**
     * Set up observers for trade plan data
     */
    private fun setupObservers() {
        Log.d(TAG, "setupObservers: 开始设置观察者")
        
        // Observe trade plan summary from TradePlansViewModel
        tradePlansViewModel.tradePlanSummary.observe(lifecycleOwner) { summary ->
            Log.d(TAG, "TradePlansViewModel tradePlanSummary updated: $summary")
            summary?.let {
                updateItem(it)
            }
        }
    }
    
    /**
     * Update the current item and notify the callback
     */
    private fun updateItem(summary: TradePlanSummary) {
        currentItem = TradePlansItem.ItemTradePlansSummary(
            status = currentItem.status,
            pendingCount = summary.pendingCount,
            approvedCount = summary.approvedCount,
            rejectedCount = summary.rejectedCount,
            executedSuccessCount = summary.executedSuccessCount,
            executedFailedCount = summary.executedFailedCount
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
            TradePlanSummary(
                pendingCount = 0,
                approvedCount = 0,
                rejectedCount = 0,
                executedSuccessCount = 0,
                executedFailedCount = 0
            )
        )
        
        // Trigger refresh through ViewModel
        tradePlansViewModel.refresh()
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
