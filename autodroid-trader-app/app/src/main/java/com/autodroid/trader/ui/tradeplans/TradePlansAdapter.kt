package com.autodroid.trader.ui.tradeplans

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.data.dao.TradePlanEntity

class TradePlansAdapter(
    private var items: MutableList<Any>?,
    private val listener: OnTradePlanClickListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val TAG = "TradePlanAdapter"
        private const val TYPE_TRADE_PLAN = 0
        private const val TYPE_SUMMARY = 1
    }
    
    private var currentSummary: TradePlanSummary? = null
    private var isSelectionMode = false
    
    interface OnTradePlanClickListener {
        fun onTradePlanClick(tradePlanEntity: TradePlanEntity?)
        fun onTradePlanLongClick(tradePlanEntity: TradePlanEntity?)
        fun onTradePlanStatusToggle(tradePlanEntity: TradePlanEntity?)
        fun onExecuteApprovedPlans()
        fun onCompleteSelection()
        fun onStatusFilterChanged(status: String, isChecked: Boolean)
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
                val tradePlanEntity = items!![position] as TradePlanEntity
                val tradePlanHolder = holder as TradePlanViewHolder
                tradePlanHolder.bind(tradePlanEntity, isSelectionMode)
            }
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items!![position]) {
            is TradePlanEntity -> TYPE_TRADE_PLAN
            else -> TYPE_SUMMARY
        }
    }

    override fun getItemCount(): Int {
        return if (items != null) items!!.size else 0
    }

    fun updateTradePlans(newTradePlanEntities: MutableList<TradePlanEntity>?) {
        val newItems = mutableListOf<Any>()
        newItems.add(SummaryItem())
        newTradePlanEntities?.let { plans ->
            newItems.addAll(plans)
        }
        this.items = newItems
        notifyDataSetChanged()
    }
    
    fun updateItems(newItems: MutableList<Any>) {
        this.items = newItems
        notifyDataSetChanged()
    }
    
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
        
        return TradePlanSummary(
            pendingCount = pendingCount,
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            executedSuccessCount = executedSuccessCount,
            executedFailedCount = executedFailedCount
        )
    }
    
    fun setSelectionMode(selectionMode: Boolean) {
        android.util.Log.d(TAG, "setSelectionMode: selectionMode=$selectionMode")
        isSelectionMode = selectionMode
        notifyDataSetChanged()
    }
    
    fun clearSelection() {
        isSelectionMode = false
        notifyDataSetChanged()
    }

    inner class TradePlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkboxView: CheckBox
        private val iconView: ImageView
        private val timestampView: TextView
        private val nameView: TextView
        private val timeView: TextView
        private val infoLine1View: TextView
        private val infoLine2View: TextView
        private val statusView: TextView

        init {
            checkboxView = itemView.findViewById(R.id.trade_plan_checkbox)
            iconView = itemView.findViewById(R.id.trade_plan_icon)
            timestampView = itemView.findViewById(R.id.trade_plan_timestamp)
            nameView = itemView.findViewById(R.id.trade_plan_name)
            timeView = itemView.findViewById(R.id.trade_plan_time)
            infoLine1View = itemView.findViewById(R.id.trade_plan_info_line1)
            infoLine2View = itemView.findViewById(R.id.trade_plan_info_line2)
            statusView = itemView.findViewById(R.id.trade_plan_status)
        }

        fun bind(tradePlanEntity: TradePlanEntity, isSelectionMode: Boolean) {
            val displayTime = tradePlanEntity.getDisplayTime()
            timestampView.text = displayTime
            nameView.text = tradePlanEntity.getDisplayName()
            timeView.text = displayTime
            infoLine1View.text = tradePlanEntity.getDisplayInfoLine1()
            infoLine2View.text = tradePlanEntity.getDisplayInfoLine2()
            
            val status = tradePlanEntity.status ?: "PENDING"
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
            
            val canToggleStatus = status.lowercase() in listOf("pending", "approved", "rejected")
            
            if (isSelectionMode && canToggleStatus) {
                android.util.Log.d(TAG, "bind: SELECTION MODE, entityId=${tradePlanEntity.id}")
                checkboxView.visibility = View.VISIBLE
                iconView.visibility = View.GONE
                
                when (status.lowercase()) {
                    "pending" -> {
                        checkboxView.buttonDrawable = null
                        checkboxView.text = "☐"
                        checkboxView.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    }
                    "approved" -> {
                        checkboxView.buttonDrawable = null
                        checkboxView.text = "☑"
                        checkboxView.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    }
                    "rejected" -> {
                        checkboxView.buttonDrawable = null
                        checkboxView.text = "✕"
                        checkboxView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    }
                }
                
                checkboxView.setOnClickListener {
                    android.util.Log.d(TAG, "checkboxView click: calling onTradePlanStatusToggle")
                    listener?.onTradePlanStatusToggle(tradePlanEntity)
                }
                
                itemView.setOnClickListener(null)
                itemView.setOnLongClickListener(null)
            } else {
                android.util.Log.d(TAG, "bind: NORMAL MODE, entityId=${tradePlanEntity.id}")
                checkboxView.visibility = View.GONE
                iconView.visibility = View.VISIBLE
                checkboxView.setOnClickListener(null)
                
                itemView.setOnClickListener {
                    listener?.onTradePlanClick(tradePlanEntity)
                }
                itemView.setOnLongClickListener {
                    android.util.Log.d(TAG, "onLongClick: entityId=${tradePlanEntity.id}")
                    listener?.onTradePlanLongClick(tradePlanEntity)
                    true
                }
            }
        }
    }
    
    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkboxAll: CheckBox
        private val textAllCount: TextView
        private val checkboxPending: CheckBox
        private val textPendingCount: TextView
        private val checkboxApproved: CheckBox
        private val textApprovedCount: TextView
        private val checkboxRejected: CheckBox
        private val textRejectedCount: TextView
        private val checkboxCompleted: CheckBox
        private val textCompletedCount: TextView
        private val checkboxFailed: CheckBox
        private val textFailedCount: TextView
        private val executeApprovedButton: Button
        private val completeButton: Button

        init {
            checkboxAll = itemView.findViewById(R.id.checkbox_all)
            textAllCount = itemView.findViewById(R.id.text_all_count)
            checkboxPending = itemView.findViewById(R.id.checkbox_pending)
            textPendingCount = itemView.findViewById(R.id.text_pending_count)
            checkboxApproved = itemView.findViewById(R.id.checkbox_approved)
            textApprovedCount = itemView.findViewById(R.id.text_approved_count)
            checkboxRejected = itemView.findViewById(R.id.checkbox_rejected)
            textRejectedCount = itemView.findViewById(R.id.text_rejected_count)
            checkboxCompleted = itemView.findViewById(R.id.checkbox_completed)
            textCompletedCount = itemView.findViewById(R.id.text_completed_count)
            checkboxFailed = itemView.findViewById(R.id.checkbox_failed)
            textFailedCount = itemView.findViewById(R.id.text_failed_count)
            executeApprovedButton = itemView.findViewById(R.id.btn_execute_approved)
            completeButton = itemView.findViewById(R.id.btn_complete)
            
            checkboxAll.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkboxPending.isChecked = true
                    checkboxApproved.isChecked = true
                    checkboxRejected.isChecked = true
                    checkboxCompleted.isChecked = true
                    checkboxFailed.isChecked = true
                } else {
                    checkboxPending.isChecked = false
                    checkboxApproved.isChecked = false
                    checkboxRejected.isChecked = false
                    checkboxCompleted.isChecked = false
                    checkboxFailed.isChecked = false
                }
                listener?.onStatusFilterChanged("ALL", isChecked)
            }
            
            checkboxPending.setOnCheckedChangeListener { _, isChecked ->
                updateAllCheckboxState()
                listener?.onStatusFilterChanged("PENDING", isChecked)
            }
            
            checkboxApproved.setOnCheckedChangeListener { _, isChecked ->
                updateAllCheckboxState()
                listener?.onStatusFilterChanged("APPROVED", isChecked)
            }
            
            checkboxRejected.setOnCheckedChangeListener { _, isChecked ->
                updateAllCheckboxState()
                listener?.onStatusFilterChanged("REJECTED", isChecked)
            }
            
            checkboxCompleted.setOnCheckedChangeListener { _, isChecked ->
                updateAllCheckboxState()
                listener?.onStatusFilterChanged("COMPLETED", isChecked)
            }
            
            checkboxFailed.setOnCheckedChangeListener { _, isChecked ->
                updateAllCheckboxState()
                listener?.onStatusFilterChanged("FAILED", isChecked)
            }
            
            executeApprovedButton.setOnClickListener {
                android.util.Log.d(TAG, "executeApprovedButton clicked!")
                listener?.onExecuteApprovedPlans()
            }
            
            completeButton.setOnClickListener {
                listener?.onCompleteSelection()
            }
        }
        
        private fun updateAllCheckboxState() {
            val allChecked = checkboxPending.isChecked && checkboxApproved.isChecked && 
                           checkboxRejected.isChecked && checkboxCompleted.isChecked && 
                           checkboxFailed.isChecked
            checkboxAll.isChecked = allChecked
        }

        fun bind(summary: TradePlanSummary) {
            val summaryToUse = currentSummary ?: summary
            val totalCount = summaryToUse.pendingCount + summaryToUse.approvedCount + 
                           summaryToUse.rejectedCount + summaryToUse.executedSuccessCount + 
                           summaryToUse.executedFailedCount
            
            android.util.Log.d(TAG, "SummaryViewHolder.bind: isSelectionMode=$isSelectionMode")
            
            textAllCount.text = totalCount.toString()
            textPendingCount.text = summaryToUse.pendingCount.toString()
            textApprovedCount.text = summaryToUse.approvedCount.toString()
            textRejectedCount.text = summaryToUse.rejectedCount.toString()
            textCompletedCount.text = summaryToUse.executedSuccessCount.toString()
            textFailedCount.text = summaryToUse.executedFailedCount.toString()
            
            executeApprovedButton.isEnabled = summaryToUse.approvedCount > 0
            
            if (isSelectionMode) {
                android.util.Log.d(TAG, "SummaryViewHolder: SHOWING completeButton")
                executeApprovedButton.visibility = View.GONE
                completeButton.visibility = View.VISIBLE
            } else {
                android.util.Log.d(TAG, "SummaryViewHolder: SHOWING executeApprovedButton")
                executeApprovedButton.visibility = View.VISIBLE
                completeButton.visibility = View.GONE
            }
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
