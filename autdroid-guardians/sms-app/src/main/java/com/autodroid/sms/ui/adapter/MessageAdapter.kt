package com.autodroid.sms.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.sms.R
import com.autodroid.sms.data.model.SmsMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : ListAdapter<SmsMessage, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvSender: TextView = itemView.findViewById(R.id.tv_sender)
        
        fun bind(message: SmsMessage) {
            tvMessage.text = message.body
            tvTime.text = formatTime(message.date)
            
            when (message.type) {
                1, 2 -> {
                    tvSender.text = "对方"
                    tvSender.visibility = View.VISIBLE
                }
                4, 5, 6 -> {
                    tvSender.text = "我"
                    tvSender.visibility = View.VISIBLE
                }
                else -> {
                    tvSender.visibility = View.GONE
                }
            }
        }
        
        private fun formatTime(date: Date): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(date)
        }
    }
    
    class MessageDiffCallback : DiffUtil.ItemCallback<SmsMessage>() {
        override fun areItemsTheSame(oldItem: SmsMessage, newItem: SmsMessage): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: SmsMessage, newItem: SmsMessage): Boolean {
            return oldItem == newItem
        }
    }
}