package com.autodroid.guardiansdk.ui.guardians.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.guardians.model.GuardianItem

class GuardianAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_GUARDIAN_CARD = 0
        const val TYPE_GUARDIAN_LIST = 1
        const val TYPE_ADD_BUTTON = 2
        const val TYPE_EMPTY_STATE = 3
    }

    private val items = mutableListOf<GuardianItem>()

    fun updateData(newItems: List<GuardianItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GuardianItem.GuardianCard -> TYPE_GUARDIAN_CARD
            is GuardianItem.GuardianListItem -> TYPE_GUARDIAN_LIST
            is GuardianItem.AddGuardianButton -> TYPE_ADD_BUTTON
            is GuardianItem.EmptyState -> TYPE_EMPTY_STATE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GUARDIAN_CARD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_guardian_large_card, parent, false)
                GuardianCardViewHolder(view)
            }
            TYPE_GUARDIAN_LIST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_guardian_list, parent, false)
                GuardianListViewHolder(view)
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
            is GuardianCardViewHolder -> {
                val item = items[position] as GuardianItem.GuardianCard
                holder.bind(item)
            }
            is GuardianListViewHolder -> {
                val item = items[position] as GuardianItem.GuardianListItem
                holder.bind(item)
            }
            is AddButtonViewHolder -> {
                val item = items[position] as GuardianItem.AddGuardianButton
                holder.bind(item)
            }
            is EmptyStateViewHolder -> {
                val item = items[position] as GuardianItem.EmptyState
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class GuardianCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: GuardianItem.GuardianCard) {
            nameText.text = item.name
            lastContactText.text = "最后联系: ${item.lastContactTime}"
            locationText.text = item.lastLocation
            alarmMessageText.text = item.lastAlarmMessage
            avatarImage.setImageResource(R.drawable.guardian_ic_people)
        }
    }

    class GuardianListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: GuardianItem.GuardianListItem) {
            nameText.text = item.name
            lastContactText.text = item.lastContactTime
            locationText.text = item.lastLocation
            alarmMessageText.text = item.lastAlarmMessage
            avatarImage.setImageResource(R.drawable.guardian_ic_people)
        }
    }

    class AddButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: GuardianItem.AddGuardianButton) {
            itemView.setOnClickListener {
                
            }
        }
    }

    class EmptyStateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_empty_message)

        fun bind(item: GuardianItem.EmptyState) {
            messageText.text = item.message
        }
    }
}
