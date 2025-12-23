package com.autodroid.trader.ui.tradeplans

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.ui.BaseFragment
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.managers.TradePlanManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class TradePlansFragment : BaseFragment() {
    private val TAG = "TradePlansFragment"
    
    private var tradePlansTitleTextView: TextView? = null
    private var tradePlansRecyclerView: RecyclerView? = null
    private var adapter: TradePlansAdapter? = null
    private var tradePlanItems: MutableList<TradePlan>? = null
    
    // Trade plan manager
    private lateinit var tradePlanManager: TradePlanManager
    
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
        tradePlanItems = ArrayList<TradePlan>()
        tradePlanManager = TradePlanManager.getInstance(requireContext())
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_trade_plans
    }

    override fun initViews(view: View) {
        tradePlansTitleTextView = view?.findViewById<TextView?>(R.id.tradeplans_title)
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
        itemTradePlanManager = ItemTradePlanManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onTradePlanItemUpdate
        )
        Log.d(TAG, "itemTradePlanManager 初始化完成")
        
        itemTradePlanSummaryManager = ItemTradePlanSummaryManager(
            requireContext(),
            viewLifecycleOwner,
            appViewModel,
            ::onTradePlanSummaryItemUpdate
        )
        Log.d(TAG, "itemTradePlanSummaryManager 初始化完成")
        
        // Initialize TradePlansAdapter
        adapter = TradePlansAdapter(
            null,
            object : TradePlansAdapter.OnTradePlanClickListener {
                override fun onTradePlanClick(tradePlan: TradePlan?) {
                    if (!isSelectionMode) {
                        openTradePlanDetail(tradePlan)
                    }
                }
                
                override fun onTradePlanLongClick(tradePlan: TradePlan?) {
                    if (!isSelectionMode) {
                        enterSelectionMode()
                        tradePlan?.id?.let { id ->
                            adapter?.toggleSelection(id)
                            selectedTradePlans.add(id)
                        }
                    }
                }
                
                override fun onSelectionChanged(selectedIds: Set<String>) {
                    selectedTradePlans.clear()
                    selectedTradePlans.addAll(selectedIds)
                    updateCommandSection()
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
        
        // Update UI with initial data
        updateUI()
        
        // Initial update of command section
        updateCommandSection()
    }

    override fun setupObservers() {
        // 观察者设置已在各个ItemManager中实现
    }
    
    /**
     * Callback for trade plan item updates
     */
    private fun onTradePlanItemUpdate(item: TradePlansItem) {
        if (item is TradePlansItem.ItemTradePlans) {
            Log.d(TAG, "onTradePlanItemUpdate called: status=${item.status}, executionStatus=${item.executionStatus}")
        }
        updateUI()
    }
    
    /**
     * Callback for trade plan summary item updates
     */
    private fun onTradePlanSummaryItemUpdate(item: TradePlansItem) {
        if (item is TradePlansItem.ItemTradePlansSummary) {
            Log.d(TAG, "onTradePlanSummaryItemUpdate called: status=${item.status}")
        }
        updateUI()
    }
    
    /**
     * Update UI with current data
     */
    private fun updateUI() {
        tradePlansItemsList.clear()
        
        // Add summary item at the beginning
        val summaryItem = itemTradePlanSummaryManager.getCurrentItem()
        tradePlansItemsList.add(summaryItem)
        
        // Load trade plans from repository
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updatedTradePlans = tradePlanManager.getAllTradePlans()
                
                withContext(Dispatchers.Main) {
                    tradePlanItems?.clear()
                    tradePlanItems?.addAll(updatedTradePlans)
                    
                    // Add trade plan items
                    updatedTradePlans.forEach { tradePlan: TradePlan ->
                        val itemTradePlan = TradePlansItem.ItemTradePlans(
                            status = tradePlan.status ?: "PENDING",
                            executionStatus = tradePlan.executionResult ?: "IDLE",
                            pendingCount = 0,
                            approvedCount = 0,
                            rejectedCount = 0,
                            executedSuccessCount = 0,
                            executedFailedCount = 0
                        )
                        tradePlansItemsList.add(itemTradePlan)
                    }
                    
                    // Update adapter with new items
                    val adapterItems = mutableListOf<Any>()
                    adapterItems.add(TradePlansAdapter.SummaryItem())
                    tradePlanItems?.let { adapterItems.addAll(it) }
                    adapter?.updateTradePlans(tradePlanItems)
                    
                    // Update summary section with data from ItemTradePlanSummaryManager
                    updateSummarySection(summaryItem)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading trade plans: ${e.message}", e)
            }
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
        if (selectedTradePlans.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    showMessage("正在更新 ${selectedTradePlans.size} 个交易计划的状态...")
                    
                    selectedTradePlans.forEach { id ->
                        try {
                            itemTradePlanManager.updateTradePlanStatus(id, "approved")
                        } catch (e: Exception) {
                            Log.e(TAG, "更新交易计划 $id 状态失败: ${e.message}")
                        }
                    }
                    
                    refreshTradePlans()
                    
                } catch (e: Exception) {
                    showMessage("更新交易计划状态失败: ${e.message}")
                }
            }
        }
        
        isSelectionMode = false
        adapter?.setSelectionMode(false)
        selectedTradePlans.clear()
        updateCommandSection()
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
        itemTradePlanManager.refresh()
        itemTradePlanSummaryManager.refresh()
        updateUI()
    }
    
    /**
     * 显示消息
     */
    private fun showMessage(message: String) {
        android.util.Log.d(TAG, message)
    }

    private fun openTradePlanDetail(tradePlan: TradePlan?) {
        // Get trade plan ID
        val tradePlanId = tradePlan?.id
        
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