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
 * MindMap树形结构适配器
 * 用于显示MindMap的树形节点结构
 */
class MindMapTreeAdapter(private var nodes: List<MindMapNode> = emptyList()) : 
    RecyclerView.Adapter<MindMapTreeAdapter.MindMapNodeViewHolder>() {

    /**
     * 更新节点数据
     */
    fun updateNodes(newNodes: List<MindMapNode>) {
        nodes = newNodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MindMapNodeViewHolder {
        val binding = ItemChatMindmapNodeBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return MindMapNodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MindMapNodeViewHolder, position: Int) {
        holder.bind(nodes[position])
    }

    override fun getItemCount(): Int = nodes.size

    /**
     * MindMap节点ViewHolder
     */
    class MindMapNodeViewHolder(private val binding: ItemChatMindmapNodeBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(node: MindMapNode) {
            binding.nodeTitle.text = node.title
            binding.progressText.text = "${node.progress}%"
            binding.nodeProgressBar.progress = node.progress
            
            // 根据进度设置颜色
            val progressColor = when {
                node.progress == 0 -> R.color.red
                node.progress < 100 -> R.color.yellow
                else -> R.color.green
            }
            
            binding.progressIndicator.setBackgroundResource(progressColor)
            binding.nodeProgressBar.progressTintList = 
                binding.root.context.getColorStateList(progressColor)
            
            // 设置缩进（根据层级关系）
            val indentLevel = calculateIndentLevel(node)
            val indentWeight = indentLevel * 1.0f // 每级缩进1个单位
            binding.indentSpace.layoutParams = 
                binding.indentSpace.layoutParams.apply {
                    (this as? LinearLayout.LayoutParams)?.weight = indentWeight
                }
        }
        
        /**
         * 计算节点的缩进级别（简化版本，实际应该根据父子关系计算）
         */
        private fun calculateIndentLevel(node: MindMapNode): Int {
            // 简化实现：根节点缩进0，其他节点缩进1
            return if (node.parentId == null) 0 else 1
        }
    }
}