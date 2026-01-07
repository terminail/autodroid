package com.autodroid.trader.app.ui.tradeplans

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.viewModels
import com.autodroid.trader.app.R
import com.autodroid.trader.app.ui.BaseFragment
import com.autodroid.trader.app.data.dao.TradePlanEntity
import com.autodroid.trader.app.managers.TradePlanManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.CoroutineScope
import android.util.Log

class TradePlansFragment : BaseFragment() {
    private val TAG = "TradePlansFragment"
    
    private var tradePlansRecyclerView: RecyclerView? = null
    private var adapter: TradePlansAdapter? = null
    
    private lateinit var tradePlanManager: TradePlanManager
    
    private val tradePlansViewModel: TradePlansViewModel by viewModels()
    
    private var appBarLayout: AppBarLayout? = null
    private var touchStartY = 0f
    private var isPullingDown = false
    private var touchSlop = 0
    
    private val selectedStatusFilters = mutableSetOf<String>()
    
    private lateinit var itemTradePlanManager: ItemTradePlanManager
    private lateinit var itemTradePlanSummaryManager: ItemTradePlanSummaryManager
    
    private val tradePlansItemsList = mutableListOf<Any>()
    
    private var isSelectionMode = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tradePlanManager = TradePlanManager.getInstance(requireContext())
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_trade_plans
    }

    override fun initViews(view: View) {
        tradePlansRecyclerView = view?.findViewById<RecyclerView>(R.id.tradeplans_recycler_view)
        
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        tradePlansRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        
        tradePlansRecyclerView!!.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        Log.d(TAG, "开始初始化 itemTradePlanManager")
        
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
        
        adapter = TradePlansAdapter(
            null,
            object : TradePlansAdapter.OnTradePlanClickListener {
                override fun onTradePlanClick(tradePlanEntity: TradePlanEntity?) {
                    if (!isSelectionMode) {
                        openTradePlanDetail(tradePlanEntity)
                    }
                }
                
                override fun onTradePlanLongClick(tradePlanEntity: TradePlanEntity?) {
                    android.util.Log.d(TAG, "onTradePlanLongClick: entityId=${tradePlanEntity?.id}")
                    if (!isSelectionMode) {
                        enterSelectionMode()
                        tradePlanEntity?.id?.let { id ->
                            selectTradePlan(id)
                        }
                    }
                }
                
                override fun onTradePlanStatusToggle(tradePlanEntity: TradePlanEntity?) {
                    android.util.Log.d(TAG, "onTradePlanStatusToggle: entityId=${tradePlanEntity?.id}, status=${tradePlanEntity?.status}")
                    if (tradePlanEntity != null) {
                        tradePlansViewModel.toggleTradePlanStatus(tradePlanEntity.id, tradePlanEntity.status)
                    }
                }
                
                override fun onExecuteApprovedPlans() {
                    android.util.Log.d(TAG, "onExecuteApprovedPlans called!")
                    itemTradePlanManager.executeApprovedPlans()
                }
                
                override fun onCompleteSelection() {
                    android.util.Log.d(TAG, "onCompleteSelection: exit selection mode")
                    exitSelectionMode()
                }
                
                override fun onStatusFilterChanged(status: String, isChecked: Boolean) {
                    if (status == "ALL") {
                        if (isChecked) {
                            selectedStatusFilters.addAll(listOf("PENDING", "APPROVED", "REJECTED", "COMPLETED", "FAILED"))
                        } else {
                            selectedStatusFilters.clear()
                        }
                    } else {
                        if (isChecked) {
                            selectedStatusFilters.add(status)
                        } else {
                            selectedStatusFilters.remove(status)
                        }
                    }
                    applyStatusFilter()
                }
            }
        )
        tradePlansRecyclerView!!.setAdapter(adapter)
        
        itemTradePlanSummaryManager.setAdapter(adapter)
        
        Log.d(TAG, "调用 itemTradePlanManager.initialize()")
        itemTradePlanManager.initialize()
        Log.d(TAG, "itemTradePlanManager.initialize() 调用完成")
        itemTradePlanSummaryManager.initialize()
        
        updateCommandSection()
    }
    
    private fun enterSelectionMode() {
        android.util.Log.d(TAG, "enterSelectionMode")
        isSelectionMode = true
        adapter?.setSelectionMode(true)
        updateSummarySection()
    }
    
    private fun exitSelectionMode() {
        android.util.Log.d(TAG, "exitSelectionMode")
        isSelectionMode = false
        adapter?.setSelectionMode(false)
        updateSummarySection()
    }
    
    private fun selectTradePlan(tradePlanId: String) {
        android.util.Log.d(TAG, "selectTradePlan: $tradePlanId")
        adapter?.setSelectionMode(true)
    }
    
    private fun toggleTradePlanSelection(tradePlanId: String) {
        android.util.Log.d(TAG, "toggleTradePlanSelection: $tradePlanId")
        adapter?.setSelectionMode(true)
    }

    override fun setupObservers() {
        Log.d(TAG, "setupObservers: 开始设置观察者")
        tradePlansViewModel.availableTradePlans.observe(viewLifecycleOwner) { tradePlanEntities ->
            Log.d(TAG, "收到交易计划数据更新: ${tradePlanEntities.size} 个交易计划")
            
            val sortedEntities = tradePlanEntities.sortedByDescending { it.createdAt }
            
            tradePlansItemsList.clear()
            
            tradePlansItemsList.add(TradePlansAdapter.SummaryItem())
            
            sortedEntities.forEach { entity ->
                tradePlansItemsList.add(entity)
            }
            
            applyStatusFilter()
            updateSummarySection()
            Log.d(TAG, "UI 已更新: ${sortedEntities.size} 个交易计划")
        }
        Log.d(TAG, "setupObservers: 观察者设置完成")
    }
    
    private fun applyStatusFilter() {
        val allItems = tradePlansItemsList.filterIsInstance<TradePlanEntity>()
        
        val filteredItems = if (selectedStatusFilters.isEmpty()) {
            allItems
        } else {
            allItems.filter { entity ->
                val status = entity.status?.uppercase() ?: ""
                val executionResult = entity.executionResult?.uppercase() ?: ""

                when (status) {
                    "PENDING" -> selectedStatusFilters.contains("PENDING")
                    "APPROVED" -> selectedStatusFilters.contains("APPROVED")
                    "REJECTED" -> selectedStatusFilters.contains("REJECTED")
                    "COMPLETED" -> selectedStatusFilters.contains("COMPLETED")
                    "FAILED" -> selectedStatusFilters.contains("FAILED")
                    else -> {
                        if (executionResult == "SUCCESS") {
                            selectedStatusFilters.contains("COMPLETED")
                        } else if (executionResult == "FAILED") {
                            selectedStatusFilters.contains("FAILED")
                        } else {
                            selectedStatusFilters.contains("PENDING")
                        }
                    }
                }
            }
        }
        
        val adapterItems = mutableListOf<Any>()
        adapterItems.add(TradePlansAdapter.SummaryItem())
        adapterItems.addAll(filteredItems)
        adapter?.updateItems(adapterItems)
    }
    
    private fun onTradePlanItemUpdate(item: TradePlansItem) {
        if (item is TradePlansItem.ItemTradePlans) {
            Log.d(TAG, "onTradePlanItemUpdate called: status=${item.status}, executionStatus=${item.executionStatus}")
        }
    }
    
    private fun onTradePlanSummaryItemUpdate(item: TradePlansItem) {
        if (item is TradePlansItem.ItemTradePlansSummary) {
            Log.d(TAG, "onTradePlanSummaryItemUpdate called: status=${item.status}")
        }
    }
    
    private fun updateSummarySection() {
        var pendingCount = 0
        var approvedCount = 0
        var rejectedCount = 0
        var executedSuccessCount = 0
        var executedFailedCount = 0
        
        tradePlansItemsList.forEach { item ->
            if (item is TradePlanEntity) {
                when (item.status?.uppercase()) {
                    "PENDING" -> pendingCount++
                    "APPROVED" -> approvedCount++
                    "REJECTED" -> rejectedCount++
                }
                
                if (!item.executionResult.isNullOrEmpty()) {
                    if (item.executionResult?.uppercase() == "SUCCESS") {
                        executedSuccessCount++
                    } else {
                        executedFailedCount++
                    }
                }
            }
        }
        
        val summary = TradePlansAdapter.TradePlanSummary(
            pendingCount = pendingCount,
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            executedSuccessCount = executedSuccessCount,
            executedFailedCount = executedFailedCount
        )
        
        adapter?.updateSummary(summary)
    }
    
    private fun updateCommandSection() {
        android.util.Log.d(TAG, "updateCommandSection: isSelectionMode=$isSelectionMode")
        adapter?.setSelectionMode(isSelectionMode)
        updateSummarySection()
    }
    
    private fun refreshTradePlans() {
        tradePlansViewModel.refresh()
    }
    
    private fun showMessage(message: String) {
        android.util.Log.d(TAG, message)
    }

    private fun openTradePlanDetail(tradePlanEntity: TradePlanEntity?) {
        val tradePlanId = tradePlanEntity?.id
        val tradePlanTitle = tradePlanEntity?.getDisplayName()
        
        if (tradePlanId != null) {
            val action = TradePlansFragmentDirections.actionNavTradeplansToTradeplanDetailFragment(
                tradePlanId,
                tradePlanTitle
            )
            findNavController().navigate(action)
        }
    }
    
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = event.y
                isPullingDown = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - touchStartY
                
                if (deltaY > touchSlop && !isPullingDown) {
                    val layoutManager = tradePlansRecyclerView?.layoutManager as? LinearLayoutManager
                    if (layoutManager?.findFirstVisibleItemPosition() == 0) {
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
