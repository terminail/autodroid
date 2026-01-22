package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.ItemWhyPresetTopicsCardBinding

class WhyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var items: List<WhyItem> = emptyList()
    private var onCopyTopic: ((com.autodroid.teachitback.model.TopicEntity) -> Unit)? = null
    
    fun submitList(newItems: List<WhyItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    fun setOnCopyTopic(listener: (com.autodroid.teachitback.model.TopicEntity) -> Unit) {
        onCopyTopic = listener
    }
    
    override fun getItemViewType(position: Int): Int {
        return items[position].getType()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            WhyItem.TYPE_SECTION_HEADER -> {
                val view = inflater.inflate(R.layout.item_setting_header, parent, false)
                SectionHeaderViewHolder(view)
            }
            WhyItem.TYPE_TEXT_CARD -> {
                val view = inflater.inflate(R.layout.item_why_text_card, parent, false)
                TextCardViewHolder(view)
            }
            WhyItem.TYPE_PRESET_TOPIC -> {
                val binding = ItemWhyPresetTopicsCardBinding.inflate(inflater, parent, false)
                PresetTopicViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is WhyItem.SectionHeaderItem -> {
                (holder as SectionHeaderViewHolder).bind(item)
            }
            is WhyItem.TextCardItem -> {
                (holder as TextCardViewHolder).bind(item)
            }
            is WhyItem.PresetTopicItem -> {
                (holder as PresetTopicViewHolder).bind(item, onCopyTopic)
            }
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.header_text)
        
        fun bind(item: WhyItem.SectionHeaderItem) {
            titleTextView.text = item.title
        }
    }
    
    class TextCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text)
        private val contentTextView: TextView = itemView.findViewById(R.id.content_text)
        
        fun bind(item: WhyItem.TextCardItem) {
            titleTextView.text = item.title
            contentTextView.text = item.content
        }
    }
    
    class PresetTopicViewHolder(private val binding: ItemWhyPresetTopicsCardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: WhyItem.PresetTopicItem, onCopyTopic: ((com.autodroid.teachitback.model.TopicEntity) -> Unit)?) {
            binding.titleText.text = item.topic.title
            binding.descriptionText.text = item.topic.description
            
            val topicsText = buildString {
                append("• CFP财务规划: 系统化学习CFP考试内容\n")
                append("• 投资组合管理: 掌握资产配置策略\n")
                append("• 税务规划: 深入理解税务优化方法")
            }
            binding.topicsText.text = topicsText
            
            binding.root.setOnClickListener {
                onCopyTopic?.invoke(item.topic)
            }
        }
    }
}
