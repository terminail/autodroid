package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R

/**
 * TopicsFragment适配器
 * 专门处理话题列表界面的异构数据显示
 */
class TopicsAdapter(
    private val onTopicClick: ((TopicsItem.TopicItem) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var items: List<TopicsItem> = emptyList()
    
    fun submitList(newItems: List<TopicsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return items[position].getType()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            TopicsItem.TYPE_TOPIC -> {
                val view = inflater.inflate(R.layout.item_topic, parent, false)
                TopicViewHolder(view)
            }
            TopicsItem.TYPE_SECTION_HEADER -> {
                val view = inflater.inflate(R.layout.item_setting_header, parent, false)
                SectionHeaderViewHolder(view)
            }
            TopicsItem.TYPE_ADD_BUTTON -> {
                val view = inflater.inflate(R.layout.item_topic_add_button, parent, false)
                AddButtonViewHolder(view)
            }
            TopicsItem.TYPE_EMPTY_STATE -> {
                val view = inflater.inflate(R.layout.item_topic_empty_state, parent, false)
                EmptyStateViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        
        when (holder) {
            is TopicViewHolder -> holder.bind(item as TopicsItem.TopicItem)
            is SectionHeaderViewHolder -> holder.bind(item as TopicsItem.SectionHeaderItem)
            is AddButtonViewHolder -> holder.bind(item as TopicsItem.AddButtonItem)
            is EmptyStateViewHolder -> holder.bind(item as TopicsItem.EmptyStateItem)
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    /**
     * 话题ViewHolder
     */
    inner class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description_text)
        private val progressTextView: TextView = itemView.findViewById(R.id.progress_text)
        private val dateTextView: TextView = itemView.findViewById(R.id.date_text)
        
        fun bind(item: TopicsItem.TopicItem) {
            titleTextView.text = item.topic.title
            descriptionTextView.text = item.topic.description
            progressTextView.text = "掌握度: ${item.topic.masteryLevel}%"
            
            val lastAccessed = item.topic.lastAccessed
            val date = if (lastAccessed > 0) {
                val daysAgo = ((System.currentTimeMillis() - lastAccessed) / (1000 * 60 * 60 * 24)).toInt()
                if (daysAgo == 0) "今天" else "$daysAgo 天前"
            } else {
                ""
            }
            dateTextView.text = date
            
            // 设置点击监听器
            itemView.setOnClickListener {
                onTopicClick?.invoke(item)
            }
        }
    }
    
    /**
     * 分类标题ViewHolder
     */
    class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.header_text)
        
        fun bind(item: TopicsItem.SectionHeaderItem) {
            textView.text = "=== ${item.title} ==="
            if (item.subtitle != null) {
                textView.text = "=== ${item.title} ===\n${item.subtitle}"
            }
        }
    }
    
    /**
     * 添加按钮ViewHolder
     */
    class AddButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.add_button_text)
        
        fun bind(item: TopicsItem.AddButtonItem) {
            textView.text = "[+] ${item.label}"
        }
    }
    
    /**
     * 空状态ViewHolder
     */
    class EmptyStateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.empty_state_text)
        
        fun bind(item: TopicsItem.EmptyStateItem) {
            textView.text = item.message
        }
    }
}