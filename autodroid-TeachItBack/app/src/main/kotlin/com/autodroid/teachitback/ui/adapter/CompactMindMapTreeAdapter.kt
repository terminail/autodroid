package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.ItemChatMindmapNodeBinding
import com.autodroid.teachitback.model.MindMapNode

/**
 * 紧凑版MindMap树形结构适配器
 * 使用更紧凑的布局显示MindMap节点，减少空间占用
 */
class CompactMindMapTreeAdapter(private var nodes: List<MindMapNode> = emptyList()) : 
    RecyclerView.Adapter<CompactMindMapTreeAdapter.CompactMindMapNodeViewHolder>() {

    /**
     * 更新节点数据
     */
    fun updateNodes(newNodes: List<MindMapNode>) {
        nodes = newNodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompactMindMapNodeViewHolder {
        val binding = ItemChatMindmapNodeBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return CompactMindMapNodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompactMindMapNodeViewHolder, position: Int) {
        holder.bind(nodes[position])
    }

    override fun getItemCount(): Int = nodes.size

    /**
     * 紧凑版MindMap节点ViewHolder
     */
    class CompactMindMapNodeViewHolder(private val binding: ItemChatMindmapNodeBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(node: MindMapNode) {
            // 简化显示：只显示标题和进度
            binding.nodeTitle.text = node.title
            binding.progressText.text = "${node.progress}%"
            binding.nodeProgressBar.progress = node.progress
            
            // 隐藏不必要的元素
            binding.nodeDescription.visibility = View.GONE
            binding.expandCollapseIcon.visibility = View.GONE
            
            // 根据进度设置颜色
            val progressColor = when {
                node.progress == 0 -> R.color.red
                node.progress < 100 -> R.color.yellow
                else -> R.color.green
            }
            
            binding.progressIndicator.setBackgroundResource(progressColor)
            binding.nodeProgressBar.progressTintList = 
                binding.root.context.getColorStateList(progressColor)
            
            // 简化缩进：只区分根节点和子节点
            val indentLevel = if (node.parentId == null) 0 else 1
            val indentWeight = indentLevel * 0.3f // 减少缩进距离
            binding.indentSpace.layoutParams = 
                binding.indentSpace.layoutParams.apply {
                    (this as? LinearLayout.LayoutParams)?.weight = indentWeight
                }
            
            // 减少内边距
            binding.root.setPadding(
                binding.root.paddingLeft,
                2, // 减少垂直内边距
                binding.root.paddingRight,
                2
            )
            
            // 减少节点内容容器的内边距
            binding.nodeTitle.setPadding(
                binding.nodeTitle.paddingLeft,
                2,
                binding.nodeTitle.paddingRight,
                2
            )
            
            // 减小字体大小
            binding.nodeTitle.textSize = 12f
            binding.progressText.textSize = 10f
        }
    }
}