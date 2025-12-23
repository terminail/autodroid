package com.autodroid.trader.ui.tradeplans

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.model.TradePlan

class TradePlansAdapter(
    private var items: MutableList<Any>?,
    private val listener: OnTradePlanClickListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    
    companion object {
        private const val TAG = "TradePlanAdapter"
        private const val TYPE_TRADE_PLAN = 0
        private const val TYPE_SUMMARY = 1
    }
    
    // 多选模式状态
    private var isSelectionMode = false
    private val selectedTradePlans = mutableSetOf<String>()
    
    interface OnTradePlanClickListener {
        fun onTradePlanClick(tradePlan: TradePlan?)
        fun onTradePlanLongClick(tradePlan: TradePlan?)
        fun onSelectionChanged(selectedIds: Set<String>)
        fun onExecuteApprovedPlans()
        fun onCompleteSelection()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SUMMARY -> {
                val view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trade_plan_summary, parent, false)
                SummaryViewHolder(view)
            }
            TYPE_TRADE_PLAN -> {
                val view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trade_plan, parent, false)
                TradePlanViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_SUMMARY -> {
                val summaryHolder = holder as SummaryViewHolder
                summaryHolder.bind(getCurrentSummary())
            }
            TYPE_TRADE_PLAN -> {
                val tradePlan = items!![position] as TradePlan
                val tradePlanHolder = holder as TradePlanViewHolder
                tradePlanHolder.bind(tradePlan, isSelectionMode, selectedTradePlans.contains(tradePlan.id))
            }
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items!![position]) {
            is TradePlan -> TYPE_TRADE_PLAN
            else -> TYPE_SUMMARY
        }
    }

    override fun getItemCount(): Int {
        return if (items != null) items!!.size else 0
    }

    fun updateTradePlans(newTradePlans: MutableList<TradePlan>?) {
        val newItems = mutableListOf<Any>()
        newItems.add(SummaryItem())
        newTradePlans?.let { plans ->
            newItems.addAll(plans)
        }
        this.items = newItems
        notifyDataSetChanged()
    }
    
    fun updateItems(newItems: MutableList<Any>) {
        this.items = newItems
        notifyDataSetChanged()
    }
    
    private var currentSummary: TradePlanSummary? = null
    
    fun updateSummary(summary: TradePlanSummary) {
        currentSummary = summary
        notifyItemChanged(0)
    }
    
    fun getCurrentSummary(): TradePlanSummary {
        return currentSummary ?: getTradePlanSummary()
    }
    
    private fun getTradePlanSummary(): TradePlanSummary {
        var pendingCount = 0
        var approvedCount = 0
        var rejectedCount = 0
        var executedSuccessCount = 0
        var executedFailedCount = 0
        
        items?.forEach { item ->
            if (item is TradePlan) {
                when (item.status?.lowercase()) {
                    "pending" -> pendingCount++
                    "approved" -> approvedCount++
                    "rejected" -> rejectedCount++
                }
                
                if (!item.executionResult.isNullOrEmpty()) {
                    if (item.executionResult?.lowercase() == "success") {
                        executedSuccessCount++
                    } else {
                        executedFailedCount++
                    }
                }
            }
        }
        
        return TradePlanSummary(
            pendingCount = pendingCount,
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            executedSuccessCount = executedSuccessCount,
            executedFailedCount = executedFailedCount
        )
    }
    
    fun setSelectionMode(selectionMode: Boolean) {
        if (isSelectionMode != selectionMode) {
            isSelectionMode = selectionMode
            if (!selectionMode) {
                selectedTradePlans.clear()
            }
            notifyDataSetChanged()
        }
    }
    
    fun toggleSelection(tradePlanId: String) {
        if (selectedTradePlans.contains(tradePlanId)) {
            selectedTradePlans.remove(tradePlanId)
        } else {
            selectedTradePlans.add(tradePlanId)
        }
        
        listener?.onSelectionChanged(selectedTradePlans)
        
        items?.forEachIndexed { index, item ->
            if (item is TradePlan && item.id == tradePlanId) {
                notifyItemChanged(index)
                return@forEachIndexed
            }
        }
    }
    
    fun getSelectedTradePlans(): Set<String> {
        return selectedTradePlans.toSet()
    }
    
    fun clearSelection() {
        if (selectedTradePlans.isNotEmpty()) {
            val previouslySelected = selectedTradePlans.toSet()
            selectedTradePlans.clear()
            
            items?.forEachIndexed { index, item ->
                if (item is TradePlan && previouslySelected.contains(item.id)) {
                    notifyItemChanged(index)
                }
            }
            
            listener?.onSelectionChanged(selectedTradePlans)
        }
    }

    inner class TradePlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView
        private val nameView: TextView
        private val timeView: TextView
        private val infoLine1View: TextView
        private val infoLine2View: TextView
        private val statusView: TextView

        init {
            iconView = itemView.findViewById(R.id.trade_plan_icon)
            nameView = itemView.findViewById(R.id.trade_plan_name)
            timeView = itemView.findViewById(R.id.trade_plan_time)
            infoLine1View = itemView.findViewById(R.id.trade_plan_info_line1)
            infoLine2View = itemView.findViewById(R.id.trade_plan_info_line2)
            statusView = itemView.findViewById(R.id.trade_plan_status)
        }

        fun bind(tradePlan: TradePlan, isSelectionMode: Boolean, isSelected: Boolean) {
            nameView.text = tradePlan.name ?: tradePlan.title ?: "Unknown Trade Plan"
            timeView.text = tradePlan.getDisplayTime()
            infoLine1View.text = tradePlan.getDisplayInfoLine1()
            infoLine2View.text = tradePlan.getDisplayInfoLine2()
            
            val status = tradePlan.status ?: "PENDING"
            statusView.text = status
            
            when (status.lowercase()) {
                "approved" -> statusView.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                "pending" -> statusView.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                "rejected" -> statusView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                "executing" -> statusView.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                "completed" -> statusView.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                "failed" -> statusView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                else -> statusView.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
            }
            
            if (isSelectionMode) {
                itemView.setOnClickListener {
                    toggleSelection(tradePlan.id ?: return@setOnClickListener)
                }
                itemView.setOnLongClickListener(null)
            } else {
                itemView.setOnClickListener {
                    listener?.onTradePlanClick(tradePlan)
                }
                itemView.setOnLongClickListener {
                    listener?.onTradePlanLongClick(tradePlan)
                    true
                }
            }
        }
    }
    
    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusSummaryTextView: TextView
        private val executeApprovedButton: Button
        private val completeButton: Button

        init {
            statusSummaryTextView = itemView.findViewById(R.id.status_summary)
            executeApprovedButton = itemView.findViewById(R.id.btn_execute_approved)
            completeButton = itemView.findViewById(R.id.btn_complete)
            
            executeApprovedButton.setOnClickListener {
                listener?.onExecuteApprovedPlans()
            }
            
            completeButton.setOnClickListener {
                listener?.onCompleteSelection()
            }
        }

        fun bind(summary: TradePlanSummary) {
            val summaryToUse = currentSummary ?: summary
            statusSummaryTextView.text = "待批准: ${summaryToUse.pendingCount} | 已批准: ${summaryToUse.approvedCount} | " +
                    "已否决: ${summaryToUse.rejectedCount} | 执行成功: ${summaryToUse.executedSuccessCount} | " +
                    "执行失败: ${summaryToUse.executedFailedCount}"
            
            executeApprovedButton.isEnabled = summaryToUse.approvedCount > 0
            completeButton.visibility = if (selectedTradePlans.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    data class TradePlanSummary(
        val pendingCount: Int,
        val approvedCount: Int,
        val rejectedCount: Int,
        val executedSuccessCount: Int,
        val executedFailedCount: Int
    )
    
    class SummaryItem
}
