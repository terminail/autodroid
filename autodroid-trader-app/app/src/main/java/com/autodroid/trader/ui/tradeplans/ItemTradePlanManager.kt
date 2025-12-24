package com.autodroid.trader.ui.tradeplans

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.autodroid.trader.network.TradePlanStatus
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
    private val tradePlansViewModel: TradePlansViewModel,
    private val onItemUpdate: (TradePlansItem) -> Unit
) {
    
    companion object {
        private const val TAG = "ItemTradePlanManager"
    }
    
    private var currentItem = TradePlansItem.ItemTradePlans()
    
    private val tradePlanManager = com.autodroid.trader.managers.TradePlanManager.getInstance(context)
    
    /**
     * Initialize the ItemTradePlanManager
     */
    fun initialize() {
        setupObservers()
        Log.d(TAG, "ItemTradePlanManager initialized")
    }
    
    /**
     * Set up observers for trade plan status
     */
    private fun setupObservers() {
        Log.d(TAG, "setupObservers: 开始设置观察者")
        
        // Observe available trade plans from TradePlansViewModel (single source of truth)
        tradePlansViewModel.availableTradePlans.observe(lifecycleOwner) { tradePlanEntities ->
            Log.d(TAG, "TradePlansViewModel availableTradePlans updated: ${tradePlanEntities.size} 个交易计划")
            
            val status = when {
                tradePlanEntities.isEmpty() -> "暂无交易计划"
                tradePlanEntities.any { it.status == TradePlanStatus.EXECUTING.value } -> "正在执行交易计划..."
                tradePlanEntities.any { it.status == TradePlanStatus.APPROVED.value } -> "有已批准的交易计划待执行"
                else -> "交易计划已加载"
            }
            
            val executionStatus = when {
                tradePlanEntities.any { it.status == TradePlanStatus.EXECUTING.value } -> TradePlanStatus.EXECUTING.value
                tradePlanEntities.any { it.status == TradePlanStatus.APPROVED.value } -> TradePlanStatus.APPROVED.value
                else -> "IDLE"
            }
            
            updateItem(
                status = status,
                executionStatus = executionStatus
            )
            
            Log.d(TAG, "Trade plans status updated: $status, executionStatus: $executionStatus")
        }
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
     * Refresh the trade plan data
     */
    fun refresh() {
        Log.d(TAG, "Refreshing trade plan data")
        
        // Reset execution state
        currentItem = TradePlansItem.ItemTradePlans()
        
        // Immediately update UI to show refreshing state
        updateItem(
            status = "Refreshing trade plans...",
            executionStatus = "REFRESHING"
        )
        
        // Reinitialize the trade plan item with current data
        initialize()
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
