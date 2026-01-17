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
        const val TYPE_ALARM_TRIGGER_MODE_INACTIVITY = 8
        const val TYPE_ALARM_MESSAGE_PASSWORD = 9
        const val TYPE_ALARM_RECORDING_MODE = 10
        const val TYPE_ALARM_EMAIL_SETTINGS = 11
        const val TYPE_HIDDEN_SECURE_UI = 12
        const val TYPE_TEST_MODE = 13
        const val TYPE_PING_SETTINGS = 14
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
        fun onInactivityAlarmModeClick()
        fun onAlarmMessagePasswordClick()
        fun onAlarmRecordingModeClick()
        fun onAlarmEmailSettingsClick()
        fun onPasswordBookClick()
        fun onHiddenSecureUIClick()
        fun onTestModeClick()
        fun onPingSettingsClick()
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
            is SettingItem.InactivityAlarmMode -> TYPE_ALARM_TRIGGER_MODE_INACTIVITY
            is SettingItem.AlarmMessagePassword -> TYPE_ALARM_MESSAGE_PASSWORD
            is SettingItem.AlarmRecordingMode -> TYPE_ALARM_RECORDING_MODE
            is SettingItem.AlarmEmailSettings -> TYPE_ALARM_EMAIL_SETTINGS
            is SettingItem.HiddenSecureUI -> TYPE_HIDDEN_SECURE_UI
            is SettingItem.TestMode -> TYPE_TEST_MODE
            is SettingItem.PingSettings -> TYPE_PING_SETTINGS
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
            TYPE_ALARM_TRIGGER_MODE_INACTIVITY -> {
                val view = inflater.inflate(R.layout.guardian_alarm_trigger_mode_item_inactivity, parent, false)
                InactivityAlarmModeViewHolder(view)
            }
            TYPE_ALARM_MESSAGE_PASSWORD -> {
                val view = inflater.inflate(R.layout.guardian_item_alarm_message_password, parent, false)
                AlarmMessagePasswordViewHolder(view)
            }
            TYPE_ALARM_RECORDING_MODE -> {
                val view = inflater.inflate(R.layout.guardian_item_alarm_recording_mode, parent, false)
                AlarmRecordingModeViewHolder(view)
            }
            TYPE_ALARM_EMAIL_SETTINGS -> {
                val view = inflater.inflate(R.layout.guardian_item_alarm_email_settings, parent, false)
                AlarmEmailSettingsViewHolder(view)
            }
            TYPE_HIDDEN_SECURE_UI -> {
                val view = inflater.inflate(R.layout.guardian_item_hidden_secure_ui, parent, false)
                HiddenSecureUIViewHolder(view)
            }
            TYPE_TEST_MODE -> {
                val view = inflater.inflate(R.layout.guardian_item_test_mode, parent, false)
                TestModeViewHolder(view)
            }
            TYPE_PING_SETTINGS -> {
                val view = inflater.inflate(R.layout.guardian_item_ping_settings, parent, false)
                PingSettingsViewHolder(view)
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
            is InactivityAlarmModeViewHolder -> holder.bind(item as SettingItem.InactivityAlarmMode, listener)
            is AlarmMessagePasswordViewHolder -> holder.bind(item as SettingItem.AlarmMessagePassword, listener)
            is AlarmRecordingModeViewHolder -> holder.bind(item as SettingItem.AlarmRecordingMode, listener)
            is AlarmEmailSettingsViewHolder -> holder.bind(item as SettingItem.AlarmEmailSettings, listener)
            is HiddenSecureUIViewHolder -> holder.bind(item as SettingItem.HiddenSecureUI, listener)
            is TestModeViewHolder -> holder.bind(item as SettingItem.TestMode, listener)
            is PingSettingsViewHolder -> holder.bind(item as SettingItem.PingSettings, listener)
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

    inner class InactivityAlarmModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAlarmModeTitle: TextView = itemView.findViewById(R.id.tv_alarm_mode_title)
        private val tvAlarmModeInfo: TextView = itemView.findViewById(R.id.tv_alarm_mode_info)

        fun bind(item: SettingItem.InactivityAlarmMode, listener: OnItemClickListener?) {
            tvAlarmModeTitle.text = "报警触发模式4"
            val enabledText = if (item.enabled) "☑️" else "□"
            tvAlarmModeInfo.text = "长时间未使用手机$enabledText，普通报警：${item.normalInactivityTime}分钟，紧急报警：${item.emergencyInactivityTime}分钟"
            
            itemView.setOnClickListener {
                listener?.onInactivityAlarmModeClick()
            }
        }
    }

    inner class AlarmMessagePasswordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val tvItemSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)

        fun bind(item: SettingItem.AlarmMessagePassword, listener: OnItemClickListener?) {
            tvItemTitle.text = "报警信息密码"
            val passwordStatus = if (item.password.isNotEmpty()) "已设置" else "未设置"
            tvItemSubtitle.text = "配置报警时的暗语密码 - $passwordStatus"
            
            itemView.setOnClickListener {
                listener?.onAlarmMessagePasswordClick()
            }
        }
    }

    inner class AlarmRecordingModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val tvItemSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)

        fun bind(item: SettingItem.AlarmRecordingMode, listener: OnItemClickListener?) {
            tvItemTitle.text = "报警录音模式"
            val enabledText = if (item.enabled) "已启用" else "已禁用"
            tvItemSubtitle.text = "录制报警时的音频 - $enabledText，时长：${item.duration}分钟"
            
            itemView.setOnClickListener {
                listener?.onAlarmRecordingModeClick()
            }
        }
    }

    inner class AlarmEmailSettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val tvItemSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)

        fun bind(item: SettingItem.AlarmEmailSettings, listener: OnItemClickListener?) {
            tvItemTitle.text = "报警邮件设置"
            val enabledText = if (item.enabled) "已启用" else "已禁用"
            val emailInfo = if (item.emailAddress.isNotEmpty()) item.emailAddress else "未配置"
            tvItemSubtitle.text = "配置邮件发送报警信息 - $enabledText，邮箱：$emailInfo"
            
            itemView.setOnClickListener {
                listener?.onAlarmEmailSettingsClick()
            }
        }
    }

    inner class HiddenSecureUIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val tvItemSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)

        fun bind(item: SettingItem.HiddenSecureUI, listener: OnItemClickListener?) {
            tvItemTitle.text = "隐秘界面设置"
            val enabledText = if (item.enabled) "已开启" else "已关闭"
            tvItemSubtitle.text = "隐藏监护界面，防止被发现 - $enabledText"
            
            itemView.setOnClickListener {
                listener?.onHiddenSecureUIClick()
            }
        }
    }

    inner class TestModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val tvItemSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)

        fun bind(item: SettingItem.TestMode, listener: OnItemClickListener?) {
            tvItemTitle.text = "测试模式"
            val enabledText = if (item.isEnabled) "已启用" else "已禁用"
            val practiceInfo = "练习次数：${item.practiceCount}"
            tvItemSubtitle.text = "练习报警操作 - $enabledText，$practiceInfo"
            
            itemView.setOnClickListener {
                listener?.onTestModeClick()
            }
        }
    }

    inner class PingSettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val tvItemSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)

        fun bind(item: SettingItem.PingSettings, listener: OnItemClickListener?) {
            tvItemTitle.text = "Ping响应设置"
            val enabledText = if (item.enabled) "已启用" else "已禁用"
            val intervalInfo = "检查间隔：${item.checkInterval}分钟"
            tvItemSubtitle.text = "响应监护人Ping请求 - $enabledText，$intervalInfo"
            
            itemView.setOnClickListener {
                listener?.onPingSettingsClick()
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
