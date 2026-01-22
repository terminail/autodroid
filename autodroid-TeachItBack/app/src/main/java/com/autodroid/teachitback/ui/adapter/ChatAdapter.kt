package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.ItemChatAiMessageBinding
import com.autodroid.teachitback.databinding.ItemChatMindmapBinding
import com.autodroid.teachitback.databinding.ItemChatMindmapNodeBinding
import com.autodroid.teachitback.databinding.ItemChatUserMessageBinding
import com.autodroid.teachitback.model.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ChatFragment适配器
 * 专门处理ChatFragment的异构数据显示，包括消息、AI响应、MindMap、文件等
 */
class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var items: List<ChatItem> = emptyList()
    
    fun submitList(newItems: List<ChatItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return items[position].getType()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            ChatItem.TYPE_USER_MESSAGE -> {
                val binding = ItemChatUserMessageBinding.inflate(inflater, parent, false)
                UserMessageViewHolder(binding)
            }
            ChatItem.TYPE_AI_MESSAGE -> {
                val binding = ItemChatAiMessageBinding.inflate(inflater, parent, false)
                AIMessageViewHolder(binding)
            }
            ChatItem.TYPE_MINDMAP -> {
                val binding = ItemChatMindmapBinding.inflate(inflater, parent, false)
                MindMapViewHolder(binding)
            }
            ChatItem.TYPE_FILE -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                FileViewHolder(view)
            }
            ChatItem.TYPE_SYSTEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                SystemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        
        when (holder) {
            is UserMessageViewHolder -> holder.bind(item as ChatItem.UserMessageItem)
            is AIMessageViewHolder -> holder.bind(item as ChatItem.AIMessageItem)
            is MindMapViewHolder -> holder.bind(item as ChatItem.MindMapDisplayItem)
            is FileViewHolder -> holder.bind(item as ChatItem.FileItem)
            is SystemViewHolder -> holder.bind(item as ChatItem.SystemItem)
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    /**
     * 用户消息ViewHolder
     */
    class UserMessageViewHolder(private val binding: ItemChatUserMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ChatItem.UserMessageItem) {
            binding.messageContent.text = item.message.content
            binding.messageTime.text = formatTimestamp(item.message.timestamp)
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
    
    /**
     * AI消息ViewHolder
     */
    class AIMessageViewHolder(private val binding: ItemChatAiMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ChatItem.AIMessageItem) {
            binding.messageContent.text = item.message.content
            binding.messageTime.text = formatTimestamp(item.message.timestamp)
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
    
    /**
     * MindMap显示ViewHolder
     */
    class MindMapViewHolder(private val binding: ItemChatMindmapBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ChatItem.MindMapDisplayItem) {
            binding.mindmapTitle.text = item.title
            
            val nodes = item.mindMapNodes
            val totalNodes = nodes.size
            val redNodes = nodes.count { it.progress == 0 }
            val yellowNodes = nodes.count { it.progress > 0 && it.progress < 100 }
            val greenNodes = nodes.count { it.progress == 100 }
            val overallProgress = if (totalNodes > 0) {
                nodes.sumOf { it.progress } / totalNodes
            } else 0
            
            binding.progressStats.text = "总体进度: $overallProgress% | 红色($redNodes) 黄色($yellowNodes) 绿色($greenNodes)"
            binding.overallProgressBar.progress = overallProgress
            binding.nodeCount.text = "总节点数: $totalNodes"
            
            binding.expandCollapseButton.setOnClickListener {
                if (binding.mindmapTreeRecycler.visibility == View.GONE) {
                    binding.mindmapTreeRecycler.visibility = View.VISIBLE
                    binding.expandCollapseButton.text = "收起思维导图"
                } else {
                    binding.mindmapTreeRecycler.visibility = View.GONE
                    binding.expandCollapseButton.text = "展开思维导图"
                }
            }
        }
    }
    
    /**
     * 文件ViewHolder
     */
    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: ChatItem.FileItem) {
            var text = "[文件] ${item.fileName}"
            if (item.extractedText != null) {
                text += "\n${item.extractedText.take(50)}..."
            }
            textView.text = text
        }
    }
    
    /**
     * 系统消息ViewHolder
     */
    class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: ChatItem.SystemItem) {
            textView.text = "[系统] ${item.content}"
        }
    }
}
