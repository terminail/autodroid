package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.config.PresetTopicCategories
import com.autodroid.teachitback.databinding.ItemMindmapFlexboxBinding
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.model.TopicEntity

/**
 * 基于FlexboxLayout的多级树形结构适配器
 * 支持面包屑导航式的多级显示
 * 面包屑导航使用TopicTreeNode的分类路径而非MindMapNode的父节点路径
 */
class MindMapFlexboxAdapter(
    private var nodes: List<MindMapNode> = emptyList(),
    private var topic: TopicEntity? = null
) : RecyclerView.Adapter<MindMapFlexboxAdapter.MindMapFlexboxViewHolder>() {

    private var currentPath: List<MindMapNode> = emptyList()
    private var currentChildren: List<MindMapNode> = emptyList()
    private var currentNode: MindMapNode? = null

    private var onNodeClickListener: ((MindMapNode) -> Unit)? = null
    
    init {
        // 初始化：找到根节点
        val rootNodes = nodes.filter { it.parentId == null }
        if (rootNodes.isNotEmpty()) {
            navigateToNode(rootNodes.first())
        }
    }
    
    /**
     * 更新节点数据
     */
    fun updateNodes(newNodes: List<MindMapNode>, newTopic: TopicEntity? = null) {
        nodes = newNodes
        topic = newTopic

        // 重新计算当前路径
        if (currentNode != null) {
            val updatedNode = nodes.find { it.id == currentNode?.id }
            if (updatedNode != null) {
                navigateToNode(updatedNode)
            } else {
                // 如果当前节点不存在，回到根节点
                val rootNodes = nodes.filter { it.parentId == null }
                if (rootNodes.isNotEmpty()) {
                    navigateToNode(rootNodes.first())
                }
            }
        } else {
            val rootNodes = nodes.filter { it.parentId == null }
            if (rootNodes.isNotEmpty()) {
                navigateToNode(rootNodes.first())
            }
        }

        notifyDataSetChanged()
    }
    
    /**
     * 导航到指定节点
     */
    fun navigateToNode(node: MindMapNode) {
        currentNode = node
        
        // 构建路径（从根节点到当前节点）
        currentPath = buildPathToNode(node)
        
        // 获取当前节点的子节点
        currentChildren = nodes.filter { it.parentId == node.id }
        
        notifyDataSetChanged()
    }
    
    /**
     * 构建从根节点到指定节点的路径
     */
    private fun buildPathToNode(node: MindMapNode): List<MindMapNode> {
        val path = mutableListOf<MindMapNode>()
        var currentNode: MindMapNode? = node
        
        // 从当前节点向上回溯到根节点
        while (currentNode != null) {
            path.add(0, currentNode)
            currentNode = if (currentNode.parentId != null) {
                nodes.find { it.id == currentNode?.parentId }
            } else {
                null
            }
        }
        
        return path
    }
    
    /**
     * 设置节点点击监听器
     */
    fun setOnNodeClickListener(listener: (MindMapNode) -> Unit) {
        onNodeClickListener = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MindMapFlexboxViewHolder {
        val binding = ItemMindmapFlexboxBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return MindMapFlexboxViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: MindMapFlexboxViewHolder, position: Int) {
        holder.bind(currentPath, currentChildren, currentNode, topic)
    }
    
    override fun getItemCount(): Int = 1
    
    /**
     * MindMap Flexbox ViewHolder
     */
    inner class MindMapFlexboxViewHolder(private val binding: ItemMindmapFlexboxBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(path: List<MindMapNode>, children: List<MindMapNode>, currentNode: MindMapNode?, topic: TopicEntity?) {
            // 清空之前的视图
            binding.breadcrumbContainer.removeAllViews()
            binding.childrenContainer.removeAllViews()

            // 设置面包屑导航（使用TopicTreeNode的分类路径）
            setupBreadcrumb(path, topic)

            // 设置当前节点信息
            setupCurrentNodeInfo(currentNode)
            
            // 设置子节点
            setupChildren(children)
        }
        
        /**
         * 设置面包屑导航
         * 使用TopicTreeNode的分类路径而非MindMapNode的父节点路径
         */
        private fun setupBreadcrumb(path: List<MindMapNode>, topic: TopicEntity?) {
            // 如果有主题信息，显示TopicTreeNode的分类路径
            if (topic != null) {
                val categoryId = topic.topicCategoryId
                val categoryPath = PresetTopicCategories.buildCategoryPath(categoryId)

                categoryPath.forEachIndexed { index, categoryNode ->
                    val breadcrumbView = LayoutInflater.from(binding.root.context)
                        .inflate(R.layout.item_breadcrumb, binding.breadcrumbContainer, false)

                    val textView = breadcrumbView.findViewById<TextView>(R.id.breadcrumb_text)

                    textView.text = categoryNode.name

                    // 设置颜色：当前节点为深色，父节点为浅色
                    if (index == categoryPath.size - 1) {
                        // 当前节点
                        textView.setTextColor(binding.root.context.getColor(android.R.color.white))
                        textView.setBackgroundResource(R.drawable.bg_breadcrumb_current)
                    } else {
                        // 父节点
                        textView.setTextColor(binding.root.context.getColor(R.color.colorPrimaryDark))
                        textView.setBackgroundResource(R.drawable.bg_breadcrumb_parent)
                    }

                    // 分类路径节点不可点击（只显示导航，不可交互）
                    binding.breadcrumbContainer.addView(breadcrumbView)

                    // 如果不是最后一个节点，添加分隔符
                    if (index < categoryPath.size - 1) {
                        val separatorView = LayoutInflater.from(binding.root.context)
                            .inflate(R.layout.item_breadcrumb_separator, binding.breadcrumbContainer, false)
                        binding.breadcrumbContainer.addView(separatorView)
                    }
                }
            }
        }

        /**
         * 设置当前节点信息
         */
        private fun setupCurrentNodeInfo(node: MindMapNode?) {
            node?.let { currentNode ->
                binding.currentNodeTitle.text = currentNode.title
                binding.currentNodeProgress.text = "进度: ${currentNode.progress}%"
                binding.currentNodeProgressBar.progress = currentNode.progress
                
                // 根据进度设置颜色
                val progressColor = when {
                    currentNode.progress == 0 -> R.color.red
                    currentNode.progress < 100 -> R.color.yellow
                    else -> R.color.green
                }
                
                binding.currentNodeProgressBar.progressTintList = 
                    binding.root.context.getColorStateList(progressColor)
            }
        }
        
        /**
         * 设置子节点
         */
        private fun setupChildren(children: List<MindMapNode>) {
            if (children.isEmpty()) {
                binding.childrenLabel.visibility = View.GONE
                return
            }
            
            binding.childrenLabel.visibility = View.VISIBLE
            
            children.forEach { childNode ->
                val childView = LayoutInflater.from(binding.root.context)
                    .inflate(R.layout.item_mindmap_child_flex, binding.childrenContainer, false)
                
                val titleView = childView.findViewById<TextView>(R.id.child_title)
                val progressView = childView.findViewById<TextView>(R.id.child_progress)
                val progressBar = childView.findViewById<android.widget.ProgressBar>(R.id.child_progress_bar)
                
                titleView.text = childNode.title
                progressView.text = "${childNode.progress}%"
                progressBar.progress = childNode.progress
                
                // 根据进度设置颜色
                val progressColor = when {
                    childNode.progress == 0 -> R.color.red
                    childNode.progress < 100 -> R.color.yellow
                    else -> R.color.green
                }
                
                progressBar.progressTintList = binding.root.context.getColorStateList(progressColor)
                
                // 设置点击事件
                childView.setOnClickListener {
                    onNodeClickListener?.invoke(childNode)
                }
                
                binding.childrenContainer.addView(childView)
            }
        }
    }
}