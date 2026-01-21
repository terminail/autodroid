package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.utils.AIServiceProvider

sealed class SettingsItem {
    data class Header(val title: String) : SettingsItem()
    data class AIService(val service: AIServiceProvider) : SettingsItem()
    data class OtherSetting(val title: String, val description: String, val type: String) : SettingsItem()
    object Divider : SettingsItem()
}

class SettingsAdapter(private val onClick: (SettingsItem) -> Unit) : 
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_AI_SERVICE = 1
        private const val TYPE_OTHER_SETTING = 2
        private const val TYPE_DIVIDER = 3
    }
    
    private var items: List<SettingsItem> = emptyList()
    
    fun submitList(newItems: List<SettingsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is SettingsItem.Header -> TYPE_HEADER
            is SettingsItem.AIService -> TYPE_AI_SERVICE
            is SettingsItem.OtherSetting -> TYPE_OTHER_SETTING
            SettingsItem.Divider -> TYPE_DIVIDER
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_setting_header, parent, false)
            )
            TYPE_AI_SERVICE -> {
                AIServiceViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.setting_item_openai, parent, false)
                )
            }
            TYPE_OTHER_SETTING -> OtherSettingViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.setting_item_about, parent, false)
            )
            TYPE_DIVIDER -> DividerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_setting_divider, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as SettingsItem.Header)
            is AIServiceViewHolder -> holder.bind(item as SettingsItem.AIService, onClick)
            is OtherSettingViewHolder -> holder.bind(item as SettingsItem.OtherSetting, onClick)
            is DividerViewHolder -> {}
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView as TextView
        
        fun bind(header: SettingsItem.Header) {
            titleView.text = header.title
        }
    }
    
    private class AIServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.setting_title)
        private val descriptionView: TextView = itemView.findViewById(R.id.setting_description)
        private val statusView: TextView = itemView.findViewById(R.id.setting_status)
        
        fun bind(service: SettingsItem.AIService, onClick: (SettingsItem) -> Unit) {
            titleView.text = service.service.name
            descriptionView.text = service.service.description
            statusView.text = if (service.service.isEnabled) "已配置" else "未配置"
            statusView.setTextColor(if (service.service.isEnabled) 0xFF07C160.toInt() else 0xFFFF4444.toInt())
            
            itemView.setOnClickListener {
                onClick(service)
            }
        }
    }
    
    private class OtherSettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.setting_title)
        private val descriptionView: TextView = itemView.findViewById(R.id.setting_description)
        
        fun bind(setting: SettingsItem.OtherSetting, onClick: (SettingsItem) -> Unit) {
            titleView.text = setting.title
            descriptionView.text = setting.description
            
            itemView.setOnClickListener {
                onClick(setting)
            }
        }
    }
    
    private class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}