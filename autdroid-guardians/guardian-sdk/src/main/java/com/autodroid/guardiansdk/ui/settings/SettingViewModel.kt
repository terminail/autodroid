package com.autodroid.guardiansdk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.entity.Setting
import com.autodroid.guardiansdk.data.entity.Contact
import com.autodroid.guardiansdk.data.entity.ContactType
import com.autodroid.guardiansdk.ui.settings.model.SettingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页面的ViewModel
 * 负责加载和更新设置项数据（报警触发模式、报警密码等）
 */
class SettingViewModel(private val database: GuardianDatabase) : ViewModel() {
    
    private val _settingItems = MutableStateFlow<List<SettingItem>>(emptyList())
    val settingItems: StateFlow<List<SettingItem>> = _settingItems.asStateFlow()
    
    private val settingDao = database.settingDao()
    
    /**
     * 加载所有设置项
     */
    fun loadSettingItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settingItems = mutableListOf<SettingItem>()
                
                // 从Contact表加载5个监护人数据
                val contactDao = database.contactDao()
                val guardians = contactDao.getGuardians()
                
                // 生成5个监护人项，如果数据库中的联系人不足，用占位符
                for (i in 1..5) {
                    val guardian = guardians.find { it.orderIndex == i }
                    if (guardian != null) {
                        when (i) {
                            1 -> settingItems.add(SettingItem.GuardianItem1(
                                phoneNumber = guardian.phoneNumber,
                                name = guardian.name,
                                isPrimary = guardian.isPrimary,
                                isPlaceholder = false
                            ))
                            2 -> settingItems.add(SettingItem.GuardianItem2(
                                phoneNumber = guardian.phoneNumber,
                                name = guardian.name,
                                isPrimary = guardian.isPrimary,
                                isPlaceholder = false
                            ))
                            3 -> settingItems.add(SettingItem.GuardianItem3(
                                phoneNumber = guardian.phoneNumber,
                                name = guardian.name,
                                isPrimary = guardian.isPrimary,
                                isPlaceholder = false
                            ))
                            4 -> settingItems.add(SettingItem.GuardianItem4(
                                phoneNumber = guardian.phoneNumber,
                                name = guardian.name,
                                isPrimary = guardian.isPrimary,
                                isPlaceholder = false
                            ))
                            5 -> settingItems.add(SettingItem.GuardianItem5(
                                phoneNumber = guardian.phoneNumber,
                                name = guardian.name,
                                isPrimary = guardian.isPrimary,
                                isPlaceholder = false
                            ))
                        }
                    } else {
                        // 使用占位符
                        when (i) {
                            1 -> settingItems.add(SettingItem.GuardianItem1(
                                phoneNumber = "",
                                name = "请设置监护人${i}",
                                isPrimary = false,
                                isPlaceholder = true
                            ))
                            2 -> settingItems.add(SettingItem.GuardianItem2(
                                phoneNumber = "",
                                name = "请设置监护人${i}",
                                isPrimary = false,
                                isPlaceholder = true
                            ))
                            3 -> settingItems.add(SettingItem.GuardianItem3(
                                phoneNumber = "",
                                name = "请设置监护人${i}",
                                isPrimary = false,
                                isPlaceholder = true
                            ))
                            4 -> settingItems.add(SettingItem.GuardianItem4(
                                phoneNumber = "",
                                name = "请设置监护人${i}",
                                isPrimary = false,
                                isPlaceholder = true
                            ))
                            5 -> settingItems.add(SettingItem.GuardianItem5(
                                phoneNumber = "",
                                name = "请设置监护人${i}",
                                isPrimary = false,
                                isPlaceholder = true
                            ))
                        }
                    }
                }
                
                // 添加报警触发模式相关设置，加载实际数据
                settingItems.addAll(loadAlarmModeSettings())
                
                // 添加录音模式
                settingItems.add(SettingItem.AlarmRecordingMode())
                
                // 添加邮件配置
                settingItems.add(SettingItem.AlarmEmailSettings())
                
                // 添加报警信息密码
                settingItems.add(SettingItem.AlarmMessagePassword())
                
                // 添加Ping设置
                settingItems.add(loadPingSettings())
                
                // 添加隐秘界面设置
                settingItems.add(SettingItem.HiddenSecureUI())
                
                // 添加其他常规设置
                settingItems.add(SettingItem.TestMode())

                _settingItems.value = settingItems
                
            } catch (e: Exception) {
                // 出错时返回空列表
                _settingItems.value = emptyList()
            }
        }
    }
    
    /**
     * 加载报警触发模式设置项的实际数据
     */
    private suspend fun loadAlarmModeSettings(): List<SettingItem> {
        val alarmModeSettings = mutableListOf<SettingItem>()
        
        // 音量键报警触发模式
        val volumeKeyEnabled = settingDao.getSetting("volume_key_alarm_enabled")?.value?.toBoolean() ?: true
        val normalTime = settingDao.getSetting("volume_key_normal_hold_time")?.value?.toIntOrNull() ?: 5
        val emergencyTime = settingDao.getSetting("volume_key_emergency_hold_time")?.value?.toIntOrNull() ?: 10
        
        val volumeKeySetting = SettingItem.VolumeKeyAlarmMode(
            enabled = volumeKeyEnabled,
            normalHoldTime = normalTime,
            emergencyHoldTime = emergencyTime
        )
        alarmModeSettings.add(volumeKeySetting)
        
        // 浮动窗口报警触发模式
        val floatingWindowEnabled = settingDao.getSetting("floating_window_alarm_enabled")?.value?.toBoolean() ?: true
        val floatingNormalTime = settingDao.getSetting("floating_window_normal_hold_time")?.value?.toIntOrNull() ?: 5
        val floatingNormalOpacity = settingDao.getSetting("floating_window_normal_opacity")?.value?.toIntOrNull() ?: 50
        val floatingEmergencyTime = settingDao.getSetting("floating_window_emergency_hold_time")?.value?.toIntOrNull() ?: 10
        val floatingEmergencyOpacity = settingDao.getSetting("floating_window_emergency_opacity")?.value?.toIntOrNull() ?: 80
        
        val floatingWindowSetting = SettingItem.FloatingWindowAlarmMode(
            enabled = floatingWindowEnabled,
            normalHoldTime = floatingNormalTime,
            normalOpacity = floatingNormalOpacity,
            emergencyHoldTime = floatingEmergencyTime,
            emergencyOpacity = floatingEmergencyOpacity
        )
        alarmModeSettings.add(floatingWindowSetting)
        
        // 摇动手机报警触发模式
        val shakeEnabled = settingDao.getSetting("shake_alarm_enabled")?.value?.toBoolean() ?: true
        val normalSensitivity = settingDao.getSetting("shake_normal_sensitivity")?.value?.toIntOrNull() ?: 5
        val emergencySensitivity = settingDao.getSetting("shake_emergency_sensitivity")?.value?.toIntOrNull() ?: 8
        
        val shakePhoneSetting = SettingItem.ShakePhoneAlarmMode(
            enabled = shakeEnabled,
            normalSensitivity = normalSensitivity,
            emergencySensitivity = emergencySensitivity
        )
        alarmModeSettings.add(shakePhoneSetting)
        
        // 长时间未使用手机报警触发模式
        val inactivityEnabled = settingDao.getSetting("inactivity_alarm_enabled")?.value?.toBoolean() ?: true
        val normalInactivityTime = settingDao.getSetting("inactivity_normal_time")?.value?.toIntOrNull() ?: 60
        val emergencyInactivityTime = settingDao.getSetting("inactivity_emergency_time")?.value?.toIntOrNull() ?: 30
        
        val inactivitySetting = SettingItem.InactivityAlarmMode(
            enabled = inactivityEnabled,
            normalInactivityTime = normalInactivityTime,
            emergencyInactivityTime = emergencyInactivityTime
        )
        alarmModeSettings.add(inactivitySetting)
        
        return alarmModeSettings
    }

    /**
     * 更新音量键报警触发模式设置
     */
    fun updateVolumeKeyAlarmMode(enabled: Boolean, normalTime: Int, emergencyTime: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "volume_key_alarm_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "音量键报警启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "volume_key_normal_hold_time",
                        value = normalTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "音量键普通报警长按时间（秒）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "volume_key_emergency_hold_time",
                        value = emergencyTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "音量键紧急报警长按时间（秒）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
                // 重新加载设置项以更新UI
                loadSettingItems()
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新浮动窗口报警触发模式设置
     */
    fun updateFloatingWindowAlarmMode(enabled: Boolean, normalTime: Int, normalOpacity: Int, emergencyTime: Int, emergencyOpacity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "floating_window_alarm_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "浮动窗口报警启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_normal_hold_time",
                        value = normalTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口普通报警长按时间（秒）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_normal_opacity",
                        value = normalOpacity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口普通报警透明度（%）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_emergency_hold_time",
                        value = emergencyTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口紧急报警长按时间（秒）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_emergency_opacity",
                        value = emergencyOpacity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口紧急报警透明度（%）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
                // 重新加载设置项以更新UI
                loadSettingItems()
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新摇动手机报警触发模式设置
     */
    fun updateShakePhoneAlarmMode(enabled: Boolean, normalSensitivity: Int, emergencySensitivity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "shake_alarm_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "摇动手机报警启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "shake_normal_sensitivity",
                        value = normalSensitivity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "摇动手机普通报警敏感度（1-10级）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "shake_emergency_sensitivity",
                        value = emergencySensitivity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "摇动手机紧急报警敏感度（1-10级）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
                // 重新加载设置项以更新UI
                loadSettingItems()
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 根据键获取设置项
     */
    suspend fun getSetting(key: String): Setting? {
        return settingDao.getSetting(key)
    }
    
    /**
     * 批量插入或更新设置项
     */
    suspend fun insertOrUpdateAllSettings(settings: List<Setting>) {
        settingDao.insertOrUpdateAll(settings)
    }
    
    /**
     * 插入或更新单个设置项
     */
    suspend fun insertOrUpdateSetting(setting: Setting) {
        settingDao.insertOrUpdate(setting)
    }

    /**
     * 保存监护人
     */
    fun saveGuardianContact(index: Int, name: String, phoneNumber: String, isPrimary: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contactDao = database.contactDao()
                
                // 创建或更新联系人
                val contact = Contact(
                    phoneNumber = phoneNumber,
                    name = name,
                    type = ContactType.GUARDIAN,
                    relationship = "监护人",
                    isPrimary = isPrimary,
                    orderIndex = index
                )
                
                contactDao.insertOrUpdate(contact)

                // 重新加载设置项以更新UI
                loadSettingItems()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 获取监护人姓名
     */
    suspend fun getGuardianName(index: Int): String {
        val contactDao = database.contactDao()
        val guardian = contactDao.getGuardianByOrderIndex(index)
        return guardian?.name ?: ""
    }

    /**
     * 获取监护人电话
     */
    suspend fun getGuardianPhoneNumber(index: Int): String {
        val contactDao = database.contactDao()
        val guardian = contactDao.getGuardianByOrderIndex(index)
        return guardian?.phoneNumber ?: ""
    }

    /**
     * 获取监护人是否主要
     */
    suspend fun getGuardianPrimary(index: Int): Boolean {
        val contactDao = database.contactDao()
        val guardian = contactDao.getGuardianByOrderIndex(index)
        return guardian?.isPrimary ?: false
    }

    /**
     * 加载Ping设置
     */
    private suspend fun loadPingSettings(): SettingItem.PingSettings {
        val enabled = settingDao.getSetting("ping_enabled")?.value?.toBoolean() ?: false
        val checkInterval = settingDao.getSetting("ping_check_interval")?.value?.toIntOrNull() ?: 30
        val emailRetryCount = settingDao.getSetting("ping_email_retry_count")?.value?.toIntOrNull() ?: 3
        val emailTimeout = settingDao.getSetting("ping_email_timeout")?.value?.toIntOrNull() ?: 60
        val useSmsFallback = settingDao.getSetting("ping_use_sms_fallback")?.value?.toBoolean() ?: true

        return SettingItem.PingSettings(
            enabled = enabled,
            checkInterval = checkInterval,
            emailRetryCount = emailRetryCount,
            emailTimeout = emailTimeout,
            useSmsFallback = useSmsFallback
        )
    }

    /**
     * 获取布尔设置
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return settingDao.getSetting(key)?.value?.toBooleanStrictOrNull() ?: defaultValue
    }

    /**
     * 获取整数设置
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        return settingDao.getSetting(key)?.value?.toIntOrNull() ?: defaultValue
    }
}