package com.autodroid.guardiansdk.ui.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.settings.model.*

/**
 * 设置项列表适配器 - 异构item设计
 */
class SettingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_MY_GUARDIAN = 0
        const val TYPE_VOLUME_KEY_ALARM_MODE = 1
        const val TYPE_FLOATING_WINDOW_ALARM_MODE = 2
        const val TYPE_SHAKE_PHONE_ALARM_MODE = 3
        const val TYPE_PASSWORD_BOOK = 4
        const val TYPE_FLOATING_WINDOW = 5
        const val TYPE_WIPE_ALARM_INFO = 6
        const val TYPE_DOOR_PASSPHRASE = 7
        const val TYPE_TEST_MODE = 8
        const val TYPE_ALARM_HISTORY = 9
        const val TYPE_GUARDIAN_QUERY_HISTORY = 10
    }

    private val items = mutableListOf<SettingItem>()

    interface OnItemClickListener {
        fun onMyGuardianClick(index: Int)
        fun onVolumeKeyAlarmModeClick()
        fun onFloatingWindowAlarmModeClick()
        fun onShakePhoneAlarmModeClick()
        fun onPasswordBookClick()
        fun onFloatingWindowClick()
        fun onWipeAlarmInfoClick()
        fun onDoorPassphraseClick()
        fun onTestModeClick()
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
            is SettingItem.MyGuardian -> TYPE_MY_GUARDIAN
            is SettingItem.VolumeKeyAlarmMode -> TYPE_VOLUME_KEY_ALARM_MODE
            is SettingItem.FloatingWindowAlarmMode -> TYPE_FLOATING_WINDOW_ALARM_MODE
            is SettingItem.ShakePhoneAlarmMode -> TYPE_SHAKE_PHONE_ALARM_MODE
            is SettingItem.PasswordBook -> TYPE_PASSWORD_BOOK
            is SettingItem.FloatingWindow -> TYPE_FLOATING_WINDOW
            is SettingItem.WipeAlarmInfo -> TYPE_WIPE_ALARM_INFO
            is SettingItem.DoorPassphrase -> TYPE_DOOR_PASSPHRASE
            is SettingItem.TestMode -> TYPE_TEST_MODE
            is SettingItem.AlarmHistory -> TYPE_ALARM_HISTORY
            is SettingItem.GuardianQueryHistory -> TYPE_GUARDIAN_QUERY_HISTORY
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MY_GUARDIAN -> {
                val view = inflater.inflate(R.layout.item_my_guardian, parent, false)
                MyGuardianViewHolder(view)
            }
            TYPE_VOLUME_KEY_ALARM_MODE -> {
                val view = inflater.inflate(R.layout.item_volume_key_alarm_mode, parent, false)
                VolumeKeyAlarmModeViewHolder(view)
            }
            TYPE_FLOATING_WINDOW_ALARM_MODE -> {
                val view = inflater.inflate(R.layout.item_floating_window_alarm_mode, parent, false)
                FloatingWindowAlarmModeViewHolder(view)
            }
            TYPE_SHAKE_PHONE_ALARM_MODE -> {
                val view = inflater.inflate(R.layout.item_shake_phone_alarm_mode, parent, false)
                ShakePhoneAlarmModeViewHolder(view)
            }
            TYPE_PASSWORD_BOOK -> {
                val view = inflater.inflate(R.layout.item_password_book, parent, false)
                PasswordBookViewHolder(view)
            }
            TYPE_FLOATING_WINDOW -> {
                val view = inflater.inflate(R.layout.item_floating_window, parent, false)
                FloatingWindowViewHolder(view)
            }
            TYPE_WIPE_ALARM_INFO -> {
                val view = inflater.inflate(R.layout.item_wipe_alarm_info, parent, false)
                WipeAlarmInfoViewHolder(view)
            }
            TYPE_DOOR_PASSPHRASE -> {
                val view = inflater.inflate(R.layout.item_door_passphrase, parent, false)
                DoorPassphraseViewHolder(view)
            }
            TYPE_TEST_MODE -> {
                val view = inflater.inflate(R.layout.item_test_mode, parent, false)
                TestModeViewHolder(view)
            }
            TYPE_ALARM_HISTORY -> {
                val view = inflater.inflate(R.layout.item_alarm_history, parent, false)
                AlarmHistoryViewHolder(view)
            }
            TYPE_GUARDIAN_QUERY_HISTORY -> {
                val view = inflater.inflate(R.layout.item_guardian_query_history, parent, false)
                GuardianQueryHistoryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is MyGuardianViewHolder -> holder.bind(item as SettingItem.MyGuardian, listener)
            is VolumeKeyAlarmModeViewHolder -> holder.bind(item as SettingItem.VolumeKeyAlarmMode, listener)
            is FloatingWindowAlarmModeViewHolder -> holder.bind(item as SettingItem.FloatingWindowAlarmMode, listener)
            is ShakePhoneAlarmModeViewHolder -> holder.bind(item as SettingItem.ShakePhoneAlarmMode, listener)
            is PasswordBookViewHolder -> holder.bind(item as SettingItem.PasswordBook, listener)
            is FloatingWindowViewHolder -> holder.bind(item as SettingItem.FloatingWindow, listener)
            is WipeAlarmInfoViewHolder -> holder.bind(item as SettingItem.WipeAlarmInfo, listener)
            is DoorPassphraseViewHolder -> holder.bind(item as SettingItem.DoorPassphrase, listener)
            is TestModeViewHolder -> holder.bind(item as SettingItem.TestMode, listener)
            is AlarmHistoryViewHolder -> holder.bind(item as SettingItem.AlarmHistory)
            is GuardianQueryHistoryViewHolder -> holder.bind(item as SettingItem.GuardianQueryHistory)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class MyGuardianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.MyGuardian, listener: OnItemClickListener?) {
            textView.text = "我的监护人${item.index}"
            itemView.setOnClickListener {
                listener?.onMyGuardianClick(item.index)
            }
        }
    }

    inner class VolumeKeyAlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.VolumeKeyAlarmMode, listener: OnItemClickListener?) {
            textView.text = "音量键报警模式"
            itemView.setOnClickListener {
                listener?.onVolumeKeyAlarmModeClick()
            }
        }
    }

    inner class FloatingWindowAlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.FloatingWindowAlarmMode, listener: OnItemClickListener?) {
            textView.text = "浮动窗口报警模式"
            itemView.setOnClickListener {
                listener?.onFloatingWindowAlarmModeClick()
            }
        }
    }

    inner class ShakePhoneAlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.ShakePhoneAlarmMode, listener: OnItemClickListener?) {
            textView.text = "摇动手机报警模式"
            itemView.setOnClickListener {
                listener?.onShakePhoneAlarmModeClick()
            }
        }
    }

    inner class PasswordBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.PasswordBook, listener: OnItemClickListener?) {
            textView.text = "我的位置密码本"
            itemView.setOnClickListener {
                listener?.onPasswordBookClick()
            }
        }
    }

    inner class FloatingWindowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.FloatingWindow, listener: OnItemClickListener?) {
            textView.text = "浮动窗口"
            itemView.setOnClickListener {
                listener?.onFloatingWindowClick()
            }
        }
    }

    inner class WipeAlarmInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.WipeAlarmInfo, listener: OnItemClickListener?) {
            textView.text = "擦除报警信息"
            itemView.setOnClickListener {
                listener?.onWipeAlarmInfoClick()
            }
        }
    }

    inner class DoorPassphraseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.DoorPassphrase, listener: OnItemClickListener?) {
            textView.text = "开门密语"
            itemView.setOnClickListener {
                listener?.onDoorPassphraseClick()
            }
        }
    }

    inner class TestModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.TestMode, listener: OnItemClickListener?) {
            textView.text = "测试模式"
            itemView.setOnClickListener {
                listener?.onTestModeClick()
            }
        }
    }

    inner class AlarmHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.tv_time)
        private val descriptionText: TextView = itemView.findViewById(R.id.tv_description)

        fun bind(item: SettingItem.AlarmHistory) {
            timeText.text = item.time
            descriptionText.text = item.description
            // History items are not clickable
        }
    }

    inner class GuardianQueryHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.tv_time)
        private val queryText: TextView = itemView.findViewById(R.id.tv_query)
        private val responseText: TextView = itemView.findViewById(R.id.tv_response)

        fun bind(item: SettingItem.GuardianQueryHistory) {
            timeText.text = item.time
            queryText.text = "${item.guardianName}查询：${item.queryContent}"
            responseText.text = "回复信息：${item.responseContent}"
            // History items are not clickable
        }
    }
}
