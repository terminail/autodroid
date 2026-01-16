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
 * 包括：报警模式、位置密码本等设置项和历史记录
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
        
        // 加载设置项
        viewModel.loadSettingItems()
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

            override fun onPasswordBookClick() {
                showPasswordBookDialog()
            }

            override fun onDoorPassphraseClick() {
                showDoorPassphraseDialog()
            }

            override fun onTestModeClick() {
                showTestModeDialog()
            }

            override fun onAlarmMessageClick() {
                showAlarmMessageDialog()
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
        lifecycleScope.launch {
            viewModel.settingItems.collect { settingItems ->
                settingAdapter.updateItems(settingItems)
            }
        }
    }

    /**
     * 显示报警模式-音量键设置对话框
     */
    private fun showVolumeKeyAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_alarm_mode_dialog_volume_key, null)

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
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmergencyTime.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvEmergencyTime.text = "当前值: ${progress}秒"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警模式-音量键设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警模式-音量键设置
                saveVolumeKeyAlarmModeSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示报警模式-浮动窗口设置对话框
     */
    private fun showFloatingWindowAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_alarm_mode_dialog_floating_window, null)

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
                tvEmergencyTime.text = "当前值: ${progress}秒"
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
            .setTitle("报警模式-浮动窗口设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警模式-浮动窗口设置
                saveFloatingWindowAlarmModeSettings(dialogView)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    /**
     * 显示报警模式-摇动手机设置对话框
     */
    private fun showShakePhoneAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_alarm_mode_dialog_shake_phone, null)

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
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekEmergencySensitivity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvEmergencySensitivity.text = "当前值: ${progress}级"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("报警模式-摇动手机设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存报警模式-摇动手机设置
                saveShakePhoneAlarmModeSettings(dialogView)
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
     * 显示门密码设置对话框
     */
    private fun showDoorPassphraseDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_door_passphrase, null)

        AlertDialog.Builder(requireContext())
            .setTitle("门密码设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存门密码设置
                saveDoorPassphraseSettings(dialogView)
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

        AlertDialog.Builder(requireContext())
            .setTitle("测试模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存测试模式设置
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
                
                withContext(Dispatchers.IO) {
                    viewModel.insertOrUpdateAllSettings(settings)
                }
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警模式-音量键设置已保存",
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
                
                withContext(Dispatchers.IO) {
                    viewModel.insertOrUpdateAllSettings(settings)
                }
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警模式-浮动窗口设置已保存",
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
                
                withContext(Dispatchers.IO) {
                    viewModel.insertOrUpdateAllSettings(settings)
                }
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "报警模式-摇动手机设置已保存",
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
    
    private fun saveWipeAlarmInfoSettings(dialogView: View) {}
    private fun saveDoorPassphraseSettings(dialogView: View) {}
    private fun saveTestModeSettings(dialogView: View) {}

    /**
     * 显示报警联系人编辑对话框
     */
    private fun showGuardianDialog(index: Int) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_my_guardian, null)

        val etGuardianName = dialogView.findViewById<EditText>(R.id.et_guardian_name)
        val etGuardianPhone = dialogView.findViewById<EditText>(R.id.et_guardian_phone)
        val cbPrimary = dialogView.findViewById<android.widget.CheckBox>(R.id.cb_guardian_primary)
        val tvPhoneError = dialogView.findViewById<TextView>(R.id.tv_phone_error)

        // 加载已有的联系人数据
        lifecycleScope.launch {
            val guardianName = viewModel.getGuardianName(index)
            val guardianPhone = viewModel.getGuardianPhoneNumber(index)
            val guardianPrimary = viewModel.getGuardianPrimary(index)

            etGuardianName.setText(guardianName)
            etGuardianPhone.setText(guardianPhone)
            cbPrimary.isChecked = guardianPrimary
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
            .setTitle("报警联系人${index}")
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
                        "报警联系人${index}已保存",
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