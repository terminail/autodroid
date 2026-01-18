package com.autodroid.sms.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.sms.R
import com.autodroid.guardiansdk.sms.model.Conversation
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 会话列表适配器
 */
class ConversationAdapter(
    private val onItemClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = getItem(position)
        holder.bind(conversation)
        holder.itemView.setOnClickListener {
            onItemClick(conversation)
        }
    }
    
    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContactName: TextView = itemView.findViewById(R.id.tv_contact_name)
        private val tvSnippet: TextView = itemView.findViewById(R.id.tv_snippet)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvUnreadCount: TextView = itemView.findViewById(R.id.tv_unread_count)
        
        fun bind(conversation: Conversation) {
            // 显示联系人名称或电话号码
            tvContactName.text = conversation.contactName ?: conversation.address
            
            // 显示消息摘要
            tvSnippet.text = conversation.snippet
            
            // 显示时间
            val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            tvTime.text = dateFormat.format(conversation.date)
            
            // 显示未读消息数
            if (conversation.unreadCount > 0) {
                tvUnreadCount.visibility = View.VISIBLE
                tvUnreadCount.text = conversation.unreadCount.toString()
            } else {
                tvUnreadCount.visibility = View.GONE
            }
            
            // 根据会话状态设置样式
            if (conversation.unreadCount > 0) {
                tvContactName.setTextColor(itemView.context.getColor(android.R.color.black))
                tvSnippet.setTextColor(itemView.context.getColor(android.R.color.black))
            } else {
                tvContactName.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                tvSnippet.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            }
        }
    }
    
    private class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.threadId == newItem.threadId
        }
        
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}