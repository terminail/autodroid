package com.autodroid.guardiansdk.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.ui.settings.adapter.SettingAdapter
import com.autodroid.guardiansdk.ui.settings.model.SettingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 我的设置界面
 * 显示所有设置项的列表，点击弹出对应的修改对话框
 * 包括：报警触发模式、位置密码本等设置项和历史记录
 */
class SettingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var settingAdapter: SettingAdapter
    
    private val viewModel: SettingViewModel by viewModels {
        SettingViewModelFactory(
            GuardianDatabase.getDatabase(requireContext())
        )
    }

    companion object {
        fun newInstance() = SettingFragment()
        
        private fun formatMinutesToTime(minutes: Int): String {
            val hours = minutes / 60
            val mins = minutes % 60
            return if (hours > 0) {
                "${hours}小时${mins}分钟"
            } else {
                "${mins}分钟"
            }
        }
        
        /**
         * 格式化时间，类似微信的时间显示格式
         * 今天：显示时间（如：16:23）
         * 昨天：显示"昨天"
         * 一周内：显示星期几（如：周一）
         * 更早：显示月日（如：12月3日）
         */
        fun formatWeChatStyleTime(timeString: String): String {
            if (timeString.isEmpty()) {
                return "从未练习"
            }
            
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val practiceTime = dateFormat.parse(timeString)
                val now = java.util.Date()
                val calendar = java.util.Calendar.getInstance()
                calendar.time = now
                
                val practiceCalendar = java.util.Calendar.getInstance()
                practiceCalendar.time = practiceTime
                
                // 计算时间差（毫秒）
                val diff = now.time - practiceTime.time
                val diffMinutes = diff / (60 * 1000)
                val diffHours = diff / (60 * 60 * 1000)
                val diffDays = diff / (24 * 60 * 60 * 1000)
                
                // 今天
                if (practiceCalendar.get(java.util.Calendar.YEAR) == calendar.get(java.util.Calendar.YEAR) &&
                    practiceCalendar.get(java.util.Calendar.DAY_OF_YEAR) == calendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    return timeFormat.format(practiceTime)
                }
                
                // 昨天
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                if (practiceCalendar.get(java.util.Calendar.YEAR) == calendar.get(java.util.Calendar.YEAR) &&
                    practiceCalendar.get(java.util.Calendar.DAY_OF_YEAR) == calendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                    return "昨天"
                }
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                
                // 一周内
                if (diffDays < 7) {
                    val dayOfWeek = practiceCalendar.get(java.util.Calendar.DAY_OF_WEEK)
                    return when (dayOfWeek) {
                        java.util.Calendar.SUNDAY -> "周日"
                        java.util.Calendar.MONDAY -> "周一"
                        java.util.Calendar.TUESDAY -> "周二"
                        java.util.Calendar.WEDNESDAY -> "周三"
                        java.util.Calendar.THURSDAY -> "周四"
                        java.util.Calendar.FRIDAY -> "周五"
                        java.util.Calendar.SATURDAY -> "周六"
                        else -> ""
                    }
                }
                
                // 更早的时间
                val monthDayFormat = java.text.SimpleDateFormat("M月d日", java.util.Locale.getDefault())
                return monthDayFormat.format(practiceTime)
                
            } catch (e: Exception) {
                return "时间格式错误"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guardian_fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupObservers()
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        settingAdapter = SettingAdapter()
        settingAdapter.setOnItemClickListener(object : SettingAdapter.OnItemClickListener {
            override fun onGuardian1Click() {
                showGuardianDialog(1)
            }

            override fun onGuardian2Click() {
                showGuardianDialog(2)
            }

            override fun onGuardian3Click() {
                showGuardianDialog(3)
            }

            override fun onGuardian4Click() {
                showGuardianDialog(4)
            }

            override fun onGuardian5Click() {
                showGuardianDialog(5)
            }

            override fun onVolumeKeyAlarmModeClick() {
                showVolumeKeyAlarmModeDialog()
            }

            override fun onFloatingWindowAlarmModeClick() {
                showFloatingWindowAlarmModeDialog()
            }

            override fun onShakePhoneAlarmModeClick() {
                showShakePhoneAlarmModeDialog()
            }

            override fun onInactivityAlarmModeClick() {
                showInactivityAlarmModeDialog()
            }

            override fun onAlarmMessagePasswordClick() {
                showPasswordBookDialog()
            }

            override fun onAlarmRecordingModeClick() {
                showAlarmRecordingModeDialog()
            }

            override fun onAlarmEmailSettingsClick() {
                showAlarmEmailSettingsDialog()
            }

            override fun onHiddenSecureUIClick() {
                showHiddenSecureUIDialog()
            }

            override fun onPasswordBookClick() {
                showPasswordBookDialog()
            }

            override fun onTestModeClick() {
                showTestModeDialog()
            }

            override fun onPingSettingsClick() {
                showPingSettingsDialog()
            }
        })

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingAdapter
        }
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        viewModel.settingItems.observe(viewLifecycleOwner) { settingItems ->
            settingItems?.let {
                settingAdapter.updateItems(it)
            }
        }
    }

    /**
     * 显示报警触发模式-音量键设置对话框
     */
    private fun showVolumeKeyAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_alarm_trigger_mode_dialog_volume_key, null)

        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_volume_key_alarm_enabled)
        val seekNormalTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_hold_time)
        val seekEmergencyTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_hold_time)
        val tvNormalTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_normal_hold_time_value)
        val tvEmergencyTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_emergency_hold_time_value)

        // 加载已保存的设置
        lifecycleScope.launch {
            val enabledSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("volume_key_alarm_enabled")
            }
            val normalTimeSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("volume_key_normal_hold_time")
            }
            val emergencyTimeSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("volume_key_emergency_hold_time")
            }

            cbEnabled.isChecked = enabledSetting?.value?.toBoolean() ?: true
            val normalTime = normalTimeSetting?.value?.toIntOrNull() ?: 5
            val emergencyTime = emergencyTimeSetting?.value?.toIntOrNull() ?: 10

            seekNormalTime.progress = normalTime
            seekEmergencyTime.progress = emergencyTime
            tvNormalTime.text = "当前值: ${normalTime}秒"
            tvEmergencyTime.text = "当前值: ${emergencyTime}秒"
        }

        // 设置SeekBar监听器
        seekNormalTime.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvNormalTime.text = "当前值: ${progress}秒"
                if (fromUser) {
                    val emergencyValue = (progress + 5).coerceAtMost(seekEmergencyTime.max)
                    seekEmergencyTime.progress = emergencyValue
                    tvEmergencyTime.text = "当前值: ${emergencyValue}秒"
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmergencyTime.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val minValue = seekNormalTime.progress + 5
                val adjustedProgress = if (progress < minValue) minValue else progress
                if (adjustedProgress != progress) {
                    seekEmergencyTime.progress = adjustedProgress
                }
                tvEmergencyTime.text = "当前值: ${adjustedProgress}秒"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警触发模式-音量键设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警触发模式-音量键设置
                saveVolumeKeyAlarmModeSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示报警触发模式-浮动窗口设置对话框
     */
    private fun showFloatingWindowAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_alarm_trigger_mode_dialog_floating_window, null)

        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_floating_window_alarm_enabled)
        val seekNormalTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_hold_time)
        val seekNormalOpacity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_opacity)
        val seekEmergencyTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_hold_time)
        val seekEmergencyOpacity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_opacity)
        val tvNormalTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_normal_hold_time_value)
        val tvNormalOpacity = dialogView.findViewById<android.widget.TextView>(R.id.tv_normal_opacity_value)
        val tvEmergencyTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_emergency_hold_time_value)
        val tvEmergencyOpacity = dialogView.findViewById<android.widget.TextView>(R.id.tv_emergency_opacity_value)

        // 加载已保存的设置
        lifecycleScope.launch {
            val enabledSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("floating_window_alarm_enabled")
            }
            val normalTimeSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("floating_window_normal_hold_time")
            }
            val normalOpacitySetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("floating_window_normal_opacity")
            }
            val emergencyTimeSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("floating_window_emergency_hold_time")
            }
            val emergencyOpacitySetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("floating_window_emergency_opacity")
            }

            cbEnabled.isChecked = enabledSetting?.value?.toBoolean() ?: true
            val normalTime = normalTimeSetting?.value?.toIntOrNull() ?: 5
            val normalOpacity = normalOpacitySetting?.value?.toIntOrNull() ?: 50
            val emergencyTime = emergencyTimeSetting?.value?.toIntOrNull() ?: 10
            val emergencyOpacity = emergencyOpacitySetting?.value?.toIntOrNull() ?: 80

            seekNormalTime.progress = normalTime
            seekNormalOpacity.progress = normalOpacity
            seekEmergencyTime.progress = emergencyTime
            seekEmergencyOpacity.progress = emergencyOpacity
            tvNormalTime.text = "当前值: ${normalTime}秒"
            tvNormalOpacity.text = "当前值: ${normalOpacity}%"
            tvEmergencyTime.text = "当前值: ${emergencyTime}秒"
            tvEmergencyOpacity.text = "当前值: ${emergencyOpacity}%"
        }

        // 设置SeekBar监听器
        seekNormalTime.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvNormalTime.text = "当前值: ${progress}秒"
                if (fromUser) {
                    val emergencyValue = (progress + 5).coerceAtMost(seekEmergencyTime.max)
                    seekEmergencyTime.progress = emergencyValue
                    tvEmergencyTime.text = "当前值: ${emergencyValue}秒"
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekNormalOpacity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvNormalOpacity.text = "当前值: ${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmergencyTime.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val minValue = seekNormalTime.progress + 5
                val adjustedProgress = if (progress < minValue) minValue else progress
                if (adjustedProgress != progress) {
                    seekEmergencyTime.progress = adjustedProgress
                }
                tvEmergencyTime.text = "当前值: ${adjustedProgress}秒"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmergencyOpacity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvEmergencyOpacity.text = "当前值: ${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警触发模式-浮动窗口设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警触发模式-浮动窗口设置
                saveFloatingWindowAlarmModeSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示报警触发模式-摇动手机设置对话框
     */
    private fun showShakePhoneAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_alarm_trigger_mode_dialog_shake_phone, null)

        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_shake_alarm_enabled)
        val seekNormalSensitivity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_sensitivity)
        val seekEmergencySensitivity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_sensitivity)
        val tvNormalSensitivity = dialogView.findViewById<android.widget.TextView>(R.id.tv_normal_sensitivity_value)
        val tvEmergencySensitivity = dialogView.findViewById<android.widget.TextView>(R.id.tv_emergency_sensitivity_value)

        // 加载已保存的设置
        lifecycleScope.launch {
            val enabledSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("shake_alarm_enabled")
            }
            val normalSensitivitySetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("shake_normal_sensitivity")
            }
            val emergencySensitivitySetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("shake_emergency_sensitivity")
            }

            cbEnabled.isChecked = enabledSetting?.value?.toBoolean() ?: true
            val normalSensitivity = normalSensitivitySetting?.value?.toIntOrNull() ?: 5
            val emergencySensitivity = emergencySensitivitySetting?.value?.toIntOrNull() ?: 8

            seekNormalSensitivity.progress = normalSensitivity
            seekEmergencySensitivity.progress = emergencySensitivity
            tvNormalSensitivity.text = "当前值: ${normalSensitivity}级"
            tvEmergencySensitivity.text = "当前值: ${emergencySensitivity}级"
        }

        // 设置SeekBar监听器
        seekNormalSensitivity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvNormalSensitivity.text = "当前值: ${progress}级"
                if (fromUser) {
                    val emergencyValue = (progress + 5).coerceAtMost(seekEmergencySensitivity.max)
                    seekEmergencySensitivity.progress = emergencyValue
                    tvEmergencySensitivity.text = "当前值: ${emergencyValue}级"
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmergencySensitivity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val minValue = seekNormalSensitivity.progress + 5
                val adjustedProgress = if (progress < minValue) minValue else progress
                if (adjustedProgress != progress) {
                    seekEmergencySensitivity.progress = adjustedProgress
                }
                tvEmergencySensitivity.text = "当前值: ${adjustedProgress}级"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警触发模式-摇动手机设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警触发模式-摇动手机设置
                saveShakePhoneAlarmModeSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示报警触发模式-长时间未使用手机设置对话框
     */
    private fun showInactivityAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_inactivity_alarm_mode, null)

        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_inactivity_alarm_enabled)
        val seekNormalTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_inactivity_time)
        val seekEmergencyTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_inactivity_time)
        val tvNormalTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_normal_inactivity_time_value)
        val tvEmergencyTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_emergency_inactivity_time_value)

        // 加载已保存的设置
        lifecycleScope.launch {
            val enabledSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("inactivity_alarm_enabled")
            }
            val normalTimeSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("inactivity_normal_time")
            }
            val emergencyTimeSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("inactivity_emergency_time")
            }

            cbEnabled.isChecked = enabledSetting?.value?.toBoolean() ?: true
            val normalTime = normalTimeSetting?.value?.toIntOrNull() ?: 60
            val emergencyTime = emergencyTimeSetting?.value?.toIntOrNull() ?: 30

            seekNormalTime.progress = normalTime
            seekEmergencyTime.progress = emergencyTime
            tvNormalTime.text = "当前值: ${formatMinutesToTime(normalTime)}"
            tvEmergencyTime.text = "当前值: ${formatMinutesToTime(emergencyTime)}"
        }

        // 设置SeekBar监听器
        seekNormalTime.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvNormalTime.text = "当前值: ${formatMinutesToTime(progress)}"
                if (fromUser) {
                    val emergencyValue = (progress + 5).coerceAtMost(seekEmergencyTime.max)
                    seekEmergencyTime.progress = emergencyValue
                    tvEmergencyTime.text = "当前值: ${formatMinutesToTime(emergencyValue)}"
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmergencyTime.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val minValue = seekNormalTime.progress + 5
                val adjustedProgress = if (progress < minValue) minValue else progress
                if (adjustedProgress != progress) {
                    seekEmergencyTime.progress = adjustedProgress
                }
                tvEmergencyTime.text = "当前值: ${formatMinutesToTime(adjustedProgress)}"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警触发模式-长时间未使用手机设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警触发模式-长时间未使用手机设置
                saveInactivityAlarmModeSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示报警录音模式设置对话框
     */
    private fun showAlarmRecordingModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_alarm_recording_mode, null)

        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_recording_enabled)
        val seekDuration = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_recording_duration)
        val seekSegmentDuration = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_segment_duration)
        val tvDuration = dialogView.findViewById<android.widget.TextView>(R.id.tv_recording_duration_value)
        val tvSegmentDuration = dialogView.findViewById<android.widget.TextView>(R.id.tv_segment_duration_value)

        // 加载已保存的设置
        lifecycleScope.launch {
            val enabledSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("recording_enabled")
            }
            val durationSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("recording_duration")
            }
            val segmentDurationSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("recording_segment_duration")
            }

            cbEnabled.isChecked = enabledSetting?.value?.toBoolean() ?: true
            val duration = durationSetting?.value?.toIntOrNull() ?: 5
            val segmentDuration = segmentDurationSetting?.value?.toIntOrNull() ?: 2

            seekDuration.progress = duration
            seekSegmentDuration.progress = segmentDuration
            tvDuration.text = "当前值: ${duration}分钟"
            tvSegmentDuration.text = "当前值: ${segmentDuration}分钟"
        }

        // 设置SeekBar监听器
        seekDuration.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvDuration.text = "当前值: ${progress}分钟"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekSegmentDuration.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvSegmentDuration.text = "当前值: ${progress}分钟"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警录音模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警录音模式设置
                saveAlarmRecordingModeSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示报警邮件设置对话框
     */
    private fun showAlarmEmailSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_alarm_email_settings, null)

        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_email_enabled)
        val etEmailAddress = dialogView.findViewById<EditText>(R.id.et_email_address)
        val tvEmailError = dialogView.findViewById<TextView>(R.id.tv_email_error)
        val etEmailPassword = dialogView.findViewById<EditText>(R.id.et_email_password)
        val etSmtpHost = dialogView.findViewById<EditText>(R.id.et_smtp_host)
        val etSmtpPort = dialogView.findViewById<EditText>(R.id.et_smtp_port)
        val cbUseTls = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_use_tls)

        // 加载已保存的设置
        lifecycleScope.launch {
            val enabledSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("email_enabled")
            }
            val emailAddressSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("email_address")
            }
            val emailPasswordSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("email_password")
            }
            val smtpHostSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("smtp_host")
            }
            val smtpPortSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("smtp_port")
            }
            val useTlsSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("smtp_tls")
            }

            cbEnabled.isChecked = enabledSetting?.value?.toBoolean() ?: false
            etEmailAddress.setText(emailAddressSetting?.value ?: "")
            etEmailPassword.setText(
                if (emailPasswordSetting?.value?.isNotEmpty() == true) {
                    try {
                        com.autodroid.guardiansdk.util.EncryptionUtils.decryptString(emailPasswordSetting.value)
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    ""
                }
            )
            etSmtpHost.setText(smtpHostSetting?.value ?: "smtp.gmail.com")
            etSmtpPort.setText(smtpPortSetting?.value ?: "587")
            cbUseTls.isChecked = useTlsSetting?.value?.toBoolean() ?: true
        }

        // 实时验证邮箱格式
        etEmailAddress.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val email = s?.toString()?.trim() ?: ""
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    tvEmailError.visibility = View.VISIBLE
                } else {
                    tvEmailError.visibility = View.GONE
                }
            }
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警邮件设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警邮件设置
                saveAlarmEmailSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示密码本设置对话框
     */
    private fun showPasswordBookDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_secure_book, null)

        // 获取所有TextView引用
        val tvDigit0 = dialogView.findViewById<TextView>(R.id.tv_digit_0)
        val tvDigit1 = dialogView.findViewById<TextView>(R.id.tv_digit_1)
        val tvDigit2 = dialogView.findViewById<TextView>(R.id.tv_digit_2)
        val tvDigit3 = dialogView.findViewById<TextView>(R.id.tv_digit_3)
        val tvDigit4 = dialogView.findViewById<TextView>(R.id.tv_digit_4)
        val tvDigit5 = dialogView.findViewById<TextView>(R.id.tv_digit_5)
        val tvDigit6 = dialogView.findViewById<TextView>(R.id.tv_digit_6)
        val tvDigit7 = dialogView.findViewById<TextView>(R.id.tv_digit_7)
        val tvDigit8 = dialogView.findViewById<TextView>(R.id.tv_digit_8)
        val tvDigit9 = dialogView.findViewById<TextView>(R.id.tv_digit_9)
        val tvComma = dialogView.findViewById<TextView>(R.id.tv_comma)
        val tvAlarm0 = dialogView.findViewById<TextView>(R.id.tv_alarm_0)
        val tvAlarm1 = dialogView.findViewById<TextView>(R.id.tv_alarm_1)
        val tvReserve0 = dialogView.findViewById<TextView>(R.id.tv_reserve_0)
        val tvReserve1 = dialogView.findViewById<TextView>(R.id.tv_reserve_1)
        val tvReserve2 = dialogView.findViewById<TextView>(R.id.tv_reserve_2)

        // 保存当前生成的密码本（用于保存）
        var currentSecureBook: com.autodroid.guardiansdk.data.model.SecureBook? = null

        // 加载当前密码本设置
        lifecycleScope.launch {
            val secureBookSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("secure_book")
            }

            val secureBook = if (secureBookSetting != null) {
                com.autodroid.guardiansdk.utils.SecureBookUtils.decodeSecureBook(secureBookSetting.value)
            } else {
                com.autodroid.guardiansdk.data.model.SecureBook.getDefault()
            }
            currentSecureBook = secureBook

            // 填充TextView
            tvDigit0.setText(secureBook.digit0)
            tvDigit1.setText(secureBook.digit1)
            tvDigit2.setText(secureBook.digit2)
            tvDigit3.setText(secureBook.digit3)
            tvDigit4.setText(secureBook.digit4)
            tvDigit5.setText(secureBook.digit5)
            tvDigit6.setText(secureBook.digit6)
            tvDigit7.setText(secureBook.digit7)
            tvDigit8.setText(secureBook.digit8)
            tvDigit9.setText(secureBook.digit9)
            tvComma.setText(secureBook.comma)
            tvAlarm0.setText(secureBook.alarm0)
            tvAlarm1.setText(secureBook.alarm1)
            tvReserve0.setText(secureBook.reserve0)
            tvReserve1.setText(secureBook.reserve1)
            tvReserve2.setText(secureBook.reserve2)
        }

        // 一键生成随机按钮
        val btnGenerateRandom = dialogView.findViewById<android.widget.Button>(R.id.btn_generate_random)
        btnGenerateRandom.setOnClickListener {
            val randomSecureBook = com.autodroid.guardiansdk.utils.SecureBookUtils.generateRandomSecureBook()
            currentSecureBook = randomSecureBook
            tvDigit0.setText(randomSecureBook.digit0)
            tvDigit1.setText(randomSecureBook.digit1)
            tvDigit2.setText(randomSecureBook.digit2)
            tvDigit3.setText(randomSecureBook.digit3)
            tvDigit4.setText(randomSecureBook.digit4)
            tvDigit5.setText(randomSecureBook.digit5)
            tvDigit6.setText(randomSecureBook.digit6)
            tvDigit7.setText(randomSecureBook.digit7)
            tvDigit8.setText(randomSecureBook.digit8)
            tvDigit9.setText(randomSecureBook.digit9)
            tvComma.setText(randomSecureBook.comma)
            tvAlarm0.setText(randomSecureBook.alarm0)
            tvAlarm1.setText(randomSecureBook.alarm1)
            tvReserve0.setText(randomSecureBook.reserve0)
            tvReserve1.setText(randomSecureBook.reserve1)
            tvReserve2.setText(randomSecureBook.reserve2)
            android.widget.Toast.makeText(requireContext(), "已生成随机密码本", android.widget.Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("密码本设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存密码本设置
                currentSecureBook?.let { saveSecureBookSettings(it) }
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示隐秘界面设置对话框
     */
    private fun showHiddenSecureUIDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_hidden_secure_ui, null)

        val tvHiddenUIStatus = dialogView.findViewById<TextView>(R.id.tv_hidden_ui_status)
        val switchHiddenUIEnabled = dialogView.findViewById<android.widget.Switch>(R.id.switch_hidden_ui_enabled)
        val etDoorPassphrase = dialogView.findViewById<EditText>(R.id.et_door_passphrase)

        // 加载已保存的设置
        lifecycleScope.launch {
            val hiddenUIEnabledSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("hidden_secure_ui_enabled")
            }
            val doorPassphraseSetting = withContext(Dispatchers.IO) {
                viewModel.getSetting("door_passphrase")
            }

            val isEnabled = hiddenUIEnabledSetting?.value?.toBoolean() ?: false
            switchHiddenUIEnabled.isChecked = isEnabled
            tvHiddenUIStatus.text = if (isEnabled) "隐秘界面开启" else "隐秘界面关闭"
            etDoorPassphrase.setText(doorPassphraseSetting?.value ?: "小兔子乖乖把门开开")
        }

        // Switch状态改变时更新TextView文字
        switchHiddenUIEnabled.setOnCheckedChangeListener { _, isChecked ->
            tvHiddenUIStatus.text = if (isChecked) "隐秘界面开启" else "隐秘界面关闭"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("隐秘界面设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存隐秘界面设置
                saveHiddenSecureUISettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示测试模式设置对话框
     */
    private fun showTestModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_test_mode, null)

        val cbTestMode = dialogView.findViewById<android.widget.CheckBox>(R.id.cbTestMode)

        lifecycleScope.launch {
            val isEnabled = withContext(Dispatchers.IO) {
                viewModel.getBoolean("test_mode_enabled", false)
            }

            cbTestMode.isChecked = isEnabled
        }

        AlertDialog.Builder(requireContext())
            .setTitle("测试模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                saveTestModeSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveVolumeKeyAlarmModeSettings(dialogView: View) {
        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_volume_key_alarm_enabled)
        val seekNormalTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_hold_time)
        val seekEmergencyTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_hold_time)
        
        val isEnabled = cbEnabled.isChecked
        val normalHoldTime = seekNormalTime.progress
        val emergencyHoldTime = seekEmergencyTime.progress
        
        lifecycleScope.launch {
            try {
                // 保存设置到数据库
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "volume_key_alarm_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "音量键报警启用状态",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "volume_key_normal_hold_time",
                        value = normalHoldTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "音量键普通报警长按时间（秒）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "volume_key_emergency_hold_time",
                        value = emergencyHoldTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "音量键紧急报警长按时间（秒）",
                        category = "alarm"
                    )
                )
                
                viewModel.updateVolumeKeyAlarmMode(isEnabled, normalHoldTime, emergencyHoldTime)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警触发模式-音量键设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveFloatingWindowAlarmModeSettings(dialogView: View) {
        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_floating_window_alarm_enabled)
        val seekNormalTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_hold_time)
        val seekNormalOpacity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_opacity)
        val seekEmergencyTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_hold_time)
        val seekEmergencyOpacity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_opacity)
        
        val isEnabled = cbEnabled.isChecked
        val normalHoldTime = seekNormalTime.progress
        val normalOpacity = seekNormalOpacity.progress
        val emergencyHoldTime = seekEmergencyTime.progress
        val emergencyOpacity = seekEmergencyOpacity.progress
        
        lifecycleScope.launch {
            try {
                // 保存设置到数据库
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "floating_window_alarm_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "浮动窗口报警启用状态",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "floating_window_normal_hold_time",
                        value = normalHoldTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口普通报警长按时间（秒）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "floating_window_normal_opacity",
                        value = normalOpacity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口普通报警透明度（%）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "floating_window_emergency_hold_time",
                        value = emergencyHoldTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口紧急报警长按时间（秒）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "floating_window_emergency_opacity",
                        value = emergencyOpacity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口紧急报警透明度（%）",
                        category = "alarm"
                    )
                )
                
                viewModel.updateFloatingWindowAlarmMode(isEnabled, normalHoldTime, normalOpacity, emergencyHoldTime, emergencyOpacity)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警触发模式-浮动窗口设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveShakePhoneAlarmModeSettings(dialogView: View) {
        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_shake_alarm_enabled)
        val seekNormalSensitivity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_sensitivity)
        val seekEmergencySensitivity = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_sensitivity)
        
        val isEnabled = cbEnabled.isChecked
        val normalSensitivity = seekNormalSensitivity.progress
        val emergencySensitivity = seekEmergencySensitivity.progress
        
        lifecycleScope.launch {
            try {
                // 保存设置到数据库
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "shake_alarm_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "摇动手机报警启用状态",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "shake_normal_sensitivity",
                        value = normalSensitivity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "摇动手机普通报警敏感度（1-10级）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "shake_emergency_sensitivity",
                        value = emergencySensitivity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "摇动手机紧急报警敏感度（1-10级）",
                        category = "alarm"
                    )
                )
                
                viewModel.updateShakePhoneAlarmMode(isEnabled, normalSensitivity, emergencySensitivity)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警触发模式-摇动手机设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveInactivityAlarmModeSettings(dialogView: View) {
        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_inactivity_alarm_enabled)
        val seekNormalTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_normal_inactivity_time)
        val seekEmergencyTime = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_emergency_inactivity_time)
        
        val isEnabled = cbEnabled.isChecked
        val normalTime = seekNormalTime.progress
        val emergencyTime = seekEmergencyTime.progress
        
        lifecycleScope.launch {
            try {
                // 保存设置到数据库
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "inactivity_alarm_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "长时间未使用手机报警启用状态",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "inactivity_normal_time",
                        value = normalTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "长时间未使用手机普通报警时间阈值（分钟）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "inactivity_emergency_time",
                        value = emergencyTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "长时间未使用手机紧急报警时间阈值（分钟）",
                        category = "alarm"
                    )
                )
                
                viewModel.updateInactivityAlarmMode(isEnabled, normalTime, emergencyTime)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警触发模式-长时间未使用手机设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveAlarmRecordingModeSettings(dialogView: View) {
        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_recording_enabled)
        val seekDuration = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_recording_duration)
        val seekSegmentDuration = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_segment_duration)
        
        val isEnabled = cbEnabled.isChecked
        val duration = seekDuration.progress
        val segmentDuration = seekSegmentDuration.progress
        
        lifecycleScope.launch {
            try {
                // 保存设置到数据库
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "recording_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "报警录音启用状态",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "recording_duration",
                        value = duration.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "报警录音总时长（分钟）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "recording_segment_duration",
                        value = segmentDuration.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "报警录音分段时长（分钟）",
                        category = "alarm"
                    )
                )
                
                viewModel.updateAlarmRecordingMode(isEnabled, duration, segmentDuration)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警录音模式设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveAlarmEmailSettings(dialogView: View) {
        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_email_enabled)
        val etEmailAddress = dialogView.findViewById<EditText>(R.id.et_email_address)
        val tvEmailError = dialogView.findViewById<TextView>(R.id.tv_email_error)
        val etEmailPassword = dialogView.findViewById<EditText>(R.id.et_email_password)
        val etSmtpHost = dialogView.findViewById<EditText>(R.id.et_smtp_host)
        val etSmtpPort = dialogView.findViewById<EditText>(R.id.et_smtp_port)
        val cbUseTls = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_use_tls)
        
        val isEnabled = cbEnabled.isChecked
        val emailAddress = etEmailAddress.text.toString().trim()
        val emailPassword = etEmailPassword.text.toString().trim()
        val smtpHost = etSmtpHost.text.toString().trim()
        val smtpPort = etSmtpPort.text.toString().trim()
        val useTls = cbUseTls.isChecked
        
        if (isEnabled && emailAddress.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "请填写邮箱地址",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // 验证邮箱格式
        if (isEnabled && !isValidEmail(emailAddress)) {
            tvEmailError.visibility = View.VISIBLE
            android.widget.Toast.makeText(
                requireContext(),
                "邮箱格式不正确",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // 加密密码
                val encryptedPassword = if (emailPassword.isNotEmpty()) {
                    com.autodroid.guardiansdk.util.EncryptionUtils.encryptString(emailPassword)
                } else {
                    ""
                }
                
                // 保存设置到数据库
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "email_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "报警邮件启用状态",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "email_address",
                        value = emailAddress,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "报警邮件地址",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "email_password",
                        value = encryptedPassword,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "报警邮箱密码（加密存储）",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "smtp_host",
                        value = smtpHost,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "SMTP服务器地址",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "smtp_port",
                        value = smtpPort,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "SMTP服务器端口",
                        category = "alarm"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "smtp_tls",
                        value = useTls.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "是否使用TLS加密",
                        category = "alarm"
                    )
                )
                
                viewModel.updateAlarmEmailSettings(isEnabled, emailAddress, emailPassword, smtpHost, smtpPort, useTls)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警邮件设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveSecureBookSettings(secureBook: com.autodroid.guardiansdk.data.model.SecureBook) {
        if (!secureBook.isValid()) {
            android.widget.Toast.makeText(requireContext(), "密码本格式不正确", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // 验证密码本格式
        if (!com.autodroid.guardiansdk.utils.SecureBookUtils.validateSecureBook(secureBook)) {
            android.widget.Toast.makeText(requireContext(), "密码本格式不正确，请检查是否有重复值或缺失项", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // 编码密码本
                val encodedSecureBook = com.autodroid.guardiansdk.utils.SecureBookUtils.encodeSecureBook(secureBook)
                
                // 保存到数据库
                val setting = com.autodroid.guardiansdk.data.entity.Setting(
                    key = "secure_book",
                    value = encodedSecureBook,
                    type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                    description = "密码本设置",
                    category = "security"
                )
                
                withContext(Dispatchers.IO) {
                    viewModel.insertOrUpdateSetting(setting)
                }
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "密码本已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun saveHiddenSecureUISettings(dialogView: View) {
        val switchHiddenUIEnabled = dialogView.findViewById<android.widget.Switch>(R.id.switch_hidden_ui_enabled)
        val etDoorPassphrase = dialogView.findViewById<EditText>(R.id.et_door_passphrase)
        
        val isHiddenUIEnabled = switchHiddenUIEnabled.isChecked
        val doorPassphrase = etDoorPassphrase.text.toString().trim()
        
        if (doorPassphrase.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "请输入开门密语",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // 使用ViewModel的更新方法
                viewModel.updateHiddenSecureUI(isHiddenUIEnabled, doorPassphrase)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "隐秘界面设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveTestModeSettings(dialogView: View) {
        val cbTestMode = dialogView.findViewById<android.widget.CheckBox>(R.id.cbTestMode)
        
        val isEnabled = cbTestMode.isChecked
        
        lifecycleScope.launch {
            try {
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "test_mode_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "测试模式启用状态",
                        category = "test"
                    )
                )
                
                viewModel.updateTestMode(isEnabled)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "测试模式设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showPingSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_ping_settings, null)

        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_ping_enabled)
        val seekCheckInterval = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_check_interval)
        val tvCheckIntervalValue = dialogView.findViewById<TextView>(R.id.tv_check_interval_value)
        val seekEmailRetryCount = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_email_retry_count)
        val tvEmailRetryCountValue = dialogView.findViewById<TextView>(R.id.tv_email_retry_count_value)
        val seekEmailTimeout = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_email_timeout)
        val tvEmailTimeoutValue = dialogView.findViewById<TextView>(R.id.tv_email_timeout_value)
        val cbUseSmsFallback = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_use_sms_fallback)

        lifecycleScope.launch {
            val isEnabled = withContext(Dispatchers.IO) {
                viewModel.getBoolean("ping_enabled", false)
            }
            val checkInterval = withContext(Dispatchers.IO) {
                viewModel.getInt("ping_check_interval", 30)
            }
            val emailRetryCount = withContext(Dispatchers.IO) {
                viewModel.getInt("ping_email_retry_count", 3)
            }
            val emailTimeout = withContext(Dispatchers.IO) {
                viewModel.getInt("ping_email_timeout", 60)
            }
            val useSmsFallback = withContext(Dispatchers.IO) {
                viewModel.getBoolean("ping_use_sms_fallback", true)
            }

            cbEnabled.isChecked = isEnabled
            seekCheckInterval.progress = checkInterval
            tvCheckIntervalValue.text = "当前值: ${checkInterval}分钟"
            seekEmailRetryCount.progress = emailRetryCount
            tvEmailRetryCountValue.text = "当前值: ${emailRetryCount}次"
            seekEmailTimeout.progress = emailTimeout
            tvEmailTimeoutValue.text = "当前值: ${emailTimeout}分钟"
            cbUseSmsFallback.isChecked = useSmsFallback
        }

        seekCheckInterval.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvCheckIntervalValue.text = "当前值: ${progress}分钟"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmailRetryCount.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvEmailRetryCountValue.text = "当前值: ${progress}次"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmailTimeout.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvEmailTimeoutValue.text = "当前值: ${progress}分钟"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        AlertDialog.Builder(requireContext())
            .setTitle("Ping响应设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                savePingSettingsSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun savePingSettingsSettings(dialogView: View) {
        val cbEnabled = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_ping_enabled)
        val seekCheckInterval = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_check_interval)
        val seekEmailRetryCount = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_email_retry_count)
        val seekEmailTimeout = dialogView.findViewById<android.widget.SeekBar>(R.id.seek_bar_email_timeout)
        val cbUseSmsFallback = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_use_sms_fallback)

        val isEnabled = cbEnabled.isChecked
        val checkInterval = seekCheckInterval.progress
        val emailRetryCount = seekEmailRetryCount.progress
        val emailTimeout = seekEmailTimeout.progress
        val useSmsFallback = cbUseSmsFallback.isChecked

        lifecycleScope.launch {
            try {
                val settings = listOf(
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "ping_enabled",
                        value = isEnabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "Ping响应启用状态",
                        category = "ping"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "ping_check_interval",
                        value = checkInterval.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "Ping检查间隔（分钟）",
                        category = "ping"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "ping_email_retry_count",
                        value = emailRetryCount.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "邮件重试次数",
                        category = "ping"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "ping_email_timeout",
                        value = emailTimeout.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "邮件超时时间（分钟）",
                        category = "ping"
                    ),
                    com.autodroid.guardiansdk.data.entity.Setting(
                        key = "ping_use_sms_fallback",
                        value = useSmsFallback.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "邮件失败时使用短信",
                        category = "ping"
                    )
                )

                viewModel.updatePingSettings(isEnabled, checkInterval, emailRetryCount, emailTimeout, useSmsFallback)
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "Ping响应设置已保存",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 显示监护人编辑对话框
     */
    private fun showGuardianDialog(index: Int) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_my_guardian, null)

        val etGuardianName = dialogView.findViewById<EditText>(R.id.et_guardian_name)
        val etGuardianPhone = dialogView.findViewById<EditText>(R.id.et_guardian_phone)
        val cbPrimary = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_guardian_primary)
        val tvPhoneError = dialogView.findViewById<TextView>(R.id.tv_phone_error)

        // 加载已有的联系人数据 - 从当前设置项中获取，确保数据一致性
        lifecycleScope.launch {
            val currentItems = viewModel.settingItems.value ?: emptyList()
            
            // 使用单独的变量来存储数据
            var name = ""
            var phoneNumber = ""
            var isPrimary = false
            var isPlaceholder = true
            
            when (index) {
                1 -> {
                    val item = currentItems.find { it is SettingItem.GuardianItem1 } as? SettingItem.GuardianItem1
                    if (item != null) {
                        name = item.name
                        phoneNumber = item.phoneNumber
                        isPrimary = item.isPrimary
                        isPlaceholder = item.isPlaceholder
                    }
                }
                2 -> {
                    val item = currentItems.find { it is SettingItem.GuardianItem2 } as? SettingItem.GuardianItem2
                    if (item != null) {
                        name = item.name
                        phoneNumber = item.phoneNumber
                        isPrimary = item.isPrimary
                        isPlaceholder = item.isPlaceholder
                    }
                }
                3 -> {
                    val item = currentItems.find { it is SettingItem.GuardianItem3 } as? SettingItem.GuardianItem3
                    if (item != null) {
                        name = item.name
                        phoneNumber = item.phoneNumber
                        isPrimary = item.isPrimary
                        isPlaceholder = item.isPlaceholder
                    }
                }
                4 -> {
                    val item = currentItems.find { it is SettingItem.GuardianItem4 } as? SettingItem.GuardianItem4
                    if (item != null) {
                        name = item.name
                        phoneNumber = item.phoneNumber
                        isPrimary = item.isPrimary
                        isPlaceholder = item.isPlaceholder
                    }
                }
                5 -> {
                    val item = currentItems.find { it is SettingItem.GuardianItem5 } as? SettingItem.GuardianItem5
                    if (item != null) {
                        name = item.name
                        phoneNumber = item.phoneNumber
                        isPrimary = item.isPrimary
                        isPlaceholder = item.isPlaceholder
                    }
                }
            }
            
            if (!isPlaceholder) {
                etGuardianName.setText(name)
                etGuardianPhone.setText(phoneNumber)
                cbPrimary.isChecked = isPrimary
            } else {
                // 如果是占位符，使用默认值
                etGuardianName.setText("")
                etGuardianPhone.setText("")
                cbPrimary.isChecked = false
            }
        }

        // 实时验证手机号格式
        etGuardianPhone.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val phone = s?.toString()?.trim() ?: ""
                if (phone.isNotEmpty() && !isValidPhoneNumber(phone)) {
                    tvPhoneError.visibility = View.VISIBLE
                } else {
                    tvPhoneError.visibility = View.GONE
                }
            }
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("监护人${index}")
            .setView(dialogView)
            .setPositiveButton("保存", null) // 先设置为null，后面手动处理
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                // 保存联系人数据
                val name = etGuardianName.text.toString().trim()
                val phone = etGuardianPhone.text.toString().trim()
                val isPrimary = cbPrimary.isChecked

                if (name.isEmpty() || phone.isEmpty()) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "请填写完整信息",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // 验证手机号格式
                if (!isValidPhoneNumber(phone)) {
                    tvPhoneError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel.saveGuardianContact(index, name, phone, isPrimary)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "监护人${index}已保存",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }

    /**
     * 显示报警信息密码对话框（已移除，现在集成到密码本设置中）
     */
    private fun showAlarmMessageDialog() {
        // 直接打开密码本设置对话框
        showPasswordBookDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}