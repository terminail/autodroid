package com.autodroid.guardiansdk.ui.contacts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.contacts.model.ContactItem

class ContactAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_GUARDIAN_CARD = 0
        const val TYPE_WARD_CARD = 1
        const val TYPE_GUARDIAN_LIST = 2
        const val TYPE_WARD_LIST = 3
        const val TYPE_ADD_BUTTON = 4
        const val TYPE_EMPTY_STATE = 5
    }

    interface OnItemClickListener {
        fun onWardClick(phoneNumber: String, name: String)
    }

    private var listener: OnItemClickListener? = null
    private val items = mutableListOf<ContactItem>()

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun updateData(newItems: List<ContactItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ContactItem.ContactCard -> {
                if (item.contactType == "监护人") TYPE_GUARDIAN_CARD else TYPE_WARD_CARD
            }
            is ContactItem.ContactListItem -> {
                if (item.contactType == "监护人") TYPE_GUARDIAN_LIST else TYPE_WARD_LIST
            }
            is ContactItem.AddContactButton -> TYPE_ADD_BUTTON
            is ContactItem.EmptyState -> TYPE_EMPTY_STATE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GUARDIAN_CARD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_guardian_large_card, parent, false)
                GuardianCardViewHolder(view)
            }
            TYPE_WARD_CARD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_ward_large_card, parent, false)
                WardCardViewHolder(view)
            }
            TYPE_GUARDIAN_LIST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_guardian_list, parent, false)
                GuardianListViewHolder(view)
            }
            TYPE_WARD_LIST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.guardian_item_ward_list, parent, false)
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
            is GuardianCardViewHolder -> {
                val item = items[position] as ContactItem.ContactCard
                android.util.Log.d("ContactAdapter", "=== Binding guardian card position $position, item: ${item.name} ===")
                holder.bind(item, listener)
            }
            is WardCardViewHolder -> {
                val item = items[position] as ContactItem.ContactCard
                android.util.Log.d("ContactAdapter", "=== Binding ward card position $position, item: ${item.name} ===")
                holder.bind(item, listener)
            }
            is GuardianListViewHolder -> {
                val item = items[position] as ContactItem.ContactListItem
                holder.bind(item, listener)
            }
            is WardListViewHolder -> {
                val item = items[position] as ContactItem.ContactListItem
                holder.bind(item, listener)
            }
            is AddButtonViewHolder -> {
                val item = items[position] as ContactItem.AddContactButton
                holder.bind(item)
            }
            is EmptyStateViewHolder -> {
                val item = items[position] as ContactItem.EmptyState
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class GuardianCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val contactTypeText: TextView = itemView.findViewById(R.id.tv_contact_type)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: ContactItem.ContactCard, listener: OnItemClickListener?) {
            nameText.text = item.name
            contactTypeText.text = item.contactType
            lastContactText.text = "最后联系: ${item.lastContactTime}"
            locationText.text = item.lastLocation
            alarmMessageText.text = item.lastAlarmMessage
            avatarImage.setImageResource(R.drawable.guardian_ic_people)
            
            android.util.Log.d("ContactAdapter", "=== Setting click listener on itemView for guardian: ${item.name} ===")
            
            itemView.setOnClickListener {
                android.util.Log.d("ContactAdapter", "=== Guardian card clicked: ${item.name}, phone: ${item.phoneNumber} ===")
                listener?.onWardClick(item.phoneNumber, item.name)
            }
        }
    }

    class WardCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_ward_name)
        private val contactTypeText: TextView = itemView.findViewById(R.id.tv_contact_type)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: ContactItem.ContactCard, listener: OnItemClickListener?) {
            nameText.text = item.name
            contactTypeText.text = item.contactType
            lastContactText.text = "最后联系: ${item.lastContactTime}"
            locationText.text = item.lastLocation
            alarmMessageText.text = item.lastAlarmMessage
            avatarImage.setImageResource(R.drawable.guardian_ic_people)
            
            android.util.Log.d("ContactAdapter", "=== Setting click listener on itemView for ward: ${item.name} ===")
            
            itemView.setOnClickListener {
                android.util.Log.d("ContactAdapter", "=== Ward card clicked: ${item.name}, phone: ${item.phoneNumber} ===")
                listener?.onWardClick(item.phoneNumber, item.name)
            }
        }
    }

    class GuardianListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: ContactItem.ContactListItem, listener: OnItemClickListener?) {
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

    class WardListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_ward_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(item: ContactItem.ContactListItem, listener: OnItemClickListener?) {
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
        fun bind(item: ContactItem.AddContactButton) {
            itemView.setOnClickListener {
                
            }
        }
    }

    class EmptyStateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_empty_message)

        fun bind(item: ContactItem.EmptyState) {
            messageText.text = item.message
        }
    }
}
