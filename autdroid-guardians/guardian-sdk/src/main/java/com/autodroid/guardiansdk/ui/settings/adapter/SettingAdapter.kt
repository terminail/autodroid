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
        const val TYPE_GUARDIAN_ITEM_1 = 0
        const val TYPE_GUARDIAN_ITEM_2 = 1
        const val TYPE_GUARDIAN_ITEM_3 = 2
        const val TYPE_GUARDIAN_ITEM_4 = 3
        const val TYPE_GUARDIAN_ITEM_5 = 4
        const val TYPE_ALARM_TRIGGER_MODE_VOLUME_KEY = 5
        const val TYPE_ALARM_TRIGGER_MODE_FLOATING_WINDOW = 6
        const val TYPE_ALARM_TRIGGER_MODE_SHAKE_PHONE = 7
        const val TYPE_DOOR_PASSPHRASE = 11
        const val TYPE_TEST_MODE = 12
    }

    private val items = mutableListOf<SettingItem>()

    interface OnItemClickListener {
        fun onGuardian1Click()
        fun onGuardian2Click()
        fun onGuardian3Click()
        fun onGuardian4Click()
        fun onGuardian5Click()
        fun onVolumeKeyAlarmModeClick()
        fun onFloatingWindowAlarmModeClick()
        fun onShakePhoneAlarmModeClick()
        fun onPasswordBookClick()
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
        return when (val item = items[position]) {
            is SettingItem.GuardianItem1 -> TYPE_GUARDIAN_ITEM_1
            is SettingItem.GuardianItem2 -> TYPE_GUARDIAN_ITEM_2
            is SettingItem.GuardianItem3 -> TYPE_GUARDIAN_ITEM_3
            is SettingItem.GuardianItem4 -> TYPE_GUARDIAN_ITEM_4
            is SettingItem.GuardianItem5 -> TYPE_GUARDIAN_ITEM_5
            is SettingItem.VolumeKeyAlarmMode -> TYPE_ALARM_TRIGGER_MODE_VOLUME_KEY
            is SettingItem.FloatingWindowAlarmMode -> TYPE_ALARM_TRIGGER_MODE_FLOATING_WINDOW
            is SettingItem.ShakePhoneAlarmMode -> TYPE_ALARM_TRIGGER_MODE_SHAKE_PHONE
            is SettingItem.DoorPassphrase -> TYPE_DOOR_PASSPHRASE
            is SettingItem.TestMode -> TYPE_TEST_MODE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GUARDIAN_ITEM_1 -> {
                val view = inflater.inflate(R.layout.guardian_contact_guardian_item, parent, false)
                GuardianItem1ViewHolder(view)
            }
            TYPE_GUARDIAN_ITEM_2 -> {
                val view = inflater.inflate(R.layout.guardian_contact_guardian_item, parent, false)
                GuardianItem2ViewHolder(view)
            }
            TYPE_GUARDIAN_ITEM_3 -> {
                val view = inflater.inflate(R.layout.guardian_contact_guardian_item, parent, false)
                GuardianItem3ViewHolder(view)
            }
            TYPE_GUARDIAN_ITEM_4 -> {
                val view = inflater.inflate(R.layout.guardian_contact_guardian_item, parent, false)
                GuardianItem4ViewHolder(view)
            }
            TYPE_GUARDIAN_ITEM_5 -> {
                val view = inflater.inflate(R.layout.guardian_contact_guardian_item, parent, false)
                GuardianItem5ViewHolder(view)
            }
            TYPE_ALARM_TRIGGER_MODE_VOLUME_KEY -> {
                val view = inflater.inflate(R.layout.guardian_alarm_trigger_mode_item_volume_key, parent, false)
                VolumeKeyAlarmModeViewHolder(view)
            }
            TYPE_ALARM_TRIGGER_MODE_FLOATING_WINDOW -> {
                val view = inflater.inflate(R.layout.guardian_alarm_trigger_mode_item_floating_window, parent, false)
                FloatingWindowAlarmModeViewHolder(view)
            }
            TYPE_ALARM_TRIGGER_MODE_SHAKE_PHONE -> {
                val view = inflater.inflate(R.layout.guardian_alarm_trigger_mode_item_shake_phone, parent, false)
                ShakePhoneAlarmModeViewHolder(view)
            }

            TYPE_DOOR_PASSPHRASE -> {
                val view = inflater.inflate(R.layout.guardian_item_door_passphrase, parent, false)
                DoorPassphraseViewHolder(view)
            }
            TYPE_TEST_MODE -> {
                val view = inflater.inflate(R.layout.guardian_item_test_mode, parent, false)
                TestModeViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is GuardianItem1ViewHolder -> holder.bind(item as SettingItem.GuardianItem1, listener)
            is GuardianItem2ViewHolder -> holder.bind(item as SettingItem.GuardianItem2, listener)
            is GuardianItem3ViewHolder -> holder.bind(item as SettingItem.GuardianItem3, listener)
            is GuardianItem4ViewHolder -> holder.bind(item as SettingItem.GuardianItem4, listener)
            is GuardianItem5ViewHolder -> holder.bind(item as SettingItem.GuardianItem5, listener)
            is VolumeKeyAlarmModeViewHolder -> holder.bind(item as SettingItem.VolumeKeyAlarmMode, listener)
            is FloatingWindowAlarmModeViewHolder -> holder.bind(item as SettingItem.FloatingWindowAlarmMode, listener)
            is ShakePhoneAlarmModeViewHolder -> holder.bind(item as SettingItem.ShakePhoneAlarmMode, listener)
            is DoorPassphraseViewHolder -> holder.bind(item as SettingItem.DoorPassphrase, listener)
            is TestModeViewHolder -> holder.bind(item as SettingItem.TestMode, listener)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class VolumeKeyAlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAlarmModeTitle: TextView = itemView.findViewById(R.id.tv_alarm_mode_title)
        private val tvAlarmModeInfo: TextView = itemView.findViewById(R.id.tv_alarm_mode_info)

        fun bind(item: SettingItem.VolumeKeyAlarmMode, listener: OnItemClickListener?) {
            tvAlarmModeTitle.text = "报警触发模式1"
            val enabledText = if (item.enabled) "☑️" else "□"
            tvAlarmModeInfo.text = "音量键$enabledText，普通报警：${item.normalHoldTime}秒，紧急报警：${item.emergencyHoldTime}秒"
            
            itemView.setOnClickListener {
                listener?.onVolumeKeyAlarmModeClick()
            }
        }
    }

    inner class FloatingWindowAlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAlarmModeTitle: TextView = itemView.findViewById(R.id.tv_alarm_mode_title)
        private val tvAlarmModeInfo: TextView = itemView.findViewById(R.id.tv_alarm_mode_info)

        fun bind(item: SettingItem.FloatingWindowAlarmMode, listener: OnItemClickListener?) {
            tvAlarmModeTitle.text = "报警触发模式2"
            val enabledText = if (item.enabled) "☑️" else "□"
            tvAlarmModeInfo.text = "浮动窗口$enabledText，普通报警：${item.normalHoldTime}秒/${item.normalOpacity}%，紧急报警：${item.emergencyHoldTime}秒/${item.emergencyOpacity}%"
            
            itemView.setOnClickListener {
                listener?.onFloatingWindowAlarmModeClick()
            }
        }
    }

    inner class ShakePhoneAlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAlarmModeTitle: TextView = itemView.findViewById(R.id.tv_alarm_mode_title)
        private val tvAlarmModeInfo: TextView = itemView.findViewById(R.id.tv_alarm_mode_info)

        fun bind(item: SettingItem.ShakePhoneAlarmMode, listener: OnItemClickListener?) {
            tvAlarmModeTitle.text = "报警触发模式3"
            val enabledText = if (item.enabled) "☑️" else "□"
            tvAlarmModeInfo.text = "摇动手机$enabledText，普通报警：灵敏度${item.normalSensitivity}，紧急报警：灵敏度${item.emergencySensitivity}"
            
            itemView.setOnClickListener {
                listener?.onShakePhoneAlarmModeClick()
            }
        }
    }

    inner class DoorPassphraseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)

        fun bind(item: SettingItem.DoorPassphrase, listener: OnItemClickListener?) {
            textView.text = "短信开门密语"
            itemView.setOnClickListener {
                listener?.onDoorPassphraseClick()
            }
        }
    }

    inner class TestModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SettingItem.TestMode, listener: OnItemClickListener?) {
            itemView.setOnClickListener {
                listener?.onTestModeClick()
            }
        }
    }



    // 监护人ViewHolder
    inner class GuardianItem1ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)
        fun bind(item: SettingItem.GuardianItem1, listener: OnItemClickListener?) {
            val displayText = if (item.isPlaceholder) "监护人1: ${item.name}" else "监护人1: ${item.name}"
            textView.text = displayText
            // 如果占位符，设置不同的样式
            if (item.isPlaceholder) {
                textView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            } else {
                textView.setTextColor(itemView.context.getColor(android.R.color.black))
            }
            itemView.setOnClickListener {
                listener?.onGuardian1Click()
            }
        }
    }

    inner class GuardianItem2ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)
        fun bind(item: SettingItem.GuardianItem2, listener: OnItemClickListener?) {
            val displayText = if (item.isPlaceholder) "监护人2: ${item.name}" else "监护人2: ${item.name}"
            textView.text = displayText
            // 如果占位符，设置不同的样式
            if (item.isPlaceholder) {
                textView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            } else {
                textView.setTextColor(itemView.context.getColor(android.R.color.black))
            }
            itemView.setOnClickListener {
                listener?.onGuardian2Click()
            }
        }
    }

    inner class GuardianItem3ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)
        fun bind(item: SettingItem.GuardianItem3, listener: OnItemClickListener?) {
            val displayText = if (item.isPlaceholder) "监护人3: ${item.name}" else "监护人3: ${item.name}"
            textView.text = displayText
            // 如果占位符，设置不同的样式
            if (item.isPlaceholder) {
                textView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            } else {
                textView.setTextColor(itemView.context.getColor(android.R.color.black))
            }
            itemView.setOnClickListener {
                listener?.onGuardian3Click()
            }
        }
    }

    inner class GuardianItem4ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)
        fun bind(item: SettingItem.GuardianItem4, listener: OnItemClickListener?) {
            val displayText = if (item.isPlaceholder) "监护人4: ${item.name}" else "监护人4: ${item.name}"
            textView.text = displayText
            // 如果占位符，设置不同的样式
            if (item.isPlaceholder) {
                textView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            } else {
                textView.setTextColor(itemView.context.getColor(android.R.color.black))
            }
            itemView.setOnClickListener {
                listener?.onGuardian4Click()
            }
        }
    }

    inner class GuardianItem5ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item_title)
        fun bind(item: SettingItem.GuardianItem5, listener: OnItemClickListener?) {
            val displayText = if (item.isPlaceholder) "监护人5: ${item.name}" else "监护人5: ${item.name}"
            textView.text = displayText
            // 如果占位符，设置不同的样式
            if (item.isPlaceholder) {
                textView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            } else {
                textView.setTextColor(itemView.context.getColor(android.R.color.black))
            }
            itemView.setOnClickListener {
                listener?.onGuardian5Click()
            }
        }
    }
}
