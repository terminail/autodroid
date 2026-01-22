package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ProgressBar
import android.widget.TextView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.model.MindMapNode

/**
 * MindMap可展开列表适配器
 * 使用Android原生的ExpandableListView显示MindMap结构
 * 支持2级层级：组（学科）和子项（章节）
 */
class MindMapExpandableAdapter(private val nodes: List<MindMapNode>) : BaseExpandableListAdapter() {
    
    // 分组数据：根节点作为组，子节点作为子项
    private val groups: List<MindMapNode>
    private val children: Map<MindMapNode, List<MindMapNode>>
    
    init {
        // 分离根节点和子节点
        groups = nodes.filter { it.parentId == null }
        children = groups.associateWith { group ->
            nodes.filter { it.parentId == group.id }
        }
    }
    
    override fun getGroupCount(): Int = groups.size
    
    override fun getChildrenCount(groupPosition: Int): Int = 
        children[groups[groupPosition]]?.size ?: 0
    
    override fun getGroup(groupPosition: Int): Any = groups[groupPosition]
    
    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        val group = groups[groupPosition]
        return children[group]?.get(childPosition) ?: throw IndexOutOfBoundsException()
    }
    
    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
    
    override fun getChildId(groupPosition: Int, childPosition: Int): Long = 
        (groupPosition * 1000 + childPosition).toLong()
    
    override fun hasStableIds(): Boolean = true
    
    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView ?: LayoutInflater.from(parent?.context).inflate(
            R.layout.item_mindmap_group, parent, false
        )
        
        val group = getGroup(groupPosition) as MindMapNode
        
        val titleTextView = view.findViewById<TextView>(R.id.group_title)
        val progressTextView = view.findViewById<TextView>(R.id.group_progress)
        val progressBar = view.findViewById<ProgressBar>(R.id.group_progress_bar)
        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        
        titleTextView.text = group.title
        progressTextView.text = "${group.progress}%"
        progressBar.progress = group.progress
        
        // 根据进度设置颜色
        val progressColor = when {
            group.progress == 0 -> R.color.red
            group.progress < 100 -> R.color.yellow
            else -> R.color.green
        }
        
        progressIndicator.setBackgroundResource(progressColor)
        progressBar.progressTintList = view.context.getColorStateList(progressColor)
        
        return view
    }
    
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView ?: LayoutInflater.from(parent?.context).inflate(
            R.layout.item_mindmap_child, parent, false
        )
        
        val child = getChild(groupPosition, childPosition) as MindMapNode
        
        val titleTextView = view.findViewById<TextView>(R.id.child_title)
        val progressTextView = view.findViewById<TextView>(R.id.child_progress)
        val progressBar = view.findViewById<ProgressBar>(R.id.child_progress_bar)
        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        
        titleTextView.text = child.title
        progressTextView.text = "${child.progress}%"
        progressBar.progress = child.progress
        
        // 根据进度设置颜色
        val progressColor = when {
            child.progress == 0 -> R.color.red
            child.progress < 100 -> R.color.yellow
            else -> R.color.green
        }
        
        progressIndicator.setBackgroundResource(progressColor)
        progressBar.progressTintList = view.context.getColorStateList(progressColor)
        
        return view
    }
    
    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
}