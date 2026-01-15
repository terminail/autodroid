package com.autodroid.guardiansdk.ui.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.settings.model.*

/**
 * 设置项列表适配器 - 异构item设计
 */
class SettingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_EMERGENCY_CONTACTS = 0
        const val TYPE_ALARM_MODE = 1
        const val TYPE_PASSWORD_BOOK = 2
        const val TYPE_FLOATING_WINDOW = 3
        const val TYPE_TEST_MODE = 4
        const val TYPE_EMERGENCY_WIPE = 5
    }

    private val items = mutableListOf<SettingItem>()

    interface OnItemClickListener {
        fun onEmergencyContactsClick()
        fun onAlarmModeClick()
        fun onPasswordBookClick()
        fun onFloatingWindowClick()
        fun onTestModeClick()
        fun onEmergencyWipeClick()
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun updateItems(newItems: List<SettingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SettingItem.EmergencyContacts -> TYPE_EMERGENCY_CONTACTS
            is SettingItem.AlarmMode -> TYPE_ALARM_MODE
            is SettingItem.PasswordBook -> TYPE_PASSWORD_BOOK
            is SettingItem.FloatingWindow -> TYPE_FLOATING_WINDOW
            is SettingItem.TestMode -> TYPE_TEST_MODE
            is SettingItem.EmergencyWipe -> TYPE_EMERGENCY_WIPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_EMERGENCY_CONTACTS -> {
                val view = inflater.inflate(R.layout.item_emergency_contacts, parent, false)
                EmergencyContactsViewHolder(view)
            }
            TYPE_ALARM_MODE -> {
                val view = inflater.inflate(R.layout.item_alarm_mode, parent, false)
                AlarmModeViewHolder(view)
            }
            TYPE_PASSWORD_BOOK -> {
                val view = inflater.inflate(R.layout.item_password_book, parent, false)
                PasswordBookViewHolder(view)
            }
            TYPE_FLOATING_WINDOW -> {
                val view = inflater.inflate(R.layout.item_floating_window, parent, false)
                FloatingWindowViewHolder(view)
            }
            TYPE_TEST_MODE -> {
                val view = inflater.inflate(R.layout.item_test_mode, parent, false)
                TestModeViewHolder(view)
            }
            TYPE_EMERGENCY_WIPE -> {
                val view = inflater.inflate(R.layout.item_emergency_wipe, parent, false)
                EmergencyWipeViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is EmergencyContactsViewHolder -> holder.bind(item as SettingItem.EmergencyContacts)
            is AlarmModeViewHolder -> holder.bind(item as SettingItem.AlarmMode)
            is PasswordBookViewHolder -> holder.bind(item as SettingItem.PasswordBook)
            is FloatingWindowViewHolder -> holder.bind(item as SettingItem.FloatingWindow)
            is TestModeViewHolder -> holder.bind(item as SettingItem.TestMode)
            is EmergencyWipeViewHolder -> holder.bind(item as SettingItem.EmergencyWipe)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class EmergencyContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SettingItem.EmergencyContacts) {
            itemView.setOnClickListener {
                listener?.onEmergencyContactsClick()
            }
        }
    }

    inner class AlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SettingItem.AlarmMode) {
            itemView.setOnClickListener {
                listener?.onAlarmModeClick()
            }
        }
    }

    inner class PasswordBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SettingItem.PasswordBook) {
            itemView.setOnClickListener {
                listener?.onPasswordBookClick()
            }
        }
    }

    inner class FloatingWindowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SettingItem.FloatingWindow) {
            itemView.setOnClickListener {
                listener?.onFloatingWindowClick()
            }
        }
    }

    inner class TestModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SettingItem.TestMode) {
            itemView.setOnClickListener {
                listener?.onTestModeClick()
            }
        }
    }

    inner class EmergencyWipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SettingItem.EmergencyWipe) {
            itemView.setOnClickListener {
                listener?.onEmergencyWipeClick()
            }
        }
    }
}
