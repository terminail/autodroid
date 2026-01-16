package com.autodroid.guardiansdk.ui.wards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.wards.model.WardItem

class WardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_GUARDIAN_CARD = 0
        const val TYPE_GUARDIAN_LIST = 1
        const val TYPE_ADD_BUTTON = 2
        const val TYPE_EMPTY_STATE = 3
    }

    interface OnItemClickListener {
        fun onWardClick(phoneNumber: String, name: String)
    }

    private var listener: OnItemClickListener? = null
    private val items = mutableListOf<WardItem>()

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun updateData(newItems: List<WardItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is WardItem.WardCard -> TYPE_GUARDIAN_CARD
            is WardItem.WardListItem -> TYPE_GUARDIAN_LIST
            is WardItem.AddWardButton -> TYPE_ADD_BUTTON
            is WardItem.EmptyState -> TYPE_EMPTY_STATE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GUARDIAN_CARD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_guardian_large_card, parent, false)
                WardCardViewHolder(view)
            }
            TYPE_GUARDIAN_LIST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_guardian_list, parent, false)
                WardListViewHolder(view)
            }
            TYPE_ADD_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_add_guardian_button, parent, false)
                AddButtonViewHolder(view)
            }
            TYPE_EMPTY_STATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_empty_state, parent, false)
                EmptyStateViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WardCardViewHolder -> {
                val item = items[position] as WardItem.WardCard
                android.util.Log.d("WardAdapter", "=== Binding position $position, item: ${item.name} ===")
                holder.bind(item, listener)
            }
            is WardListViewHolder -> {
                val item = items[position] as WardItem.WardListItem
                holder.bind(item, listener)
            }
            is AddButtonViewHolder -> {
                val item = items[position] as WardItem.AddWardButton
                holder.bind(item)
            }
            is EmptyStateViewHolder -> {
                val item = items[position] as WardItem.EmptyState
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class WardCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: WardItem.WardCard, listener: OnItemClickListener?) {
            nameText.text = item.name
            lastContactText.text = "最后联系: ${item.lastContactTime}"
            locationText.text = item.lastLocation
            alarmMessageText.text = item.lastAlarmMessage
            avatarImage.setImageResource(R.drawable.guardian_ic_people)
            
            android.util.Log.d("WardAdapter", "=== Setting click listener on itemView for: ${item.name} ===")
            android.util.Log.d("WardAdapter", "=== itemView isClickable: ${itemView.isClickable} ===")
            
            itemView.setOnClickListener {
                android.util.Log.d("WardAdapter", "=== Item clicked: ${item.name}, phone: ${item.phoneNumber} ===")
                listener?.onWardClick(item.phoneNumber, item.name)
            }
        }
    }

    class WardListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: WardItem.WardListItem, listener: OnItemClickListener?) {
            nameText.text = item.name
            lastContactText.text = item.lastContactTime
            locationText.text = item.lastLocation
            alarmMessageText.text = item.lastAlarmMessage
            avatarImage.setImageResource(R.drawable.guardian_ic_people)
            
            itemView.setOnClickListener {
                listener?.onWardClick(item.phoneNumber, item.name)
            }
        }
    }

    class AddButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: WardItem.AddWardButton) {
            itemView.setOnClickListener {
                
            }
        }
    }

    class EmptyStateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_empty_message)

        fun bind(item: WardItem.EmptyState) {
            messageText.text = item.message
        }
    }
}
