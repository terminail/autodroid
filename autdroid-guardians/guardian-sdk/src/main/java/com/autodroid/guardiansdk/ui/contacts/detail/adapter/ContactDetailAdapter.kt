package com.autodroid.guardiansdk.ui.contacts.detail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.data.entity.MessageContent
import com.autodroid.guardiansdk.data.entity.MessageContentSerializer
import com.autodroid.guardiansdk.ui.contacts.detail.model.ContactDetailItem
import java.text.SimpleDateFormat
import java.util.*

class ContactDetailAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SEND_MESSAGE = 1
        private const val TYPE_RECEIVE_MESSAGE = 2
        private const val TYPE_TIME_DIVIDER = 3

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    private val items = mutableListOf<ContactDetailItem>()
    private var currentUserPhoneNumber: String = "" // 当前用户手机号，用于判断消息方向

    fun setCurrentUserPhoneNumber(phoneNumber: String) {
        currentUserPhoneNumber = phoneNumber
    }

    fun updateData(newItems: List<ContactDetailItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item) {
            is ContactDetailItem.Header -> TYPE_HEADER
            is ContactDetailItem.MessageItem -> {
                // 判断消息方向
                if (item.message.fromPhoneNumber == currentUserPhoneNumber) {
                    TYPE_SEND_MESSAGE
                } else {
                    TYPE_RECEIVE_MESSAGE
                }
            }
            is ContactDetailItem.TimeDivider -> TYPE_TIME_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_contact_detail_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_SEND_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_contact_detail_message_send, parent, false)
                SendMessageViewHolder(view)
            }
            TYPE_RECEIVE_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_contact_detail_message_receive, parent, false)
                ReceiveMessageViewHolder(view)
            }
            TYPE_TIME_DIVIDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_contact_detail_time_divider, parent, false)
                TimeDividerViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val item = items[position] as ContactDetailItem.Header
                holder.bind(item)
            }
            is SendMessageViewHolder -> {
                val item = items[position] as ContactDetailItem.MessageItem
                holder.bind(item.message)
            }
            is ReceiveMessageViewHolder -> {
                val item = items[position] as ContactDetailItem.MessageItem
                holder.bind(item.message)
            }
            is TimeDividerViewHolder -> {
                val item = items[position] as ContactDetailItem.TimeDivider
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_ward_name)
        private val tvPhone: TextView = itemView.findViewById(R.id.tv_ward_phone)
        private val tvRelationship: TextView = itemView.findViewById(R.id.tv_ward_relationship)
        private val tvAlarmCount: TextView = itemView.findViewById(R.id.tv_alarm_count)
        private val tvLastAlarmTime: TextView = itemView.findViewById(R.id.tv_last_alarm_time)

        fun bind(item: ContactDetailItem.Header) {
            tvName.text = item.name
            tvPhone.text = "手机号: ${item.phoneNumber}"
            tvRelationship.text = "关系: ${item.relationship}"
            tvAlarmCount.text = "报警次数: ${item.alarmCount}"
            tvLastAlarmTime.text = "最后报警: ${dateFormat.format(Date(item.lastAlarmTime))}"
        }
    }

    class SendMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_message_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_message_time)

        fun bind(message: com.autodroid.guardiansdk.data.entity.Message) {
            // 反序列化content
            val content = MessageContentSerializer.deserialize(message.content)
            tvContent.text = formatMessageContent(content)
            tvTime.text = dateFormat.format(Date(message.timestamp))
        }

        private fun formatMessageContent(content: MessageContent): String {
            return when (content) {
                is MessageContent.TextMessage -> content.text
                is MessageContent.AlarmMessage -> "[报警] ${content.message} - 位置: ${content.location}"
                is MessageContent.QueryMessage -> "[查询] ${content.query}"
                is MessageContent.PingMessage -> "[心跳] ${content.status}"
            }
        }
    }

    class ReceiveMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_message_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_message_time)

        fun bind(message: com.autodroid.guardiansdk.data.entity.Message) {
            // 反序列化content
            val content = MessageContentSerializer.deserialize(message.content)
            tvContent.text = formatMessageContent(content)
            tvTime.text = dateFormat.format(Date(message.timestamp))
        }

        private fun formatMessageContent(content: MessageContent): String {
            return when (content) {
                is MessageContent.TextMessage -> content.text
                is MessageContent.AlarmMessage -> "[报警] ${content.message} - 位置: ${content.location}"
                is MessageContent.QueryMessage -> "[查询] ${content.query}"
                is MessageContent.PingMessage -> "[心跳] ${content.status}"
            }
        }
    }

    class TimeDividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tv_divider_time)

        fun bind(item: ContactDetailItem.TimeDivider) {
            tvTime.text = formatDate(item.timestamp)
        }

        private fun formatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val messageDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            
            return when (messageDate) {
                today -> "今天"
                else -> SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(date)
            }
        }
    }
}