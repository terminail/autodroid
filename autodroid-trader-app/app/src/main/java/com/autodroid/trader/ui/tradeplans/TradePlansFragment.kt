package com.autodroid.trader.ui.tradeplans

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.viewModels
import com.autodroid.trader.R
import com.autodroid.trader.ui.BaseFragment
import com.autodroid.trader.network.TradePlanResponse
import com.autodroid.trader.managers.TradePlanManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class TradePlansFragment : BaseFragment() {
    private val TAG = "TradePlansFragment"
    
    private var tradePlansRecyclerView: RecyclerView? = null
    private var adapter: TradePlansAdapter? = null
    private var tradePlanResponseItems: MutableList<TradePlanResponse>? = null
    
    // Trade plan manager
    private lateinit var tradePlanManager: TradePlanManager
    
    // TradePlansViewModel
    private val tradePlansViewModel: TradePlansViewModel by viewModels()
    
    // Pull-down detection for fragment header
    private var appBarLayout: AppBarLayout? = null
    private var touchStartY = 0f
    private var isPullingDown = false
    private var touchSlop = 0
    
    // Selection mode state
    private var isSelectionMode = false
    private val selectedTradePlans = mutableSetOf<String>()
    
    // Item managers for modular architecture
    private lateinit var itemTradePlanManager: ItemTradePlanManager
    private lateinit var itemTradePlanSummaryManager: ItemTradePlanSummaryManager
    
    // Trade plan items list
    private val tradePlansItemsList = mutableListOf<TradePlansItem>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tradePlanResponseItems = ArrayList<TradePlanResponse>()
        tradePlanManager = TradePlanManager.getInstance(requireContext())
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_trade_plans
    }

    override fun initViews(view: View) {
        tradePlansRecyclerView = view?.findViewById<RecyclerView>(R.id.tradeplans_recycler_view)
        
        // Find AppBarLayout
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        
        // Initialize touch slop for pull-down detection
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        // Set up RecyclerView
        tradePlansRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        
        // Set up touch listener for pull-down detection
        tradePlansRecyclerView!!.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        // Initialize item managers FIRST
        Log.d(TAG, "开始初始化 itemTradePlanManager")
        
        // Initialize TradePlansViewModel
        tradePlansViewModel.initialize()
        
        itemTradePlanManager = ItemTradePlanManager(
            requireContext(),
            viewLifecycleOwner,
            tradePlansViewModel,
            ::onTradePlanItemUpdate
        )
        Log.d(TAG, "itemTradePlanManager 初始化完成")
        
        itemTradePlanSummaryManager = ItemTradePlanSummaryManager(
            requireContext(),
            viewLifecycleOwner,
            tradePlansViewModel,
            ::onTradePlanSummaryItemUpdate
        )
        Log.d(TAG, "itemTradePlanSummaryManager 初始化完成")
        
        // Initialize TradePlansAdapter
        adapter = TradePlansAdapter(
            null,
            object : TradePlansAdapter.OnTradePlanClickListener {
                override fun onTradePlanClick(tradePlanResponse: TradePlanResponse?) {
                    if (!isSelectionMode) {
                        openTradePlanDetail(tradePlanResponse)
                    }
                }
                
                override fun onTradePlanLongClick(tradePlanResponse: TradePlanResponse?) {
                    if (!isSelectionMode) {
                        enterSelectionMode()
                        tradePlanResponse?.id?.let { id ->
                            adapter?.toggleSelection(id)
                            selectedTradePlans.add(id)
                        }
                    }
                }
                
                override fun onSelectionChanged(selectedIds: Set<String>) {
                    selectedTradePlans.clear()
                    selectedTradePlans.addAll(selectedIds)
                    updateCommandSection()
                    
                    if (isSelectionMode) {
                        syncTradePlanStatuses()
                    }
                }
                
                override fun onExecuteApprovedPlans() {
                    itemTradePlanManager.executeApprovedPlans()
                }
                
                override fun onCompleteSelection() {
                    exitSelectionMode()
                }
            }
        )
        tradePlansRecyclerView!!.setAdapter(adapter)
        
        // Start item managers
        Log.d(TAG, "调用 itemTradePlanManager.initialize()")
        itemTradePlanManager.initialize()
        Log.d(TAG, "itemTradePlanManager.initialize() 调用完成")
        itemTradePlanSummaryManager.initialize()
        
        // Initial update of command section
        updateCommandSection()
    }

    override fun setupObservers() {
        Log.d(TAG, "setupObservers: 开始设置观察者")
        tradePlansViewModel.availableTradePlans.observe(viewLifecycleOwner) { tradePlanEntities ->
            Log.d(TAG, "收到交易计划数据更新: ${tradePlanEntities.size} 个交易计划")
            
            tradePlanResponseItems?.clear()
            
            val sortedEntities = tradePlanEntities.sortedByDescending { it.createdAt }
            
            val updatedTradePlanResponses = sortedEntities.map { entity ->
                TradePlanResponse(
                    id = entity.id,
                    script_id = entity.script_id,
                    name = entity.name,
                    title = entity.title,
                    subtitle = entity.subtitle,
                    description = entity.description,
                    exchange = entity.exchange,
                    symbol = entity.symbol,
                    symbol_name = entity.symbol_name,
                    ohlcv = entity.ohlcv,
                    change_percent = entity.change_percent,
                    data = entity.data,
                    status = entity.status,
                    executionStatus = entity.executionStatus,
                    executionResult = entity.executionResult,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
            
            tradePlanResponseItems?.addAll(updatedTradePlanResponses)
            
            tradePlansItemsList.clear()
            
            val summaryItem = itemTradePlanSummaryManager.getCurrentItem()
            tradePlansItemsList.add(summaryItem)
            
            updatedTradePlanResponses.forEach { tradePlanResponse: TradePlanResponse ->
                val itemTradePlan = TradePlansItem.ItemTradePlans(
                    status = tradePlanResponse.status ?: "PENDING",
                    executionStatus = tradePlanResponse.executionResult ?: "IDLE",
                    pendingCount = 0,
                    approvedCount = 0,
                    rejectedCount = 0,
                    executedSuccessCount = 0,
                    executedFailedCount = 0
                )
                tradePlansItemsList.add(itemTradePlan)
            }
            
            val adapterItems = mutableListOf<Any>()
            adapterItems.add(TradePlansAdapter.SummaryItem())
            tradePlanResponseItems?.let { adapterItems.addAll(it) }
            adapter?.updateTradePlans(tradePlanResponseItems)
            
            updateSummarySection(summaryItem)
            Log.d(TAG, "UI 已更新: ${updatedTradePlanResponses.size} 个交易计划")
        }
        Log.d(TAG, "setupObservers: 观察者设置完成")
    }
    
    /**
     * Callback for trade plan item updates
     */
    private fun onTradePlanItemUpdate(item: TradePlansItem) {
        if (item is TradePlansItem.ItemTradePlans) {
            Log.d(TAG, "onTradePlanItemUpdate called: status=${item.status}, executionStatus=${item.executionStatus}")
        }
    }
    
    /**
     * Callback for trade plan summary item updates
     */
    private fun onTradePlanSummaryItemUpdate(item: TradePlansItem) {
        if (item is TradePlansItem.ItemTradePlansSummary) {
            Log.d(TAG, "onTradePlanSummaryItemUpdate called: status=${item.status}")
        }
    }
    
    /**
     * Update summary section with data from ItemTradePlanSummaryManager
     */
    private fun updateSummarySection(summaryItem: TradePlansItem.ItemTradePlansSummary) {
        val summary = TradePlansAdapter.TradePlanSummary(
            pendingCount = summaryItem.pendingCount,
            approvedCount = summaryItem.approvedCount,
            rejectedCount = summaryItem.rejectedCount,
            executedSuccessCount = summaryItem.executedSuccessCount,
            executedFailedCount = summaryItem.executedFailedCount
        )
        
        adapter?.updateSummary(summary)
    }
    
    /**
     * 进入多选模式
     */
    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter?.setSelectionMode(true)
        updateCommandSection()
    }
    
    /**
     * 退出多选模式
     */
    private fun exitSelectionMode() {
        isSelectionMode = false
        adapter?.setSelectionMode(false)
        selectedTradePlans.clear()
        updateCommandSection()
    }
    
    /**
     * 同步交易计划状态（每次点击立即同步）
     */
    private fun syncTradePlanStatuses() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                selectedTradePlans.forEach { id ->
                    try {
                        itemTradePlanManager.updateTradePlanStatus(id, "approved")
                        Log.d(TAG, "交易计划 $id 状态已同步: approved")
                    } catch (e: Exception) {
                        Log.e(TAG, "同步交易计划 $id 状态失败: ${e.message}")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    refreshTradePlans()
                }
            } catch (e: Exception) {
                Log.e(TAG, "同步交易计划状态失败: ${e.message}")
            }
        }
    }
    
    /**
     * 更新命令区域状态
     */
    private fun updateCommandSection() {
        adapter?.notifyItemChanged(0)
        updateSummarySection(itemTradePlanSummaryManager.getCurrentItem())
    }
    
    /**
     * 刷新交易计划数据
     */
    private fun refreshTradePlans() {
        tradePlansViewModel.refresh()
    }
    
    /**
     * 显示消息
     */
    private fun showMessage(message: String) {
        android.util.Log.d(TAG, message)
    }

    private fun openTradePlanDetail(tradePlanResponse: TradePlanResponse?) {
        // Get trade plan ID
        val tradePlanId = tradePlanResponse?.id
        
        // Navigate to TradePlanFragment using Navigation Component
        if (tradePlanId != null) {
            val action = TradePlansFragmentDirections.actionNavTradeplansToTradeplanDetailFragment(tradePlanId)
            findNavController().navigate(action)
        }
    }
    
    /**
     * Handle touch events for pull-down detection
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = event.y
                isPullingDown = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - touchStartY
                
                // Check if pulling down at the top of the list
                if (deltaY > touchSlop && !isPullingDown) {
                    val layoutManager = tradePlansRecyclerView?.layoutManager as? LinearLayoutManager
                    if (layoutManager?.findFirstVisibleItemPosition() == 0) {
                        // At the top of the list and pulling down
                        isPullingDown = true
                        appBarLayout?.visibility = VISIBLE
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPullingDown = false
            }
        }
        return false
    }
}