package com.autodroid.trader.ui.tradeplans

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.managers.TradePlanManager
import com.autodroid.trader.model.TradePlanStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager class for handling Trade Plan functionality
 * This class manages trade plan execution, status updates, and operations
 */
class ItemTradePlanManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val appViewModel: AppViewModel,
    private val onItemUpdate: (TradePlansItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ItemTradePlanManager"
    }
    
    private var currentItem = TradePlansItem.ItemTradePlans()
    
    private val tradePlanManager = TradePlanManager.getInstance(context)
    
    /**
     * Initialize the ItemTradePlanManager
     */
    fun initialize() {
        Log.d(TAG, "ItemTradePlanManager initialized")
    }
    
    /**
     * Update the current item and notify the callback
     */
    private fun updateItem(
        status: String = currentItem.status,
        executionStatus: String = currentItem.executionStatus
    ) {
        currentItem = TradePlansItem.ItemTradePlans(
            status = status,
            executionStatus = executionStatus
        )
        
        onItemUpdate(currentItem)
    }
    
    /**
     * Execute approved trade plans
     */
    fun executeApprovedPlans() {
        Log.d(TAG, "Executing approved trade plans")
        updateItem(
            status = "Executing approved trade plans...",
            executionStatus = TradePlanStatus.EXECUTING.value
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = tradePlanManager.executeApprovedPlans()
                
                updateItem(
                    status = result,
                    executionStatus = TradePlanStatus.COMPLETED.value
                )
                
                Log.d(TAG, "Trade plans execution completed: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Error executing approved trade plans: ${e.message}", e)
                updateItem(
                    status = "Error executing trade plans: ${e.message}",
                    executionStatus = "ERROR"
                )
            }
        }
    }
    
    /**
     * Update trade plan status
     */
    fun updateTradePlanStatus(tradePlanId: String, newStatus: String) {
        Log.d(TAG, "Updating trade plan $tradePlanId status to $newStatus")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = tradePlanManager.updateTradePlanStatus(tradePlanId, newStatus)
                
                if (result.contains("successfully", ignoreCase = true)) {
                    Log.d(TAG, "Successfully updated trade plan status")
                } else {
                    Log.e(TAG, "Failed to update trade plan status")
                    updateItem(
                        status = "Failed to update trade plan status"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating trade plan status: ${e.message}", e)
                updateItem(
                    status = "Error updating trade plan status: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Refresh the trade plan data
     */
    fun refresh() {
        Log.d(TAG, "Refreshing trade plan data")
        
        updateItem(
            status = "Refreshing trade plans...",
            executionStatus = "REFRESHING"
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                tradePlanManager.getAllTradePlans()
                updateItem(
                    status = "Trade plans refreshed",
                    executionStatus = "IDLE"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing trade plans: ${e.message}", e)
                updateItem(
                    status = "Error refreshing trade plans: ${e.message}",
                    executionStatus = "ERROR"
                )
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
    }
    
    /**
     * Handle list update logic for trade plan item
     */
    fun handleListUpdate(item: TradePlansItem, tradePlanItems: MutableList<Any>, tradePlansAdapter: TradePlansAdapter?): Boolean {
        return try {
            if (item is TradePlansItem.ItemTradePlans) {
                val existingIndex = tradePlanItems.indexOfFirst { it is TradePlansItem.ItemTradePlans }
                
                if (existingIndex != -1) {
                    tradePlanItems[existingIndex] = item
                } else {
                    val summaryIndex = tradePlanItems.indexOfFirst { it is TradePlansItem.ItemTradePlansSummary }
                    if (summaryIndex != -1) {
                        tradePlanItems.add(summaryIndex + 1, item)
                    } else {
                        tradePlanItems.add(item)
                    }
                }
                
                tradePlansAdapter?.let { adapter ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        adapter.updateItems(tradePlanItems)
                    }
                }
                true
            } else {
                Log.e(TAG, "Invalid item type for trade plan: ${item::class.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating trade plan item in list: ${e.message}", e)
            false
        }
    }
}
