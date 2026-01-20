package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.databinding.ItemAiMessageBinding
import com.autodroid.teachitback.databinding.ItemUserMessageBinding
import com.autodroid.teachitback.model.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_AI = 2

    private var messages = listOf<MessageEntity>()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submitList(newMessages: List<MessageEntity>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderType == "USER") VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val binding = ItemUserMessageBinding.inflate(inflater, parent, false)
                UserMessageViewHolder(binding)
            }
            VIEW_TYPE_AI -> {
                val binding = ItemAiMessageBinding.inflate(inflater, parent, false)
                AIMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message, dateFormat)
            is AIMessageViewHolder -> holder.bind(message, dateFormat)
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserMessageViewHolder(private val binding: ItemUserMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageEntity, dateFormat: SimpleDateFormat) {
            binding.messageContent.text = message.content
            binding.messageTime.text = dateFormat.format(Date(message.timestamp))
        }
    }

    class AIMessageViewHolder(private val binding: ItemAiMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageEntity, dateFormat: SimpleDateFormat) {
            binding.messageContent.text = message.content
            binding.messageTime.text = dateFormat.format(Date(message.timestamp))
        }
    }
}
