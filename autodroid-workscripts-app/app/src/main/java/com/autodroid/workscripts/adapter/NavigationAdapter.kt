package com.autodroid.workscripts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.workscripts.R
import com.autodroid.workscripts.model.NavigationItem

class NavigationAdapter(
    private val onAppClick: (NavigationItem.AppItem) -> Unit,
    private val onFlowClick: (NavigationItem.FlowItem) -> Unit,
    private val onPageClick: (NavigationItem.StepItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var flatItems: List<NavigationItem> = emptyList()
    val appItems = mutableListOf<NavigationItem.AppItem>()

    fun setData(apps: List<NavigationItem.AppItem>) {
        // 保存展开状态
        val expansionState = mutableMapOf<String, Boolean>()
        val flowExpansionState = mutableMapOf<String, Boolean>()
        
        appItems.forEach { app ->
            expansionState[app.name] = app.isExpanded
            app.flows?.forEach { flow ->
                flowExpansionState[flow.name] = flow.isExpanded
            }
        }
        
        appItems.clear()
        appItems.addAll(apps)
        
        // 恢复展开状态
        appItems.forEach { app ->
            app.isExpanded = expansionState[app.name] ?: false
            app.flows?.forEach { flow ->
                flow.isExpanded = flowExpansionState[flow.name] ?: false
            }
        }
        
        updateFlatItems()
    }

    fun toggleAppExpansion(app: NavigationItem.AppItem) {
        app.isExpanded = !app.isExpanded
        updateFlatItems()
    }
    
    fun toggleFlowExpansion(flow: NavigationItem.FlowItem) {
        flow.isExpanded = !flow.isExpanded
        updateFlatItems()
    }
    
    private fun updateFlatItems() {
        val items = mutableListOf<NavigationItem>()
        
        appItems.forEach { app ->
            // 添加应用项
            items.add(app)
            
            // 如果应用展开，添加流程项
            if (app.isExpanded) {
                // 添加流程项
                app.flows?.forEach { flow ->
                    items.add(flow)
                    
                    // 如果流程展开，添加页面项
                    if (flow.isExpanded) {
                        flow.pages?.forEach { page ->
                            items.add(page)
                        }
                    }
                }
            }
        }
        
        flatItems = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (flatItems[position]) {
            is NavigationItem.AppItem -> VIEW_TYPE_APP
            is NavigationItem.FlowItem -> VIEW_TYPE_FLOW
            is NavigationItem.StepItem -> VIEW_TYPE_STEP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_APP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app, parent, false)
                AppViewHolder(view)
            }
            VIEW_TYPE_FLOW -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_flow, parent, false)
                FlowViewHolder(view)
            }
            VIEW_TYPE_STEP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_step, parent, false)
                PageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flatItems[position]) {
            is NavigationItem.AppItem -> {
                (holder as AppViewHolder).bind(item)
            }
            is NavigationItem.FlowItem -> {
                (holder as FlowViewHolder).bind(item)
            }
            is NavigationItem.StepItem -> {
                (holder as PageViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount(): Int {
        val count = flatItems.size
        println("getItemCount: $count")
        return count
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val appDescriptionTextView: TextView = itemView.findViewById(R.id.appDescriptionTextView)
        private val flowCountTextView: TextView = itemView.findViewById(R.id.flowCountTextView)
        
        fun bind(app: NavigationItem.AppItem) {
            // 设置应用名称
            appNameTextView.text = app.name
            
            // We don't have description in AppItem anymore, so hide the description view
            appDescriptionTextView.visibility = View.GONE
            
            // 设置流程数量
            val flowCount = app.flows?.size ?: 0
            flowCountTextView.text = if (flowCount > 0) {
                "$flowCount 个测试流程"
            } else {
                "无测试流程"
            }
            
            itemView.setOnClickListener {
                onAppClick(app)
            }
        }
    }

    inner class FlowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flowNameTextView: TextView = itemView.findViewById(R.id.flowNameTextView)
        private val flowDescriptionTextView: TextView = itemView.findViewById(R.id.flowDescriptionTextView)
        private val expandIndicator: TextView = itemView.findViewById(R.id.expandIndicator)
        private val itemContainer: View = itemView.findViewById(R.id.itemContainer)

        fun bind(flow: NavigationItem.FlowItem) {
            // 设置缩进
            val padding = (16 + 1 * 24) * itemView.context.resources.displayMetrics.density
            itemContainer.setPadding(padding.toInt(), 12, 12, 12)
            
            // 设置流程名称
            flowNameTextView.text = flow.name
            
            // 设置流程描述
            val description = flow.description
            if (!description.isNullOrEmpty()) {
                flowDescriptionTextView.text = description
                flowDescriptionTextView.visibility = View.VISIBLE
            } else {
                flowDescriptionTextView.visibility = View.GONE
            }
            
            // 设置展开指示器
            expandIndicator.text = if (flow.isExpanded) "▼" else "▶"
            
            // 点击整个项目区域时触发onFlowClick（用于导航到流程页面）
            itemView.setOnClickListener {
                onFlowClick(flow)
            }
            
            // 点击展开指示器时切换流程的展开/收起状态
            expandIndicator.setOnClickListener {
                toggleFlowExpansion(flow)
            }
        }
    }

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pageNameTextView: TextView = itemView.findViewById(R.id.pageNameTextView)

        fun bind(page: NavigationItem.StepItem) {
            // 设置缩进
            val padding = (16 + 2 * 24) * itemView.context.resources.displayMetrics.density
            itemView.setPadding(padding.toInt(), 12, 12, 12)
            
            // 设置页面名称
            pageNameTextView.text = page.name
            
            itemView.setOnClickListener {
                onPageClick(page)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_APP = 0
        private const val VIEW_TYPE_FLOW = 1
        private const val VIEW_TYPE_STEP = 2
    }
}